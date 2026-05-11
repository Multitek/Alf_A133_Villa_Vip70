package com.alfanar.villaroom.util;

import android.os.Handler;
import android.os.Looper;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.activities.DoorsAndRoomsActivity;
import com.alfanar.villaroom.models.DeviceModel;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceController {

    static ConcurrentHashMap<String, DeviceModel> roomMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, DeviceModel> doorMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, TimeoutHandle> map = new ConcurrentHashMap<>();
    private static final Object DOOR_LOCK = new Object();
    private static final Object ROOM_LOCK = new Object();

    private static final class InstanceHolder {
        static final DeviceController instance = new DeviceController();
    }
    public static DeviceController getInstance() {
        return InstanceHolder.instance;
    }


    public DeviceModel getDoorWithMac(String mac) {
        return doorMap.get(mac);
    }

    public DeviceModel getRoomWithMac(String mac) {
        return roomMap.get(mac);
    }

    public DeviceModel getDoorWithIp(String ip) {
        if (ip == null || ip.isEmpty()) return null;

        for (DeviceModel m : doorMap.values()) {
            if (m != null && ip.equals(m.getIp())) {
                return m;
            }
        }
        return null;
    }

    public DeviceModel getRoomWithIp(String ip) {

        if (ip == null || ip.isEmpty()) return null;

        for (DeviceModel m : roomMap.values()) {
            if (m != null && ip.equals(m.getIp())) {
                return m;
            }
        }
        return null;
    }

    public void addDeviceToList(DeviceModel model) {
        final String mac = model.getMac();
        if (mac == null || mac.isEmpty()) return;

        if (mac.startsWith("4C:4B:F9")) {


            TimeoutHandle mapHandler = map.get(mac);
            if(mapHandler!=null){
                mapHandler.cancel();
            }
            map.put(mac,new TimeoutHandle(model));


            if (model.getType().equals("DOOR")) {
                DeviceModel dev = doorMap.get(mac);
                if (dev == null) {
                   doorMap.put(mac,model);
                }else{
                    dev.setName(model.getName());
                    dev.setAppVer(model.getAppVer());
                    dev.setBaseVer(model.getBaseVer());
                    dev.setMac(model.getMac());
                    dev.setSip(model.getSip());
                    dev.setIp(model.getIp());
                    dev.setState(true);
                    dev.setLocationId(model.getLocationId());
                    dev.setRelayCount(model.getRelayCount());
                    dev.setRelay1Name(model.getRelay1Name());
                    dev.setRelay2Name(model.getRelay2Name());
                    dev.setStaticIp(model.isStaticIp());
                }

            } else if (model.getType().equals("ROOM")) {
                DeviceModel dev = roomMap.get(mac);
                if(dev == null){
                    roomMap.put(mac,model);
                }else{
                    dev.setName(model.getName());
                    dev.setAppVer(model.getAppVer());
                    dev.setBaseVer(model.getBaseVer());
                    dev.setMac(model.getMac());
                    dev.setSip(model.getSip());
                    dev.setIp(model.getIp());
                    dev.setState(true);
                    dev.setLocationId(model.getLocationId());
                    dev.setRelayCount(0);
                    dev.setRelay1Name("");
                    dev.setRelay2Name("");
                }
            }
            notifyList();
        }

    }




    public void checkIpConflict() {
        if (MyUtils.getInstance().getShared().getBoolean("STATIC_IP_MODE", false)) {
            for (DeviceModel m : roomMap.values()) {
                if (MyUtils.getInstance().getIpAddress().equals(m.getIp())) {
                    Logger.d("IP Conflict detected");
                    resetIp();
                    break;
                }
            }
        }
    }

    private void resetIp() {
       // MyUtils.getInstance().dhcpOff(App.getInstance());
      //  new Handler(Looper.getMainLooper()).postDelayed(() -> MyUtils.getInstance().restartApp(), 1000);
    }

    public ArrayList<DeviceModel> getDoorsList() {
        return new ArrayList<>(doorMap.values());
    }

    public ArrayList<DeviceModel> getRoomsList() {
        return new ArrayList<>(roomMap.values());
    }

    public void notifyList() {
        DoorsAndRoomsActivity ins = DoorsAndRoomsActivity.getInstance();
        if (ins != null) {
            ins.notifyAdapters();
        }
    }

    private static final class TimeoutHandle {
        private final DeviceModel model;
        private  ScheduledExecutorService scheduledExecutorService;

        TimeoutHandle(DeviceModel model) {
            this.model = model;


           ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutor.schedule(() -> {
                try {
                    Logger.d("DeviceController SCHEDULER: "+model.getMac());
                    removeFromLists(model);
                } finally {
                    // İş bittikten sonra map'ten düş (aynı handle ise)
                    String mac = model.getMac();
                    if (mac != null) {
                        map.remove(mac,this);
                    }
                    DeviceController.getInstance().notifyList();
                }
            }, 100, TimeUnit.SECONDS);
            this.scheduledExecutorService = scheduledExecutor;

        }

        void cancel() {
            Logger.d("DeviceController cancel(): "+model.getMac());
          if(scheduledExecutorService!=null){
               scheduledExecutorService.shutdownNow();
               scheduledExecutorService = null;
           }
        }
    }


    public void clearLists() {
        synchronized (DOOR_LOCK) {
            doorMap.clear();
        }
        synchronized (ROOM_LOCK) {
            roomMap.clear();
        }
        // Tüm timeout handler'ları iptal et
        for (TimeoutHandle handle : map.values()) {
            handle.cancel();
        }
        map.clear();
        notifyList();
        Logger.d("DeviceController clearLists: tüm listeler temizlendi");
    }

    private static void removeFromLists(DeviceModel model) {
        final String mac = model.getMac();
        if (mac == null) return;

        if ("DOOR".equals(model.getType())) {
            synchronized (DOOR_LOCK) {
                doorMap.remove(mac);
            }
        } else {
            synchronized (ROOM_LOCK) {
                roomMap.remove(mac);
            }
        }
    }



}
