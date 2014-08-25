package com.charles.mileagetracker.app.adapter.containers;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by charles on 7/9/14.
 * Container class that I'll load the values from the database into.
 */
public class ExpandListChild implements Serializable {

    private String date = null;
    private long millis = -1;
    private int id = -1;
    private double distance = -1;
    private int tripKey = -1;
    private double lat = -1;
    private double lon = -1;
    private int businessRelated = -1;
    private String address = null;
    private ExpandListGroup listGroup;
    private LinkedList<LatLng> linePoints;

    public ExpandListChild(String date, long millis, int id, double distance, int tripKey, double lat,
                           double lon, int businessRelated, String address) {
        this.date = date;
        this.millis = millis;
        this.id = id;
        this.distance = distance;
        this.tripKey = tripKey;
        this.lat = lat;
        this.lon = lon;
        this.businessRelated = businessRelated;
        this.address = address;
        linePoints = new LinkedList<LatLng>();
    }


    public String getDate() {
        return date;
    }

    public int getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }

    public int getTripKey() {
        return tripKey;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int isBusinessRelated() {
        return businessRelated;
    }

    public void setBusinessRelated(int businessRelated) {

        this.businessRelated = businessRelated;
    }


    public String getAddress() {
        return address;
    }

    public void setExpandListGroup(ExpandListGroup listGroup) {
        this.listGroup = listGroup;
    }


    public ExpandListGroup getExpandGroup() {
        return listGroup;
    }

    public void addPoint(LatLng latLng) {
        linePoints.add(latLng);
    }

    public void addAllPoints(LinkedList<LatLng> points) {
        linePoints.addAll(points);
    }

    public LinkedList<LatLng> getLinePoints() {
        return linePoints;
    }

}
