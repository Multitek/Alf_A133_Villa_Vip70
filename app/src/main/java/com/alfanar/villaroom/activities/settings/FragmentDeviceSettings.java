package com.alfanar.villaroom.activities.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.alfanar.i2c.I2CUtil;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.databinding.FragmentSettingsDeviceSettingsBinding;
import com.alfanar.villaroom.models.TZone;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FragmentDeviceSettings extends Fragment {
    public boolean firstEnter = true;
    int step = 1;
    private SharedPreferences sp;
   // private TextView languageTitle;
    //private LinearLayout layoutLanguage;
    //private ImageView expandCollapseLanguage;
    private View selectedLayout;
    private ImageView selectedImgExpandCollapse;
    private boolean expandState = true;
    //private ToggleButton baseSwitch;
    private Activity context;
    private Dialog dialog;

    FragmentSettingsDeviceSettingsBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsDeviceSettingsBinding.inflate(inflater);

        if (getActivity() != null) {
            context = getActivity();
        }
        sp = MyUtils.getInstance().getShared();


        binding.layoutLanguageClick.setOnClickListener(v -> setSize(binding.layoutLanguage,  binding.imgExpandCollapseLanguage));

        binding.language2.setOnClickListener(v -> {
            setSize(binding.layoutLanguage,  binding.imgExpandCollapseLanguage);
            changeLanguage(0);
        });


        binding.language3.setOnClickListener(v -> {
            setSize(binding.layoutLanguage,  binding.imgExpandCollapseLanguage);
            changeLanguage(1);
        });
        setLanguage();




        boolean ledState = sp.getBoolean("baseLed", false);
        binding.toggleButton.setChecked(ledState);

        binding.toggleButton.setOnClickListener(v -> {
            boolean isChecked =   binding.toggleButton.isChecked();
            sp.edit().putBoolean("baseLed", isChecked).commit();
            MyUtils.getInstance().hideKeyboard(context);
            I2CUtil.setAmbianceLedStatus(isChecked ? 1 : 0);
            displayDialog(context.getResources().getString(R.string.saved));
        });




        binding.edtPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Logger.d("count = " + s.toString().length());
                if (s.toString().length() == 4) {
                    String currentPass = sp.getString("system_password", "0000");
                    String enteredPass =  binding.edtPass.getText().toString();

                    if (step == 1) {
                        if (enteredPass.equals(currentPass)) {
                            binding.txtPassInfo.setText(getString(R.string.enter_new_password));
                            step = 2;
                        } else {
                            displayDialog(getString(R.string.pass_incorrect));
                        }
                        binding.edtPass.setText("");

                    } else if (step == 2) {
                        sp.edit().putString("system_password", enteredPass).apply();
                        displayDialog(getString(R.string.new_password_accepted));
                        binding.txtPassInfo.setText(getString(R.string.change_password));
                        binding.edtPass.setText("");
                        step = 1;

                        MyUtils.getInstance().hideKeyboard(getActivity());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
       // Spinner spinner = view.findViewById(R.id.spn_zones);

       /* List<TZone> zoneList = new ArrayList<>();
        zoneList.add(new TZone(0, "Etc/GMT+4", "GMT-4"));
        zoneList.add(new TZone(1, "Etc/GMT+3", "GMT-3"));
        zoneList.add(new TZone(2, "Etc/GMT+2", "GMT-2"));
        zoneList.add(new TZone(3, "Etc/GMT+1", "GMT-1"));

        zoneList.add(new TZone(4, "Etc/GMT0", "GMT0"));

        zoneList.add(new TZone(5, "Etc/GMT-1", "GMT+1"));
        zoneList.add(new TZone(6, "Etc/GMT-2", "GMT+2"));
        zoneList.add(new TZone(7, "Etc/GMT-3", "GMT+3"));
        zoneList.add(new TZone(8, "Etc/GMT-4", "GMT+4"));


        ArrayAdapter<TZone> aa = new ArrayAdapter<>(getActivity(), R.layout.custom_spinner_item, zoneList);
        aa.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        binding.spnZones.setAdapter(aa);

        int timeZone = sp.getInt("time_zone_index", 7);
        binding.spnZones.setSelection(timeZone);


        binding.spnZones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstEnter) {
                    firstEnter = false;
                    return;
                }
                TZone zone = zoneList.get(position);
                Logger.d("zone.getGmtName() =" + zone.getGmtName());
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.setTimeZone(zone.getOlsId());
                sp.edit().putInt("time_zone_index", position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
*/

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        MyUtils.getInstance().hideKeyboard(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    private void setLanguage() {
        String currentLangCode = sp.getString("language_code", "en");

        switch (currentLangCode) {
            case "en":
                binding.textLanguageTitle.setText(context.getResources().getString(R.string.language_english));
                break;
            case "ar":
                binding.textLanguageTitle.setText(context.getResources().getString(R.string.language_arabic));
                break;
            default:
                binding.textLanguageTitle.setText(context.getResources().getString(R.string.language_english));
                break;
        }
    }

    private void setSize(View viewLayout, ImageView imgExpandCollapse) {
        if (selectedLayout != null) {
            if (selectedLayout == viewLayout) {
                if (expandState) {
                    expandState = false;
                    MyUtils.getInstance().collapse(viewLayout, 60);
                    imgExpandCollapse.setImageResource(R.drawable.ic_expand);
                } else {
                    expandState = true;
                    MyUtils.getInstance().expand(viewLayout);
                    imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
                }
            } else {
                expandState = true;
                MyUtils.getInstance().expand(viewLayout);
                MyUtils.getInstance().collapse(selectedLayout, 60);
                imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
                selectedImgExpandCollapse.setImageResource(R.drawable.ic_expand);
            }
        } else {
            expandState = true;
            MyUtils.getInstance().expand(viewLayout);
            imgExpandCollapse.setImageResource(R.drawable.ic_collapse);
        }
        selectedLayout = viewLayout;
        selectedImgExpandCollapse = imgExpandCollapse;
    }

    public void changeLanguage(int index) {
        String languageCode = switch (index) {
            case 0 -> "en";
            case 1 -> "ar";
            default -> "ar";
        };

        sp.edit().putString("language_code", languageCode).commit();

        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (context != null) {
                ProcessPhoenix.triggerRebirth(context);
            }
        }, 250);
    }




    private void displayDialog(String text) {
        dialog = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_ok);
        TextView customText = dialog.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialog.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

}
