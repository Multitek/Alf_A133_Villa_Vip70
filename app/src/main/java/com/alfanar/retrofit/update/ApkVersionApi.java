package com.alfanar.retrofit.update;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.dto.DtoApkModel;
import com.alfanar.retrofit.ApiUpdate;
import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.util.Logger;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApkVersionApi extends Thread {

    @Override
    public void run() {
        super.run();

        try {
            ApiUpdate apiUpdate = RetrofitUpdateClient.getClient().create(ApiUpdate.class);
            DtoApkModel mm = new DtoApkModel(String.valueOf(BuildConfig.APP_ID), 0, 0);
            Call<DtoApkModel> call2 = apiUpdate.getApk(mm);

            Logger.d("ApkVersionApi3.start");
            call2.enqueue(new Callback<DtoApkModel>() {
                @Override
                public void onResponse(@NonNull Call<DtoApkModel> call, @NonNull Response<DtoApkModel> response) {
                    Logger.d("ApkVersionApi.onResponse " + response);
                    if (response.isSuccessful()) {
                        DtoApkModel model = response.body();
                        if (model != null) {
                            Logger.d("ApkVersionApi.onResponse  = " + model);
                            if (model.getVersioncode() > BuildConfig.VERSION_CODE) {
                                Logger.d("ApkVersionApi.onResponse new Application will download " + model.getUrl());
                                new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        downloadApplication(model);
                                    }
                                }.start();
                            } else {
                                Logger.d("ApkVersionApi.onResponse application is up-to-date");
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<DtoApkModel> call, @NonNull Throwable t) {
                    Logger.d("ApkVersionApi.onFailure ");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void downloadApplication(DtoApkModel model) {
        try {
            int fileLen = model.getFile_len();
            Logger.d("ApkVersionApi.downloadApplication start fileLen = " + fileLen);

            int CONNECT_TIMEOUT = 10000;
            int READ_TIMEOUT = 30000;

            File f = new File("mnt/sdcard/newApplication.apk");
            long start = System.currentTimeMillis();
            FileUtils.copyURLToFile(new URL(model.getUrl()), f, CONNECT_TIMEOUT, READ_TIMEOUT);
            long end = System.currentTimeMillis();
            long total = (end - start) / 1000;
            long downloadedLen = f.length();

            Logger.d("ApkVersionApi.downloadApplication done " + f.length() + " byte, download in  " + total + "second ,  " + f.length() / 1024 / total + "kb/s");

            if (downloadedLen == fileLen) {
                Logger.d("ApkVersionApi.downloadApplication success download, newApplication will be install");
                installApp();
            }

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            Logger.d("ApkVersionApi.downloadApplication Exception = " + e.getMessage());
        }
    }

    private void installApp() {
        try {
            Logger.d("ApkVersionApi.installApp ");
            File fd = new File("mnt/sdcard/newApplication.apk");
            if (fd.exists()) {
                Uri uri = Uri.fromFile(fd);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                App.getInstance().startActivity(intent);
            } else {
                Logger.d("ApkVersionApi.installApp file not found !!!!! ");
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            Logger.d("ApkVersionApi.installApp.Exception =  " + e.getMessage());
        }
    }
}
