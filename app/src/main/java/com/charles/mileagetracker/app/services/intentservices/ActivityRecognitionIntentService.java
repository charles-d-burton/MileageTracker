package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
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

public class ActivityRecognitionIntentService extends IntentService {


    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    private static int secondsElapsed = 0;
    private static long lastDrivingUpdate = 0l;
    private static int notInVehicleCounter = 0;


    private static boolean locationUpdateInProgress = false;


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
            case DetectedActivity.ON_FOOT:
                notInVehicleCounter = notInVehicleCounter +1;
                if (confidence > 90 ) {
                    createPathSegment();
                }
                notDriving();
                break;
            case DetectedActivity.UNKNOWN:
                Log.v("DEBUG: ", "Unknown");
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.v("DEBUG:", "Bike");
                break;
            case DetectedActivity.STILL:
                notInVehicleCounter = notInVehicleCounter +1;
                if (confidence > 80 ) {
                    createPathSegment();
                }
                notDriving();
                break;
            case DetectedActivity.TILTING:
                Log.v("DEBUG: ", "Tilting at windmills");
                break;
            default:
                Log.v("DEBUG: ", "Unknown");

                break;
        }
    }

    private void driving() {

    }

    private void notDriving() {
       if (notInVehicleCounter >= 2 && notInVehicleCounter < 10) {//Want to check for a quick update, if it's been more than 10 though not interested because you're stopped
           if (!locationUpdateInProgress) {
               locationUpdateInProgress = true;
               Log.v("DEBUG: ", Integer.toString(notInVehicleCounter) + " Requests not driving, getting current location");

           }
       }
    }

    private void checkLocation(LatLng location) {
        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1);
            for (Address address : addresses) {
                //Log.v("DEBUG: ", "Thoroughfare: " + address.getThoroughfare());
                Log.v("DEBUG: ", "Address line: " + address.getAddressLine(0));
                address.getThoroughfare();
            }
        } catch (IOException ioe) {

        }
        locationUpdateInProgress = false;//Done checking current location
    }

    private void createPathSegment() {

    }

}
