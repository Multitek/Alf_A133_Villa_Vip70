package com.alfanar.villaroom.activities.settings;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentSettingsDeviceNameBinding;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.sockets.MulticastPublisher;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;

public class FragmentDeviceName extends Fragment {
    FragmentSettingsDeviceNameBinding binding;
    private SharedPreferences sp;
    private Dialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSettingsDeviceNameBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        sp = MyUtils.getInstance().getShared();

        binding.edDeviceName.setText(sp.getString("DEVICE_NAME", getResources().getString(R.string.item_room)));


        binding.cardSave.setOnClickListener(v -> {
            String devName = binding.edDeviceName.getText().toString();
            if (devName.isEmpty() || devName.length() > 20) {
                int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
                int[] colors = new int[]{App.getInstance().getColor(R.color.red_new)};
                ColorStateList colorStateList = new ColorStateList(states, colors);
                binding.textInputDeviceName.setErrorEnabled(true);
                binding.textInputDeviceName.setError(getResources().getString(R.string.check_input));
                binding.textInputDeviceName.setErrorTextColor(colorStateList);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    binding.textInputDeviceName.setHint(getResources().getString(R.string.device_name));
                    binding.textInputDeviceName.setError(null);
                }, 2500);

            } else {
                if (!MyUtils.getInstance().ethernetState || MyUtils.getInstance().getIpAddress().equals("192.168.256.256")) {
                    displayDialog(App.getInstance().getResources().getString(R.string.error_network_unreachable));
                } else {
                    sp.edit().putString("DEVICE_NAME", devName).apply();
                    GeneralModel mes = new GeneralModel("", "");
                    mes.setType(AppEnums.SET_DEVICE_STATES.name());
                    DeviceModel deviceModel = new DeviceModel();
                    deviceModel.setIp(MyUtils.getInstance().getIpAddress());
                    deviceModel.setMac(MyUtils.getInstance().getMACAddress());
                    deviceModel.setBaseVer(MyUtils.getInstance().getBaseVer());
                    deviceModel.setName(devName);
                    deviceModel.setMaster(sp.getBoolean("IS_MASTER", false));
                    deviceModel.setLocationId("");
                    deviceModel.setState(true);
                    deviceModel.setType(AppEnums.ROOM.name());
                    mes.setData(new Gson().toJson(deviceModel));
                     MulticastPublisher.send(new Gson().toJson(mes));
                    MyUtils.getInstance().hideKeyboard(requireActivity());
                    displayDialog(App.getInstance().getResources().getString(R.string.saved));
                }
            }
        });

        return view;
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
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
