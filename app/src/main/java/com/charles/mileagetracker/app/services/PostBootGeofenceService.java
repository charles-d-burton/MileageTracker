package com.charles.mileagetracker.app.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
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
    private Context context;

    public PostBootGeofenceService() {
        super("PostBootGeofenceService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            locationClient = new LocationClient(context, this, this);
            locationClient.connect();
            Log.v("DEBUG: ", "Trying to start locationclient from boot");

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("DEBUG: ", "Location Services Connected from Boot");
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
                addProximityAlert(new LatLng(lat, lon), id);
            }
        }
        locationClient.disconnect();
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
}
