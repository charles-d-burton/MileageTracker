package com.charles.mileagetracker.app.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.activities.PathSelectorActivity;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.services.intentservices.ActivityRecognitionIntentService;
import com.charles.mileagetracker.app.services.intentservices.GetCurrentLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 *A long-running service that starts when you leave a fenced in area.  This registers an IntentService
 * ActivityRecognitionIntentService to process your current activity.
 */
public class ActivityRecognitionService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String RECOGNITION_SERVICE_MESSENGER = "com.charles.milagetracker.app.ACTIVITY_MESSENGER";
    public static final int RECOGNITION_SERVICE_MSG_ID = 1;
    private static Messenger messenger = null;

    private boolean servicesAvailable = false;
    private boolean mInProgress = false;
    private boolean mClientConnected = false;

    private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private com.google.android.gms.location.ActivityRecognitionClient mActivityRecognitionClient;

    public enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;

    private int id = 0;
    private double lat = 0;
    private double lon = 0;
    private long startTime = -1;

    static double lastLat = -1;
    private static double lastLon = -1;
    private static long lastTime = -1;

    private static boolean driving = false;
    private static boolean segmentRecorded = false;
    private static int notDrivingUpdates = 0;



    public ActivityRecognitionService() {

    }

    //Instantiate some variables, verify that Google Play Services is available, register the PendingIntent
    @Override
    public void onCreate() {
        mInProgress = false;
        servicesAvailable = servicesConnected();
        messenger = new Messenger(incomingMessageHandler);
        startTime = System.currentTimeMillis();

        mActivityRecognitionClient = new com.google.android.gms.location.ActivityRecognitionClient(getApplicationContext(), this, this);

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v("DEBUG: ", "Binding ActivityRecognition Class");
        Log.v("DEBUG: ", Integer.toString(intent.getIntExtra("id", -1)));
        return null;
    }


    /*
    I use this to both start and stop the program.  If it's flagged to stop, I get the ending geofence variables,
    use them to record the ending geofence location and then stop the service.  If it's not flagged to stop then I
    register the @param ActivityRecognitionIntentService with the Android system to provide regular updates
    about what you're doing.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("stop", false)) {
            generateNotification("Stopping");
            //TODO: pull out the variables for the ending location
            stopSelf();
            return 0;
        }

        id = intent.getIntExtra("id", -1);
        lat = intent.getDoubleExtra("lat", -1);
        lon = intent.getDoubleExtra("lon", -1);
        //It's just starting so we're going to drop the center of the calling geofence as the starting location
        lastLat = lat;
        lastLon = lon;

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ActivityRecognitionIntentService.ACTIVITY_BROADCAST);
        registerReceiver(activityTypeReceiver, filter);


        Log.v("DEBUG: ", "ActivityRecognitionSerivce, starting from id: " + Integer.toString(id));

        Intent pendingIntent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(),0, pendingIntent,PendingIntent.FLAG_UPDATE_CURRENT);


        //setStartPoint(id);
        startUpdates();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        stopUpdates();
        unregisterReceiver(activityTypeReceiver);
        Intent getLocationIntent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        getLocationIntent.putExtra("stop", true);
        startService(getLocationIntent);
        super.onDestroy();
    }


    //Get sensor data every minute and register the PendingIntent with the Android system to get updates
    @Override
    public void onConnected(Bundle bundle) {
        switch (mRequestType) {
            case START:
                mActivityRecognitionClient.requestActivityUpdates(60000,mActivityRecognitionPendingIntent);
                mInProgress = false;
                mActivityRecognitionClient.disconnect();
                break;
            case STOP:
                mActivityRecognitionClient.removeActivityUpdates(mActivityRecognitionPendingIntent);
                getApplicationContext().stopService(new Intent(getApplicationContext(), ActivityRecognitionIntentService.class));
                //mActivityRecognitionClient.disconnect();

                break;
            default :
                //throw new Exception("Unknown request type in onConnected().");
                break;
        }

    }

    @Override
    public void onDisconnected() {

        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mActivityRecognitionClient = null;

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("Connection Failed", "Connection Failed");

    }

    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            return true;
        } else {

            return false;
        }
    }

    /**
     * Request activity recognition updates based on the current
     * detection interval.
     *
     */
    private void startUpdates() {
        // Check for Google Play services
        mRequestType = REQUEST_TYPE.START;
        if (!servicesConnected()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
            //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }

    private void stopUpdates() {
        mRequestType = REQUEST_TYPE.STOP;

        if(!servicesConnected()) {
            return;
        }

        if (!mInProgress) {
            mInProgress = true;
            mActivityRecognitionClient.connect();
        }
    }

    /*
    Broadcast receiver to receive updates from the ActivityRecognitionIntentService
    If you're driving it just keeps on trucking.  Otherwise it checks to see if you've recorded a path
    segment since the last time you were driving. If not, how many times has it recorded you not driving.
    If you've been sitting still or walking for 2 cycles(two minutes) then you're probably not traveling in a
    car so it's safe to record a path segment.
     */

    private BroadcastReceiver activityTypeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int confidence = intent.getIntExtra("confidence", -1);
            String type = intent.getStringExtra("type");
            Log.v("DEBUG: ", "RECEIVED ACTIVITY: " + type + " Confidence: " + Integer.toString(confidence));
            if (type.equalsIgnoreCase("driving")) {
                driving = true;
                segmentRecorded = false;
                notDrivingUpdates = 0;
                killGetLocation();
            } else if (type.equalsIgnoreCase("notdriving")) {
                driving = false;
                notDrivingUpdates = notDrivingUpdates + 1;
                Log.d("DEBUG: ", "Driving: " + Boolean.toString(driving) + " Recorded: " + Boolean.toString(segmentRecorded) + " Attempts: " + Integer.toString(notDrivingUpdates));
                if (!segmentRecorded && notDrivingUpdates == 2) {//Don't want to keep checking for locations

                    handleNotDriving();
                }

            }

        }

    };

    //Kills the GetCurrentLocation class.  Uses start service but sets a boolean to tell it to unregister and stop cleanly
    private void killGetLocation() {
        Intent intent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        intent.putExtra("stop", true);
        startService(intent);
    }

    private void handleNotDriving() {

        Intent startLocationIntent = new Intent(getApplicationContext(), GetCurrentLocation.class);
        startLocationIntent.putExtra(GetCurrentLocation.LOCATION_MESSENGER, messenger);
        startService(startLocationIntent);
    }


    private Handler incomingMessageHandler =new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //if (driving)return;//Last sanity check, this short circuits recording a trip if you suddenly start driving again
            Bundle reply = msg.getData();
            if (reply != null) {
                if (msg.arg1 == GetCurrentLocation.GET_LOCATION_MSG_ID) {
                    if (!segmentRecorded) {
                        double lat = reply.getDouble("lat");
                        double lon = reply.getDouble("lon");
                        logSegment(new LatLng(lat, lon));
                    }
                    //Address addy = checkLocation(new LatLng(lat, lon)).get(0);
                    //Log.v("DEBUG: ", "Current address is: " + addy.getAddressLine(0));
                }
            }
        }
    };

    /*
    Database handling code goes here
     */

    private void logSegment(LatLng latLng) {
        if (!sufficientDistanceTraveled(latLng)) {
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
        values.put(PendingSegmentTable.START_LAT, lastLat);
        values.put(PendingSegmentTable.START_LON, lastLon);
        values.put(PendingSegmentTable.START_ADDRESS, startAddress.getAddressLine(0));
        values.put(PendingSegmentTable.TIME_START, lastTime);

        getContentResolver().insert(TrackerContentProvider.PENDING_URI, values);

        lastLat = latLng.latitude;
        lastLon = latLng.longitude;

    }

    /*
    A check to see if you've traveled more than 500 meters from the last taken location.
     */
    private boolean sufficientDistanceTraveled(LatLng latLng) {
        double distance = 0;
        if (lastLat == -1 && lastLon == -1 ) { //Means we're in our first run.;
            distance = getDistance(new LatLng(lat, lon), new LatLng(latLng.latitude, latLng.longitude));
            if (distance > 500) {
                return true;//More than 500 meters from start point
            }

        } else {
            distance = getDistance(latLng, new LatLng(lastLat, lastLon));
            if (distance > 500) {
                return true;
            }
        }

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
}
