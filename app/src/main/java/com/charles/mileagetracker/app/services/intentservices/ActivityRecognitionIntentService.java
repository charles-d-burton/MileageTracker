package com.charles.mileagetracker.app.services.intentservices;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

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

    private CreatePathSegment mService = null;
    private boolean mBound = false;

    private static double startlat = -1;
    private static double startlon = 1;
    private static int id = -1;
    private static long startTime = -1;

    /*
    Initialize some variables and get the calling Intent to retrieve the most likely activity
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                if (startlat == -1 && startlon == -1) {
                    startlat = intent.getDoubleExtra("lat", -1);
                    startlon = intent.getDoubleExtra("lon", -1);
                    id = intent.getIntExtra("id", -1);
                    startTime = intent.getLongExtra("startTime" ,-1);

                }

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
     * then using the confidence attempt to be sure that the current activity we think is happening
     * is actually happening.
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
                secondsElapsed = 0;
                driving();
                break;
            case DetectedActivity.ON_FOOT:
                notDriving(confidence);
                break;
            case DetectedActivity.UNKNOWN:
                Log.v("DEBUG: ", "Unknown");
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.v("DEBUG:", "Bike");
                break;
            case DetectedActivity.STILL:
                Log.v("DEBUG: ", "Sitting Still");
                notDriving(confidence);
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

    /*
    Take the current time and calculate what the last time there was a not driving event was.  Use that
    time to get the time elapsed for not driving.  This is partly a safety check to help reduce false
    positives for things like sitting in traffic.
     */
    private void notDriving(int confidence) {
       notInVehicleCounter = notInVehicleCounter +1;

       long currentTime = System.currentTimeMillis();

       if (lastDrivingUpdate == 0) lastDrivingUpdate = currentTime + 1;
       int difference = Double.valueOf((currentTime - lastDrivingUpdate)/1000).intValue();
       secondsElapsed = secondsElapsed + difference;
       Log.v("DEBUG: ", "Seconds not driving: " + Integer.toString(secondsElapsed));

       if (notInVehicleCounter >= 2 && notInVehicleCounter < 10) {//Want to check for a quick update, if it's been more than 10 though not interested because you're stopped
           locationUpdateInProgress = true;
           createPathSegment();
           Log.v("DEBUG: ", Integer.toString(notInVehicleCounter) + " Requests not driving, getting current location");
       }
    }

    /*
    Start the intent to get the current location and update the database
     */
    private void createPathSegment() {
        if (!isCreatePathSegmentRunning()) {
            Intent intent = new Intent(getApplicationContext(), CreatePathSegment.class);
            intent.putExtra("id", id);
            intent.putExtra("lat", startlat);
            intent.putExtra("lon", startlon);
            intent.putExtra("startTime", startTime);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//            mService.getLocationUpdate();
        }

    }

    /*
    Safety check to make sure that another instance of the @CreatePathSegment is not running.
    Don't want to creat a memory leak or other problems by having a lot of them running.
     */
    private boolean isCreatePathSegmentRunning() {
        ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (CreatePathSegment.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
   private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CreatePathSegment.LocalBinder binder = (CreatePathSegment.LocalBinder) service;
            mService = binder.getService();
            mService.getLocationUpdate();
            mBound = true;
            Log.v("DEBUG: ", "Bound CreatePathSegment Service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
