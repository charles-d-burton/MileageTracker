package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.ExpandableListAdapter;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.webapicalls.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class ExpandingTripList extends Activity {

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private ProgressBar bar;

    private ArrayList<ExpandListGroup> listGroups;
    //private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expanding_trip_list);
        expListView = (ExpandableListView)findViewById(R.id.expanding_view);
        bar = (ProgressBar)findViewById(R.id.progressBar);

        listGroups = new ArrayList<ExpandListGroup>();

        new FillData().execute("");

        listAdapter = new ExpandableListAdapter(this, listGroups);

        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                    TextView text = (TextView)v.findViewById(R.id.end_trip_item_address);
                    //Log.v("DEBUG: ", "FROM CHILD VIEw: " + text.getText());

                    ExpandListChild child = (ExpandListChild)listAdapter.getChild(groupPosition,childPosition);
                    if (child.isBusinessRelated() ==1) {
                        child.setBusinessRelated(0);
                    } else {
                        child.setBusinessRelated(1);
                    }
                    listAdapter.notifyDataSetChanged();
                    //v.setBackgroundColor(R.drawable.abc_ab_solid_light_holo);
                    return false;
                }
            }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.expanding_trip_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class FillData extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            bar.setVisibility(View.VISIBLE);
        }


        @Override
        protected String doInBackground(String... params) {
            String projection[] = {
                    TripTable.DISTANCE,
                    TripTable.TRIP_KEY,
                    TripTable.COLUMN_ID,
                    TripTable.LAT,
                    TripTable.LON,
                    TripTable.BUSINESS_RELATED,
                    TripTable.CLOSED,
                    TripTable.TIME,
                    TripTable.ADDRESS
            };

            Cursor c = getContentResolver().query(TrackerContentProvider.TRIP_URI,projection, null, null,null);

            if (c != null && c.getCount() > 0) {
                c.moveToPosition(c.getCount() + 1);//Move cursor one postion beyond end of list
                while (c.moveToPrevious()) {
                    int group_id = c.getInt(c.getColumnIndexOrThrow(TripTable.TRIP_KEY));
                    double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));
                    ExpandListGroup group = getGroup(group_id);
                    ExpandListChild child = buildChild(c);
                    group.addItem(child);

                }
            }

            if (c != null) c.close();
            reverseChildren();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            listAdapter.notifyDataSetChanged();
            bar.setVisibility(View.INVISIBLE);
        }

        private ExpandListGroup getGroup(int group_id){
            Iterator it = listGroups.iterator();
            while (it.hasNext()) {
                ExpandListGroup group = (ExpandListGroup)it.next();
                if (group.getGroupId() == group_id) {
                    return group;
                }
            }
            ExpandListGroup group = new ExpandListGroup(group_id);
            listGroups.add(group);
            return group;//Default to
        }

        private ExpandListChild buildChild(Cursor c) {
            long millis = c.getLong(c.getColumnIndexOrThrow(TripTable.TIME));
            String dateString = format.format(new Date(millis));
            double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
            double lon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
            double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));
            double miles =  convertKmToMi(distance);
            Log.v("DEBUG: ", "DISTANCE: " + Double.toString(miles) + "mi");
            int businessRelated = c.getInt(c.getColumnIndexOrThrow(TripTable.BUSINESS_RELATED));
            int id = c.getInt(c.getColumnIndexOrThrow(TripTable.COLUMN_ID));
            String address = c.getString(c.getColumnIndexOrThrow(TripTable.ADDRESS));
            int group_id = c.getInt(c.getColumnIndexOrThrow(TripTable.TRIP_KEY));
            if (address.trim().length() == 0) {
                address = getAddress(lat, lon);
            }
            return new ExpandListChild(dateString,millis,id,distance,group_id,lat,lon,businessRelated,address);
        }

        private void reverseChildren() {
            //Reverse the internal lists so the dates are in the correct order.
            Iterator it = listGroups.iterator();
            while (it.hasNext()) {
                ExpandListGroup group = (ExpandListGroup)it.next();
                group.reverseChildren();
                setName(group);
            }
        }

        //Set the name of the group to the first date of a recorded trip
        private void setName(ExpandListGroup group) {
            ExpandListChild child = group.getItems().get(0);
            group.setName(child.getDate());
        }

        private String getAddress(double lat, double lon) {
            LocationServices services = new LocationServices(getApplicationContext());
            String address = services.getRoadName(lat, lon);
            Log.v("EXList Address: ", address);
            return address;
        }

        private double convertKmToMi(double kilometers) {
            // Assume there are 0.621 miles in a kilometer.
            double miles = kilometers * 0.621;
            return miles;
        }
    }
}
