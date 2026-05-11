package com.alfanar.villaroom.util;

import android.os.Build;
import android.util.Log;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class WriteException extends Thread {
    private final Throwable ex;

    public WriteException(Throwable exception) {
        this.ex = exception;
    }

    @Override
    public void run() {
        super.run();
        try {
            if (ex != null) {
                Log.d("Multitek ", "[WriteException] Reason" + ex.getMessage());
                File file = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/AlfanarRoom/");
                boolean result = file.mkdirs();
                String user = MyUtils.getInstance().getMACAddress();
                String versionName = BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")";
                String device = Build.MANUFACTURER + "(" + Build.MODEL + ")";

                File exceptionFile = new File(file, "exception.txt");
                FileOutputStream out = new FileOutputStream(exceptionFile, true);

                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.getDefault());
                String title = "\r\n" + "\r\n" + "***" + " date:" + formatter.format(System.currentTimeMillis()) + "  " + "  version:" + versionName + "  user:" + user + "  device:" + device + "\r\n";

                String detail = "Detail:" + Log.getStackTraceString(ex) + "\r\n";
                String exception = title + " " + detail + "***\r\n";
                out.write(exception.getBytes());


                out.flush();
                out.close();
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            ProcessPhoenix.triggerRebirth(App.getInstance());
        }
    }
}
