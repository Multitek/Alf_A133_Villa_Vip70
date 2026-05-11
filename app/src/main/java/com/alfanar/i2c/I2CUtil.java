package com.alfanar.i2c;


import static com.alfanar.i2c.I2CWorkerThread.pushI2CSenderQueue;


public class I2CUtil {

    public static void setAmbianceLedStatus(int state) {
        int[] buf = {0x25, state};
        I2CMessage mes = new I2CMessage(1, buf);
        pushI2CSenderQueue(mes);
        I2CTransfer.setLedState(state);
    }


    public static void askBaseVersion() {
        int[] buf = {0x27};
        I2CMessage mes = new I2CMessage(1, buf);
        pushI2CSenderQueue(mes);

    }

    public static void askAmbianceLedStatus() {
        int[] buf = {0x40};
        I2CMessage mes = new I2CMessage(1, buf);
        pushI2CSenderQueue(mes);
    }


    public static int readIndoor() {
        return I2CTransfer.readIndoor();
    }







}
