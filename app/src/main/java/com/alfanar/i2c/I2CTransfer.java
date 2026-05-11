package com.alfanar.i2c;




import static com.alfanar.i2c.HexBinaryUtil.convertByteToHexString;

import android.os.Handler;
import android.os.Looper;

import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.util.MyUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

public class I2CTransfer {


    private static boolean indoorFlag=true;

    static {
        System.loadLibrary("I2CTransfer");
    }

    private static native int i2cWrite(byte[] out);

    private static native int i2cRead(byte[] in);


    static native int readIndoor();


    static native void setLedState(int val);



    public static volatile boolean baseDetected=false;


    static private final byte[] sendBytes    = new byte[24];
    static final int PACKET_SIZE = 24, MAX_DATA = 21;
    static final byte END_INDICATOR = (byte) 0xFF;
    static int checksum;


    protected static void write(int[] data) {

        if (data.length > MAX_DATA)
            throw new IllegalArgumentException("data.length must be <= " + MAX_DATA);

        Arrays.fill(sendBytes, (byte) 0);

        IntStream.range(0, data.length)
                .forEach(i -> sendBytes[i + 1] = (byte) (data[i] & 0xFF));


        checksum = IntStream.of(data).sum();

        sendBytes[0]                   = (byte) ((data.length + 1) & 0xFF);
        sendBytes[data.length + 1]     = (byte) (checksum & 0xFF);
        sendBytes[data.length + 2]     = END_INDICATOR;
        sendBytes[PACKET_SIZE - 1]     = (byte) ((data.length + 3) & 0xFF);

        System.out.println("I2CTransferDebug writeBytes = " + convertByteToHexString(sendBytes));
        i2cWrite(sendBytes);
    }


    public static boolean verifyChecksum(byte[] data) {
        if (data == null || data.length < 2) return false;

        int expected = 0;
        for (int i = 0; i < data.length - 1; i++)
            expected = (expected + (data[i] & 0xFF)) & 0xFF; // her adımda 8-bit sınırı

        return (data[data.length - 1] & 0xFF) == expected;
    }

    static byte[] receiveBytes = new byte[15];
    protected static void read() {
        if (i2cRead(receiveBytes) == 15) {
            if (verifyChecksum(receiveBytes)) {
                System.out.println("I2CTransferDebug Checksum OK readBytes  = " + convertByteToHexString(receiveBytes));
                process(convertIntArray(receiveBytes));
            } else {
                System.out.println("I2CTransferDebug.Checksum ERROR");

            }
        }
    }
    private static void process(int[] arr) {
        int code = arr[0];
        baseDetected = true; //mcu detected

        switch (code) {

            case 0x28: // version
                String data1 = String.valueOf(arr[1] - 48);
                String data2 = String.valueOf(arr[2] - 48);
                String data3 = String.valueOf(arr[3] - 48);
                String version = data1 + data2 + "." + data3;
                MyUtils.getInstance().getShared().edit().putString("base_version", version).apply();
                break;

            case 0x24: // ic_zil2


                        if (indoorFlag) {
                            indoorFlag = false;
                            MainActivity.getInstance().indoorRingReceived("gpio");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> indoorFlag = true, 8000);

                        }




                break;

            default:
                break;
        }
    }
    private static   int[] convertIntArray(byte[] byteArray){
        int[] arr = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            arr[i] =  ((int) byteArray[i]) & 0xFF;
        }
        return arr;
    }

}
