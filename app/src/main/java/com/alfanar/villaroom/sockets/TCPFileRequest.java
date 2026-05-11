package com.alfanar.villaroom.sockets;

import android.os.SystemClock;

import com.alfanar.villaroom.util.MyUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPFileRequest extends Thread {
    private final String SERVER;
    private final String path;
    private FileOutputStream fos = null;
    private InputStream in = null;
    private Socket sock = null;

    public TCPFileRequest(String ServerIP, String path) {
        SERVER = ServerIP;
        this.path = path;
    }

    @Override
    public void run() {
        super.run();
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(InetAddress.getByName(SERVER), 13267), 5000);
            in = sock.getInputStream();
            fos = new FileOutputStream(path);
            byte[] fileBuffer = new byte[1024 * 10];
            int bytesRead;
            while ((bytesRead = in.read(fileBuffer)) != -1) {
                fos.write(fileBuffer, 0, bytesRead);
            }

        } catch (Exception ignore) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception ignore) {
            }
            try {
                if (in != null) {
                    in.close();
                }

            } catch (Exception ignore) {
            }

            try {
                if (sock != null) {
                    sock.close();
                }
            } catch (Exception ignore) {
            }

            fos = null;
            in = null;
            sock = null;

            if (MyUtils.getInstance().fileListener != null) {
                MyUtils.getInstance().fileListener.loadImage();
            } else {
                SystemClock.sleep(1000);
                if (MyUtils.getInstance().fileListener != null) {
                    MyUtils.getInstance().fileListener.loadImage();
                }
            }


            MyUtils.getInstance().controlCallImagesFolder();
        }
    }
}
