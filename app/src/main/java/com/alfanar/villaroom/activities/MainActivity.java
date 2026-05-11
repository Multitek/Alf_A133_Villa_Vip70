package com.alfanar.villaroom.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.cameras.IpCameras;
import com.alfanar.villaroom.activities.gallery.GalleryActivity;
import com.alfanar.villaroom.activities.intercom.IncomingDoorConnected;
import com.alfanar.villaroom.activities.intercom.IncomingDoorParallelRinging;
import com.alfanar.villaroom.activities.intercom.IncomingRoomRinging;
import com.alfanar.villaroom.activities.intercom.IndoorRingActivity;
import com.alfanar.villaroom.activities.intercom.RecentCalls;
import com.alfanar.villaroom.activities.settings.PasswordControlActivity;
import com.alfanar.villaroom.activities.settings.SettingsActivity;
import com.alfanar.villaroom.databinding.MainBinding;
import com.alfanar.villaroom.interfaces.WeatherListener;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.models.MenuModel;
import com.alfanar.villaroom.networkmanager.NetworkWatcher;
import com.alfanar.villaroom.receivers.ScreenStateReceiver;
import com.alfanar.villaroom.threads.MainThread;
import com.alfanar.villaroom.threads.WeatherControl;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.GeneralMediaPlayer;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, WeatherListener, LocationListener, MotionLayout.TransitionListener{
    @SuppressLint("StaticFieldLeak")
    private static MainActivity instance;

    private SharedPreferences sp;
    private Dialog dialogInitial, dialogInfo;
    private ScreenStateReceiver mScreenStateReceiver;
  //  private Handler handler;
    private ArrayList<MenuModel> menuList = new ArrayList<>();
    private MenuAdapter menuAdapter;
    private boolean motionCallIsActive = false;
    private MainBinding binding;
    // private boolean mainClickAble = true;

    private TextView txtHistoryCount;

    public static MainActivity getInstance() {
        return instance;
    }

    @SuppressLint({"ApplySharedPref", "StringFormatInvalid", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sp = getSharedPreferences("DEVICE_SHARED", MODE_PRIVATE);
        MyUtils.getInstance().darkTheme = sp.getBoolean("DarkTheme", false);
        setTheme(MyUtils.getInstance().getTheme());
        MyUtils.getInstance().setShared(sp);
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(MyUtils.getInstance().getWindowBackground(-1));
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        GeneralMediaPlayer.getInstance();
        instance = MainActivity.this;
        Logger.d("MainActivity.onCreate");


        MyUtils.getInstance().hideKeyboard(this);

        binding.rootMain.setOnClickListener(this);
        binding.mainSettings.setOnClickListener(this);
        binding.mainSilent.setOnClickListener(this);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.CENTER);
        binding.mainMenuRecyclerView.setLayoutManager(layoutManager);
        binding.includeNotificationCall.notificationCallText.setText(String.format(getString(R.string.call_notif), 5));
        binding.notificationClear.setOnClickListener(this);
        binding.motionLayoutCall.addTransitionListener(MainActivity.this);
        binding.mainEthernetOff.setOnClickListener(this);


        binding.imgDeleteMotionAlarm.setOnClickListener(this);
        MyUtils.getInstance().weatherListener = this;


        mScreenStateReceiver = new ScreenStateReceiver();
        IntentFilter mScreenFilter = new IntentFilter();
        mScreenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenStateReceiver, mScreenFilter);


        displayInitialDialog();


        setPermissions();

        NetworkWatcher netWatcher = new NetworkWatcher(this);
        netWatcher.setListener(isConnected -> {
            if (MyUtils.getInstance().ethernetState) {
                binding.mainEthernetOff.setVisibility(View.GONE);
            } else {
                binding.mainEthernetOff.setVisibility(View.VISIBLE);
            }
        });

        new MainThread(netWatcher).start();


        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("MultitekLock");
            lock.setReferenceCounted(false);
            lock.acquire();
        }



    }
    private final Handler screensaverHandler = new Handler(Looper.getMainLooper());
    private final Runnable showScreensaverRunnable = this::showScreensaver;


    private void resetScreensaverTimer() {
        screensaverHandler.removeCallbacks(showScreensaverRunnable);
        screensaverHandler.postDelayed(showScreensaverRunnable, MyUtils.getInstance().getScreenOffTimeOut());
        // System.out.println("debugLife.resetScreensaverTimer = " + MyUtils.getInstance().getScreenOffTimeOut());
    }

    private void showScreensaver() {
        //MyUtils.getInstance().backToRootActivity();
        startActivity(new Intent(this, ScreensaverActivity.class));
        System.out.println("debugLife.showScreensaver ");
    }


    @Override
    protected void onStart() {
        super.onStart();
        Logger.d("MainActivity.onStart");
        MyUtils.getInstance().hideKeyboard(this);
        this.overridePendingTransition(0, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d("MainActivity.onResume");
        MyUtils.getInstance().hideNavigation(this);
        MyUtils.getInstance().hideKeyboard(this);
        if (MyUtils.getInstance().initCompleted) {
            if (menuAdapter != null) {
                new Handler(Looper.getMainLooper()).postDelayed(this::controlNotifications, 250);
            }
            controlViews();

        }
        resetScreensaverTimer();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        resetScreensaverTimer();
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d("MainActivity.onPause");
        screensaverHandler.removeCallbacks(showScreensaverRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.d("MainActivity.onStop");
    }

    private void controlViews() {
        binding.infoDayState.setText(getDayState());
        binding.infoDeviceName.setText(sp.getString("DEVICE_NAME", getString(R.string.info_device_name)));
        if (!MyUtils.getInstance().ethernetState || MyUtils.getInstance().getIpAddress().equals("192.168.256.256")) {
            binding.mainEthernetOff.setVisibility(View.VISIBLE);
        } else {
            binding.mainEthernetOff.setVisibility(View.GONE);
        }
        setSilentMode();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        Logger.d("debugLifeCycle.onDestroy");
        if (mScreenStateReceiver != null) unregisterReceiver(mScreenStateReceiver);

        if (dialogInitial != null && dialogInitial.isShowing()) {
            dialogInitial.cancel();
        }
        if (dialogInfo != null && dialogInfo.isShowing()) {
            dialogInfo.cancel();
        }
        MyUtils.getInstance().weatherListener = null;


    }

    @SuppressLint("StringFormatInvalid")
    private void controlNotifications() {
        binding.linearNotifications.setVisibility(View.GONE);
        if (motionCallIsActive) {
            binding.motionLayoutCall.transitionToStart();
            motionCallIsActive = false;
        }

        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    int oldCallCount = MyUtils.getInstance().unAnsweredCallCount;
                    int currentCallCount = MyUtils.getInstance().getUnAnsweredCount();
                    runOnUiThread(() -> {
                        if (currentCallCount > 0) {
                            binding.linearNotifications.setVisibility(View.VISIBLE);
                            binding.includeNotificationCall.notificationCallText.setText(String.format(getString(R.string.call_notif), currentCallCount));
                            if (MyUtils.getInstance().incomingCall && currentCallCount > oldCallCount) {
                                MyUtils.getInstance().playNotification(MainActivity.this);
                            }
                            MyUtils.getInstance().incomingCall = false;
                        }
                        if (menuAdapter != null && txtHistoryCount != null) {
                            if (currentCallCount > 0) {
                                txtHistoryCount.setVisibility(View.VISIBLE);
                                txtHistoryCount.setText(String.valueOf(currentCallCount));
                            } else {
                                txtHistoryCount.setVisibility(View.GONE);
                                txtHistoryCount.setText(String.valueOf(0));
                            }
                        }
                        MyUtils.getInstance().unAnsweredCallCount = currentCallCount;
                    });

                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }

            }
        }.start();


    }


    @SuppressLint("ApplySharedPref")
    @Override
    public void onClick(@NonNull View v) {

        if (v.getId() == binding.mainSilent.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            if (sp.getBoolean("DISTURB", false)) {
                sp.edit().putBoolean("DISTURB", false).commit();
                sp.edit().putInt("volIndex", 7).commit();
            } else {
                sp.edit().putBoolean("DISTURB", true).commit();
                sp.edit().putInt("volIndex", 0).commit();
            }
            setSilentMode();
        } else if (v.getId() == binding.mainSettings.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            startActivityForResult(new Intent(MainActivity.this, PasswordControlActivity.class), 200);
        } else if (v.getId() == binding.notificationClear.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        DatabaseHelper.getInstance().setAllCallReadState();
                    } catch (Exception e) {
                        Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                    } finally {
                        runOnUiThread(() -> controlNotifications());
                    }
                }
            }.start();
        } else if (v.getId() == binding.mainEthernetOff.getId()) {
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            displayInfoDialog(getString(R.string.error_network_unreachable));
        }


    }




    public void setSilentMode() {
        boolean state = sp.getBoolean("DISTURB", false);
        if (state) {
            binding.mainSilent.setImageResource(R.drawable.ic_disturb_active);
        } else {
            binding.mainSilent.setImageResource(R.drawable.ic_disturbe_passive);
        }
    }

    public void setPermissions() {
        requestPermissions(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,  Manifest.permission.SET_TIME, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_SECURE_SETTINGS, Manifest.permission.WRITE_SETTINGS}, 1);
    }

    private String getDayState() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        if (timeOfDay > 4 && timeOfDay < 12) {
            return getString(R.string.good_morning);
        } else if (timeOfDay > 11 && timeOfDay < 17) {
            return getString(R.string.good_afternoon);
        } else if (timeOfDay > 16 && timeOfDay < 21) {
            return getString(R.string.good_evening);
        } else {
            return getString(R.string.good_night);
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }


    public void startIncomingDoorCall(String mac, String remoteIp, String callId, String doorCount, String relay1Name, String relay2Name) {
        Intent intent = new Intent(this, IncomingDoorConnected.class);
        intent.putExtra("mac", mac);
        intent.putExtra("remote_ip", remoteIp);
        intent.putExtra("call_id", callId);
        intent.putExtra("door_relay_count", doorCount);
        intent.putExtra("door_relay_name1", relay1Name);
        intent.putExtra("door_relay_name2", relay2Name);

        runOnUiThread(() -> {
            if (MyUtils.getInstance().isNotInCall()) {
                DeviceModel dev = DeviceController.getInstance().getDoorWithIp(remoteIp);
                if (dev != null) {
                    startActivity(intent);
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> startActivity(intent), 2000);
                }
            } else {
                MyUtils.getInstance().incomingCall = true;
            }
        });


    }

    public void startIncomingRoomCall(String remote_ip, String call_id) {
        runOnUiThread(() -> {
            if (MyUtils.getInstance().isNotInCall()) {
                Intent intent = new Intent(this, IncomingRoomRinging.class);
                intent.putExtra("remote_ip", remote_ip);
                intent.putExtra("call_id", call_id);
                startActivity(intent);
            } else {
                MyUtils.getInstance().incomingCall = true;
            }
        });
    }

    public void startIncomingDoorParallelCall(String remote_ip, String call_id) {
        runOnUiThread(() -> {
            if (MyUtils.getInstance().isNotInCall()) {
                Intent intent = new Intent(this, IncomingDoorParallelRinging.class);
                intent.putExtra("remote_ip", remote_ip);
                intent.putExtra("call_id", call_id);
                startActivity(intent);
            } else {
                MyUtils.getInstance().incomingCall = true;
            }

        });

    }

    public void startQRActivity() {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, QrDetectorActivity.class);
            startActivity(intent);
        });
    }




    private void displayInfoDialog(String text) {
        closeInfoDialog();
        dialogInfo = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_ok);
        TextView customText = dialogInfo.findViewById(R.id.customText);
        customText.setText(text);
        Button ok = dialogInfo.findViewById(R.id.btnOk);
        ok.setOnClickListener(v -> dialogInfo.dismiss());
        dialogInfo.show();
        new Handler(Looper.getMainLooper()).postDelayed(this::closeInfoDialog,2250);
    }

    private void closeInfoDialog() {
        if (dialogInfo != null && dialogInfo.isShowing()) {
            dialogInfo.dismiss();
        }
    }


    private void displayInitialDialog() {
        MyUtils.getInstance().initCompleted = false;
        dialogInitial = MyUtils.getInstance().dialogPublic(this, R.layout.dialog_progress);
        dialogInitial.setCancelable(false);
        dialogInitial.setCanceledOnTouchOutside(false);
        TextView dialogText = dialogInitial.findViewById(R.id.customText);
        dialogText.setText(getResources().getString(R.string.initial_settings));
        dialogInitial.show();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialogInitial != null && dialogInitial.isShowing()) {
                dialogInitial.cancel();
                resetScreensaverTimer();
            }
        },15000);
    }




    @Override
    public void onWeather(String temp, String tempMin, String tempMax, String description, String iconId) {
        runOnUiThread(() -> {
            binding.weatherLayout.setVisibility(View.VISIBLE);
            binding.weatherTemp.setText(temp);
            binding.weatherTempMin.setText(tempMin);
            binding.weatherTempMax.setText(tempMax);
            binding.weatherDescription.setText(description);
            binding.weatherIcon.setImageResource(getWeatherIcon(iconId));
        });
    }

    @Override
    public void onWeatherException(int result) {
        Log.d("Multitek ", "[MainActivity] onWeatherException result= " + result);
        if (result == 5) {
            MyUtils.getInstance().weatherNotFoundCount++;
            if (MyUtils.getInstance().weatherNotFoundCount == 1) {
                new WeatherControl().start();
            } else if (MyUtils.getInstance().weatherNotFoundCount == 2) {
                new WeatherControl().start();
            }
        }
    }

    @Override
    public void onWeatherLocationChange() {
        setLocation();
        MyUtils.getInstance().weatherNotFoundCount = 0;
        new WeatherControl().start();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("onLocationChanged ", "Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
    }

    private int getWeatherIcon(String iconId) {
        return switch (iconId) {
            case "01d" -> R.drawable.ic_weather_clear_sky_day;
            case "01n" -> R.drawable.ic_weather_clear_sky_night;
            case "02d" -> R.drawable.ic_weather_few_clouds_day;
            case "02n" -> R.drawable.ic_weather_few_clouds_night;
            case "03d", "03n" -> R.drawable.ic_weather_scattered_cloud;
            case "04d", "04n" -> R.drawable.ic_weather_broken_clouds;
            case "09d", "09n" -> R.drawable.ic_weather_shower_rain;
            case "10d" -> R.drawable.ic_weather_rain_day;
            case "10n" -> R.drawable.ic_weather_rain_night;
            case "11d", "11n" -> R.drawable.ic_weather_storm;
            case "13d", "13n" -> R.drawable.ic_weather_snow;
            case "50d", "50n" -> R.drawable.ic_weather;
            default -> R.drawable.ic_weather_mist;
        };
    }


    private void setLocation() {
        try {
            int countryIndex = MyUtils.getInstance().getShared().getInt("weather_country_index", 44);
            int cityIndex = MyUtils.getInstance().getShared().getInt("weather_city_index", 107);
            String countryName = MyUtils.weatherCountryList.get(countryIndex).getName();
            String cityName = MyUtils.weatherCountryList.get(countryIndex).getCityList().get(cityIndex).getName();
            runOnUiThread(() -> binding.location.setText(MessageFormat.format("{0}/{1}", cityName, countryName)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public void initDone(){
        menuList = DatabaseHelper.getInstance().getAllMenuItems();
        Collections.sort(menuList, (o1, o2) -> o1.getItemPosition() - o2.getItemPosition());
        new Handler(Looper.getMainLooper()).post(() -> {
            menuAdapter = new MenuAdapter();
            binding.mainMenuRecyclerView.setAdapter(menuAdapter);
            controlNotifications();
            controlViews();
            closeInfoDialog();
            MyUtils.getInstance().initCompleted = true;

            if (dialogInitial != null && dialogInitial.isShowing()) {
                dialogInitial.cancel();
            }
        });
    }

    public void weatherJsonDone(){
        new Handler(Looper.getMainLooper()).post(this::setLocation);
    }


    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {

        if (currentId == R.id.end_call) {
            motionCallIsActive = true;
            TextView delete = findViewById(R.id.delete_call);
            delete.setOnClickListener(v -> new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        DatabaseHelper.getInstance().setAllCallReadState();
                    } catch (Exception e) {
                        Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                    } finally {
                        runOnUiThread(() -> controlNotifications());
                    }
                }
            }.start());
            TextView show = findViewById(R.id.show_call);
            show.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RecentCalls.class)));
        }
    }

    @Override
    public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {

        } else if (requestCode == 200) {
            if (resultCode == 1) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        } else if (requestCode == 300) {


        }
    }


    public void indoorRingReceived(String who) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Logger.d("indoorRingReceived.who = " + who);
            if (IndoorRingActivity.instance == null) {
                MyUtils.getInstance().wakeUp();
                startActivity(new Intent(MainActivity.this, IndoorRingActivity.class));
            }
        });

    }

    class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> implements View.OnDragListener, View.OnClickListener, View.OnLongClickListener {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MenuModel menuModel = menuList.get(position);
            holder.title.setText(menuModel.getItemName());
            holder.counter.setVisibility(View.GONE);
            switch (menuModel.getItemTag()) {
                case "historyCalls":
                    txtHistoryCount = holder.counter;
                    holder.img.setPadding(MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10));
                    if (MyUtils.getInstance().unAnsweredCallCount > 0) {
                        holder.counter.setVisibility(View.VISIBLE);
                        holder.counter.setText(String.valueOf(MyUtils.getInstance().unAnsweredCallCount));
                    }
                    break;

                case "cameras":
                case "roomsDoors":
                    holder.img.setPadding(MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10), MyUtils.getInstance().dpToPx(10));
                    break;
            }
            holder.img.setImageResource(menuModel.getItemIconId());
            holder.cardItem.setTag(menuModel.getItemTag());
            holder.cardItem.setOnClickListener(this);
            holder.cardItem.setOnLongClickListener(this);
            holder.cardItem.setOnDragListener(this);
        }

        @Override
        public int getItemCount() {
            return menuList.size();
        }

        @Override
        public void onClick(View v) {

            String tag = v.getTag().toString();
            MyUtils.getInstance().setViewClickTimeout(v, 250);
            switch (tag) {
                case "historyCalls":
                    startActivity(new Intent(getApplicationContext(), RecentCalls.class));

                    break;
                case "roomsDoors":
                    startActivity(new Intent(getApplicationContext(), DoorsAndRoomsActivity.class));
                    break;
                case "smartHome":
                    /*String autoType = sp.getString("automation_type", "ALFA_NONE");
                    if (autoType.equals("ALFA_PREM")) {
                        String url = "aioremoteneo://";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } else if (autoType.equals("ALFA_SMART")) {
                        displayInfoDialog(getResources().getString(R.string.alfa_mini_not_supported));
                    } else if (autoType.equals("ALFA_KNX")) {
                        displayInfoDialog(getResources().getString(R.string.not_supported));
                    } else {
                        displayInfoDialog(getResources().getString(R.string.not_supported));
                    }
                    break;*/


                    String packageName = "com.alfanar.smartdisplay";
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);

                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    } else {
                        displayInfoDialog("SmartHome App Not Found, Please Install it!!!!");

                    }
                    break;
                case "cameras":
                    startActivity(new Intent(getApplicationContext(), IpCameras.class));
                    break;
                case "gallery":
                    startActivity(new Intent(MainActivity.this, GalleryActivity.class));
                    break;
            }


        }

        @Override
        public boolean onLongClick(View v) {
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
            String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
            View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
            v.startDrag(data        // data to be dragged
                    , dragshadow   // drag shadow builder
                    , v           // local data about the drag and drop operation
                    , 0          // flags (not currently used, set to 0)
            );
            return true;
        }


        @Override
        public boolean onDrag(View v, DragEvent event) {
            // Defines a variable to store the action type for the incoming event
            int action = event.getAction();
            // Handles each of the expected events
            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:

                case DragEvent.ACTION_DRAG_ENDED:

                case DragEvent.ACTION_DRAG_LOCATION:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.getBackground().setColorFilter(getColor(R.color.green), PorterDuff.Mode.SRC_IN);
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.getBackground().clearColorFilter();
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DROP:
                    ClipData.Item item = event.getClipData().getItemAt(0);
                    String dragData = item.getText().toString();
                    String dropData = v.getTag().toString();
                    v.getBackground().clearColorFilter();
                    v.invalidate();
                    if (!dragData.equals(dropData)) {
                        MenuModel dragModel = null, dropModel = null;
                        for (int i = 0; i < menuList.size(); i++) {
                            if (menuList.get(i).getItemTag().equals(dragData)) {
                                dragModel = menuList.get(i);
                            }
                            if (menuList.get(i).getItemTag().equals(dropData)) {
                                dropModel = menuList.get(i);
                            }
                        }
                        if (dropModel != null && dragModel != null) {
                            int dragPos = dragModel.getItemPosition();
                            int dropPos = dropModel.getItemPosition();
                            dragModel.setItemPosition(dropPos);
                            DatabaseHelper.getInstance().updateMenuItem(dragModel);
                            menuList.set(dragPos, dragModel);
                            dropModel.setItemPosition(dragPos);
                            DatabaseHelper.getInstance().updateMenuItem(dropModel);
                            menuList.set(dropPos, dropModel);
                            notifyItemMoved(dragPos, dropPos);
                            if (dragPos < dropPos) {
                                notifyItemMoved(dropPos - 1, dragPos);
                            } else {
                                notifyItemMoved(dropPos + 1, dragPos);
                            }
                        }
                        return true;
                    }
                    return false;

                default:
                    break;
            }
            return false;
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            CardView cardItem;
            ImageView img;
            TextView counter;

            public ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_item);
                cardItem = itemView.findViewById(R.id.card_item);
                img = itemView.findViewById(R.id.img);
                counter = itemView.findViewById(R.id.counter);
            }
        }
    }


}
