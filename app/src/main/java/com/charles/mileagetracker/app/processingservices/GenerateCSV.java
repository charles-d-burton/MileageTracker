package com.charles.mileagetracker.app.processingservices;

//This queries the database to retrieve the business related trips for a given date range.  It
//will then start a process to email the results as a CSV

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.orm.query.Condition;
import com.orm.query.Select;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class GenerateCSV extends AsyncTask<Long, Void, String> {

    private final String firstLine = "Date,Address,Distance(mi),Latitude,Longitude\n";
    private Context context;
    private ProgressDialog csvDialog;
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");
    protected static final String dir = "MileageTracker";
    protected static final String fileName = "TripReport.csv";

    public GenerateCSV(Context context) {
        this.context = context;
    }

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
        //List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
        List lines = new ArrayList<String[]>();
        lines.add(new String[]{"Date", "Address", "Distance Traveled(miles)", "Latitude", "Longitude"});
        /*if (groups != null && !groups.isEmpty()) {
            for (TripGroup group : groups) {
                String entries[] = {Long.toString(group.getId())};
                List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);

            }
        }*/
        String entries[] = {Long.toString(start), Long.toString(end)};
        List<TripRow> rows = TripRow.find(TripRow.class, "time_start BETWEEN ? AND ? ", entries, null, "id DESC", null);
        //If nothing is found, continue no further
        if (rows == null || rows.isEmpty() || rows.size() < 2) {
            return null;
        }

        Log.v("CSV: Num Rows: ", Integer.toString(rows.size()));
        Iterator<TripRow> it = rows.iterator();
        TripRow lastRow = it.next();
        while (it.hasNext()) {

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
        if (csv == null ) {
            csvDialog.dismiss();
            Toast.makeText(context, "No Data Found", Toast.LENGTH_LONG).show();
        } else {
            csvDialog.dismiss();
            Toast.makeText(context, "Email Sent", Toast.LENGTH_LONG).show();
        }

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
            File dir = new File(Environment.getExternalStorageDirectory(), this.dir);
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
