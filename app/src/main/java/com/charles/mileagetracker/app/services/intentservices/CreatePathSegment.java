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
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class CreatePathSegment extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;

    protected static double lastLat = -1;
    protected static double lastLon = -1;
    protected static long lastTime = -1;

    private static volatile boolean driving = false;

    private static int attempts = 0;

    private final IBinder mBinder = new LocalBinder();

    public CreatePathSegment() {
        super("CheckCurrentLocation");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (lastLat == -1 && lastLon == 1) {
            if (intent != null) {
                lastLat = intent.getDoubleExtra("lat", -1);
                lastLon = intent.getDoubleExtra("lon", -1);
                //lastLat = ActivityRecognitionService.startPoint.latitude;
                //lastLon = ActivityRecognitionService.startPoint.longitude;
                lastTime = intent.getLongExtra("startTime", -1);
            }
        }
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
    protected void getLocationUpdate() {
        driving = false;
        locationClient = new LocationClient(getApplicationContext(), this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationListener = new MyLocationListener();
        locationClient.connect();

    }

    protected void startedDriving() {
        driving = true;
    }

    private void logSegment(LatLng latLng) {
        if (!sufficientDistanceTraveled(latLng)) {
            return;  //We're too close to the last point
        }
        Address startAddress = checkLocation(new LatLng(lastLat, lastLon)).get(0);
        Address endAddress = checkLocation(latLng).get(0);
        ContentValues  values = new ContentValues();
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
            if (driving) {
                locationClient.disconnect();
            } else if (location.getAccuracy() < 5 ) { //Less than 5 meter accuracy
                locationClient.disconnect();
                logSegment(new LatLng(location.getLatitude(), location.getLongitude()));
            } else if (attempts < 10) {
                Log.v("DEBUG: ", "Not enough precision");
                attempts = attempts +1;
            } else {

                logSegment(new LatLng(location.getLatitude(), location.getLongitude()));
                locationClient.disconnect();
            }

        }
    }

    public class LocalBinder extends Binder {
        CreatePathSegment getService() {
            // Return this instance of CreatePathSegment so clients can call public methods
            return CreatePathSegment.this;
        }
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
}
