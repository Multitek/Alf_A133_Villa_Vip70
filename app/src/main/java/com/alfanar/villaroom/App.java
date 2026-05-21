package com.alfanar.villaroom;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alfanar.villaroom.util.BootRebootDetector;
import com.alfanar.villaroom.util.ExceptionHandler;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.WriteException;
import com.github.anrwatchdog.ANRWatchDog;

import org.conscrypt.Conscrypt;
import org.linphone.LinphoneManager;

import java.security.Security;

public class App extends Application {

    private static volatile App appInstance;

    long duration = 4;
    ANRWatchDog anrWatchDog = new ANRWatchDog(15000);

    public static App getInstance() {
        return appInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
       // Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        appInstance = this;
        anrWatchDog.setANRListener(error -> {
            Log.e("ANR-Watchdog-Demo", "Detected Application Not Responding!");
            Throwable thr = error.fillInStackTrace();
            new WriteException(thr).start();
        });

      //  anrWatchDog.start();

        try {
            Security.insertProviderAt(Conscrypt.newProvider(), 1);
            Log.d("TLS", "✅ Conscrypt provider yüklendi: " + Security.getProviders()[0].getName());
        } catch (Exception e) {
            Log.e("TLS", "⚠️ Conscrypt yüklenemedi", e);
        }



        boolean rebooted = BootRebootDetector.isNewBootSession(this);
        if (rebooted) {
            Logger.d("BootRebootDetector.DEVICE_BOOT");
            MyUtils.getInstance().dhcpOn(this);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                String ip = MyUtils.getInstance().getIpAddress();
                if(ip.equals("192.168.256.256")){
                    MyUtils.getInstance().dhcpOff2(App.getInstance());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> MyUtils.getInstance().restartApp(),5000);
                }
            },90*1000);

        } else {
            Logger.d("APP_RESTART_SAME_BOOT");
        }






    }


}
