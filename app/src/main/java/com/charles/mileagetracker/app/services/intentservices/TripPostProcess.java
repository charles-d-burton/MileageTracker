package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.activities.MapDrawerActivity;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processors.AddressDistanceServices;
import com.charles.mileagetracker.app.processors.ConnectivityCheck;
import com.charles.mileagetracker.app.processors.TripGroupProcessor;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class TripPostProcess extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_PROCESS_GROUP = "com.charles.mileagetracker.app.services.intentservices.action.proccess_group";
    private static final String GROUP_PARAM = "com.charles.mileagetracker.app.services.intentservices.extra.PARAM1";

    //private AddressDistanceServices addressDistanceServices = null;
    private Context context;

    private TripGroup group = null;
    private final String question = "Were all stops business related?";
    private final String complete = "Trip Complete";


    /**
     *
     * @see IntentService
     */
    public static void processGroup(Context context, Integer group_id) {
        Intent intent = new Intent(context, TripPostProcess.class);
        intent.setAction(ACTION_PROCESS_GROUP);
        intent.putExtra(GROUP_PARAM, group_id);
        context.startService(intent);
    }

    public TripPostProcess() {
        super("TripPostProcess");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        this.context = getApplicationContext();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_GROUP.equals(action)) {
                final Integer group_id = intent.getIntExtra(GROUP_PARAM, -1);
                handleActionProcessGroup(group_id);
            }
        }
    }

    /**
     * Handle action in the provided background thread with the provided
     * parameters.
     */
    private void handleActionProcessGroup(Integer group_id) {
        //Verify that we have a validate group and access to the internet with data
        final Context context = getApplicationContext();
        if (group_id != -1) {
            group = TripGroup.findById(TripGroup.class, new Long(group_id));
            if (group != null) {
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        String entries[] = {Long.toString(group.getId())};
                        List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
                        if (rows == null) {
                            group.delete();
                        } else if (rows.size() == 1) {
                            group.delete();
                        } else if (rows.size() == 2) {//Only two stops, need to see how close they are
                            AddressDistanceServices addressDistanceServices1 = new AddressDistanceServices(context);
                            TripRow row1 = rows.get(0);
                            TripRow row2 = rows.get(1);
                            double distance = addressDistanceServices1.getStraigtLineDistance(row1.lat, row1.lon, row2.lat, row2.lon);
                            if (distance > 1500) {
                                generateNotification(complete, question, group.getId().intValue());
                            } else {
                                row1.delete();
                                row2.delete();
                                group.delete();
                            }
                        } else {
                            generateNotification(complete, question, group.getId().intValue());
                        }
                    }
                });
            }
        }
    }


    private void generateNotification(String title, String message, int groupId) {

        Intent pathIntent = new Intent(context, MapDrawerActivity.class);
        pathIntent.putExtra("tgroup", groupId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MapDrawerActivity.class);
        stackBuilder.addNextIntent(pathIntent);

        PendingIntent selectPathSegments = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        //Intent to record all trip segments
        Intent saveTripService = new Intent(context, SaveBusinessRelated.class);
        saveTripService.putExtra("tgroup", groupId);
        PendingIntent saveTrip = PendingIntent.getService(context, 0, saveTripService,PendingIntent.FLAG_ONE_SHOT);

        PendingIntent doNothing = PendingIntent.getService(context, 0, new Intent(), PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(selectPathSegments)
                .addAction(R.drawable.ic_action_accept, "Yes", saveTrip)
                .addAction(R.drawable.ic_action_accept, "Select", selectPathSegments)
                .addAction(R.drawable.ic_action_cancel, "No", doNothing );


        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());

    }
}
