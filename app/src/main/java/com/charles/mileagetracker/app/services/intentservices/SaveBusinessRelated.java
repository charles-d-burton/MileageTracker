package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;

import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class SaveBusinessRelated extends IntentService {


    public SaveBusinessRelated() {
        super("SaveBusinessRelated");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            int groupId = bundle.getInt("group");
            if (groupId != -1) {
                markAllAsBusiness(groupId);
            }

        }
    }

    private void markAllAsBusiness(int group) {
        ContentValues values = new ContentValues();
        values.put(TripTable.BUSINESS_RELATED, 1);
        getContentResolver().update(TrackerContentProvider.TRIP_URI, values, TripTable.TRIP_KEY + "=" + group, null);
    }

}
