package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
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

public class ActivityRecognitionIntentService extends IntentService {


    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

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


    private static double lastLat = -1;
    private static double lastLon = -1;
    private static long lastTime = -1;

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
        notInVehicleCounter = 0;
        secondsElapsed = 0;
        driving = true;
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

       if (notInVehicleCounter >= 2 && notInVehicleCounter < 5) {//Want to check for a quick update, if it's been more than 10 though not interested because you're stopped
           locationUpdateInProgress = true;
           createPathSegment();
           Log.v("DEBUG: ", Integer.toString(notInVehicleCounter) + " Requests not driving, getting current location");
       }
    }

    /*
    Start the intent to get the current location and update the database
     */
    private void createPathSegment() {
        Intent intent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        intent.putExtra(GetCurrentLocation.LOCATION_MESSENGER, new Messenger(handler));
        startService(intent);

    }

    /*
    A check to see if you've traveled more than 500 meters from the last taken location.
     */
    private boolean sufficientDistanceTraveled(LatLng latLng) {
        double distance = 0;
        if (lastLat == 0 && lastLon == 0 ) { //Means we're in our first run.
            lastLat = latLng.latitude;
            lastLon = latLng.longitude;
            return true;
        } else {
            distance = getDistance(latLng, new LatLng(lastLat, lastLon));
            if (distance > 500) {
                lastLat = latLng.latitude;
                lastLon = latLng.longitude;

                return true;
            }
        }

        return false;
    }

    private double getDistance(LatLng pointA, LatLng pointB) {
        double distance = 0f;

        Location a = new Location("pointA");
        a.setLatitude(pointA.latitude);
        a.setLongitude(pointA.longitude);

        Location b = new Location("pointB");
        b.setLatitude(pointB.latitude);
        b.setLongitude(pointB.longitude);

        distance = a.distanceTo(b);

        return distance;
    }

    /*
    Currently not used, it's a way to reverse lookup where you are in address format from a LatLng
     */
    private List<Address> checkLocation(LatLng location) {
        Geocoder geoCoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1);
            if (addresses.size() > 0) {
                return addresses;
            }
            for (Address address : addresses) {
                //Log.v("DEBUG: ", "Thoroughfare: " + address.getThoroughfare());
                Log.v("DEBUG: ", "Address line: " + address.getAddressLine(0));
                generateNotification(address.getAddressLine(0));
                address.getThoroughfare();
            }
        } catch (IOException ioe) {

        }
        return null;
    }

    private void generateNotification(String message) {
        Context context = getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Test")
                .setContentText(message);
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }


    /*
    Database handling code goes here
     */

    private void logSegment(LatLng latLng) {
        if (!sufficientDistanceTraveled(latLng)) {
            return;  //We're too close to the last point
        }
        Address startAddress = checkLocation(new LatLng(lastLat, lastLon)).get(0);
        Address endAddress = checkLocation(latLng).get(0);
        ContentValues values = new ContentValues();
        Log.v("DEBUG: ", "Address: " + endAddress.getAddressLine(0));
        values.put(PendingSegmentTable.END_ADDRESS, endAddress.getAddressLine(0));
        values.put(PendingSegmentTable.END_LAT, latLng.latitude);
        values.put(PendingSegmentTable.END_LON, latLng.longitude);
        values.put(PendingSegmentTable.TIME_END, System.currentTimeMillis());
        values.put(PendingSegmentTable.START_LAT, lastLat);
        values.put(PendingSegmentTable.START_LON, lastLon);
        values.put(PendingSegmentTable.START_ADDRESS, startAddress.getAddressLine(0));
        values.put(PendingSegmentTable.TIME_START, lastTime);

        getContentResolver().insert(TrackerContentProvider.PENDING_URI, values);

    }

    /*
    Message Handler
     */

    private Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle reply = msg.getData();
            double lat = reply.getDouble("lat");
            double lon = reply.getDouble("lon");
            Address addy = checkLocation(new LatLng(lat, lon)).get(0);
            Log.v("DEBUG: ", "Current address is: " + addy.getAddressLine(0));

        }
    };
}
