package com.charles.mileagetracker.app.maphandlers;

import android.location.Location;

import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by charles on 12/15/14.
 */
public class TripHandler implements GetCurrentLocation.GetLocationCallback {




    @Override
    public void retrievedLocation(double resolution, Location location) {

    }

    @Override
    public void locationConnectionFailed() {

    }
}
