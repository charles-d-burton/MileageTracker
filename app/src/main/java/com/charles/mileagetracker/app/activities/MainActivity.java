package com.charles.mileagetracker.app.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.fragments.ExpandableListFragment;
import com.charles.mileagetracker.app.fragments.SetHomeFragment;
import com.charles.mileagetracker.app.fragments.ShowTripsFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;

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

public class MainActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        ExpandableListFragment.ExpandableListInteractionListener,
        SetHomeFragment.OnShowHomeInteractionListener,
        ShowTripsFragment.OnShowTripsInteractionListener{

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private final static String TITLE = "Mileage Tracker";

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private static ContentResolver resolver;
    private static Context context;

    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.csv";

    private ActionBarDrawerToggle drawerToggle = null;

    private double lat;
    private double lon;
    private int id;

    private Button startPointButton = null;

    private FrameLayout containerLayout = null;
    private ExpandableListFragment drawerFragment = null;
    private DrawerLayout drawerLayout = null;
    private ShowTripsFragment showTripFragment = null;
    private SetHomeFragment showHomesFragment = null;
    private static ProgressDialog csvDialog = null;

    public static final int MAP_SHOW_TRIPS = 0;
    public static final int MAP_SHOW_HOMES = 1;
    private int CURRENT_MAP = MAP_SHOW_TRIPS;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        context = this;
        resolver = context.getContentResolver();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        containerLayout = (FrameLayout) findViewById(R.id.map_container);
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        showTripFragment = ShowTripsFragment.newInstance();
        transaction.add(R.id.map_container, showTripFragment);
        transaction.commit();
        manager.executePendingTransactions();

        startPointButton = (Button) findViewById(R.id.add_start_point);
        startPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CURRENT_MAP == MAP_SHOW_TRIPS) {
                    drawerLayout.closeDrawers();
                    switchMap(MAP_SHOW_HOMES);
                }
            }
        });

        drawerFragment = (ExpandableListFragment) getFragmentManager().findFragmentById(R.id.drawer_view_map);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        drawerToggle = new ActionBarDrawerToggle(
                this,                             /* host Activity */
                drawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_navigation_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_closed  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                getActionBar().setTitle("Title");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(TITLE);

            }
        };
        drawerLayout.setDrawerListener(drawerToggle);


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new CalculateMileage().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_email) {
            showDatePickerDialog();
            return true;
        }
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void expandListItemTouch(ExpandListChild child) {
        if (child != null && showTripFragment != null) {
            if (CURRENT_MAP != MAP_SHOW_TRIPS) switchMap(MAP_SHOW_TRIPS);
            showTripFragment.redrawLines(child.getExpandGroup());//Redraw to update colors
            new CalculateMileage().execute();
        }
    }

    @Override
    public void expandListItemLongTouch(ExpandListGroup group) {
        drawerLayout.closeDrawer(drawerFragment.getView());
        if (CURRENT_MAP != MAP_SHOW_TRIPS) switchMap(MAP_SHOW_TRIPS);
        if (showTripFragment != null && group != null) {
            showTripFragment.redrawLines(group);
        }
    }

    @Override
    public void expandListGroupTouch(ExpandListGroup group) {
        if (CURRENT_MAP != MAP_SHOW_TRIPS) switchMap(MAP_SHOW_TRIPS);
        if (showTripFragment != null && group != null) {
            showTripFragment.redrawLines(group);
        }
    }

    public void switchMap(int map) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        switch (map) {
            case MAP_SHOW_HOMES:
                CURRENT_MAP = MAP_SHOW_HOMES;
                showHomesFragment = SetHomeFragment.newInstance();
                transaction.replace(R.id.map_container, showHomesFragment);
                transaction.commit();
                fragmentManager.executePendingTransactions();

                break;
            case MAP_SHOW_TRIPS:
                CURRENT_MAP = MAP_SHOW_TRIPS;
                showTripFragment = ShowTripsFragment.newInstance();
                transaction.replace(R.id.map_container, showTripFragment);
                transaction.commit();
                fragmentManager.executePendingTransactions();
                break;
            default:
                throw new IllegalStateException("Unknown Map Type");
        }
    }

    @Override
    public void onShowHomeInteraction() {

    }

    @Override
    public void onShowTripInteraction() {

    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error code
            int errorCode = resultCode;
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(),
                        "Location Updates");
            }
        }
        return false;
    }

    public void showDatePickerDialog() {
        DialogFragment newFragment = new DatePicker();
        newFragment.show(getFragmentManager(), "startDatePicker");
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog errorDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            errorDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            errorDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return errorDialog;
        }
    }

    //This adds up all the mileage so far marked as business related and then sets the TextVIew
    //to reflect that.
    private class CalculateMileage extends AsyncTask<Void, Void, Void> {
        private double totalDistance = 0.0;
        @Override
        protected Void doInBackground(Void... params) {
            String projection[] = {
                    TripTable.COLUMN_ID,
                    TripTable.DISTANCE,
                    TripTable.BUSINESS_RELATED
            };
            Cursor c = getContentResolver().query(TrackerContentProvider.TRIP_URI, projection, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    int business = c.getInt(c.getColumnIndexOrThrow(TripTable.BUSINESS_RELATED));
                    if (business == 1) {

                        double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));

                        totalDistance = totalDistance + (distance * 0.621);//Convert to mileage
                        //Log.v("DISTANCE: " , Double.toString(totalDistance));
                    }
                }
            }
            if (c != null) c.close();
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

        private static class GenerateCSV extends AsyncTask<Long, Void, String> {

            private final String firstLine = "Date,Address,Distance(mi),Latitude,Longitude\n";

            @Override
            protected void onPreExecute() {
                csvDialog = new ProgressDialog(context);
                csvDialog.setMessage("Generating CSV....");
                csvDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                csvDialog.setIndeterminate(true);
                csvDialog.show();
                //bar.setVisibility(ProgressBar.VISIBLE);
            }


            @Override
            protected String doInBackground(Long... params) {
                long start = params[0];
                long end = params[1];
                String projection[] = {
                        TripTable.ADDRESS,
                        TripTable.BUSINESS_RELATED,
                        TripTable.DISTANCE,
                        TripTable.COLUMN_ID,
                        TripTable.LAT,
                        TripTable.LON,
                        TripTable.TIME
                };
                Cursor c = resolver.query(TrackerContentProvider.TRIP_URI, projection, TripTable.TIME + " BETWEEN " + start + " AND " + end, null, null);
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
                emailFile(mailAddress);
                //readFromFile();

                return null;
            }

            @Override
            protected void onPostExecute(String csv) {
                super.onPostExecute(csv);
                csvDialog.dismiss();
                //bar.setVisibility(ProgressBar.INVISIBLE);
            }

            private String[] getLine(long date, String address, int distance, double lat, double lon) {


                String array[] = new String[5];


                array[0] = sdf2.format(new Date(date));
                array[1] = "\"" + address + "\"";
                array[2] = Integer.toString(distance);
                array[3] = Double.toString(lat);
                array[4] = Double.toString(lon);
                return array;


            /*line = line + dateString + ",";
            line = line + address + ",";
            line = line + (Double.toString(distance * 0.621)) + ",";//Convert to miles
            line = line + Double.toString(lat) + ",";
            line = line + Double.toString(lon) + "\n";
            Log.v("CSV Line: " , line);*/
                //return line;
            }

            private boolean writeToFile(List<String[]> lines) {
                if (isExternalStorageWritable()) {
                    File dir = new File(Environment.getExternalStorageDirectory(), MainActivity.dir);
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

}
