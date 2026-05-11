package com.alfanar.villaroom.activities.intercom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityIncomingRoomConnectedBinding;
import com.alfanar.villaroom.interfaces.CallListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.linphone.AndroidAudioManager;
import org.linphone.LinphoneManager;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import java.text.MessageFormat;

public class IncomingRoomConnected extends AppCompatActivity implements View.OnClickListener, CallListener, Handler.Callback {


    private static IncomingRoomConnected instance;
    ActivityIncomingRoomConnectedBinding binding;
    String remote_ip = "null";
    private Core mCore;
    private Handler mHandler;
    private final CoreListenerStub stub = new CoreListenerStub() {
        @Override
        public void onCallStateChanged(@NonNull Core lc, @NonNull Call call, Call.State cstate, @NonNull String message) {
            super.onCallStateChanged(lc, call, cstate, message);
            Logger.d("IncomingRoomConnected.onCallStateChanged state = " + cstate + " | message = " + message + " | callNb = " + lc.getCallsNb());
            if (lc.getCurrentCall() == null || lc.getCallsNb() == 0) {
                mHandler.removeMessages(100);
                mHandler.sendEmptyMessageDelayed(100, 10);
            }
        }
    };
    private boolean micEnabled = true;


    public static IncomingRoomConnected getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityIncomingRoomConnectedBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().removeTimeout();
        mCore = LinphoneManager.getInstance().getCore();
        mHandler = new Handler(Looper.getMainLooper(), this);

        if (getIntent().getExtras() != null) {
            remote_ip = getIntent().getExtras().getString("remote_ip", "null");
        }


        binding.fabClose.setOnClickListener(this);
        binding.imgMicrophone.setOnClickListener(this);
        binding.includeTop.imgBack.setVisibility(View.GONE);
        binding.includeTop.imgOther.setVisibility(View.GONE);
        binding.txtCallName.setText(getResources().getString(R.string.in_call));

        mHandler.sendEmptyMessageDelayed(100, 180000);

        LinphoneManager.getInstance().setMicrophoneEnable();
        mCore.addListener(stub);
        MyUtils.getInstance().callListener = this;
        instance = this;
        DeviceModel dev = DeviceController.getInstance().getRoomWithIp(remote_ip);
        if (dev != null) {
            binding.includeTop.textTitle.setText(dev.getName());
        } else {
            binding.includeTop.textTitle.setText(remote_ip);
        }

        binding.imgVolSetting.setOnClickListener(v -> {
            if (LinphoneManager.getInstance().getCore().getCurrentCall() != null) {
                Intent i = new Intent(IncomingRoomConnected.this, SetStreamVolumeActivity.class);
                i.putExtra("fromRoom", true);
                i.putExtra("remoteIp", remote_ip);
                startActivity(new Intent(i));
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCore.removeListener(stub);
        MainTimeout.getInstance().removeTimeout();
        mHandler.removeMessages(100);
        mHandler.removeMessages(101);
        mHandler.removeCallbacksAndMessages(null);
        instance = null;
        MyUtils.getInstance().callListener = null;
        LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.fabClose.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 10);
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

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(IncomingRoomConnected.this);
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
                switch (callModel.getCallState()) {
                    case "Answered":
                        binding.textNewCall.setText(String.format(getResources().getString(R.string.call_answered), callModel.getCallData(), finalDev.getName()));
                        break;
                    case "Declined":
                        binding.textNewCall.setText(String.format(getResources().getString(R.string.call_declined), callModel.getCallData(), finalDev.getName()));
                        break;
                    case "Missed":
                        binding.textNewCall.setText(String.format(getResources().getString(R.string.call_missed), finalDev.getName()));
                        break;
                    default:
                        binding.textNewCall.setText(MessageFormat.format("{0} {1}", finalDev.getName(), getResources().getString(R.string.calls_calling)));
                        break;
                }
            });
            mHandler.removeMessages(101);
            mHandler.sendEmptyMessageDelayed(101, 3000);
        }
    }

    @Override
    public void incomingDoorCall(final CallModel callModel, String callerDoorIp) {
        runOnUiThread(() -> showSecondCall(callModel, "DOOR"));
    }

    @Override
    public void incomingRoomCall(final CallModel callModel) {
        runOnUiThread(() -> showSecondCall(callModel, "ROOM"));
    }

    @Override
    public void startParallelConnected(String remoteIP, String doorCount, String relay1Name, String relay2Name) {
    }

    @Override
    public void callStateChanged(final CallModel callModel) {
        runOnUiThread(() -> {
            String device;
            if (callModel != null && callModel.getCallType().startsWith("DOOR")) {
                device = "DOOR";
            } else {
                device = "ROOM";
            }
            showSecondCall(callModel, device);
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 100:
                MyUtils.getInstance().callListener = null;
                instance = null;
                mCore.removeListener(stub);
                LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
                GeneralMediaPlayer.getInstance().stopMedia();
                MyUtils.getInstance().backToRootActivity();
                IncomingRoomConnected.this.finish();
                break;
            case 101:
                runOnUiThread(() -> binding.textNewCall.setVisibility(View.GONE));
                break;

            default:
                break;
        }
        return false;
    }
}
