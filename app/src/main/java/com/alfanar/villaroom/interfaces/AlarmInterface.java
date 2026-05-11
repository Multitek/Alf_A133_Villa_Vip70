package com.alfanar.villaroom.interfaces;

public interface AlarmInterface {
    void onMotionAlarmSetEventDone();

    void onMotionAlarmDetected(int zoneID);

    void onAllMotionZonesClosed(boolean selectCancelled);

    void onLeakageAlarmDetected();
}
