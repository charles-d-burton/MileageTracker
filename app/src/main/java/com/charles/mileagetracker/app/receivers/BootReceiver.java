package com.charles.mileagetracker.app.receivers;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.services.intentservices.PostBootGeofenceService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private LocationClient locationClient = null;
    private Location location = null;
    private Context context;

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("DEBUG: ", "Booted");
        Intent geoFenceIntent = new Intent(context, PostBootGeofenceService.class);
        context.startService(geoFenceIntent);

    }
}
