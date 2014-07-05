package com.charles.mileagetracker.app.services.intentservices;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.IOException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */


//TRY TO MAKE THIS A BROADCAST RECEIVER IN THE MORNING
public class ActivityRecognitionIntentService extends IntentService {


    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    public static final String ACTIVITY_BROADCAST = "com.charles.mileagetracker.app.ACTIVITY_BROADCAST";


    public enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN}

    private ACTIVITY_TYPE mActivityType;

    private GetCurrentLocation mService = null;
    private boolean mBound = false;

    private TripVars tripVars = null;
    private AccessInternalStorage accessCache = null;

    @Override
    public void onCreate() {

        super.onCreate();
    }

    /*
    Initialize some variables by reading in the TripVars stored in
     the cache system.  Get the calling Intent to retrieve the most likely activity
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("DEBUG: ", "Handling Activity Recognition");
        accessCache = new AccessInternalStorage();
        try {
            tripVars = (TripVars)accessCache.readObject(getApplicationContext(), TripVars.KEY);
            Log.d("DEBUG: ", "Driving Counter: " + Integer.toString(tripVars.getNotDrivingCounter()));
            Log.d("DEBUG: ", "ID: " + Integer.toString(tripVars.getId()));
            Log.d("DEBUG: ", "SegmentRecorded: " + Boolean.toString(tripVars.isSegmentRecorded()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if (intent != null) {

            if (ActivityRecognitionResult.hasResult(intent)) {

                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                DetectedActivity mostProbableActivity = result.getMostProbableActivity();

                int confidence = mostProbableActivity.getConfidence();

                activityUpdate(mostProbableActivity.getType(), confidence);

            } else {
                Log.v("DEBUG: ", "Where is the Result Intent WTH?");
            }
        }
    }

    /**
     * Map detected activity types to strings
     *
     * @param activityType The detected activity type
     * @return A user-readable name for the type
     * then using the confidence attempt to be sure that the current activity we think is happening
     * is actually happening.
     */

    private void activityUpdate(int activityType, int confidence) {
        //Log.v("DEBUG: ", "Confidence level: " + Integer.toString(confidence));
        /*if (confidence < 62) { //Less than really sure
            return;
        }*/
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                //Log.v("DEBUG: " , "Driving");
                handleDriving();
                break;
            case DetectedActivity.ON_FOOT:
                handleWalking(confidence);
                //notDriving(confidence, "walking");
                break;
            case DetectedActivity.UNKNOWN:
                //Log.v("DEBUG: ", "Unknown");
                break;
            case DetectedActivity.ON_BICYCLE:
                //Log.v("DEBUG:", "Bike");
                break;
            case DetectedActivity.STILL:
                handleStill(confidence);
                break;
            case DetectedActivity.TILTING:
                //Log.v("DEBUG: ", "Tilting at windmills");
                break;
            default:
                Log.v("DEBUG: ", "Unknown");

                break;
        }
    }

    //If driving then we're going to set the variables to their least known values and write them

    private void handleDriving() {
        tripVars.setDriving(true);
        tripVars.setNotDrivingCounter(0);
        tripVars.setSegmentRecorded(false);

        try {
            accessCache.writeObject(getApplicationContext(), tripVars.KEY, tripVars);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*If not driving then we're going to examine the variables
    If the counter is less than two that means that we have been not driving for less than 2 minutes,
    I'm trying to avoid false positives so I'm going to ignore 2 minutes of not driving.  If that's
    not the case then we're going to start the process to get the current location and then record it
    in the database.  There is a boolean that will be set so that at any point in that process we start
    driving again then I'm going to break off recording a path segment.
     */

    private void handleWalking(int confidence) {
        tripVars.setDriving(false);

        int counter = tripVars.getNotDrivingCounter();
        counter = counter + 1;
        tripVars.setNotDrivingCounter(counter);
        if (counter > 1 && !tripVars.isSegmentRecorded()) {//Trip segment not recorded, the class GetCurrentLocation will set this flag to true
            startLocationHandler();
        }

        try {
            accessCache.writeObject(getApplicationContext(), tripVars.KEY, tripVars);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleStill(int confidence) {
        tripVars.setDriving(false);
        int counter = tripVars.getNotDrivingCounter();
        counter = counter + 1;
        tripVars.setNotDrivingCounter(counter);
        if (counter >= 4 && !tripVars.isSegmentRecorded()) {
            startLocationHandler();
        }

        try {
            accessCache.writeObject(getApplicationContext(), tripVars.KEY, tripVars);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Kills the GetCurrentLocation class.  Uses start service but sets a boolean to tell it to unregister and stop cleanly
    private void killGetLocation() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), GetCurrentLocation.class);
            intent.putExtra("stop", true);
            startService(intent);
        }
    }

    private void startLocationHandler() {

        Intent startLocationIntent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        startService(startLocationIntent);
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (GetCurrentLocation.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
