package com.charles.mileagetracker.app.database.orm;

import android.content.Context;

import com.orm.SugarRecord;

/**
 * Created by charles on 10/27/14.
 */
public class Status extends SugarRecord<Status> {

    boolean driving;
    double lastLat;
    double lastLon;
    double lat;
    double lon;
    int notDrivingCount;
    int trip_group;


    public Status() {
    }

    public Status(Context context, boolean driving, double lastLat, double lastLon, double lat,
                  double lon, int notDrivingCount, int trip_group) {
        this.driving = driving;
        this.lastLat = lastLat;
        this.lastLon = lastLon;
        this.lat = lat;
        this.lon = lon;
        this.notDrivingCount = notDrivingCount;
        this.trip_group = trip_group;
    }
}
