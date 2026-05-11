package com.alfanar.villaroom.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.jakewharton.processphoenix.ProcessPhoenix;

import org.linphone.LinphoneManager;

public class ScreenStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String act = intent.getAction();
        Logger.d("ScreenStateReceiver action = " + act);
        if (act.equals(Intent.ACTION_SCREEN_ON)) {

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            if (LinphoneManager.getInstance().getCore() != null && LinphoneManager.getInstance().getCore().inCall()) {
                PowerManager pm;
                PowerManager.WakeLock screen_wakeLock;
                pm = (PowerManager) App.getInstance().getSystemService(Context.POWER_SERVICE);

                screen_wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "screen:WakeLock");
                screen_wakeLock.acquire(60000);
                return;
            }
            if (!MyUtils.getInstance().inUpdateMode) {
                if (LinphoneManager.getInstance().getCore() != null) {
                    LinphoneManager.getInstance().getCore().terminateAllCalls();
                }
               // MyUtils.getInstance().backToRootActivity();
               // LinphoneManager.getInstance().restartLinphone();

                ProcessPhoenix.triggerRebirth(MainActivity.getInstance());
            }

        }
    }
}
