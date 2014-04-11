package com.charles.mileagetracker.app.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize c lass - update intent actions, extra parameters and static
 * helper methods.
 * This class is intended to learn location information and store it when you define a new home.
 */
public class LearnLocationService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_GET_TOWERS = "com.charles.mileagetracker.app.services.action.GET_TOWERS";
    private static final String ACTION_GET_WIFI = "com.charles.mileagetracker.app.services.action.GET_WIFI";
    private static final String ACTION_GET_BLUE = "com.charles.mileagetracker.app.services.action.GET_BLUE";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.charles.mileagetracker.app.services.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.charles.mileagetracker.app.services.extra.PARAM2";

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     * Get all the cell tower data that it can
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startListeningForTowers(Context context, String param1, String param2) {
        Intent intent = new Intent(context, LearnLocationService.class);
        intent.setAction(ACTION_GET_TOWERS);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     * Retrieve all the wifi data that it can.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void getAllWifi(Context context, String param1, String param2) {
        Intent intent = new Intent(context, LearnLocationService.class);
        intent.setAction(ACTION_GET_WIFI);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void getAllBlueTooth(Context context, String param1, String param2) {

    }

    public LearnLocationService() {
        super("LearnLocationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_GET_TOWERS.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_GET_WIFI.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
