package com.alfanar.villaroom.activities.intercom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityOutGoingRoomCallingBinding;
import com.alfanar.villaroom.interfaces.CallListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.linphone.LinphoneManager;
import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;

import java.text.MessageFormat;

public class OutGoingRoomCalling extends AppCompatActivity implements View.OnClickListener, CallListener, Handler.Callback {

    public static String callId;
    @SuppressLint("StaticFieldLeak")
    private static OutGoingRoomCalling instance;
    ActivityOutGoingRoomCallingBinding binding;
    Handler mHandler;    private final CoreListenerStub stub = new CoreListenerStub() {
        @Override
        public void onCallStateChanged(@NonNull Core lc, @NonNull Call call, Call.State state, @NonNull String message) {
            super.onCallStateChanged(lc, call, state, message);

            Logger.d("OutGoingRoomCalling.onCallStateChanged state = " + state + " | message = " + message + " | callNb = " + lc.getCallsNb());
            if (callCurrent == null) {
                callCurrent = call;
            }
            if (lc.getCurrentCall() == null || lc.getCallsNb() == 0) {
                Logger.d("OutGoingRoomCalling.null0: " + message);
                if (message.toLowerCase().startsWith("busy")) {
                    lc.removeListener(stub);
                    call.terminate();
                    runOnUiThread(() -> binding.txtCallName.setText(R.string.device_busy));

                    mHandler.removeMessages(100);
                    mHandler.sendEmptyMessageDelayed(100, 5000);
                } else {
                    mHandler.removeMessages(100);
                    mHandler.sendEmptyMessageDelayed(100, 10);
                }
            } else if (state.equals(Call.State.StreamsRunning)) {
                mHandler.removeMessages(100);
                mHandler.removeMessages(101);
                MyUtils.getInstance().callListener = null;
                mCore.removeListener(stub);
                running = true;
                Intent i = new Intent(OutGoingRoomCalling.this, OutGoingRoomConnected.class);
                i.putExtra("remote_ip", remote_ip);
                startActivity(i);
                instance = null;
                OutGoingRoomCalling.this.finish();
            } else if (state == Call.State.End || state == Call.State.Error) {
                Logger.d("OutGoingRoomCalling.state: " + state);
                if (message.startsWith("Busy")) {
                    Logger.d("OutGoingRoomCalling.message: " + message);
                    runOnUiThread(() -> {
                        binding.textNewCall.setVisibility(View.VISIBLE);
                        binding.textNewCall.setText(getResources().getString(R.string.device_busy));
                        Logger.d("OutGoingRoomCalling: device is busy");
                    });
                    mHandler.removeMessages(100);
                    mHandler.sendEmptyMessageDelayed(100, 2500);
                }
            }
        }
    };
    private String remote_ip = "null";
    private boolean running = false;
    private Core mCore;
    private Call callCurrent;

    public static OutGoingRoomCalling getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityOutGoingRoomCallingBinding.inflate(getLayoutInflater());
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

        binding.includeTop.imgBack.setVisibility(View.GONE);
        binding.includeTop.imgOther.setVisibility(View.GONE);
        binding.fabClose.setOnClickListener(this);
        LinphoneManager.getInstance().setMicrophoneDisable();


        mCore.addListener(stub);
        LinphoneManager.getInstance().connectRoom(remote_ip);
        MyUtils.getInstance().callListener = this;
        mHandler.sendEmptyMessageDelayed(100, 60000);
        instance = this;

        DeviceModel dev = DeviceController.getInstance().getRoomWithIp(remote_ip);
        if (dev != null) {
            binding.includeTop.textTitle.setText(dev.getName());
        } else {
            binding.includeTop.textTitle.setText(getString(R.string.item_room));
        }
    }

    private void setDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    CallModel callModel = DatabaseHelper.getInstance().getCall(callId);
                    Logger.d("OutGoingRoomCalling.remote_ip: " + remote_ip);

                    if (running) {
                        callModel.setCallData("Connected");
                    } else {
                        callModel.setCallData("Not connected");
                    }
                    callModel.setCallReadState(true);
                    DatabaseHelper.getInstance().updateCall(callModel);

                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCore.removeListener(stub);
        setDatabase();
        GeneralMediaPlayer.getInstance().stopMedia();
        instance = null;
        mHandler.removeMessages(100);
        mHandler.removeMessages(101);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.fabClose.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 10);
            mCore.terminateAllCalls();
        }
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
                mCore.removeListener(stub);
                LinphoneManager.getInstance().terminateCurrentCallOrConferenceOrAll();
                MyUtils.getInstance().callListener = null;
                instance = null;
                GeneralMediaPlayer.getInstance().stopMedia();
                MyUtils.getInstance().backToRootActivity();
                OutGoingRoomCalling.this.finish();
                break;
            case 101:
                binding.textNewCall.setVisibility(View.GONE);
                break;

            default:
                break;
        }
        return false;
    }


}
