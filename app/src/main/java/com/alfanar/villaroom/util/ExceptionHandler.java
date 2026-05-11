package com.alfanar.villaroom.util;

import android.Manifest;

import androidx.annotation.NonNull;

public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        if (MyUtils.getInstance().checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new WriteException(e).start();
        } else {
            Logger.d("ExceptionHandler WRITE_EXTERNAL_STORAGE not GRANTED!!! exception= " + e.getMessage());
        }
    }
}
