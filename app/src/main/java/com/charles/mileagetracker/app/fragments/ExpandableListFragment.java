package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.ExpandableListAdapter;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private final String CLASS = ((Object)this).getClass().getName();

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;

    private ArrayList<TripGroup> listGroups;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    private ProgressDialog mDialog = null;

    private Context context = null;

    private ExpandableListInteractionListener mListener;

    private int lastLoadedItemId = 0;
    private int numberLoaded = 0;
    private final int LOAD_NUMBER = 20;

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

        listGroups = new ArrayList<TripGroup>();
        listAdapter = new ExpandableListAdapter(this.getActivity(), listGroups);

        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                TextView text = (TextView)v.findViewById(R.id.end_trip_item_address);

                TripRow child = (TripRow)listAdapter.getChild(groupPosition,childPosition);
                if (child.businessRelated) {
                    child.businessRelated = false;
                    child.save();
                } else {
                    child.businessRelated = true;
                    child.save();
                }

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
                    TripGroup group = (TripGroup)parent.getAdapter().getItem(position);
                    groupLongClick(group);

                } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD){
                    TripRow child = (TripRow)parent.getAdapter().getItem(position);
                    childLongClick(child);
                }
                return false;
            }
        });

        expListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.v(CLASS, "Scrolling");
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.v(CLASS, "Num Items Visible: " + Integer.toString(visibleItemCount));
                Log.v(CLASS, "First Visible Item: " + Integer.toString(firstVisibleItem));
                Log.v(CLASS, "Total Items: " + Integer.toString(totalItemCount));
            }
        });

        /*expListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (expListView.isGroupExpanded(groupPosition)) {
                    expListView.collapseGroup(groupPosition);
                } else {
                    expListView.expandGroup(groupPosition, true);
                }

                ExpandListGroup group = (ExpandListGroup) parent.getAdapter().getItem(groupPosition);
                mListener.expandListGroupTouch(group);
                return false;
            }
        });*/
        return view;
    }

    private void groupLongClick(final TripGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Trip?");
        builder.setMessage("Delete all trips from:" +
                "\n" + getNameFromGroup(group) +
                "\n" + "This operation cannot be undone!");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //listGroups.clear();
                for (TripRow child : group.getChildren()) {
                    child.delete();
                }
                group.delete();
                new FillData(getActivity()).execute();
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

    private String getNameFromGroup(TripGroup group) {
        Date date = group.getChildren().get(0).timeStart;
        return format.format(date);
    }


    private void childLongClick(TripRow child) {
        double lat = child.lat;
        double lon = child.lon;
        int id = child.getId().intValue();
        mListener.expandListItemLongTouch(child.trip_group);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ExpandableListInteractionListener) activity;

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new FillData(getActivity()).execute(lastLoadedItemId);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /*
    This is a background thread that runs to pull all the trips from the database.  Provides a
    dialog that waits for everything to load.
     */

    private class FillData extends AsyncTask<Integer, String, TripGroup> {

        //Necessary evil so that I can modify the data in the background and then quickly move it into place
        //it's done processing.
        private ArrayList<TripGroup> groups = new ArrayList<TripGroup>();
        private Context context;

        //I don't know why this isn't showing :(

        public FillData(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
            mDialog = new ProgressDialog(context);
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
        protected TripGroup doInBackground(Integer... params) {

            //TripGroup group = null;
            List<TripGroup> tripGroups = TripGroup.find(TripGroup.class, null, null, null, " id DESC ", null);
            for (TripGroup group : tripGroups) {
                String entries[] = {Long.toString(group.getId())};
                List<TripRow> rows = TripRow.find(TripRow.class, " trip_group = ? ", entries, null, " id ASC", null);
                for (TripRow row : rows) {
                    if (row.distance != 0 && row.units.equalsIgnoreCase("km")) {
                        row.distance = convertKmToMi(row.distance);
                        row.units = "mi";
                        row.save();
                    }
                }
                group.setChildren(rows);
            }
            if (tripGroups.isEmpty()) {
                return null;
            }
            return tripGroups.get(0);
        }

        /*
        Supposed to stop the progressbar
         */
        @Override
        protected void onPostExecute(TripGroup group) {
            //bar.setVisibility(View.INVISIBLE);
            listGroups.clear();
            listGroups.addAll(groups);
            listAdapter.notifyDataSetChanged();
            mListener.expandListItemLongTouch(group);
            mDialog.dismiss();
            //mDialog.hide();

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

        private double convertMitoKM(double miles ) {
            double km = miles * 1.60934;
            return km;
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
        public void expandListItemTouch(TripRow child);
        public void expandListItemLongTouch(TripGroup group);
        public void expandListGroupTouch(TripGroup group);
    }

}
