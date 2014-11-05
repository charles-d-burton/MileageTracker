package com.charles.mileagetracker.app.database.orm;

import com.orm.SugarRecord;

import java.util.Date;

/**
 * Created by charles on 10/27/14.
 */
public class TripsData extends SugarRecord<TripsData> {

    Date timeStart;
    Date timeEnd;
    double lat;
    double lon;
    String address;
    double distance;
    boolean businessRelated = false;

    //Relationship to a trip_group.  Meaning a collection of trips that are logically grouped together.
    //I may add additional columns for groups later.
    TripGroup trip_group;

    public TripsData( Date timeStart, double lat, double lon, String address, double distance, TripGroup trip_group) {
        this.timeStart = timeStart;
        this.lat = lat;
        this.lon = lon;
        this.address = address;
        this.distance = distance;
        this.trip_group = trip_group;
    }

    public TripsData() {

    }


}
