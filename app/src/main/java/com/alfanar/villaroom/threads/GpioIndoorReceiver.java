package com.alfanar.villaroom.threads;

import android.os.SystemClock;

import com.alfanar.i2c.I2CUtil;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.util.Logger;


public class GpioIndoorReceiver extends Thread {
    private boolean flag = true;

    @Override
    public void run() {
        super.run();
        //instance = this;
        SystemClock.sleep(5000);
        try {
            while (flag) {
                int res = I2CUtil.readIndoor();
                if (res == 0) {
                    MainActivity.getInstance().indoorRingReceived("gpio");
                    SystemClock.sleep(8000);
                }

                SystemClock.sleep(200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
