package com.alfanar.villaroom.activities.cameras;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivityCameraPlayerBinding;
import com.alfanar.villaroom.databinding.CameraListItemBinding;
import com.alfanar.villaroom.models.Camera2Model;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.VlcUtil;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.OnvifMediaProfilesListener;
import be.teletask.onvif.listeners.OnvifMediaStreamURIListener;
import be.teletask.onvif.listeners.OnvifResponseListener;
import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifMediaProfile;
import be.teletask.onvif.responses.OnvifResponse;


public class CameraPlayer extends AppCompatActivity implements OnvifResponseListener, OnvifMediaProfilesListener, OnvifMediaStreamURIListener, MediaPlayer.EventListener, IVLCVout.OnNewVideoLayoutListener, Handler.Callback {

    private static CameraPlayer instance;
    ActivityCameraPlayerBinding binding;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private ArrayList<Camera2Model> list;
    private OnvifManager onvifManager;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivityCameraPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MainTimeout.getInstance().removeTimeout();

        MyUtils.getInstance().hideNavigation(this);
        list = DatabaseHelper.getInstance().getAllCameras2();
        Collections.sort(list, (o1, o2) -> {
            String v1 = (o1.getName());
            String v2 = (o2.getName());
            return v1.compareToIgnoreCase(v2);
        });

        MainTimeout.getInstance().setTimeout(1000 * 60 * 2);

        int index = getIntent().getIntExtra("index", 0);
        Camera2Model dev = list.get(index);
        binding.imgCamBack.setOnClickListener(v -> CameraPlayer.this.finish());
        binding.imgCamBack.setEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> binding.imgCamBack.setEnabled(true), 3000);

        onvifManager = new OnvifManager(this);
        mLibVLC = new LibVLC(this, VlcUtil.getInstance().getVlcArgs());
        mMediaPlayer = new MediaPlayer(mLibVLC);


        IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        mMediaPlayer.setScale(0);
        vlcVout.setVideoView(binding.videoLayout);
        if (vlcVout.areViewsAttached()) {
            vlcVout.detachViews();
        }
        vlcVout.attachViews(this);
        mMediaPlayer.setEventListener(this);


        binding.name.setText(dev.getName());
        binding.name.setText(dev.getName());

        instance = this;
        handler = new Handler(Looper.getMainLooper(), this);
        CameraListAdapter adapter = new CameraListAdapter(list);
        binding.recyclerCameras.setAdapter(adapter);

        playCamera(dev);
        handler.sendEmptyMessageDelayed(101, 5000);
    }

    void getRtspLink(String ip, String user, String pass) {
        OnvifDevice device = new OnvifDevice(ip, user, pass);
        onvifManager.getMediaProfiles(device, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        onvifManager.destroy();
        instance = null;
        VlcUtil.getInstance().destroyVlc(mLibVLC, mMediaPlayer);


    }


    void playCamera(Camera2Model cam) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }

            IMedia media = mMediaPlayer.getMedia();
            if (media != null && !media.isReleased()) {
                media.release();
            }
        }
        binding.name.setText(cam.getName());
        getRtspLink(cam.getIp(), cam.getUserName(), cam.getPassword());
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int stat = event.getAction();
        if (stat == MotionEvent.ACTION_DOWN) {
            MainTimeout.getInstance().setTimeout(120 * 1000);
            handler.removeMessages(100);
            handler.removeMessages(101);
            handler.sendEmptyMessageDelayed(100, 10);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onMediaProfilesReceived(OnvifDevice device, List<OnvifMediaProfile> mediaProfiles) {
        Logger.d("xxx.Onvif.onMediaProfilesReceived ");
        onvifManager.getMediaStreamURI(device, mediaProfiles.get(0), CameraPlayer.this);

    }

    @Override
    public void onMediaStreamURIReceived(OnvifDevice device, OnvifMediaProfile profile, String uri) {
        //rtsp://admin:admin.123@10.99.28.12/media/video2
        //rtsp://10.99.28.12/media/video1
        Logger.d("xxx.Onvif.onMediaStreamURIReceived = " + uri);
        String part2 = uri.substring(7);
        String localRtsp = "rtsp://" + device.getUsername() + ":" + device.getPassword() + "@" + part2;
        Logger.d("xxx.localRtsp = " + localRtsp);
        Media media = VlcUtil.getInstance().getVlcMediaOption(mLibVLC, localRtsp);
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();
    }

    @Override
    public void onResponse(OnvifDevice onvifDevice, OnvifResponse response) {

    }

    @Override
    public void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
        Logger.d("xxx.Onvif.onError = " + errorMessage);
        new Handler(Looper.getMainLooper()).post(() -> binding.txtInfo.setText(getString(R.string.rtsp_connection_error)));
    }


    @Override
    public void onEvent(MediaPlayer.Event event) {

        switch (event.type) {
            case MediaPlayer.Event.Buffering:
                Logger.d("onEvent.Buffering");
                break;

            case MediaPlayer.Event.EncounteredError:
                Logger.d("onEvent.EncounteredError");
                VlcUtil.getInstance().stopMedia(mMediaPlayer);
                new Handler(Looper.getMainLooper()).post(() -> binding.txtInfo.setText(getString(R.string.rtsp_connection_error)));
                break;

            case MediaPlayer.Event.EndReached:
                Logger.d("onEvent.EndReached");
                VlcUtil.getInstance().stopMedia(mMediaPlayer);
                break;

            case MediaPlayer.Event.Opening:
                Logger.d("onEvent.Opening");
                break;

            case MediaPlayer.Event.Playing:
                Logger.d("onEvent.Playing");
                new Handler(Looper.getMainLooper()).post(() -> binding.txtInfo.setText(""));
                break;

            case MediaPlayer.Event.Stopped:
                Logger.d("onEvent.Stopped");

                break;

            default:
                //Log.d("Multitek ","onEvent.default " + event.type);
                break;
        }
    }


    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {

    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == 100) {
            binding.recyclerCameras.setVisibility(View.VISIBLE);
            handler.removeMessages(101);
            handler.sendEmptyMessageDelayed(101, 5000);
        } else if (msg.what == 101) {
            binding.recyclerCameras.setVisibility(View.INVISIBLE);
        }
        return false;
    }


    static class CameraListAdapter extends RecyclerView.Adapter<CameraListAdapter.ViewHolder> {

        private final ArrayList<Camera2Model> list;

        public CameraListAdapter(ArrayList<Camera2Model> li) {
            this.list = li;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            @NonNull CameraListItemBinding binding = CameraListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CameraListAdapter.ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Camera2Model cam = list.get(holder.getAdapterPosition());
            holder.bindingCameras.txtCamera.setText(cam.getName());
            holder.bindingCameras.txtCamera.setOnClickListener(v -> {
                MyUtils.getInstance().setViewClickTimeout(v, 500);
                if (instance != null) {
                    instance.playCamera(cam);
                }

            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CameraListItemBinding bindingCameras;

            public ViewHolder(CameraListItemBinding binding) {
                super(binding.getRoot());
                this.bindingCameras = binding;
            }
        }
    }
}
