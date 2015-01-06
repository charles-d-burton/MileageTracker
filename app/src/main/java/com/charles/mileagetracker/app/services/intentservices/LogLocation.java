package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class LogLocation extends IntentService implements
        GetCurrentLocation.GetLocationCallback {

    public static final String LOCATION_MESSENGER = "com.charles.milagetracker.app.LOCATION_MESSENGER";
    public static final int GET_LOCATION_MSG_ID = 2;

    private GetCurrentLocation getLocation = null;

    public LogLocation() {
        super("CheckCurrentLocation");
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            if (intent.getBooleanExtra("stop", false)) {
                getLocation = new GetCurrentLocation(getApplicationContext(), 10, GetCurrentLocation.PRECISION.HIGH);
                getLocation.updateLocation(this, false);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getLocation != null) {
            getLocation.forceDisconnect();
        }
    }

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
     }

    @Override
    public void locationClientConnected() {

    }

    @Override
     public void locationConnectionFailed() {

     }

    private void logLocation(Location location) throws IOException, ClassNotFoundException {
        Status status = Status.listAll(Status.class).get(0);
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