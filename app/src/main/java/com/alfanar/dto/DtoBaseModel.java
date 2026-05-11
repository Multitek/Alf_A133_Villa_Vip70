package com.alfanar.dto;

public class DtoBaseModel {
    String url;
    double versioncode;
    int file_len;

    public DtoBaseModel(String url, double versioncode, int file_len) {
        this.url = url;
        this.versioncode = versioncode;
        this.file_len = file_len;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public double getVersioncode() {
        return versioncode;
    }

    public void setVersioncode(double versioncode) {
        this.versioncode = versioncode;
    }

    public int getFile_len() {
        return file_len;
    }

    public void setFile_len(int file_len) {
        this.file_len = file_len;
    }
}
