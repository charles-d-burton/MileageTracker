package com.charles.mileagetracker.app.maphandlers;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.activities.MapDrawerActivity;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    public TripHandler(){

    }

    public void loadGroup(TripGroup group) {

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

    /*
    This class takes a TripGroup and generates the PolyLine points in a background thread.  It then
    puts them on the map after it generates them.
     */
    private class DrawLines extends AsyncTask<TripGroup, Integer, TripGroup> {

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            distanceServices = new AddressDistanceServices(context);
        }

        @Override
        protected TripGroup doInBackground(TripGroup... params) {

            TripGroup tripGroup = params[0];
            String entries[] = {Long.toString(tripGroup.getId())};

            List<TripRow> rowsList = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
            LinkedList<TripRow> rows = new LinkedList<TripRow>(rowsList);
            tripGroup.setChildren(rows);
            if (rows.size() <=1) {
                return null;
            }
            TripRow lastRow = rows.get(0);//Remove the first so that it's not iterated over
            for (TripRow nextRow : rows) {
                if (rows.equals(rows.getFirst())){
                    continue;
                }
                String url = distanceServices.getDirectionsURL(lastRow.lat, lastRow.lon, nextRow.lat, nextRow.lon);
                String result = distanceServices.getStringFromUrl(url);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray routeArray = json.getJSONArray("routes");
                    JSONObject routes = routeArray.getJSONObject(0);
                    JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                    String encodedString = overviewPolylines.getString("points");
                    List lines = distanceServices.decodePoly(encodedString);
                    Log.v("Number of lines: ", Integer.toString(lines.size()));
                    polyLineTracker.put(lastRow, lines);
                    //point1.addAllPoints(lines);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            return tripGroup;
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
        protected void onPostExecute(TripGroup group) {
            if (group == null) return;
            super.onPostExecute(group);
            ArrayList<TripRow> children = group.getChildren();
            //SetHomeDrawerAdapter drawerAdapter = new SetHomeDrawerAdapter(ShowTripsFragment.this.getActivity(), children);
            //drawerView.setAdapter(drawerAdapter);
            Iterator it = children.iterator();
            Polyline polyline = null;
            while (it.hasNext()) {

                TripRow child = (TripRow) it.next();

                if (polyline != null && child.businessRelated) {
                    polyline.setColor(Color.GREEN);
                }
                Marker marker = map.addMarker(
                        new MarkerOptions().position(new LatLng(child.lat, child.lon))
                                .draggable(false)
                                .title(child.address)
                                .flat(true)

                );

                markerTracker.put(marker, child);
                List<LatLng> points = polyLineTracker.get(child);
                //Log.v("POINTS: ", "Number of points: " + Integer.toString(points.size()));

                if (points.size() > 0) {
                    polyline = map.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED).geodesic(true));
                }
            }
        }
    }
}
