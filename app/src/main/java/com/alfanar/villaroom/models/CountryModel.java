package com.alfanar.villaroom.models;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class CountryModel {
    String emoji;
    String emojiU;
    String nativeName;
    String name;
    String countryCode;
    ArrayList<CityModel> cityList;
    int index;

    public CountryModel(String emoji, String emojiU, String nativeName, String name, ArrayList<CityModel> cityList, int index) {
        this.emoji = emoji;
        this.emojiU = emojiU;
        this.nativeName = nativeName;
        this.name = name;
        this.cityList = cityList;
        this.index = index;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }


    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getEmojiU() {
        return emojiU;
    }

    public void setEmojiU(String emojiU) {
        this.emojiU = emojiU;
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public ArrayList<CityModel> getCityList() {
        return cityList;
    }

    public void setCityList(ArrayList<CityModel> cityList) {
        this.cityList = cityList;
    }

    @NonNull
    @Override
    public String toString() {
        return getEmoji() + " " + getName() + " (" + getNativeName() + ")";
    }
}
