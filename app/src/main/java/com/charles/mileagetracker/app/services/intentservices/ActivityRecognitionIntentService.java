package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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


    private static int secondsElapsed = 0;
    private static long lastDrivingUpdate = 0l;
    private static int notInVehicleCounter = 0;
    private static boolean driving = false;


    private static boolean locationUpdateInProgress = false;


    public enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN }
    private ACTIVITY_TYPE mActivityType;

    private GetCurrentLocation mService = null;
    private boolean mBound = false;

    private static int id = -1;
    private static long startTime = -1;
    public static LatLng startPoint;

    private static double lastLat = -1;
    private static double lastLon = -1;
    private static long lastTime = -1;

    private Messenger messenger = null;




    @Override
    public void onCreate() {

        super.onCreate();
        //messenger = new Messenger(incomingMessageHandler);
    }

    /*
    Initialize some variables and get the calling Intent to retrieve the most likely activity
     */
    @Override
    protected void onHandleIntent(Intent intent) {
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
     *@param activityType The detected activity type
     *@return A user-readable name for the type
     * then using the confidence attempt to be sure that the current activity we think is happening
     * is actually happening.
     */

    private void activityUpdate(int activityType, int confidence) {
        //Log.v("DEBUG: ", "Confidence level: " + Integer.toString(confidence));
        if (confidence < 62) { //Less than really sure
            return;
        }
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                //Log.v("DEBUG: " , "Driving");
                broadcastActivityType(confidence, ACTIVITY_TYPE.DRIVING);
                break;
            case DetectedActivity.ON_FOOT:
                broadcastActivityType(confidence, ACTIVITY_TYPE.WALKING);
                //notDriving(confidence, "walking");
                break;
            case DetectedActivity.UNKNOWN:
                //Log.v("DEBUG: ", "Unknown");
                break;
            case DetectedActivity.ON_BICYCLE:
                //Log.v("DEBUG:", "Bike");
                break;
            case DetectedActivity.STILL:
                //Log.v("DEBUG: ", "Sitting Still");
                broadcastActivityType(confidence, ACTIVITY_TYPE.STILL);
                //notDriving(confidence, "still");
                break;
            case DetectedActivity.TILTING:
                Log.v("DEBUG: ", "Tilting at windmills");
                break;
            default:
                Log.v("DEBUG: ", "Unknown");

                break;
        }
    }

    /*
    Take the current time and calculate what the last time there was a not driving event was.  Use that
    time to get the time elapsed for not driving.  This is partly a safety check to help reduce false
    positives for things like sitting in traffic.
     */
    private void broadcastActivityType(int confidence, ACTIVITY_TYPE type) {
        Intent extras = new Intent();
        extras.putExtra("confidence", confidence);
        extras.setAction(ACTIVITY_BROADCAST);
        switch (type) {
            case DRIVING:
                extras.putExtra("type", "driving");
                break;
            case WALKING:
                extras.putExtra("type", "notdriving");
                break;
            case STILL:
                extras.putExtra("type", "notdriving");
                break;
            default:
                return;
        }
        sendBroadcast(extras);

       /*notInVehicleCounter = notInVehicleCounter +1;

       long currentTime = System.currentTimeMillis();

       if (lastDrivingUpdate == 0) lastDrivingUpdate = currentTime + 1;
       int difference = Double.valueOf((currentTime - lastDrivingUpdate)/1000).intValue();
       secondsElapsed = secondsElapsed + difference;
       Log.v("DEBUG: ", "Seconds not driving: " + Integer.toString(secondsElapsed));

       if (notInVehicleCounter >= 2 && notInVehicleCounter < 5) {//Want to check for a quick update, if it's been more than 10 though not interested because you're stopped
           locationUpdateInProgress = true;
           createPathSegment();
           Log.v("DEBUG: ", Integer.toString(notInVehicleCounter) + " Requests not driving, getting current location");
       }*/
    }
}
