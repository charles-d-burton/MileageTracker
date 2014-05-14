package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class CreatePathSegment extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;

    private static volatile boolean driving = true;

    private final IBinder mBinder = new LocalBinder();

    public CreatePathSegment() {
        super("CheckCurrentLocation");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, locationListener);
    }

    @Override
    public void onDisconnected() {
        locationClient.removeLocationUpdates(locationListener);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected void getLocationUpdate() {
        driving = false;
        locationClient = new LocationClient(getApplicationContext(), this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationListener = new MyLocationListener();
        locationClient.connect();

    }

    protected void startedDriving() {
        driving = true;
    }

    private void logSegment(LatLng latLng) {

    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location.getAccuracy() < 5 && !driving) { //Less than 5 meter accuracy
                locationClient.disconnect();
                logSegment(new LatLng(location.getLatitude(), location.getLongitude()));
            } else {
                Log.v("DEBUG: ", "Not enough precision");
            }

        }
    }

    public class LocalBinder extends Binder {
        CreatePathSegment getService() {
            // Return this instance of CreatePathSegment so clients can call public methods
            return CreatePathSegment.this;
        }
    }
}
