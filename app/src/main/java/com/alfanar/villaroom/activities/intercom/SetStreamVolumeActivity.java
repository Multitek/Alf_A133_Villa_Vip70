package com.alfanar.villaroom.activities.intercom;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.databinding.ActivitySetStreamVolumeBinding;
import com.alfanar.villaroom.sockets.TCPSenderThread;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.linphone.AndroidAudioManager;
import org.linphone.LinphoneManager;
import org.linphone.core.Call;

public class SetStreamVolumeActivity extends AppCompatActivity {

    public static SetStreamVolumeActivity instance;
    ActivitySetStreamVolumeBinding binding;
    private String doorIp = "10.10.10.10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetStreamVolumeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MyUtils.getInstance().hideNavigation(this);


        if (getIntent().getExtras() != null) {
            boolean fromRoom = getIntent().getExtras().getBoolean("fromRoom", false);
            if (fromRoom) {
                binding.ln1.setVisibility(View.GONE);
                binding.ln2.setVisibility(View.GONE);
                binding.ln3.setVisibility(View.GONE);
            }
            doorIp = getIntent().getExtras().getString("remoteIp", "10.10.10.10");

        }


        binding.imgBack.setOnClickListener(v -> SetStreamVolumeActivity.this.finish());



        binding.imgMinusDoor.setOnClickListener(v -> {
            int val = Integer.parseInt(binding.txtValDoor.getText().toString());
            if (val > 1) {
                Call call = LinphoneManager.getInstance().getCore().getCurrentCall();
                if (call != null) {
                    binding.txtValDoor.setText(String.valueOf(val - 1));
                    Logger.d("call_ip = " + doorIp);
                    LinphoneManager.getInstance().sendLocalSipMessage(doorIp, AppEnums.SET_STREAM_VOLUME.name() + "#" + (val - 1));
                } else {
                    SetStreamVolumeActivity.this.finish();
                }

            }
        });



        binding.imgPlusDoor.setOnClickListener(v -> {
            int val = Integer.parseInt(binding.txtValDoor.getText().toString());
            if (val < 5) {
                Call call = LinphoneManager.getInstance().getCore().getCurrentCall();
                if (call != null) {
                    binding.txtValDoor.setText(String.valueOf(val + 1));
                    Logger.d("call_ip = " + doorIp);
                    LinphoneManager.getInstance().sendLocalSipMessage(doorIp, AppEnums.SET_STREAM_VOLUME.name() + "#" + (val + 1));
                } else {
                    SetStreamVolumeActivity.this.finish();
                }
            }
        });






        binding.imgMinusRoom.setOnClickListener(v -> {
            int val = Integer.parseInt(binding.txtValRoom.getText().toString());
            if (val > 1) {
                Call call = LinphoneManager.getInstance().getCore().getCurrentCall();
                if (call != null) {
                    binding.txtValRoom.setText(String.valueOf(val - 1));
                    AndroidAudioManager.getInstance().setCallStreamVolume(val - 1);
                } else {
                    SetStreamVolumeActivity.this.finish();
                }

            }
        });

        binding.imgPlusRoom.setOnClickListener(v -> {
            int val = Integer.parseInt(binding.txtValRoom.getText().toString());
            if (val < 5) {
                Call call = LinphoneManager.getInstance().getCore().getCurrentCall();
                if (call != null) {
                    binding.txtValRoom.setText(String.valueOf(val + 1));
                    AndroidAudioManager.getInstance().setCallStreamVolume(val + 1);
                } else {
                    SetStreamVolumeActivity.this.finish();
                }
            }
        });


        instance = this;

        if (!doorIp.isEmpty()) {
            new TCPSenderThread(doorIp, "GET_DOOR_SPEAKER_VOL").start();
        } else {
            setDoorVol("4");
        }

        binding.txtValRoom.setText(AndroidAudioManager.getInstance().getStreamVolume());

    }

    public void setDoorVol(String vol) {
        Logger.d("setDoorVol = " + vol);
        new Handler(Looper.getMainLooper()).post(() -> binding.txtValDoor.setText(vol));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}