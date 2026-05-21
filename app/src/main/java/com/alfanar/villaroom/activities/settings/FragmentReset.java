package com.alfanar.villaroom.activities.settings;

import static android.widget.Toast.LENGTH_LONG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.alfanar.i2c.I2CUtil;
import com.alfanar.villaroom.App;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentSettingsResetBinding;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.sockets.MulticastPublisher;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;

import org.apache.commons.lang3.RandomUtils;
import org.linphone.LinphoneManager;

import java.util.Objects;

public class FragmentReset extends Fragment {
    FragmentSettingsResetBinding binding;
    private Dialog dialog;
    private Activity context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsResetBinding.inflate(inflater, container, false);

        if (getActivity() != null) {
            context = getActivity();
        }


        binding.cardReset.setOnClickListener(v -> displayDialog(context.getResources().getString(R.string.factory_reset)));


        boolean staticMode = MyUtils.getInstance().getShared().getBoolean("STATIC_IP_MODE", false);


        if (staticMode) {
            binding.txtIpMode.setText(context.getResources().getString(R.string.staticIpMode));
        } else {
            binding.txtIpMode.setText(context.getResources().getString(R.string.dhcpModeMode));
        }


        binding.txtIpMode.setPaintFlags(binding.txtIpMode.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        binding.cardChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                displayModeDialog();
            }
        });


        return binding.getRoot();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @SuppressLint("ApplySharedPref")
    private void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(getActivity(), R.layout.dialog_yes_no);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button yes = dialog.findViewById(R.id.btnYes);
        yes.setOnClickListener(v -> {
            boolean currentStaticMode = MyUtils.getInstance().getShared().getBoolean("STATIC_IP_MODE", false);
            MyUtils.getInstance().getShared().edit().clear().commit();
            DatabaseHelper.getInstance().resetFactory();
            I2CUtil.setAmbianceLedStatus(0);
            MyUtils.getInstance().getShared().edit().putBoolean("baseLed", false).apply();
            MyUtils.getInstance().getShared().edit().putBoolean("STATIC_IP_MODE", currentStaticMode).commit();

            dialog.dismiss();
            MyUtils.getInstance().restartApp();
        });
        Button no = dialog.findViewById(R.id.btnNo);
        no.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void displayModeDialog() {

        boolean currentStaticMode = MyUtils.getInstance().getShared().getBoolean("STATIC_IP_MODE", false);
        boolean toStaticMode = !currentStaticMode;

        dialog = MyUtils.getInstance().dialogPublic(getActivity(), R.layout.dialog_yes_no);
        int messageResId = toStaticMode ? R.string.StaticIpDetail : R.string.DhcpIpDetail;

        String message = context.getString(messageResId);

        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(message);
        Button yes = dialog.findViewById(R.id.btnYes);
        yes.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(v,1000);
            if (toStaticMode) {
                switchToStaticIp();
            } else {
                switchToDhcpIp();
            }
        });
        Button no = dialog.findViewById(R.id.btnNo);
        no.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void switchToStaticIp() {

        binding.txtIpMode.setText(context.getResources().getString(R.string.staticIpMode));
         MyUtils.getInstance().dhcpOff2(App.getInstance());
        Toast.makeText(getActivity(), requireActivity().getResources().getString(R.string.saving),LENGTH_LONG).show();
        MyUtils.getInstance().backToRootActivity();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            MyUtils.getInstance().restartApp3();
        },5000);
    }

    private void switchToDhcpIp() {
        binding.txtIpMode.setText(context.getResources().getString(R.string.dhcpModeMode));
        MyUtils.getInstance().dhcpOn(App.getInstance());
        MyUtils.getInstance().backToRootActivity();
        Toast.makeText(getActivity(), requireActivity().getResources().getString(R.string.saving),LENGTH_LONG).show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            MyUtils.getInstance().restartApp3();
        },5000);
    }

}
