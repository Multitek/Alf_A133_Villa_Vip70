package com.alfanar.qrcode;

import android.util.Log;

import com.alfanar.villaroom.util.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastQrPublisher extends Thread {
    DatagramSocket socket;
    private final String mesx;

    public MulticastQrPublisher(String mes) {
        this.mesx = mes;
    }

    @Override
    public void run() {
        super.run();
        sendMultiCast(mesx);
    }

    public void sendMultiCast(final String message) {
        try {
            Thread.sleep(250);
            socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName("224.0.0.12");
            byte[] buf = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, 40001);
            socket.send(packet);

            Logger.d("multicast sended  = " + message);
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            if (socket != null) {
                socket.close();
            }

        }
    }
}
