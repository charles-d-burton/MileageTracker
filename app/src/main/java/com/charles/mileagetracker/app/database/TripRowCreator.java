package com.charles.mileagetracker.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.webapicalls.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

        //Address addy = checkLocation(new LatLng(lat, lon)).get(0);//TODO: Fix this to prevent NPE
        String addy = checkLocation(new LatLng(lat, lon));
        Log.v("DEBUG: ", "Current Location Address is: " + addy);
        if (addy != null) {
            values.put(TripTable.ADDRESS, addy);
        }

        values.put(TripTable.TIME, System.currentTimeMillis());

        context.getContentResolver().insert(TrackerContentProvider.TRIP_URI, values);
    }

    //Close the group.  This closes the group and seals a trip.

    public boolean closeGroup(int id, double lat, double lon) {
        int lastTripGroupId = lastTripOpen();

        if (lastTripGroupId == -1) {
            return false;
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
        new GenerateDistances().doInBackground(lastTripGroupId);

        return true;
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

        @Override
        protected String doInBackground(Integer... params) {
            int groupId = params[0];
            String projection[] = {TripTable.DISTANCE,
                                   TripTable.LAT,
                                   TripTable.LON,
                                   TripTable.TRIP_KEY,
                                   TripTable.COLUMN_ID};
            Cursor c = context.getContentResolver().query(TrackerContentProvider.TRIP_URI, projection, TripTable.TRIP_KEY + "=" + Integer.toString(groupId), null, null);

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                if (c.isFirst()) continue;

            }
            return null;
        }
    }
}
