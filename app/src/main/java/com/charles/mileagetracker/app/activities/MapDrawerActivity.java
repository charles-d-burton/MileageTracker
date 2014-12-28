package com.charles.mileagetracker.app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.fragments.ExpandableListFragment;
import com.charles.mileagetracker.app.locationservices.GetCurrentLocation;
import com.charles.mileagetracker.app.maphandlers.HomeHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;

public class MapDrawerActivity extends ActionBarActivity
    implements ExpandableListFragment.ExpandableListInteractionListener{

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");
    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.csv";
    public static final int MAP_SHOW_TRIPS = 0;
    public static final int MAP_SHOW_HOMES = 1;
    private int CURRENT_MAP = MAP_SHOW_TRIPS;

    private Toolbar toolbar;
    private Button addStartPointButton;
    private DrawerLayout drawerLayout;

    private ActionBarDrawerToggle drawerToggle = null;

    private GoogleMap googleMap = null;
    private Location location = null;
    private MapHandlerInterface handler = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_closed) {
            @Override
            public void onDrawerClosed(View drawerView) {
                Log.v("Drawer", "Closed");
                //getActionBar().setTitle("Title");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Log.v("Drawer", "Open");
                //getActionBar().setTitle("Title");

            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        addStartPointButton = (Button)findViewById(R.id.add_start_point);
        addStartPointButton.setOnClickListener(new StartButtonClickListener());

        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.activity_map)).getMap();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
        googleMap.setMyLocationEnabled(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPlayServices()) {
            Log.v("Services Check: ", "Good!");
        }
        if (handler != null) {
            handler.connect(googleMap, this);
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        googleMap.clear();
        if (handler != null) {
            handler.disconnect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(Gravity.START|Gravity.LEFT)){
            drawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void expandListItemTouch(TripRow child) {

    }

    @Override
    public void expandListItemLongTouch(TripGroup group) {

    }

    @Override
    public void expandListGroupTouch(TripGroup group) {

    }

    private class StartButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            googleMap.clear();

            if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
                drawerLayout.closeDrawers();
            }
            handler = new HomeHandler();
            handler.connect(googleMap, MapDrawerActivity.this);
            //handler.addMarker(location);
            //Log.v("Start Button Clicked: ", "CLICK");

        }
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, "This device is not supported.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface MapHandlerInterface {
        public void disconnect();
        public void connect(GoogleMap map, Context context);
    }
}
