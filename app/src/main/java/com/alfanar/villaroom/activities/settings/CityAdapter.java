package com.alfanar.villaroom.activities.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.databinding.CityItemBinding;
import com.alfanar.villaroom.models.CityModel;
import com.alfanar.villaroom.util.MyUtils;

import java.util.ArrayList;
import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityHolder> {

    private final CityChangeListener cityChangeListener;
    private final Context context;
    private final List<CityModel> cityList;
    private final List<CityModel> filteredList; // For search filtering

    public CityAdapter(List<CityModel> itemList, Context c, CityChangeListener listener) {
        this.cityList = itemList;
        this.filteredList = new ArrayList<>(itemList); // Copy original list
        this.context = c;
        this.cityChangeListener = listener;
    }

    @NonNull
    @Override
    public CityHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CityItemBinding itemBinding = CityItemBinding.inflate(LayoutInflater.from(context), null, false);
        return new CityHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull CityHolder holder, int position) {
        final CityModel city = filteredList.get(position);
        holder.rootBinding.itemText.setText(city.getName().trim());
        holder.rootBinding.itemText.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            cityChangeListener.onCityChanged(city);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Filter method for search
    public void filter(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(cityList); // Show all if search is empty
        } else {
            for (CityModel item : cityList) {
                if (item.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged(); // Refresh RecyclerView
    }


}


