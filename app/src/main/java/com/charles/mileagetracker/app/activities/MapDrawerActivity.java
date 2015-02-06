package com.charles.mileagetracker.app.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.HomePoints;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.fragments.TripFragment;
import com.charles.mileagetracker.app.fragments.TripStopsFragment;
import com.charles.mileagetracker.app.maphandlers.HomeHandler;
import com.charles.mileagetracker.app.maphandlers.TripHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class MapDrawerActivity extends ActionBarActivity
    implements TripFragment.OnTripFragmentInteraction,
        TripStopsFragment.OnStopInteractionListener{

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");
    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.csv";
    public static final int MAP_SHOW_TRIPS = 0;
    public static final int MAP_SHOW_HOMES = 1;
    private int CURRENT_MAP = MAP_SHOW_TRIPS;

    private static Context context = null;
    private Toolbar toolbar;
    private Button addStartPointButton;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        setContentView(R.layout.activity_map_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Mileage Tracker");


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

        addStartPointButton = (Button)findViewById(R.id.add_start_point_trip);
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
            mapHandlerInterface.connect(googleMap, this);
        } else {
            mapHandlerInterface = new TripHandler();
            mapHandlerInterface.connect(googleMap, this);
        }
    }

    @Override
    protected  void onPause() {
        super.onPause();
        googleMap.clear();
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
        currentWorkingGroup = row.tgroup;
        googleMap.clear();

        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
            slideUpLayout.expandPanel();
        }
        TripHandler tripHandler = new TripHandler();
        mapHandlerInterface = tripHandler;
        mapHandlerInterface.connect(googleMap, this);
        tripStopsFragment.setData(row);
    }

    @Override
    public void onItemLongPressed(TripGroup group) {
        currentWorkingGroup = group;
        googleMap.clear();
        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
        }
        TripHandler tripHandler = new TripHandler();
        mapHandlerInterface = tripHandler;
        mapHandlerInterface.connect(googleMap, this);

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
        newFragment.show(getFragmentManager(), "startDatePicker");
    }

    private class StartButtonClickListener implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            googleMap.clear();

            if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
                drawerLayout.closeDrawers();
            }
            mapHandlerInterface = new HomeHandler();
            mapHandlerInterface.connect(googleMap, MapDrawerActivity.this);
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
                new GenerateCSV().execute(start, end);
                start = 0;
                end = 0;
            }

        }


        //This queries the database to retrieve the business related trips for a given date range.  It
        //will then start a process to email the results as a CSV

        private class GenerateCSV extends AsyncTask<Long, Void, String> {

            private final String firstLine = "Date,Address,Distance(mi),Latitude,Longitude\n";

            //Create a dialog telling the user that the app is doing something
            @Override
            protected void onPreExecute() {
                csvDialog = new ProgressDialog(context);
                csvDialog.setMessage("Generating CSV....");
                csvDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                csvDialog.setIndeterminate(true);
                csvDialog.show();
            }



            /*
            NEED TO FIX TO SEARCH BETWEEN DATES
            Construct a query to pull the data out of the database between two dates.  Because of the way
            it works I have to write the generated CSV to a file.  I then pass the URL of this file to the
            email that is generated.
             */
            @Override
            protected String doInBackground(Long... params) {
                long start = params[0];
                long end = params[1];
                List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
                List lines = new ArrayList<String[]>();
                lines.add(new String[]{"Date", "Address", "Distance Traveled(miles)", "Latitude", "Longitude"});
                if (groups != null && !groups.isEmpty()) {
                    for (TripGroup group : groups) {
                        String entries[] = {Long.toString(group.getId())};
                        List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);

                    }
                }
                /*Cursor c = resolver.query(TrackerContentProvider.TRIP_URI, projection, TripTable.TIME + " BETWEEN " + start + " AND " + end, null, null);
                List<String[]> lines = new ArrayList<String[]>();
                if (c != null) {
                    c.moveToPosition(-1);
                    lines.add(new String[]{"Date", "Address", "Distance Traveled(miles)", "Latitude", "Longitude"});
                    while (c.moveToNext()) {
                        if (c.getInt(c.getColumnIndexOrThrow(TripTable.BUSINESS_RELATED)) == 1) {
                            long date = c.getLong(c.getColumnIndexOrThrow(TripTable.TIME));
                            String address = c.getString(c.getColumnIndexOrThrow(TripTable.ADDRESS));
                            int distance = new Double(c.getInt(c.getColumnIndexOrThrow(TripTable.DISTANCE)) * 0.621).intValue();
                            double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                            double lon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
                            String line[] = getLine(date, address, distance, lat, lon);
                            lines.add(line);
                        }
                    }
                }
                writeToFile(lines);
                String mailAddress = getEmail();
                emailFile(mailAddress);*/
                //readFromFile();

                return null;
            }

            @Override
            protected void onPostExecute(String csv) {
                super.onPostExecute(csv);
                csvDialog.dismiss();
            }

            //Create a CSV line
            private String[] getLine(long date, String address, int distance, double lat, double lon) {


                String array[] = new String[5];


                array[0] = sdf2.format(new Date(date));
                array[1] = "\"" + address + "\"";
                array[2] = Integer.toString(distance);
                array[3] = Double.toString(lat);
                array[4] = Double.toString(lon);
                return array;
            }

            //Save the CSV array to a file.
            private boolean writeToFile(java.util.List lines) {
                if (isExternalStorageWritable()) {
                    File dir = new File(Environment.getExternalStorageDirectory(), MapDrawerActivity.dir);
                    if (!dir.exists()) dir.mkdir();
                }


                File file = new File(Environment.getExternalStorageDirectory(), dir + File.separator + fileName);
                try {
                    CSVWriter writer = new CSVWriter(new FileWriter(file));
                    writer.writeAll(lines);
                    writer.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            //Create the intent to email the file.  It gets the default emaila address from the Android
            //system, then attaches the file to that email.
            private void emailFile(String mailAddress) {
                File file = new File(Environment.getExternalStorageDirectory(), dir + File.separator + fileName);
                //File file = new File(context.getFilesDir(), "email.csv");
                if (file.exists()) {
                    Log.v("File URI: ", Uri.fromFile(file).toString());
                    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, mailAddress);
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your Trip Report");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Your Trip Report");
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(emailIntent);

                }
            }

            private void readFromFile() {
                File file = new File(context.getApplicationContext().getFilesDir(), "email.csv");

                try {
                    CSVReader reader = new CSVReader(new FileReader(file));
                    String[] line = null;
                    while ((line = reader.readNext()) != null) {
                        Log.v("CSV: ", Arrays.toString(line));
                    }
                } catch (Exception e) {
                    Log.v("CSV: ", "FILE NOT FOUND");
                    e.printStackTrace();
                }

            }

            private String getEmail() {
                Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
                Account[] accounts = AccountManager.get(context).getAccounts();
                for (Account account : accounts) {
                    if (emailPattern.matcher(account.name).matches()) {
                        String possibleEmail = account.name;
                        if (possibleEmail.contains("gmail")) {
                            return possibleEmail;
                        }
                        Log.v("Email Address: ", possibleEmail);
                    }
                }
                return null;
            }


            /* Checks if external storage is available for read and write */
            public boolean isExternalStorageWritable() {
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    return true;
                }
                return false;
            }

            /* Checks if external storage is available to at least read */
            public boolean isExternalStorageReadable() {
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    return true;
                }
                return false;
            }
        }
    }

    public interface MapHandlerInterface {
        public void disconnect();
        public void connect(GoogleMap map, Context context);
        public void setTripData(List<TripRow> rows);
        public void setHomeData(List<HomePoints> homes);
    }
}
