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
import com.charles.mileagetracker.app.activities.MainActivity;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.charles.mileagetracker.app.services.RecordTrackService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class GeofenceReceiver extends BroadcastReceiver {

    private Context context = null;
    public GeofenceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        int transitionType = LocationClient.getGeofenceTransition(intent);

        String message = "";


        if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            message = "Leaving Fence";
            Intent recordTrackService = new Intent(context, ActivityRecognitionService.class);
            recordTrackService.putExtra("id", intent.getIntExtra("id", -1));
            context.startService(recordTrackService);
            generateNotification(message);
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            message = "Entering fence";
            ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (RecordTrackService.class.getName().equals(service.service.getClassName())) {
                    Log.v("DEBUG: ", "Killing Running Record Service");
                    generateNotification("Killing Running Record Service");
                    context.stopService(new Intent(context, ActivityRecognitionService.class));
                    //context.stopService(new Intent(context, RecordTrackService.class));
                }
            }
        }

        //Log.v("Fenced: ", "Running geofence code");
    }

    private void generateNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Test")
                .setContentText(message);
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
