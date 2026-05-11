package com.alfanar.villaroom.util;

import static android.content.Context.ACTIVITY_SERVICE;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import android.os.SystemClock;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.BuildConfig;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.activities.ScreensaverActivity;
import com.alfanar.villaroom.activities.intercom.IncomingDoorConnected;
import com.alfanar.villaroom.activities.intercom.IncomingDoorParallelConnected;
import com.alfanar.villaroom.activities.intercom.IncomingDoorParallelRinging;
import com.alfanar.villaroom.activities.intercom.IncomingRoomConnected;
import com.alfanar.villaroom.activities.intercom.IncomingRoomRinging;
import com.alfanar.villaroom.activities.intercom.OutGoingDoorConnected;
import com.alfanar.villaroom.activities.intercom.OutGoingRoomCalling;
import com.alfanar.villaroom.activities.intercom.OutGoingRoomConnected;
import com.alfanar.villaroom.interfaces.CallListener;
import com.alfanar.villaroom.interfaces.FileListener;
import com.alfanar.villaroom.interfaces.HistoryListener;
import com.alfanar.villaroom.interfaces.RFListener;
import com.alfanar.villaroom.interfaces.WeatherListener;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.CameraDevice;
import com.alfanar.villaroom.models.CountryListModel;
import com.alfanar.villaroom.models.CountryModel;
import com.alfanar.villaroom.models.GeneralModel;
import com.alfanar.villaroom.sockets.MultiCastThread;
import com.alfanar.villaroom.sockets.MulticastPublisher;
import com.google.gson.Gson;
import com.jakewharton.processphoenix.ProcessPhoenix;

import org.apache.commons.lang3.RandomUtils;
import org.linphone.LinphoneManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MyUtils {
    public static String UPDATE_SERVER_DOMAIN = "smart.multitek.com.tr";
    public static String CLOUD_SERVER_DOMAIN = "vdi.alfanar.com"; //"test.multitek.com.tr" ;//
    public static String SERVER_API_URL = "https://vdi.alfanar.com:8096/alfanar_service/"; //"https://test.multitek.com.tr:8096/multitek_service/";//
    public static String UPDATE_API_URL = "https://smart.multitek.com.tr:8095/update_service/";
    public static String STUN_SERVER = "smart.multitek.com.tr:3478";
    public static String API_USER = "alfanar";
    public static String API_PASS = "Alf.3838!";
    public static String API_USER_UPDATE = "multitek";
    public static String API_PASS_UPDATE = "Mlt.3838!";
    public static int MQTT_PORT = 1883;
    public static String MQTT_USER = "activemq-consumer";

    //public String SERVER_URL = "http://vdi.alfanar.com:8093/alfanar_service/";
    //public String UPDATE_URL = "http://smart.multitek.com.tr:8094/update_service/";
    //public String SERVER_URL = "https://vdi.alfanar.com:8096/alfanar_service/";
    //public String UPDATE_URL = "https://smart.multitek.com.tr:8095/update_service/";
    //public String STUN_SERVER = "smart.multitek.com.tr:3478";
    //public String CLOUD_SERVER = "vdi.alfanar.com";
    //public String EXCEPTION_SERVER = "update.multitek.com.tr";
    //public int EXCEPTION_PORT = 6701;
    //public String MQTT_URL = "tcp://vdi.alfanar.com:1883";//"tcp://95.0.234.99:1883"
    public static String MQTT_PASS = "Alf.3838!";
    private static MyUtils instance;
    //public boolean gpioOk = false;
    public boolean base_exist = true;
    public boolean alarm_supported = false;
    public boolean knx_supported = false;
    public boolean relay_supported = false;
    public boolean rf_supported = false;
    public boolean scenarioClickAble = true;
    public boolean smartHomeUnlockedState = false;
    public RFListener rfListener = null;
    public boolean FIRST_BOOT = false;
    public boolean ethernetState = true;
    public static volatile ArrayList<CountryModel> weatherCountryList;
    public HistoryListener historyListener = null;
    public CallListener callListener = null;
    public FileListener fileListener = null;
    public boolean darkTheme = true;
    public int weatherNotFoundCount = 0;
    public ArrayList<CameraDevice> list;
    public int unAnsweredCallCount = 0;
    public WeatherListener weatherListener;
    public boolean initCompleted = true;
    public int unReadAlarmCount = 0;
    public boolean incomingCall = false;

    public boolean internetActive = false;
    public boolean inUpdateMode = false;
    // public String gateway = "";

    public AlphaAnimation buttonClickAnimation = new AlphaAnimation(1F, 0.3F);
    public int callsCount = 0;

    public int ethernetCounter = 0;
    private String macAddress = "";
    private SharedPreferences sp;

    // private Animation anim1;
    private MediaPlayer mplayerNotification;

    public static MyUtils getInstance() {
        if (instance == null) {
            instance = new MyUtils();
        }
        return instance;
    }

    public static boolean internetOnline() {
        HttpURLConnection urlc = null;
        try {
            urlc = (HttpURLConnection) new URL("https://clients3.google.com/generate_204").openConnection();
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(2000);
            urlc.connect();
            int responseCode = urlc.getResponseCode();
            Logger.d("MyUtils.isInternetOnline responseCode = " + responseCode);

            return (responseCode == 204);
        } catch (IOException e) {
            Logger.d("MyUtils.internetOnline API exception" + e.getMessage());
            return false;
        } finally {
            if (urlc != null) {
                urlc.disconnect();
            }
        }
    }

    public int getWindowBackground(int index) {
        int bgIndex;
        if (index == -1) {
            SharedPreferences sp = getShared();
            bgIndex = sp.getInt("background_img_index", -1);
            if (bgIndex == -1) {
                if (MyUtils.getInstance().darkTheme) {
                    bgIndex = 2;
                } else {
                    bgIndex = 4;
                }
            }
        } else {
            bgIndex = index;
        }

        if (bgIndex == 1) {
            return R.drawable.main_background1;
        } else if (bgIndex == 2) {
            return R.drawable.main_background2;
        } else if (bgIndex == 3) {
            return R.drawable.main_background3;
        } else if (bgIndex == 4) {
            return R.drawable.main_background4;
        } else if (bgIndex == 5) {
            return R.drawable.main_background5;
        } else if (bgIndex == 6) {
            return R.drawable.main_background6;
        } else if (bgIndex == 7) {
            return R.drawable.main_background7;
        } else if (bgIndex == 8) {
            return R.drawable.main_background8;
        } else if (bgIndex == 9) {
            return R.drawable.main_background9;
        } else if (bgIndex == 10) {
            return R.drawable.main_background10;
        }

        return R.drawable.main_background1;
    }

    public SharedPreferences getShared() {
        if (sp == null) {
            sp = App.getInstance().getSharedPreferences("DEVICE_SHARED", Context.MODE_PRIVATE);
        }
        return sp;
    }

    public void setShared(SharedPreferences sharedPreferences) {
        sp = sharedPreferences;
    }

    public int getTheme() {
        return MyUtils.getInstance().darkTheme ? R.style.AppTheme_DarkTheme : R.style.AppTheme_LightTheme;
    }

    public int getUnAnsweredCount() {
        int count = 0;
        List<CallModel> calls = DatabaseHelper.getInstance().getShowCalls();
        for (CallModel callModel : calls) {
            if (!callModel.isCallReadState()) {
                count++;
            }
        }

        return count;
    }

    public String getAppVer() {
        try {
            return BuildConfig.VERSION_NAME + "_V" + BuildConfig.VERSION_CODE;
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        return "";
    }

    public String getDoorImagesDir(){
        File dirCallImage = new File(App.getInstance().getExternalFilesDir(null), "MultitekCallImages");
        return dirCallImage.getAbsolutePath();
    }

    public String getCallSnapShotsDir() {
        File dirCaptureImages = new File(App.getInstance().getExternalFilesDir(null), "MultitekCallCaptureImages");
        return dirCaptureImages.getAbsolutePath();
    }

    public void initEnvironments() {
        hideStatusBarAndKeyboard();



        File dirCallImage = new File(App.getInstance().getExternalFilesDir(null), "MultitekCallImages");
        dirCallImage.mkdirs();



        File dirCaptureImages = new File(App.getInstance().getExternalFilesDir(null), "MultitekCallCaptureImages");
        dirCaptureImages.mkdirs();

        File apkDir = new File(App.getInstance().getExternalFilesDir(null), "ApkFolder");
        apkDir.mkdirs();

        try {
            if (MyUtils.getInstance().checkPermission(Manifest.permission.WRITE_SETTINGS)) {
                android.provider.Settings.System.putInt(
                        App.getInstance().getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT,
                        Integer.MAX_VALUE
                );
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }


        if (MyUtils.getInstance().checkPermission(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            Settings.Secure.putInt(App.getInstance().getContentResolver(), "camera_double_tap_power_gesture_disabled", 1);
            Settings.Secure.putInt(App.getInstance().getContentResolver(), "doze_enabled", 0);
            Settings.System.putInt(App.getInstance().getContentResolver(), "sound_effects_enabled", 0);
            Settings.Global.putInt(App.getInstance().getContentResolver(), Settings.Global.AUTO_TIME, 1);
            Settings.Global.putInt(App.getInstance().getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 1);
        }

        controlCallImages();
        controlCallCaptureImages();
    }

    public void controlCallImages() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/AlfanarCallImages");
                    if (directory.exists()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            ArrayList<String> callsPathList = DatabaseHelper.getInstance().getAllCallsPath();
                            ArrayList<String> deleteList = new ArrayList<>();
                            for (File file : files) {
                                if (!callsPathList.contains(file.getAbsolutePath())) {
                                    deleteList.add(file.getAbsolutePath());
                                }
                            }
                            if (deleteList.size() > 0) {
                                for (String path : deleteList) {
                                    File f = new File(path);
                                    if (f.exists()) {
                                        boolean res = f.delete();
                                        Logger.d("file removing res = " + res);
                                    } else {
                                        Logger.d("file not exist");
                                    }
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();

    }

    public void controlCallImagesFolder() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/AlfanarCallImages");
                    if (directory.exists()) {
                        File[] files = directory.listFiles();
                        if (files != null && files.length > 2500) {
                            ArrayList<String> callsPathList = DatabaseHelper.getInstance().getDeleteCallsPathForFolderLimit();
                            for (String path : callsPathList) {
                                File f = new File(path);
                                if (f.exists()) {
                                    boolean res = f.delete();
                                    Logger.d("file removing res = " + res);
                                } else {
                                    Logger.d("file not exist");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();

    }

    public void controlCallCaptureImages() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    File directory = new File(Environment.getExternalStorageDirectory() + "/AlfanarCallCaptureImages");
                    if (directory.exists()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            ArrayList<String> imagesPathList = DatabaseHelper.getInstance().getAllImagesPath();
                            ArrayList<String> deleteList = new ArrayList<>();
                            for (File file : files) {
                                if (!imagesPathList.contains(file.getAbsolutePath())) {
                                    deleteList.add(file.getAbsolutePath());
                                }
                            }
                            if (deleteList.size() > 0) {
                                for (String path : deleteList) {
                                    File f = new File(path);
                                    if (f.exists()) {
                                        boolean res = f.delete();
                                        Logger.d("file removing res = " + res);
                                    } else {
                                        Logger.d("file not exist");
                                    }
                                }

                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
            }
        }.start();

    }

    public int getRelayCheck() {
        return MyUtils.instance.sp.getInt("relaycheck", 0);
    }

    public void setRelayCheck(int value) {
        MyUtils.instance.sp.edit().putInt("relaycheck", value).apply();
    }

    public String getBaseVer() {
        return MyUtils.getInstance().getShared().getString("base_version", "0.00");
    }

    public String getFwVer() {
        return Build.DISPLAY;
    }











    /*public void setDhcpOn() {
        try {
            Logger.d("Switching Dhcp On...");
            Class<?> ethernetManagerClass = Class.forName("android.net.ethernet.EthernetManager");
            Class<?> ethernetDevInfoClass = Class.forName("android.net.ethernet.EthernetDevInfo");

            Method methodGetInstance = ethernetManagerClass.getMethod("getInstance");
            Object ethernetManagerObject = methodGetInstance.invoke(ethernetManagerClass);

            Method m1 = ethernetManagerClass.getMethod("getSavedConfig");
            Object obj = m1.invoke(ethernetManagerObject);

            Method m9 = obj.getClass().getMethod("setHwaddr", String.class);
            m9.invoke(obj, getMACAddress());

            Method m8 = obj.getClass().getMethod("setConnectMode", Integer.TYPE);
            m8.invoke(obj, 1);

            Method m2 =
                    ethernetManagerObject
                            .getClass()
                            .getMethod("updateDevInfo", ethernetDevInfoClass);
            m2.invoke(ethernetManagerObject, obj);

        } catch (Exception e) {
             Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }*/





    public String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                String interfaceName = networkInterface.getName().toLowerCase();

                // Arayüz adı "eth" (Ethernet) veya "wlan" (Wi-Fi) ile başlamıyorsa atla
                if (!interfaceName.startsWith("eth") && !interfaceName.startsWith("wlan")) {
                    continue;
                }

                for (Enumeration<InetAddress> enumIpAddress = networkInterface.getInetAddresses(); enumIpAddress.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddress.nextElement();

                    // Loopback (localhost) adresi değilse ve IPv4 formatındaysa IP'yi döndür
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        // Log.d("","getIpAddress = " + inetAddress.getHostAddress());
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

        return "192.168.256.256";
    }

    public  void setTextEn(TextView tv, String text) {
        tv.setTextLocale(Locale.US);

        tv.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        tv.setTextDirection(View.TEXT_DIRECTION_LTR);

        // 2) LRM karakteri: bidi/digit shaping'i çok kez çözer
        String forced = "\u200E" + text;   // LRM + text

        // 3) LocaleSpan ile US locale uygula
        SpannableString ss = new SpannableString(forced);
        ss.setSpan(new LocaleSpan(Locale.US), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);



        tv.setText(ss);

    }


    private long lastNetworkResetTime = 0;
    private static final long NETWORK_RESET_DEBOUNCE = 5000;

    public void resetNetworkComponents(Context context) {
        long now = System.currentTimeMillis();
        if (now - lastNetworkResetTime < NETWORK_RESET_DEBOUNCE) {
            Logger.d("resetNetworkComponents SKIPPED (debounce)");
            return;
        }
        lastNetworkResetTime = now;

        String newIp = getIpAddress();
        Logger.d("resetNetworkComponents START — yeni IP: " + newIp);

        // 1. MulticastLock yeniden al
        try {
            android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                android.net.wifi.WifiManager.MulticastLock lock =
                        wifi.createMulticastLock("AlfanarLock");
                lock.setReferenceCounted(false);
                lock.acquire();
                Logger.d("resetNetworkComponents — MulticastLock yeniden alındı");
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        // 2. Cihaz listelerini temizle
        try {
            DeviceController.getInstance().clearLists();
            Logger.d("resetNetworkComponents — cihaz listeleri temizlendi");
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        // 3. MultiCastThread'i yeniden başlat
        try {
            MultiCastThread oldThread = MultiCastThread.getInstance();
            if (oldThread != null) {
                oldThread.requestRestart();
                Logger.d("resetNetworkComponents — MultiCastThread yeniden başlatıldı");
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        // 4. Linphone ağ değişikliğini bildir
        try {
            org.linphone.core.Core core = LinphoneManager.getInstance().getCore();
            if (core != null) {
                core.setNetworkReachable(false);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        core.setNetworkReachable(true);
                        Logger.d("resetNetworkComponents — Linphone ağ yeniden aktif");
                    } catch (Exception e) {
                        Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                    }
                }, 1500);
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        // 5. Cihazları yeniden keşfet
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                getDeviceStates();
                Logger.d("resetNetworkComponents — getDeviceStates çağrıldı");
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }, 4000);

        Logger.d("resetNetworkComponents COMPLETED");
    }


    private static volatile String cachedSerial;
    public static String getCpuSerial() {
        // ro.serialno pratikte değişmez -> cache
        if (cachedSerial != null){
            Log.d("getCpuSerial"," cachedSerial = "+ cachedSerial);
            return cachedSerial;
        }

        String serial = "UNKNOWN";



        // 2) Fallback: getprop
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("getprop ro.serialno");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                line = line.trim();
                if (!line.isEmpty()) serial = line;
            }
        } catch (Exception ignored) {
        } finally {
            try { if (reader != null) reader.close(); } catch (IOException ignored) {}
            if (process != null) process.destroy();
        }

        cachedSerial = serial;
        Log.d("getCpuSerial"," serial = "+ serial);
        return serial;
    }

    private static String ETH_MAC="";
    public synchronized String getMACAddress() {
        if(!ETH_MAC.isEmpty()){
            Log.i("getMACAddress ","cached mac = " + ETH_MAC);
            return ETH_MAC;
        }
        // Process yerine dosya oku
        try (BufferedReader br = new BufferedReader(new java.io.FileReader("/data/mac5.txt"))) {
            String line = br.readLine();
            if (line == null) return "EMPTY";
            ETH_MAC = line.trim().toUpperCase();
            return ETH_MAC;
        } catch (Exception e) {
            e.printStackTrace();
            return "EMPTY";
        }

    }

    public boolean isInCall() {
        String strActivity = getTopActivity();
        return LinphoneManager.getInstance().getCore().inCall() || strActivity.contains("IncomingDoorConnected") || strActivity.contains("IncomingDoorParalelConnected") || strActivity.contains("IncomingDoorParalelRinging") || strActivity.contains("IncomingRoomConnected") || strActivity.contains("IncomingRoomRinging") || strActivity.contains("OutGoingDoorConnected") || strActivity.contains("OutGoingRoomCalling") || strActivity.contains("OutGoingRoomConnected");
    }

    public String getTopActivity() {
        ActivityManager am = (ActivityManager) MainActivity.getInstance().getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        String strActivity = taskInfo.get(0).topActivity.getClassName();
        Logger.d("getTopActivity CURRENT Activity :: " + strActivity);
        return strActivity;
    }

    public void backToRootActivity() {
        Intent startApp = new Intent(MainActivity.getInstance(), MainActivity.class);
        startApp.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startApp.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // startApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity.getInstance().startActivity(startApp);
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void hideNavigation(Activity context) {
        View decorView = context.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public int dpToPx(int dp) {
        Resources r = App.getInstance().getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public void expand(final View v) {
        int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
        int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
        final int targetHeight = v.getMeasuredHeight();

        ValueAnimator anim = ValueAnimator.ofInt(v.getMeasuredHeightAndState(), targetHeight);
        anim.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                // int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                layoutParams.height = targetHeight;
                v.setLayoutParams(layoutParams);
            }
        });
        anim.start();
    }

    public void collapse(final View v, int height) {
        // final int initialHeight = v.getMeasuredHeight();
        ValueAnimator anim = ValueAnimator.ofInt(v.getMeasuredHeightAndState(), MyUtils.getInstance().dpToPx(height));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
                layoutParams.height = val;
                v.setLayoutParams(layoutParams);
            }
        });
        anim.start();
    }

    public void rebootDevice(int delay) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent("COM.MULTITEK.REBOOT");
                i.addFlags(Intent.FLAG_FROM_BACKGROUND);
                App.getInstance().sendBroadcast(i);

            }
        },delay);
    }

    public boolean checkPermission(String per) {
        int checkVal = App.getInstance().checkCallingOrSelfPermission(per);
        return checkVal == PackageManager.PERMISSION_GRANTED;
    }





    public void wakeUp() {

        if(ScreensaverActivity.instance!=null){
            ScreensaverActivity.instance.destroyActivity();
        }
        /*if (pm == null) {
            System.out.println("UtilWakeUp pm NULL");
            pm = (PowerManager) App.getInstance().getSystemService(Context.POWER_SERVICE);
        } else {
            System.out.println("UtilWakeUp pm OK");
        }
        if (screen_wakeLock == null) {
            System.out.println("UtilWakeUp screen_wakeLock NULL");
            screen_wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "util:WakeLock");
        } else {
            System.out.println("UtilWakeUp screen_wakeLock OK");
        }

        if (screen_wakeLock.isHeld()) {
            System.out.println("UtilWakeUp screen_wakeLock NOT RELEASED its Active");
        } else {
            System.out.println("UtilWakeUp screen_wakeLock RELEASED create new Wakelock");
            screen_wakeLock.acquire(90 * 1000L);
        }*/
    }

    public void restartApp() {
        backToRootActivity();
        ProcessPhoenix.triggerRebirth(App.getInstance());
    }

    public void restartApp3() {
        ProcessPhoenix.triggerRebirth(App.getInstance());
    }





    public boolean isNotInCall() {
        return IncomingDoorConnected.getInstance() == null && IncomingDoorParallelConnected.getInstance() == null && IncomingRoomRinging.getInstance() == null && IncomingRoomConnected.getInstance() == null && IncomingDoorParallelRinging.getInstance() == null && OutGoingDoorConnected.getInstance() == null && OutGoingRoomConnected.getInstance() == null && OutGoingRoomCalling.getInstance() == null;
    }

    public Dialog dialogPublic(Context context, int dialogId) { //all_dialogs_call_with_layout_id
        Dialog dialog = new Dialog(context);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable d = new ColorDrawable(ContextCompat.getColor(context, R.color.black));
        d.setAlpha(125);
        dialog.setContentView(dialogId);
        if (dialog.getWindow() != null) {
            View decorView = dialog.getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(d);
        }
        return dialog;
    }

    public Dialog dialogPublic2(Context context) { //all_dialogs_call_with_layout_id
        Dialog dialog = new Dialog(context);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable d = new ColorDrawable(ContextCompat.getColor(context, R.color.black));
        d.setAlpha(125);
        if (dialog.getWindow() != null) {
            View decorView = dialog.getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(d);
        }
        return dialog;
    }

    public int generateUniqueIDForDB(int value, int value2) {
        String timeMillis = String.valueOf(System.currentTimeMillis());
        String randomUID = UUID.randomUUID().toString();
        String longID = timeMillis + randomUID;
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        if (m != null) {
            m.update(longID.getBytes(), 0, longID.length());
        }
        byte[] p_md5Data = m != null ? m.digest() : new byte[0];
        int sum = 0;
        for (byte p_md5Datum : p_md5Data) {
            int b = (0xFF & p_md5Datum);
            sum += b;
        }
        Random rn = new Random();

        int randomX = rn.nextInt(999999);
        int finalVal = randomX * sum;
        if (finalVal < 0) {
            finalVal = -1 * finalVal;
        }
        finalVal = (value2 * value) / 10 + finalVal;
        if (finalVal > 2147400000) {
            finalVal = finalVal / 10;

        }
        return finalVal;
    }

    public int generateUniqueID() {
        String timeMillis = String.valueOf(System.currentTimeMillis());
        String randomUID = UUID.randomUUID().toString();
        String longID = timeMillis + randomUID;
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        if (m != null) {
            m.update(longID.getBytes(), 0, longID.length());
        }
        byte[] p_md5Data = m != null ? m.digest() : new byte[0];
        int sum = 0;
        for (byte p_md5Datum : p_md5Data) {
            int b = (0xFF & p_md5Datum);
            sum += b;
        }
        Random rn = new Random();

        int randomX = rn.nextInt(999999);
        int finalVal = randomX * sum;
        if (finalVal < 0) {
            finalVal = -1 * finalVal;
        }
        return finalVal;
    }

    public int[] convertTemperature(String dVal) {

        Logger.d("convertTemperature.selectedTemperature " + dVal);
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
        int temperature = 0;
        try {
            temperature = (int) (Objects.requireNonNull(numberFormat.parse(dVal)).doubleValue() * 100);
        } catch (ParseException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }

        if (temperature > 2047 && temperature < 4096) {
            temperature = temperature / 2;
            temperature = temperature + 2048;
        } else if (temperature > 4095) {
            temperature = temperature / 4;
            temperature = temperature + 4097;
        }

        int[] intTemperature = new int[2];
        intTemperature[1] = temperature % 256;
        intTemperature[0] = temperature / 256;

        Logger.d("convertTemperature.temperature3 " + Arrays.toString(intTemperature));
        return intTemperature;
    }

    public int getScreenPositionCheck() {
        return getShared().getInt("screen", 0);
    }

    public void setScreenPositionCheck(int value) {
        getShared().edit().putInt("screen", value).apply();
    }

    public void getDeviceStates() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                }
                GeneralModel mes = new GeneralModel("", "");
                mes.setType("GET_DEVICE_STATES");
                MulticastPublisher.send(new Gson().toJson(mes));
            }
        }.start();
    }



    public ArrayList<CountryModel> loadWeatherCountryList() {
        // String fullName = "weather_country_indexed.json";

        try (InputStream raw = App.getInstance().getAssets().open("weather_country_indexed.json");
             InputStream is = new BufferedInputStream(raw, 256 * 1024);
             Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(r, 128 * 1024)) {

            CountryListModel model = new Gson().fromJson(br, CountryListModel.class);
            return model != null && model.getCountryList() != null ? model.getCountryList() : new ArrayList<>();
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void resetEthernet() {
        Logger.d("MyUtils.resetEthernet");
        ethernetState = false;
        // MainActivity.getInstance().ethChanged();

        if (ethernetCounter > 30) {
            rebootDevice(0);
            ethernetCounter = 0;
        } else {
            ethernetCounter++;
            App.getInstance().sendBroadcastAsUser(new Intent("COM.MULTITEK.ETHOFF"), android.os.Process.myUserHandle());
            SystemClock.sleep(5000);
            App.getInstance().sendBroadcastAsUser(new Intent("COM.MULTITEK.ETHON"), android.os.Process.myUserHandle());
        }
    }

    public void setViewClickTimeout(View view, int delay, Boolean... animation) {
        boolean animationApply = animation == null || animation.length == 0 || animation[0];
        if (animationApply) {
            view.startAnimation(MyUtils.getInstance().buttonClickAnimation);
        }
        view.setEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> view.setEnabled(true), delay);
    }


    public void playNotification(Context context) {
        try {
            if (mplayerNotification != null) {
                mplayerNotification.pause();
                mplayerNotification.stop();
                mplayerNotification.release();
                mplayerNotification = null;
            }
        } catch (IllegalStateException e) {
            Log.e("EXCEPTION", Log.getStackTraceString(e));
        }
        mplayerNotification = MediaPlayer.create(context, R.raw.tone_notification);
        mplayerNotification.setLooping(false);
        mplayerNotification.start();
    }

    public ColorStateList getErrorTextInputColor() {
        int[][] states = new int[][]{new int[]{android.R.attr.state_enabled}};
        int[] colors = new int[]{App.getInstance().getColor(R.color.red_new)};
        return new ColorStateList(states, colors);
    }

    public boolean isSupport2Relay(String fwVersion) {
        if (fwVersion != null && !fwVersion.isEmpty()) {
            String pattern = ".*[Vv]([0-9]+(?:\\.[0-9]+)?)$";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(fwVersion.trim());
            if (matcher.matches()) {
                try {
                    float versionFloat = Float.parseFloat(Objects.requireNonNull(matcher.group(1)));
                    int version = (int) (versionFloat * 100); // 8.25 → 825
                    return version > 650;
                } catch (Exception e) {
                    return true;
                }
            }

        }
        return true;


    }

    private void hideStatusBarAndKeyboard() {
        Intent i1 = new Intent();
        i1.setAction("MULTITEK_SETKEYBOARD_LAYOUT");
        App.getInstance().sendBroadcast(i1);
        SystemClock.sleep(500);
        Intent i2 = new Intent();
        i2.setAction("HIDE_STATUS_BAR");
        App.getInstance().sendBroadcast(i2);
    }




    private  final String TAG = "EthernetDhcp";

    @SuppressLint("WrongConstant")
    public  void dhcpOn(Context ctx) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Android 6'da bazen Context.ETHERNET_SERVICE yok; string ile almak daha garantili
                Object ethernetManager = ctx.getSystemService("ethernet");
                if (ethernetManager == null) {
                    Log.e(TAG, "EthernetManager is null (no ethernet service?)");
                    return ;
                }

                Class<?> ipConfCl = Class.forName("android.net.IpConfiguration");
                Class<?> ipAssignCl = Class.forName("android.net.IpConfiguration$IpAssignment");
                Class<?> proxyCl = Class.forName("android.net.IpConfiguration$ProxySettings");
                Class<?> staticIpCl = Class.forName("android.net.StaticIpConfiguration");
                Class<?> proxyInfoCl = Class.forName("android.net.ProxyInfo");

                Object DHCP = Enum.valueOf((Class<Enum>) ipAssignCl, "DHCP");
                Object NONE = Enum.valueOf((Class<Enum>) proxyCl, "NONE");

                Constructor<?> ctor = ipConfCl.getConstructor(ipAssignCl, proxyCl, staticIpCl, proxyInfoCl);
                Object ipConf = ctor.newInstance(DHCP, NONE, null, null);

                Method setConfiguration = ethernetManager.getClass().getMethod("setConfiguration", ipConfCl);
                setConfiguration.invoke(ethernetManager, ipConf);

                Log.i(TAG, "Ethernet set to DHCP OK");


            } catch (SecurityException se) {
                Log.e(TAG, "SecurityException: app is not privileged/system-signed", se);

            } catch (Throwable t) {
                Log.e(TAG, "Failed to set Ethernet DHCP", t);
                ctx.sendBroadcastAsUser(new Intent("COM.MULTITEK.DHCP_ON"), android.os.Process.myUserHandle());
            }finally {
                MyUtils.getInstance().getShared().edit().putBoolean("STATIC_IP_MODE", false).commit();
            }
        });


    }




    public int getScreenOffTimeOut(){
        int index = sp.getInt("timeout_index", 0);
        // int index = 0;
        int value = 60 * 60 * 1000;
        switch (index) {
            case 0:
                value = Integer.MAX_VALUE;
                break;
            case 1:
                value = 2 * 60 * 1000;
                break;

            case 2:
                value = 5 * 60 * 1000;
                break;

            case 3:
                value = 10 * 60 * 1000;
                break;

            case 4:
                value =  30 * 60 * 1000;
                break;

            case 5:
                value = Integer.MAX_VALUE;
                break;

            default:
                break;
        }

        return value;
    }

    @SuppressLint("WrongConstant")
    public void dhcpOff(Context ctx) {
        new Handler(Looper.getMainLooper()).post(() -> {
            boolean success = false;
            try {
               Object ethernetManager = App.getInstance().getSystemService("ethernet");
                if (ethernetManager == null) {
                    Log.e(TAG, "EthernetManager is null");
                    return;
                }


                String ip = ipFromMac();
                Log.i(TAG, "staticIpOn MAC'ten üretilen IP: " + ip);
                int prefixLen  = 24;
                String gateway = "192.168.2.1";    // ulaşılamaz ama boş bırakma,



                Class<?> ipConfCl   = Class.forName("android.net.IpConfiguration");
                Class<?> ipAssignCl = Class.forName("android.net.IpConfiguration$IpAssignment");
                Class<?> proxyCl    = Class.forName("android.net.IpConfiguration$ProxySettings");
                Class<?> staticIpCl = Class.forName("android.net.StaticIpConfiguration");
                Class<?> linkAddrCl = Class.forName("android.net.LinkAddress");

                Object STATIC = Enum.valueOf((Class<Enum>) ipAssignCl, "STATIC");
                Object NONE   = Enum.valueOf((Class<Enum>) proxyCl, "NONE");
                // StaticIpConfiguration

                Object staticCfg = staticIpCl.getConstructor().newInstance();

                staticIpCl.getField("ipAddress").set(staticCfg,
                        linkAddrCl.getConstructor(InetAddress.class, int.class)
                                .newInstance(InetAddress.getByName(ip), prefixLen));

                staticIpCl.getField("gateway").set(staticCfg,
                        InetAddress.getByName(gateway));

                // dnsServers — final ArrayList, add() ile ekle, replace etme
                Field fDns = staticIpCl.getField("dnsServers");
                ArrayList<InetAddress> dnsList = (ArrayList<InetAddress>) fDns.get(staticCfg);
                dnsList.add(InetAddress.getByName(gateway)); // dummy DNS, size() > 0 şartı için

                // IpConfiguration — field'lara direkt yaz
                Object ipConf = ipConfCl.getConstructor().newInstance();
                ipConfCl.getField("ipAssignment").set(ipConf, STATIC);
                ipConfCl.getField("proxySettings").set(ipConf, NONE);
                ipConfCl.getField("staticIpConfiguration").set(ipConf, staticCfg);

                Method setConfiguration = ethernetManager.getClass()
                        .getMethod("setConfiguration", ipConfCl);
                setConfiguration.invoke(ethernetManager, ipConf);

                success = true;
                Log.i(TAG, "Ethernet STATIC OK: " + ip + "/" + prefixLen + " gw=" + gateway);



            } catch (SecurityException se) {
                Log.e(TAG, "Uygulama system/privileged değil", se);
            } catch (Throwable t) {
                Log.e(TAG, "Ethernet STATIC başarısız", t);
            } finally {
                MyUtils.getInstance().getShared().edit()
                        .putBoolean("STATIC_IP_MODE", success)
                        .apply(); // commit() yerine apply() — UI thread'i bloklamaz
            }
        });
    }

    private String ipFromMac() {
        try {
            NetworkInterface iface = NetworkInterface.getByName("eth0");
            if (iface == null) return "192.168.2.100"; // fallback

            byte[] mac = iface.getHardwareAddress();
            if (mac == null) return "192.168.2.100";

            // Son 2 byte'ı kullan → 1-254 arası
            int b1 = mac[4] & 0xFF;
            int b2 = mac[5] & 0xFF;

            // 0 ve 255'i dışla, ikisini XOR'la → tek oktet yeterli
            int last = ((b1 ^ b2) % 253) + 2; // 2-254 arası

            return "192.168.2." + last;

        } catch (Throwable t) {
            Log.e(TAG, "MAC okunamadı", t);
            return "192.168.2.100";
        }
    }

    public  void dhcpOff2(Context ctx) {
        new Handler(Looper.getMainLooper()).post(() -> {
            int r = RandomUtils.nextInt(2, 125);
            Intent i = new Intent("COM.MULTITEK.SETIP");
            i.putExtra("IP_ADDR", "192.168.2." + r);
            i.putExtra("GW_ADDR", "192.168.2.1");
            ctx.sendBroadcastAsUser(i, android.os.Process.myUserHandle());
            MyUtils.getInstance().getShared().edit().putBoolean("STATIC_IP_MODE", true).commit();
        });

    }



}
