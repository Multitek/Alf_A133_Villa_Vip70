package com.alfanar.villaroom.util;

import android.net.Uri;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;

import java.util.ArrayList;

public class VlcUtil {

    public static VlcUtil getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public Media getVlcMediaOption(LibVLC mLibVLC, String addr) {
        Media media = new Media(mLibVLC, Uri.parse(addr));
        media.addOption(":rtsp-tcp");
        media.setHWDecoderEnabled(true, false);
        media.addOption(":avcodec-hw=any"); // donanım decode fallback
        media.addOption(":network-caching=500");
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");
        media.addOption(":no-audio");
        media.addOption(":no-spu");
        media.addOption(":no-osd");

        return media;


    }

    public ArrayList<String> getVlcArgs() {
        ArrayList<String> args = new ArrayList<>();
        args.add("--verbose=0");
        args.add("--vout=android-display");
        args.add("--android-display-chroma=RV32");
        args.add("--avcodec-hw=any");
        return args;
    }

    public void destroyVlc(LibVLC mLibVLC, MediaPlayer mMediaPlayer) {
        if (mMediaPlayer != null) {
            try {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.getVLCVout().detachViews();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!mMediaPlayer.isReleased()) {
                mMediaPlayer.release();
            }
        }

        if (mLibVLC != null) {
            mLibVLC.release();
        }


    }

    public void stopMedia(MediaPlayer mMediaPlayer) {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            IMedia media = mMediaPlayer.getMedia();
            if (media != null && !media.isReleased()) {
                media.release();
            }
        }
    }

    private static class SingletonHelper {
        private static final VlcUtil INSTANCE = new VlcUtil();
    }
}
