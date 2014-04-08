package com.charles.mileagetracker.app.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class LocationPingService extends Service implements LocationListener {
    private boolean locationTimeExpired = false;

    private LocationManager lm;
    public static double lat;
    public static double lon;
    public static double acc;

    public static long updateInterval = 10000;//10 Second check

    private ArrayList<LocationUpdates> listeners = null;



    public LocationPingService() {
        listeners = new ArrayList<LocationUpdates>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v("Bind: ", "On Bind");
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        acc = location.getAccuracy();
        LatLng update = new LatLng(lat, lon);
        for (LocationUpdates lu : listeners) {
            lu.locationUpdated(update);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10f, this);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Location Manager: ", "onProviderDisabled");
        Toast.makeText(getApplicationContext(),"Attempted to ping your location, and GPS was disabled.",Toast.LENGTH_LONG).show();
    }

    public void addLocationListener(LocationUpdates lu) {
        listeners.add(lu);
    }


    private void timer() {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                locationTimeExpired = true;
            }
        }, updateInterval);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10f, this);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000,
                300f, this);

        return super.onStartCommand(intent, flags, startId);
    }

    private class PingLocationTask extends AsyncTask<String, Void, Boolean> {
        private Context context;
        private Service service;

        public PingLocationTask(Service service) {
            this.service = service;
            context = service;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return null;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                lm.removeUpdates(LocationPingService.this);
                onDestroy();
            }
        }
    }

    public interface LocationUpdates {
        public void locationUpdated(LatLng location);
    }
}