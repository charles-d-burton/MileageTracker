package com.charles.mileagetracker.app.cache;

import java.io.Serializable;

/**
 * Created by charles on 6/10/14.
 */
public class TripVars implements Serializable {

    public static final String KEY = "com.charles.app.TRIPVARS";

    public int fenceTransitionType = 0;

    private int id = 0;

    private int currentTripIndex = 0;

    private int notDrivingCounter = 0;

    private double lat = 0;
    private double lon = 0;
    private long startTime = -1;

    private double lastLat = -1;
    private double lastLon = -1;



    private long lastTime = -1;

    private boolean driving = false;
    private boolean segmentRecorded = false;
    private int notDrivingUpdates = 0;

    public int getFenceTransitionType() {
        return fenceTransitionType;
    }

    public void setFenceTransitionType(int fenceTransitionType) {
        this.fenceTransitionType = fenceTransitionType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }



    public int getCurrentTripIndex() {
        return currentTripIndex;
    }

    public void setCurrentTripIndex(int currentTripIndex) {
        this.currentTripIndex = currentTripIndex;
    }

    public int getNotDrivingCounter() {
        return notDrivingCounter;
    }

    public void setNotDrivingCounter(int notDrivingCounter) {
        this.notDrivingCounter = notDrivingCounter;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getLastLat() {
        return lastLat;
    }

    public void setLastLat(double lastLat) {
        this.lastLat = lastLat;
    }

    public double getLastLon() {
        return lastLon;
    }

    public void setLastLon(double lastLon) {
        this.lastLon = lastLon;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public boolean isDriving() {
        return driving;
    }

    public void setDriving(boolean driving) {
        this.driving = driving;
    }

    public boolean isSegmentRecorded() {
        return segmentRecorded;
    }

    public void setSegmentRecorded(boolean segmentRecorded) {
        this.segmentRecorded = segmentRecorded;
    }

    public int getNotDrivingUpdates() {
        return notDrivingUpdates;
    }

    public void setNotDrivingUpdates(int notDrivingUpdates) {
        this.notDrivingUpdates = notDrivingUpdates;
    }
}
