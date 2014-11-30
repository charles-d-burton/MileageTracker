package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;

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
            /*HashMap<Integer, Double> mileageUpdates = getUpdates(context);
            if (mileageUpdates.size() > 0) {
                updateTripSegments(context, mileageUpdates);
            }*/
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

    //Process a group, find rows based on their relationship with the TripGroup.  Then iterate through the
    //trips.
    private void processGroup(TripGroup group) {
        List<TripRow> stops = TripRow.find(TripRow.class, " trip_group = ? ", Long.toString(group.getId()), " ORDER BY id ASC");
        if (stops.isEmpty() || stops.size() == 1) {

            Log.v(CLASS_NAME, "Empty Trip Set");
            group.delete();

        } else {
            Iterator<TripRow> it = stops.iterator();
            TripRow stop = it.next();
            while (it.hasNext()) {
                TripRow nextStop = it.next();
                if (stop.distance != 0) { //Skip Rows that have been computed
                    stop = nextStop;//Re-assigns stop.
                    continue;
                } else {
                    double distance = locationServices.getDistance(stop.lat, stop.lon, nextStop.lat, nextStop.lon);
                    nextStop.distance = distance;
                    stop = nextStop;
                }
            }
        }
    }

    private void updateAddress(TripRow row) {
        if (row.address == null || row.address.equals("") || row.address.equalsIgnoreCase("NULL")) {
            new AddressDistanceServices(getApplicationContext()).setAddress(row);
        }
    }


    /*
    //Get all the trip groups, then run the method to test the distance in each of those groups
    private HashMap<Integer, Double> getUpdates(Context context) {
        String projection[] = {
                TripGroup.GROUP_ID
        };

        ArrayList<Integer> groups = new ArrayList<Integer>();

        Cursor c = context.getContentResolver().query(TrackerContentProvider.GROUP_URI, projection, null, null, null);
        if (c != null) {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                int group_id = c.getInt(c.getColumnIndexOrThrow(TripGroup.GROUP_ID));
                groups.add(group_id);
            }
        }
        if (c != null) c.close();
        HashMap<Integer, Double> updates = getMileageUpdatesMap(context, groups);
        return updates;
    }

    //Take the context and all the group id's.  Use them to figure out if distances have been calculated for trips
    //Doing this on Wifi to make it faster.
    private HashMap<Integer, Double> getMileageUpdatesMap(Context context, ArrayList<Integer> groups) {
        AddressDistanceServices locationServices = new AddressDistanceServices(context);
        HashMap<Integer, Double> updates = new HashMap<Integer, Double>();
        String projection[] = {
                TripTable.DISTANCE,
                TripTable.COLUMN_ID,
                TripTable.LAT,
                TripTable.LON
        };

        Iterator it = groups.iterator();

        while (it.hasNext() ) {
            double lastLat = 0;
            double lastLon = 0;
            int group_id = (Integer)it.next();
            Cursor c = context.getContentResolver().query(TrackerContentProvider.TRIP_URI, projection, TripTable.TRIP_KEY + "=" + group_id, null, null);
            if (c != null) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (c.getCount() == 1){
                        cleanupSingleEntry(group_id);
                        break;
                    }
                    int id = c.getInt(c.getColumnIndexOrThrow(TripTable.COLUMN_ID));
                    double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));
                    Log.v("CALCULATED DISTANCE: ", Double.toString(distance));
                    if (c.isFirst()) {
                        lastLat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                        lastLon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
                    } else if (distance == 0 || distance == -1) {
                        double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                        double lon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
                        distance = locationServices.getDistance(lastLat, lastLon, lat, lon);
                        if (distance != -1 && distance < 1 ) {
                            getContentResolver().delete(TrackerContentProvider.TRIP_URI, TripTable.COLUMN_ID + "=" + id,null);
                        } else {
                            updates.put(id, distance);
                        }
                    }
                }
            }
            if (c != null) c.close();
        }
        return updates;
    }

    private void cleanupSingleEntry(int groupId) {
        getContentResolver().delete(TrackerContentProvider.GROUP_URI, TripGroup.GROUP_ID + "=" + groupId, null);
    }

    private void updateTripSegments(Context context, HashMap<Integer, Double> updates) {
        Iterator it = updates.keySet().iterator();
        ContentValues values = new ContentValues();
        while (it.hasNext()) {
            int id = (Integer)it.next();
            double distance = updates.get(id);
            Log.v("UPDATING DB: ", Double.toString(distance));
            values.put(TripTable.DISTANCE, distance);
            context.getContentResolver().update(TrackerContentProvider.TRIP_URI,values,TripTable.COLUMN_ID + "=" + id,null);
        }
    }*/
}
