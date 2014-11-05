package com.charles.mileagetracker.app.database.orm;

import android.util.Log;

import com.orm.SugarRecord;

/**
 * Created by charles on 10/27/14.
 */
public class HomePoints extends SugarRecord<HomePoints> {
    public String name;
    public double lat;
    public double lon;

    public HomePoints(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        Log.v("Sugar: ", name);
        Log.v("Sugar: ", Double.toString(lat));
        Log.v("Sugar: ", Double.toString(lon));
    }

    public HomePoints() {

    }
}
