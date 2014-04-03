package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.os.Bundle;

import com.charles.mileagetracker.app.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

/**
 * Created by charles on 3/31/14.
 */
public class SetHome extends Activity {

    private GoogleMap gmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        gmap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map_view)).getMap();
    }
}
