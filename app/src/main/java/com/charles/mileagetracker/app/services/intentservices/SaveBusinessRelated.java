package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processors.AddressDistanceServices;

import java.util.Iterator;
import java.util.concurrent.Executors;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class SaveBusinessRelated extends IntentService {


    public SaveBusinessRelated() {
        super("SaveBusinessRelated");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(TripPostProcess.TRIP_COMPLETE_NOTIFICATION_ID);
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            int groupId = bundle.getInt("tgroup");
            if (groupId != -1) {

                Executors.newSingleThreadExecutor().execute(new MarkBusinessRelated(groupId));
                //markAllAsBusiness(groupId);
            }
        }
    }

    private class MarkBusinessRelated implements Runnable {
        private int group_id = 0;

        public MarkBusinessRelated(int group_id) {
            this.group_id = group_id;
        }
        @Override
        public void run() {
            markAllAsBusiness(group_id);
        }
    }

    //Mark all stops as business related and calculate the total/billable mileage for each stop
    private void markAllAsBusiness(int group) {
        AddressDistanceServices addressDistanceServices = new AddressDistanceServices(getApplicationContext());
        //List<TripRow>  tripRows = TripRow.find(TripRow.class, " tgroup = ? ", Integer.toString(group));
        Iterator<TripRow> rows = TripRow.findAsIterator(TripRow.class, " tgroup = ? ", Integer.toString(group));
        TripRow lastRow = rows.next();
        lastRow.businessRelated = true;
        lastRow.save();
        double totalDistance = 0.0;
        while (rows.hasNext()) {
            TripRow nextRow = rows.next();
            double distance = addressDistanceServices.getDistance(lastRow.lat, lastRow.lon, nextRow.lat, nextRow.lon);
            nextRow.distance = distance;
            totalDistance = totalDistance + distance;
            nextRow.businessRelated = true;
            nextRow.save();
            lastRow = nextRow;
        }
        TripGroup tripGroup = lastRow.tgroup;
        tripGroup.totalMileage = totalDistance;
        tripGroup.billableMileage = totalDistance;
    }
}
