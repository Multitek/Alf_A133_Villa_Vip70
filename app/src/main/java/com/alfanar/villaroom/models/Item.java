package com.alfanar.villaroom.models;

public class Item implements Comparable<Item> {
    private final String name;
    private final String data;
    private final String date;
    private final String path;
    private final String image;

    public Item(String n, String d, String dt, String p, String img) {
        name = n;
        data = d;
        date = dt;
        path = p;
        image = img;
    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public String getDate() {
        return date;
    }

    public String getPath() {
        return path;
    }

    public String getImage() {
        return image;
    }

    public int compareTo(Item o) {
        if (this.name != null) return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else throw new IllegalArgumentException();
    }
}
