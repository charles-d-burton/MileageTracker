package com.charles.mileagetracker.app.maphandlers;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.activities.MapDrawerActivity;
import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processingservices.AddressDistanceServices;
import com.charles.mileagetracker.app.processingservices.GetCurrentLocation;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by charles on 12/15/14.
 */
public class TripHandler implements GetCurrentLocation.GetLocationCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener,
        MapDrawerActivity.MapHandlerInterface{

    private GoogleMap map = null;
    private Context context = null;

    private HashMap<Marker, TripRow> markerTracker = new HashMap<Marker,TripRow>();
    private HashMap<TripRow, List<LatLng>> polyLineTracker = new HashMap<TripRow, List<LatLng>>();
    private TripGroup group;

    public TripHandler(){

    }

    @Override
    public void retrievedLocation(double resolution, Location location) {

    }

    @Override
    public void locationClientConnected() {

    }

    @Override
    public void locationConnectionFailed() {

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

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void connect(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
        map.setOnMapLongClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);
    }

    @Override
    public void setTripData(TripGroup group) {
        this.group = group;
        new DrawLines().execute(group);
    }

    @Override
    public void setHomeData(List<HomePoints> homes) {

    }

    /*
    This class takes a TripGroup and generates the PolyLine points in a background thread.  It then
    puts them on the map after it generates them.
     */
    private class DrawLines extends AsyncTask<TripGroup, Integer, LinkedList<TripRow>> implements
            TripGroupProcessor.GroupProcessorInterface {

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            distanceServices = new AddressDistanceServices(context);
        }

        @Override
        protected LinkedList<TripRow> doInBackground(TripGroup... params) {

            TripGroup tripGroup = params[0];
            String entries[] = {Long.toString(group.getId())};
            //Get the full list, check each stop to make sure it's not too close to a HomePoint
            List<TripRow> rowsList = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
            return null;
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
        protected void onPostExecute(LinkedList<TripRow> rows) {
            if (rows == null) return;
            super.onPostExecute(rows);
            //SetHomeDrawerAdapter drawerAdapter = new SetHomeDrawerAdapter(ShowTripsFragment.this.getActivity(), children);
            //drawerView.setAdapter(drawerAdapter);
            for (int i = 0; i < rows.size(); i++) {
                TripRow row = rows.get(i);

                Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(row.lat, row.lon))
                            .draggable(true)
                            .title(row.address)
                            .flat(true)
                );
                markerTracker.put(marker, row);
                //We need to skip the first row
                if (!rows.getFirst().equals(row)) {
                    List<LatLng> points = polyLineTracker.get(row);
                    if (points != null && points.size() > 0) {
                        Polyline polyline = map.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED).geodesic(true));
                        if (row.businessRelated) {
                            polyline.setColor(Color.GREEN);
                        }
                    }
                }
            }
        }

        @Override
        public void finishedGroupProcessing(List<TripRow> rows) {

        }

        @Override
        public void unableToProcessGroup(int failCode) {

        }
    }
}
