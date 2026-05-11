package com.alfanar.villaroom.sockets;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.update.UpdateMulticastModel;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import java.net.DatagramPacket;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiCastParser extends Thread {
    private static final LinkedBlockingQueue<MulticastModel> multicastReceiverQueue = new LinkedBlockingQueue<>(250);
    private SharedPreferences sp;
    private boolean flag = true;
    Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
    public static synchronized void pushMulticastReceiverQueue(MulticastModel pack) {
        try {
            multicastReceiverQueue.offer(pack);
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void run() {
        super.run();
        sp = MyUtils.getInstance().getShared();
        try {
            while (flag) {
                MulticastModel pack = multicastReceiverQueue.take();
                if (pack != null) {
                    parse(pack);
                }
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            flag = false;
            multicastReceiverQueue.clear();
            SystemClock.sleep(5000);
            new MultiCastParser().start();
        }
    }

    public void parse(MulticastModel packet) {
        try {
            if (MyUtils.getInstance().inUpdateMode) return;


            String received =packet.getData();

            GeneralModel generalModel = gson.fromJson(received, GeneralModel.class);
            String type = generalModel.getType();

            Logger.d("MulticastParser.parse = " + received);

            if (type.equals(AppEnums.UPDATE_SCAN_DEVICE.name())) {
                GeneralModel gm = new GeneralModel(AppEnums.UPDATE_SET_SCAN_DEVICE.name(), "");
                UpdateMulticastModel umm1 = new UpdateMulticastModel();
                umm1.setAppVersion(BuildConfig.VERSION_CODE);
                umm1.setAppId(BuildConfig.APP_ID);
                umm1.setFwVersion(Build.DISPLAY);
                umm1.setMac(MyUtils.getInstance().getMACAddress());
                umm1.setIp(MyUtils.getInstance().getIpAddress());
                umm1.setAppName(BuildConfig.APP_NAME);
                umm1.setBaseVersion(MyUtils.getInstance().getShared().getString("base_version", "0.0"));
                gm.setData(gson.toJson(umm1));
                MulticastPublisher.send(gson.toJson(gm));
            } else if (type.equals(AppEnums.START_UPDATE.name())) {
                UpdateMulticastModel umm = gson.fromJson(generalModel.getData(), UpdateMulticastModel.class);
                Logger.d("umm.getMac = " + umm.getMac() + " util.mac = " + MyUtils.getInstance().getMACAddress());
                if (umm.getMac().equals(MyUtils.getInstance().getMACAddress())) {
                    String ip = packet.getIp();
                    if (umm.getFileLength() > 0) {
                        MyUtils.getInstance().wakeUp();
                        MyUtils.getInstance().inUpdateMode = true;
                        new DownloadApk(ip, umm.getFileLength()).start();
                    }
                }
            } else if (type.equals(AppEnums.SET_DEVICE_STATES.name())) {
                DeviceModel device = new Gson().fromJson(generalModel.getData(), DeviceModel.class);
                device.setState(true);
                DeviceController.getInstance().addDeviceToList(device);
            } else if (type.equals(AppEnums.GET_DEVICE_STATES.name())) {
                GeneralModel model = new GeneralModel("", "");
                model.setType(AppEnums.SET_DEVICE_STATES.name());
                DeviceModel deviceModel = new DeviceModel();
                deviceModel.setIp(MyUtils.getInstance().getIpAddress());
                deviceModel.setMac(MyUtils.getInstance().getMACAddress());
                deviceModel.setBaseVer(MyUtils.getInstance().getBaseVer());
                deviceModel.setName(sp.getString("DEVICE_NAME", App.getInstance().getResources().getString(R.string.item_room)));
                deviceModel.setLocationId("");
                deviceModel.setMaster(sp.getBoolean("IS_MASTER", false));
                deviceModel.setState(true);
                deviceModel.setType(AppEnums.ROOM.name());
                deviceModel.setAppVer(MyUtils.getInstance().getAppVer());
                deviceModel.setFwVer(MyUtils.getInstance().getFwVer());
                model.setData(gson.toJson(deviceModel));
                 MulticastPublisher.send(gson.toJson(model));
            }

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }
}
