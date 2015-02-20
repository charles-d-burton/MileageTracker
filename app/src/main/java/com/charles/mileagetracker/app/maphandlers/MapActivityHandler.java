package com.charles.mileagetracker.app.maphandlers;

import android.content.Context;
import android.location.Location;

import com.charles.mileagetracker.app.activities.MapDrawerActivity;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processingservices.GetCurrentLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;

/**
 * Created by charles on 2/13/15.
 */
public abstract class MapActivityHandler implements
        MapDrawerActivity.MapHandlerInterface,
        GetCurrentLocation.GetLocationCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener{

    private static GoogleMap map;
    private static Context context;
    private Location currentLocation;
    private static GetCurrentLocation getCurrentLocation = null;

    public MapActivityHandler(Context context, GoogleMap map) {
        this.map = map;
        this.context = context;
    }


    @Override
    public void disconnect() {
        map.clear();
        if (getCurrentLocation != null) {
            getCurrentLocation.forceDisconnect();
        }

    }

    @Override
    public void connect() {
        map.setOnMapLongClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMarkerDragListener(this);

        getCurrentLocation = new GetCurrentLocation(context);
        getCurrentLocation.updateLocation(this, true);
    }

    @Override
    public void retrievedLocation(double resolution, Location location) {
        String locationString = "\n" + Double.toString(location.getLatitude()) + "\n" + Double.toString(location.getLongitude()) + "\n";

        //Log.v(CLASS_NAME, locationString);
        this.currentLocation = location;
    }

    //TODO: Zoom to a location here
    @Override
    public void zoomToLocation(LatLng latLng) {
        if (latLng != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
    }

    @Override
    public void locationClientConnected() {

    }

    @Override
    public void locationConnectionFailed() {

    }

    @Override
    public abstract void setTripData(List<TripRow> data);

    @Override
    public abstract String getTag();

    @Override
    public abstract void onMapClick(LatLng latLng);

    @Override
    public abstract void onMapLongClick(LatLng latLng);

    @Override
    public abstract boolean onMarkerClick(Marker marker);

    @Override
    public abstract void onMarkerDragStart(Marker marker);

    @Override
    public abstract void onMarkerDrag(Marker marker);

    @Override
    public abstract void onMarkerDragEnd(Marker marker);
}
