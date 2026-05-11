package org.linphone;

import static org.linphone.mediastream.Factory.DEVICE_HAS_BUILTIN_AEC;
import static org.linphone.mediastream.Factory.DEVICE_HAS_BUILTIN_AEC_CRAPPY;
import static org.linphone.mediastream.Factory.DEVICE_HAS_CRAPPY_AAUDIO;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alfanar.villaroom.App;
import com.alfanar.villaroom.R;
import com.alfanar.villaroom.activities.MainActivity;
import com.alfanar.villaroom.activities.intercom.OutGoingDoorConnected;
import com.alfanar.villaroom.activities.intercom.OutGoingRoomCalling;
import com.alfanar.villaroom.models.CallModel;
import com.alfanar.villaroom.models.DeviceModel;
import com.alfanar.villaroom.sockets.TCPFileRequest;
import com.alfanar.villaroom.util.AppEnums;
import com.alfanar.villaroom.util.DatabaseHelper;
import com.alfanar.villaroom.util.DeviceController;
import com.alfanar.villaroom.util.Logger;
import com.alfanar.villaroom.util.MyUtils;
import com.alfanar.villaroom.util.TimeManager;
import com.google.gson.Gson;

import org.linphone.core.Account;
import org.linphone.core.Address;
import org.linphone.core.AudioDevice;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.CallStats;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.ChatRoomParams;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.GlobalState;
import org.linphone.core.LogCollectionState;
import org.linphone.core.LogLevel;
import org.linphone.core.LoggingService;
import org.linphone.core.MediaDirection;
import org.linphone.core.NatPolicy;
import org.linphone.core.PayloadType;
import org.linphone.core.Reason;
import org.linphone.core.RegistrationState;
import org.linphone.core.ToneID;
import org.linphone.core.Transports;
import org.linphone.core.VideoActivationPolicy;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.UUID;

public class LinphoneManager extends CoreListenerStub {

    private Core mCore;
    private HandlerThread linphoneThread;
    private Handler linphoneHandler;


    private void runOnLinphoneThread(Runnable r) {
        Handler h = linphoneHandler;
        if (h != null) h.post(r);
    }


    private final Runnable iterateLoop = new Runnable() {
        @Override public void run() {
            mCore.iterate();
            linphoneHandler.postDelayed(this, 20);
        }
    };

    public void startIterate() {
        if (linphoneHandler != null) {
            linphoneHandler.removeCallbacks(iterateLoop);
            linphoneHandler.post(iterateLoop);
        }
    }

    public void stopIterate() {
        if (linphoneHandler != null) {
            linphoneHandler.removeCallbacks(iterateLoop);
        }
    }



    public Core getCore() {
        return mCore;
    }


    private static class SingletonHelper {
        private static final LinphoneManager INSTANCE = new LinphoneManager();
    }

    private LinphoneManager() {
        PowerManager pm = (PowerManager) App.getInstance().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock cpu_wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lm:wakeLock");
        cpu_wakeLock.acquire(60 * 1000L);

    }

    public static LinphoneManager getInstance() {
        return SingletonHelper.INSTANCE;
    }



    public synchronized void startLibLinphone() {
        Logger.d("LinphoneManager.startLibLinphone");
        try {

            if (linphoneThread != null) return; // zaten başladıysa

            linphoneThread = new HandlerThread("LinphoneThread");
            linphoneThread.start();
            linphoneHandler = new Handler(linphoneThread.getLooper());

            linphoneHandler.post(() -> {
                Factory.instance().enableLogCollection(LogCollectionState.Disabled);
                Factory.instance().enableLogcatLogs(true);
                LoggingService loggingService = Factory.instance().getLoggingService();
                loggingService.setLogLevel(LogLevel.Debug);

                mCore = Factory.instance().createCore(null, null, App.getInstance());
                mCore.addListener(this);
                mCore.setAutoIterateEnabled(false);
                mCore.clearCallLogs();


                mCore.setIpv6Enabled(false);

                mCore.setKeepAliveEnabled(false);
                mCore.setMicEnabled(true);


                Transports transports = mCore.getTransports();
                transports.setUdpPort(5060);
                transports.setTcpPort(-2);
                transports.setTlsPort(-2);
                mCore.setTransports(transports);

                NatPolicy nat = mCore.createNatPolicy();
                nat.setStunServer("");
                nat.setStunEnabled(false);
                nat.setIceEnabled(false);
                nat.setTurnEnabled(false);
                Logger.d("ICE_DISABLED");
                mCore.setNatPolicy(nat);
                mCore.setInCallTimeout(200);


                VideoActivationPolicy vap = mCore.getVideoActivationPolicy().clone();
                vap.setAutomaticallyInitiate(true);
                vap.setAutomaticallyAccept(true);
                mCore.setVideoActivationPolicy(vap);



                mCore.setUserAgent("vip70", "123");


                mCore.setSelfViewEnabled(false);
                mCore.usePreviewWindow(false);
                mCore.setVideoCaptureEnabled(false);
                mCore.setVideoPreviewEnabled(false);



                mCore.setNativeRingingEnabled(true);
                mCore.setRing(null);
                mCore.setRingback(null);
                mCore.setTone(ToneID.Busy,null);
                mCore.setTone(ToneID.CallEnd, null);
                mCore.setTone(ToneID.CallLost, null);
                mCore.setTone(ToneID.Undefined, null);
                mCore.setTone(ToneID.CallNotAnswered, null);

                mCore.getConfig().setBool("misc", "use_native_ringing", false);
                mCore.getConfig().setBool("app", "use_in_app_ringing", false);

                mCore.setCallToneIndicationsEnabled(false);
                mCore.setRemoteRingbackTone(null);

                mCore.setRingerDevice(null);
                mCore.disableCallRinging(true);

                mCore.getConfig().setBool("audio", "android_disable_audio_focus_requests", true);
                mCore.getConfig().setBool("audio", "disable_ringing_audio_focus", true);
                mCore.getConfig().setBool("misc", "enable_caller_id", false);


                mCore.setVideoDevice("StaticImage: Static picture");
                //mCore.setVideoPreset("custom");
                //mCore.setPreferredVideoDefinition(Factory.instance().createVideoDefinitionFromName("vga"));


                mCore.setAudioPortRange(49200, 65300);
                mCore.setVideoPortRange(49200, 65300);
                mCore.setRtpBundleEnabled(true);

                mCore.setPreferredFramerate(10);
                mCore.setUploadBandwidth(750);
                mCore.setDownloadBandwidth(750);
                mCore.setAudioAdaptiveJittcompEnabled(false);
                mCore.setVideoAdaptiveJittcompEnabled(false);
                mCore.setAudioJittcomp(80);
                mCore.setVideoJittcomp(80);
                mCore.setAdaptiveRateControlEnabled(false);


                mCore.setEchoCancellationEnabled(false);
            /*    mCore.setEchoLimiterEnabled(false);
                mCore.setGenericComfortNoiseEnabled(true);  // NS
                mCore.setAgcEnabled(true);                  // AGC
                mCore.getConfig().setBool("sound", "noisesuppression", false);
                mCore.getConfig().setBool("sound", "agc", false);*/


                PayloadType[] apt = mCore.getAudioPayloadTypes().clone();
                for (PayloadType pt : apt) {
                    pt.enable(pt.getMimeType().toUpperCase().startsWith("PCM"));
                }
                mCore.setAudioPayloadTypes(apt);

                AudioDevice[] ad = mCore.getAudioDevices();
                for (AudioDevice a : ad) {

                    /*if(a.getCapabilities().toInt()==3){
                        mCore.setDefaultInputAudioDevice(a);
                        mCore.setDefaultOutputAudioDevice(a);
                        System.out.println("debugCodec setMic " + a.getDriverName());
                        System.out.println("debugCodec setSpk " + a.getDriverName());
                        break;
                    }*/
                    if (a.getType().equals(AudioDevice.Type.Microphone)) {
                        System.out.println("debugCodec setMic " + a.getDriverName());
                        mCore.setDefaultInputAudioDevice(a);
                    }

                    if (a.getType().equals(AudioDevice.Type.Speaker)) {
                        System.out.println("debugCodec setSpk " + a.getDriverName());
                        mCore.setDefaultOutputAudioDevice(a);
                    }
                    System.out.println("debugCodecList getDriverName = " + a.getDriverName() + " type = " + a.getType() + " deviceName = " + a.getDeviceName() + " cap = " + a.getCapabilities().toInt());

                }

                PayloadType[] vpt = mCore.getVideoPayloadTypes().clone();
                for (PayloadType pt : vpt) {
                    Logger.d("VideoCodecX isUsable= " + pt.isUsable() + " enabled= " + pt.enabled() + " desc= " + pt.getDescription());
                    if(pt.getMimeType().equalsIgnoreCase("VP8")){
                        pt.enable(true);
                    }else {
                        pt.enable(false);
                    }
                }
                mCore.setVideoPayloadTypes(vpt);



                mCore.clearAllAuthInfo();
                mCore.clearProxyConfig();
                mCore.clearAccounts();

                mCore.getMediastreamerFactory().setDeviceInfo(
                        android.os.Build.MANUFACTURER,
                        android.os.Build.MODEL,
                        android.os.Build.BOARD,
                        DEVICE_HAS_BUILTIN_AEC_CRAPPY,
                        10,
                        48000
                );
                mCore.reloadSoundDevices();

                mCore.start();

                startIterate();
                int vol = MyUtils.getInstance().getShared().getInt("stream_volume", 5);
                AndroidAudioManager.getInstance().setCallStreamVolume(vol);

                new TimeManager().start();
            });

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }




    public void terminateCurrentCallOrConferenceOrAll() {
        try {
            runOnLinphoneThread(() -> {

                if (mCore != null) {
                    Call call = mCore.getCurrentCall();
                    if (call != null) {
                        call.terminate();
                    } else {
                        mCore.terminateAllCalls();
                    }
                }
            });

        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }




    public void acceptCall(Call call) {
        runOnLinphoneThread(() -> {
            if (mCore == null) return;

            Call target = call; // local kopya

            if (target == null) {
                target = mCore.getCurrentCall();
                if (target == null) {
                    Call[] calls = mCore.getCalls();
                    if (calls.length > 0) {
                        target = calls[0];
                    }
                }
            }

            if (target != null) {
                CallParams params = mCore.createCallParams(target);
                if (params != null) {
                    params.setVideoEnabled(true);
                    target.acceptWithParams(params);
                } else {
                    target.accept();
                }
            }
        });
    }


    public void inviteDoor(Address lAddress) {
        if (mCore != null) {
            CallParams params = mCore.createCallParams(null);
            if (params != null) {
                params.setVideoDirection(MediaDirection.RecvOnly);
                params.setVideoEnabled(true);
                params.setRtpBundleEnabled(true);
                params.addCustomSdpAttribute("TYPE", "ROOM_TO_DOOR");
                params.addCustomSdpAttribute("REMOTE_LOCAL_IP", MyUtils.getInstance().getIpAddress());
                params.addCustomSdpAttribute("CURRENT_CALL", "");
                mCore.inviteAddressWithParams(lAddress, params);
            }
        }

    }

    public void inviteRoom(Address lAddress, String callParams) {

        if (mCore != null) {
            CallParams params = mCore.createCallParams(null);
            if (params != null) {
                params.setVideoEnabled(false);
                params.setRtpBundleEnabled(true);
                params.setLowBandwidthEnabled(true);
                params.addCustomSdpAttribute("TYPE", "ROOM_TO_ROOM");
                params.addCustomSdpAttribute("REMOTE_LOCAL_IP", MyUtils.getInstance().getIpAddress());
                params.addCustomSdpAttribute("CURRENT_CALL", callParams);
                mCore.inviteAddressWithParams(lAddress, params);
            }
        }
    }




    public void connectDoor(String ip) {

        runOnLinphoneThread(() -> {
            long timex = System.currentTimeMillis();
            String callerName = MyUtils.getInstance().getShared().getString("DEVICE_NAME", App.getInstance().getResources().getString(R.string.item_room));
            String callerMac = MyUtils.getInstance().getMACAddress();
            String called = null;
            DeviceModel deviceModel = DeviceController.getInstance().getDoorWithIp(ip);
            if (deviceModel != null) {
                called = deviceModel.getMac();
            }
            String callId = generateCallID();
            CallModel callModel = new CallModel();
            callModel.setCallState(AppEnums.Outgoing.name());
            callModel.setCallId(callId);
            callModel.setCallTo(called);
            callModel.setCallDate(String.valueOf(timex));
            callModel.setCallerName(callerName);
            callModel.setCallFrom(callerMac);
            callModel.setCallData("New call");
            callModel.setCallType(AppEnums.DOOR.name());
            OutGoingDoorConnected.callId = callId;
            new AddCallToDatabase(callModel).start();

            if (mCore != null) {
                Address lAddress = mCore.interpretUrl("sip:dip01@" + ip);
                if (lAddress != null) {
                    Logger.d("lAddress.getDomain   = " + lAddress.getDomain());
                    Logger.d("lAddress.getUsername = " + lAddress.getUsername());
                    Logger.d("lAddress.getScheme   = " + lAddress.getScheme());
                    inviteDoor(lAddress);
                }
            }
        });

    }

    public void connectRoom(String ip) {

        runOnLinphoneThread(() -> {
            long timex = System.currentTimeMillis();
            String callerName = MyUtils.getInstance().getShared().getString("DEVICE_NAME", App.getInstance().getResources().getString(R.string.item_room));
            String callerMac = MyUtils.getInstance().getMACAddress();
            String called = null;
            DeviceModel deviceModel = DeviceController.getInstance().getRoomWithIp(ip);
            if (deviceModel != null) {
                called = deviceModel.getMac();
            }
            String callId = generateCallID();
            CallModel callModel = new CallModel();
            callModel.setCallState(AppEnums.Outgoing.name());
            callModel.setCallId(callId);
            callModel.setCallTo(called);
            callModel.setCallDate(String.valueOf(timex));
            callModel.setCallerName(callerName);
            callModel.setCallFrom(callerMac);
            callModel.setCallData("New call");
            callModel.setCallType(AppEnums.ROOM.name());
            new AddCallToDatabase(callModel).start();
            OutGoingRoomCalling.callId = callId;


            InviteData inviteData = new InviteData();
            inviteData.setCallerMac(callerMac);
            inviteData.setCallId(callId);

            if (mCore != null) {
                Address lAddress = mCore.interpretUrl("sip:vip70@" + ip);
                if (lAddress != null) {
                    inviteRoom(lAddress, new Gson().toJson(inviteData));
                }
            }
        });



    }


    public String generateCallID() {
        String call_ID = "24226d25330aae1669c3b205d565eb";
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            byte[] hashInBytes = salt.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 15; i++) {
                String hex = Integer.toHexString(0xff & hashInBytes[i]);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            call_ID = sb.toString();
            Logger.d(call_ID.length() + " call_ID = " + call_ID);
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
        return call_ID;
    }

    @Override
    public void onMessageReceived(@NonNull Core core, @NonNull ChatRoom chatRoom, @NonNull ChatMessage message) {
        super.onMessageReceived(core, chatRoom, message);
        String mes = message.getUtf8Text();
        Logger.d("sipMessage onMessageReceived = " + mes);
    }

    @Override
    public void onCallStatsUpdated(@NonNull Core core, @NonNull Call call, @NonNull CallStats callStats) {
        super.onCallStatsUpdated(core, call, callStats);
    }

    @Override
    public void onGlobalStateChanged(@NonNull Core core, GlobalState state, @NonNull String message) {
        super.onGlobalStateChanged(core, state, message);

        Logger.d("restartLinphone onGlobalStateChanged  state = " + state + " message = " + message);

    }

    @Override
    public void onCallStateChanged(@NonNull Core core, @NonNull Call call, Call.State state, @NonNull String message) {
        super.onCallStateChanged(core, call, state, message);

        Logger.d("LinphoneManager.onCallStateChanged state = " + state + " message = " + message);
        try {
            if ((state == Call.State.IncomingReceived && !call.equals(mCore.getCurrentCall())) || (state == Call.State.IncomingEarlyMedia)) {
                MyUtils.getInstance().incomingCall = true;
                String type = Objects.requireNonNull(call.getRemoteParams()).getCustomSdpAttribute("TYPE");
                final String currentCall = call.getRemoteParams().getCustomSdpAttribute("CURRENT_CALL");
                final String remoteIp = call.getRemoteParams().getCustomSdpAttribute("REMOTE_LOCAL_IP");
                final InviteData inviteData = new Gson().fromJson(currentCall, InviteData.class);
                if (type != null) {
                    if (type.equals(AppEnums.ROOM_TO_ROOM.name())){
                        call.decline(Reason.Busy);
                        new CallReceiveRoomInCall(inviteData).start();
                    } else if (type.equals(AppEnums.DOOR_TO_MASTER.name())){
                        call.decline(Reason.Busy);
                        new CallReceiveDoor(inviteData, remoteIp).start();
                    } else {
                        call.decline(Reason.Declined);
                    }
                } else {
                    call.decline(Reason.Declined);
                }

            } else if (state == Call.State.IncomingReceived) {
                setMicrophoneDisable();
                String type = "";
                String currentCall = "";
                String remoteIp = "";
                String doorRelayCount = "1";
                String relay1Name = "", relay2Name = "";
                try {
                    type = call.getRemoteParams() != null ?
                            (call.getRemoteParams().getCustomSdpAttribute(AppEnums.TYPE.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.TYPE.name())) : "";
                    currentCall = call.getRemoteParams().getCustomSdpAttribute(AppEnums.CURRENT_CALL.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.CURRENT_CALL.name());
                    remoteIp = call.getRemoteParams().getCustomSdpAttribute(AppEnums.REMOTE_LOCAL_IP.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.REMOTE_LOCAL_IP.name());
                    doorRelayCount = call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_COUNT.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_COUNT.name());
                    String doorRelayNames = call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_NAMES.name()) == null ? "" : call.getRemoteParams().getCustomSdpAttribute(AppEnums.DOOR_RELAY_NAMES.name());
                    try {
                        if (doorRelayNames != null) {
                            String[] names = doorRelayNames.split("&&");
                            if (names.length > 1) {
                                relay1Name = names[0];
                                relay2Name = names[1];
                            }
                        }
                    } catch (Exception e) {
                        Log.e("EXCEPTION", Log.getStackTraceString(e));
                    }
                } catch (Exception e) {
                    Log.e("EXCEPTION", Log.getStackTraceString(e));
                }
                final InviteData inviteData = new Gson().fromJson(currentCall, InviteData.class);

                if (type != null && inviteData!=null) {
                    if (type.equals(AppEnums.DOOR_TO_MASTER.name())) {
                        MyUtils.getInstance().wakeUp();
                        new CallReceiveDoor(inviteData, remoteIp).start();
                        if (MyUtils.getInstance().callListener == null) {
                            MainActivity.getInstance().startIncomingDoorCall(inviteData.getCallerMac(),remoteIp,inviteData.getCallId(),doorRelayCount,relay1Name,relay2Name);
                        }
                    }else if (type.equals(AppEnums.ROOM_TO_ROOM.name())){
                        MyUtils.getInstance().wakeUp();
                        new CallReceiveRoom(inviteData).start();
                        if (MyUtils.getInstance().callListener != null) {
                            MyUtils.getInstance().incomingCall = true;
                            call.decline(Reason.Busy);
                        } else {
                            MainActivity.getInstance().startIncomingRoomCall(remoteIp, inviteData.getCallId());
                        }
                    }else if (type.equals(AppEnums.DOOR_TO_PARALEL.name())){
                        MyUtils.getInstance().wakeUp();
                        if (MyUtils.getInstance().callListener != null) {
                            MyUtils.getInstance().callListener.startParallelConnected(remoteIp,doorRelayCount,relay1Name,relay2Name);
                        }
                    }else{
                        Logger.d("DECLINEDDDD!!!!!!!!!!!!!");
                        call.decline(Reason.Declined);
                    }

                } else {
                    call.decline(Reason.Declined);
                }


            } else if (state.equals(Call.State.UpdatedByRemote)) {
                call.acceptUpdate(core.createCallParams(call));
            }
        } catch (Exception e) {
            Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onAccountRegistrationStateChanged(@NonNull Core core, @NonNull Account account, RegistrationState state, @NonNull String message) {
        super.onAccountRegistrationStateChanged(core, account, state, message);
        Logger.d("onAccountRegistrationStateChanged state = " + state);

    }

    @Override
    public void onDtmfReceived(@NonNull Core core, @NonNull Call call, int dtmf) {
        super.onDtmfReceived(core, call, dtmf);
        Logger.d("onDtmfReceived dtmf = " + dtmf);
    }


    public synchronized void setMicrophoneEnable() {
        runOnLinphoneThread(() -> {
            Logger.d("DEBUG_MIC setMicrophoneEnabled");
            if (mCore != null) {
                mCore.setMicEnabled(true);
                if (mCore.getCallsNb() == 0) return;
                Call currentCall = mCore.getCurrentCall();
                if (currentCall == null) currentCall = mCore.getCalls()[0];
                if (currentCall != null) {
                    currentCall.setMicrophoneMuted(false);
                }
            }
        });

    }


    public synchronized void setMicrophoneDisable() {
        runOnLinphoneThread(() -> {
            Logger.d("DEBUG_MIC setMicrophoneDisabled");
            if (mCore != null) {
                mCore.setMicEnabled(false);
                if (mCore.getCallsNb() == 0) return;
                Call currentCall = mCore.getCurrentCall();
                if (currentCall == null) currentCall = mCore.getCalls()[0];
                if (currentCall != null) {
                    currentCall.setMicrophoneMuted(true);
                }
            }
        });
    }

    public  void sendLocalSipMessage(String ip, String mes) {
        runOnLinphoneThread(new Runnable() {
            @Override
            public void run() {
                if (mCore != null) {
                    ChatRoomParams params = mCore.createDefaultChatRoomParams();
                    params.setEncryptionEnabled(false);
                    params.setGroupEnabled(false);
                    params.setBackend(ChatRoom.Backend.Basic);

                    String from = "sip:" + MyUtils.getInstance().getIpAddress();
                    String to = "sip:" + ip;
                    Address[] participants = new Address[1];
                    Address remoteAddress = Factory.instance().createAddress(to);
                    Address localAddress = Factory.instance().createAddress(from);
                    participants[0] = remoteAddress;

                    if (remoteAddress != null) {
                        ChatRoom room = mCore.createChatRoom(params, localAddress, participants);
                        if (room != null) {
                            ChatMessage chatMessage = room.createMessageFromUtf8(mes);
                            chatMessage.send();
                        }
                    }
                }
            }
        });

    }


    private static class CallReceiveRoom extends Thread {
        private final InviteData inviteData;

        public CallReceiveRoom(InviteData receiveData) {
            this.inviteData = receiveData;
        }

        @Override
        public void run() {
            super.run();
            try {
                String callerName;
                DeviceModel device = DeviceController.getInstance().getRoomWithMac(inviteData.getCallerMac());
                if (device != null) {
                    callerName = device.getName();
                } else {
                    callerName = App.getInstance().getResources().getString(R.string.item_room);
                }
                CallModel callModel = new CallModel();
                callModel.setCallState(AppEnums.Incoming.name());
                callModel.setCallId(inviteData.getCallId());
                callModel.setCallTo(MyUtils.getInstance().getMACAddress());
                callModel.setCallDate(String.valueOf(System.currentTimeMillis()));
                callModel.setCallerName(callerName);
                callModel.setCallFrom(inviteData.getCallerMac());
                callModel.setCallData("New call");
                callModel.setCallType(AppEnums.ROOM.name());
                DatabaseHelper.getInstance().insertCall(callModel);
                if (MyUtils.getInstance().callListener != null) {
                    MyUtils.getInstance().callListener.incomingRoomCall(callModel);
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }

    private static class CallReceiveDoor extends Thread {
        private final InviteData inviteData;
        private final String remoteIp;

        public CallReceiveDoor(InviteData receiveData, String receiveIp) {
            this.inviteData = receiveData;
            this.remoteIp = receiveIp;
        }

        @Override
        public void run() {
            super.run();

            try {

                DeviceModel device = DeviceController.getInstance().getDoorWithMac(inviteData.getCallerMac());
                String callerName;
                if (device != null) {
                    callerName = device.getName();
                } else {
                    callerName = App.getInstance().getResources().getString(R.string.item_door);
                }
                String pathx = MyUtils.getInstance().getDoorImagesDir() + "/" + inviteData.getCallId() + ".jpeg";
                CallModel callModel = new CallModel();
                callModel.setCallState(AppEnums.Calling.name());
                callModel.setCallId(inviteData.getCallId());
                callModel.setCallTo(MyUtils.getInstance().getMACAddress());
                callModel.setCallDate(String.valueOf(System.currentTimeMillis()));
                callModel.setCallerName(callerName);
                callModel.setCallFrom(inviteData.getCallerMac());
                callModel.setCallData("New call");
                callModel.setCallImgPath(pathx);
                callModel.setCallType(AppEnums.DOOR.name());
                DatabaseHelper.getInstance().insertCall(callModel);
                new TCPFileRequest(remoteIp, callModel.getCallImgPath()).start();
                if (MyUtils.getInstance().callListener != null) {
                    MyUtils.getInstance().callListener.incomingDoorCall(callModel, remoteIp);
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }

    private static class CallReceiveRoomInCall extends Thread {

        private final InviteData inviteData;

        public CallReceiveRoomInCall(InviteData receiveData) {
            this.inviteData = receiveData;
        }

        @Override
        public void run() {
            super.run();

            try {

                String callerName;
                DeviceModel device = DeviceController.getInstance().getRoomWithMac(inviteData.getCallerMac());
                if (device != null) {
                    callerName = device.getName();
                } else {
                    callerName = App.getInstance().getResources().getString(R.string.item_room);
                }
                CallModel callModel = new CallModel();
                callModel.setCallState(AppEnums.Incoming.name());
                callModel.setCallId(inviteData.getCallId());
                callModel.setCallTo(MyUtils.getInstance().getMACAddress());
                callModel.setCallDate(String.valueOf(System.currentTimeMillis()));
                callModel.setCallerName(callerName);
                callModel.setCallFrom(inviteData.getCallerMac());
                callModel.setCallData(AppEnums.Busy.name());
                callModel.setCallType(AppEnums.ROOM.name());
                DatabaseHelper.getInstance().insertCall(callModel);
                if (MyUtils.getInstance().historyListener != null) {
                    MyUtils.getInstance().historyListener.refreshCalls();
                }
                if (MyUtils.getInstance().callListener != null) {
                    MyUtils.getInstance().callListener.incomingRoomCall(callModel);
                }
            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }



    private static class InviteData {
        private String callId;
        private String callerMac;

        public String getCallId() {
            return callId;
        }

        public void setCallId(String callId) {
            this.callId = callId;
        }

        public String getCallerMac() {
            return callerMac;
        }

        public void setCallerMac(String callerMac) {
            this.callerMac = callerMac;
        }
    }

    public static class AddCallToDatabase extends Thread {
        private final CallModel callModel;

        public AddCallToDatabase(CallModel model) {
            this.callModel = model;
        }

        @Override
        public void run() {
            super.run();
            try {

                DatabaseHelper.getInstance().insertCall(callModel);

            } catch (Exception e) {
                Logger.d("EXCEPTION = " + Log.getStackTraceString(e));
            }
        }
    }


    public void enable264() {
        PayloadType[] vpt = mCore.getVideoPayloadTypes().clone();
        for (PayloadType pt : vpt) {
            Logger.d("VideoCodecX isUsable= " + pt.isUsable() + " enabled= " + pt.enabled() + " desc= " + pt.getDescription());
            if(pt.getMimeType().equalsIgnoreCase("H264")){
                pt.enable(true);
            }else{
                pt.enable(false);
            }
        }
        mCore.setVideoPayloadTypes(vpt);
    }

    public void disable264() {
        PayloadType[] vpt = mCore.getVideoPayloadTypes().clone();
        for (PayloadType pt : vpt) {
            Logger.d("VideoCodecX isUsable= " + pt.isUsable() + " enabled= " + pt.enabled() + " desc= " + pt.getDescription());
            if(pt.getMimeType().equalsIgnoreCase("H264")){
                pt.enable(false);
            }else{
                pt.enable(true);
            }
        }
        mCore.setVideoPayloadTypes(vpt);
    }
}
