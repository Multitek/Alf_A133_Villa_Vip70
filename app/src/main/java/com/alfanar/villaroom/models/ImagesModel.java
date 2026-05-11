package com.alfanar.villaroom.models;

public class ImagesModel {
    private long id;
    private String path;
    private String time;
    private String macAddress;

    public ImagesModel(long id, String path, String time, String macAddress) {
        this.id = id;
        this.path = path;
        this.time = time;
        this.macAddress = macAddress;
    }

    public ImagesModel(String path, String time, String macAddress) {
        this.path = path;
        this.time = time;
        this.macAddress = macAddress;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
