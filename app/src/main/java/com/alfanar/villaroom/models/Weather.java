package com.alfanar.villaroom.models;

public class Weather {

    private int temperture;
    private String decription;
    private long iconID;
    private long backID;

    public int getTemperture() {
        return temperture;
    }

    public void setTemperture(int temperture) {
        this.temperture = temperture;
    }

    public String getDecription() {
        return decription;
    }

    public void setDecription(String decription) {
        this.decription = decription;
    }

    public long getIconID() {
        return iconID;
    }

    public void setIconID(long iconID) {
        this.iconID = iconID;
    }

    public long getBackID() {
        return backID;
    }

    public void setBackID(long backID) {
        this.backID = backID;
    }
}
