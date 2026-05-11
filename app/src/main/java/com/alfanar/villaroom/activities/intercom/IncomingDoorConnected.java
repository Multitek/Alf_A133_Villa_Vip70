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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityIncomingDoorConnectedBinding;
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
import com.google.gson.Gson;

import org.linphone.AndroidAudioManager;
import org.linphone.LinphoneManager;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import java.text.MessageFormat;
import java.util.Objects;

public class IncomingDoorConnected extends AppCompatActivity implements View.OnClickListener, CallListener, Handler.Callback {

    private static IncomingDoorConnected instance;
    private final int CLOSE_CALL = 100, REMOVE_NEW_CALL_TEXT = 101, DOOR_DIALOG_DISMISS = 102, TAKE_PHOTO_IMAGE_DEFAULT = 103, DECLINE = 104, PLAY_RING = 105, REFRESH_CALL = 999;
    boolean req = false;
    private boolean ledStatus = true;
    private String callFromIp = "null";
    private String callId;
    private CallModel callModel;
    private boolean callAnswered = false;
    private Core mCore;
    private Dialog dialogDoorOpened;
    private ActivityIncomingDoorConnectedBinding binding;
    private Handler handler;
    private final CoreListenerStub stub = new CoreListenerStub() {
        @Override
        public void onCallStateChanged(@NonNull Core lc, @NonNull Call call, Call.State cstate, @NonNull String message) {
            super.onCallStateChanged(lc, call, cstate, message);
            Logger.d("IncomingDoorConnected.onCallStateChanged state = " + cstate + " | message = " + message + " | callNb = " + lc.getCallsNb());

            if (lc.getCurrentCall() == null || lc.getCallsNb() == 0) {
                handler.removeMessages(REFRESH_CALL);
                handler.removeMessages(CLOSE_CALL);
                handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);

            }

            if (cstate.equals(Call.State.StreamsRunning)) {
                if (!req) {
                    req = true;
                    handler.sendEmptyMessageDelayed(PLAY_RING, 4000);
                }

            }
        }
    };
    private boolean micEnabled = false;
    private DeviceModel dev;
    private String callToName;

    public static IncomingDoorConnected getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityIncomingDoorConnectedBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().removeTimeout();
        handler = new Handler(Looper.getMainLooper(), this);
        instance = this;
        if (getIntent().getExtras() != null) {
            String callFromMac = getIntent().getExtras().getString("mac", "");
            callFromIp = getIntent().getExtras().getString("remote_ip", "");
            callId = getIntent().getExtras().getString("call_id", "");
            String doorRelayCount = getIntent().getExtras().getString("door_relay_count", "1");
            String doorRelayNames1 = getIntent().getExtras().getString("door_relay_name1", "");
            String doorRelayNames2 = getIntent().getExtras().getString("door_relay_name2", "");

            int relayCount = 1;
            try {
                relayCount = (doorRelayCount == null || doorRelayCount.isEmpty()) ? 1 : Integer.parseInt(doorRelayCount);
            } catch (NumberFormatException ignore) {
            }

            if (relayCount == 2) {
                binding.layoutRelay1.setVisibility(View.VISIBLE);
                binding.layoutRelay2.setVisibility(View.VISIBLE);
                binding.imgOpenDoor.setVisibility(View.GONE);
            } else {
                binding.layoutRelay1.setVisibility(View.GONE);
                binding.layoutRelay2.setVisibility(View.GONE);
                binding.imgOpenDoor.setVisibility(View.VISIBLE);
            }
            binding.txtRelay1Name.setText(doorRelayNames1.isEmpty() ? getString(R.string.outdoor1) : doorRelayNames1);
            binding.txtRelay2Name.setText(doorRelayNames2.isEmpty() ? getString(R.string.outdoor2) : doorRelayNames2);
            binding.includeTop.imgBack.setVisibility(View.GONE);
            binding.includeTop.imgOther.setVisibility(View.GONE);
            dev = DeviceController.getInstance().getDoorWithIp(callFromIp);
            if (dev != null) {
                callToName = dev.getName();
            } else {
                dev = DeviceController.getInstance().getDoorWithMac(callFromMac);
                if (dev != null) {
                    callToName = dev.getName();
                } else {
                    callToName = callFromIp;
                }
            }


            binding.includeTop.textTitle.setText(String.format("%s %s", callToName, getString(R.string.calls_calling)));
            binding.imgVolSetting.setVisibility(View.GONE);
            mCore = LinphoneManager.getInstance().getCore();
            if (mCore != null && mCore.getCurrentCall() != null) {
                mCore.addListener(stub);
                LinphoneManager.getInstance().setMicrophoneDisable();
                binding.imgLedOnOff.setImageResource(R.drawable.ic_light_enable);
                binding.imgMicrophone.setClickable(false);
                mCore.setNativeVideoWindowId(binding.remoteVideo);
                mCore.setNativePreviewWindowId(binding.selfVideo);
                LinphoneManager.getInstance().acceptCall(mCore.getCurrentCall());
                handler.sendEmptyMessageDelayed(REFRESH_CALL, 6000);
                LinphoneManager.getInstance().setMicrophoneDisable();
                handler.sendEmptyMessageDelayed(CLOSE_CALL, 60000);
                MyUtils.getInstance().callListener = this;
                callModel = DatabaseHelper.getInstance().getCall(callId);
                callModel.setCallTo(MyUtils.getInstance().getMACAddress());
                callModel.setCallData(MyUtils.getInstance().getShared().getString("DEVICE_NAME", "Room"));
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    binding.fabAnswer.setOnClickListener(IncomingDoorConnected.this);
                    binding.fabClose.setOnClickListener(IncomingDoorConnected.this);
                    binding.imgLedOnOff.setOnClickListener(IncomingDoorConnected.this);
                    binding.imgTakePhoto.setOnClickListener(IncomingDoorConnected.this);
                    binding.imgOpenDoor.setOnClickListener(IncomingDoorConnected.this);
                    binding.layoutRelay1.setOnClickListener(IncomingDoorConnected.this);
                    binding.layoutRelay2.setOnClickListener(IncomingDoorConnected.this);
                    binding.imgVolSetting.setOnClickListener(IncomingDoorConnected.this);
                    binding.imgMicrophone.setOnClickListener(this);


                }, 3000);
            } else {
                handler.removeMessages(CLOSE_CALL);
                handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
            }
        } else {
            handler.removeMessages(CLOSE_CALL);
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
        }


    }


    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCore.removeListener(stub);
        instance = null;
        handler.removeMessages(CLOSE_CALL);
        handler.removeMessages(REMOVE_NEW_CALL_TEXT);
        handler.removeMessages(DECLINE);
        handler.removeMessages(PLAY_RING);
        handler.removeMessages(DOOR_DIALOG_DISMISS);
        handler.removeMessages(TAKE_PHOTO_IMAGE_DEFAULT);
        handler.removeMessages(REFRESH_CALL);
        handler.removeCallbacksAndMessages(null);
        MyUtils.getInstance().callListener = null;
        LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
        mCore.setNativeVideoWindowId(null);
        mCore.setNativePreviewWindowId(null);
        try {
            if (dialogDoorOpened != null) {
                dialogDoorOpened.dismiss();
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));

        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.fabAnswer.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            if (callAnswered) {
                return;
            }
            callAnswered = true;
            GeneralMediaPlayer.getInstance().stopMedia();
            LinphoneManager.getInstance().setMicrophoneEnable();
            handler.removeMessages(PLAY_RING);
            callModel.setCallState(AppEnums.Answered.name());
            String data = new Gson().toJson(callModel);
            new TCPSenderThread(callFromIp, "CALL&&" + data).start();
            LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.START_AUDIO.name());
            handler.removeMessages(CLOSE_CALL);
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 8000);
            binding.imgVolSetting.setVisibility(View.VISIBLE);


            micEnabled = true;
            binding.imgMicrophone.setClickable(true);
            binding.imgMicrophone.setImageResource(R.drawable.ic_microphone_enable);

        } else if (id == binding.fabClose.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            handler.removeMessages(PLAY_RING);
            handler.removeMessages(DECLINE);
            handler.sendEmptyMessageDelayed(DECLINE, 1000);
        } else if (id == binding.imgOpenDoor.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000, false);
            binding.imgOpenDoor.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.OPEN_DOOR.name());
            doorOpenDialogShow();
        } else if (id == binding.layoutRelay1.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000, false);
            binding.imgOpenDoor1.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.OPEN_DOOR.name());
            doorOpenDialogShow();
        } else if (id == binding.layoutRelay2.getId()) {
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay2, 3000);
            MyUtils.getInstance().setViewClickTimeout(binding.layoutRelay1, 3000, false);
            MyUtils.getInstance().setViewClickTimeout(binding.imgOpenDoor, 3000, false);
            binding.imgOpenDoor2.setImageResource(R.drawable.ic_key_enable);
            LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.OPEN_DOOR2.name());
            doorOpenDialogShow();
        } else if (id == binding.imgLedOnOff.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            handler.removeMessages(PLAY_RING);
            ledStatus = !ledStatus;
            ImageView imv = (ImageView) v;
            if (ledStatus) {
                LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.LED_ON.name());
                imv.setImageResource(R.drawable.ic_light_enable);
            } else {
                LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.LED_OFF.name());
                imv.setImageResource(R.drawable.ic_light_disable);
            }
        } else if (id == binding.imgTakePhoto.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 2000);
            if (mCore.getCurrentCall() == null) {
                return;
            }
            handler.removeMessages(PLAY_RING);
            binding.imgTakePhoto.setImageResource(R.drawable.ic_camera_enable);

            String safeMac = (dev != null && dev.getMac() != null) ? dev.getMac().replace(":", "") : "unknown";

            String pathSnapshot =
                    MyUtils.getInstance().getCallSnapShotsDir()
                            + "/"
                            + safeMac       // Temizlenmiş MAC adresini kullanıyoruz
                            + "_"           // '%' yerine '_' kullanıyoruz
                            + System.currentTimeMillis()
                            + ".jpeg";




            /*String pathSnapshot =
                    MyUtils.getInstance().getCallSnapShotsDir()
                            + "/"
                            + dev.getMac()
                            + "%"
                            + System.currentTimeMillis()
                            + ".jpeg";*/


            mCore.getCurrentCall().takeVideoSnapshot(pathSnapshot);
            GeneralMediaPlayer.getInstance().playMedia(3);
            DatabaseHelper.getInstance().insertImage(new ImagesModel(pathSnapshot, String.valueOf(System.currentTimeMillis()), dev.getMac()));
            handler.sendEmptyMessageDelayed(TAKE_PHOTO_IMAGE_DEFAULT, 2000);
        } else if (id == binding.imgVolSetting.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            Intent i = new Intent(IncomingDoorConnected.this, SetStreamVolumeActivity.class);
            i.putExtra("fromRoom", false);
            i.putExtra("remoteIp", callFromIp);
            startActivity(new Intent(i));
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

    private void doorOpenDialogShow() {
        if (dialogDoorOpened != null && dialogDoorOpened.isShowing()) {
            dialogDoorOpened.dismiss();
        }
        GeneralMediaPlayer.getInstance().playMedia(4);
        dialogDoorOpened = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_door_opened);
        dialogDoorOpened.show();
        handler.sendEmptyMessageDelayed(DOOR_DIALOG_DISMISS, 3000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(IncomingDoorConnected.this);
    }


    private void setState(final CallModel callModel) {
        if (callModel == null) {
            return;
        }
        if (callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress()) && callModel.getCallId().equals(callId) && callModel.getCallState().equals("Answered")) {
            handler.removeMessages(CLOSE_CALL);
            LinphoneManager.getInstance().setMicrophoneEnable();
            LinphoneManager.getInstance().sendLocalSipMessage(callFromIp, AppEnums.START_AUDIO.name());
            runOnUiThread(() -> {
                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{getColor(R.color.gray_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                binding.fabAnswer.setBackgroundTintList(colorStateList);
                binding.includeTop.textTitle.setText(String.format("%s %s", callToName, getString(R.string.in_call)));
                binding.includeTop.textTitle.setTextColor(getColor(R.color.green_new));
            });
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 180000);

        } else if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress()) && callModel.getCallId().equals(callId)) {
            handler.removeMessages(CLOSE_CALL);
            handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
        } else if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress()) && !callModel.getCallId().equals(callId)) {
            String device;
            if (callModel.getCallType().startsWith(AppEnums.DOOR.name())) {
                device = AppEnums.DOOR.name();
            } else {
                device = AppEnums.ROOM.name();
            }
            showSecondCall(callModel, device);
        }
    }

    private void showSecondCall(final CallModel callModel, String device) {
        if (callModel == null) {
            return;
        }
        DeviceModel dev;
        if (device.equals(AppEnums.DOOR.name())) {
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
                handler.removeMessages(REMOVE_NEW_CALL_TEXT);
                handler.removeMessages(DECLINE);
                MyUtils.getInstance().callListener = null;
                instance = null;
                LinphoneManager.getInstance().getCore().removeListener(stub);
                LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
                GeneralMediaPlayer.getInstance().stopMedia();
                MyUtils.getInstance().incomingCall = true;
                MyUtils.getInstance().backToRootActivity();
                IncomingDoorConnected.this.finish();
                break;

            case REMOVE_NEW_CALL_TEXT:
                binding.textNewCall.setVisibility(View.GONE);
                break;

            case DECLINE:
                handler.removeMessages(CLOSE_CALL);
                callModel.setCallState(AppEnums.Declined.name());
                String data = new Gson().toJson(callModel);
                new TCPSenderThread(callFromIp, "CALL&&" + data).start();
                if (!callAnswered) {
                    try {
                        CallModel callModelX = DatabaseHelper.getInstance().getCall(callModel.getCallId());
                        if (callModelX.getCallImgPath() != null) {
                            callModel.setCallImgPath(callModelX.getCallImgPath());
                        }
                        callModel.setCallReadState(true);
                        DatabaseHelper.getInstance().updateCall(callModel);
                    } catch (Exception e) {
                        Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                    }
                }
                handler.sendEmptyMessageDelayed(CLOSE_CALL, 10);
                break;

            case PLAY_RING:
                GeneralMediaPlayer.getInstance().playMedia(1);
                break;

            case TAKE_PHOTO_IMAGE_DEFAULT:
                binding.imgTakePhoto.setImageResource(R.drawable.ic_camera_disable);
                break;

            case DOOR_DIALOG_DISMISS:
                binding.imgOpenDoor1.setImageResource(R.drawable.ic_key_disable);
                binding.imgOpenDoor.setImageResource(R.drawable.ic_key_disable);
                binding.imgOpenDoor2.setImageResource(R.drawable.ic_key_disable);
                if (dialogDoorOpened != null) {
                    dialogDoorOpened.dismiss();
                }
                MyUtils.getInstance().hideNavigation(IncomingDoorConnected.this);
                break;

            case REFRESH_CALL:
                Core core = LinphoneManager.getInstance().getCore();
                if (core != null) {
                    Call call = core.getCurrentCall();
                    if (call != null) {
                        CallParams par = mCore.createCallParams(call);
                        if (par != null) {
                            call.update(par);
                        }

                    }
                }
                break;
            default:
                break;
        }
        return false;
    }
}
