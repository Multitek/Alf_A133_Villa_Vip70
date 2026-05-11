package com.alfanar.villaroom.activities.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.databinding.FragmentSettingsShowManualBinding;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

public class FragmentManuel extends Fragment {


    FragmentSettingsShowManualBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsShowManualBinding.inflate(inflater, container, false);

        displayFromAsset();

        return binding.getRoot();
    }

    private void displayFromAsset() {
        String fileName = "user_manuel.pdf";
        binding.pdfView.fromAsset(fileName).defaultPage(0).enableAnnotationRendering(true).scrollHandle(new DefaultScrollHandle(getActivity())).spacing(1) // in dp
                .load();

        binding.pdfView.fitToWidth(0);
    }


}
