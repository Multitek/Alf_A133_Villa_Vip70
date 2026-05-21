package com.alfanar.villaroom.activities.intercom;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityOutGoingDoorConnectedBinding;
import com.alfanar.villaroom.interfaces.CallListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.ImagesModel;
import com.alfanar.villaroom.sockets.TCPSenderThread;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.android.material.textfield.TextInputLayout;

import org.linphone.AndroidAudioManager;
import org.linphone.LinphoneManager;
import org.linphone.core.Call;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutGoingDoorConnected extends AppCompatActivity implements View.OnClickListener, CallListener, Handler.Callback {
    public static String callId;
    private static OutGoingDoorConnected instance;
    private final int CLOSE_CALL = 100, REMOVE_NEW_CALL_TEXT = 101, DOOR_DIALOG_DISMISS = 102;
    boolean streamsRunning = false;
    ActivityOutGoingDoorConnectedBinding binding;
    private boolean ledStatus = false;
    private String callToIp = "null";
    private boolean micEnabled = false;
    private Core mCore;
    private boolean close = false;
    private String path = "";
    private Dialog dialogDoorOpened, dialogDoorSettings;
    private Handler handler;
    private final CoreListenerStub stub = new CoreListenerStub() {
        @Override
        public void onCallStateChanged(@NonNull Core lc, @NonNull final Call call, Call.State cstate, @NonNull String message) {
            super.onCallStateChanged(lc, call, cstate, message);
            boolean busy = cstate.equals(Call.State.Error) && message.toLowerCase().startsWith("busy");

            Logger.d("OutGoingDoorConnected.onCallStateChanged state = " + cstate + " | message = " + message + " | callNb = " + lc.getCallsNb());

            if (lc.getCurrentCall() == null || lc.getCallsNb() == 0) {
                if (busy) {
                    runOnUiThread(() -> {
                        binding.textNewCall.setVisibility(View.VISIBLE);
                        binding.textNewCall.setText(getResources().getString(R.string.device_busy));
                    });
                    if (!close) {
                        close = true;
                        handler.removeMessages(CLOSE_CALL);
                        handler.sendEmptyMessageDelayed(CLOSE_CALL, 2500);
                    }
                } else {
                    if (!close) {
                        handler.removeMessages(CLOSE_CALL);
                        handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
                    }
                }
                return;
            }

            if (cstate.equals(Call.State.Connected)) {

                if (!streamsRunning) {
                    try {
                        String relayAttribute = call.getRemoteParams() != null ?
                                (call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_COUNT.name()) == null ? "1" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_COUNT.name()))
                                : "1";
                        int relayCount = 1;

                        try {
                            relayCount = (relayAttribute == null || relayAttribute.isEmpty()) ? 1 : Integer.parseInt(relayAttribute);
                        } catch (NumberFormatException e) {
                            Log.e("EXCEPTION", Log.getStackTraceString(e));
                        }

                        if (relayCount == 2) {
                            runOnUiThread(() -> {
                                binding.layoutRelay1.setVisibility(View.VISIBLE);
                                binding.layoutRelay2.setVisibility(View.VISIBLE);
                                binding.imgOpenDoor.setVisibility(View.GONE);
                            });
                        } else {
                            runOnUiThread(() -> {
                                binding.layoutRelay1.setVisibility(View.GONE);
                                binding.layoutRelay2.setVisibility(View.GONE);
                                binding.imgOpenDoor.setVisibility(View.VISIBLE);
                            });
                        }

                        String doorRelayNames = call.getRemoteParams() != null ? (call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_NAMES.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_NAMES.name()))
                                : "";
                        try {
                            if (doorRelayNames != null) {
                                String[] names = doorRelayNames.split("&&");
                                if (names.length > 1) {
                                    runOnUiThread(() -> {
                                        binding.txtRelay1Name.setText(names[0].isEmpty() ? getString(R.string.outdoor1) : names[0]);
                                        binding.txtRelay2Name.setText(names[1].isEmpty() ? getString(R.string.outdoor2) : names[1]);
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e("EXCEPTION", Log.getStackTraceString(e));
                        }


                    } catch (Exception e) {
                        Log.e("EXCEPTION", Log.getStackTraceString(e));
                    }

                }
            } else if (cstate.equals(Call.State.StreamsRunning)) {
                streamsRunning = true;
                path = takePhotoOutgoing(callId);
                runOnUiThread(() -> {
                    binding.imgTakePhoto.setClickable(true);
                    binding.imgOpenDoor.setClickable(true);
                    binding.layoutRelay1.setClickable(true);
                    binding.layoutRelay2.setClickable(true);
                    binding.imgLedOnOff.setClickable(true);
                    binding.includeTop.imgOther.setClickable(true);
                    binding.fabAnswer.setClickable(true);
                    binding.imgVolSetting.setVisibility(View.VISIBLE);
                });


            }
        }

        @Override
        public void onMessageReceived(@NonNull Core lc, @NonNull ChatRoom room, @NonNull ChatMessage message) {
            String messageDetail = ((message.getUtf8Text() == null) ? "null" : message.getUtf8Text());
            Log.d("alfanar ", " [OutgoingDoorConnected] messageReceived_message= " + messageDetail);
            if (message.getUtf8Text() != null) {
                String allMessage = message.getUtf8Text();
                if (allMessage.startsWith("CALL_BUSY")) {
                    runOnUiThread(() -> {
                        binding.textNewCall.setVisibility(View.VISIBLE);
                        binding.textNewCall.setText(getResources().getString(R.string.device_busy));
                    });
                    handler.removeMessages(CLOSE_CALL);
                    handler.sendEmptyMessageDelayed(CLOSE_CALL, 2500);
                }
            }
        }
    };
    private DeviceModel dev;


    public static OutGoingDoorConnected getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityOutGoingDoorConnectedBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().removeTimeout();
        mCore = LinphoneManager.getInstance().getCore();
        handler = new Handler(Looper.getMainLooper(), this);

        if (getIntent().getExtras() != null) {
            callToIp = getIntent().getExtras().getString("remote_ip", "null");
        }

        binding.imgTakePhoto.setOnClickListener(this);
        binding.fabAnswer.setOnClickListener(this);
        binding.fabClose.setOnClickListener(this);
        binding.imgOpenDoor.setOnClickListener(this);
        binding.layoutRelay1.setOnClickListener(this);
        binding.layoutRelay2.setOnClickListener(this);
        binding.imgLedOnOff.setOnClickListener(this);

        binding.imgVolSetting.setVisibility(View.GONE);
        binding.imgVolSetting.setOnClickListener(this);

        binding.includeTop.imgBack.setVisibility(View.GONE);
        binding.includeTop.imgOther.setOnClickListener(this);
        binding.includeTop.imgOther.setClickable(false);

        binding.imgTakePhoto.setClickable(false);
        binding.imgOpenDoor.setClickable(false);
        binding.layoutRelay1.setClickable(false);
        binding.layoutRelay2.setClickable(false);
        binding.imgLedOnOff.setClickable(false);
        binding.fabAnswer.setClickable(false);

        binding.imgMicrophone.setOnClickListener(this);
        binding.imgMicrophone.setClickable(false);

        LinphoneManager.getInstance().setMicrophoneDisable();

        MainTimeout.getInstance().removeTimeout();
        handler.removeMessages(CLOSE_CALL);
        handler.sendEmptyMessageDelayed(CLOSE_CALL, 180000);

        if (!callToIp.equals("null")) {
            mCore.addListener(stub);
            mCore.setNativeVideoWindowId(binding.remoteVideo);
            mCore.setNativePreviewWindowId(binding.selfVideo);
            LinphoneManager.getInstance().setMicrophoneDisable();
            LinphoneManager.getInstance().connectDoor(callToIp);
            LinphoneManager.getInstance().setMicrophoneDisable();
        } else {
            binding.textNewCall.setVisibility(View.VISIBLE);
            binding.textNewCall.setText(getResources().getString(R.string.not_connected));
            handler.removeMessages(CLOSE_CALL);
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 2500);
        }


        MyUtils.getInstance().callListener = this;


        instance = this;
        dev = DeviceController.getInstance().getDoorWithIp(callToIp);
        if (dev != null) {
            binding.includeTop.textTitle.setText(dev.getName());
        } else {
            binding.includeTop.textTitle.setText(callToIp);
        }


    }

    private void setDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    CallModel callModel = DatabaseHelper.getInstance().getCall(callId);
                    if (streamsRunning) {
                        callModel.setCallData(AppEnums.Connected.name());
                    } else {
                        callModel.setCallData("Not connected");
                    }
                    callModel.setCallImgPath(path);
                    callModel.setCallReadState(true);
                    DatabaseHelper.getInstance().updateCall(callModel);
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();
    }

    private synchronized String takePhotoOutgoing(String callId) {
        File file = new File(MyUtils.getInstance().getDoorImagesDir());
        Log.d("alfanar ", "[ActivityCallOutgoing]takePhotoOutgoing ");
        if (!file.exists()) {
            boolean b = file.mkdirs();
            Log.d("alfanar ", "[ActivityCallOutgoing] takePhotoOutgoing= " + b);
        }
        String FILE_PATH = file.getPath() + "/" + callId + ".jpeg";
        if (mCore != null) {
            if (mCore.getCurrentCall() != null) {
                mCore.getCurrentCall().takeVideoSnapshot(FILE_PATH);
            }

        }
        MyUtils.getInstance().controlCallImagesFolder();
        return FILE_PATH;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);


    }

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        MyUtils.getInstance().callListener = null;
        mCore.removeListener(stub);
        LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
        setDatabase();
        mCore.setNativeVideoWindowId(null);
        mCore.setNativePreviewWindowId(null);
        try {
            if (dialogDoorOpened != null) {
                dialogDoorOpened.dismiss();
            }
            if (dialogDoorSettings != null) {
                dialogDoorSettings.dismiss();
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        handler.removeMessages(CLOSE_CALL);
        handler.removeMessages(REMOVE_NEW_CALL_TEXT);
        handler.removeMessages(DOOR_DIALOG_DISMISS);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.fabAnswer.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            LinphoneManager.getInstance().setMicrophoneEnable();
            LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.START_AUDIO.name());
            int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
            int[] colors = new int[]{getColor(R.color.gray_new)};
            ColorStateList colorStateList = new ColorStateList(states, colors);
            binding.fabAnswer.setBackgroundTintList(colorStateList);


            micEnabled = true;
            binding.imgMicrophone.setClickable(true);
            binding.imgMicrophone.setImageResource(R.drawable.ic_microphone_enable);

        } else if (id == binding.fabClose.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            close = true;
            handler.removeMessages(CLOSE_CALL);
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
        } else if (id == binding.imgOpenDoor.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000, false);
            binding.imgOpenDoor.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.OPEN_DOOR.name());
            doorOpenDialogShow();
        } else if (id == binding.layoutRelay1.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000, false);
            binding.imgOpenDoor1.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.OPEN_DOOR.name());
            doorOpenDialogShow();
        } else if (id == binding.layoutRelay2.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000, false);
            binding.imgOpenDoor2.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.OPEN_DOOR2.name());
            doorOpenDialogShow();
        } else if (id == binding.imgLedOnOff.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            ledStatus = !ledStatus;
            ImageView imv = (ImageView) v;
            if (ledStatus) {
                LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.LED_ON.name());
                imv.setImageResource(R.drawable.ic_light_enable);
            } else {
                LinphoneManager.getInstance().sendLocalSipMessage(callToIp, AppEnums.LED_OFF.name());
                imv.setImageResource(R.drawable.ic_light_disable);
            }
        } else if (id == binding.imgTakePhoto.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            Call call = mCore.getCurrentCall();
            if (call == null || !call.getState().equals(Call.State.StreamsRunning)) {
                return;
            }
            binding.imgTakePhoto.setImageResource(R.drawable.ic_camera_enable);
            binding.imgTakePhoto.setClickable(false);
            String mac = null;
            try {
                mac = DeviceController.getInstance().getDoorWithIp(callToIp).getMac();
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }


            String safeMac = mac != null ? mac.replace(":", "") : "unknown";
            String path = MyUtils.getInstance().getCallSnapShotsDir() + "/" + safeMac + "_" + System.currentTimeMillis() + ".jpeg";

            call.takeVideoSnapshot(path);
            GeneralMediaPlayer.getInstance().playMedia(3);
            DatabaseHelper.getInstance().insertImage(new ImagesModel(path, String.valueOf(System.currentTimeMillis()), mac));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    binding.imgTakePhoto.setClickable(true);
                    binding.imgTakePhoto.setImageResource(R.drawable.ic_camera_disable);
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }, 2000);

        } else if (id == binding.includeTop.imgOther.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            displayDoorSettings();
        } else if (id == binding.imgVolSetting.getId()) {
            if (LinphoneManager.getInstance().getCore().getCurrentCall() != null) {
                Intent i = new Intent(OutGoingDoorConnected.this, SetStreamVolumeActivity.class);
                i.putExtra("fromRoom", false);
                i.putExtra("remoteIp", callToIp);
                startActivity(new Intent(i));
            }
        } else if (id == binding.imgMicrophone.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            if (micEnabled) {
                LinphoneManager.getInstance().setMicrophoneDisable();
                binding.imgMicrophone.setImageResource(R.drawable.ic_microphone_disable);
                micEnabled = false;
            } else {
                LinphoneManager.getInstance().setMicrophoneEnable();
                binding.imgMicrophone.setImageResource(R.drawable.ic_microphone_enable);
                micEnabled = true;
            }
        }
    }

    private void setState(final CallModel callModel) {
        if (callModel != null && !callModel.getCallId().equals(callId)) {
            String device;
            if (callModel.getCallType().startsWith(AppEnums.DOOR.name())) {
                device = AppEnums.DOOR.name();
            } else {
                device = AppEnums.ROOM.name();
            }
            showSecondCall(callModel, device);
        }
    }

    private void doorOpenDialogShow() {
        if (dialogDoorOpened != null && dialogDoorOpened.isShowing()) {
            dialogDoorOpened.dismiss();
        }
        GeneralMediaPlayer.getInstance().playMedia(4);
        dialogDoorOpened = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_door_opened);
        dialogDoorOpened.show();
        handler.sendEmptyMessageDelayed(DOOR_DIALOG_DISMISS, 3000);
    }

    private void displayDoorSettings() {
        if (dialogDoorSettings != null && dialogDoorSettings.isShowing()) {
            return;
        }


        dev = dev == null ? DeviceController.getInstance().getDoorWithIp(callToIp) : dev;
        dialogDoorSettings = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_door_settings);

        if (dialogDoorSettings.getWindow() != null) {
            CheckBox cbMaster = dialogDoorSettings.findViewById(R.id.cbMaster);
            EditText edDoorName = dialogDoorSettings.findViewById(R.id.ed_door_name);
            TextView fwVersion = dialogDoorSettings.findViewById(R.id.fw_version);
            TextView apkVersion = dialogDoorSettings.findViewById(R.id.apk_version);
            TextView txtMacAddress = dialogDoorSettings.findViewById(R.id.txtMacAddr);
            TextView staticIpText = dialogDoorSettings.findViewById(R.id.txtStaticIp);
            LinearLayout layoutRelaySettings = dialogDoorSettings.findViewById(R.id.layoutRelaySettings);
            EditText edRelay1 = dialogDoorSettings.findViewById(R.id.ed_relay1_name);
            EditText edRelay2 = dialogDoorSettings.findViewById(R.id.ed_relay2_name);
            RadioButton relay1 = dialogDoorSettings.findViewById(R.id.rb_relay1);
            RadioButton relay2 = dialogDoorSettings.findViewById(R.id.rb_relay2);
            CardView cardRelay1Name = dialogDoorSettings.findViewById(R.id.cardview_relay1);
            CardView cardRelay2Name = dialogDoorSettings.findViewById(R.id.cardview_relay2);
            TextInputLayout inputLayoutDeviceName = dialogDoorSettings.findViewById(R.id.text_input_device_name);
            TextInputLayout inputLayoutRelay1Name = dialogDoorSettings.findViewById(R.id.text_input_relay1_name);
            TextInputLayout inputLayoutRelay2Name = dialogDoorSettings.findViewById(R.id.text_input_relay2_name);




            Spinner spinner1 = dialogDoorSettings.findViewById(R.id.spnRelay1);
            Spinner spinner2 = dialogDoorSettings.findViewById(R.id.spnRelay2);

            String mac = dev.getMac();
            final int[] relayIndex1 = {MyUtils.getInstance().getShared().getInt(mac+"_relayIndex1", 0)};
            final int[] relayIndex2 = {MyUtils.getInstance().getShared().getInt(mac+"_relayIndex2", 0)};


            List<String> relayTimeOutList = new ArrayList<>();
            relayTimeOutList.add("3  sec");
            relayTimeOutList.add("5  sec");
            relayTimeOutList.add("10 sec");
            relayTimeOutList.add("20 sec");
            relayTimeOutList.add("30 sec");






            ArrayAdapter<String> ad1 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, relayTimeOutList);
            ad1.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinner1.setAdapter(ad1);
            spinner1.setSelection(relayIndex1[0]);
            spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    relayIndex1[0] = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });


            ArrayAdapter<String> ad2 = new ArrayAdapter<>(this, R.layout.custom_spinner_item, relayTimeOutList);
            ad2.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinner2.setAdapter(ad2);
            spinner2.setSelection(relayIndex2[0]);
            spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    relayIndex2[0] = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });




            edDoorName.setText(dev.getName());
            edRelay1.setText(dev.getRelay1Name());
            edRelay2.setText(dev.getRelay2Name());
            fwVersion.setText(dev.getFwVer());
            staticIpText.setText(dev.isStaticIp() ? getString(R.string.yes):getString(R.string.no));
            apkVersion.setText(String.format("%s / %s", dev.getAppVer(), dev.getBaseVer()));
            txtMacAddress.setText(String.format("%s / %s", dev.getIp(), dev.getMac()));
            boolean isSupport2relay = MyUtils.getInstance().isSupport2Relay(dev.getFwVer());
            if (isSupport2relay) {
                layoutRelaySettings.setVisibility(View.VISIBLE);
                if (dev.getRelayCount() == 2) {
                    relay2.setChecked(true);
                    relay1.setChecked(false);
                    cardRelay1Name.setVisibility(View.VISIBLE);
                    cardRelay2Name.setVisibility(View.VISIBLE);
                } else {
                    relay1.setChecked(true);
                    relay2.setChecked(false);
                    cardRelay1Name.setVisibility(View.GONE);
                    cardRelay2Name.setVisibility(View.GONE);
                }

                relay1.setOnClickListener(v -> {
                    relay1.setChecked(true);
                    relay2.setChecked(false);
                    cardRelay1Name.setVisibility(View.GONE);
                    cardRelay2Name.setVisibility(View.GONE);
                });

                relay2.setOnClickListener(v -> {
                    relay2.setChecked(true);
                    relay1.setChecked(false);
                    cardRelay1Name.setVisibility(View.VISIBLE);
                    cardRelay2Name.setVisibility(View.VISIBLE);
                });
            } else {
                layoutRelaySettings.setVisibility(View.GONE);
            }


            Button btnCancel = dialogDoorSettings.findViewById(R.id.btnCancel);
            btnCancel.setOnClickListener(
                    v -> {
                        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                        MyUtils.getInstance().hideKeyboard(OutGoingDoorConnected.this);
                        dialogDoorSettings.dismiss();
                    });

            Button btnSave = dialogDoorSettings.findViewById(R.id.btnSave);
            btnSave.setOnClickListener(
                    v -> {
                        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
                        MyUtils.getInstance().hideKeyboard(OutGoingDoorConnected.this);
                        String doorName = edDoorName.getText() != null ? edDoorName.getText().toString() : "";
                        String name1 = edRelay1.getText() != null ? edRelay1.getText().toString() : "";
                        String name2 = edRelay2.getText() != null ? edRelay2.getText().toString() : "";
                        if (doorName.isEmpty()) {
                            inputLayoutDeviceName.setErrorEnabled(true);
                            inputLayoutDeviceName.setError(
                                    getResources().getString(R.string.check_input));
                            inputLayoutDeviceName.setErrorTextColor(MyUtils.getInstance().getErrorTextInputColor());
                            timerRemoveInputError(inputLayoutDeviceName);
                        } else if (relay2.isChecked() && name1.isEmpty()) {
                            inputLayoutRelay1Name.setErrorEnabled(true);
                            inputLayoutRelay1Name.setError(
                                    getResources().getString(R.string.check_input));
                            inputLayoutRelay1Name.setErrorTextColor(MyUtils.getInstance().getErrorTextInputColor());
                            timerRemoveInputError(inputLayoutRelay1Name);
                        } else if (relay2.isChecked() && name2.isEmpty()) {
                            inputLayoutRelay2Name.setErrorEnabled(true);
                            inputLayoutRelay2Name.setError(
                                    getResources().getString(R.string.check_input));
                            inputLayoutRelay2Name.setErrorTextColor(MyUtils.getInstance().getErrorTextInputColor());
                            timerRemoveInputError(inputLayoutRelay2Name);
                        } else {
                            String r1Name = isSupport2relay ? (name1.isEmpty() ? "R1" : name1) : "";
                            String r2Name = isSupport2relay ? (name2.isEmpty() ? "R2" : name2) : "";
                            int relayCount = isSupport2relay ? (relay2.isChecked() ? 2 : 1) : 1;
                            boolean isMaster = cbMaster.isChecked();
                            new TCPSenderThread(
                                    callToIp,
                                    AppEnums.SET_DOOR_NAME_MASTER.name()
                                            + "#"
                                            + MyUtils.getInstance().getMACAddress()
                                            + "#"
                                            + doorName
                                            + "#"
                                            + isMaster
                                            + "#"
                                            + relayCount
                                            + "#"
                                            + r1Name
                                            + "#"
                                            + r2Name
                                            + "#"
                                            +  getRelayTimeoutSeconds(relayIndex1[0])
                                            + "#"
                                            +  getRelayTimeoutSeconds(relayIndex2[0])
                            )
                                    .start();
                            MyUtils.getInstance().getShared().edit().putInt(mac+"_relayIndex1",relayIndex1[0]).commit();
                            MyUtils.getInstance().getShared().edit().putInt(mac+"_relayIndex2",relayIndex2[0]).commit();
                            dialogDoorSettings.dismiss();
                        }

                    });

            dialogDoorSettings.show();
        }

        MyUtils.getInstance().hideKeyboard(OutGoingDoorConnected.this);
    }

    private String getRelayTimeoutSeconds(int index){
        return switch (index) {
            case 1 -> "5";
            case 2 -> "10";
            case 3 -> "20";
            case 4 -> "30";
            default -> "3";
        };
    }

    private void timerRemoveInputError(TextInputLayout textInputLayout) {
        if (textInputLayout != null) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(() -> {
                                textInputLayout.setHint(
                                        getResources()
                                                .getString(
                                                        R.string.enter_door_name));
                                textInputLayout.setError(null);
                            },
                            2500);
        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(OutGoingDoorConnected.this);
    }

    private void showSecondCall(final CallModel callModel, String device) {
        if (callModel == null) {
            return;
        }
        DeviceModel dev;
        if (device.equals("DOOR")) {
            dev = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
        } else {
            dev = DeviceController.getInstance().getRoomWithMac(callModel.getCallFrom());
        }
        if (dev != null) {
            final DeviceModel finalDev = dev;
            runOnUiThread(() -> {
                binding.textNewCall.setVisibility(View.VISIBLE);
                if (Objects.equals(callModel.getCallState(), AppEnums.Answered.name())) {
                    binding.textNewCall.setText(String.format(getResources().getString(R.string.call_answered), callModel.getCallData(), finalDev.getName()));
                } else if (Objects.equals(callModel.getCallState(), AppEnums.Declined.name())) {
                    binding.textNewCall.setText(String.format(getResources().getString(R.string.call_declined), callModel.getCallData(), finalDev.getName()));
                } else if (Objects.equals(callModel.getCallState(), AppEnums.Missed.name())) {
                    binding.textNewCall.setText(String.format(getResources().getString(R.string.call_missed), finalDev.getName()));
                } else {
                    binding.textNewCall.setText(MessageFormat.format("{0} {1}", finalDev.getName(), getResources().getString(R.string.calls_calling)));
                }
            });
            handler.removeMessages(REMOVE_NEW_CALL_TEXT);
            handler.sendEmptyMessageDelayed(REMOVE_NEW_CALL_TEXT, 3000);
        }
    }

    @Override
    public void incomingDoorCall(final CallModel callModel, String callerDoorIp) {
        runOnUiThread(() -> showSecondCall(callModel, AppEnums.DOOR.name()));
    }

    @Override
    public void incomingRoomCall(final CallModel callModel) {
        runOnUiThread(() -> showSecondCall(callModel, AppEnums.ROOM.name()));
    }

    @Override
    public void startParallelConnected(String remoteIP, String doorCount, String relay1Name, String relay2Name) {
    }

    @Override
    public void callStateChanged(final CallModel callModel) {
        runOnUiThread(() -> setState(callModel));
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case CLOSE_CALL:
                mCore.removeListener(stub);
                LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
                //SendSipMessage.sendLocalSipMessage(remote_ip,"BYE");
                MyUtils.getInstance().callListener = null;
                instance = null;
                GeneralMediaPlayer.getInstance().stopMedia();
                MyUtils.getInstance().backToRootActivity();
                OutGoingDoorConnected.this.finish();
                break;
            case REMOVE_NEW_CALL_TEXT:
                binding.textNewCall.setVisibility(View.GONE);
                break;
            case DOOR_DIALOG_DISMISS:
                try {
                    binding.imgOpenDoor.setImageResource(R.drawable.ic_key_disable);
                    binding.imgOpenDoor1.setImageResource(R.drawable.ic_key_disable);
                    binding.imgOpenDoor2.setImageResource(R.drawable.ic_key_disable);
                    if (dialogDoorOpened != null) {
                        dialogDoorOpened.dismiss();
                    }
                    MyUtils.getInstance().hideNavigation(OutGoingDoorConnected.this);
                    binding.layoutRelay1.setClickable(true);
                    binding.layoutRelay2.setClickable(true);
                    binding.imgOpenDoor.setClickable(true);
                } catch (Exception e) {
                    Log.e("EXCEPTION", Log.getStackTraceString(e));
                }

                break;
            default:
                break;
        }
        return false;
    }
}
