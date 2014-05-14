package com.charles.mileagetracker.app.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.charles.mileagetracker.app.services.intentservices.ActivityRecognitionIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Iterator;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
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


    public ActivityRecognitionService() {

    }

    @Override
    public void onCreate() {
        mInProgress = false;
        servicesAvailable = servicesConnected();

        mActivityRecognitionClient = new com.google.android.gms.location.ActivityRecognitionClient(getApplicationContext(), this, this);
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(getApplicationContext(),0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startUpdates();
        return 0;
    }

    @Override
    public void onDestroy() {
        stopUpdates();
        super.onDestroy();
    }


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
}
