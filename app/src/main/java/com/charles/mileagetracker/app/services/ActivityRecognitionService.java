package com.charles.mileagetracker.app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.services.intentservices.ActivityRecognitionIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Date;

/**
 *A long-running service that starts when you leave a fenced in area.  This registers an IntentService
 * ActivityRecognitionIntentService to process your current activity.
 */

//TODO: I need to rework this class so that it initializes the variables as well as handles creating the db start entry
public class ActivityRecognitionService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {


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

    public ActivityRecognitionService() {

    }

    //Instantiate some variables, verify that Google Play Services is available, register the PendingIntent
    @Override
    public void onCreate() {
        mInProgress = false;
        servicesAvailable = servicesConnected();

        mActivityRecognitionClient = new com.google.android.gms.location.ActivityRecognitionClient(getApplicationContext(), this, this);

    }

    @Override
    public IBinder onBind(Intent intent) {
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

        if (intent == null) {
            return 0;
        }

        Intent pendingIntent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(),0, pendingIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        if (intent.getBooleanExtra("stop", false)) {

            /*Intent getLocationIntent = new Intent(getApplicationContext(), LogLocation.class);
            getLocationIntent.putExtra("stop", true);
            startService(getLocationIntent);*/
            stopUpdates();
            return 0;
        } else {
            id = intent.getIntExtra("id", -1);
            lat = intent.getDoubleExtra("lat", -1);
            lon = intent.getDoubleExtra("lon", -1);
            Log.v("DEBUG: ", "Lat/Lng from ActivityService: "+ "Lat: " + Double.toString(lat) + " Lon: " + Double.toString(lon));

            Status status = Status.listAll(Status.class).get(0);
            TripGroup group = status.trip_group;

            TripRow row = new TripRow(new Date(), new Date(), lat, lon, null, 0, group);
            row.save();

            Log.v("DEBUG: ", "ActivityRecognitionSerivce, starting from id: " + Integer.toString(id));
            mInProgress = false;
            startUpdates();
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        //generateNotification("Service Destroyed");
        super.onDestroy();
    }


    //Get sensor data every minute and register the PendingIntent with the Android system to get updates
    @Override
    public void onConnected(Bundle bundle) {
        switch (mRequestType) {
            case START:
                mActivityRecognitionClient.requestActivityUpdates(60000,mActivityRecognitionPendingIntent);
                mInProgress = false;
                Log.d("DEBUG:", "Request fulfilled, disconnecting");
                mActivityRecognitionClient.disconnect();
                break;
            case STOP:
                mActivityRecognitionClient.removeActivityUpdates(mActivityRecognitionPendingIntent);
                getApplicationContext().stopService(new Intent(getApplicationContext(), ActivityRecognitionIntentService.class));
                mActivityRecognitionClient.disconnect();
                mActivityRecognitionPendingIntent.cancel();
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
            Log.v("DEBUG Activity RecognitionService: ", "Play Services Not Available");
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
            Log.d("DEBUG: ", "Connecting client");
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
}
