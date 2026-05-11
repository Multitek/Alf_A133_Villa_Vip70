package com.alfanar.villaroom.util;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.villaroom.App;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.Date;

public class TimeManager extends Thread implements Handler.Callback {
    private final int DESTROY_TIME_CLIENT = 100;
    private boolean received = false;
    //private long id;
    private NTPUDPClient timeClient;
    private Handler handler;

    @Override
    public void run() {
        super.run();

       // if(!MyUtils.getInstance().internetActive) return;

        received = false;
        handler = new Handler(Looper.getMainLooper(), this);
       // id = this.getId();
        String[] timeServers = new String[]{"time.google.com", "time.windows.com", "time.apple.com", "time.nist.gov"};
        for (String host : timeServers) {
            if (getTimeFromNtp(host)) {
                break;
            } else {
                SystemClock.sleep(15000);
            }
        }
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    private boolean getTimeFromNtp(String host) {
        handler.sendEmptyMessageDelayed(DESTROY_TIME_CLIENT, 12000);
        timeClient = new NTPUDPClient();

        // Log.d("TimeManager_"+id,host +" <- TimeManager Requesting... " );
        try {
            timeClient.setDefaultTimeout(5000);
            timeClient.open();
            timeClient.setSoTimeout(5000);

            TimeInfo timeInfo = timeClient.getTime(InetAddress.getByName(host));
            NtpV3Packet message = timeInfo.getMessage();
            long serverTime = message.getTransmitTimeStamp().getTime();
            handler.removeMessages(DESTROY_TIME_CLIENT);
            received = true;
              Log.d("TimeManager_",host +" <- TimeManager Time Received " + new Date(serverTime) );

            if (MyUtils.getInstance().checkPermission(Manifest.permission.SET_TIME)) {
                AlarmManager am = (AlarmManager) App.getInstance().getSystemService(Context.ALARM_SERVICE);
                am.setTime(serverTime);
                Log.d("TimeManager_",host +" <- TimeManager time set to = " + new Date(serverTime));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Log.d("TimeManager_"+id,host +" <- TimeManager Exception " + e.getMessage()  +" received = "+ received);
            received = false;
        } finally {
            try {
                if (timeClient != null) {
                    //    Log.d("TimeManager_"+id,host +" <- TimeManager timeClient closing. received = " + received);
                    timeClient.close();
                    timeClient = null;
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
        return received;
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == DESTROY_TIME_CLIENT) {
            if (timeClient != null) {
                //   Log.d("TimeManager_"+id," <- TimeManager Timeout Close received = " + received );
                received = false;
                timeClient.close();
            }
        }
        return false;
    }
}
