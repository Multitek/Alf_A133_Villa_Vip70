package com.alfanar.villaroom.sockets;


import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer extends Thread {

    private static final int PORT = 2442;

    private volatile boolean running = true;
    private volatile boolean restartRequested = false;
    private volatile ServerSocket serverSocket;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    private static class SingletonHelper {
        private static final TCPServer INSTANCE = new TCPServer();
    }

    public static TCPServer getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private TCPServer() { }

    private boolean hasValidIp() {
        String ip = MyUtils.getInstance().getIpAddress();
        return ip != null && !ip.isEmpty() && !"192.168.256.256".equals(ip);
    }

    /** Sadece yeniden bind etsin (thread ölmesin) */
    public void requestRestart() {
        restartRequested = true;
        closeServerSocket(); // accept() bloktan çıkar
    }

    /** Tamamen kapatmak için */
    public void shutdown() {
        running = false;
        closeServerSocket();
        interrupt();
        executor.shutdownNow();

    }

    private void closeServerSocket() {
        try {
            ServerSocket ss = serverSocket;
            if (ss != null) ss.close();
        } catch (Exception ignore) { }
    }

    @Override
    public void run() {
        while (running) {
            try {

                while (running && !hasValidIp()) {
                    Thread.sleep(1000);
                }


                if (!running) break;

                restartRequested = false;

                ServerSocket ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(PORT));
                serverSocket = ss;
                Logger.d("TCPServer.started");
                while (running && !restartRequested) {
                    try {
                        Socket socket = ss.accept();
                        socket.setSoTimeout(5000);
                       new TCPServerMessageExecutor(socket).start();
                    } catch (SocketException se) {
                        break;
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("TCPServer", "Server loop error", e);
            } finally {
                closeServerSocket();
                serverSocket = null;
            }

            if (running) {
                try { Thread.sleep(500); } catch (InterruptedException ignore) { }
            }
        }
    }
}
