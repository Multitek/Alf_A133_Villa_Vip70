package com.alfanar.villaroom.models;

public class CameraDevice {

    private String address;
    private String name;
    private int id;

    public CameraDevice(int id, String name, String address) {
        this.address = address;
        this.name = name;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getId() + "_" + getName() + "_" + getAddress();
    }
}
