package com.charles.mileagetracker.app.receivers;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.Status;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.charles.mileagetracker.app.services.intentservices.GetCurrentLocation;
import com.charles.mileagetracker.app.services.intentservices.SaveBusinessRelated;
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

    private void transitionEnter(int id, LatLng center) {
        double lat = center.latitude;
        double lon = center.longitude;

        Intent stopActivityService = new Intent(this.context, ActivityRecognitionService.class);
        stopActivityService.putExtra("stop", true);
        stopActivityService.putExtra("id", id);
        stopActivityService.putExtra("lat", lat);
        stopActivityService.putExtra("lon", lon);

        this.context.startService(stopActivityService);
        this.context.stopService(new Intent(this.context,GetCurrentLocation.class));

        Status status = Status.listAll(Status.class).get(0);
        TripGroup group = status.trip_group;
        group.group_closed = true;
        group.save();

        TripRow row = new TripRow(status.lastStopTime, new Date(), lat, lon, null, 0, group);
        row.save();

        //Set the address in the background
        new AddressDistanceServices(context).setAddress(row);

        Status.deleteAll(Status.class);

        generateNotification("Trip Complete","Were all stops business related?", group.getId().intValue());
    }

    private void transitionExit(int id, LatLng center) {

        initTables(center.latitude, center.longitude);

        String message = "Leaving Fence, starting record";
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


    private void generateNotification(String title, String message, int groupId) {

        Intent pathIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(pathIntent);

        PendingIntent selectPathSegments = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        //Intent to record all trip segments
        Intent saveTripService = new Intent(context, SaveBusinessRelated.class);
        saveTripService.putExtra("group", groupId);
        PendingIntent saveTrip = PendingIntent.getService(context, 0, saveTripService,PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(selectPathSegments)
                .addAction(android.R.drawable.btn_plus, "Yes", saveTrip)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "No", selectPathSegments);

        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
