package com.charles.mileagetracker.app.database.orm;

import com.orm.SugarRecord;

/**
 * Created by charles on 11/3/14.
 */
public class PolyLine extends SugarRecord<PolyLine> {
    double lat;
    double lon;

    TripGroup trip_group;

    public PolyLine() {

    }

    public PolyLine(double lat, double lon, TripGroup trip_group) {
        this.lat = lat;
        this.lon = lon;
        this.trip_group = trip_group;
    }

}
