package com.charles.mileagetracker.app.maphandlers;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processingservices.AddressDistanceServices;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by charles on 12/15/14.
 */
public class TripHandler  extends MapActivityHandler implements
        TripGroupProcessor.GroupProcessorInterface{

    private GoogleMap map = null;
    private Context context = null;

    private List<TripRow> rows = null;

    public final String HANDLER_TAG = ((Object)this).getClass().getName();

    public TripHandler(Context context, GoogleMap map) {
        super(context, map);
    }


    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        boolean matchfound = false;
        for (TripRow row: rows) {
            if (row.marker != null && row.marker.equals(marker)) {
                row.lat = marker.getPosition().latitude;
                row.lon = marker.getPosition().longitude;
                row.save();

                //I have to get the first object, they're not reloaded dynamically
                TripGroup group = rows.get(0).tgroup;
                group.processed = false;
                group.save();

                new DrawLines().execute(rows);

                matchfound = true;
            }
        }
        if (!matchfound) {
            Log.v("Match FOUND: ", "No Match Found");
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();

    }

    @Override
    public void connect() {
        super.connect();
    }

    @Override
    public void setTripData(List<TripRow> rows) {
        this.rows = rows;
        new DrawLines().execute(rows);
    }

    @Override
    public String getTag() {
        return HANDLER_TAG;
    }

    @Override
    public void finishedGroupProcessing(List<TripRow> rows) {

    }

    @Override
    public void unableToProcessGroup(int failCode) {

    }

    /*
    This class takes a TripGroup and generates the PolyLine points in a background thread.  It then
    puts them on the map after it generates them.
     */
    private class DrawLines extends AsyncTask<List<TripRow>, Integer, List<TripRow>> implements
        TripGroupProcessor.GroupProcessorInterface {

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Log.v("MATCH FOUND: ", "RUNNING BACKGROUND THREAD");
            distanceServices = new AddressDistanceServices(context);
            map.clear();
        }

        @Override
        protected List<TripRow> doInBackground(List... params) {

            List<TripRow> rows = params[0];

            TripRow topRow = rows.get(0);
            TripGroup group = topRow.tgroup;

            if (!group.processed) {
                TripGroupProcessor processor = new TripGroupProcessor(context, this);
                processor.processTripGroup(rows);
                Log.v("MATCH FOUND: ", "Processing Rows");
            }

            Iterator<TripRow> it = rows.iterator();
            while (it.hasNext()) {
                TripRow row = it.next();
                if (row.points != null) {
                    Log.v("POINTS FOUND: ", row.points);
                    row.polyPoints = distanceServices.decodePoly(row.points);
                }
            }

            return rows;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }


        /*
        Take the tgroup, pull out all of the children and then process all of the points that they posses
        and draw them as a polyline on the screen.
         */
        @Override
        protected void onPostExecute(List<TripRow> rows) {
            if (rows == null) return;

            super.onPostExecute(rows);
            for (int i = 0; i < rows.size(); i++) {
                TripRow row = rows.get(i);

                Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(row.lat, row.lon))
                            .draggable(true)
                            .title(row.address)
                            .flat(true)
                );
                row.marker = marker;
                marker.setTitle(row.address);
                //We need to skip the first row
                if (row.points != null) {
                    int color = Color.RED;
                    if (row.businessRelated) {
                        color = Color.GREEN;
                    }
                    Polyline polyline = map.addPolyline(new PolylineOptions().addAll(row.polyPoints)
                            .width(5).color(color).geodesic(true));
                }
            }
            TripHandler.this.rows = rows;
        }

        @Override
        public void finishedGroupProcessing(List<TripRow> rows) {

        }

        @Override
        public void unableToProcessGroup(int failCode) {

        }
    }

    public interface TripMapCallbacks {
        public void onTripMapMarkerChanged(TripRow row);
    }
}
