package com.alfanar.villaroom.sockets;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DownloadApk extends Thread {

    private final String ip;
    private final int fileLen;
    private Socket socket;
    //private PrintWriter pwr;
    //private BufferedReader bfr;

    private BufferedReader bufferedReader;
    private PrintWriter bufferedWriter;

    public DownloadApk(String ip, int fileLen) {
        this.ip = ip;
        this.fileLen = fileLen;
    }

    @Override
    public void run() {
        super.run();
        try {
            MyUtils.getInstance().wakeUp();
            pingDevice(ip);
            socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getByName(ip), 38000), 8000);
            socket.setSoTimeout(20000);
            Logger.d("DownloadApk filelen = " + fileLen);



            File apkDir = new File(App.getInstance().getExternalFilesDir(null), "ApkFolder");
            File fd = new File(apkDir, "localApp.apk");

            if (fd.exists()) {
                boolean res = fd.delete();
                Logger.d("DownloadApk localApp.apk deleted = " + res);
            }

            MyUtils.getInstance().inUpdateMode = true;

            bufferedWriter = new PrintWriter(socket.getOutputStream(), true);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());


            int remaining = fileLen;
            byte[] buffer = new byte[16 * 1024];
            int bytes;
            FileOutputStream fileOutputStream = new FileOutputStream(fd);
            while (remaining > 0 && (bytes = dataInputStream.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                remaining -= bytes;      // read upto file size
            }
            fileOutputStream.close();
            bufferedWriter.println("File_Received remaining = " + remaining);

            MyUtils.getInstance().backToRootActivity();
            if (remaining == 0) {
                installApp();
            }


        } catch (Exception e) {
            Logger.d("DownloadApk EXCEPTION = " + Log.getStackTraceString(e));
            MyUtils.getInstance().inUpdateMode = false;
        } finally {


            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (Exception e) {
                    Logger.d("DownloadApk EXCEPTION = " + Log.getStackTraceString(e));
                }
            }


            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Logger.d("DownloadApk EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
            bufferedWriter = null;
            bufferedReader = null;
            socket = null;

        }

    }

    private void installApp() {
        try {
            Logger.d("DownloadApk DownloadApk.installApp ");

            File apkDir = new File(App.getInstance().getExternalFilesDir(null), "ApkFolder");
            File fd = new File(apkDir, "localApp.apk");

            if (fd.exists()) {
                Uri apkUri = FileProvider.getUriForFile(
                        App.getInstance(),
                        App.getInstance().getPackageName() + ".fileprovider",
                        fd
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                App.getInstance().startActivity(intent);
            } else {
                Logger.d("DownloadApk.installApp file not found !!!!! ");
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            Logger.d("DownloadApk.installApp.Exception =  " + e.getMessage());
        } finally {
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> MyUtils.getInstance().inUpdateMode = false,
                    5000
            );
        }
    }

    private void pingDevice(String ip) {
        try {
            InetAddress.getByName(ip).isReachable(3000);
        } catch (IOException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }
}
