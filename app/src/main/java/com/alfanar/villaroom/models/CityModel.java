package com.alfanar.villaroom.models;

public class CityModel {
    int id;
    int index;
    String name;
    String country;


    public CityModel(int id, String name, String country, int index) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.index = index;

    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return getName();
    }
}
