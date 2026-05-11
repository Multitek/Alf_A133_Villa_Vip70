package com.alfanar.villaroom.activities.intercom;

import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityIncomingDoorParalelRingingBinding;
import com.alfanar.villaroom.interfaces.CallListener;
import com.alfanar.villaroom.interfaces.FileListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.sockets.TCPSenderThread;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.linphone.LinphoneManager;

import java.text.MessageFormat;
import java.util.ArrayList;

public class IncomingDoorParallelRinging extends AppCompatActivity implements OnClickListener, CallListener, FileListener, Handler.Callback {

    public static IncomingDoorParallelRinging instance;
    private String callerIP = "null";
    private String callId = "null";
    private CallModel callModel;
    private boolean call_closed = false;

    private boolean callChange = false;

    private ActivityIncomingDoorParalelRingingBinding binding;
    private Handler mHandler;
    private String callToName = "";

    public static IncomingDoorParallelRinging getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityIncomingDoorParalelRingingBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().removeTimeout();
        mHandler = new Handler(Looper.getMainLooper(), this);

        GeneralMediaPlayer.getInstance().playMedia(1);
        if (getIntent().getExtras() != null) {
            callId = getIntent().getExtras().getString("call_id", "null");
            callerIP = getIntent().getExtras().getString("remote_ip", "null");
        }


        callModel = DatabaseHelper.getInstance().getCall(callId);

        if (callerIP == null || callerIP.equals("null")) {
            String callFrom = callModel.getCallFrom();
            ArrayList<DeviceModel> doors = DeviceController.getInstance().getDoorsList();
            for (DeviceModel model : doors) {
                if (model.getMac().equals(callFrom)) {
                    callerIP = model.getIp();
                }
            }
        }

        binding.fabAnswer.setOnClickListener(this);
        binding.fabClose.setOnClickListener(this);

        binding.includeTop.imgBack.setVisibility(View.GONE);
        binding.includeTop.imgOther.setVisibility(View.GONE);


        mHandler.sendEmptyMessageDelayed(100, 60000);

        LinphoneManager.getInstance().setMicrophoneDisable();

        MyUtils.getInstance().callListener = this;
        MyUtils.getInstance().fileListener = this;
        instance = this;

        DeviceModel dev = DeviceController.getInstance().getDoorWithIp(callerIP);
        if (dev != null) {
            callToName = dev.getName();
            binding.includeTop.textTitle.setText(dev.getName());
        } else {
            binding.includeTop.textTitle.setText(callerIP);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!callChange) {
            MyUtils.getInstance().callListener = null;
        }
        MyUtils.getInstance().fileListener = null;
        instance = null;
        mHandler.removeMessages(100);
        mHandler.removeMessages(101);
        mHandler.removeMessages(102);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.fabAnswer.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            GeneralMediaPlayer.getInstance().stopMedia();
            new TCPSenderThread(callerIP, "CALL&&" + getModel("Answered")).start();
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 8000);
        } else if (id == binding.fabClose.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 500);
            if (call_closed) {
                return;
            }
            mHandler.removeMessages(102);
            mHandler.sendEmptyMessageDelayed(102, 1000);
            call_closed = true;
        }
    }

    public String getModel(String state) {
        callModel.setCallState(state);
        callModel.setCallTo(MyUtils.getInstance().getMACAddress());
        callModel.setCallData(MyUtils.getInstance().getShared().getString("DEVICE_NAME", getResources().getString(R.string.item_room)));
        return new Gson().toJson(callModel);
    }

    private void setState(final CallModel callModel) {
        if (callModel == null) {
            return;
        }
        if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress()) && !callModel.getCallId().equals(callId)) {
            String device;
            if (callModel.getCallType().startsWith("DOOR")) {
                device = "DOOR";
            } else {
                device = "ROOM";
            }
            showSecondCall(callModel, device);
        } else if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress()) && callModel.getCallId().equals(callId)) {
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, 10);
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(IncomingDoorParallelRinging.this);
    }

    @Override
    public void incomingDoorCall(CallModel callModel, String callerDoorIp) {
        callChange = true;
        mHandler.removeMessages(100);
        MyUtils.getInstance().callListener = null;
        instance = null;
        GeneralMediaPlayer.getInstance().stopMedia();
        Intent intent = new Intent(IncomingDoorParallelRinging.this, IncomingDoorConnected.class);
        intent.putExtra("remote_ip", callerDoorIp);
        intent.putExtra("call_id", callModel.getCallId());
        startActivity(intent);
        IncomingDoorParallelRinging.this.finish();

    }

    @Override
    public void incomingRoomCall(final CallModel callModel) {
        runOnUiThread(() -> showSecondCall(callModel, "ROOM"));
    }

    @Override
    public void startParallelConnected(String remoteIP, String doorCount, String relay1Name, String relay2Name) {
        callChange = true;
        mHandler.removeMessages(100);
        MyUtils.getInstance().callListener = null;
        instance = null;
        GeneralMediaPlayer.getInstance().stopMedia();
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    CallModel callModelX = DatabaseHelper.getInstance().getCall(callModel.getCallId());
                    if (callModelX.getCallImgPath() != null) {
                        callModel.setCallImgPath(callModelX.getCallImgPath());
                    }
                    callModel.setCallReadState(true);
                    DatabaseHelper.getInstance().updateCall(callModel);
                } catch (SQLException e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();
        MyUtils.getInstance().incomingCall = true;
        Intent intent = new Intent(IncomingDoorParallelRinging.this, IncomingDoorParallelConnected.class);
        intent.putExtra("remote_ip", remoteIP);
        intent.putExtra("call_name", callToName);
        intent.putExtra("door_relay_count", doorCount);
        intent.putExtra("door_relay_name1", relay1Name);
        intent.putExtra("door_relay_name2", relay2Name);
        startActivity(intent);
        IncomingDoorParallelRinging.this.finish();

    }

    @Override
    public void callStateChanged(final CallModel callModel) {
        runOnUiThread(() -> setState(callModel));
    }

    @Override
    public void loadImage() {
        runOnUiThread(() -> {
            try {
                Glide.with(IncomingDoorParallelRinging.this).load(callModel.getCallImgPath()).fitCenter().into(binding.imgDoor);
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        });
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == 100) {

            mHandler.removeMessages(102);
            MyUtils.getInstance().callListener = null;
            instance = null;
            GeneralMediaPlayer.getInstance().stopMedia();
            MyUtils.getInstance().incomingCall = true;
            MyUtils.getInstance().backToRootActivity();
            IncomingDoorParallelRinging.this.finish();
        } else if (msg.what == 101) {
            binding.textNewCall.setVisibility(View.GONE);
        } else if (msg.what == 102) {
            mHandler.removeMessages(100);
            new TCPSenderThread(callerIP, "CALL&&" + getModel("Declined")).start();
            try {

                CallModel callModelX = DatabaseHelper.getInstance().getCall(callModel.getCallId());
                if (callModelX.getCallImgPath() != null) {
                    callModel.setCallImgPath(callModelX.getCallImgPath());
                }
                callModel.setCallReadState(true);
                DatabaseHelper.getInstance().updateCall(callModel);
            } catch (SQLException e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
            mHandler.sendEmptyMessageDelayed(100, 10);
        }
        return false;
    }
}
