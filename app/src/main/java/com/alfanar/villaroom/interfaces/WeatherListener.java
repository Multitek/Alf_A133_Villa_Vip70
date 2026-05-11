package com.alfanar.villaroom.interfaces;

public interface WeatherListener {
    void onWeather(String temp, String tempMin, String tempMax, String description, String iconId);

    void onWeatherException(int result);

    void onWeatherLocationChange();
}
