package com.alfanar.villaroom.threads;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherControl extends Thread {
    @Override
    public void run() {
        Thread.currentThread().setName("WeatherControl");
        //  int cityId = MyUtils.getInstance().getShared().getInt("deviceLocationCityId", 745044);
        int countryIndex = MyUtils.getInstance().getShared().getInt("weather_country_index", 44);
        int cityIndex = MyUtils.getInstance().getShared().getInt("weather_city_index", 107);
        int cityId = MyUtils.weatherCountryList.get(countryIndex).getCityList().get(cityIndex).getId();

        try {

            if(!MyUtils.getInstance().internetActive) return;

            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build();
            Request request = new Request.Builder().url("http://api.openweathermap.org/data/2.5/weather?id=" + cityId + "&appid=" + App.getInstance().getString(R.string.open_weather_maps_app_id) + "&units=metric").get().build();


            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    e.printStackTrace();
                    if (MyUtils.getInstance().weatherListener != null) {
                        MyUtils.getInstance().weatherListener.onWeatherException(2);
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    String data;
                    if (response.isSuccessful()) {
                        if (response.body() != null) {
                            data = response.body().string();
                            try {
                                JSONObject json = new JSONObject(data);

                                JSONArray weatherObjectArray = json.getJSONArray("weather");
                                JSONObject object = weatherObjectArray.getJSONObject(0);
                                String iconId = object.getString("icon");
                                String description = object.getString("description");


                                JSONObject mainObject = json.getJSONObject("main");
                                String temp = Math.round(mainObject.getDouble("temp")) + "°";
                                String tempMin = Math.round(mainObject.getDouble("temp_min")) + "°";
                                String tempMax = Math.round(mainObject.getDouble("temp_max")) + "°";
                                if (MyUtils.getInstance().weatherListener != null) {
                                    MyUtils.getInstance().weatherListener.onWeather(temp, tempMin, tempMax, description, iconId);
                                }

                            } catch (Exception e) {
                                Logger.e("EXCEPTION", e);
                                if (MyUtils.getInstance().weatherListener != null) {
                                    MyUtils.getInstance().weatherListener.onWeatherException(5);
                                }
                            }
                        } else {
                            if (MyUtils.getInstance().weatherListener != null) {
                                MyUtils.getInstance().weatherListener.onWeatherException(5);
                            }
                        }
                    } else {
                        if (MyUtils.getInstance().weatherListener != null) {
                            MyUtils.getInstance().weatherListener.onWeatherException(6);
                        }
                    }

                    response.close();

                }

            });
        } catch (Exception e) {
            Logger.e("EXCEPTION", e);
            if (MyUtils.getInstance().weatherListener != null) {
                MyUtils.getInstance().weatherListener.onWeatherException(1);
            }
        }
    }

}