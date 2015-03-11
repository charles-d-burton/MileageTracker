package com.charles.mileagetracker.app.processingservices;

import android.content.Context;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by charles on 1/19/15.
 * This class acts as a generic processor for handling trip groups.  It handles updating all of
 * the rows with the correct information.  As well as deleteing erroneous entries or entire
 * groups if they are not valid.  Invalid entries groups of stops that have the same start and end point
 * with no stops in between.
 */
public class TripGroupProcessor {
    private TripGroup group;
    private Context context;
    private AddressDistanceServices addressDistanceServices = null;
    private GroupProcessorInterface callback;

    public static final int CONNECT_FAILED = 9000;
    public static final int INVALID_GROUP = 9001;
    public static final int UNKOWN_FAILURE = 9002;

    public TripGroupProcessor(Context context, GroupProcessorInterface callback) {
        this.context = context;
        this.callback = callback;
    }

    public void processTripGroup(TripGroup group) {

        this.group = group;
        if (group != null && !group.processed && context != null  && callback != null) {
            addressDistanceServices = new AddressDistanceServices(context);
            String entries[] = {Long.toString(group.getId())};
            //Get the full list, check each stop to make sure it's not too close to a HomePoint
            List<TripRow> rowsList = processHomePoints(TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null));
            processTripGroup(rowsList);
        } else if (group != null && group.processed) {
            addressDistanceServices = new AddressDistanceServices(context);
            String entries[] = {Long.toString(group.getId())};
            //Get the full list, check each stop to make sure it's not too close to a HomePoint
            List<TripRow> rowsList = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
            updateData(rowsList);
            callback.finishedGroupProcessing(null);
        } else if (context !=null) {
            callback.unableToProcessGroup(CONNECT_FAILED);
        } else {
            callback.unableToProcessGroup(UNKOWN_FAILURE);
        }
    }

    public void processTripGroup(List<TripRow> rowsList) {
        TripGroup group = null;
        addressDistanceServices = new AddressDistanceServices(context);
        if (rowsList == null || rowsList.size() == 0) { //Null list of rows something went weird
            callback.unableToProcessGroup(INVALID_GROUP);
        } else if (rowsList.size() == 1) {//Only one or no stops, remove it all as invalid data
            group = rowsList.get(0).tgroup;
            if (group.group_closed) {
                rowsList.get(0).delete();
                group.delete();
                callback.unableToProcessGroup(INVALID_GROUP);
            }

        } else if (rowsList.size() == 2) {//Check if the start and and end are the same with no stops between
            group = rowsList.get(0).tgroup;
            if (group.group_closed) {
                boolean isSameStop = sameStop(rowsList.get(0), rowsList.get(1));
                if (isSameStop) {
                    rowsList.get(0).delete();
                    rowsList.get(1).delete();
                    group.delete();
                    callback.unableToProcessGroup(INVALID_GROUP);
                } else {
                    updateData(rowsList);
                }
            } else {
                updateData(rowsList);
            }

        } else {
            updateData(rowsList);
        }
    }




    //Work through the trip stops, if any of them are too close to HomePoints we want to remove it.
    private List<TripRow> processHomePoints(List<TripRow> rows) {
        List homes = HomePoints.listAll(HomePoints.class);
        if (rows != null) {
            LinkedList<TripRow> linkedRows = new LinkedList<TripRow>();
            linkedRows.addAll(rows);
            //Remove the first and the last, they're the geofence start points and LatLng will be same as HomePoints
            linkedRows.removeFirst();
            linkedRows.removeLast();

            Iterator<TripRow> rowIterator = linkedRows.iterator();
            while (rowIterator.hasNext()) {
                TripRow row  = rowIterator.next();
                if (tooCloseToHome(row, homes)) {
                    rows.remove(row);//Remove a row from the list and delete it
                    row.delete();
                }
            }
        }
        return rows;
    }

    private boolean tooCloseToHome(TripRow row, List<HomePoints> homes) {
        Iterator<HomePoints> it = homes.iterator();
        while (it.hasNext()) {
            HomePoints home = it.next();
            double distance = addressDistanceServices.getStraigtLineDistance(row.lat, row.lon, home.lat, home.lon);
            if (distance < 1000) {
                return true;
            }
        }
        return false;
    }

    private boolean sameStop(TripRow stop1, TripRow stop2) {
        double distance = addressDistanceServices.getStraigtLineDistance(stop1.lat, stop1.lon, stop2.lat, stop2.lon);
        if (distance < 1000) { //Within 1km of eachother
            return true;
        }
        return false;
    }

    /*
    Process the TripRows here.  It'll update the address as well as write in the encoded polyline.
     */
    private void updateData(List<TripRow> rows) {
        try {
            Iterator<TripRow> rowsIterator = rows.iterator();
            TripRow lastRow = rowsIterator.next();//Skip the first, it's a start point and doesn't have distance data
            addressDistanceServices.setAddress(lastRow);
            lastRow.save();

            while (rowsIterator.hasNext()) {
                TripRow nextRow = rowsIterator.next();
                JSONObject json = addressDistanceServices.getTripJson(new LatLng(lastRow.lat, lastRow.lon), new LatLng(nextRow.lat, nextRow.lon));
                nextRow.distance = addressDistanceServices.getDistance(json);
                nextRow.points = addressDistanceServices.getEncodedPoly(json);
                //Log.v("TRIPGROUPPROCESSOR: ", "POINTS: " + nextRow.points);
                nextRow.address = addressDistanceServices.getEndAddressFromJson(json);
                nextRow.save();
                lastRow = nextRow;
            }
            if (lastRow.tgroup.group_closed) {
                lastRow.tgroup.processed = true;
            }

            lastRow.tgroup.save();
        } catch (JSONException je) {
            callback.unableToProcessGroup(UNKOWN_FAILURE);
        }

        callback.finishedGroupProcessing(rows);
    }

    public interface GroupProcessorInterface {
        public void finishedGroupProcessing(List<TripRow> rows);
        public void unableToProcessGroup(int failCode);
    }
}
