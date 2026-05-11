package com.alfanar.villaroom.activities.intercom;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityIndoorRingBinding;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.MyUtils;

public class IndoorRingActivity extends AppCompatActivity implements Handler.Callback {
    public static IndoorRingActivity instance;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().darkTheme ? R.style.Transparent_TransparentDark : R.style.Transparent_TransparentLight);
        super.onCreate(savedInstanceState);
        ActivityIndoorRingBinding binding = ActivityIndoorRingBinding.inflate(getLayoutInflater());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);


        setContentView(binding.getRoot());
        SharedPreferences sp = MyUtils.getInstance().getShared();
        MyUtils.getInstance().hideNavigation(this);

        int duration = (sp.getInt("ringDuration", 8) * 1000) + 1000;

        binding.btnOk.setOnClickListener(v -> {
            handler.removeMessages(1000);
            handler.sendEmptyMessageDelayed(1000, 5);
        });
        handler = new Handler(Looper.getMainLooper(), this);
        handler.sendEmptyMessageDelayed(1000, duration);

        instance = this;

        GeneralMediaPlayer.getInstance().playMedia(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        handler.removeMessages(1000);
        GeneralMediaPlayer.getInstance().stopMedia();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == 1000) {
            GeneralMediaPlayer.getInstance().stopMedia();
            instance = null;
            IndoorRingActivity.this.finish();
        }
        return false;
    }
}