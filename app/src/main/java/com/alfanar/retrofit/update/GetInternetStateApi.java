package com.alfanar.retrofit.update;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.retrofit.ApiUpdate;
import com.alfanar.retrofit.cloud.RetrofitCloudClient;
import com.alfanar.villaroom.App;
import com.alfanar.villaroom.models.ControlServiceModel;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetInternetStateApi extends Thread {
    @Override
    public void run() {
        super.run();

        if (isNetworkConnected(App.getInstance())){
            MyUtils.getInstance().internetActive =  probe204();
        }else{
            MyUtils.getInstance().internetActive = false;
        }
    }


    private boolean isNetworkConnected(Context ctx) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private  boolean probe204() {
        HttpURLConnection urlc = null;
        try {
            URL url = new URL("https://clients3.google.com/generate_204");
            urlc = (HttpURLConnection) url.openConnection();

            urlc.setInstanceFollowRedirects(false); // captive portal 302 yakalamak için
            urlc.setUseCaches(false);
            urlc.setRequestMethod("GET");

            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");

            urlc.setConnectTimeout(5000);
            urlc.setReadTimeout(5000);

            int code = urlc.getResponseCode();
            Log.d("InternetChecker", "probe code=" + code);

            return code >= 200 && code < 400;

        } catch (IOException e) {
            Log.d("InternetChecker", "probe exception=" + e);
            return false;
        } finally {
            if (urlc != null) urlc.disconnect();
        }
    }
}
