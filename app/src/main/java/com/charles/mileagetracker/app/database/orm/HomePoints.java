package com.charles.mileagetracker.app.database.orm;

import com.google.android.gms.maps.model.Marker;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * Created by charles on 10/27/14.
 */
public class HomePoints extends SugarRecord<HomePoints> {

    public String name = null;
    public String address = null;
    public double lat = -1;
    public double lon = -1;

    @Ignore
    private Marker marker = null;

    public HomePoints(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    public HomePoints() {

    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public Marker getMarker() {
        return marker;
    }
}
