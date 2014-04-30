package com.charles.mileagetracker.app.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.HashMap;

public class RecordTrackService extends Service {

    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListner = null;
    private HashMap pathSegments = null;

    public RecordTrackService() {

    }

    @Override
    public IBinder onBind(Intent intent) {

        //Log.v("DEBUG: ", "Recording Track Intent Bound");

        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("DEBUG: ", "Recording Track Command Started");

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);

        return 0;
    }

    /*
Helper class that listens for location change events.
 */
    private final class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            //SetHome.this.location = location;
            //zoomToLocation(location);
        }
    }
}
