package com.alfanar.villaroom.models;

public class MenuModel {
    private String itemId;
    private String itemTag;
    private int itemPosition;
    private boolean itemVisibility;
    private int itemIconId;
    private String itemName;

    public MenuModel(String id, String tag, int position, boolean visibility) {
        this.itemId = id;
        this.itemVisibility = visibility;
        this.itemTag = tag;
        this.itemPosition = position;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getItemIconId() {
        return itemIconId;
    }

    public void setItemIconId(int itemIconId) {
        this.itemIconId = itemIconId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public boolean isItemVisibility() {
        return itemVisibility;
    }

    public void setItemVisibility(boolean itemVisibility) {
        this.itemVisibility = itemVisibility;
    }

    public String getItemTag() {
        return itemTag;
    }

    public void setItemTag(String itemTag) {
        this.itemTag = itemTag;
    }

    public int getItemPosition() {
        return itemPosition;
    }

    public void setItemPosition(int itemPosition) {
        this.itemPosition = itemPosition;
    }
}
