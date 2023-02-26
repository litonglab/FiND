package com.example.wifi;

import static java.lang.Integer.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class myWiFiInfo {
    private String phoneId, longitude, latitude, cityId,
                   buildingId, floor, beaconId, spaceId,
                   distance, timeStamp;
    private List<ScanResult> wifiList;
    private HashMap<String, Integer> hash = new HashMap<>();

    public myWiFiInfo(double longitude, double latitude, String name, List<ScanResult> result,
                      List<String> totalWiFi, String beacon, double dist){
        phoneId = name;
        wifiList = result;

        for(ScanResult item : result) {
            hash.put(item.BSSID, item.level);
        }

        this.longitude = Double.toString(longitude);
        this.latitude = Double.toString(latitude);

        cityId = buildingId = floor = spaceId = "unknown";
        beaconId = beacon;

        if(dist == -1) distance = "-1";
        else if(dist < 1) distance = "0";
        else if(dist < 3) distance = "1";
        else distance = "2";
        Log.i("Distance", Double.toString(dist));

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        timeStamp = df.format(new Date());
    }

    public String getPhoneId() {
        return phoneId;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getCityId() {
        return cityId;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public String getFloor() {
        return floor;
    }

    public String getBeaconId() {
        return beaconId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getDistance() {
        return distance;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public HashMap<String, Integer> getHash() {
        return hash;
    }

}
