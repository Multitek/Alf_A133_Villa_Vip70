package com.alfanar.dto;

public class DtoMacAddressModel {
    String fw_version;
    String baseVer;
    String appName;
    String appVer;
    String brand;
    String cpu;
    String addr;
    int appId;

    public DtoMacAddressModel(String fw_version, String baseVer, String appName, String appVer, String brand, String cpu, int appId) {
        this.fw_version = fw_version;
        this.baseVer = baseVer;
        this.appName = appName;
        this.appVer = appVer;
        this.brand = brand;
        this.cpu = cpu;
        this.appId = appId;
    }

    public DtoMacAddressModel() {
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getFw_version() {
        return fw_version;
    }

    public void setFw_version(String fw_version) {
        this.fw_version = fw_version;
    }

    public String getBaseVer() {
        return baseVer;
    }

    public void setBaseVer(String baseVer) {
        this.baseVer = baseVer;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }
}
