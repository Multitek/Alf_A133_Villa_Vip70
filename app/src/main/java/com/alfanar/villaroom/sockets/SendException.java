package com.alfanar.villaroom.sockets;

import android.content.Context;
import android.util.Log;

import com.alfanar.villaroom.util.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SendException extends Thread {
    private final File file;
    private final Context context;
    private ByteArrayOutputStream baos;
    private InputStream fis;
    private OutputStream os;
    private Socket socket;

    public SendException(File f, Context ctx) {
        file = f;
        this.context = ctx;
    }

    @Override
    public void run() {
        super.run();
        try {
            Log.d("alfanar ", "[ThreadSendException] file name: " + file.getName());
            socket = new Socket("", 6666);
            baos = new ByteArrayOutputStream();
            Gson gson = new GsonBuilder().setLenient().create();
            ExceptionModel exceptionModel = new ExceptionModel();
            exceptionModel.setDevice("Vip70");
            exceptionModel.setKey("Alfanar_Exception_File_Key");
            String message = gson.toJson(exceptionModel);
            byte[] arr = message.getBytes();
            byte[] arrDoc = new byte[2048];
            System.arraycopy(arr, 0, arrDoc, 0, arr.length);
            baos.write(arrDoc);
            Thread.sleep(1000);
            byte[] arrImg = new byte[(int) file.length()];
            fis = new FileInputStream(file);
            fis.read(arrImg);
            baos.write(arrImg);
            byte[] data = baos.toByteArray();
            os = socket.getOutputStream();
            os.write(data, 0, data.length);
            os.flush();

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }

            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
            try {
                if (fis != null) {
                    fis.close();
                }

            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }

            try {
                if (os != null) {
                    os.close();
                }

            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }

            try {
                if (socket != null) {
                    socket.close();
                }

            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
            try {
                String path = context.getFilesDir().getAbsolutePath() + "/AlfanarRoom/exception.txt";
                File file = new File(path);
                boolean result = file.delete();
                Log.d("alfanar ", "[ThreadSendException] exception.txt delete result: " + result);
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }

    private static class ExceptionModel {
        private String device;
        private String key;

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
