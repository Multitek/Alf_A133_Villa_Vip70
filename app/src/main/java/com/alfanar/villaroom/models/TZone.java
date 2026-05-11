package com.alfanar.villaroom.models;

public class TZone {
    private int index;
    private String olsId;
    private String gmtName;

    public TZone(int index, String olsId, String gmtName) {
        this.index = index;
        this.olsId = olsId;
        this.gmtName = gmtName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getOlsId() {
        return olsId;
    }

    public void setOlsId(String olsId) {
        this.olsId = olsId;
    }

    public String getGmtName() {
        return gmtName;
    }

    public void setGmtName(String gmtName) {
        this.gmtName = gmtName;
    }

    @Override
    public String toString() {
        return gmtName;
    }
}
