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
import com.charles.mileagetracker.app.processingservices.AddressDistanceServices;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;

import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class TripPostProcess extends IntentService implements TripGroupProcessor.GroupProcessorInterface {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_PROCESS_GROUP = "com.charles.mileagetracker.app.services.intentservices.action.proccess_group";
    private static final String GROUP_PARAM = "com.charles.mileagetracker.app.services.intentservices.extra.PARAM1";

    private AddressDistanceServices addressDistanceServices = null;
    private Context context;

    private TripGroup group = null;
    private final String question = "Were all stops business related?";
    private final String complete = "Trip Complete";


    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
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
        if (group_id != -1) {
            group = TripGroup.findById(TripGroup.class, new Long(group_id));
            if (group != null) {
                TripGroupProcessor processor = new TripGroupProcessor(getApplicationContext(), this);
                processor.processTripGroup(group);
            }
        }
    }

    @Override
    public void finishedGroupProcessing(List<TripRow> rows) {
        if (rows != null) {
            //group = rows.get(0).tgroup;
            generateNotification(complete, question, group.getId().intValue());
        }

    }

    @Override
    public void unableToProcessGroup(int failCode) {
        switch (failCode) {
            case TripGroupProcessor.CONNECT_FAILED:
                generateNotification(complete, question, group.getId().intValue());
                break;
            case TripGroupProcessor.INVALID_GROUP:
                break;
            case TripGroupProcessor.UNKOWN_FAILURE:
                generateNotification(complete, question, group.getId().intValue());
                break;
            default:
                generateNotification(complete, question, group.getId().intValue());
                break;
        }

    }

    private void generateNotification(String title, String message, int groupId) {

        Intent pathIntent = new Intent(context, MapDrawerActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MapDrawerActivity.class);
        stackBuilder.addNextIntent(pathIntent);

        PendingIntent selectPathSegments = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        //Intent to record all trip segments
        Intent saveTripService = new Intent(context, SaveBusinessRelated.class);
        saveTripService.putExtra("tgroup", groupId);
        PendingIntent saveTrip = PendingIntent.getService(context, 0, saveTripService,PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(selectPathSegments)
                .addAction(R.drawable.ic_action_accept, "Yes", saveTrip)
                .addAction(R.drawable.ic_action_cancel, "No", selectPathSegments);

        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
