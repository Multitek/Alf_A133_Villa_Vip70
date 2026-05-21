package com.alfanar.villaroom.threads;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.util.MyUtils;

public class MainTimeout {
    private static volatile MainTimeout instance;
    public boolean buttonsStat = true;
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    if (MainActivity.getInstance() == null) {
                        MyUtils.getInstance().backToRootActivity();
                    }
                    break;
                case 200:
                    buttonsStat = true;
                    break;

                default:
                    break;
            }
        }
    };

    MainTimeout() {
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static MainTimeout getInstance() {
        if (instance == null) {
            synchronized (MainTimeout.class) {
                if (instance == null) instance = new MainTimeout();
            }
        }
        return instance;
    }

    public void removeTimeout() {
        mHandler.removeMessages(100);
    }

    public void setTimeout(long delay) {
        mHandler.removeMessages(100);
        mHandler.sendEmptyMessageDelayed(100, delay);
    }


}
