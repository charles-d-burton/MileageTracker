package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.charles.mileagetracker.app.database.orm.TripRow;

import java.util.List;


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
            int groupId = bundle.getInt("tgroup");
            if (groupId != -1) {
                markAllAsBusiness(groupId);
            }

        }
    }

    private void markAllAsBusiness(int group) {
        List<TripRow>  tripRows = TripRow.find(TripRow.class, " trip_group = ? ", Integer.toString(group));
        for (TripRow tripRow : tripRows) {
            tripRow.businessRelated = true;
            tripRow.save();
        }
    }

}
