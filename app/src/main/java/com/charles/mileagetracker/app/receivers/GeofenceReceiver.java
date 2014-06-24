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
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripRowCreator;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.services.ActivityRecognitionService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.io.IOException;

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

            try {
                initializeVars(intent.getIntExtra("id", -1), intent.getDoubleExtra("lat", -1), intent.getDoubleExtra("lon", -1), Geofence.GEOFENCE_TRANSITION_EXIT);
            } catch (IOException e) {
                e.printStackTrace();
                return;

            }

            message = "Leaving Fence, starting record";
            Intent activityRecognitionService = new Intent(this.context, ActivityRecognitionService.class);
            activityRecognitionService.putExtra("id", intent.getIntExtra("id", -1));
            activityRecognitionService.putExtra("lat", intent.getDoubleExtra("lat", -1));
            activityRecognitionService.putExtra("lon", intent.getDoubleExtra("lon", -1));
            activityRecognitionService.putExtra("transition", "exit");


            this.context.startService(activityRecognitionService);
            generateNotification("Recording Trip",message, Geofence.GEOFENCE_TRANSITION_EXIT);

        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            message = "Entering fence";
            //boolean running = isServiceRunning();
            Log.v("DEBUG: ", "Killing Running Record Service");
            int id = intent.getIntExtra("id", -1);
            double lat = intent.getDoubleExtra("lat", -1);
            double lon = intent.getDoubleExtra("lon", -1);

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
    }

    private void initializeVars(int id, double lat, double lon, int transition) throws IOException {
        AccessInternalStorage accessInternalStorage = new AccessInternalStorage();
        TripVars vars = new TripVars();
        vars.setId(id);
        vars.setLat(lat);
        vars.setLon(lon);
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

    private String getAddresses() {

        String projection[] = {TripTable.COLUMN_ID, TripTable.ADDRESS};

        Cursor c = context.getContentResolver().query(TrackerContentProvider.TRIP_URI, projection, null, null, null);
        String addresses = "";
        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                addresses = addresses + "\n" + c.getString(c.getColumnIndexOrThrow(TripTable.ADDRESS));
            }
        }
        return addresses;
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
