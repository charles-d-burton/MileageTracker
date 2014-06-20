package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.charles.mileagetracker.app.database.TripRowCreator;
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
public class GetCurrentLocation extends IntentService implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String LOCATION_MESSENGER = "com.charles.milagetracker.app.LOCATION_MESSENGER";
    public static final int GET_LOCATION_MSG_ID = 2;

    private static LocationClient locationClient = null;
    private static LocationRequest locationRequest = null;
    private static LocationListener locationListener = null;




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

            stopSelf();
            return;
        }

        if (intent != null) {
            locationClient = new LocationClient(getApplicationContext(), this, this);
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(1000);
            locationListener = new MyLocationListener();
            locationClient.connect();
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
            if (location != null) {
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

    private void logLocation(Location location) throws IOException, ClassNotFoundException {
        AccessInternalStorage accessCache = new AccessInternalStorage();
        TripVars tripVars = (TripVars)accessCache.readObject(getApplicationContext(), TripVars.KEY);
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        LatLng oldLocation = new LatLng(tripVars.getLastLat(), tripVars.getLon());
        double distance = getDistance(oldLocation, new LatLng(lat, lon));

        if (distance > 750) {//Larger than the geofence, gives me a margin of error
            Address addy = checkLocation(new LatLng(lat, lon)).get(0);//TODO:  I need to fix this in case it's null
            Log.v("DEBUG: ", "Current address is: " + addy.getAddressLine(0));
            boolean logged = logSegment(new LatLng(lat, lon), tripVars);

            if (logged) {
                tripVars.setLastLat(lat);
                tripVars.setLastLon(lon);
                tripVars.setSegmentRecorded(true);
                accessCache.writeObject(getApplicationContext(), TripVars.KEY, tripVars);
            }
        }
    }

    /*
    Database handling code goes here
     */

    private boolean logSegment(LatLng latLng, TripVars vars) {
        double lastLat = vars.getLastLat();
        double lastLon = vars.getLastLon();
        if (lastLat == -1 && lastLon == -1) {
            lastLat = vars.getLat();
            lastLon = vars.getLon();
        }

        double distance = getDistance(latLng, new LatLng(lastLat, lastLon));
        if (distance > 500) {
            TripRowCreator rowCreator = new TripRowCreator(getApplicationContext());
            rowCreator.recordSegment(vars.getId(),latLng.latitude, latLng.longitude);
            vars.setSegmentRecorded(true);
            return true;
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
            /*for (Address address : addresses) {
                //Log.v("DEBUG: ", "Thoroughfare: " + address.getThoroughfare());
                Log.v("DEBUG: ", "Address line: " + address.getAddressLine(0));
                //generateNotification(address.getAddressLine(0));
            }*/
        } catch (IOException ioe) {

        }
        return null;
    }
}
