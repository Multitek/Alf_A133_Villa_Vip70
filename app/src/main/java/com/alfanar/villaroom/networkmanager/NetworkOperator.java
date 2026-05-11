package com.alfanar.villaroom.networkmanager;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.linphone.LinphoneManager;
import org.linphone.core.Core;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class NetworkOperator extends Thread {


    //  private static int internetCounter = 0;

    Process p;

    @Override
    public void run() {
        super.run();

        boolean staticMode = MyUtils.getInstance().getShared().getBoolean("STATIC_IP_MODE", false);
        if (staticMode) {
            return;
        }


        if (MyUtils.getInstance().inUpdateMode) return;

        Core core = LinphoneManager.getInstance().getCore();
        if (core != null) {
            if (core.inCall()) return;
        }

        InetAddress in = getGateway(App.getInstance());

        if (in == null) {
            Logger.d("NetworkOperator.InetAddress gateway NULL1");
            mSleep(10);
            in = getGateway(App.getInstance());
            if (in == null) {
                Logger.d("NetworkOperator.InetAddress gateway NULL2");
                MyUtils.getInstance().resetEthernet();
                return;
            }
        }

        //   Logger.d("NetworkOperator.InetAddress = " + in);
        String gateway = in.getHostAddress();
        String hostname = in.getHostName();
        Logger.d("NetworkOperator.gateway = " + gateway + " hostname = " + hostname);

        if (gateway == null || gateway.isEmpty()) {
            Logger.d("NetworkOperator.InetAddress gateway NULL3");
            MyUtils.getInstance().resetEthernet();
        } else {
            boolean isValid = InetAddressValidator.getInstance().isValidInet4Address(gateway);
            if (isValid) {
                boolean isReachable = isGatewayReachable(gateway);
                if (!isReachable) {
                    MyUtils.getInstance().resetEthernet();
                } else {
                    MyUtils.getInstance().ethernetState = true;
                    MyUtils.getInstance().ethernetCounter = 0;
                    Logger.d("NetworkOperator gateway reachable");

                }
            } else {
                MyUtils.getInstance().resetEthernet();
            }
        }
    }

    public InetAddress getGateway(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            LinkProperties linkProp = connectivityManager.getLinkProperties(activeNetwork);
            if (linkProp != null) {
                for (RouteInfo route : linkProp.getRoutes()) {
                    if (route.isDefaultRoute() && !(route.getGateway() instanceof Inet6Address)) {
                        return route.getGateway();
                    }
                }
            }

        }
        return null;
    }

    private void mSleep(int i) {
        try {
            Thread.sleep(i * 1000L);
        } catch (InterruptedException e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    private boolean isGatewayReachable(String gw) {
        boolean res = checkPort(gw, 80, 3000);
        Logger.d("NetworkOperator.isGatewayReachable.checkPort80 res = " + res);
        if (!res) {
            mSleep(3);
            res = checkHttp("http://" + gw + ":80");
            Logger.d("NetworkOperator.isGatewayReachable.checkHttp res = " + res);
            if (!res) {
                mSleep(3);
                res = checkPing(gw);
                Logger.d("NetworkOperator.isGatewayReachable.checkPing res = " + res);
            }
        }
        return res;
    }

    private boolean checkPing(String gw) {

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Logger.d("NetworkOperator.checkPing.destroyProcess1");
            if (p != null) {
                p.destroy();
                Logger.d("NetworkOperator.checkPing.destroyProcess2");
            }
        }, 5000);


        Runtime runtime = Runtime.getRuntime();
        try {
            p = runtime.exec("/system/bin/ping -c1 " + gw);
            int exitValue = p.waitFor();
            p.destroy();
            p = null;
            return (exitValue == 0);
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        return false;
    }

    private boolean checkHttp(String url) {
        HttpURLConnection connection = null;
        int responseCode = 0;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);

            connection.setRequestMethod("GET");
            connection.addRequestProperty("Accept-Encoding", "identity");
            responseCode = connection.getResponseCode();
            try {
                connection.getInputStream().close();
            } catch (Exception e) {
                // Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
            Logger.d("NetworkOperator.checkHttp  responseCode = " + responseCode);
            connection.disconnect();

        } catch (Exception e) {
            // Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return ((200 <= responseCode && responseCode <= 399) || responseCode == 400 || responseCode == 401 || responseCode == 403 || responseCode == 405 || responseCode == 407);
    }

    private boolean checkPort(String address, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        Socket soc;
        try {
            soc = new Socket();
            soc.connect(new InetSocketAddress(address, openPort), timeOutMillis);
            soc.close();
            return true;
        } catch (IOException ex) {
            return false;
        }

    }

    public void sendBr(String action) {
        App.getInstance().sendBroadcastAsUser(new Intent(action), android.os.Process.myUserHandle());
    }

}
