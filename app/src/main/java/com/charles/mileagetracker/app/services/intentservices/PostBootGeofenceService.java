package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class PostBootGeofenceService extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationClient.OnAddGeofencesResultListener{

    private LocationClient locationClient = null;
    private LocationRequest locationRequest = null;
    private LocationListener locationListener = null;
    private Context context;

    private HashMap<Integer, LatLng> fenceCenters = new HashMap<Integer, LatLng>();
    private boolean addingProximityAlerts = false;
    private boolean startedActivityRecognition = false;

    private int counter = 0;

    public PostBootGeofenceService() {
        super("PostBootGeofenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            locationClient = new LocationClient(context, this, this);
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(1000);
            locationListener = new MyLocationListener();
            locationClient.connect();
            Log.v("DEBUG: ", "Trying to start locationclient from boot");

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("DEBUG: ", "Location Services Connected from Boot");
        addingProximityAlerts = true;
        locationClient.requestLocationUpdates(locationRequest, locationListener);

        Uri uri = TrackerContentProvider.STARTS_URI;
        String[] projection = {
                StartPoints.COLUMN_ID,
                StartPoints.START_LAT,
                StartPoints.START_LON
        };

        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);

        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                int id = c.getInt(c.getColumnIndexOrThrow(StartPoints.COLUMN_ID));
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));

                LatLng latLng = new LatLng(lat, lon);
                addProximityAlert(latLng, id);
                fenceCenters.put(id, latLng);
            }
        }

        if (fenceCenters.size() == 0) { //No fences defined disconnect so everything can close
            locationClient.removeLocationUpdates(locationListener);
            locationClient.disconnect();
        }
        addingProximityAlerts = false;

    }

    @Override
    public void onDisconnected() {
        Log.v("DEBUG: ", "Geofences added, disconnected from location services");

    }

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void addProximityAlert(LatLng latLng, int id) {
        this.context = context;
        Intent intent = new Intent("com.charles.mileagetracker.app.ACTION_RECEIVE_GEOFENCE");
        intent.putExtra("id", id);
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("lon", latLng.longitude);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Geofence fence = new Geofence.Builder()
                .setRequestId(Integer.toString(id))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        List fencesList = new ArrayList();
        fencesList.add(fence);
        locationClient.addGeofences(fencesList, pendingIntent, this);
        Log.d("DEBUG: ", "Adding proximity alert");
    }

    private int checkInFence(LatLng currentLocation, HashMap<Integer, LatLng> fenceCenters) {
        Log.v("DEBUG: ", "Checking if in fence");

        Iterator it = fenceCenters.keySet().iterator();
        double smallestDistance = Double.MAX_VALUE;
        int closestId = -1;
        while (it.hasNext()) {
            int id = (Integer)it.next();
            LatLng center = fenceCenters.get(id);
            double distance = getDistance(currentLocation, center);
            if (distance < 500){//Inside GeoFence
                return -1; //Known impossible value
            }
            if (distance < smallestDistance) {
                smallestDistance = distance; //Finding the nearest geofence
                closestId = id;
            }

        }
        return closestId;
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

    private final class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            counter = ++counter;
            Log.v("DEBUG: ", "Current Accuracy: " + Double.toString(location.getAccuracy()));
            if (!addingProximityAlerts) {
                if (location.getAccuracy() <= 100) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    int infenceId = checkInFence(currentLocation, fenceCenters);
                    if (infenceId != -1) {
                        startUpdates(location, infenceId);
                    }

                    locationClient.removeLocationUpdates(locationListener);
                    locationClient.disconnect();
                }

            }
        }
    }

    /*
    Check if there was a previously started record.  We want to use that if there was, if not then create
    a new one then start the service.  It uses the fence that it was closest to for the id.
     */
    private void startUpdates(Location location, int id) {
        AccessInternalStorage internalStorage = new AccessInternalStorage();

        try {
            TripVars tripVars = (TripVars)internalStorage.readObject(context, TripVars.KEY);
        } catch (IOException e) {
            TripVars tripVars = new TripVars();
            tripVars.setFenceTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);
            tripVars.setId(id);
            tripVars.setLat(location.getLatitude());
            tripVars.setLon(location.getLongitude());
            try {
                internalStorage.writeObject(context, TripVars.KEY, tripVars);
            } catch (IOException e1) {
                e1.printStackTrace();
                return;//Break points to prevent starting the service if there's problems
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        intent.putExtra("id", id);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lon", location.getLongitude());
        context.startService(intent);
        startedActivityRecognition = true;
    }

}
