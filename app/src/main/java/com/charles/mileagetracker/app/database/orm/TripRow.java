package com.charles.mileagetracker.app.database.orm;

import com.google.android.gms.maps.model.Marker;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.Date;
import java.util.List;

/**
 * Created by charles on 10/27/14.
 */
public class TripRow extends SugarRecord<TripRow> {

    public Date timeStart;
    public Date timeEnd;
    public double lat;
    public double lon;
    public String address;
    public double distance = 0;
    public String units = "km";
    public boolean businessRelated = false;
    public String points;

    @Ignore
    public Marker marker = null;
    @Ignore
    public List polyPoints = null;

    //Relationship to a trip_group.  Meaning a collection of trips that are logically grouped together.
    //I may add additional columns for groups later.
    public TripGroup tgroup;

    public TripRow(Date timeStart, Date timeEnd, double lat, double lon, String address, double distance, TripGroup trip_group) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.lat = lat;
        this.lon = lon;
        this.address = address;
        this.distance = distance;
        this.tgroup = trip_group;
    }

    public TripRow() {

    }
}
