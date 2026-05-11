package com.alfanar.i2c;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.alfanar.villaroom.util.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class I2CWorkerThread extends Thread {
    private static final LinkedBlockingQueue<I2CMessage> i2cSenderQueue = new LinkedBlockingQueue<>();
    private static final AtomicBoolean flag = new AtomicBoolean(true);
    private final Runnable readTask = () -> pushI2CSenderQueue(new I2CMessage(0, null));
    private final Runnable keepAliveTask = () -> {
        int[] writeBuf = {0x29};
        I2CMessage mes = new I2CMessage(1, writeBuf);
        pushI2CSenderQueue(mes);
    };




    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();



    private static final I2CMessage POISON = new I2CMessage(-1, null);


    public static I2CWorkerThread getInstance() {
        return SingletonHelper.INSTANCE;
    }


    public static void pushI2CSenderQueue(I2CMessage mes) {
        if (flag.get()) {
            i2cSenderQueue.offer(mes);
        }
    }



    public void kill() {
        Logger.d("I2CWorkerThread.Killing");
        flag.set(false);
        executor.shutdownNow();
        interrupt();
        i2cSenderQueue.clear();
    }


    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        setName("I2CWorkerThread");
        Logger.d("I2CWorkerThread.Started");
        I2CUtil.askBaseVersion();
        I2CUtil.askAmbianceLedStatus();
        I2CUtil.askBaseVersion();
        I2CUtil.askBaseVersion();



        executor.scheduleWithFixedDelay(readTask, 3000, 250, TimeUnit.MILLISECONDS);
        executor.scheduleWithFixedDelay(keepAliveTask, 1, 30, TimeUnit.SECONDS);


        while (flag.get()) {
            try {
                I2CMessage msg = i2cSenderQueue.take();
                executeMessage(msg);

            } catch (InterruptedException e) {
                break;

            } catch (Exception e) {
                Log.e("EXCEPTION", Log.getStackTraceString(e));
            }
        }

        Logger.d("I2CWorkerThread.Killed");


    }


    private void executeMessage(I2CMessage mes) {
        if (mes.mode() == 0) {
            I2CTransfer.read();

        } else if (mes.mode() == 1) {
            I2CTransfer.write(mes.data());
            SystemClock.sleep(300);
        }
    }

    private static class SingletonHelper {
        private static final I2CWorkerThread INSTANCE = new I2CWorkerThread();
    }
}
