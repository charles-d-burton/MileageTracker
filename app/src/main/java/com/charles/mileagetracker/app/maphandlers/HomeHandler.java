package com.charles.mileagetracker.app.maphandlers;

import android.location.Location;

import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by charles on 12/15/14.
 */
public class HomeHandler implements GetCurrentLocation.GetLocationCallback {
    private GoogleMap map;

    public HomeHandler(GoogleMap map) {
        this.map = map;
    }

    @Override
    public void retrievedLocation(double resolution, Location location) {

    }

    @Override
    public void locationConnectionFailed() {

    }
}
