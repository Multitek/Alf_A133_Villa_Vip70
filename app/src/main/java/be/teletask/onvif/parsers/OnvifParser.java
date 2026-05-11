package be.teletask.onvif.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import be.teletask.onvif.responses.OnvifResponse;

/**
 * Created by Tomas Verhelst on 03/09/2018.
 * Copyright (c) 2018 TELETASK BVBA. All rights reserved.
 */
public abstract class OnvifParser<T> {

    //Constants
    public static final String TAG = OnvifParser.class.getSimpleName();
    int eventType;
    //Attributes
    private XmlPullParserFactory xmlFactory;
    private XmlPullParser xpp;

    //Constructors

    OnvifParser() {
        try {
            xmlFactory = XmlPullParserFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            xpp = xmlFactory.newPullParser();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    //Properties
    protected XmlPullParserFactory getXmlFactory() {
        return xmlFactory;
    }

    protected void setXmlFactory(XmlPullParserFactory xmlFactory) {
        this.xmlFactory = xmlFactory;
    }

    XmlPullParser getXpp() {
        return xpp;
    }

    public abstract T parse(OnvifResponse response);
}
