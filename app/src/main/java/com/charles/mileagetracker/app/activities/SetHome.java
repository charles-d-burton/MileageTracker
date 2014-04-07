package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.provider.SyncStateContract;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.services.LocationPingService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by charles on 3/31/14.
 */
public class SetHome extends Activity implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks {

    private GoogleMap gmap = null;
    private LatLng coords = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        gmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();

        gmap.setMyLocationEnabled(true);
        coords = new LatLng(LocationPingService.lat, LocationPingService.lon);
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 16));
        gmap.setOnMapLongClickListener(this);

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker m = gmap.addMarker(new MarkerOptions().position(latLng).draggable(true));

    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
