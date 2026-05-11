package com.alfanar.villaroom.sockets;

import android.os.SystemClock;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPSenderThread extends Thread {
    private final String address;
    private final String message;
    private Socket socket;
    private PrintWriter out;

    public TCPSenderThread(String dstAddress, String mess) {
        address = dstAddress;
        message = mess;
    }

    @Override
    public void run() {
        super.run();
        try {
            socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(address), 2442);
            socket.connect(socketAddress, 3000);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            SystemClock.sleep(50);
        } catch (Exception ignore) {

        } finally {
            try {
                if (out != null) {
                    out.close();
                }

            } catch (Exception ignore) {
            }

            try {
                if (socket != null) {
                    socket.close();
                }

            } catch (Exception ignore) {
            }
            out = null;
            socket = null;
        }
    }
}
