package com.alfanar.villaroom.models;

import java.util.ArrayList;

public class CountryListModel {

    public ArrayList<CountryModel> countryList;

    public CountryListModel() {
        // Required for Gson deserialization
    }

    public CountryListModel(ArrayList<CountryModel> countryList) {
        this.countryList = countryList;
    }

    public ArrayList<CountryModel> getCountryList() {
        return countryList;
    }

    public void setCountryList(ArrayList<CountryModel> countryList) {
        this.countryList = countryList;
    }
}