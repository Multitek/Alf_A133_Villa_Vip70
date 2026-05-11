package com.alfanar.villaroom.activities.intercom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.SQLException;
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
import com.alfanar.villaroom.databinding.ActivityIncomingRoomRingingBinding;
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

public class IncomingRoomRinging extends AppCompatActivity implements View.OnClickListener, CallListener, Handler.Callback {
    @SuppressLint("StaticFieldLeak")
    private static IncomingRoomRinging instance;
    ActivityIncomingRoomRingingBinding binding;
    private String remote_ip, callId = "null";
    private Core mCore;
    private String callState = "Missed";
    private Handler mHandler;
    private final CoreListenerStub stub = new CoreListenerStub() {
        @Override
        public void onCallStateChanged(@NonNull Core lc, @NonNull Call call, Call.State cstate, @NonNull String message) {
            super.onCallStateChanged(lc, call, cstate, message);
            Logger.d("IncomingRoomRinging.onCallStateChanged state = " + cstate + " | message = " + message + " | callNb = " + lc.getCallsNb());
            if (lc.getCurrentCall() == null || lc.getCallsNb() == 0) {
                mHandler.removeMessages(100);
                mHandler.sendEmptyMessageDelayed(100, 10);
            }
        }
    };

    public static IncomingRoomRinging getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityIncomingRoomRingingBinding.inflate(getLayoutInflater());
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
            callId = getIntent().getExtras().getString("call_id", "null");
        }


        binding.fabAnswer.setOnClickListener(this);
        binding.fabClose.setOnClickListener(this);


        binding.includeTop.imgBack.setVisibility(View.GONE);
        binding.includeTop.imgOther.setVisibility(View.GONE);

        LinphoneManager.getInstance().setMicrophoneDisable();

        binding.txtCallName.setText(getResources().getString(R.string.calls_incoming));


        mCore.addListener(stub);
        MyUtils.getInstance().callListener = this;
        mHandler.sendEmptyMessageDelayed(100, 60000);
        instance = this;
        DeviceModel dev = DeviceController.getInstance().getRoomWithIp(remote_ip);
        if (dev != null) {
            binding.includeTop.textTitle.setText(dev.getName());
        } else {
            binding.includeTop.textTitle.setText(getString(R.string.item_room));
        }

        GeneralMediaPlayer.getInstance().playMedia(2);
    }

    private void setDatabase() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    CallModel callModel = DatabaseHelper.getInstance().getCall(callId);
                    if (callState.equals("Answered")) {
                        callModel.setCallData("Connected");
                        callModel.setCallReadState(true);
                    } else if (callState.equals("Declined")) {
                        callModel.setCallData("Not connected");
                        callModel.setCallReadState(true);
                    } else {
                        callModel.setCallData("Missed");
                    }

                    DatabaseHelper.getInstance().updateCall(callModel);

                } catch (SQLException e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                } finally {
                    if (MyUtils.getInstance().historyListener != null) {
                        MyUtils.getInstance().historyListener.refreshCalls();
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCore.removeListener(stub);
        setDatabase();
        mHandler.removeMessages(100);
        mHandler.removeMessages(101);
        mHandler.removeCallbacksAndMessages(null);
        instance = null;
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
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            callState = "Declined";
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 10);
        } else if (id == binding.fabAnswer.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            callState = "Answered";
            mHandler.removeMessages(100);
            mHandler.removeMessages(101);
            GeneralMediaPlayer.getInstance().stopMedia();
            instance = null;
            MyUtils.getInstance().callListener = null;
            mCore.removeListener(stub);
            MyUtils.getInstance().incomingCall = true;
            LinphoneManager.getInstance().acceptCall(mCore.getCurrentCall());
            Intent i = new Intent(this, IncomingRoomConnected.class);
            i.putExtra("remote_ip", remote_ip);
            startActivity(i);
            IncomingRoomRinging.this.finish();
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
        MyUtils.getInstance().hideNavigation(IncomingRoomRinging.this);
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
                MyUtils.getInstance().incomingCall = true;
                instance = null;
                GeneralMediaPlayer.getInstance().stopMedia();
                MyUtils.getInstance().backToRootActivity();
                IncomingRoomRinging.this.finish();
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
