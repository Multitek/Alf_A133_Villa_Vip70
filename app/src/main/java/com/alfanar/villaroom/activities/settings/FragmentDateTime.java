package com.alfanar.villaroom.activities.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.databinding.FragmentDateTimeBinding;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FragmentDateTime extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {
    private static final String TIME_PATTERN = "HH:mm";
    private Calendar calendar;
    private DateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private Map<Integer, String> zoneMap;
    private SharedPreferences sp;
    private Dialog dialog, dialog2, dateDialog, timeDialog, dialogDateTimeUpdate;
    private ArrayList<RadioButton> radioButtonArrayList;
    private int zoneID;
    private Activity context;
    private FragmentDateTimeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDateTimeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        if (getActivity() != null) {
            context = getActivity();
        } else {
            context = MainActivity.getInstance();
        }
        Logger.v("FragmentDateTime onCreateView" );
        sp = MyUtils.getInstance().getShared();
        binding.fabDone.setOnClickListener(this);
        binding.layoutGmt.setOnClickListener(this);
        binding.cardDate.setOnClickListener(this);
        binding.cardTime.setOnClickListener(this);
        binding.layoutGmt.setVisibility(View.VISIBLE);
        binding.layoutManually.setVisibility(View.GONE);

        calendar = Calendar.getInstance();
        dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        timeFormat = new SimpleDateFormat(TIME_PATTERN, Locale.getDefault());
        zoneMap = new HashMap<>();
        zoneMap.put(1, context.getResources().getString(R.string.gmt096));
        zoneMap.put(2, context.getResources().getString(R.string.gmt097));
        zoneMap.put(3, context.getResources().getString(R.string.gmt098));
        zoneMap.put(4, context.getResources().getString(R.string.gmt099));
        zoneMap.put(5, context.getResources().getString(R.string.gmt100));
        zoneMap.put(6, context.getResources().getString(R.string.gmt101));
        zoneMap.put(7, context.getResources().getString(R.string.gmt102));
        zoneMap.put(8, context.getResources().getString(R.string.gmt103));
        zoneMap.put(9, context.getResources().getString(R.string.gmt104));
        binding.textGmt.setText(zoneMap.get(sp.getInt("zone_id", 8)));
        update();
        binding.cbManually.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                binding.layoutGmt.setVisibility(View.GONE);
                binding.layoutManually.setVisibility(View.VISIBLE);
            }else{
                binding.layoutGmt.setVisibility(View.VISIBLE);
                binding.layoutManually.setVisibility(View.GONE);
            }

        });
        return view;
    }




    @Override
    public void onResume() {
        super.onResume();
        binding.textGmt.setText(zoneMap.get(sp.getInt("zone_id", 8)));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (dialog2 != null && dialog2.isShowing()) {
                dialog2.dismiss();
            }
            if (dateDialog != null && dateDialog.isShowing()) {
                dateDialog.dismiss();
            }
            if (timeDialog != null && timeDialog.isShowing()) {
                timeDialog.dismiss();
            }
            if (dialogDateTimeUpdate != null && dialogDateTimeUpdate.isShowing()) {
                dialogDateTimeUpdate.dismiss();
            }
        } catch (Exception e) {
            Logger.e("EXCEPTION", e);
        }
    }

    private void update() {
        binding.textDate.setText(dateFormat.format(calendar.getTime()));
        binding.textTime.setText(timeFormat.format(calendar.getTime()));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(year, monthOfYear, dayOfMonth);
        update();
    }


    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        update();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.layoutGmt.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            displayDialog();
        } else if (id == binding.cardDate.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
           /* dateDialog = new DatePickerDialog(context, DatePickerDialog.THEME_HOLO_DARK, this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            if (dateDialog.getWindow() != null) {
                View decorView = dateDialog.getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
                dateDialog.show();
            }*/

             dateDialog = new DatePickerDialog(
                    requireActivity(),
                    DatePickerDialog.THEME_HOLO_DARK,
                    this,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dateDialog.setOnShowListener(dialog -> {
                Window w = dateDialog.getWindow();
                if (w != null) {
                    View decor = w.getDecorView();
                    decor.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    );
                }
            });

            dateDialog.show();


        } else if (id == binding.cardTime.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
        /*    timeDialog = new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_DARK, this, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            if (timeDialog.getWindow() != null) {
                View decorView = timeDialog.getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
                timeDialog.show();
            }*/

            timeDialog = new TimePickerDialog(
                    requireActivity(),
                    TimePickerDialog.THEME_HOLO_DARK,
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                   true
            );

            timeDialog.setOnShowListener(dialog -> {
                Window w = timeDialog.getWindow();
                if (w != null) {
                    View decor = w.getDecorView();
                    decor.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    );
                }
            });
            timeDialog.show();
        } else if (id == binding.fabDone.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            showProgress();
        }
    }

    @SuppressLint("ApplySharedPref")
    public void showProgress() {
        if (dialogDateTimeUpdate != null && dialogDateTimeUpdate.isShowing()) {
            dialogDateTimeUpdate.dismiss();
        }

        dialogDateTimeUpdate = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_date_time_update);
        dialogDateTimeUpdate.show();
        try {
            Thread.sleep(500);
            if (MyUtils.getInstance().checkPermission(Manifest.permission.SET_TIME)) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.setTime(calendar.getTimeInMillis());
                sp.edit().putLong("time_changed", calendar.getTimeInMillis()).commit();
            } else {
                Logger.w("FragmentDateTime -> SET_TIME Permission Not Granted");
            }
        } catch (Exception e) {
            Logger.e("EXCEPTION", e);
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> dialogDateTimeUpdate.dismiss(), 5000);
    }

    private void displayDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        radioButtonArrayList = new ArrayList<>();
        dialog = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_time_zone);
        final RadioButton r1 = dialog.findViewById(R.id.radioButton1);
        final RadioButton r2 = dialog.findViewById(R.id.radioButton2);
        final RadioButton r3 = dialog.findViewById(R.id.radioButton3);
        final RadioButton r4 = dialog.findViewById(R.id.radioButton4);
        final RadioButton r5 = dialog.findViewById(R.id.radioButton5);
        final RadioButton r6 = dialog.findViewById(R.id.radioButton6);
        final RadioButton r7 = dialog.findViewById(R.id.radioButton7);
        final RadioButton r8 = dialog.findViewById(R.id.radioButton8);
        final RadioButton r9 = dialog.findViewById(R.id.radioButton9);
        r1.setOnClickListener(v -> {
            v.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r1);
            zoneID = 1;
        });
        r2.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r2);
            zoneID = 2;
        });

        r3.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r3);
            zoneID = 3;
        });

        r4.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r4);
            zoneID = 4;
        });

        r5.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r5);
            zoneID = 5;
        });

        r6.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r6);
            zoneID = 6;
        });

        r7.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r7);
            zoneID = 7;
        });

        r8.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r8);
            zoneID = 8;
        });

        r9.setOnClickListener(view -> {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
            configureList(r9);
            zoneID = 9;
        });
        radioButtonArrayList.add(r1);
        radioButtonArrayList.add(r2);
        radioButtonArrayList.add(r3);
        radioButtonArrayList.add(r4);
        radioButtonArrayList.add(r5);
        radioButtonArrayList.add(r6);
        radioButtonArrayList.add(r7);
        radioButtonArrayList.add(r8);
        radioButtonArrayList.add(r9);
        zoneID = sp.getInt("zone_id", 8);
        radioButtonArrayList.get(zoneID - 1).setChecked(true);
        Button save = dialog.findViewById(R.id.btnSave);
        Button cancel = dialog.findViewById(R.id.btnCancel);
        save.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(save, 250);
            changeZone(zoneID);
        });
        cancel.setOnClickListener(v -> {
            MyUtils.getInstance().setViewClickTimeout(cancel, 250);
            dialog.dismiss();
        });
        dialog.show();

    }

    public void changeZone(int index) {
        switch (index) {
            case 1: //
                updateZone(1, "Etc/GMT+4");
                break;
            case 2: //
                updateZone(2, "Etc/GMT+3");
                break;
            case 3: //
                updateZone(3, "Etc/GMT+2");
                break;
            case 4: //
                updateZone(4, "Etc/GMT+1");
                break;
            case 5: //
                updateZone(5, "Etc/GMT0");
                break;
            case 6: //
                updateZone(6, "Etc/GMT-1");
                break;
            case 7: //
                updateZone(7, "Etc/GMT-2");
                break;
            case 8: //
                updateZone(8, "Etc/GMT-3");
                break;
            case 9: //
                updateZone(9, "Etc/GMT-4");
                break;
        }
    }

    public void configureList(RadioButton radio) {
        for (RadioButton r : radioButtonArrayList) {
            if (!radio.equals(r)) {
                r.setChecked(false);
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    public void updateZone(int id, String gmt) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            sp.edit().putInt("zone_id", id).commit();
            am.setTimeZone(gmt);
            dialog.dismiss();
            if (dialog2 != null && dialog2.isShowing()) {
                dialog2.dismiss();
            }
            dialog2 = MyUtils.getInstance().dialogPublic(context, R.layout.dialog_ok);
            TextView customText = dialog2.findViewById(R.id.customText);
            customText.setText(context.getResources().getString(R.string.saving));
            Button ok = dialog2.findViewById(R.id.btnOk);
            ok.setVisibility(View.GONE);
            dialog2.show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                //MyUtils.getInstance(context).restartApp();
                binding.textGmt.setText(zoneMap.get(id));
                update();
                dialog2.dismiss();
            }, 2000);

        } catch (Exception e) {
            Logger.e("EXCEPTION", e);
        }
    }
}
