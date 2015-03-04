package com.charles.mileagetracker.app.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import android.widget.TextView;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.fragments.TripFragment;
import com.charles.mileagetracker.app.fragments.TripStopsFragment;
import com.charles.mileagetracker.app.maphandlers.HomeHandler;
import com.charles.mileagetracker.app.maphandlers.TripHandler;
import com.charles.mileagetracker.app.processingservices.GenerateCSV;
import com.charles.mileagetracker.app.processingservices.GenerateXLS;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class MapDrawerActivity extends ActionBarActivity
    implements TripFragment.OnTripFragmentInteraction,
        TripStopsFragment.OnStopInteractionListener{

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static final int MAP_SHOW_TRIPS = 0;
    public static final int MAP_SHOW_HOMES = 1;
    private int CURRENT_MAP = MAP_SHOW_TRIPS;

    private static Context context = null;
    private Toolbar toolbar;
    //private Button addStartPointButton;
    private TextView addStartView;
    private DrawerLayout drawerLayout;
    private ProgressDialog loadingDialog;
    private static ProgressDialog csvDialog = null;
    private SlidingUpPanelLayout slideUpLayout = null;

    private ActionBarDrawerToggle drawerToggle = null;

    private GoogleMap googleMap = null;
    private Location location = null;
    private MapHandlerInterface mapHandlerInterface = null;
    private TripStopsFragment tripStopsFragment = null;

    private TripGroup currentWorkingGroup = null;
    private String currentHandler = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_map_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Mileage Tracker");
        //toolbar.setBackgroundColor(getResources().getColor(R.color.primary));

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
                if (slideUpLayout.isPanelExpanded()) {
                    slideUpLayout.collapsePanel();
                }
                //getActionBar().setTitle("Title");

            }
        };
        slideUpLayout = (SlidingUpPanelLayout)findViewById(R.id.stops_slide_up_layout);
        //slideUpLayout.setPanelHeight(40);
        //slideUpLayout.setShadowHeight(4);
        drawerLayout.setDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        addStartView = (TextView)findViewById(R.id.add_start_point_trip);
        addStartView.setOnClickListener(new StartButtonClickListener());
        //addStartPointButton = (Button)findViewById(R.id.add_start_point_trip);
        //addStartPointButton.setOnClickListener(new StartButtonClickListener());

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

        FragmentManager fragmentManager = getFragmentManager();
        tripStopsFragment = (TripStopsFragment)fragmentManager.findFragmentById(R.id.stops_panel);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPlayServices()) {
            Log.v("Services Check: ", "Good!");
        }
        if (mapHandlerInterface != null) {
            mapHandlerInterface.connect();
        } else {
            toolbar.setTitle("Trip Stops");
            mapHandlerInterface = new TripHandler(this, googleMap);
            mapHandlerInterface.connect();
        }

        if (currentWorkingGroup != null) {
            tripStopsFragment.setData(currentWorkingGroup);
        } else {
            try {
                TripGroup row = TripGroup.find(TripGroup.class, null, null, null, " id DESC LIMIT 1", null).get(0);
                currentWorkingGroup = TripGroup.listAll(TripGroup.class).get(0);
                tripStopsFragment.setData(currentWorkingGroup);
            } catch (Exception aiobe) {

            }

        }
    }


    @Override
    protected  void onPause() {
        super.onPause();
        //googleMap.clear();
        if (mapHandlerInterface != null) {
            mapHandlerInterface.disconnect();
        }
        if (loadingDialog != null) {
            loadingDialog.dismiss();
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
            //Log.v("CSV: ", "CSV Action Clicked");
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            DatePicker dp = new DatePicker();

            dp.show(fm, "Date Picker");
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
    public void onFragmentInteraction() {

    }

    @Override
    public void onTripFragmentStartLoad() {
        Log.v("Loading Data: ", "Started");
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog.cancel();
            loadingDialog = null;
        }
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Loading Trips....");
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        loadingDialog.setIndeterminate(false);
        loadingDialog.setMax(100);
        loadingDialog.show();
    }

    @Override
    public void onTripFragmentProgressUpdate(Integer tripNum) {
        Log.v("Loaded %: ", tripNum.toString());
        loadingDialog.setProgress(tripNum);
    }

    @Override
    public void onTripFragmentFinishLoad() {
        Log.v("Loading Data: ", "Finished");
        loadingDialog.dismiss();
    }

    public void onItemTouched(TripRow row) {
        toolbar.setTitle("Trip Stops");
        currentWorkingGroup = row.tgroup;
        googleMap.clear();

        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
            slideUpLayout.expandPanel();
        }
        TripHandler tripHandler = new TripHandler(this, googleMap);
        mapHandlerInterface = tripHandler;
        mapHandlerInterface.connect();
        mapHandlerInterface.zoomToLocation(new LatLng(row.lat, row.lon));
        tripStopsFragment.setData(row);
    }

    @Override
    public void onItemLongPressed(TripGroup group) {

        currentWorkingGroup = group;
        googleMap.clear();
        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
        }
        TripHandler tripHandler = new TripHandler(this, googleMap);
        mapHandlerInterface = tripHandler;
        mapHandlerInterface.connect();

    }

    @Override
    public void onStopClicked(List<TripRow> rows) {
        if (mapHandlerInterface != null && rows != null) {
            mapHandlerInterface.setTripData(rows);
        }
    }

    @Override
    public void onStopLongPress(TripRow row) {

    }

    @Override
    public void tripStopsDataLoaded(List<TripRow> rows) {
        mapHandlerInterface.setTripData(rows);
    }

    public void showDatePickerDialog() {
        DialogFragment newFragment = new DatePicker();

        //newFragment.show(getFragmentManager(), "startDatePicker");
    }

    private class StartButtonClickListener implements TextView.OnClickListener {

        @Override
        public void onClick(View v) {
            toolbar.setTitle("Home Points");
            googleMap.clear();

            if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
                drawerLayout.closeDrawers();
            }
            mapHandlerInterface = new HomeHandler(MapDrawerActivity.this, googleMap);
            mapHandlerInterface.connect();
            //mapHandlerInterface.addMarker(location);
            Log.v("Start Button Clicked: ", "CLICK");

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

    //This adds up all the mileage so far marked as business related and then sets the TextVIew
    //to reflect that.
    private class CalculateMileage extends AsyncTask<Void, Void, Void> {
        private double totalDistance = 0.0;
        @Override
        protected Void doInBackground(Void... params) {
            List<TripGroup> tripGroups = TripGroup.listAll(TripGroup.class);
            for (TripGroup group : tripGroups) {
                totalDistance = totalDistance + group.billableMileage;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            ((TextView)findViewById(R.id.total_miles)).setText(Integer.toString(new Double(totalDistance).intValue()) + " Miles");
        }
    }

    public static class DatePicker extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        private static long start = 0l;
        private static long end = 0l;


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dpd = new DatePickerDialog(getActivity(), this, year, month, day);
            if (start == 0) {
                dpd.setTitle("Start Date:");
            } else {
                dpd.setTitle("End Date:");
            }
            return dpd;
        }

        @Override
        public void onDateSet(android.widget.DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar c = Calendar.getInstance();
            c.set(year, monthOfYear, dayOfMonth);
            if (start == 0) {

                start = c.getTimeInMillis();
                Log.v("Start Date Set: ", sdf.format(c.getTime()));

                DialogFragment newFragment = new DatePicker();
                newFragment.show(getFragmentManager(), "endDatePicker");
            } else {

                end = c.getTimeInMillis();
                Log.v("End Date Set: ", sdf.format(c.getTime()));
                new GenerateXLS(context).execute(start, end);
                start = 0;
                end = 0;
            }

        }
    }

    public interface MapHandlerInterface {
        public void disconnect();
        public void connect();
        public void setTripData(List<TripRow> data);
        public String getTag();
        public void zoomToLocation(LatLng latLng);
    }
}
