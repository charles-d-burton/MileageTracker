package com.charles.mileagetracker.app.database;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.webapicalls.LocationServices;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by charles on 6/14/14.
 */
public class TripRowCreator {
    private Context context;

    public TripRowCreator(Context context) {
        if (context == null) {
            throw new NullPointerException();
        }
        this.context = context;
    }

    /*
    Get the currently open trip.
     */
    public void recordSegment(int id, double lat, double lon) {
        Log.v("DEBUG: ", "Recording path segment");
        int group_id = -1;
        int lastTripGroupId = lastTripOpen();
        ContentValues values = new ContentValues();

        if (lastTripGroupId != -1) {
            group_id = lastTripGroupId;
        } else {

            values.put(TripGroup.GROUP_CLOSED, 0);

            Uri uri = context.getContentResolver().insert(TrackerContentProvider.GROUP_URI, values);
            group_id = Integer.parseInt(uri.getLastPathSegment());
            values.clear();
        }


        values.put(TripTable.TRIP_KEY, group_id);
        values.put(TripTable.CLOSED, 0);
        values.put(TripTable.LAT, lat);
        values.put(TripTable.LON, lon);
        values.put(TripTable.FENCE_RELATION, id);

        String addy = checkLocation(new LatLng(lat, lon));
        Log.v("DEBUG: ", "Current Location Address is: " + addy);
        if (addy != null) {
            values.put(TripTable.ADDRESS, addy);
        }

        values.put(TripTable.TIME, System.currentTimeMillis());

        context.getContentResolver().insert(TrackerContentProvider.TRIP_URI, values);
    }

    /*Close the group.  This closes the group and seals a trip by getting the last groupId
    and then updating it as closed.  This has a cascade effect on all the associated trip points
    */

    public int closeGroup(int id, double lat, double lon) {
        int lastTripGroupId = lastTripOpen();

        if (lastTripGroupId == -1) {
            return -1;
        }

        recordSegment(id, lat, lon);//Record our end point

        Uri uri = TrackerContentProvider.GROUP_URI;
        String[] projection = {
                TripGroup.GROUP_CLOSED,
                TripGroup.GROUP_ID
        };
        ContentValues values = new ContentValues();
        values.put(TripGroup.GROUP_CLOSED, 1);

        context.getContentResolver().update(uri, values, TripGroup.GROUP_ID + "=" + lastTripGroupId, null);
        new GenerateDistances().execute(lastTripGroupId);

        return lastTripGroupId;
    }



    /*
    Query the database to find out of the last trip was open or closed, return the id of the group if it's
    open otherwise return -1
     */

    private int lastTripOpen() {

        Uri uri = TrackerContentProvider.GROUP_URI;
        String[] projection = {
                TripGroup.GROUP_CLOSED,
                TripGroup.GROUP_ID
        };

        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
        if (!(c == null) && !(c.getCount() < 1)) {
            c.moveToLast(); //Go to the last entry, all previous should be closed
            int group_id = c.getInt(c.getColumnIndexOrThrow(TripGroup.GROUP_ID));
            int closed = c.getInt((c.getColumnIndexOrThrow(TripGroup.GROUP_CLOSED)));
            if (closed != 1) {//Means the last trip was not closed
                return group_id;//Return the id of the current group
            }

        }
        c.close();
        return -1;//Known impossible value
    }

    private String checkLocation(LatLng location) {
        LocationServices locationServices = new LocationServices(context);
        return locationServices.getRoadName(location.latitude, location.longitude);
    }

    private class GenerateDistances extends AsyncTask<Integer, Integer, String> {

        /*
        On a background thread I'm getting a cursor and pulling all rows associated with a specific group.
        Once I have that I define four double that represent the locations that I need the distance between.
        I iterate through the rows getting the lat/long of where the stop was taken then processing the distance
        between the previous one and the current one.  It then resets the variables when finished making the current
        one the previous.  It also handles cleaning up false positive trips, meaning a "trip" where the fence was triggered,
        but you didn't actually go anywhere because you're right on the edge of the fence.  So your distance is effectively zero
        and you didn't actually travel anywhere.
         */
        @Override
        protected String doInBackground(Integer... params) {
            int groupId = params[0];
            String projection[] = {TripTable.DISTANCE,
                                   TripTable.LAT,
                                   TripTable.LON,
                                   TripTable.TRIP_KEY,
                                   TripTable.COLUMN_ID};
            Cursor c = context.getContentResolver().query(TrackerContentProvider.TRIP_URI, projection, TripTable.TRIP_KEY + "=" + groupId, null, null);


            double startLat = 0;
            double startLon = 0;
            double endLat = 0;
            double endLon = 0;
            LocationServices locationServices = new LocationServices(context);
            if (c != null && c.getCount() > 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    int id = c.getInt(c.getColumnIndexOrThrow(TripTable.COLUMN_ID));
                    if (c.isFirst()) {
                        startLat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                        startLon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
                    } else {
                        endLat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                        endLon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
                        double distance = locationServices.getDistance(startLat, startLon, endLat, endLon);
                        if (c.getCount() == 2 && (distance * 0.621) < 1) { //Started and ended in the same place with no stops.
                            context.getContentResolver().delete(TrackerContentProvider.TRIP_URI, TripTable.TRIP_KEY + "=" +Integer.toString(groupId),null);
                            NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
                            //notificationManager.cancel(0);
                            break;//Get out of the loop
                        } else {
                            updateRow(id, distance, context);
                            startLat = endLat;
                            startLon = endLon;
                        }
                    }
                }
            }
            if (c != null) c.close();

            return null;
        }

        private void updateRow(int rowId, double distance, Context context) {
            ContentValues values = new ContentValues();
            values.put(TripTable.DISTANCE, distance);

            ContentResolver resolver = context.getContentResolver();

            resolver.update(TrackerContentProvider.TRIP_URI, values, TripTable.COLUMN_ID + "=" + rowId, null);
        }
    }
}
