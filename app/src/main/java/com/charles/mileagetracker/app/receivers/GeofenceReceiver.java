package com.charles.mileagetracker.app.receivers;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MapDrawerActivity;
import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processingservices.AddressDistanceServices;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.charles.mileagetracker.app.services.intentservices.CalcMileageService;
import com.charles.mileagetracker.app.services.intentservices.SaveBusinessRelated;
import com.charles.mileagetracker.app.services.intentservices.TripPostProcess;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class GeofenceReceiver extends BroadcastReceiver {


    private Context context = null;

    public GeofenceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context.getApplicationContext();
        if (intent != null) {
            int transitionType = LocationClient.getGeofenceTransition(intent);
            List geoFences = LocationClient.getTriggeringGeofences(intent);
            if (geoFences == null) return;//Fixes bug when geofence turned off
            Geofence fence = (Geofence)geoFences.get(0);
            int id = Integer.parseInt(fence.getRequestId());
            LatLng center = getCenter(id);
            if (center == null) {
                return;
            }


            if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                transitionExit(id, center);

            } else if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                transitionEnter(id, center);

            }
        }
    }

    private boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (ActivityRecognitionService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void calculateDistanceInBackground() {
        boolean alreadyRunning = false;
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (CalcMileageService.class.getName().equals(service.service.getClassName())){
                alreadyRunning = true;
                break;
            }
        }

        if (!alreadyRunning) {
            Intent connectedIntent = new Intent(context, CalcMileageService.class);
            context.startService(connectedIntent);
        }
    }

    private void transitionEnter(int id, LatLng center) {
        double lat = center.latitude;
        double lon = center.longitude;

        Intent stopActivityService = new Intent(this.context, ActivityRecognitionService.class);
        stopActivityService.putExtra("stop", true);
        stopActivityService.putExtra("id", id);
        stopActivityService.putExtra("lat", lat);
        stopActivityService.putExtra("lon", lon);

        this.context.startService(stopActivityService);
        //this.context.stopService(new Intent(this.context,LogLocation.class));


        List<Status> statuses = Status.listAll(Status.class);
        if (statuses.isEmpty()) return;

        Status status = statuses.get(0);
        TripGroup group = status.trip_group;
        group.group_closed = true;
        group.save();

        TripRow row = new TripRow(status.lastStopTime, new Date(), lat, lon, null, 0, group);
        row.save();

        TripPostProcess postProcess = new TripPostProcess();
        postProcess.processGroup(context, group.getId().intValue());

        //Set the address in the background
        //new AddressDistanceServices(context).setAddressBackground(row);
        Status.deleteAll(Status.class);
    }

    private void transitionExit(int id, LatLng center) {

        initTables(center.latitude, center.longitude);

        String message = "Leaving Fence, starting record";
        Log.v("From Geofence: ", message);
        Intent activityRecognitionService = new Intent(this.context, ActivityRecognitionService.class);
        activityRecognitionService.putExtra("id", id);
        activityRecognitionService.putExtra("lat", center.latitude);
        activityRecognitionService.putExtra("lon", center.longitude);
        activityRecognitionService.putExtra("transition", "exit");


        this.context.startService(activityRecognitionService);
        //generateNotification("Recording Trip",message, Geofence.GEOFENCE_TRANSITION_EXIT);
    }


    private void initTables(double lat, double lon) {
        List<Status>  statuses = Status.listAll(Status.class);
        if (!statuses.isEmpty()) {
            for (Status status : statuses) {
                status.delete();
            }
        } else {
            TripGroup group = new TripGroup(false);
            group.save();

            Status status = new Status(false, lat, lon, lat, lon, 0, new Date(), group);
            status.save();
        }
    }

    //Get the center of the geofence based on the id.
    private LatLng getCenter(int id) {
        LatLng center = null;

        List<HomePoints> homePoints = HomePoints.listAll(HomePoints.class);
        Iterator<HomePoints> it = homePoints.iterator();
        while (it.hasNext()) {
            HomePoints homePoint = it.next();
            if (homePoint.getId().intValue() == id) {
                center = new LatLng(homePoint.lat, homePoint.lon);
                break;
            }
        }

        return center;
    }
}
