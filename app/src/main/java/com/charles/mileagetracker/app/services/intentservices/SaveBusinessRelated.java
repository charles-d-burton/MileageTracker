package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
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
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            final int groupId = bundle.getInt("tgroup");
            if (groupId != -1) {
                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        markAllAsBusiness(groupId);
                    }
                });
            }
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
