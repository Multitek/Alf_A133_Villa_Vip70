package com.alfanar.villaroom.models;

public class CallModel {
    private String callId;
    private String callFrom;
    private String callTo;
    private String callType;
    private String callDate;
    private String callState;
    private String callImgPath;
    private String callerName = "";
    private String callData;
    private boolean callReadState = false;

    public String getCallData() {
        return callData;
    }

    public void setCallData(String callData) {
        this.callData = callData;
    }

    public String getCallImgPath() {
        return callImgPath;
    }

    public void setCallImgPath(String callImgPath) {
        this.callImgPath = callImgPath;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getCallFrom() {
        return callFrom;
    }

    public void setCallFrom(String callFrom) {
        this.callFrom = callFrom;
    }

    public String getCallTo() {
        return callTo;
    }

    public void setCallTo(String callTo) {
        this.callTo = callTo;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getCallDate() {
        return callDate;
    }

    public void setCallDate(String callDate) {
        this.callDate = callDate;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallState() {
        return callState;
    }

    public void setCallState(String callState) {
        this.callState = callState;
    }

    public boolean isCallReadState() {
        return callReadState;
    }

    public void setCallReadState(boolean callReadState) {
        this.callReadState = callReadState;
    }
}
