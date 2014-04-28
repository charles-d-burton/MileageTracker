package com.charles.mileagetracker.app.receivers;

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
import com.charles.mileagetracker.app.services.LearnLocationIntentService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class GeofenceReceiver extends BroadcastReceiver {
    public GeofenceReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int transitionType = LocationClient.getGeofenceTransition(intent);
        String message = "WTF?";
        if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            message = "Leaving Fence";
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            message = "Entering fence";
        }
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

        Intent locationServerIntent = new Intent(context, LearnLocationIntentService.class);
        locationServerIntent.putExtra("id", intent.getIntExtra("id", -1));
        context.startService(locationServerIntent);
        Log.v("Fenced: ", "Running geofence code");
    }
}
