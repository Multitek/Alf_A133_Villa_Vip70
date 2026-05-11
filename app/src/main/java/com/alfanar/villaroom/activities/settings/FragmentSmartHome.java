package com.alfanar.villaroom.activities.settings;

import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentSettingsSmarthomeBinding;
import com.alfanar.villaroom.util.MyUtils;

public class FragmentSmartHome extends Fragment {

    private static final int REQUEST_CODE = 170;
    FragmentSettingsSmarthomeBinding binding;
    // private CardView btnInstall;
    // private TextView txtFileName;
    // private CardView cardKnx, cardMini, cardPremium;
    // private Resources.Theme themes;
    // private TypedValue storedValueInTheme;
    private Dialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsSmarthomeBinding.inflate(inflater, container, false);


        String autoType = MyUtils.getInstance().getShared().getString("automation_type", "ALFA_NONE");
        switch (autoType) {
            case "ALFA_SMART":
                binding.btnSmart.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                break;
            case "ALFA_PREM":
                binding.btnPremium.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                break;
            case "ALFA_KNX":
                binding.btnKnx.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                break;
            case "ALFA_NONE":
                binding.btnNone.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                break;
        }


        binding.btnSmart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*MyUtils.getInstance().getShared().edit().putString("automation_type", "ALFA_SMART").apply();

                binding.btnSmart.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                binding.btnNone.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnKnx.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnPremium.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                displayDialog(requireActivity().getString(R.string.saved));*/


                displayDialog(requireActivity().getString(R.string.not_supported));
            }
        });

        binding.btnPremium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.getInstance().getShared().edit().putString("automation_type", "ALFA_PREM").apply();

                binding.btnPremium.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                binding.btnNone.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnSmart.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnKnx.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                displayDialog(requireActivity().getString(R.string.saved));

            }
        });


        binding.btnKnx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*MyUtils.getInstance().getShared().edit().putString("automation_type", "ALFA_KNX").apply();

                binding.btnKnx.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                binding.btnNone.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnSmart.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnPremium.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                displayDialog(requireActivity().getString(R.string.saved));*/

                displayDialog(requireActivity().getString(R.string.not_supported));

            }
        });

        binding.btnNone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.getInstance().getShared().edit().putString("automation_type", "ALFA_NONE").apply();

                binding.btnNone.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_active, 0, 0, 0);
                binding.btnKnx.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnSmart.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                binding.btnPremium.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.radio_passive, 0, 0, 0);
                displayDialog(requireActivity().getString(R.string.saved));
            }
        });


        return binding.getRoot();
    }

    private void selectSmart(String type) {

    }


    public boolean isPackageExisted(String targetPackage) {
        PackageManager pm = requireActivity().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }


    private void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(getActivity(), R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


}