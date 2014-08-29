package com.charles.mileagetracker.app.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;
import com.google.android.gms.common.ConnectionResult;
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
        LoaderManager.LoaderCallbacks {

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private LocationManager lm;

    private TextView mileageView = null;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private static ProgressBar bar = null;

    private static ContentResolver resolver;
    private static Context context;

    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        servicesConnected();
        setContentView(R.layout.activity_main);

        Button b = (Button) findViewById(R.id.set_home);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SetHome.class);
                startActivity(intent);
            }
        });

        Button expandTrips = (Button)findViewById(R.id.review_trips);
        expandTrips.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShowLocation.class);
                MainActivity.this.startActivity(intent);
            }
        });

        bar = (ProgressBar)findViewById(R.id.progressBar);
        resolver = getContentResolver();
        context = getBaseContext();

        /*Button generateReport = (Button)findViewById(R.id.gen_report);
        generateReport.setOnClickListener(new View.OnClickListener() {

              @Override
              public void onClick(View v) {
                  Intent intent = new Intent(MainActivity.this, ReportGenerateActivity.class);
                  MainActivity.this.startActivity(intent);
              }
          }

        );*/
        mileageView = (TextView) findViewById(R.id.totalMileageField);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        getLoaderManager().initLoader(0, null, this);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        new CalculateMileage().execute();
    }


    @Override
    public void onDestroy() {
        //stopService(new Intent(this, LocationPingService.class));
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_email) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Log.v("MAIN ACTIVITY LOADER: ", "Loader finished");
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }


    /*
    Google Play Services error handling
     */

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
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

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePicker();
        newFragment.show(getFragmentManager(), "startDatePicker");
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
            c.set(year,monthOfYear,dayOfMonth);
            if (start == 0) {

                start = c.getTimeInMillis();
                Log.v("Start Date Set: " , sdf.format(c.getTime()));

                DialogFragment newFragment = new DatePicker();
                newFragment.show(getFragmentManager(), "endDatePicker");
            } else {

                end = c.getTimeInMillis();
                Log.v("End Date Set: " , sdf.format(c.getTime()));
                new GenerateCSV().execute(start, end);
                start = 0;
                end = 0;
            }

        }
    }

    /*
    Get the results of started activities
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    /*
                     * Try the request again
                     */
                        break;
                }
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
            ((TextView)findViewById(R.id.totalMileageField)).setText(Integer.toString(new Double(totalDistance).intValue()) + " Miles");
        }
    }

    //This queries the database to retrieve the business related trips for a given date range.  It
    //will then start a process to email the results as a CSV

    private static class GenerateCSV extends AsyncTask<Long, Void, String> {

        private final String firstLine = "Date,Address,Distance(mi),Latitude,Longitude\n";

        @Override
        protected void onPreExecute() {
            bar.setVisibility(ProgressBar.VISIBLE);
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
                lines.add(new String[] {"Date", "Address", "Distance Traveled(miles)", "Latitude", "Longitude"});
                while (c.moveToNext()) {
                    if (c.getInt(c.getColumnIndexOrThrow(TripTable.BUSINESS_RELATED)) == 1) {
                        long date = c.getLong(c.getColumnIndexOrThrow(TripTable.TIME));
                        String address = c.getString(c.getColumnIndexOrThrow(TripTable.ADDRESS));
                        int distance = new Double(c.getInt(c.getColumnIndexOrThrow(TripTable.DISTANCE)) * 0.621).intValue();
                        double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
                        double lon  = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
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
            bar.setVisibility(ProgressBar.INVISIBLE);
        }

        private String[] getLine(long date, String address, int distance, double lat, double lon) {


            String array[] = new String[5];


            array[0] = sdf2.format(new Date(date));
            array[1] = "\"" + address + "\"";
            array[2] = Integer.toString(distance );
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
                Log.v("File URI: " , Uri.fromFile(file).toString());
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

        private void readFromFile(){
            File file = new File(context.getApplicationContext().getFilesDir(), "email.csv");

            try {
                CSVReader reader = new CSVReader(new FileReader(file));
                String [] line = null;
                while ((line = reader.readNext()) != null) {
                    Log.v("CSV: " , Arrays.toString(line));
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
                    Log.v("Email Address: " , possibleEmail);
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
