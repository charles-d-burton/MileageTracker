package com.charles.mileagetracker.app.processingservices;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.HashMap;
import java.util.List;


/**
 * Created by charles on 12/15/14.
 * I created this class to be more convenient for requesting location.  It automatically handles checking
 * which providers are enabled in the device, stepping down resolution if it can't get a good fix, and
 * returning the last known value.
 */
public class GetCurrentLocation implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private final String CLASS_NAME = ((Object)this).getClass().getName();

    public enum PRECISION {HIGH, LOW};
    private PRECISION precision = PRECISION.LOW;

    private Context context = null;

    private double resolution   = 1000;
    private int retries = 5;
    private int attempts = 0;

    private boolean continuous = false;

    private GetLocationCallback callback = null;

    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListener = null;

    private boolean disconnect = false;

    private HashMap<String, List> pendingFences;


    public GetCurrentLocation(Context context, int retries, PRECISION precision) {
        this.retries = retries;
        this.precision = precision;
        this.context = context;
        this.pendingFences = new HashMap<String, List>();
    }

    public GetCurrentLocation(Context context) {
        this.continuous = true;
        this.context = context;
        this.pendingFences = new HashMap<String, List>();
    }

    /*
    This method takes a callback and then initializes the class to receive location updates.  It creates
    and starts the listener for location updates.  It also figures out which providers are enabled and resets
    the precision if the requested provider is not enabled on the device.
     */
    public void updateLocation(GetLocationCallback callback, boolean updateImmediately) {
        this.callback = callback;
        disconnect = false;

        locationRequest = LocationRequest.create();

        LocationManager lm = (LocationManager)context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (updateImmediately) {
            callback.retrievedLocation(resolution,location);
        }

        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            precision = PRECISION.HIGH;
        } else {
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            precision = PRECISION.LOW;
        }

        switch (precision) {
            case HIGH:
                resolution = 250;
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
            case LOW:
                resolution = 750;
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
        }

        locationClient = new LocationClient(context, this, this);
        locationListener = new MyLocationListener();
        if (continuous) {
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(10000);
        }
        locationClient.connect();

    }

    //This forces the app to disconnect the location services, if it's called while still setting up everything
    //it sets a boolean to be read later to disconnect.
    public void forceDisconnect() {
        disconnect = true;
        if (locationClient != null) {
            locationClient.disconnect();
        }
    }

    //Add geofences for the app
    public void addGeoFence(List fences, PendingIntent intent, LocationClient.OnAddGeofencesResultListener callback) {
        if (locationClient.isConnected()) {
            locationClient.addGeofences(fences, intent, callback);
        }
    }

    //Remove geofences for the app
    public void removeGeoFence(List fences, LocationClient.OnRemoveGeofencesResultListener callback) {
        locationClient.removeGeofences(fences, callback);
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, locationListener);
        callback.locationClientConnected();
    }

    @Override
    public void onDisconnected() {
        if (callback != null) {
            callback.locationConnectionFailed();
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (callback != null) {
            callback.locationConnectionFailed();
        }
    }

    /*
    This is where the location updates are actually processed.  It checks to make sure that there
    is a callback to send to and make sure that it hasn't been called on to disconnect.  It compares
    the requested resolution against the actual resolution.  If the resolution is too low(number larger
    than requested) it keeps listening and trying.  It will eventually based on the maximum requested
    attempts take the best location that it has and send that to the callback.
     */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (callback == null || disconnect) {
                locationClient.disconnect();
                disconnect = false;
            } else if (location != null) {
                if (continuous) {
                    callback.retrievedLocation(resolution, location);
                } else if (location.getAccuracy() <= resolution) {
                    callback.retrievedLocation(location.getAccuracy(), location);
                    locationClient.disconnect();
                } else if (location.getAccuracy() > resolution && attempts < retries) {
                    attempts = attempts + 1;
                } else if (location.getAccuracy() > resolution && attempts >= retries) {
                    attempts = attempts +1;
                    callback.retrievedLocation(location.getAccuracy(), location);
                    locationClient.disconnect();
                }
            }
        }
    }

    public interface GetLocationCallback {
        public void retrievedLocation(double resolution, Location location);
        public void locationClientConnected();
        public void locationConnectionFailed();
    }
}
