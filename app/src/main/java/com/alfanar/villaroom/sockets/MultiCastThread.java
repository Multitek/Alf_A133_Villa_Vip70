package com.alfanar.villaroom.sockets;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.update.UpdateMulticastModel;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiCastThread extends Thread {
    private volatile boolean running = true;
    private volatile MulticastSocket socket;

    private final String groupIp = "224.0.0.4";
    private final int port = 7071;

    private final AtomicBoolean restartRequested = new AtomicBoolean(false);

    // --- YENİ: WiFi Lock'ları ---
    private WifiManager.MulticastLock multicastLock;
    private WifiManager.WifiLock wifiLock;

    private static class SingletonHelper {
        private static final MultiCastThread INSTANCE = new MultiCastThread();
    }

    public static MultiCastThread getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private MultiCastThread() {}

    /**
     * Thread başlatılmadan önce Context ile lock'ları hazırla.
     * Örn: MultiCastThread.getInstance().init(getApplicationContext()).start();
     */
    public MultiCastThread init(Context context) {
        WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            multicastLock = wifiManager.createMulticastLock("vertroom_multicast");
            multicastLock.setReferenceCounted(false);
            wifiLock = wifiManager.createWifiLock(  WifiManager.WIFI_MODE_FULL_LOW_LATENCY,   "vertroom_wifi_lock");
            wifiLock.setReferenceCounted(false);

        }
        return this;
    }

    public void requestRestart() {
        restartRequested.set(true);
        clearSocket();
    }

    /** Uygulama kapanırken veya servis destroy'da çağır */
    public void stopThread() {
        running = false;
        releaseLocks();
        clearSocket();
    }

    private void acquireLocks() {
        try {
            if (multicastLock != null) {
                if (!multicastLock.isHeld()) multicastLock.acquire();
                Logger.d("MulticastThread Lock held: " + multicastLock.isHeld()); // true olmalı
            } else {
                Logger.w("MulticastThread Lock NULL — init() çağrıldı mı?"); // ← bu çıkıyorsa init eksik
            }

            if (wifiLock != null) {
                if (!wifiLock.isHeld()) wifiLock.acquire();
                Logger.d("MulticastThread WifiLock held: " + wifiLock.isHeld()); // true olmalı
            }
        } catch (Exception e) {
            Logger.e("MulticastThread Lock acquire hatası: ", e);
        }
    }
    private void releaseLocks() {
        try {
            if (multicastLock != null && multicastLock.isHeld()) {
                multicastLock.release();
            }
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (Exception e) {
            Logger.e("MulticastThread Lock release hatası: ", e);
        }
    }

    private boolean hasValidIp() {
        String ip = MyUtils.getInstance().getIpAddress();
        return ip != null && !ip.isEmpty() && !"192.168.256.256".equals(ip);
    }

    private void clearSocket() {
        MulticastSocket s = socket;
        socket = null;
        if (s != null) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private NetworkInterface getActiveNetworkInterface() {
        try {
            String myIp = MyUtils.getInstance().getIpAddress();
            if (myIp == null || myIp.isEmpty()) return null;

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                if (!nif.isUp() || nif.isLoopback() || !nif.supportsMulticast()) continue;

                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.getHostAddress().equals(myIp)) return nif;
                }
            }
        } catch (Exception e) {
            Logger.e("Interface bulma hatası: ", e);
        }
        return null;
    }

    @Override
    public void run() {
        // Lock'ları thread başında bir kez al, thread boyunca tut
        acquireLocks();

        while (running) {
            NetworkInterface ni = null;
            InetAddress group = null;
            MulticastSocket s = null;

            try {
                if (!hasValidIp()) {
                    SystemClock.sleep(5000);
                    continue;
                }

                ni = getActiveNetworkInterface();

                if (ni == null) {
                    SystemClock.sleep(5000);
                    continue;
                }

                restartRequested.set(false);




                group = InetAddress.getByName(groupIp);
                s = new MulticastSocket(null);
                s.setReuseAddress(true);
                s.setSoTimeout(30000);
                s.setReceiveBufferSize(256 * 1024);
                s.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
                s.setTimeToLive(1);
                SocketAddress mCastAddress = new InetSocketAddress(group, port);
                s.joinGroup(mCastAddress, ni);



                socket = s;

                byte[] buf = new byte[5 * 1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                MulticastPublisher.send(new Gson().toJson(
                        new GeneralModel(AppEnums.UPDATE_SET_SCAN_DEVICE.name(), getUpdateDevice())));
                MulticastPublisher.send(new Gson().toJson(
                        new GeneralModel(AppEnums.SET_DEVICE_STATES.name(), getDevice())));
                MulticastPublisher.send(new Gson().toJson(
                        new GeneralModel(AppEnums.GET_DEVICE_STATES.name(), "")));

                while (running && socket == s && !restartRequested.get()) {
                    try {
                        packet.setLength(buf.length);
                        s.receive(packet);

                        String remoteIP = packet.getAddress().getHostAddress();
                        if (remoteIP != null && remoteIP.equals(MyUtils.getInstance().getIpAddress())) {
                            Logger.d("MulticastSocket skip OWN Packet");
                            continue;
                        }

                        String data = new String(packet.getData(), 0, packet.getLength()).trim();
                        MultiCastParser.pushMulticastReceiverQueue(new MulticastModel(data, remoteIP));

                    } catch (java.net.SocketTimeoutException ignore) {
                        // Normal — döngü devam eder
                    }
                }

            } catch (java.net.SocketException e) {
                // Socket closed — requestRestart() tetikledi
            } catch (Exception e) {
                Logger.e("Multicast error: ", e);
            } finally {
                if (socket == s) socket = null;
                if (s != null) {
                    try {
                        try {s.leaveGroup(new InetSocketAddress(group, port), ni);} catch (Exception ignored) {}
                        s.close();
                    } catch (Exception ignored) {}
                }
            }

            if (running) {
                SystemClock.sleep(5000);
            }
        }

        // Thread durduğunda lock'ları bırak
        releaseLocks();
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