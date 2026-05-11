package com.alfanar.villaroom.models;

public class DeviceModel {
    private String ip = "";
    private String mac = "";
    private String name = "";
    private String sip = "";
    private String locationId = "";
    private String type = ""; // ROOM or DOOR
    private boolean state;
    private boolean master;
    private String appVer = "";
    private String fwVer = "";
    private String baseVer = "";
    private int relayCount = 1;
    private String relay1Name = "";
    private String relay2Name = "";
    private boolean staticIp =false;

    public String getBaseVer() {
        return baseVer;
    }

    public void setBaseVer(String baseVer) {
        this.baseVer = baseVer;
    }

    public String getAppVer() {
        return appVer;
    }

    public void setAppVer(String appVer) {
        this.appVer = appVer;
    }

    public String getFwVer() {
        return fwVer;
    }

    public void setFwVer(String fwVer) {
        this.fwVer = fwVer;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public String getSip() {
        return sip;
    }

    public void setSip(String sip) {
        this.sip = sip;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public int getRelayCount() {
        return relayCount;
    }

    public void setRelayCount(int relayCount) {
        this.relayCount = relayCount;
    }

    public String getRelay1Name() {
        return relay1Name;
    }

    public void setRelay1Name(String relay1Name) {
        this.relay1Name = relay1Name;
    }

    public String getRelay2Name() {
        return relay2Name;
    }

    public void setRelay2Name(String relay2Name) {
        this.relay2Name = relay2Name;
    }


    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public boolean isStaticIp() {
        return staticIp;
    }

    public void setStaticIp(boolean staticIp) {
        this.staticIp = staticIp;
    }
}
