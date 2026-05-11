package com.alfanar.villaroom.networkmanager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.alfanar.villaroom.sockets.MultiCastThread;
import com.alfanar.villaroom.sockets.TCPServer;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

public final class NetworkWatcher {

    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback networkCallback;
    //private volatile boolean lastState = false;
    private volatile Listener listener;
    public NetworkWatcher(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void start() {
        if (networkCallback != null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Logger.d("NetWatcher.onAvailable network = " + network);
                DeviceController.getInstance().clearLists();
                updateState();
                MultiCastThread.getInstance().requestRestart();
                TCPServer.getInstance().requestRestart();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Logger.d("NetWatcher.onLost");
                updateState();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities nc) {
                Logger.d("NetWatcher.onCapabilitiesChanged");
                updateState();
            }

        };

        NetworkRequest req = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(req, networkCallback);


        updateState();
    }

    public void stop() {
        if (networkCallback == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) {}
        networkCallback = null;
    }

    private void updateState() {
        boolean isConnected = isCurrentlyConnected();
        MyUtils.getInstance().ethernetState = isConnected;
        Logger.d("NetWatcher.updateState isConnected = " + isConnected);

        // UI callback mutlaka main thread
        mainHandler.post(() -> {
            Listener l = listener;
            if (l != null) l.onNetworkChanged(isConnected);

        });
    }

    private boolean isCurrentlyConnected() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) return false;

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        if (caps == null) return false;

        // Hem Ethernet hem de Wi-Fi bağlantısını kabul eder
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public interface Listener {
        void onNetworkChanged(boolean isConnected);
    }
}
