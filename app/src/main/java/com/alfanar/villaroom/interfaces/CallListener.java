package com.alfanar.villaroom.interfaces;

import com.alfanar.villaroom.models.CallModel;

public interface CallListener {
    void incomingDoorCall(CallModel callModel, String callerDoorIp);

    void incomingRoomCall(CallModel callModel);

    void startParallelConnected(String remoteIP, String doorCount, String relay1Name, String relay2Name);

    void callStateChanged(CallModel callModel);

}
