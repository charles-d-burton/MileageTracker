package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;


/**
 * Silly boilerplate mostly to cancel a Notification
 * <p/>
 */
public class NotificationCancelService extends IntentService {


    public NotificationCancelService() {
        super("NotificationCancelService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final int notificationId = intent.getIntExtra("nId", -1);
            if (notificationId != -1) {
                handleActionFoo(notificationId);
            }
        }
    }

    /**
     * Handle action to cancel the notification
     */
    private void handleActionFoo(int notification_id) {
        NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notification_id);
    }
}
