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

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.ExpandableListAdapter;
import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.webapicalls.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public class ExpandingTripList extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    LinkedHashMap<String, ArrayList<HashMap<String, String>>> listDataChild;
    //private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expanding_trip_list);


        listDataChild = new LinkedHashMap<String, ArrayList<HashMap<String, String>>>();
        listDataHeader = new ArrayList<String>();

        expListView = (ExpandableListView)findViewById(R.id.expanding_view);

        initCursor();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                    Log.d("DEBUG: ", "Child clicked: " + Integer.toString(groupPosition));


                    HashMap<String, String> data = listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition);
                    v.setBackgroundColor(R.drawable.abc_ab_solid_light_holo);
                    return false;
                }
            }
        );

    }

    private void initCursor() {
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
                String group_id = Integer.toString(c.getInt(c.getColumnIndexOrThrow(TripTable.TRIP_KEY)));
                HashMap info = buildSegment(c);
                if (!listDataChild.containsKey(group_id)) {
                    listDataHeader.add(group_id);
                    ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
                    data.add(info);
                    listDataChild.put(group_id, data);
                } else {
                    ArrayList<HashMap<String, String>> data = listDataChild.get(group_id);
                    data.add(info);
                    listDataChild.put(group_id, data);
                }
            }
        }

        Iterator it = listDataChild.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            ArrayList data = (ArrayList)listDataChild.get(key);
            Collections.reverse(data);//Reverse the data so that it shows newest first
        }
        if (c != null) c.close();

    }

    private HashMap<String, String> buildSegment(Cursor c) {
        long date = c.getLong(c.getColumnIndexOrThrow(TripTable.TIME));
        String dateString = format.format(new Date(date));
        double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
        double lon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
        Log.v("EXList Lat: ", Double.toString(lat));
        Log.v("EXList Lon: ", Double.toString(lon));
        String address = c.getString(c.getColumnIndexOrThrow(TripTable.ADDRESS));
        if (address.trim().length() == 0) {
            address = getAddress(lat, lon);
        }

        HashMap<String, String> values = new HashMap<String, String>();
        values.put("address", address);
        values.put("date", dateString);
        return values;
        //return dateString + "\n" + address;
    }

    private String getAddress(double lat, double lon) {
        LocationServices services = new LocationServices(getApplicationContext());
        String address = services.getRoadName(lat, lon);
        Log.v("EXList Address: ", address);
        return address;
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
        protected String doInBackground(String... params) {
            return null;
        }
    }
}
