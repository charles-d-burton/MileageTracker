package com.charles.mileagetracker.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.charles.mileagetracker.app.services.intentservices.PostBootGeofenceService;
import com.google.android.gms.location.LocationClient;

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
