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
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.activities.PathSelectorActivity;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.charles.mileagetracker.app.services.RecordTrackService;
import com.charles.mileagetracker.app.services.intentservices.ActivityRecognitionIntentService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.model.LatLng;

public class GeofenceReceiver extends BroadcastReceiver {

    private Context context = null;

    public GeofenceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context.getApplicationContext();
        int transitionType = LocationClient.getGeofenceTransition(intent);

        String message = "";


        if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            message = "Leaving Fence, starting record";
            boolean running = isServiceRunning();
            if (!running){
                Intent activityRecognitionService = new Intent(this.context, ActivityRecognitionService.class);
                activityRecognitionService.putExtra("id", intent.getIntExtra("id", -1));
                activityRecognitionService.putExtra("lat", intent.getDoubleExtra("lat", -1));
                activityRecognitionService.putExtra("lon", intent.getDoubleExtra("lon", -1));
                activityRecognitionService.putExtra("transition", "exit");

                this.context.startService(activityRecognitionService);
                generateNotification(message, Geofence.GEOFENCE_TRANSITION_EXIT);
            }

        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            message = "Entering fence";
            boolean running = isServiceRunning();
            if (running) {
                Log.v("DEBUG: ", "Killing Running Record Service");

                this.context.stopService(new Intent(context, ActivityRecognitionService.class));


                //context.stopService(new Intent(context, ActivityRecognitionIntentService.class)):
            }
            generateNotification(getAddresses(), Geofence.GEOFENCE_TRANSITION_ENTER);


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

    private String getAddresses() {

        String projection[] = {PendingSegmentTable.COLUMN_ID, PendingSegmentTable.END_ADDRESS};

        Cursor c = context.getContentResolver().query(TrackerContentProvider.PENDING_URI, projection, null, null, null);
        String addresses = "";
        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                addresses = addresses + "\n" + c.getString(c.getColumnIndexOrThrow(PendingSegmentTable.END_ADDRESS));
            }
        }
        return addresses;
    }


    private void generateNotification(String message, int geoFenceMode) {

        Intent pathIntent = new Intent(context, PathSelectorActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(PathSelectorActivity.class);
        stackBuilder.addNextIntent(pathIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Test")
                .setContentText(message)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
