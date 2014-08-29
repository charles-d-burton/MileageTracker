package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.charles.mileagetracker.app.fragments.SetHomeFragment.OnShowHomeInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetHomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SetHomeFragment extends MapFragment implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationClient.OnAddGeofencesResultListener,
        LocationClient.OnRemoveGeofencesResultListener{


    private OnShowHomeInteractionListener mListener;
    private GoogleMap gmap;

    private static LatLng coords = null;

    private final int LOADER_ID = 1;
    private static SimpleCursorAdapter mAdapter;

    //private ArrayList<Home> homeList = new ArrayList<Home>();
    private HashMap<Integer, Home> homeMap = new HashMap<Integer, Home>();

    //private static LocationManager lm;
    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListener = null;

    private static AsyncTask task;

    private boolean mapStarted = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SetHomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetHomeFragment newInstance() {
        SetHomeFragment fragment = new SetHomeFragment();
        return fragment;
    }
    public SetHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        gmap = getMap();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //locationRequest.setInterval(5000);
        //locationRequest.setFastestInterval(1000);

        locationClient = new LocationClient(getActivity(), this, this);
        locationListener = new MyLocationListener();

        //gmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();

        gmap.setMyLocationEnabled(true);
        //coords = new LatLng(LocationPingService.lat, LocationPingService.lon);
        gmap.setOnMapLongClickListener(this);
        gmap.setOnMarkerClickListener(this);
        gmap.setOnMarkerDragListener(this);
        getLoaderManager().initLoader(LOADER_ID, null, this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnShowHomeInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(locationListener);
        }
        locationClient.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        locationClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        task.cancel(true);//Prevent the app from crashing if the Activity is closed before this can run
        getLoaderManager().destroyLoader(LOADER_ID);
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationClient.requestLocationUpdates(locationRequest, locationListener);
    }

    @Override
    public void onDisconnected() {

    }

    /*
   Loader methods that handle interfacing with the database.
    */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String projection[] = {StartPoints.COLUMN_ID, StartPoints.NAME, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS};
        return new CursorLoader(getActivity().getApplicationContext(), TrackerContentProvider.STARTS_URI, projection,null,null,null);
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        double distance = getDistance(latLng);

        if (homeMap.size() <= 1 || distance > 1000) {
            createMarker(latLng);
        } else if (distance > 500) {
            //Need to add a dialog here to prompt
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            Toast.makeText(getActivity(), "That's much too close", Toast.LENGTH_LONG).show();
        }

    }

    /*
    When you tap a marker it opens a dialog that lets you rename or delete the marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        LinearLayout modifyMarkerLayout = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.marker_modify_layout, null);
        final EditText modifyMarkerText = (EditText)modifyMarkerLayout.findViewById(R.id.marker_name);
        final CheckBox removeMarkerCheckBox = (CheckBox)modifyMarkerLayout.findViewById(R.id.delete_marker_checkbox);

        final int id = getMarkerId(marker);
        final String markerText = homeMap.get(id).getName();
        modifyMarkerText.setText(markerText);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(modifyMarkerLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getLoaderManager().initLoader(LOADER_ID, null, SetHomeFragment.this);
                //Remove a marker from the map, homeList, and database
                if (removeMarkerCheckBox.isChecked()) {
                    marker.remove();
                    homeMap.remove(id);
                    getActivity().getContentResolver().delete(TrackerContentProvider.STARTS_URI, StartPoints.COLUMN_ID + "=" + id, null);
                    Log.v("DEBUG: ", "Prepping to remove Geofence: " + Integer.toString(id));
                    List listOfGeofences = Collections.singletonList(Integer.toString(id));
                    locationClient.removeGeofences(listOfGeofences, SetHomeFragment.this);
                } else if (!modifyMarkerText.getText().toString().equals(markerText)) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(StartPoints.NAME, modifyMarkerText.getText().toString());
                    int rowsUpdated = getActivity().getContentResolver().update(TrackerContentProvider.STARTS_URI,contentValues ,StartPoints.COLUMN_ID + "=" + id, null);
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
        getActivity().getContentResolver().update(TrackerContentProvider.STARTS_URI,values, StartPoints.COLUMN_ID + "=" + id, null);

        //TODO, update the GeoFence
    }

    /*
    Methods for handling connection to Google Play Services and notifications about the addition
    or removal of Geofences.
     */

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {
        if (LocationStatusCodes.SUCCESS == i) {
            Log.v("DEBUG: ", "Successfully Added Geofence");
        } else {
            switch (i) {
                case LocationStatusCodes.ERROR:
                    Log.v("Debug:", "Generic Error, really not helpful");
                    break;
                case LocationStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    Log.v("DEBUG: ", "Geofence not available");
                    break;
                case LocationStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    Log.v("DEBUG: ", "Too many geofences");
                    break;
                case LocationStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    Log.v("DEBUG: ", "Too many pending intents");
                    break;
                default:
                    Log.v("DEBUG: ", "Other unknown error");
                    Log.v("Error Code: ", Integer.toString(i));
                    break;
            }
        }
    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int i, String[] strings) {
        Log.v("DEBUG: ", "Removed Geofence: " + Integer.toString(i));
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent) {

    }

    private synchronized void addHomes(ArrayList<Home> homes) {
        for (Home home : homes) {
            Log.v("HOMES: ", "Adding new home");
            home.instantiateMarker();
        }
        ((Object)this).notify();
    }

    /*
    There's a bug in here somewhere that's not checking the very first added point.  I need to find
    it sometime.
    This method creates a marker on the given LatLng.  It prompts with a Dialog to name the marker.
    TODO: Find the effing bug
     */
    private void createMarker(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //TODO:  This might be broken
        LinearLayout nameFieldLayout = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.marker_title_layout, null);
        final EditText nameField = (EditText)nameFieldLayout.findViewById(R.id.marker_name);
        builder.setTitle("Set Name");
        builder.setView(nameFieldLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getLoaderManager().initLoader(LOADER_ID, null, SetHomeFragment.this);
                ContentValues values = new ContentValues();
                values.put(StartPoints.START_LAT, latLng.latitude);
                values.put(StartPoints.START_LON, latLng.longitude);
                values.put(StartPoints.NAME, nameField.getText().toString());

                Uri uri = getActivity().getContentResolver().insert(TrackerContentProvider.STARTS_URI, values);
                int id = Integer.parseInt(uri.getLastPathSegment());

                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    LatLng currentLocation = new LatLng(lat, lon);
                    double distance = getDistance(currentLocation, latLng);
                    Log.d("DEBUG: ", "Distance is: " + Double.toString(distance));
                    if (distance > 500) {
                        Log.v("DEBUG: ", "ID To add Proximity Alert Is: " + Integer.toString(id));
                        addProximityAlert(latLng, id);
                    } else if (distance < 500) {
                        Log.v("DEBUG: ", "ID To add Proximity Alert Is: " + Integer.toString(id));
                        //Intent intent = new Intent(SetHome.this, LearnLocationIntentService.class);
                        //intent.putExtra("id", id);
                        //startService(intent);
                        addProximityAlert(latLng, id);
                    }
                } else {
                    addProximityAlert(latLng, id);
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
    }

    //I don't think it will be necessary for people to create starting points less than a km apart
    private double getDistance(LatLng point) {

        double distance = 0f;
        Location a = new Location("point A");
        a.setLatitude(point.latitude);
        a.setLongitude(point.longitude);

        Iterator it = homeMap.values().iterator();
        while (it.hasNext()) {
            Home home = (Home)it.next();

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

    //Another convenience method that just lets me get the distance between two points.
    private double getDistance(LatLng pointA, LatLng pointB) {
        double distance = 0f;

        Location a = new Location("pointA");
        a.setLatitude(pointA.latitude);
        a.setLongitude(pointA.longitude);

        Location b = new Location("pointB");
        b.setLatitude(pointB.latitude);
        b.setLongitude(pointB.longitude);

        distance = a.distanceTo(b);

        return distance;
    }


    //Not close enough to a fixed point to learn anything about it, add a proximity alert that will run
    //when we get close.
    private void addProximityAlert(LatLng latLng, int id) {
        Intent intent = new Intent("com.charles.mileagetracker.app.ACTION_RECEIVE_GEOFENCE");
        intent.putExtra("id", id);
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("Lon", latLng.longitude);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity().getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Geofence fence = new Geofence.Builder()
                .setRequestId(Integer.toString(id))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        List fencesList = new ArrayList();
        fencesList.add(fence);
        locationClient.addGeofences(fencesList, pendingIntent, this);
        Log.d("DEBUG: ", "Adding proximity alert");
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


    //Convenient way to zoom to location on map
    private void zoomToLocation(Location loc) {
        if (loc != null ){
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            LatLng lastKnown = new LatLng(lat, lon);
            gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnown, 16));

        }
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
            if (c != null && c.getCount() > 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
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
            }
            if (c != null) c.close();

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


    /*
    Container class for the different Home locations.  I probably need to rename this class to something
    a little more obvious, but it was the best thing I could think of at the time.  This just stores
    various parameters about the Marker/Home when it's dropped on the map.  It also manages the creation
    of the marker itself for display on the map.
     */
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
            if (marker == null) {
                marker = gmap.addMarker(new MarkerOptions().draggable(true).position(loc).title(name));
            }
        }

        protected Marker getMarker() {
            return marker;
        }
    }


    /*
    Helper class that listens for location change events.
     */
    private final class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            SetHomeFragment.this.location = location;
            if (!mapStarted) {
                zoomToLocation(location);
                //checkLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                mapStarted = true;
            }

        }
    }

    private void checkLocation(LatLng location) {
        Geocoder geoCoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1);
            for (Address address : addresses) {
                //Log.v("DEBUG: ", "Thoroughfare: " + address.getThoroughfare());
                Log.v("DEBUG: ", "Address line: " + address.getAddressLine(0));
                Log.v("DEBUG: ", "Feature Name: " + address.getFeatureName());
            }
        } catch (IOException ioe) {

        }
        //locationUpdateInProgress = false;//Done checking current location
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnShowHomeInteractionListener {
        // TODO: Update argument type and name
        public void onShowHomeInteraction();
        //public void onFragmentInteraction(Uri uri);
    }
}