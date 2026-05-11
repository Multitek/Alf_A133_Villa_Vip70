package com.alfanar.villaroom.util;

import android.util.Log;

import com.alfanar.villaroom.BuildConfig;


public class Logger {

    private static final String DEFAULT_TAG = "MULTITEK"; // Varsayılan etiket

    // 🔹 DEBUG mod aktif mi?
    private static final boolean ENABLE_LOG = BuildConfig.DEBUG;


    public static void i(String message) {
        if (ENABLE_LOG) Log.i(DEFAULT_TAG, message);
    }

    public static void d(String message) {
        if (ENABLE_LOG) Log.d(DEFAULT_TAG, message);
    }

    public static void e(String message, Throwable throwable) {
        if (ENABLE_LOG) Log.e(DEFAULT_TAG, message, throwable);
    }

    public static void v(String message) {
        if (ENABLE_LOG) Log.v(DEFAULT_TAG, message);
    }

    public static void w(String message) {
        if (ENABLE_LOG) Log.w(DEFAULT_TAG, message);
    }
}
