package com.alfanar.villaroom.models;

public class Camera2Model {
    int id;
    String name;
    String ip;
    String userName;
    String password;

    public Camera2Model(int id, String name, String ip, String userName, String password) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.userName = userName;
        this.password = password;
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
