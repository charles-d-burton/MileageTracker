package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.nio.channels.spi.AbstractSelectionKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by charles on 3/31/14.
 */
public class SetHome extends Activity implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener{

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
        gmap.setOnMarkerClickListener(this);
        gmap.setOnMarkerDragListener(this);
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

    //THis determmines whether or not a marker will be dropped on the map.  It's based on distance from
    //other markers
    @Override
    public void onMapLongClick(final LatLng latLng) {
        double distance = tooClose(latLng);

        if (homeMap.size() <= 1 || distance > 1000) {
            createMarker(latLng);
        } else if (distance > 500) {
            //Need to add a dialog here to prompt
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    createMarker(latLng);
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("DEBUG: ", "Location Cancel Clicked");
                }
            });
            builder.setMessage("That's pretty close to another start point.  Are you sure you want to" +
                    " add a new one?");
            builder.create();
            builder.show();
        } else if (distance > 0 && distance < 500) {
           Toast.makeText(this,"That's much too close", Toast.LENGTH_LONG).show();
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

    private void createMarker(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout nameFieldLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.marker_title_layout, null);
        final EditText nameField = (EditText)nameFieldLayout.findViewById(R.id.marker_name);
        builder.setTitle("Set Name");
        builder.setView(nameFieldLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getLoaderManager().initLoader(LOADER_ID, null, SetHome.this);
                ContentValues values = new ContentValues();
                values.put(StartPoints.START_LAT, latLng.latitude);
                values.put(StartPoints.START_LON, latLng.longitude);
                values.put(StartPoints.NAME, nameField.getText().toString());
                getContentResolver().insert(TrackerContentProvider.STARTS_URI, values);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }

    //I don't think it will be necessary for people to create starting points less than a km apart
    private double tooClose(LatLng point) {
        Iterator it = homeMap.values().iterator();
        double distance = 0;
        while (it.hasNext()) {
            Home home = (Home)it.next();
            Location a = new Location("point A");
            a.setLatitude(point.latitude);
            a.setLongitude(point.longitude);
            Location b = new Location("Point b");
            b.setLatitude(home.getLatLng().latitude);
            b.setLongitude(home.getLatLng().longitude);

            double newDistance = a.distanceTo(b);
            //Finding the shortest distance between two points
            if (distance == 0) {
                distance = newDistance;
            } else if (newDistance < distance ) {
                distance = newDistance;
            }
        }
        return distance;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        int id = getMarkerId(marker);
        Home home = (Home)homeMap.get(id);
        home.loc = marker.getPosition();
        ContentValues values = new ContentValues();
        values.put(StartPoints.START_LAT, marker.getPosition().latitude);
        values.put(StartPoints.START_LON, marker.getPosition().longitude);
        getContentResolver().update(TrackerContentProvider.STARTS_URI,values, StartPoints.COLUMN_ID + "=" + id, null);
    }

    /*
    When you tap a marker it opens a dialog that lets you rename or delete the marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        LinearLayout modifyMarkerLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.marker_modify_layout, null);
        final EditText modifyMarkerText = (EditText)modifyMarkerLayout.findViewById(R.id.marker_name);
        final CheckBox removeMarkerCheckBox = (CheckBox)modifyMarkerLayout.findViewById(R.id.delete_marker_checkbox);

        final int id = getMarkerId(marker);
        final String markerText = homeMap.get(id).getName();
        modifyMarkerText.setText(markerText);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(modifyMarkerLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getLoaderManager().initLoader(LOADER_ID, null, SetHome.this);
                //Remove a marker from the map, homeList, and database
                if (removeMarkerCheckBox.isChecked()) {
                    marker.remove();
                    homeMap.remove(id);
                    getContentResolver().delete(TrackerContentProvider.STARTS_URI, StartPoints.COLUMN_ID + "=" + id, null);
                } else if (!modifyMarkerText.getText().toString().equals(markerText)) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(StartPoints.NAME, modifyMarkerText.getText().toString());
                    int rowsUpdated = getContentResolver().update(TrackerContentProvider.STARTS_URI,contentValues ,StartPoints.COLUMN_ID + "=" + id, null);
                    Log.v("ROWS UPDATED: ", Integer.toString(rowsUpdated));
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
        return false;
    }


    //Helper method to find the id of a marker.  Tests against the tracking HashMap homeMap
    private int getMarkerId(Marker marker) {
        int id = 0;
        Iterator it = homeMap.keySet().iterator();
        while (it.hasNext()) {
            int testId = (Integer)it.next();
            Marker m = homeMap.get(testId).getMarker();
            if (marker.equals(m)) {
                id = testId;
                break;//Exit the loop, we're done here
            }
        }
        return id;
    }




    /*Get the markers from the database and put them on the map asynchronously.  This spawns
    a background thread that reads the database values and updates the map in the background.
    Considering that there probably won't be that many markers on the map, this is probably overkill
    but it's generally a good practice.
    */
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
                        Home mappedHome = homeMap.get(key);
                        if (mappedHome.name != home.name) {//Rename the marker on the map if it changed
                            mappedHome.name = home.name;
                            mappedHome.marker.setTitle(home.name);
                        }
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

        protected int id = 0;
        protected String name = null;
        protected LatLng loc = null;

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
