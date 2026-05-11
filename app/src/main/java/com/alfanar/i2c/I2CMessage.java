package com.alfanar.i2c;


public final class I2CMessage {
    private final int mode;
    private final int[] data;

    public I2CMessage(int mode, int[] data) {
        this.mode = mode;
        this.data = data;
    }

    public int mode()   { return mode; }
    public int[] data() { return data; }
}