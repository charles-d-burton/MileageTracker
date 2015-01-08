package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class CalcMileageService extends IntentService {

    private final String CLASS_NAME = ((Object)this).getClass().getName();
    private AddressDistanceServices locationServices = null;

    public CalcMileageService() {
        super("CalcMileageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Context context = getApplicationContext();
            locationServices = new AddressDistanceServices(context);
            iterateGroups();
        }
    }

    private void iterateGroups() {
        List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
        Iterator<TripGroup> it = groups.iterator();
        while (it.hasNext()) {
            TripGroup group = it.next();
            processGroup(group);
        }
    }

    //Process a tgroup, find rows based on their relationship with the TripGroup.  Then iterate through the
    //trips.
    private void processGroup(TripGroup group) {
        AddressDistanceServices addressDistanceServices = new AddressDistanceServices(getApplicationContext());

        String params[] = {Long.toString(group.getId())};
        List<TripRow> stops = TripRow.find(TripRow.class, " tgroup = ? ", params ,null , " ORDER BY id ASC",null);

        double totalMileage = 0;
        double billableMileage = 0;

        //Empty or unresolved Trip Set, remove it.
        if (stops.isEmpty() || stops.size() == 1) {

            Log.v(CLASS_NAME, "Empty Trip Set");
            for (TripRow stop: stops) {
                stop.delete();
            }
            group.delete();

        } else {
            Iterator<TripRow> it = stops.iterator();
            TripRow stop = it.next();
            while (it.hasNext()) {
                TripRow nextStop = it.next();
                if (nextStop.distance != 0) { //Skip Rows that have been computed
                    totalMileage = totalMileage + nextStop.distance;
                    if (nextStop.businessRelated) {
                        billableMileage = billableMileage + nextStop.distance;
                    }
                    stop = nextStop;//Re-assigns stop.
                    continue;
                } else {
                    double distance = locationServices.getDistance(stop.lat, stop.lon, nextStop.lat, nextStop.lon);
                    nextStop.distance = distance;

                    totalMileage = totalMileage + distance;
                    if (nextStop.businessRelated) {
                        billableMileage = billableMileage + distance;
                    }

                    try {
                        nextStop.address = addressDistanceServices.getAddressFromLatLng(new LatLng(nextStop.lat, nextStop.lon));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    nextStop.save();
                    //updateAddress(nextStop);
                    stop = nextStop;
                }
            }
        }

        if (totalMileage != 0 && billableMileage != 0) {
            group.billableMileage = billableMileage;
            group.totalMileage = totalMileage;
            group.save();
        }
    }

    /*
    I need to think about this.  It lets me update the addresses in a background thread, but there's no
    control for it.  I need to create a bounded queue if I want to use this.

    private void updateAddress(TripRow row) {
        if (row.address == null || row.address.equals("") || row.address.equalsIgnoreCase("NULL")) {
            new AddressDistanceServices(getApplicationContext()).setAddress(row);
        }
    }*/
}
