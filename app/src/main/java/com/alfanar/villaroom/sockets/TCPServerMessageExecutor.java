package com.alfanar.villaroom.sockets;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.activities.intercom.SetStreamVolumeActivity;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class TCPServerMessageExecutor extends Thread {
    private Socket s;
    private BufferedReader in;

    public TCPServerMessageExecutor(Socket mSocket) {
        s = mSocket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String clientMessage = in.readLine();
            Log.d("alfanar ", "[TCPServer] Receive new message : " + clientMessage);

            if (!DeviceController.getInstance().getDoorsList().isEmpty()) {
                if (clientMessage != null) {
                    if (clientMessage.startsWith("CALL")) {
                        String[] detail = clientMessage.split("&&");
                        if (detail.length > 0 && detail[1] != null) {
                            final CallModel callModel = new Gson().fromJson(detail[1], CallModel.class);
                            if (callModel != null && callModel.getCallState().equals("Calling")) {
                                if (MyUtils.getInstance().getMACAddress().equals(callModel.getCallFrom())) {
                                    return;
                                }

                                try {
                                    if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress())) {
                                        String pathx = MyUtils.getInstance().getDoorImagesDir() + "/" + callModel.getCallId() + ".jpeg";
                                        callModel.setCallImgPath(pathx);
                                        DatabaseHelper.getInstance().insertCall(callModel);
                                        String callerIp = null;
                                        String callFrom = callModel.getCallFrom();
                                        ArrayList<DeviceModel> doors = DeviceController.getInstance().getDoorsList();
                                        for (DeviceModel model : doors) {
                                            if (model.getMac().equals(callFrom)) {
                                                callerIp = model.getIp();
                                            }
                                        }
                                        if (callerIp != null) {
                                            new TCPFileRequest(callerIp, callModel.getCallImgPath()).start();
                                        }
                                    }
                                } catch (Exception e) {
                                    Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
                                } finally {
                                    if (!callModel.getCallTo().equals(MyUtils.getInstance().getMACAddress())) {
                                        DeviceModel door = DeviceController.getInstance().getDoorWithMac(callModel.getCallFrom());
                                        if (door != null) {
                                            if (MyUtils.getInstance().callListener != null) {
                                                MyUtils.getInstance().incomingCall = true;
                                                MyUtils.getInstance().callListener.callStateChanged(callModel);
                                            } else {
                                                MyUtils.getInstance().wakeUp();
                                                MainActivity.getInstance().startIncomingDoorParallelCall(door.getIp(), callModel.getCallId());
                                            }
                                        }
                                    }
                                }

                            } else if (callModel != null && (callModel.getCallState().equals("Answered") || callModel.getCallState().equals("Missed") || callModel.getCallState().equals("Declined"))) {
                                try {
                                    CallModel callModelX = DatabaseHelper.getInstance().getCall(callModel.getCallId());
                                    if (callModelX.getCallImgPath() != null) {
                                        callModel.setCallImgPath(callModelX.getCallImgPath());
                                    }

                                    if (callModel.getCallState().equals("Declined") || (callModel.getCallState().equals("Answered") && !callModel.getCallTo().equals("Mobile"))) {
                                        callModel.setCallReadState(true);
                                    }

                                    DatabaseHelper.getInstance().updateCall(callModel);

                                } catch (Exception e) {
                                    Logger.e("EXCEPTION = ", e);
                                } finally {
                                    if (MyUtils.getInstance().callListener != null) {
                                        MyUtils.getInstance().callListener.callStateChanged(callModel);
                                    } else {
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            if (MyUtils.getInstance().callListener != null) {
                                                MyUtils.getInstance().callListener.callStateChanged(callModel);
                                            }
                                        }, 500);
                                    }
                                    if (MyUtils.getInstance().historyListener != null) {
                                        MyUtils.getInstance().historyListener.refreshCalls();
                                    }
                                }

                            }
                        }
                    } else if (clientMessage.startsWith("SET_DOOR_SPEAKER_VOL")) {
                        String vol = clientMessage.split("#")[1];
                        if (SetStreamVolumeActivity.instance != null) {
                            SetStreamVolumeActivity.instance.setDoorVol(vol);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }

            try {
                s.close();
            } catch (Exception ignore) {
            }

            in = null;
            s = null;


        }
    }
}
