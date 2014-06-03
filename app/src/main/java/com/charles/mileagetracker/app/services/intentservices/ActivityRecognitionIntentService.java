package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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

    public static final String RECOGNITION_SERVICE_MESSENGER = "com.charles.milagetracker.app.ACTIVITY_MESSENGER";
    public static final int RECOGNITION_SERVICE_MSG_ID = 1;


    private static int secondsElapsed = 0;
    private static long lastDrivingUpdate = 0l;
    private static int notInVehicleCounter = 0;
    private static boolean driving = false;


    private static boolean locationUpdateInProgress = false;


    private enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN }
    private ACTIVITY_TYPE mActivityType;

    private GetCurrentLocation mService = null;
    private boolean mBound = false;

    private static double startlat = -1;
    private static double startlon = -1;
    private static int id = -1;
    private static long startTime = -1;

    private Messenger messenger = null;




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

                //Messenger messenger = extractMessenger(intent);
                //if (messenger == null) return;

                activityUpdate(mostProbableActivity.getType(), confidence);

            } else {
                Log.v("DEBUG: ", "Where is the Result Intent WTH?");
            }


        }
    }

    private Messenger extractMessenger(Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null) {
            Log.v("EXTRAS NOT NULL", "GETTING MESSENGER");
            messenger = (Messenger)extras.get(RECOGNITION_SERVICE_MESSENGER);
            if (messenger == null) {
                Log.v("MESSENGER NULL: ", "I DON'T KNOW WHY THIS IS NULL");
            }
            return messenger;
        } else {
            return null;
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
                //Log.v("DEBUG: " , "Driving");
                driving(confidence);
                break;
            case DetectedActivity.ON_FOOT:
                //notDriving(confidence);
                notDriving(confidence, "walking");
                break;
            case DetectedActivity.UNKNOWN:
                Log.v("DEBUG: ", "Unknown");
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.v("DEBUG:", "Bike");
                break;
            case DetectedActivity.STILL:
                //Log.v("DEBUG: ", "Sitting Still");
                notDriving(confidence, "still");
                break;
            case DetectedActivity.TILTING:
                Log.v("DEBUG: ", "Tilting at windmills");
                break;
            default:
                Log.v("DEBUG: ", "Unknown");

                break;
        }
    }

    private void driving(int confidence) {
        Message message = Message.obtain();
        message.arg1 = RECOGNITION_SERVICE_MSG_ID;
        Bundle extras = new Bundle();
        extras.putString("activity", "driving");
        extras.putInt("confidence", confidence);
        message.setData(extras);
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            Log.v("DEBUG: ", "Messenger null");
        }

        /*notInVehicleCounter = 0;
        secondsElapsed = 0;
        driving = true;*/
    }

    /*
    Take the current time and calculate what the last time there was a not driving event was.  Use that
    time to get the time elapsed for not driving.  This is partly a safety check to help reduce false
    positives for things like sitting in traffic.
     */
    /*private void notDriving(int confidence) {
       notInVehicleCounter = notInVehicleCounter +1;

       long currentTime = System.currentTimeMillis();

       if (lastDrivingUpdate == 0) lastDrivingUpdate = currentTime + 1;
       int difference = Double.valueOf((currentTime - lastDrivingUpdate)/1000).intValue();
       secondsElapsed = secondsElapsed + difference;
       Log.v("DEBUG: ", "Seconds not driving: " + Integer.toString(secondsElapsed));

       if (notInVehicleCounter >= 2 && notInVehicleCounter < 5) {//Want to check for a quick update, if it's been more than 10 though not interested because you're stopped
           locationUpdateInProgress = true;
           createPathSegment();
           Log.v("DEBUG: ", Integer.toString(notInVehicleCounter) + " Requests not driving, getting current location");
       }
    }*/

    private void notDriving(int confidence, String activity) {
        Message message = Message.obtain();
        message.arg1 = RECOGNITION_SERVICE_MSG_ID;
        Bundle extras = new Bundle();
        extras.putBoolean("driving", false);
        extras.putString("activity", activity);
        extras.putInt("confidence", confidence);
        message.setData(extras);

        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            Log.v("DEBUG: ", "Messenger null");
        }

    }


    /*
    Start the intent to get the current location and update the database
     */
    private void createPathSegment() {
        Intent intent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        //intent.putExtra(GetCurrentLocation.LOCATION_MESSENGER, new Messenger(handler));
        //startService(intent);

    }


}
