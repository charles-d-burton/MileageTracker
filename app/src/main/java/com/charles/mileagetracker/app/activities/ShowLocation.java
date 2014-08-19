package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class ShowLocation extends Activity implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener,
        LocationClient.OnAddGeofencesResultListener,
        LocationClient.OnRemoveGeofencesResultListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener{

    private double lat;
    private double lon;
    private int id;

    private GoogleMap gmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        gmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();
        gmap.setOnMapLongClickListener(this);
        gmap.setOnMarkerClickListener(this);
        gmap.setOnMarkerDragListener(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            id = bundle.getInt("id");
            ExpandListChild child = (ExpandListChild) bundle.getSerializable("child");
            lat = child.getLat();
            lon = child.getLon();

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(13)
                    .build();
            gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            ExpandListGroup listGroup = child.getExpandGroup();
            new DrawLines().execute(listGroup);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

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
    public void onRemoveGeofencesByRequestIdsResult(int i, String[] strings) {

    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent) {

    }

    private class DrawLines extends AsyncTask<ExpandListGroup, Integer, ExpandListGroup>{

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            distanceServices = new AddressDistanceServices(ShowLocation.this);
        }

        @Override
        protected ExpandListGroup doInBackground(ExpandListGroup... params) {
            ExpandListGroup expandListGroup = params[0];
            ArrayList children = expandListGroup.getListChildren();
            for (int i = 0; i < children.size(); i++) {
                if (i == children.size() -1){
                    Log.v("LAST POINT: ", "LAST POINT");
                    break;
                }
                ExpandListChild point1 = (ExpandListChild)children.get(i);
                ExpandListChild point2 = (ExpandListChild)children.get(i + 1);

                String url = distanceServices.getDirectionsURL(point1.getLat(), point1.getLon(), point2.getLat(), point2.getLon());
                String result = distanceServices.getStringFromUrl(url);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray routeArray = json.getJSONArray("routes");
                    JSONObject routes = routeArray.getJSONObject(0);
                    JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                    String encodedString = overviewPolylines.getString("points");
                    LinkedList lines = distanceServices.decodePoly(encodedString);
                    Log.v("Number of lines: " , Integer.toString(lines.size()));
                    point1.addAllPoints(lines);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            return expandListGroup;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ExpandListGroup expandListGroup) {
            super.onPostExecute(expandListGroup);
            ArrayList<ExpandListChild> children = expandListGroup.getListChildren();
            Iterator it = children.iterator();
            while (it.hasNext()) {
                ExpandListChild child = (ExpandListChild) it.next();
                LinkedList<LatLng> points = child.getLinePoints();
                if (points.size() > 0) {
                    Polyline line = gmap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED).geodesic(true));
                }
            }
        }

    }
}
