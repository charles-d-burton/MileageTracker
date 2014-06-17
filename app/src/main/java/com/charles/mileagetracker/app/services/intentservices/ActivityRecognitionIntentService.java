package com.charles.mileagetracker.app.services.intentservices;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.charles.mileagetracker.app.activities.PathSelectorActivity;
import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripRowCreator;
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


    public enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN}

    private ACTIVITY_TYPE mActivityType;

    private GetCurrentLocation mService = null;
    private boolean mBound = false;

    private static int id = -1;

    private Messenger messenger = null;


    //private boolean segmentRecorded;

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
        if (confidence < 62) { //Less than really sure
            return;
        }
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                //Log.v("DEBUG: " , "Driving");
                handleDriving();
                break;
            case DetectedActivity.ON_FOOT:
                handleNotDriving();
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
                handleNotDriving();
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

    //If driving then we're going to set the variables to their least known values and write them

    private void handleDriving() {
        tripVars.setDriving(true);
        tripVars.setNotDrivingCounter(0);
        tripVars.setSegmentRecorded(false);
        killGetLocation();
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
    private void handleNotDriving() {
        tripVars.setDriving(false);
        int counter = tripVars.getNotDrivingCounter();
        if (counter < 2 ) {
            counter = counter + 1;
            tripVars.setNotDrivingCounter(counter);
            try {
                accessCache.writeObject(getApplicationContext(), tripVars.KEY, tripVars);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (counter >= 2) {
            counter = counter + 1;
            tripVars.setNotDrivingCounter(counter);
            if (!tripVars.isSegmentRecorded()) {
                double lastLat = tripVars.getLastLat();
                double lastLon = tripVars.getLastLon();
                //tripVars.setSegmentRecorded(true);

                messenger = new Messenger(incomingMessageHandler);
                startLocationHandler(lastLat, lastLon, false);
            }
            //The following try catch block might be moved later
            try {
                accessCache.writeObject(getApplicationContext(), tripVars.KEY, tripVars);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    This is created to handle messages incoming from the GetCurrentLocation service.  When that service
    gets a good locatino fix it fires this to record that location as a database entry.
     */
    private Handler incomingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TripVars roTripVars = null;
            try {
                roTripVars = (TripVars)accessCache.readObject(getApplicationContext(), TripVars.KEY);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (roTripVars.isDriving() == false && !roTripVars.isSegmentRecorded()) {
                Bundle reply = msg.getData();
                if (reply != null) {
                    if (msg.arg1 == GetCurrentLocation.GET_LOCATION_MSG_ID) {
                        double lat = reply.getDouble("lat");
                        double lon = reply.getDouble("lon");
                        Address addy = checkLocation(new LatLng(lat, lon)).get(0);
                        Log.v("DEBUG: ", "Current address is: " + addy.getAddressLine(0));
                        boolean logged = logSegment(new LatLng(lat, lon), tripVars);
                        if (logged) try {
                            accessCache.writeObject(getApplicationContext(),TripVars.KEY,tripVars);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            //if (driving)return;//Last sanity check, this short circuits recording a trip if you suddenly start driving again
        }
    };


    //Kills the GetCurrentLocation class.  Uses start service but sets a boolean to tell it to unregister and stop cleanly
    private void killGetLocation() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), GetCurrentLocation.class);
            intent.putExtra("stop", true);
            startService(intent);
        }
    }

    private void startLocationHandler(double lastLat, double lastLon, boolean stop) {

        Intent startLocationIntent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        if (stop) startLocationIntent.putExtra("stop", stop);
        startLocationIntent.putExtra(GetCurrentLocation.LOCATION_MESSENGER, messenger);
        startService(startLocationIntent);
    }


    /*
    Database handling code goes here
     */

    private boolean logSegment(LatLng latLng, TripVars vars) {
        double lastLat = vars.getLastLat();
        double lastLon = vars.getLastLon();
        if (lastLat == -1 && lastLon == -1) {
            lastLat = vars.getLat();
            lastLon = vars.getLon();
        }

        double distance = getDistance(latLng, new LatLng(lastLat, lastLon));
        if (distance > 500) {
            TripRowCreator rowCreator = new TripRowCreator(getApplicationContext());
            rowCreator.recordSegment(tripVars.getId(),latLng.latitude, latLng.longitude);
            tripVars.setSegmentRecorded(true);
            return true;
        }
        return false;
        /*if (!sufficientDistanceTraveled(latLng) || driving) {
            return;  //We're too close to the last point
        }
        segmentRecorded = true;
        Address startAddress = checkLocation(new LatLng(lastLat, lastLon)).get(0);
        Address endAddress = checkLocation(latLng).get(0);
        ContentValues values = new ContentValues();
        Log.v("DEBUG: ", "Address: " + endAddress.getAddressLine(0));
        values.put(PendingSegmentTable.END_ADDRESS, endAddress.getAddressLine(0));
        values.put(PendingSegmentTable.END_LAT, latLng.latitude);
        values.put(PendingSegmentTable.END_LON, latLng.longitude);
        values.put(PendingSegmentTable.TIME_END, System.currentTimeMillis());
        values.put(PendingSegmentTable.LAT, lastLat);
        values.put(PendingSegmentTable.LON, lastLon);
        values.put(PendingSegmentTable.ADDRESS, startAddress.getAddressLine(0));
        values.put(PendingSegmentTable.TIME, lastTime);

        getContentResolver().insert(TrackerContentProvider.TRIP_URI, values);

        lastLat = latLng.latitude;
        lastLon = latLng.longitude;*/

    }

    /*
    A check to see if you've traveled more than 500 meters from the last taken location.
     */
    private boolean sufficientDistanceTraveled(LatLng sLatLng, LatLng eLatLng) {
        double distance = 0;
        /*if (lastLat == -1 && lastLon == -1 ) { //Means we're in our first run.;
            distance = getDistance(new LatLng(lat, lon), new LatLng(latLng.latitude, latLng.longitude));
            if (distance > 500) {
                return true;//More than 500 meters from start point
            }

        } else {
            distance = getDistance(latLng, new LatLng(lastLat, lastLon));
            if (distance > 500) {
                return true;
            }
        }*/

        return false;
    }

    /*
    Check if a start location was recorded,
     */

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
    A way to reverse lookup where you are in address format from a LatLng
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
        Intent resultIntent = new Intent(context, PathSelectorActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PathSelectorActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    //First run, set starting point
    private void setStartPoint(int id) {
        String projection[] = {StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON};
        String selectionClause = StartPoints.COLUMN_ID + "= ? ";
        String selectionArgs[] = {Integer.toString(id)};

        Cursor c = getContentResolver().query(TrackerContentProvider.STARTS_URI, projection, selectionClause, selectionArgs, null);

        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Log.v("DEBUG: ", "Start Point Found, setting first location");
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));
                //startPoint = new LatLng(lat, lon);
            }
        }

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
