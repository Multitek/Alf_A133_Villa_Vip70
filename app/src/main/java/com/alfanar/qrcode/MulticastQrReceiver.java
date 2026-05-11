package com.alfanar.qrcode;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.activities.QrDetectorActivity;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastQrReceiver extends Thread {

    private MulticastSocket socket;
    private final byte[] buf = new byte[512];
    private InetAddress group;
    private Gson gs;

    @Override
    public void run() {
        super.run();
        try {
            socket = new MulticastSocket(40002);
            group = InetAddress.getByName("224.0.0.12");
            socket.joinGroup(group);
            Logger.d("MulticastQrReceiver started");


            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                process(received);
            }
        } catch (Exception e) {
            //  Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
            socket = null;
            SystemClock.sleep(5000);
            new MulticastQrReceiver().start();
        }
    }

    private void process(String mes) {
        Logger.d("MulticastQrReceiver.process mes = " + mes);
        gs = new GsonBuilder().setLenient().create();
        MulticastModel model = gs.fromJson(mes, MulticastModel.class);
        if (model.getMessage().equals("QR_SCAN_DEVICE")) {
            MulticastModel sendModel = new MulticastModel();
            sendModel.setAppVersion("V" + BuildConfig.VERSION_CODE);
            sendModel.setBaseVersion(MyUtils.getInstance().getBaseVer());
            sendModel.setCpu(MyUtils.getCpuSerial());
            sendModel.setFwVersion(Build.DISPLAY);
            sendModel.setId("123");
            sendModel.setMac(MyUtils.getInstance().getMACAddress());
            sendModel.setIp(MyUtils.getInstance().getIpAddress());
            sendModel.setMessage("QR_NEW_DEVICE");
            sendModel.setPassword("");
            new MulticastQrPublisher(new Gson().toJson(sendModel)).start();
        } else if (model.getMessage().startsWith("QR_WARNING_ON")) {
            if (model.getMac().equals(MyUtils.getInstance().getMACAddress())) {
                QrDetectorActivity.id = model.getMessage().split("#")[1];
                MyUtils.getInstance().backToRootActivity();
                new Handler(Looper.getMainLooper()).postDelayed(() -> MainActivity.getInstance().startQRActivity(), 250);
            }
        } else if (model.getMessage().equals("QR_WARNING_OFF")) {
            if (model.getMac().equals(MyUtils.getInstance().getMACAddress())) {
                MyUtils.getInstance().backToRootActivity();
            }
        }
    }


}
