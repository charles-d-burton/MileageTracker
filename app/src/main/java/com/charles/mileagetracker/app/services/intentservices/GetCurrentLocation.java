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

    protected static final String LOCATION_MESSENGER = "com.charles.milagetracker.app.LOCATION_MESSENGER";
    private Messenger messenger = null;

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;

    //protected static double lastLat = -1;
    //protected static double lastLon = -1;
    //protected static long lastTime = -1;

    //private static volatile boolean driving = false;

    private static int attempts = 0;

    //private final IBinder mBinder = new LocalBinder();

    public GetCurrentLocation() {
        super("CheckCurrentLocation");
    }

   /*@Override
   public IBinder onBind(Intent intent) {
        return mBinder;
    }*/

    @Override
    protected void onHandleIntent(Intent intent) {

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


        /*if (lastLat == -1 && lastLon == 1) {
            if (intent != null) {
                lastLat = intent.getDoubleExtra("lat", -1);
                lastLon = intent.getDoubleExtra("lon", -1);
                //lastLat = ActivityRecognitionService.startPoint.latitude;
                //lastLon = ActivityRecognitionService.startPoint.longitude;
                lastTime = intent.getLongExtra("startTime", -1);
            }
        }*/
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, locationListener);

    }

    @Override
    public void onDisconnected() {
        locationClient.removeLocationUpdates(locationListener);

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    //Start the process to get a current location lock
    /*protected void getLocationUpdate() {
        driving = false;
        locationClient = new LocationClient(getApplicationContext(), this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationListener = new MyLocationListener();
        locationClient.connect();

    }*/

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
    Listen for location changes.  If it's not very accurate continue through, if after 10 attempts it can't
    get a good fix I'll just what it has.
     */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            if (location.getAccuracy() < 5 ) { //Less than 5 meter accuracy
                locationClient.disconnect();
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", location.getLatitude());
                bundle.putDouble("lon", location.getLongitude());

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            } else if (attempts < 10) {
                Log.v("DEBUG: ", "Not enough precision");
                attempts = attempts +1;
            } else {
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putDouble("lat", location.getLatitude());
                bundle.putDouble("lon", location.getLongitude());

                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                locationClient.disconnect();
            }
        }
    }

    /*public class LocalBinder extends Binder {
        GetCurrentLocation getService() {
            // Return this instance of GetCurrentLocation so clients can call public methods
            return GetCurrentLocation.this;
        }
    }*/
}
