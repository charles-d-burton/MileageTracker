package com.charles.mileagetracker.app.services.intentservices;

import android.app.Instrumentation;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */

public class ActivityRecognitionIntentService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{


    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    private static int secondsElapsed = 0;
    private static long lastDrivingUpdate = 0l;
    private static int notInVehicleCounter = 0;

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static com.google.android.gms.location.LocationListener locationListener = null;
    private static boolean locationUpdateInProgress = false;

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

    private enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN }
    private ACTIVITY_TYPE mActivityType;

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {

                if (lastDrivingUpdate == 0) {
                    lastDrivingUpdate = System.currentTimeMillis();//Means it was just initialized
                }

                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                DetectedActivity mostProbableActivity = result.getMostProbableActivity();

                int confidence = mostProbableActivity.getConfidence();

                activityUpdate(mostProbableActivity.getType(), confidence);
            }
        }
    }

    /**
     * Map detected activity types to strings
     *@param activityType The detected activity type
     *@return A user-readable name for the type
     */
    private ACTIVITY_TYPE getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return ACTIVITY_TYPE.DRIVING;

            case DetectedActivity.ON_BICYCLE:
                return ACTIVITY_TYPE.BIKING;

            case DetectedActivity.ON_FOOT:
                return ACTIVITY_TYPE.WALKING;

            case DetectedActivity.STILL:
                return ACTIVITY_TYPE.STILL;

            case DetectedActivity.UNKNOWN:
                return ACTIVITY_TYPE.UNKNOWN;

            case DetectedActivity.TILTING:
                return ACTIVITY_TYPE.TILTING;
            default:
                return ACTIVITY_TYPE.UNKNOWN;
        }
    }

    private void activityUpdate(int activityType, int confidence) {
        Log.v("DEBUG: ", "Confidence level: " + Integer.toString(confidence));
        if (confidence < 62) { //Less than really sure
            return;
        }
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                Log.v("DEBUG: " , "Driving");
                notInVehicleCounter = 0;
                driving();
                break;
            default:
                Log.v("DEBUG: ", "Not Driving");
                notInVehicleCounter = notInVehicleCounter +1;
                notDriving();
                break;
        }
    }

    private void driving() {

    }

    private void notDriving() {
       if (notInVehicleCounter >= 2 && notInVehicleCounter < 10) {//Want to check for a quick update, if it's been more than 10 though not interested
           if (!locationUpdateInProgress) {
               locationUpdateInProgress = true;
               Log.v("DEBUG: ", "2 Requests not driving, getting current location");
               locationClient = new LocationClient(getApplicationContext(), this, this);
               locationRequest = LocationRequest.create();
               locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
               locationRequest.setInterval(5000);
               locationRequest.setFastestInterval(1000);
               locationListener = new MyLocationListener();
               locationClient.connect();
           }
       }
    }

    private void checkLocation(LatLng location) {
        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1);
            for (Address address : addresses) {

            }
        } catch (IOException ioe) {

        }
        locationUpdateInProgress = false;//Done checking current location
    }


    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            locationClient.disconnect();
            checkLocation(currentLocation);

        }
    }

}
