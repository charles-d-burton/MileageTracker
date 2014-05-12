package com.charles.mileagetracker.app.services.intentservices;

import android.app.Instrumentation;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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

                //int activityType = mostProbableActivity.getType();

                //String activityName = getNameFromType(activityType);
                //mActivityType = getNameFromType(activityType);

                //activityUpdate(activityName);

                //Log.v("DEBUG: ", "Activity Name: " + activityName);


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
        if (confidence < 75) { //Less than really sure
            return;
        }
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                driving();
                break;
            default:
                notDriving();
                break;
        }

    }

    private void driving() {

    }

    private void notDriving() {

    }

}
