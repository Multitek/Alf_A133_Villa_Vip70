package com.alfanar.villaroom.activities.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentWeatherSettingsBinding;
import com.alfanar.villaroom.models.CityModel;
import com.alfanar.villaroom.models.CountryModel;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.SpacesItemDecoration;
import com.jaredrummler.materialspinner.MaterialSpinner;


public class FragmentWeather extends Fragment implements CityChangeListener {


    CityAdapter cityAdapter;
    FragmentWeatherSettingsBinding binding;
    //
    private SharedPreferences sp;
    private CountryModel selectedCountry;
    private SettingsActivity activity;
    private Dialog dialog;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = ((SettingsActivity) getActivity());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentWeatherSettingsBinding.inflate(inflater, container, false);


        sp = MyUtils.getInstance().getShared();


        //binding.includeTop.imgBack.setOnClickListener(v -> WeatherSettingsActivity.this.finish());


        //binding.includeTop.textTitle.setText(getResources().getString(R.string.weather_settings));

        MainTimeout.getInstance().setTimeout(120 * 1000);


        int countryIndex = sp.getInt("weather_country_index", 44);
        int cityIndex = sp.getInt("weather_city_index", 107);
        selectedCountry = MyUtils.weatherCountryList.get(countryIndex);


        binding.spinnerCountries.setItems(MyUtils.weatherCountryList);
        binding.spinnerCountries.setOnItemSelectedListener((MaterialSpinner.OnItemSelectedListener<CountryModel>) (materialSpinner, pos, id, countryModel) -> {
            selectedCountry = countryModel;
            refreshCities(countryModel);
        });


        //SearchView mSearchView = (SearchView) findViewById(R.id.searchview);


        int searchSrcTextId = getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = binding.searchView.findViewById(searchSrcTextId);
        searchEditText.setTextColor(Color.WHITE); // set the text color

        int id = getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchIcon = binding.searchView.findViewById(id);
        searchIcon.setColorFilter(Color.RED);

        int id2 = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView searchIcon2 = binding.searchView.findViewById(id2);
        searchIcon2.setColorFilter(Color.WHITE);


        binding.searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                MyUtils.getInstance().hideKeyboard(activity);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                cityAdapter.filter(newText);
                return false;
            }


        });
        binding.spinnerCountries.setOnClickListener(v -> MyUtils.getInstance().hideKeyboard(activity));
        binding.rootLayout.setOnClickListener(v -> MyUtils.getInstance().hideKeyboard(activity));


        binding.spinnerCountries.setSelectedIndex(countryIndex);
        binding.txtCurrentCity.setText(MyUtils.weatherCountryList.get(countryIndex).getCityList().get(cityIndex).getName());

        cityAdapter = new CityAdapter(MyUtils.weatherCountryList.get(countryIndex).getCityList(), activity, this);
        binding.recycleCity.setItemAnimator(new DefaultItemAnimator());
        binding.recycleCity.setItemViewCacheSize(-1);
        binding.recycleCity.addItemDecoration(new SpacesItemDecoration(MyUtils.getInstance().dpToPx(5)));
        binding.recycleCity.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.recycleCity.setAdapter(cityAdapter);


        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    private void refreshCities(CountryModel countryModel) {
        cityAdapter = new CityAdapter(countryModel.getCityList(), getActivity(), this);
        binding.recycleCity.setAdapter(cityAdapter);
    }


    private void displayDialog(String text) {


        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = MyUtils.getInstance().dialogPublic(getActivity(), R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

    }


    @Override
    public void onCityChanged(CityModel cityModel) {
        MyUtils.getInstance().hideKeyboard(activity);
        binding.txtCurrentCity.setText(cityModel.getName());
        sp.edit().putInt("weather_city_index", cityModel.getIndex()).commit();
        sp.edit().putInt("weather_country_index", selectedCountry.getIndex()).commit();
        displayDialog(getString(R.string.saved));
        if (MyUtils.getInstance().weatherListener != null) {
            MyUtils.getInstance().weatherListener.onWeatherLocationChange();
        }
    }
}
