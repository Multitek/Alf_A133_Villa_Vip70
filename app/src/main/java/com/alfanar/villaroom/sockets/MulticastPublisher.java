package com.alfanar.villaroom.sockets;

import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastPublisher   {

    private static final String MULTICAST_GROUP = "224.0.0.4";
    private static final int MULTICAST_PORT = 7071;
    private static final int TTL = 1;

    // Tüm uygulama genelinde tek executor — thread havuzu
    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "multicast-publisher");
                t.setDaemon(true);
                return t;
            });

    private MulticastPublisher() {}

    /**
     * Mesajı multicast grubuna asenkron gönderir.
     * Her çağrıda yeni thread açmaz — executor pool kullanır.
     */
    public static void send(String message) {
        executor.execute(() -> sendInternal(message));
    }

    /**
     * Birden fazla mesajı sırayla gönderir (tek executor job).
     */
    public static void sendAll(String... messages) {
        executor.execute(() -> {
            for (String message : messages) {
                sendInternal(message);
            }
        });
    }

    private static void sendInternal(String message) {
        NetworkInterface targetNif = getActiveNetworkInterface();
        if (targetNif == null) {
            Logger.w("MulticastPublisher: Aktif network interface bulunamadı");
            return;
        }

        MulticastSocket socket = null;
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, group, MULTICAST_PORT);

            socket = new MulticastSocket();
            socket.setTimeToLive(TTL);
            socket.setNetworkInterface(targetNif);
            socket.send(packet);

            Logger.d("MulticastPublisher: Gönderildi -> " + targetNif.getName()
                    + " (" + bytes.length + " byte)");

        } catch (Exception e) {
            Logger.e("MulticastPublisher: Gönderim hatası", e);
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * MultiCastThread.getActiveNetworkInterface() ile aynı mantık.
     * Ortak bir util'e taşınabilir.
     */
    private static NetworkInterface getActiveNetworkInterface() {
        try {
            String myIp = MyUtils.getInstance().getIpAddress();
            if (myIp == null || myIp.isEmpty()) return null;

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                if (!nif.isUp() || nif.isLoopback() || !nif.supportsMulticast()) continue;

                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement().getHostAddress().equals(myIp)) return nif;
                }
            }
        } catch (Exception e) {
            Logger.e("MulticastPublisher: Interface hatası", e);
        }
        return null;
    }

    /** Uygulama kapanırken çağır */
    public static void shutdown() {
        executor.shutdownNow();
    }
}