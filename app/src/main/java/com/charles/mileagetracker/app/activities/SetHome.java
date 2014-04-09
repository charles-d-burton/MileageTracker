package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.services.LocationPingService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by charles on 3/31/14.
 */
public class SetHome extends Activity implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMarkerDragListener{

    private static GoogleMap gmap = null;
    private static LatLng coords = null;

    private final int LOADER_ID = 1;
    private static SimpleCursorAdapter mAdapter;

   //private ArrayList<Home> homeList = new ArrayList<Home>();
    private HashMap<Integer, Home> homeMap = new HashMap<Integer, Home>();

    private static AsyncTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        gmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();

        gmap.setMyLocationEnabled(true);
        coords = new LatLng(LocationPingService.lat, LocationPingService.lon);
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 16));
        gmap.setOnMapLongClickListener(this);
        getLoaderManager().initLoader(LOADER_ID, null, this);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        task.cancel(true);//Prevent the app from crashing if the Activity is closed before this can run
        getLoaderManager().destroyLoader(LOADER_ID);
        super.onDestroy();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (!tooClose(latLng)) {
            getLoaderManager().initLoader(LOADER_ID, null, this);
            ContentValues values = new ContentValues();
            values.put(StartPoints.START_LAT, latLng.latitude);
            values.put(StartPoints.START_LON, latLng.longitude);
            values.put(StartPoints.NAME, "Test");
            getContentResolver().insert(TrackerContentProvider.STARTS_URI, values);
        } else {
            //Need to add a dialog here to prompt
            Toast.makeText(this, "Too close", Toast.LENGTH_LONG).show();
            Log.v("TOO CLOSE: ", "Too close");
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String projection[] = {StartPoints.COLUMN_ID, StartPoints.NAME, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS};
        return new CursorLoader(this, TrackerContentProvider.STARTS_URI, projection,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v("Loader: " , "Callback");
        switch(loader.getId()) {
            case LOADER_ID:
                Log.v("Loader: ", "Loader finished loading");
                task = new ShowHomes().execute(cursor);
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader loader) {
        switch(loader.getId()) {
            case LOADER_ID:
                break;

        }
    }

    private synchronized void addHomes(ArrayList<Home> homes) {
        for (Home home : homes) {
            Log.v("HOMES: ", "Adding new home");
            home.instantiateMarker();
        }
        ((Object)this).notify();
    }

    //I don't think it will be necessary for people to create starting points less than a km apart
    private boolean tooClose(LatLng point) {
        Iterator it = homeMap.values().iterator();
        while (it.hasNext()) {
            Home home = (Home)it.next();
            Location a = new Location("point A");
            a.setLatitude(point.latitude);
            a.setLongitude(point.longitude);

            Location b = new Location("Point b");
            b.setLatitude(home.getLatLng().latitude);
            b.setLongitude(home.getLatLng().longitude);

            double distance = a.distanceTo(b);
            if (distance < 1000) return true;//If it's less than a kilometer away
        }
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


    private class ShowHomes extends AsyncTask<Cursor, Home, ArrayList<Home>> {
        @Override
        protected ArrayList doInBackground(Cursor... params) {
            Cursor c = params[0];
            ArrayList<Home> homes = new ArrayList<Home>();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Log.v("SHOW HOME: ", "iterating through cursor");

                Integer id = c.getInt(c.getColumnIndexOrThrow(StartPoints.COLUMN_ID));

                String name = c.getString(c.getColumnIndexOrThrow(StartPoints.NAME));
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));

                Home home = new Home(id, name, new LatLng(lat, lon));
                homes.add(home);
                if (!homeMap.containsKey(id)) {
                    homeMap.put(id, home);
                    publishProgress(home);  //This will update the UI thread to display the marker
                }

            }
            return homes;  //Return a complete list of homes that are in the database
        }

        /*
        Modify the UI thread to add the homes that are not displayed.  Once the loop above finishes
        it will flip the boolean and pass the ArrayList generated to remove any homes that were not
        in the database.  This means that an action was taken to remove them.
         */
        @Override
        protected void onProgressUpdate(Home... homes) {
            Home home = homes[0];
            home.instantiateMarker();
        }

        /*
        Remove the homes deleted from map from the database here.  Or maybe I need to just do
        that somewhere else.  At any rate I can remove the unused markers here.
         */
        @Override
        protected void onPostExecute(ArrayList<Home> result) {
            Iterator it = homeMap.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer)it.next();
                boolean matched = false;
                for (Home home : result) {
                    if (home.getId() == key ) {
                        matched = true;
                        break;//Break out of the for loop, no need to waste any more time here
                    }
                }

                if (!matched) { //Means there wasn't a match and it doesn't exist anymore, remove it
                    Home home = homeMap.get(key);
                    if (home.getMarker() != null) {
                        Marker marker = home.getMarker();
                        marker.remove();
                    }
                    homeMap.remove(key);
                }
            }
            getLoaderManager().destroyLoader(LOADER_ID);
        }
    }

    private class Home {

        private Marker marker = null;

        public Home(int id, String name, LatLng location){
            this.id = id;
            this.name = name;
            this.loc = location;
        }

        private int id = 0;
        private String name = null;
        private LatLng loc = null;

        public int getId() {
            return id;
        }


        public String getName() {
            return name;
        }

        public void setLatLng(double lat, double lon) {
            this.loc = new LatLng(lat, lon);
        }

        public LatLng getLatLng() {
            return loc;
        }

        protected void instantiateMarker() {
            marker = gmap.addMarker(new MarkerOptions().draggable(true).position(loc).title(name));
        }

        protected Marker getMarker() {
            return marker;
        }
    }
}
