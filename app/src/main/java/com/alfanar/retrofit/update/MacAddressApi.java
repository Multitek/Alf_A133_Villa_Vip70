package com.alfanar.retrofit.update;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.dto.DtoMacAddressModel;
import com.alfanar.retrofit.ApiUpdate;
import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MacAddressApi extends Thread {

    int delay;

    public MacAddressApi(int delay) {
        this.delay = delay;
    }

    @Override
    public void run() {
        super.run();

        try {
            mSleep(delay);
            ApiUpdate apiMac = RetrofitUpdateClient.getClient().create(ApiUpdate.class);

            DtoMacAddressModel macAddrModel = new DtoMacAddressModel();
            macAddrModel.setCpu(MyUtils.getCpuSerial());
            macAddrModel.setBaseVer(MyUtils.getInstance().getBaseVer());
            macAddrModel.setFw_version(Build.DISPLAY);
            macAddrModel.setBrand(BuildConfig.APP_BRAND);
            macAddrModel.setAppName(BuildConfig.APP_NAME + "_" + BuildConfig.VERSION_NAME);
            macAddrModel.setAppVer(BuildConfig.VERSION_CODE + "");
            macAddrModel.setAppId(BuildConfig.APP_ID);
            macAddrModel.setAddr(MyUtils.getInstance().getMACAddress());

            Call<DtoMacAddressModel> call = apiMac.getMacByCpu(macAddrModel);
            call.enqueue(new Callback<DtoMacAddressModel>() {
                @Override
                public void onResponse(@NonNull Call<DtoMacAddressModel> call, @NonNull Response<DtoMacAddressModel> response) {
                    Logger.d("MacAddressApi.onResponse1 " + response);
                    if (response.isSuccessful()) {
                        DtoMacAddressModel model = response.body();
                        if (model != null) {
                            Logger.d("MacAddressApi.onResponse2 = " + model.getAddr());

                            if (isMacAssigned()) {

                            } else {
                                writeMac(model.getAddr());
                            }


                        } else {
                            if (!isMacAssigned()) {
                                new MacAddressApi(60).start();
                            }
                        }
                    } else {
                        if (!isMacAssigned()) {
                            new MacAddressApi(60).start();
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<DtoMacAddressModel> call, @NonNull Throwable t) {
                    Logger.d("MacAddressApi failure1 " + t.getMessage());
                    if (!isMacAssigned()) {
                        new MacAddressApi(60).start();
                    }
                }
            });
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    private boolean isMacAssigned() {
        return MyUtils.getInstance().getMACAddress().toUpperCase().startsWith("4C:4B:F9");
    }

    private void writeMac(String macAddr) {
        try {

            boolean valid = isValidMACAddress(macAddr);
            if (valid) {
                File f2 = new File("data", "mac5.txt");
                if (f2.exists()) {
                    FileUtils.write(f2, macAddr, Charset.defaultCharset(), false);
                        GeneralMediaPlayer.getInstance().playMedia(8);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent("COM.MULTITEK.REBOOT");
                                i.addFlags(Intent.FLAG_FROM_BACKGROUND);
                                App.getInstance().sendBroadcast(i);

                            }
                        },5000);

                } else {
                    Logger.d("MacAddressApi.writeMac MAC.TXT FILE_NOT_EXIST");
                }
            } else {
                Logger.d("MacAddressApi.writeMac NOT_VALID_MAC_ADDRESS");
            }

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    private boolean isValidMACAddress(String str) {
        // Regex to check valid
        // MAC address
        String regex = "^([0-9A-Fa-f]{2}[:-])" + "{5}([0-9A-Fa-f]{2})|" + "([0-9a-fA-F]{4}\\." + "[0-9a-fA-F]{4}\\." + "[0-9a-fA-F]{4})$";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (str == null) {
            return false;
        }

        // Find match between given string
        // and regular expression
        // uSing Pattern.matcher()

        Matcher m = p.matcher(str);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }




    private void mSleep(int delay) {
        try {
            Thread.sleep(delay * 1000L);
        } catch (InterruptedException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }


}
