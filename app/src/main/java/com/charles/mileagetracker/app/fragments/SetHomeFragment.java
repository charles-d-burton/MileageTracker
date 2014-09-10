package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

    private final String CLASS_NAME = ((Object)this).getClass().getName();


    private OnShowHomeInteractionListener mListener;
    private GoogleMap gmap;

    private static LatLng coords = null;

    private final int LOADER_ID = 1;
    private static SimpleCursorAdapter mAdapter;

    private static Location location = null;
    private static LocationRequest locationRequest = null;
    private static LocationClient locationClient = null;
    private static LocationListener locationListener = null;

    private HashMap<Marker, Integer> startPoints = new HashMap<Marker, Integer>();

    private boolean mapStarted = false;

    private Context applicationContext;

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
        this.applicationContext = getActivity().getApplicationContext();
        View view = super.onCreateView(inflater, container, savedInstanceState);
        gmap = getMap();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationManager lm = (LocationManager)this.applicationContext.getSystemService(Context.LOCATION_SERVICE);

        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }
        if (location != null) zoomToLocation(location);

        locationClient = new LocationClient(applicationContext, this, this);
        locationListener = new MyLocationListener();

        gmap.setMyLocationEnabled(true);
        gmap.setOnMapLongClickListener(this);
        gmap.setOnMarkerClickListener(this);
        gmap.setOnMarkerDragListener(this);


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
        getLoaderManager().destroyLoader(LOADER_ID);
        if (locationClient.isConnected()) {
            locationClient.removeLocationUpdates(locationListener);
        }
        locationClient.disconnect();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(LOADER_ID, null, this);
        locationClient.connect();
    }

    @Override
    public void onDestroy() {

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

        return new CursorLoader(applicationContext, TrackerContentProvider.STARTS_URI, projection,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v("SetHomeFragment: ", "Callback");
        switch(loader.getId()) {
            case LOADER_ID:
                gmap.clear();
                startPoints.clear();
                Log.v("SetHomeFragment: ", "Loader finished loading");
                addStartPoints(cursor);
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v("SetHomeFragment: ", "Dataset Changed");
        switch(loader.getId()) {
            case LOADER_ID:
                Log.v("SetHomeFragment: ", "Found My Loader ID");
                gmap.clear();
                startPoints.clear();
                break;

        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        double distance = getDistance(latLng);

        if (startPoints.size() <= 1 || distance > 2000) {
            createMarker(latLng);
        } else if (distance > 750) {
            //Need to add a dialog here to prompt
            AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
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
            Toast.makeText(applicationContext, "That's much too close", Toast.LENGTH_LONG).show();
        }

    }

    /*
    When you tap a marker it opens a dialog that lets you rename or delete the marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        zoomToLocation(marker.getPosition());
        LinearLayout modifyMarkerLayout = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.marker_modify_layout, null);
        final EditText modifyMarkerText = (EditText)modifyMarkerLayout.findViewById(R.id.marker_name);
        final CheckBox removeMarkerCheckBox = (CheckBox)modifyMarkerLayout.findViewById(R.id.delete_marker_checkbox);
        final String markerTitle = marker.getTitle();
        modifyMarkerText.setText(markerTitle);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(modifyMarkerLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int id = startPoints.get(marker);
                if (removeMarkerCheckBox.isChecked()) {
                    applicationContext.getContentResolver().delete(TrackerContentProvider.STARTS_URI, StartPoints.COLUMN_ID + "=" + id, null);
                    List listOfGeofences = Collections.singletonList(Integer.toString(id));
                    locationClient.removeGeofences(listOfGeofences, SetHomeFragment.this);
                    getLoaderManager().initLoader(LOADER_ID, null, SetHomeFragment.this);
                } else if (!modifyMarkerText.getText().toString().endsWith(markerTitle)) {
                    Log.v("SetHomeFragment: ", "Title Changed");
                    ContentValues values = new ContentValues();
                    values.put(StartPoints.NAME, modifyMarkerText.getEditableText().toString());
                    applicationContext.getContentResolver().update(TrackerContentProvider.STARTS_URI, values, StartPoints.COLUMN_ID + "=" + id, null);
                    getLoaderManager().initLoader(LOADER_ID, null, SetHomeFragment.this);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return false;

    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        zoomToLocation(marker.getPosition());

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        int id = startPoints.get(marker);
        ContentValues values = new ContentValues();
        values.put(StartPoints.START_LAT, marker.getPosition().latitude);
        values.put(StartPoints.START_LON, marker.getPosition().longitude);
        getActivity().getContentResolver().update(TrackerContentProvider.STARTS_URI,values, StartPoints.COLUMN_ID + "=" + id, null);
        addProximityAlert(marker.getPosition(), id);  //Replaces the old geofence
    }

    /*
    Methods for handling connection to Google Play Services and notifications about the addition
    or removal of Geofences.
     */

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {
        if (LocationStatusCodes.SUCCESS == i) {
            Log.v(CLASS_NAME, "Successfully Added Geofence");
        } else {
            switch (i) {
                case LocationStatusCodes.ERROR:
                    Log.v(CLASS_NAME, "Generic Error, really not helpful");
                    break;
                case LocationStatusCodes.GEOFENCE_NOT_AVAILABLE:
                    Log.v(CLASS_NAME, "Geofence not available");
                    break;
                case LocationStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                    Log.v(CLASS_NAME, "Too many geofences");
                    break;
                case LocationStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                    Log.v(CLASS_NAME, "Too many pending intents");
                    break;
                default:
                    Log.v(CLASS_NAME, "Other unknown error");
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

    /*
    Iterate through the cursor creating a marker for every row
     */
    private void addStartPoints(Cursor c) {
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            String projection[] = {StartPoints.COLUMN_ID, StartPoints.NAME, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS};
            int id = c.getInt(c.getColumnIndexOrThrow(StartPoints.COLUMN_ID));
            String name = c.getString(c.getColumnIndexOrThrow(StartPoints.NAME));
            double start_lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
            double start_lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));
            Marker marker = gmap.addMarker(new MarkerOptions()
                    .title(name)
                    .position(new LatLng(start_lat,start_lon))
                    .draggable(true));
            startPoints.put(marker, id);
        }
        getLoaderManager().destroyLoader(LOADER_ID);
    }

    /*
    There's a bug in here somewhere that's not checking the very first added point.  I need to find
    it sometime.
    This method creates a marker on the given LatLng.  It prompts with a Dialog to name the marker.
     */
    private void createMarker(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        LinearLayout nameFieldLayout = (LinearLayout)getActivity().getLayoutInflater().inflate(R.layout.marker_title_layout, null);
        final EditText nameField = (EditText)nameFieldLayout.findViewById(R.id.marker_name);
        builder.setTitle("Set Name");
        builder.setView(nameFieldLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            //Insert the values if the user clicks ok
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ContentValues values = new ContentValues();
                values.put(StartPoints.START_LAT, latLng.latitude);
                values.put(StartPoints.START_LON, latLng.longitude);
                values.put(StartPoints.NAME, nameField.getText().toString());

                getLoaderManager().restartLoader(LOADER_ID, null, SetHomeFragment.this);
                Uri uri = applicationContext.getContentResolver().insert(TrackerContentProvider.STARTS_URI, values);

                int id = Integer.parseInt(uri.getLastPathSegment());

                if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    LatLng currentLocation = new LatLng(lat, lon);
                    double distance = getDistance(currentLocation);

                    if (distance > 500) {
                        addProximityAlert(latLng, id);
                    } else if (distance < 500) {
                    }
                } else {
                    addProximityAlert(latLng, id);
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    /*I don't think it will be necessary for people to create starting points less than a km apart
    This method takes all of the startpoints that currently exist on the map and calculates the disance
    from them based on the LatLng passed to it.
     */
    private double getDistance(LatLng point) {

        double distance = 0f;
        Location a = new Location("point A");
        a.setLatitude(point.latitude);
        a.setLongitude(point.longitude);

        Iterator it = startPoints.keySet().iterator();
        while (it.hasNext()) {
            Marker marker = (Marker)it.next();
            Location b = new Location("point B");
            b.setLatitude(marker.getPosition().latitude);
            b.setLongitude(marker.getPosition().longitude);

            double newDistance = a.distanceTo(b);

            if (distance == 0) {
                distance = newDistance;
            } else if (newDistance < distance) {
                distance = newDistance;
            }
        }
        return distance;
    }



    //Not close enough to a fixed point to learn anything about it, add a proximity alert that will run
    //when we get close.
    private void addProximityAlert(LatLng latLng, int id) {
        Intent intent = new Intent("com.charles.mileagetracker.app.ACTION_RECEIVE_GEOFENCE");
        intent.putExtra("id", id);
        intent.putExtra("lat", latLng.latitude);
        intent.putExtra("Lon", latLng.longitude);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(applicationContext.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Geofence fence = new Geofence.Builder()
                .setRequestId(Integer.toString(id))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        List fencesList = new ArrayList();
        fencesList.add(fence);
        Log.d("DEBUG: ", "Adding proximity alert");
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

    private void zoomToLocation(LatLng latLng) {
        if (latLng != null) {
            gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 9));
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
