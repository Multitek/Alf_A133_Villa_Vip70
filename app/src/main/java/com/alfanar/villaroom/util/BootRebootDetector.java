package com.alfanar.villaroom.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileReader;

public final class BootRebootDetector {

    private static final String SP = "boot_prefs";

    private static final String KEY_LAST_BOOT_ID = "last_boot_id";
    private static final String KEY_LAST_ELAPSED = "last_elapsed";
    private static final String KEY_ELAPSED_WORKS = "elapsed_works_checked";
    private static final String KEY_ELAPSED_WORKS_VALUE = "elapsed_works_value";

    private BootRebootDetector() {}

    /**
     * True: Önceki çalıştırmadan sonra cihaz reboot etmiş.
     * Öncelik: boot_id -> fallback elapsedRealtime
     */
    public static boolean isNewBootSession(Context ctx) {
        String current = readBootId();
        if (current == null || current.isEmpty()) {
            return false;
        }

        SharedPreferences sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
        String last = sp.getString(KEY_LAST_BOOT_ID, null);


        sp.edit().putString(KEY_LAST_BOOT_ID, current).apply();


        if (last == null) return false;

        return !current.equals(last);
    }





    private static String readBootId() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/sys/kernel/random/boot_id"));
            return br.readLine();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (br != null) br.close(); } catch (Exception ignore) {}
        }
    }
}
