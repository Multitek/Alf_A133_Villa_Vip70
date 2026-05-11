package com.alfanar.i2c;

public class HexBinaryUtil {
    /*
     *return decimal  to binary in string form
     */
    public static String decimalToBinaryString(int val) {
        String preBin = Integer.toBinaryString(val);
        Integer length = preBin.length();
        if (length < 8) {
            for (int i = 0; i < 8 - length; i++) {
                preBin = "0" + preBin;
            }
        }
        return preBin;
    }

    /*
     *return binary string to integer
     */
    static int binaryToInt(String bin) {
        return Integer.parseInt(bin, 2);
    }

    /*
     * return a String in hex form , convert int array to String
     */
    public static String convertIntToHexString(int[] data) {
        StringBuilder sb = new StringBuilder();
        for (int b : data) {
            sb.append(String.format("0x%02X;", b & 0xFF));
        }
        return sb.toString();
    }

    public static String convertByteToHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("0x%02X;", b & 0xFF));
        }
        return sb.toString();
    }

    public static int[] convertIntArray(byte[] byteArray){
        int[] arr = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            arr[i] =  ((int) byteArray[i]) & 0xFF;
        }
        return arr;
    }
}
