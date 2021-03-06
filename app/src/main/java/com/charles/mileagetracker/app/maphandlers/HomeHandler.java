package com.charles.mileagetracker.app.maphandlers;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processors.AddressDistanceServices;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by charles on 12/15/14.
 */
public class HomeHandler extends MapActivityHandler implements
    LocationClient.OnAddGeofencesResultListener,
    LocationClient.OnRemoveGeofencesResultListener {

    private final String CLASS_NAME = ((Object)this).getClass().getName();
    public final String HANDLER_TAG = ((Object)this).getClass().getName();


    //private GoogleMap map;
    //private Location currentLocation;
    //private GetCurrentLocation getCurrentLocation = null;
    //private Context context;

    private List<HomePoints> homePoints = null;

    private AlertDialog runningDialog = null;

    public HomeHandler(Context context, GoogleMap map) {
        super(context, map);
    }

    public void addMarker(Location location) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        Log.v(CLASS_NAME, "Map Long Clicked");

        double distance = getDistance(latLng);

        if (homePoints.size() <= 1 || distance > 2000) {
            createMarker(latLng);
        } else if (distance > 750) {
            //Need to add a dialog here to prompt
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
            Toast.makeText(context, "That's much too close", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        zoomToLocation(marker.getPosition());

        //Build the dialog and enter the necessary data
        LinearLayout modifyMarkerLayout = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.marker_modify_layout, null);
        final EditText modifyMarkerText = (EditText)modifyMarkerLayout.findViewById(R.id.marker_name);
        final CheckBox removeMarkerCheckBox = (CheckBox)modifyMarkerLayout.findViewById(R.id.delete_marker_checkbox);
        final String markerTitle = marker.getTitle();
        modifyMarkerText.setText(markerTitle);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(modifyMarkerLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                Find the marker then assign it to a variable.  I'm checking if the remove checkbox is checked
                and then removing the marker and associated geofence if it is.  If that's not checked
                I check to see if the title was changed, if so I update the marker title and the
                database entry.
                 */
                HomePoints updateHomePoint = null;
                for (HomePoints homePoint : homePoints) {
                    if (homePoint.getMarker().equals(marker)) {
                        updateHomePoint = homePoint;
                    }
                }
                if (removeMarkerCheckBox.isChecked()) {
                    marker.remove();
                    List listOfGeofences = Collections.singletonList(Integer.toString(updateHomePoint.getId().intValue()));
                    getCurrentLocation.removeGeoFence(listOfGeofences, HomeHandler.this);
                    //locationClient.removeGeofences(listOfGeofences, SetHomeFragment.this);
                    updateHomePoint.delete();
                    homePoints.remove(updateHomePoint);
                } else if (!modifyMarkerText.getText().toString().endsWith(markerTitle)) {
                    Log.v("SetHomeFragment: ", "Title Changed");
                    updateHomePoint.name = modifyMarkerText.getText().toString();
                    updateHomePoint.save();
                    updateHomePoint.getMarker().setTitle(updateHomePoint.name);
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
        runningDialog = builder.show();
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        //zoomToLocation(marker.getPosition());
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        for (HomePoints home : homePoints) {
            if (home.getMarker().equals(marker)) {
                home.lat = marker.getPosition().latitude;
                home.lon = marker.getPosition().longitude;
                home.save();
                Executors.newSingleThreadExecutor().execute(new RetrieveAddress(home)); //Get the address on a background thread
                addProximityAlert(marker.getPosition(), home.getId().intValue());  //Replaces the old geofence
            }
        }
    }

    @Override
    public void onAddGeofencesResult(int i, String[] strings) {
        Log.v(CLASS_NAME, "Added Geofence");
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
    Iterate through all of the start points and add them to the map
     */
    private void addStartPoints(List<HomePoints> homePoints) {
        for (HomePoints homePoint : homePoints) {
            Log.v(CLASS_NAME, homePoint.address);
            Marker marker = map.addMarker(new MarkerOptions()
                            .title(homePoint.name)
                            .position(new LatLng(homePoint.lat, homePoint.lon))
                            .draggable(true)
            );
            homePoint.setMarker(marker);
        }
    }

    private void addStartPoint(HomePoints homePoint) {
        Marker marker = map.addMarker(new MarkerOptions()
                        .title(homePoint.name)
                        .position(new LatLng(homePoint.lat, homePoint.lon))
                        .draggable(true)
        );
        homePoint.setMarker(marker);
        homePoint.save();
    }

    /*
    There's a bug in here somewhere that's not checking the very first added point.  I need to find
    it sometime.
    This method creates a marker on the given LatLng.  It prompts with a Dialog to name the marker.
     */
    private void createMarker(final LatLng latLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout nameFieldLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.marker_title_layout, null);
        final EditText nameField = (EditText)nameFieldLayout.findViewById(R.id.marker_name);
        builder.setTitle("Set Name");
        builder.setView(nameFieldLayout);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            //Insert the values if the user clicks ok
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HomePoints newHomePoint = new HomePoints(nameField.getText().toString(), latLng.latitude, latLng.longitude);
                Executors.newSingleThreadExecutor().execute(new RetrieveAddress(newHomePoint));
                //newHomePoint.save();
                addStartPoint(newHomePoint);
                Log.v(CLASS_NAME, "Adding Proximity Alert");
                addProximityAlert(latLng, newHomePoint.getId().intValue());

                /*if (location != null) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    LatLng currentLocation = new LatLng(lat, lon);
                    double distance = getDistance(currentLocation);

                    if (distance > 500) {
                        addProximityAlert(latLng, newHomePoint.getId().intValue());
                    } else if (distance < 500) {
                    }
                } else {
                    addProximityAlert(latLng, newHomePoint.getId().intValue());
                }*/

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create();
        runningDialog = builder.show();
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

        for (HomePoints homePoint: homePoints) {
            Marker marker = homePoint.getMarker();
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
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Geofence fence = new Geofence.Builder()
                .setRequestId(Integer.toString(id))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, 500)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
        List fencesList = new ArrayList();
        fencesList.add(fence);
        getCurrentLocation.addGeoFence(fencesList, pendingIntent, this);
        Log.d("DEBUG: ", "Adding proximity alert");
    }

    //Convenient way to zoom to location on map
    private void zoomToLocation(Location loc) {
        if (loc != null ){
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            LatLng lastKnown = new LatLng(lat, lon);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnown, 16));
        }
    }

    @Override
    public void setTripData(List<TripRow> rows) {
        //this.homePoints = (List<HomePoints>)data;
    }

    @Override
    public String getTag() {
        return HANDLER_TAG;
    }

    @Override
    public void connect() {
        super.connect();
        homePoints = HomePoints.listAll(HomePoints.class);
        addStartPoints(homePoints);
    }

    /*@Override
    public void connect(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
        map.setOnMapLongClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);

        getCurrentLocation = new GetCurrentLocation(context);
        getCurrentLocation.updateLocation(this, true);

        homePoints = HomePoints.listAll(HomePoints.class);
        addStartPoints(homePoints);
    }*/

    /*@Override
    public void setTripData(List rows) {

    }

    @Override
    public void setHomeData(List homes) {
        addStartPoints(HomePoints.listAll(HomePoints.class));
    }*/

    //Helper Class that retrieves an address from Google in the background
    private class RetrieveAddress implements Runnable {
        private HomePoints homePoint = null;
        public RetrieveAddress(HomePoints homePoint) {
            this.homePoint = homePoint;
        }

        @Override
        public void run() {
            LatLng latLng = new LatLng(homePoint.lat, homePoint.lon);
            AddressDistanceServices addressDistanceServices = new AddressDistanceServices(context);
            try {
                String address = addressDistanceServices.getAddressFromLatLng(latLng);
                homePoint.address = address;
                homePoint.save();
            } catch (IOException e) {
                Log.d(CLASS_NAME, "Failed to Retrieve Address");
                e.printStackTrace();
            }
        }
    }
}
