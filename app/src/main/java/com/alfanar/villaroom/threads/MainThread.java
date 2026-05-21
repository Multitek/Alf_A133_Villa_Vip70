package com.alfanar.villaroom.threads;


import android.Manifest;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.alfanar.i2c.I2CTransfer;
import com.alfanar.i2c.I2CUtil;
import com.alfanar.i2c.I2CWorkerThread;
import com.alfanar.qrcode.MulticastQrReceiver;
import com.alfanar.retrofit.update.ApkVersionApi;
import com.alfanar.retrofit.update.GetInternetStateApi;
import com.alfanar.retrofit.update.MacAddressApi;
import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.networkmanager.NetworkOperator;
import com.alfanar.villaroom.networkmanager.NetworkWatcher;
import com.alfanar.villaroom.sockets.MultiCastParser;
import com.alfanar.villaroom.sockets.MultiCastThread;
import com.alfanar.villaroom.sockets.MulticastPublisher;
import com.alfanar.villaroom.sockets.TCPServer;
import com.alfanar.villaroom.update.UpdateMulticastModel;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.TimeManager;
import com.google.gson.Gson;

import org.linphone.LinphoneManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainThread extends Thread {

    private final NetworkWatcher netWatcher;

    public MainThread(NetworkWatcher netWatcher) {
        this.netWatcher=netWatcher;
    }

    @Override
    public void run() {
        super.run();
        I2CWorkerThread.getInstance().start();
        MyUtils.weatherCountryList = MyUtils.getInstance().loadWeatherCountryList();


        MyUtils.getInstance().initEnvironments();

        DatabaseHelper.getInstance().initDBValues();

        new TimeManager().start();

        new MacAddressApi(10).start();


        new MultiCastParser().start();
        new MulticastQrReceiver().start();


        boolean ledState = MyUtils.getInstance().getShared().getBoolean("baseLed", false);
        I2CUtil.setAmbianceLedStatus(ledState ? 1 : 0);

        ScheduledExecutorService service1 = Executors.newSingleThreadScheduledExecutor();
        service1.scheduleWithFixedDelay(() -> {
            //new NetworkOperator().start();
             MulticastPublisher.send(new Gson().toJson(new GeneralModel(AppEnums.SET_DEVICE_STATES.name(), getDevice())));
            SystemClock.sleep(3000);
            DeviceController.getInstance().checkIpConflict();
        }, 17, 90, TimeUnit.SECONDS);

        /*ScheduledExecutorService service2 = Executors.newSingleThreadScheduledExecutor();
        service2.scheduleWithFixedDelay(() -> {
            new ApkVersionApi().start();
        }, 6, 60, TimeUnit.MINUTES);*/

        ScheduledExecutorService service3 = Executors.newSingleThreadScheduledExecutor();
        service3.scheduleWithFixedDelay(() -> {
            new TimeManager().start();
            new WeatherControl().start();
            new GetInternetStateApi().start();
        }, 1, 5, TimeUnit.MINUTES);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            LinphoneManager.getInstance().startLibLinphone();
            I2CUtil.askBaseVersion();//double_check
            SystemClock.sleep(1000);

            if (!I2CTransfer.baseDetected) {
                new GpioIndoorReceiver().start();
                I2CWorkerThread.getInstance().kill();
            }

            MultiCastThread.getInstance().init(App.getInstance()).start();
            TCPServer.getInstance().start();
            SystemClock.sleep(500);
            netWatcher.start();

            MainActivity.getInstance().initDone();

        }, 5000);


        MainActivity.getInstance().weatherJsonDone();



    }

    private String getDevice() {
        SharedPreferences sp = MyUtils.getInstance().getShared();
        DeviceModel deviceModel = new DeviceModel();
        deviceModel.setIp(MyUtils.getInstance().getIpAddress());
        deviceModel.setMac(MyUtils.getInstance().getMACAddress());
        deviceModel.setBaseVer(MyUtils.getInstance().getBaseVer());
        deviceModel.setName(sp.getString("DEVICE_NAME", App.getInstance().getString(R.string.item_room)));
        deviceModel.setMaster(sp.getBoolean("IS_MASTER", false));
        deviceModel.setLocationId("");
        deviceModel.setState(true);
        deviceModel.setType(AppEnums.ROOM.name());
        deviceModel.setAppVer(MyUtils.getInstance().getAppVer());
        deviceModel.setFwVer(MyUtils.getInstance().getFwVer());
        return new Gson().toJson(deviceModel);
    }

    private String getUpdateDevice() {
        UpdateMulticastModel umm1 = new UpdateMulticastModel();
        umm1.setAppVersion(BuildConfig.VERSION_CODE);
        umm1.setAppId(BuildConfig.APP_ID);
        umm1.setFwVersion(Build.DISPLAY);
        umm1.setMac(MyUtils.getInstance().getMACAddress());
        umm1.setIp(MyUtils.getInstance().getIpAddress());
        umm1.setAppName(BuildConfig.APP_NAME);
        umm1.setBaseVersion(MyUtils.getInstance().getShared().getString("base_version", "0.0"));
        return new Gson().toJson(umm1);
    }


}
