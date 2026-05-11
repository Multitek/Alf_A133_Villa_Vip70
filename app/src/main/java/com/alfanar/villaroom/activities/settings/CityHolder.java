package com.alfanar.villaroom.activities.settings;

import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.databinding.CityItemBinding;


public class CityHolder extends RecyclerView.ViewHolder {
    CityItemBinding rootBinding;

    CityHolder(CityItemBinding r) {
        super(r.getRoot());
        this.rootBinding = r;
    }
}