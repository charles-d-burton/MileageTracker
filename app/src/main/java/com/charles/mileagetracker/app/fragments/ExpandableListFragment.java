package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.charles.mileagetracker.app.fragments.ExpandableListFragment.ExpandableListInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ExpandableListFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ExpandableListFragment extends Fragment {

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;

    private ArrayList<ExpandListGroup> listGroups;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private ProgressDialog mDialog = null;

    private Context context = null;

    private ExpandableListInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ExpandableListFragment.
     */
    public static ExpandableListFragment newInstance(String param1, String param2) {
        ExpandableListFragment fragment = new ExpandableListFragment();
        Bundle args = new Bundle();
        args.putString("test", param1);
        fragment.setArguments(args);
        return fragment;
    }

    public ExpandableListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();

        View view = null;

        if (container == null) {
            view = inflater.inflate(R.layout.fragment_expandable_list, container, false);
        }

        expListView = (ExpandableListView)view.findViewById(R.id.expanding_view);

        listGroups = new ArrayList<ExpandListGroup>();
        listAdapter = new ExpandableListAdapter(this.getActivity(), listGroups);

        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                TextView text = (TextView)v.findViewById(R.id.end_trip_item_address);

                ExpandListChild child = (ExpandListChild)listAdapter.getChild(groupPosition,childPosition);
                if (child.isBusinessRelated() ==1) {
                    child.setBusinessRelated(0);
                } else {
                    child.setBusinessRelated(1);
                }

                Runnable saveChild = new SaveChild(child);
                saveChild.run();
                listAdapter.notifyDataSetChanged();
                mListener.expandListItemTouch(child);
                return false;
            }
        });

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
        return view;
    }

    private void groupLongClick(final ExpandListGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Trip?");
        builder.setMessage("Delete all trips from:" +
                "\n" + group.getName() +
                "\n" + "This operation cannot be undone!");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listGroups.clear();
                context.getContentResolver().delete(TrackerContentProvider.GROUP_URI, TripGroup.GROUP_ID + "=" + group.getGroupId(), null);
                context.getContentResolver().delete(TrackerContentProvider.TRIP_URI, TripTable.TRIP_KEY + "=" + group.getGroupId(), null);
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
        mListener.expandListItemLongTouch(child.getExpandGroup());

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ExpandableListInteractionListener) activity;
            new FillData().execute("");
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /*
    This is a background thread that runs to pull all the trips from the database.  I'm probably going
    to simplify this by making it a runnable
     */

    private class FillData extends AsyncTask<String, String, ExpandListGroup> {

        //Necessary evil so that I can modify the data in the background and then quickly move it into place
        //it's done processing.
        private ArrayList<ExpandListGroup> groups = new ArrayList<ExpandListGroup>();

        //I don't know why this isn't showing :(
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(ExpandableListFragment.this.getActivity());
            mDialog.setMessage("Loading Trips....");
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
        protected ExpandListGroup doInBackground(String... params) {
            ExpandListGroup firstGroup = null;
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

            Cursor c = context.getContentResolver().query(TrackerContentProvider.TRIP_URI,projection, null, null,null);

            //Move backwards through the database, we want the newest data first.
            if (c != null && c.getCount() > 0) {
                c.moveToPosition(c.getCount() + 1);//Move cursor one postion beyond end of list
                while (c.moveToPrevious()) {

                    int group_id = c.getInt(c.getColumnIndexOrThrow(TripTable.TRIP_KEY));
                    double distance = c.getDouble(c.getColumnIndexOrThrow(TripTable.DISTANCE));
                    ExpandListGroup group = getGroup(group_id);
                    ExpandListChild child = buildChild(c);
                    group.addItem(child);
                    if (c.isLast()) firstGroup = group;

                }
            }

            if (c != null) c.close();
            /*
            Since we moved backwards through the database I now want the children to be reveresed
            and show the oldest elements first.
             */
            reverseChildren();
            return firstGroup;
        }

        /*
        Supposed to stop the progressbar
         */
        @Override
        protected void onPostExecute(ExpandListGroup group) {

            //bar.setVisibility(View.INVISIBLE);
            listGroups.clear();
            listGroups.addAll(groups);
            listAdapter.notifyDataSetChanged();
            mListener.expandListItemLongTouch(group);
            mDialog.dismiss();
            //mDialog.hide();

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
            AddressDistanceServices services = new AddressDistanceServices(context);
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
                context.getContentResolver().update(TrackerContentProvider.TRIP_URI,values, TripTable.COLUMN_ID + "=" + child.getId(), null);
            }

        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ExpandableListInteractionListener {
        public void expandListItemTouch(ExpandListChild child);
        public void expandListItemLongTouch(ExpandListGroup group);
        public void switchMap(int map);
    }

}
