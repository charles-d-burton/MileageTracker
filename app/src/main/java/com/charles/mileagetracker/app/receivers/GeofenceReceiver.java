package com.charles.mileagetracker.app.receivers;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.PathSelectorActivity;
import com.charles.mileagetracker.app.cache.AccessInternalStorage;
import com.charles.mileagetracker.app.cache.TripVars;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripRowCreator;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
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

    private void initializeVars(int id, double lat, double lon, int transition) throws IOException {
        AccessInternalStorage accessInternalStorage = new AccessInternalStorage();
        TripVars vars = new TripVars();
        vars.setId(id);
        vars.setLat(lat);
        vars.setLon(lon);
        vars.setLastLat(lat);
        vars.setLastLon(lon);
        vars.setFenceTransitionType(transition);
        accessInternalStorage.writeObject(this.context, TripVars.KEY, vars);
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

    private LatLng getCenter(int id) {
        LatLng center = null;
        String projection[] = {StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON};

        Cursor c = context.getContentResolver().query(TrackerContentProvider.STARTS_URI, projection,
                StartPoints.COLUMN_ID + "=" + Integer.toString(id), null, null);

        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
            double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));
            center = new LatLng(lat, lon);

        }
        if (c != null) c.close();
        return center;
    }

    private void transitionEnter(int id, LatLng center) {
        String message = "Entering fence";
        //boolean running = isServiceRunning();
        Log.v("DEBUG: ", "Killing Running Record Service");
        double lat = center.latitude;
        double lon = center.longitude;

        Intent stopActivityService = new Intent(this.context, ActivityRecognitionService.class);
        stopActivityService.putExtra("stop", true);
        stopActivityService.putExtra("id", id);
        stopActivityService.putExtra("lat", lat);
        stopActivityService.putExtra("lon", lon);

        this.context.startService(stopActivityService);

        //Record our end point then close out the trip
        TripRowCreator creator = new TripRowCreator(this.context);
        creator.closeGroup(id, lat, lon);
        generateNotification("Trip Complete","Were all stops business related?", Geofence.GEOFENCE_TRANSITION_ENTER);
    }

    private void transitionExit(int id, LatLng center) {
        try {
            initializeVars(id, center.latitude, center.longitude, Geofence.GEOFENCE_TRANSITION_EXIT);
        } catch (IOException e) {
            e.printStackTrace();
            return;

        }

        String message = "Leaving Fence, starting record";
        Intent activityRecognitionService = new Intent(this.context, ActivityRecognitionService.class);
        activityRecognitionService.putExtra("id", id);
        activityRecognitionService.putExtra("lat", center.latitude);
        activityRecognitionService.putExtra("lon", center.longitude);
        activityRecognitionService.putExtra("transition", "exit");

        this.context.startService(activityRecognitionService);
        generateNotification("Recording Trip",message, Geofence.GEOFENCE_TRANSITION_EXIT);
    }


    private void generateNotification(String title, String message, int geoFenceMode) {

        Intent pathIntent = new Intent(context, PathSelectorActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PathSelectorActivity.class);
        stackBuilder.addNextIntent(pathIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.btn_plus, "Yes", pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "No", pendingIntent);

        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
