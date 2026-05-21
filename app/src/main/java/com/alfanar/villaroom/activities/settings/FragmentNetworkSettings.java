package com.alfanar.villaroom.activities.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alfanar.villaroom.R;
import com.alfanar.villaroom.databinding.FragmentNetworkSettingsBinding;
import com.alfanar.villaroom.databinding.ItemWifiNetworkBinding;
import com.alfanar.villaroom.util.MyUtils;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint("MissingPermission")
public class FragmentNetworkSettings extends Fragment {

    private static final String TAG = "WifiSettings";
    private static final int PERM_REQ = 2001;

    private FragmentNetworkSettingsBinding binding;
    private WifiManager wifiManager;
    private ConnectivityManager connManager;

    private final List<WifiNetwork> networkList = new ArrayList<>();
    private WifiAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver wifiReceiver;
    private boolean receiverRegistered = false;

    static class WifiNetwork {
        String ssid;
        String bssid;
        int rssi;
        int signalLevel;
        String securityType;
        boolean isSecured;
        boolean isConnected;
        boolean isSaved;
        int networkId = -1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNetworkSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wifiManager = (WifiManager) requireContext()
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        initRecyclerView();
        initListeners();
        registerWifiReceiver();

        if (hasAllPermissions()) {
            updateWifiState();
        } else {
            requestRequiredPermissions();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasAllPermissions()) {
            updateWifiState();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        unregisterWifiReceiver();
        binding = null;
    }

    private boolean hasAllPermissions() {
        boolean hasLocation = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean hasNearby = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNearby = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
        }

        return hasLocation && hasNearby;
    }

    private void requestRequiredPermissions() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        requestPermissions(perms.toArray(new String[0]), PERM_REQ);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQ) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateWifiState();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.wifi_permission_required),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initRecyclerView() {
        adapter = new WifiAdapter();
        binding.recyclerWifi.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerWifi.setAdapter(adapter);
    }

    private void initListeners() {
        binding.switchWifi.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!btn.isPressed()) return;
            setWifiEnabled(isChecked);
            if (isChecked) {
                handler.postDelayed(this::startScan, 2500);
            }
        });

        binding.btnRefresh.setOnClickListener(v -> startScan());

        binding.cardConnected.setOnClickListener(v -> {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                showConnectedDialog(cleanSsid(info.getSSID()), info);
            }
        });
    }

    private void setWifiEnabled(boolean enable) {
        try {
            boolean result = wifiManager.setWifiEnabled(enable);
            Log.d(TAG, "setWifiEnabled(" + enable + ") API result: " + result);
            if (!result) {
                setWifiEnabledViaShell(enable);
            }
        } catch (Exception e) {
            Log.e(TAG, "setWifiEnabled error, trying shell", e);
            setWifiEnabledViaShell(enable);
        }
    }

    private void setWifiEnabledViaShell(boolean enable) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("svc wifi " + (enable ? "enable" : "disable") + "\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                Log.d(TAG, "Shell wifi " + (enable ? "enable" : "disable") + " executed");
            } catch (Exception e) {
                Log.e(TAG, "Shell wifi command failed", e);
                handler.post(() -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                        startActivity(intent);
                    } catch (Exception ex) {
                        Log.e(TAG, "Cannot open wifi settings", ex);
                    }
                });
            }
        }).start();
    }

    private void registerWifiReceiver() {
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isAdded() || binding == null || intent == null
                        || intent.getAction() == null) return;

                switch (intent.getAction()) {
                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                        processScanResults();
                        break;
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        handler.postDelayed(() -> updateWifiState(), 500);
                        break;
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    case ConnectivityManager.CONNECTIVITY_ACTION:
                        handler.postDelayed(() -> {
                            updateConnectionUi();
                            processScanResults();
                        }, 1000);
                        break;
                    case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                        int err = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                        if (err == WifiManager.ERROR_AUTHENTICATING) {
                            handler.post(() -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(),
                                            getString(R.string.auth_failed_check_password),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        break;
                }
            }
        };

        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        f.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(wifiReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(wifiReceiver, f);
        }
        receiverRegistered = true;
    }

    private void unregisterWifiReceiver() {
        if (receiverRegistered && wifiReceiver != null) {
            try {
                requireContext().unregisterReceiver(wifiReceiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }
    }

    private void updateWifiState() {
        if (!isAdded() || binding == null) return;

        boolean enabled = wifiManager.isWifiEnabled();
        binding.switchWifi.setChecked(enabled);

        if (enabled) {
            binding.layoutWifiDisabled.setVisibility(View.GONE);
            binding.recyclerWifi.setVisibility(View.VISIBLE);
            binding.layoutHeader.setVisibility(View.VISIBLE);
            updateConnectionUi();
            startScan();
        } else {
            binding.layoutWifiDisabled.setVisibility(View.VISIBLE);
            binding.recyclerWifi.setVisibility(View.GONE);
            binding.layoutHeader.setVisibility(View.GONE);
            binding.cardConnected.setVisibility(View.GONE);
            binding.txtEmpty.setVisibility(View.GONE);
            networkList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("MissingPermission")
    private void updateConnectionUi() {
        if (!isAdded() || binding == null) return;

        WifiInfo wi = wifiManager.getConnectionInfo();
        NetworkInfo ni = connManager.getActiveNetworkInfo();

        boolean connected = ni != null && ni.isConnected()
                && ni.getType() == ConnectivityManager.TYPE_WIFI
                && wi != null && wi.getNetworkId() != -1;

        if (connected) {
            String ssid = cleanSsid(wi.getSSID());
            if (ssid.isEmpty()) {
                binding.cardConnected.setVisibility(View.GONE);
                return;
            }
            int level = WifiManager.calculateSignalLevel(wi.getRssi(), 5);
            binding.cardConnected.setVisibility(View.VISIBLE);
            binding.txtConnectedSsid.setText(ssid);
            tintSignalIcon(binding.imgConnectedSignal, level);
        } else {
            binding.cardConnected.setVisibility(View.GONE);
        }
    }

    private void startScan() {
        if (!isAdded() || binding == null || !wifiManager.isWifiEnabled()) return;
        if (!hasAllPermissions()) {
            requestRequiredPermissions();
            return;
        }

        binding.progressScanning.setVisibility(View.VISIBLE);
        boolean scanStarted = wifiManager.startScan();
        Log.d(TAG, "startScan result: " + scanStarted);

        if (!scanStarted) {
            handler.postDelayed(this::processScanResults, 500);
        }

        handler.postDelayed(() -> {
            if (isAdded() && binding != null)
                binding.progressScanning.setVisibility(View.GONE);
        }, 12_000);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processScanResults() {
        if (!isAdded() || binding == null) return;
        binding.progressScanning.setVisibility(View.GONE);

        List<ScanResult> scans = null;
        try {
            scans = wifiManager.getScanResults();
        } catch (SecurityException e) {
            Log.e(TAG, "getScanResults permission error", e);
            Toast.makeText(requireContext(),
                    getString(R.string.wifi_scan_permission_required),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (scans == null) scans = new ArrayList<>();

        List<WifiConfiguration> saved = getSavedNetworks();
        WifiInfo current = wifiManager.getConnectionInfo();

        String connectedSsid = "";
        if (current != null && current.getNetworkId() != -1)
            connectedSsid = cleanSsid(current.getSSID());

        Map<String, Integer> savedMap = new HashMap<>();
        if (saved != null) {
            for (WifiConfiguration c : saved) {
                String s = cleanSsid(c.SSID);
                if (!s.isEmpty()) savedMap.put(s, c.networkId);
            }
        }

        Map<String, WifiNetwork> unique = new HashMap<>();
        for (ScanResult sr : scans) {
            if (sr.SSID == null || sr.SSID.isEmpty()) continue;

            WifiNetwork existing = unique.get(sr.SSID);
            if (existing != null && existing.rssi >= sr.level) continue;

            WifiNetwork n = new WifiNetwork();
            n.ssid = sr.SSID;
            n.bssid = sr.BSSID;
            n.rssi = sr.level;
            n.signalLevel = WifiManager.calculateSignalLevel(sr.level, 5);
            n.securityType = parseSecurity(sr.capabilities);
            n.isSecured = !n.securityType.equals("Açık");
            n.isConnected = sr.SSID.equals(connectedSsid);
            n.isSaved = savedMap.containsKey(sr.SSID);
            if (n.isSaved) n.networkId = savedMap.get(sr.SSID);

            unique.put(sr.SSID, n);
        }

        networkList.clear();
        networkList.addAll(unique.values());

        Iterator<WifiNetwork> it = networkList.iterator();
        while (it.hasNext()) {
            if (it.next().isConnected) it.remove();
        }

        Collections.sort(networkList, (a, b) -> {
            if (a.isSaved != b.isSaved) return a.isSaved ? -1 : 1;
            return b.rssi - a.rssi;
        });

        adapter.notifyDataSetChanged();
        binding.txtEmpty.setVisibility(networkList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("MissingPermission")
    private List<WifiConfiguration> getSavedNetworks() {
        try {
            List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
            if (configs != null) return configs;
        } catch (SecurityException e) {
            Log.e(TAG, "getConfiguredNetworks permission error", e);
        }
        return new ArrayList<>();
    }

    private void connectToNetwork(String ssid, String password, String security) {
        removeSavedNetwork(ssid);

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = quote(ssid);

        switch (security) {
            case "WEP":
                conf.wepKeys[0] = quote(password);
                conf.wepTxKeyIndex = 0;
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                break;
            case "Açık":
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
            default:
                conf.preSharedKey = quote(password);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
        }

        int netId = wifiManager.addNetwork(conf);
        Log.d(TAG, "addNetwork result: " + netId);

        if (netId != -1) {
            wifiManager.disconnect();
            boolean enabled = wifiManager.enableNetwork(netId, true);
            boolean reconnected = wifiManager.reconnect();
            wifiManager.saveConfiguration();
            Log.d(TAG, "enableNetwork: " + enabled + ", reconnect: " + reconnected);

            Toast.makeText(requireContext(),
                    getString(R.string.connecting_to_wifi, ssid),
                    Toast.LENGTH_SHORT).show();

            handler.postDelayed(() -> {
                updateConnectionUi();
                processScanResults();
                handler.postDelayed(() -> {
                    if (isAdded()) {
                        MyUtils.getInstance().resetNetworkComponents(requireContext());
                    }
                }, 3000);
            }, 3000);
        } else {
            Log.w(TAG, "addNetwork returned -1, trying shell method");
            connectViaShell(ssid, password, security);
        }
    }

    private void connectViaShell(String ssid, String password, String security) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());

                os.writeBytes("wpa_cli -i wlan0 add_network\n");
                os.writeBytes("wpa_cli -i wlan0 set_network 0 ssid '\"" + ssid + "\"'\n");

                if (security.equals("Açık")) {
                    os.writeBytes("wpa_cli -i wlan0 set_network 0 key_mgmt NONE\n");
                } else if (security.equals("WEP")) {
                    os.writeBytes("wpa_cli -i wlan0 set_network 0 key_mgmt NONE\n");
                    os.writeBytes("wpa_cli -i wlan0 set_network 0 wep_key0 '\"" + password + "\"'\n");
                } else {
                    os.writeBytes("wpa_cli -i wlan0 set_network 0 psk '\"" + password + "\"'\n");
                }

                os.writeBytes("wpa_cli -i wlan0 enable_network 0\n");
                os.writeBytes("wpa_cli -i wlan0 save_config\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();

                handler.post(() -> {
                    Toast.makeText(requireContext(),
                            getString(R.string.connecting_to_wifi, ssid),
                            Toast.LENGTH_SHORT).show();
                    handler.postDelayed(() -> {
                        updateConnectionUi();
                        processScanResults();
                        handler.postDelayed(() -> {
                            if (isAdded()) {
                                MyUtils.getInstance().resetNetworkComponents(requireContext());
                            }
                        }, 3000);
                    }, 4000);
                });

            } catch (Exception e) {
                Log.e(TAG, "Shell connect failed", e);
                handler.post(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.connection_failed),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void connectToSavedNetwork(int networkId, String ssid) {
        wifiManager.disconnect();
        boolean enabled = wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();

        if (enabled) {
            Toast.makeText(requireContext(),
                    getString(R.string.connecting_to_wifi, ssid),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(),
                    getString(R.string.connection_failed),
                    Toast.LENGTH_SHORT).show();
        }

        handler.postDelayed(() -> {
            updateConnectionUi();
            processScanResults();
        }, 3000);
    }

    private void forgetNetwork(int networkId) {
        boolean ok = wifiManager.removeNetwork(networkId);
        wifiManager.saveConfiguration();

        if (ok) {
            Toast.makeText(requireContext(),
                    getString(R.string.network_forgotten),
                    Toast.LENGTH_SHORT).show();
        } else {
            forgetNetworkViaShell(networkId);
        }

        handler.postDelayed(() -> {
            updateConnectionUi();
            startScan();
            handler.postDelayed(() -> {
                if (isAdded()) {
                    MyUtils.getInstance().resetNetworkComponents(requireContext());
                }
            }, 2000);
        }, 1500);
    }

    private void forgetNetworkViaShell(int networkId) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("wpa_cli -i wlan0 remove_network " + networkId + "\n");
                os.writeBytes("wpa_cli -i wlan0 save_config\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                handler.post(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.network_forgotten),
                                Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Shell forget failed", e);
            }
        }).start();
    }

    private void removeSavedNetwork(String ssid) {
        try {
            List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
            if (configs != null) {
                for (WifiConfiguration config : configs) {
                    if (cleanSsid(config.SSID).equals(ssid)) {
                        wifiManager.removeNetwork(config.networkId);
                        wifiManager.saveConfiguration();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "removeSavedNetwork error", e);
        }
    }

    private void disconnectCurrent() {
        wifiManager.disconnect();
        Toast.makeText(requireContext(),
                getString(R.string.connection_lost),
                Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> {
            updateConnectionUi();
            processScanResults();
            MyUtils.getInstance().ethernetState = false;
            if (com.alfanar.villaroom.activities.MainActivity.getInstance() != null) {
                // ethernetState güncellendi, gerekirse UI güncelle
            }
        }, 1500);
    }


    private void showPasswordDialog(WifiNetwork network) {
        if (!isAdded()) return;

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(4));

        TextView title = new TextView(requireContext());
        title.setText(network.ssid);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getTextColor());
        root.addView(title);

        TextView sec = new TextView(requireContext());
        sec.setText(getString(R.string.security_label, network.securityType));
        sec.setTextSize(13);
        sec.setTextColor(getHintColor());
        root.addView(sec);

        String[] signalLabels = getResources().getStringArray(R.array.signal_labels);

        TextView sig = new TextView(requireContext());
        sig.setText(String.format(
                getString(R.string.signal_format),
                signalLabels[Math.min(network.signalLevel, 4)]
        ));

        sig.setTextSize(13);
        sig.setTextColor(getHintColor());
        root.addView(sig);

        EditText edPassword = new EditText(requireContext());
        edPassword.setHint(getString(R.string.enter_password));
        edPassword.setHintTextColor(getHintColor());
        edPassword.setTextColor(getTextColor());

        edPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        edPassword.setTransformationMethod(
                android.text.method.PasswordTransformationMethod.getInstance()
        );

        edPassword.setSingleLine();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        lp.topMargin = dp(12);

        root.addView(edPassword, lp);

        CheckBox cbShow = new CheckBox(requireContext());
        cbShow.setText(getString(R.string.show_password));
        cbShow.setTextSize(13);
        cbShow.setChecked(false);
        cbShow.setTextColor(getTextColor());

        cbShow.setOnCheckedChangeListener((b, checked) -> {

            if (checked) {
                edPassword.setTransformationMethod(
                        android.text.method.HideReturnsTransformationMethod.getInstance()
                );
            } else {
                edPassword.setTransformationMethod(
                        android.text.method.PasswordTransformationMethod.getInstance()
                );
            }

            edPassword.setSelection(edPassword.getText().length());
        });

        root.addView(cbShow);

        AlertDialog dialog = new AlertDialog.Builder(
                requireContext(),
                getDialogTheme()
        )
                .setView(root)
                .setPositiveButton(getString(R.string.connect), (d, w) -> {

                    String pwd = edPassword.getText().toString().trim();

                    if (pwd.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.enter_password_prompt),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (pwd.length() < 8 &&
                            !network.securityType.equals("WEP")) {

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.password_min_length),
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    connectToNetwork(
                            network.ssid,
                            pwd,
                            network.securityType
                    );
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {

            edPassword.setTransformationMethod(
                    android.text.method.PasswordTransformationMethod.getInstance()
            );

            edPassword.setSelection(edPassword.getText().length());

            cbShow.setChecked(false);
        });

        applyImmersiveToDialog(dialog);
    }

    private void showSavedNetworkDialog(WifiNetwork network) {
        if (!isAdded()) return;

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView title = new TextView(requireContext());
        title.setText(network.ssid);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getTextColor());
        root.addView(title);

        TextView info = new TextView(requireContext());
        info.setText(getString(R.string.saved_network, network.securityType));
        info.setTextSize(13);
        info.setTextColor(getHintColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        root.addView(info, lp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), getDialogTheme())
                .setView(root)
                .setPositiveButton(getString(R.string.connect), (d, w) ->
                        connectToSavedNetwork(network.networkId, network.ssid))
                .setNeutralButton(getString(R.string.forget), (d, w) ->
                        forgetNetwork(network.networkId))
                .setNegativeButton(getString(R.string.cancel), null)
                .create();
        applyImmersiveToDialog(dialog);
    }

    private void showConnectedDialog(String ssid, WifiInfo wifiInfo) {
        if (!isAdded()) return;

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView title = new TextView(requireContext());
        title.setText(ssid);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getTextColor());
        root.addView(title);

        addInfoRow(root,
                getString(R.string.status),
                getString(R.string.connected));

        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 5);
        String[] labels = {
                getString(R.string.very_weak),
                getString(R.string.weak),
                getString(R.string.medium),
                getString(R.string.good),
                getString(R.string.excellent)
        };

        addInfoRow(root,
                getString(R.string.signal),
                labels[Math.min(level, 4)]);

        addInfoRow(root,
                getString(R.string.speed),
                getString(R.string.mbps, wifiInfo.getLinkSpeed()));

        int freq = wifiInfo.getFrequency();
        String band = freq > 4900 ? "5 GHz" : "2.4 GHz";
        addInfoRow(root,
                getString(R.string.frequency),
                freq + " MHz (" + band + ")");

        int ip = wifiInfo.getIpAddress();
        if (ip != 0) {
            String ipStr = String.format(Locale.US, "%d.%d.%d.%d",
                    ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
            addInfoRow(root,
                    getString(R.string.ip_address),
                    ipStr);
        }

        if (wifiInfo.getBSSID() != null) {
            addInfoRow(root, "BSSID", wifiInfo.getBSSID());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), getDialogTheme())
                .setView(root)
                .setPositiveButton(getString(R.string.ok), null)
                .setNeutralButton(getString(R.string.forget), (d, w) -> {
                    forgetNetwork(wifiInfo.getNetworkId());
                    disconnectCurrent();
                })
                .setNegativeButton(getString(R.string.disconnect), (d, w) -> disconnectCurrent())
                .create();
        applyImmersiveToDialog(dialog);
    }

    private void showOpenNetworkDialog(WifiNetwork network) {
        if (!isAdded()) return;

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), getDialogTheme())
                .setTitle(network.ssid)
                .setMessage(getString(R.string.open_network_message))
                .setPositiveButton(getString(R.string.connect), (d, w) ->
                        connectToNetwork(network.ssid, "", "Açık"))
                .setNegativeButton(getString(R.string.cancel), null)
                .create();
        applyImmersiveToDialog(dialog);
    }

    private void applyImmersiveToDialog(AlertDialog dialog) {
        Window w = dialog.getWindow();
        if (w == null) {
            dialog.show();
            return;
        }
        w.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController ctrl = w.getInsetsController();
            if (ctrl != null) {
                ctrl.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                ctrl.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            //noinspection deprecation
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemWifiNetworkBinding b = ItemWifiNetworkBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            WifiNetwork net = networkList.get(pos);

            h.b.txtWifiSsid.setText(net.ssid);

            StringBuilder status = new StringBuilder();
            if (net.isSaved) status.append(getString(R.string.saved));
            if (status.length() > 0 && net.isSecured) status.append(" • ");
            status.append(net.securityType);
            h.b.txtWifiStatus.setText(status);

            tintSignalIcon(h.b.imgWifiSignal, net.signalLevel);
            h.b.imgWifiLock.setVisibility(net.isSecured ? View.VISIBLE : View.GONE);
            h.b.cardWifiItem.setOnClickListener(v -> onItemClicked(net));
        }

        @Override
        public int getItemCount() {
            return networkList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ItemWifiNetworkBinding b;

            VH(ItemWifiNetworkBinding binding) {
                super(binding.getRoot());
                b = binding;
            }
        }
    }

    private void onItemClicked(WifiNetwork network) {
        if (network.isSaved) {
            showSavedNetworkDialog(network);
        } else if (network.isSecured) {
            showPasswordDialog(network);
        } else {
            showOpenNetworkDialog(network);
        }
    }

    private String parseSecurity(String caps) {
        if (caps == null) return "Açık";
        caps = caps.toUpperCase(Locale.US);
        if (caps.contains("WPA3")) return "WPA3";
        if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA")) return "WPA";
        if (caps.contains("WEP")) return "WEP";
        return "Açık";
    }

    private String cleanSsid(String ssid) {
        if (ssid == null) return "";
        if (ssid.startsWith("\"") && ssid.endsWith("\""))
            ssid = ssid.substring(1, ssid.length() - 1);
        if (ssid.equals("<unknown ssid>")) return "";
        return ssid;
    }

    private String quote(String s) {
        return String.format("\"%s\"", s);
    }

    private void tintSignalIcon(ImageView iv, int level) {
        iv.setImageResource(com.alfanar.villaroom.R.drawable.ic_wifi);
        int color;
        if (level >= 3) color = Color.parseColor("#4CAF50");
        else if (level == 2) color = Color.parseColor("#FF9800");
        else color = Color.parseColor("#F44336");
        iv.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        iv.setAlpha(level == 0 ? 0.45f : 1f);
    }

    private boolean isDarkTheme() {
        return MyUtils.getInstance().darkTheme;
    }

    private int getDialogTheme() {
        return isDarkTheme() ? R.style.DarkDialogTheme : R.style.LightDialogTheme;
    }

    private int getTextColor() {
        return isDarkTheme() ? Color.WHITE : Color.BLACK;
    }

    private int getHintColor() {
        return isDarkTheme() ? Color.LTGRAY : Color.GRAY;
    }

    private void addInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(8);
        parent.addView(row, rlp);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(getHintColor());
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tvLabel);

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(14);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextColor(getTextColor());
        row.addView(tvValue);
    }

    private int dp(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}