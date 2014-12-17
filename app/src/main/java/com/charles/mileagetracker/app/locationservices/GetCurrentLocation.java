package com.charles.mileagetracker.app.locationservices;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;


/**
 * Created by charles on 12/15/14.
 * I created this class to be more convenient for requesting location.  It automatically handles checking
 * which providers are enabled in the device, stepping down resolution if it can't get a good fix, and
 * returning the last known value.
 */
public class GetCurrentLocation implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public enum PRECISION {HIGH, LOW};
    private PRECISION precision = PRECISION.LOW;

    private Context context = null;

    private double resolution   = 1000;
    private double retries = 5;

    private boolean continuous = false;

    private GetLocationCallback callback = null;

    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListener = null;

    public GetCurrentLocation(Context context, int retries, PRECISION precision) {
        this.retries = retries;
        this.precision = precision;
        this.context = context;
    }

    public GetCurrentLocation(Context context) {
        this.continuous = true;
        this.context = context;
    }

    /*
    This method takes a callback and then initializes the class to receive location updates.  It creates
    and starts the listener for location updates.  It also figures out which providers are enabled and resets
    the precision if the requested provider is not enabled on the device.
     */
    public void updateLocation(GetLocationCallback callback, boolean updateImmediately) {
        this.callback = callback;

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
            callback.retrievedLocation(resolution, new LatLng(location.getLatitude(), location.getLongitude()));
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
        locationClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, locationListener);
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


    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (callback == null) {
                locationClient.disconnect();
            }
        }
    }

    public interface GetLocationCallback {
        public void retrievedLocation(double resolution, LatLng location);
        public void locationConnectionFailed();
    }
}
