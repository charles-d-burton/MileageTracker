package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.ExpandableListAdapter;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.TripGroup;
import com.charles.mileagetracker.app.database.TripTable;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/*
Activity that displays the trip data contained in the database.  It uses an ExpandableListView that
contains two data components.  The parent component that holds the individual trip stops is the
ExpandListGroup class and the child is the ExpandListChild class.
 */
public class ExpandingTripList extends Activity {

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;

    private ArrayList<ExpandListGroup> listGroups;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private ProgressDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expanding_trip_list);
        expListView = (ExpandableListView)findViewById(R.id.expanding_view);

        listGroups = new ArrayList<ExpandListGroup>();
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

                    Runnable saveChild = new SaveChild(child);
                    saveChild.run();
                    listAdapter.notifyDataSetChanged();
                    return false;
                }
            }
        );

        //Long click events for group headers and children
        expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    ExpandListGroup group = (ExpandListGroup)parent.getAdapter().getItem(position);
                    groupLongClick(group);

                } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD){
                    ExpandListChild child = (ExpandListChild)parent.getAdapter().getItem(position);
                    childLongClick(child);
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v("DEBUG: ", "Resuming");
        //listAdapter.notifyDataSetChanged();
        new FillData().execute("");

    }

    @Override
    public void onPause() {
        super.onPause();
        listGroups.clear();
        listAdapter.notifyDataSetChanged();
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
        if (id == R.id.action_email) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void groupLongClick(final ExpandListGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Trip?");
        builder.setMessage("Delete all trips from:" +
                "\n" + group.getName() +
                "\n" + "This operation cannot be undone!");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listGroups.clear();
                getApplicationContext().getContentResolver().delete(TrackerContentProvider.GROUP_URI, TripGroup.GROUP_ID + "=" + group.getGroupId(), null);
                getApplicationContext().getContentResolver().delete(TrackerContentProvider.TRIP_URI, TripTable.TRIP_KEY + "=" + group.getGroupId(), null);
                new FillData().execute("");
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void childLongClick(ExpandListChild child) {
        double lat = child.getLat();
        double lon = child.getLon();
        int id = child.getId();

        Intent intent = new Intent(ExpandingTripList.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("lat",lat);
        intent.putExtra("lon", lon);
        intent.putExtra("id", id);
        intent.putExtra("child", child);
        getApplicationContext().startActivity(intent);


    }



    /*
    This is a background thread that runs to pull all the trips from the database.  I'm probably going
    to simplify this by making it a runnable
     */

    private class FillData extends AsyncTask<String, String, String> {

        //Necessary evil so that I can modify the data in the background and then quickly move it into place
        //it's done processing.
        private ArrayList<ExpandListGroup> groups = new ArrayList<ExpandListGroup>();

        //I don't know why this isn't showing :(
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ExpandingTripList.this);
            mDialog.setMessage("Generating CSV....");
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setIndeterminate(true);
            mDialog.show();
            //bar.setVisibility(View.VISIBLE);
        }


        /*
        Where the real work gets done.  This pulls down the trips from the database and generates the
        children and parent elements.
         */
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

            //Move backwards through the database, we want the newest data first.
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
            /*
            Since we moved backwards through the database I now want the children to be reveresed
            and show the oldest elements first.
             */
            reverseChildren();
            return null;
        }

        /*
        Supposed to stop the progressbar
         */
        @Override
        protected void onPostExecute(String result) {

            //bar.setVisibility(View.INVISIBLE);
            listGroups.clear();
            listGroups.addAll(groups);
            listAdapter.notifyDataSetChanged();
            mDialog.hide();

        }

        /*
        Take a group id and retrieve the groups
         */
        private ExpandListGroup getGroup(int group_id){
            Iterator it = groups.iterator();
            while (it.hasNext()) {
                ExpandListGroup group = (ExpandListGroup)it.next();
                if (group.getGroupId() == group_id) {
                    return group;
                }
            }
            ExpandListGroup group = new ExpandListGroup(group_id);
            groups.add(group);
            return group;//Default to
        }

        /*
        Take the Cursor and pull down the children from one group
         */
        private ExpandListChild buildChild(Cursor c) {
            long millis = c.getLong(c.getColumnIndexOrThrow(TripTable.TIME));
            String dateString = format.format(new Date(millis));
            double lat = c.getDouble(c.getColumnIndexOrThrow(TripTable.LAT));
            double lon = c.getDouble(c.getColumnIndexOrThrow(TripTable.LON));
            double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));
            double miles =  convertKmToMi(distance);
            //Log.v("DEBUG: ", "DISTANCE: " + Double.toString(miles) + "mi");
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
            Iterator it = groups.iterator();
            while (it.hasNext()) {
                ExpandListGroup group = (ExpandListGroup)it.next();
                group.reverseChildren();
                setName(group);
            }
        }

        //Set the name of the group to the first date of a recorded trip
        private void setName(ExpandListGroup group) {
            ExpandListChild child = group.getListChildren().get(0);
            group.setName(child.getDate());
        }

        private String getAddress(double lat, double lon) {
            AddressDistanceServices services = new AddressDistanceServices(getApplicationContext());
            String address = services.getRoadName(lat, lon);
            //Log.v("EXList Address: ", address);
            return address;
        }

        private double convertKmToMi(double kilometers) {
            // Assume there are 0.621 miles in a kilometer.
            double miles = kilometers * 0.621;
            return miles;
        }
    }

    /*
    Updates the status of a child in the background
     */
    private class SaveChild implements Runnable {

        private ExpandListChild child = null;

        public SaveChild(ExpandListChild child) {
            this.child  = child;
        }

        @Override
        public void run() {
            if (child != null) {
                ContentValues values = new ContentValues();
                values.put(TripTable.BUSINESS_RELATED, child.isBusinessRelated());
                getContentResolver().update(TrackerContentProvider.TRIP_URI,values, TripTable.COLUMN_ID + "=" + child.getId(), null);
            }

        }
    }
}
