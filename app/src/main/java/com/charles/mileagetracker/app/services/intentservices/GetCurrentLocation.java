package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class GetCurrentLocation extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String LOCATION_MESSENGER = "com.charles.milagetracker.app.LOCATION_MESSENGER";
    public static final int GET_LOCATION_MSG_ID = 2;
    private Messenger messenger = null;

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;

    private static int attempts = 0;


    public GetCurrentLocation() {
        super("CheckCurrentLocation");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent.getBooleanExtra("stop", false)) {
            Log.v("DEBUG: ", "Stopping location updates");

            if (locationClient != null) locationClient.disconnect();
            stopSelf();
            return;
        }

        if (intent != null) {
            Bundle extras=intent.getExtras();
            if (extras != null) {
                messenger = (Messenger)extras.get(LOCATION_MESSENGER);
                locationClient = new LocationClient(getApplicationContext(), this, this);
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(1000);
                locationListener = new MyLocationListener();
                locationClient.connect();
            }

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //locationClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("DEBUG: ", "Location Client Connected");
        locationClient.requestLocationUpdates(locationRequest, locationListener);

    }

    @Override
    public void onDisconnected() {
        locationClient.removeLocationUpdates(locationListener);
        //stopSelf();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("DEBUG: ", "Location Client Connection Failed");

    }
    /*
    Listen for location changes.  If it's not very accurate continue through, if after 10 attempts it can't
    get a good fix I'll just what it has.
     */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d("DEBUG: ", "Location Changed");
            if (location.getAccuracy() < 5 ) { //Less than 5 meter accuracy
                try {
                    generateMessage(location.getLatitude(), location.getLongitude());
                    locationClient.disconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (attempts < 10) {
                Log.v("DEBUG: ", "Not enough precision");
                attempts = attempts +1;
            } else {
                try {
                    generateMessage(location.getLatitude(), location.getLongitude());
                    locationClient.disconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }

        private void generateMessage(double lat, double lon) throws RemoteException {
            Message msg = Message.obtain();
            msg.arg1 = GET_LOCATION_MSG_ID;
            Bundle bundle = new Bundle();
            bundle.putDouble("lat", lat);
            bundle.putDouble("lon", lon);
            msg.setData(bundle);
            messenger.send(msg);

        }
    }
}
