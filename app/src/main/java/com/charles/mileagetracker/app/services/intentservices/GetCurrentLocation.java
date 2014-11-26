package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Date;
import java.util.List;


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

    private int locationResolution = 100;

    private enum Provider {HIGH_ACCURACY, LOW_ACCURACy}


    public GetCurrentLocation() {
        super("CheckCurrentLocation");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            if (intent.getBooleanExtra("stop", false)) {
                Log.v("DEBUG: ", "Stopping location updates");
                try {
                    if (locationListener != null && locationClient.isConnected()) {
                        locationClient.removeLocationUpdates(locationListener);
                        locationClient.disconnect();
                    }
                } catch (Exception e) {

                }
                return;
            }
        }

        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            initLocation(Provider.HIGH_ACCURACY);
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            initLocation(Provider.LOW_ACCURACy);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationClient != null && locationClient.isConnected()){
            locationClient.removeLocationUpdates(locationListener);
            locationClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("DEBUG: ", "Location Client Connected");
        locationClient.requestLocationUpdates(locationRequest, locationListener);

    }

    @Override
    public void onDisconnected() {
        //locationClient.removeLocationUpdates(locationListener);
        //stopSelf();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("DEBUG: ", "Location Client Connection Failed");

    }

    //Uses the selected location provider to init the location callbacks
    private void initLocation(Provider provider) {

        if (locationListener != null && locationClient.isConnected()) {
            locationClient.removeLocationUpdates(locationListener);
            locationClient.disconnect();
        }

        locationClient = new LocationClient(getApplicationContext(), this, this);
        locationRequest = LocationRequest.create();

        switch (provider) {
            case HIGH_ACCURACY:
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationResolution = 200;
                break;
            case LOW_ACCURACy:
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                locationResolution = 1000;
                break;
            default:
                break;
        }

        if (locationListener == null) {
            locationListener = new MyLocationListener();
        }

        locationClient.connect();
    }

    /*
    Listen for location changes.  If it's not very accurate continue through, if after 10 attempts it can't
    get a good fix I'll just take what it has.
     */
    private class MyLocationListener implements LocationListener {
        private int counter = 0;
        @Override
        public void onLocationChanged(Location location) {

            //Log.d("DEBUG: ", "Location Changed.  Accuracy: " + Double.toString(location.getAccuracy()));
            if (location != null && location.getAccuracy() <= locationResolution) {
                if (tooCloseToStartPoint(location)) {
                    locationClient.removeLocationUpdates(this);
                    locationClient.disconnect();
                } else {
                    try {
                        logLocation(location);
                        //generateMessage(location.getLatitude(), location.getLongitude());
                        locationClient.removeLocationUpdates(this);
                        locationClient.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (location != null && location.getAccuracy() > locationResolution) {
                counter = counter + 1;
                if (counter == 10) {
                    LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    try {
                        logLocation(location);
                        locationClient.removeLocationUpdates(this);
                        locationClient.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void logLocation(Location location) throws IOException, ClassNotFoundException {
        Status status = Status.listAll(Status.class).get(0);
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        LatLng oldLocation = new LatLng(status.lastLat, status.lastLon);
        double distance = getDistance(oldLocation, new LatLng(lat, lon));

        if (distance > 1000) {//Larger than the geofence, gives me a margin of error
            AddressDistanceServices distanceServices = new AddressDistanceServices(this.getApplicationContext());
            String address = distanceServices.getAddressFromLatLng(new LatLng(lat, lon));

            TripRow row = new TripRow(new Date(System.currentTimeMillis()), lat, lon, address, 0, status.trip_group);
            row.save();

            new LookupDistance(row, lat, lon, status.lastLat, status.lastLon).run();

            //Update Status to reflect that a row has been recorded, where it was last recorded, and we're no longer processing
            status.stopRecorded = true;
            status.lastLat = lat;
            status.lastLon = lon;
            status.stopRecording = false;
            status.save();
        }

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
    Check if the straight line distance between two points is too close for a useful stop
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
