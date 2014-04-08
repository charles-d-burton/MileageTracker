package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

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

/**
 * Created by charles on 3/31/14.
 */
public class SetHome extends Activity implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private GoogleMap gmap = null;
    private LatLng coords = null;

    private final int LOADER_ID = 1;
    private SimpleCursorAdapter mAdapter;

    private ArrayList<Marker> markerArrayList = new ArrayList<Marker>();

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
    public void onMapLongClick(LatLng latLng) {
        ContentValues values = new ContentValues();
        values.put(StartPoints.START_LAT, latLng.latitude);
        values.put(StartPoints.START_LON, latLng.longitude);
        values.put(StartPoints.NAME, "Test");
        getContentResolver().insert(TrackerContentProvider.STARTS_URI, values);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String projection[] = {StartPoints.COLUMN_ID, StartPoints.NAME, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS};
        return new CursorLoader(this, TrackerContentProvider.STARTS_URI, projection,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch(loader.getId()) {
            case LOADER_ID:
                new ShowHomes().execute(cursor);
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

    private class ShowHomes extends AsyncTask<Cursor, Integer, ArrayList<Marker>> {
        @Override
        protected ArrayList doInBackground(Cursor... params) {
            Cursor c = params[0];
            ArrayList<Marker> homes = new ArrayList<Marker>();
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {

                String name = c.getString(c.getColumnIndexOrThrow(StartPoints.NAME));
                double lat = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LAT));
                double lon = c.getDouble(c.getColumnIndexOrThrow(StartPoints.START_LON));

                for (Marker m : markerArrayList) {

                    double markerLat = m.getPosition().latitude;
                    double markerLon = m.getPosition().longitude;
                    if (lat != markerLat && lon != markerLon) {
                        LatLng latlng = new LatLng(lat, lon);

                        Marker newMarker = gmap.addMarker(new MarkerOptions().position(latlng).draggable(true).title(name));
                        homes.add(newMarker);
                    }

                }
            }
            c.close();
            return homes;
        }

        @Override
        protected void onPostExecute(ArrayList<Marker> result) {
            markerArrayList.clear();
            markerArrayList.addAll(result);
        }
    }
}
