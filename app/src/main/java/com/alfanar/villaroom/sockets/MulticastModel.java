package com.alfanar.villaroom.sockets;

public class MulticastModel {
    String data;
    String ip;

    public MulticastModel(String data, String ip) {
        this.data = data;
        this.ip = ip;
    }

    public String getData() {
        return data;
    }

    public String getIp() {
        return ip;
    }
}
