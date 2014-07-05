package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripRowCreator;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class GetCurrentLocation extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String LOCATION_MESSENGER = "com.charles.milagetracker.app.LOCATION_MESSENGER";
    public static final int GET_LOCATION_MSG_ID = 2;

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;

    private static int locationTryCounter = 0;


    public GetCurrentLocation() {
        super("CheckCurrentLocation");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent.getBooleanExtra("stop", false)) {
            Log.v("DEBUG: ", "Stopping location updates");
            try {
                locationClient.disconnect();
            } catch (Exception e) {

            }
            return;
        }

        if (intent != null) {
            initLocation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationClient.isConnected()) locationClient.disconnect();
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
            Log.d("DEBUG: ", "Location Changed.  Accuracy: " + Double.toString(location.getAccuracy()));
            if (location != null && location.getAccuracy() <= 100) {
                if (tooCloseToStartPoint(location)) {
                    locationClient.disconnect();
                } else {
                    try {
                        logLocation(location);
                        //generateMessage(location.getLatitude(), location.getLongitude());
                        locationClient.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void initLocation() {
        locationClient = new LocationClient(getApplicationContext(), this, this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationListener = new MyLocationListener();
        locationClient.connect();
    }


    private void logLocation(Location location) throws IOException, ClassNotFoundException {
        AccessInternalStorage accessCache = new AccessInternalStorage();
        TripVars tripVars = (TripVars)accessCache.readObject(getApplicationContext(), TripVars.KEY);
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        LatLng oldLocation = new LatLng(tripVars.getLastLat(), tripVars.getLastLon());
        double distance = getDistance(oldLocation, new LatLng(lat, lon));

        if (distance > 750) {//Larger than the geofence, gives me a margin of error
            TripRowCreator rowCreator = new TripRowCreator(getApplicationContext());
            rowCreator.recordSegment(tripVars.getId(),lat, lon);
            tripVars.setSegmentRecorded(true);
            tripVars.setLastLat(lat);
            tripVars.setLastLon(lon);
            accessCache.writeObject(getApplicationContext(), TripVars.KEY, tripVars);
        }

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
    Get a cursor and check if we're too close check if we're too close to start point
     */

    private boolean tooCloseToStartPoint(Location currentLocation) {
        boolean tooClose = false;
        LatLng currentPoint = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        String projection[] = {
                StartPoints.START_LAT,
                StartPoints.START_LON

        };
        Cursor c = getContentResolver().query(TrackerContentProvider.STARTS_URI,projection, null, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));
                LatLng startPoint = new LatLng(lat, lon);
                double distance = getDistance(currentPoint, startPoint);
                if (distance < 1000) {//Closer than a kilometer which is within the margin of error
                    tooClose = true;
                }
            }
        }
        if (c != null) c.close();


        return tooClose;
    }
}
