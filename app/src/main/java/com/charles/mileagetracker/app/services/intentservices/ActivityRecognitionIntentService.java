package com.charles.mileagetracker.app.services.intentservices;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */


//TRY TO MAKE THIS A BROADCAST RECEIVER IN THE MORNING
public class ActivityRecognitionIntentService extends IntentService implements
        GetCurrentLocation.GetLocationCallback{


    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    public static final String ACTIVITY_BROADCAST = "com.charles.mileagetracker.app.ACTIVITY_BROADCAST";

    public enum ACTIVITY_TYPE {DRIVING, WALKING, BIKING, STILL, TILTING, UNKNOWN}

    private ACTIVITY_TYPE mActivityType;

    //private LogLocation mService = null;
    private boolean mBound = false;

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private GetCurrentLocation getLocation = null;

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
        List<Status> statuses = Status.listAll(Status.class);
        if (statuses.isEmpty()) return;

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
                Log.v("DEBUG: " , "Driving");
                handleDriving();
                break;
            case DetectedActivity.ON_FOOT:
                Log.v("DEBUG: ", "WALKING");
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
                Log.v("DEBUG: ", "Still");
                handleStill(confidence);
                break;
            case DetectedActivity.TILTING:
                handleStill(confidence);
                //Log.v("DEBUG: ", "Tilting at windmills");
                break;
            default:
                Log.v("DEBUG: ", "Unknown");

                break;
        }
    }

    //If driving then we're going to set the variables to their least known values and write them

    private void handleDriving() {
        Status status = loadStatus();
        Log.v("DEBUG: ", "Driving=" + Boolean.toString(status.driving));
        Log.v("DEBUG: ", "LastStopTime=" + format.format(status.lastStopTime));
        if (!status.driving) {
            status.driving = true;
            status.lastStopTime = new Date();
            status.notDrivingCount = 0;
            status.stopRecorded = false;
            status.stopRecording = false;
            status.save();
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
        if (confidence > 75) {
            Status status = loadStatus();
            Log.v("DEBUG: ", "Walking Count=" + Integer.toString(status.notDrivingCount));
            status.driving = false;

            int counter = status.notDrivingCount;
            counter = counter + 1;
            if (counter < 5) {
                status.notDrivingCount = counter;
            }

            if (counter > 2 && !status.stopRecorded && !status.stopRecording) {//Trip segment not recorded, the class GetCurrentLocation will set this flag to true
                status.stopRecording = true;
                startLocationHandler();
            }

            status.save();
        }
    }


    //Handle sitting still, if you've been sitting still for 4 minutes then record a stop
    private void handleStill(int confidence) {
        Status status = loadStatus();
        status.driving = false;
        int counter = status.notDrivingCount;
        Log.v("DEBUG: ", "Still Count=" + Integer.toString(status.notDrivingCount));
        counter = counter + 1;
        status.notDrivingCount = counter;
        if (counter >= 4 && !status.stopRecorded && !status.stopRecording) {
            status.stopRecording = true;
            startLocationHandler();
        }
        status.save();
    }

    //Kills the GetCurrentLocation class.  Uses start service but sets a boolean to tell it to unregister and stop cleanly
    private void killGetLocation() {
        /*if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LogLocation.class);
            intent.putExtra("stop", true);
            startService(intent);
        }*/
    }


    /*private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (LogLocation.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }*/

    private Status loadStatus() {
        Status status = Status.listAll(Status.class).get(0);
        Log.v("DEBUG: ", "STATUS LOADED");
        return status;
    }


    /*
    Start listening for location updates.
     */
    private void startLocationHandler() {

        //Intent startLocationIntent = new Intent(getApplicationContext(), LogLocation.class);
        //startService(startLocationIntent);
        getLocation = new GetCurrentLocation(getApplicationContext(), 10, GetCurrentLocation.PRECISION.HIGH);
        getLocation.updateLocation(this, false);
    }


    //Called when the GetCurrentLocation class defined in @startLocationHandler returns a valid location
    @Override
    public void retrievedLocation(double resolution, Location location) {
        if (!tooCloseToStartPoint(location)) {
            try {
                logLocation(location);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (getLocation != null) {
            getLocation.forceDisconnect();
        }
    }

    @Override
    public void locationClientConnected() {

    }

    @Override
    public void locationConnectionFailed() {

    }

    private void logLocation(Location location) throws IOException, ClassNotFoundException {
        Status status = loadStatus();
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        LatLng oldLocation = new LatLng(status.lastLat, status.lastLon);
        double distance = getDistance(oldLocation, new LatLng(lat, lon));

        if (distance > 1000) {//Larger than the geofence, gives me a margin of error
            TripRow row = new TripRow(status.lastStopTime, new Date(), lat, lon, null, 0, status.trip_group);
            row.save();

            new AddressDistanceServices(this.getApplicationContext()).setAddress(row);

            Executors.newSingleThreadExecutor().execute(new LookupDistance(row, lat, lon, status.lastLat, status.lastLon));

            //Update Status to reflect that a row has been recorded, where it was last recorded, and we're no longer processing
            status.stopRecorded = true;
            status.lastLat = lat;
            status.lastLon = lon;
            status.stopRecording = false;
            status.lastStopTime = new Date();
            status.save();
        }

    }

    /*
    Check if the stop distance is too close to a defined starting point
     */
    private boolean tooCloseToStartPoint(Location currentLocation) {
        boolean tooClose = false;

        LatLng currentPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        List<HomePoints> homePointsList = HomePoints.listAll(HomePoints.class);
        if (homePointsList.isEmpty()) return false;

        for (HomePoints home : homePointsList) {
            double lat = home.lat;
            double lon = home.lon;
            LatLng startPoint = new LatLng(lat, lon);
            double distance = getDistance(currentPoint, startPoint);
            if (distance < 1000) {
                tooClose = true;
            }
        }

        return tooClose;
    }

    //Find straight line distance between two points
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
    This work happens on a background thread.  It takes the two points and then calculates the ROAD
    distance between them.  It then updates the database with the distance value for the stop that
    was just created.  Doing this on a background thread is much saner and more efficient, allows the
    program to continue running without waiting for network IO to fulfill this request.
     */
    private class LookupDistance implements Runnable {

        private double startLat = Double.MAX_VALUE;
        private double startLon = Double.MAX_VALUE;
        private double endLat = Double.MAX_VALUE;
        private double endLon = Double.MAX_VALUE;
        private TripRow row = null;

        public LookupDistance(TripRow row , double startLat, double startLon, double endLat, double endLon) {
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
            this.row = row;
        }


        @Override
        public void run() {
            if (row == null) {
                return;
            }
            AddressDistanceServices distanceServices = new AddressDistanceServices(getApplicationContext());
            double distance = distanceServices.getDistance(startLat,startLon,endLat,endLon);
            if (distance != -1) {
                row.distance = distance;
                row.save();
            }
        }
    }
}
