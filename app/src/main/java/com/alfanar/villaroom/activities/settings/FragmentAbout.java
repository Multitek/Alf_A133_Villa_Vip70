package com.alfanar.villaroom.activities.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.text.Html;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentSettingsAboutBinding;
import com.alfanar.villaroom.util.MyUtils;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class FragmentAbout extends Fragment {
    FragmentSettingsAboutBinding binding;
    private int count = 0;
    private SettingsActivity activity;

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
        binding = FragmentSettingsAboutBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        count = 0;

        //Typeface tf1 = ResourcesCompat.getFont(requireActivity(), R.font.tomorrowlight);
        Typeface tf2 = ResourcesCompat.getFont(requireActivity(), R.font.tomorrowlight);

        binding.txtAppVers.setTypeface(tf2);
        binding.txtBaseVers.setTypeface(tf2);
        binding.txtFwVers.setTypeface(tf2);
        binding.txtIpAddr.setTypeface(tf2);
        binding.txtCpu.setTypeface(tf2);

        binding.txtAppVers.setText(MyUtils.getInstance().getAppVer());
        binding.txtBaseVers.setText(MyUtils.getInstance().getBaseVer());
        binding.txtFwVers.setText(MyUtils.getInstance().getFwVer());
        binding.txtIpAddr.setText(String.format( Locale.US,"%s / %s", MyUtils.getInstance().getIpAddress(), MyUtils.getInstance().getMACAddress()));
        binding.txtCpu.setText(MyUtils.getCpuSerial());


       /* MyUtils.getInstance().setTextEn(binding.txtAppVers, MyUtils.getInstance().getAppVer());
        MyUtils.getInstance().setTextEn(binding.txtBaseVers, MyUtils.getInstance().getBaseVer());
        MyUtils.getInstance().setTextEn(binding.txtFwVers, MyUtils.getInstance().getFwVer());
        MyUtils.getInstance().setTextEn(binding.txtIpAddr, String.format( Locale.US,"%s / %s", MyUtils.getInstance().getIpAddress(), MyUtils.getInstance().getMACAddress()));
        MyUtils.getInstance().setTextEn(binding.txtCpu, Build.SERIAL);*/



        binding.txtAppVers.setOnClickListener(v -> {
            count++;
            if (count == 7) {
                activity.passAndroidScreen();
            }
        });


        if (MyUtils.getInstance().internetActive) {
            binding.txtInternetState.setText(getString(R.string.yes));
        } else {
            binding.txtInternetState.setText(getString(R.string.no));
        }

        return view;
    }



}
