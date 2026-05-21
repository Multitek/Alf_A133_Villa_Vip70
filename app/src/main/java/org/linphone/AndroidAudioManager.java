package org.linphone;

import static android.content.Context.MODE_PRIVATE;
import static android.media.AudioManager.STREAM_VOICE_CALL;

import android.content.Context;
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
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

public class AndroidAudioManager implements Handler.Callback {




    private final SharedPreferences sp;

    private final AudioManager mAudioManager;
    private final Handler mHandler;
    private MediaPlayer mediaPlayer;

    private final int RELEASE_MEDIA_PLAYER = 666;


    private static class SingletonHelper {
        private static final AndroidAudioManager INSTANCE = new AndroidAudioManager();
    }

    public static AndroidAudioManager getInstance() {
        return AndroidAudioManager.SingletonHelper.INSTANCE;
    }


    private AndroidAudioManager() {
        mHandler = new Handler(Looper.getMainLooper(), this);
        mAudioManager = ((AudioManager) App.getInstance().getSystemService(Context.AUDIO_SERVICE));
        mAudioManager.setMicrophoneMute(false);
        mAudioManager.setSpeakerphoneOn(false);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 11, 0);
        sp = App.getInstance().getSharedPreferences("DEVICE_SETTINGS", MODE_PRIVATE);
        Logger.d("AndroidAudioManager.STREAM_VOICE_CALL max= "+ mAudioManager.getStreamMaxVolume(STREAM_VOICE_CALL) );
    }


    public void setCallStreamVolume(int val) {
        Logger.d("AndroidAudioManager.setCallStreamVolume index = "+ val );
        mAudioManager.setStreamVolume(STREAM_VOICE_CALL, val, 0);//0-5
        sp.edit().putInt("stream_volume", val).commit();

    }

    public String getStreamVolume() {
        return String.valueOf(sp.getInt("stream_volume",4));
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == RELEASE_MEDIA_PLAYER) {
            stopMedia();
        }

        return false;
    }


    public void adjustRingLevel(int type) {
        try {
            stopMedia();
            SharedPreferences devInfoSp = MyUtils.getInstance().getShared();
            int volIndex = devInfoSp.getInt("volIndex", 12); // progress değeri (0–15 arası)



            int media = 0;
            if (type == 1) { // door
                media = devInfoSp.getInt("ringDoorIndex", 0);
            } else if (type == 2) { // room
                media = devInfoSp.getInt("ringRoomIndex", 0);
            }

            int[] tones = {R.raw.tone1, R.raw.tone2, R.raw.tone3, R.raw.tone4, R.raw.tone5, R.raw.tone6};
            int idx = Math.max(0, Math.min(tones.length - 1, media));
            Uri uri = Uri.parse("android.resource://" + App.getInstance().getPackageName() + "/" + tones[idx]);



            mHandler.sendEmptyMessageDelayed(RELEASE_MEDIA_PLAYER, 6000);


            float vol = volIndex / 15.0f;


            Logger.d("adjustRingLevel vol = " + vol +" volIndex = " + volIndex);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); //0-15
            mediaPlayer.setDataSource(App.getInstance(), uri);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.setVolume(vol, vol);
            mediaPlayer.start();

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }



    public void stopMedia() {
        mHandler.removeMessages(RELEASE_MEDIA_PLAYER);
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            mediaPlayer = null;
        }
    }


}
