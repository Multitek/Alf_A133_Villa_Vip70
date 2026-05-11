package com.alfanar.dto;

public class DtoApkModel {
    String url;
    int versioncode;
    int file_len;

    public DtoApkModel(String url, int versioncode, int file_len) {
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

    public int getVersioncode() {
        return versioncode;
    }

    public void setVersioncode(int versioncode) {
        this.versioncode = versioncode;
    }

    public int getFile_len() {
        return file_len;
    }

    public void setFile_len(int file_len) {
        this.file_len = file_len;
    }
}
