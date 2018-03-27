package com.droidmentor.locationhelper;

import java.io.Serializable;

/**
 * Created by thetaubuntu5 on 28/11/17.
 */

public class MyLatLong implements Serializable {
    public double lat;
    public double lon;


    public MyLatLong() {
    }

    public MyLatLong(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }
}
