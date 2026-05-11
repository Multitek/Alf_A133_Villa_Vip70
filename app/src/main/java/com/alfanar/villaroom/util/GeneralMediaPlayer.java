package com.alfanar.villaroom.util;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.activities.MainActivity;

public class GeneralMediaPlayer {
    private static GeneralMediaPlayer instance;
    private MediaPlayer generalMp;

    public static GeneralMediaPlayer getInstance() {
        if (instance == null) {
            instance = new GeneralMediaPlayer();
        }
        return instance;
    }

    public void playRing(int type) {
        try {
            stopMedia();
            SharedPreferences devInfoSp = MyUtils.getInstance().getShared();
            Uri uri = null;
            int media = 0;
            if (type == 1) { // door
                media = devInfoSp.getInt("ringDoorIndex", 0);
            } else if (type == 2) { // room
                media = devInfoSp.getInt("ringRoomIndex", 0);
            }

            if (media == 0) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone1");
            } else if (media == 1) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone2");
            } else if (media == 2) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone3");
            } else if (media == 3) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone4");
            } else if (media == 4) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone5");
            } else if (media == 5) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone6");
            } else {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone1");
            }

            mHandler.sendEmptyMessageDelayed(666, 5000);




            int volIndex = devInfoSp.getInt("volIndex", 7); // progress değeri (0–15 arası)
            float ratio = 0.80f;

            float normalized = volIndex / 15f;
            if (normalized > ratio) {
                normalized = ratio;
            }
            float vol = normalized;

            if(volIndex==15){
                ratio = 0.80f;
            }


            generalMp = new MediaPlayer();
            generalMp.setAudioStreamType(AudioManager.STREAM_MUSIC); //0-15
            generalMp.setDataSource(App.getInstance(), uri);
            generalMp.setLooping(true);
            generalMp.prepare();

            generalMp.setOnPreparedListener(mp -> {
                mp.setVolume(vol, vol);
                mp.start();
            });

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }


    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 666) {
                stopMedia();
            }
        }
    };

    public void playMedia(int typex) {
        stopMedia();
        SharedPreferences devInfoSp = MyUtils.getInstance().getShared();
        if (devInfoSp.getBoolean("DISTURB", false)) {
            Log.d("GeneralMediaPlayer", "playMedia silent mode enable, ringtone disabled");
            return;
        }

        int media, duration;
        if (typex == 1) { // door
            media = devInfoSp.getInt("ringDoorIndex", 0);
            duration = devInfoSp.getInt("ringDuration", 8) * 1000;
        } else if (typex == 2) { // room
            media = devInfoSp.getInt("ringRoomIndex", 0);
            duration = devInfoSp.getInt("ringDuration", 8) * 1000;
        } else if (typex == 3) { // camera
            media = 6;
            duration = 1500;
        } else if (typex == 4) { // door_open
            media = 7;
            duration = 1500;
        }/* else if (typex == 7) { // notification
            media = 10;
            duration = 1000;
        }*/ else if (typex == 8) { // mac
            media = 8;
            duration = 5000;
        } else {
            return;
        }


       /* float maxVolume = 7;
        float volIndex = devInfoSp.getInt("volIndex", 7) / Float.parseFloat("2");
        float vol = volIndex / maxVolume;*/

        int volIndex = devInfoSp.getInt("volIndex", 7);

       /* SharedPreferences sp = App.getInstance().getSharedPreferences("DEVICE_SETTINGS", MODE_PRIVATE);
        int volumeDevice = sp.getInt("device_volume", 0);
        float ratio;
        if(volumeDevice==1){
            ratio = 0.71f;
        }else  if(volumeDevice==2){
            ratio = 0.72f;
        }else  if(volumeDevice==3){
            ratio = 0.73f;
        }else  if(volumeDevice==4){
            ratio = 0.74f;
        }else  if(volumeDevice==5){
            ratio = 0.75f;
        }else  if(volumeDevice==6){
            ratio = 0.76f;
        }else  if(volumeDevice==7){
            ratio = 0.77f;
        }else  if(volumeDevice==8){
            ratio = 0.78f;
        }else  if(volumeDevice==9){
            ratio = 0.79f;
        }else{
            ratio = 0.7f;
        }*/

        float ratio = 0.76f;

        float normalized = volIndex / 15f;
        if (normalized > ratio) {
            normalized = ratio;
        }
        float vol = normalized; // 0.0 - 0.7 arası

        Log.d("GeneralMediaPlayer", "playMedia type= " + typex + " - duration= " + duration + " - media= " + media);
        mHandler.sendEmptyMessageDelayed(666, duration);
        try {
            generalMp = new MediaPlayer();
            generalMp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Uri uri = null;
            if (media == 0) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone1");
            } else if (media == 1) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone2");
            } else if (media == 2) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone3");
            } else if (media == 3) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone4");
            } else if (media == 4) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone5");
            } else if (media == 5) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone6");
            } else if (media == 6) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone_camera_shutter");
            } else if (media == 7) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone_click");
            } else if (media == 8) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/recv");
            } else if (media == 10) {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone_notification");
            } else {
                uri = Uri.parse("android.resource://" + MainActivity.getInstance().getPackageName() + "/raw/tone1");
            }
            generalMp.setDataSource(App.getInstance(), uri);
            generalMp.setLooping(true);
            generalMp.setVolume(vol, vol);
            generalMp.prepare();
            generalMp.setOnPreparedListener(MediaPlayer::start);
        } catch (Exception e) {
            stopMedia();
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    public void stopMedia() {
        mHandler.removeMessages(666);
        try {
            if (generalMp != null) {
                generalMp.stop();
                generalMp.release();
                generalMp = null;
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            generalMp = null;
        }
    }




}
