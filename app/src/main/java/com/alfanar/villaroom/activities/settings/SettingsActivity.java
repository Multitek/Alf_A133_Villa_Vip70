package com.alfanar.villaroom.activities.settings;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alfanar.retrofit.update.GetInternetStateApi;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.ActivitySetttingsBinding;
import com.alfanar.villaroom.threads.MainTimeout;
import com.alfanar.villaroom.util.MyUtils;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private final Fragment[] fragments =
            {new FragmentDeviceSettings(),
                    new FragmentRings(),
                    new FragmentDeviceName(),
                    new FragmentScreen(),
                    new FragmentWeather(),
                    new FragmentDateTime(),
                    new FragmentSmartHome(),
                    new FragmentManuel(),
                    new FragmentAbout(),
                    new FragmentReset(),
                    new FragmentNetworkSettings()};

    private final LinearLayout[] layouts = new LinearLayout[fragments.length];
    //private final String[] tags = {"FragmentDeviceSettings", "FragmentRings", "FragmentDeviceName", "FragmentTheme", "FragmentWeather", "FragmentSmartHome", "FragmentManuel", "FragmentAbout", "FragmentReset"};
    public Resources.Theme themes;
    public TypedValue storedValueInTheme;
    ActivitySetttingsBinding binding;
    private Dialog dialogInfo;
    //private int oldFragmentNum = -1;
    private FragmentManager fragmentManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyUtils.getInstance().getTheme());
        super.onCreate(savedInstanceState);
        binding = ActivitySetttingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        MyUtils.getInstance().hideNavigation(this);
        MainTimeout.getInstance().setTimeout(60 * 1000);
        themes = this.getTheme();
        storedValueInTheme = new TypedValue();
        new GetInternetStateApi().start();
        initGui();


        fragmentManager = getSupportFragmentManager();
        addFragment(0);

    }

    @Override
    protected void onStart() {
        super.onStart();
        this.overridePendingTransition(0, 0);
    }

    private void initGui() {

        layouts[0] = binding.settingsBtnSettings;
        layouts[1] = binding.settingsBtnRings;
        layouts[2] = binding.settingsBtnDeviceName;
        layouts[3] = binding.settingsBtnScreen;
        layouts[4] = binding.settingsBtnWeather;
        layouts[5] = binding.settingsBtnDate;
        layouts[6] = binding.settingsBtnSmart;
        layouts[7] = binding.settingsBtnManuel;
        layouts[8] = binding.settingsBtnAbout;
        layouts[9] = binding.settingsBtnReset;
        layouts[10] = binding.settingsBtnNetwork;

        binding.settingsBtnSettings.setOnClickListener(this);
        binding.settingsBtnAbout.setOnClickListener(this);
        binding.settingsBtnManuel.setOnClickListener(this);
        binding.settingsBtnReset.setOnClickListener(this);
        binding.settingsBtnDate.setOnClickListener(this);
        binding.settingsBtnDeviceName.setOnClickListener(this);
        binding.settingsBtnRings.setOnClickListener(this);
        binding.settingsBtnScreen.setOnClickListener(this);
        binding.settingsBtnWeather.setOnClickListener(this);
        binding.settingsBtnSmart.setOnClickListener(this);
        binding.settingsBtnNetwork.setOnClickListener(this);

        binding.includeTop.imgOther.setVisibility(View.GONE);
        binding.includeTop.textTitle.setText(getResources().getString(R.string.settings));
        binding.includeTop.imgBack.setOnClickListener(this);
    }

    public void addFragment(int index) {

        for (LinearLayout layout : layouts) {
            layout.setBackgroundColor(getColor(R.color.transparent));
        }


        if (themes.resolveAttribute(R.attr.ToolbarBackground, storedValueInTheme, true)) {
            layouts[index].setBackgroundColor(storedValueInTheme.data);
        }

        SystemClock.sleep(50);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.replace(binding.fragDepo.getId(), fragments[index], null);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyUtils.getInstance().hideNavigation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogInfo != null && dialogInfo.isShowing()) {
            dialogInfo.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        v.startAnimation(MyUtils.getInstance().buttonClickAnimation);


        MyUtils.getInstance().hideKeyboard(this);
        MyUtils.getInstance().setViewClickTimeout(v, 500);


        int id = v.getId();
        if (id == binding.settingsBtnSettings.getId()) {
            addFragment(0);
        } else if (id == binding.settingsBtnRings.getId()) {
            addFragment(1);
        } else if (id == binding.settingsBtnDeviceName.getId()) {
            addFragment(2);
        } else if (id == binding.settingsBtnScreen.getId()) {
            addFragment(3);
        } else if (id == binding.settingsBtnWeather.getId()) {
            addFragment(4);
        } else if (id == binding.settingsBtnDate.getId()) {
            addFragment(5);
        } else if (id == binding.settingsBtnSmart.getId()) {
            addFragment(6);
        } else if (id == binding.settingsBtnManuel.getId()) {
            addFragment(7);
        } else if (id == binding.settingsBtnAbout.getId()) {
            addFragment(8);
        } else if (id == binding.settingsBtnReset.getId()) {
            addFragment(9);
        } else if (id == binding.settingsBtnNetwork.getId()) {
            addFragment(10);
        } else if (id == binding.includeTop.imgBack.getId()) {
            finish();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int stat = event.getAction();
        if (stat == MotionEvent.ACTION_DOWN) {
            MainTimeout.getInstance().setTimeout(120 * 1000);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        MyUtils.getInstance().hideNavigation(SettingsActivity.this);
    }

    public void passAndroidScreen() {
        Intent i1 = new Intent();
        i1.setAction("SHOW_STATUS_BAR");
        sendBroadcast(i1);
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
    }

    public void displayDialog(String text) {
        dialogInfo = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_ok);
        TextView customText = dialogInfo.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialogInfo.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialogInfo.dismiss());
        dialogInfo.show();
    }
}
