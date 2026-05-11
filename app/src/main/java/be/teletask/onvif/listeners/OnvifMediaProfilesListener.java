package be.teletask.onvif.listeners;

import java.util.List;

import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifMediaProfile;

/**
 * Created by Tomas Verhelst on 03/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public interface OnvifMediaProfilesListener {

    void onMediaProfilesReceived(OnvifDevice device, List<OnvifMediaProfile> mediaProfiles);

}
