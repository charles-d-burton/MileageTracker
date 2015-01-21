package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.TripStopListAdapter;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.charles.mileagetracker.app.fragments.TripStopsFragment.OnStopInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TripStopsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripStopsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private ListView list = null;
    private TripStopListAdapter adapter = null;
    private TextView numStopView = null;
    private TextView mileageView = null;
    private ProgressBar loadingBar;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnStopInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TripStopsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TripStopsFragment newInstance(String param1, String param2) {
        TripStopsFragment fragment = new TripStopsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TripStopsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = null;
        if (container == null) {
            view = inflater.inflate(R.layout.fragment_trip_stops, container, false);
        }
        numStopView = (TextView)view.findViewById(R.id.num_stops);
        mileageView = (TextView)view.findViewById(R.id.num_miles);

        adapter = new TripStopListAdapter(this.getActivity(), R.layout.trip_stop_list_item);
        list = (ListView)view.findViewById(R.id.trip_stop_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new OnListItemClickListener());
        list.setOnItemLongClickListener(new OnItemLongPressListener());
        loadingBar = (ProgressBar)view.findViewById(R.id.marker_progress);


        // Inflate the layout for this fragment
        return view;
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnStopInteractionListener) activity;
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

    public void setData(TripRow row) {
        TripGroup group = row.tgroup;
        setData(group);
    }

    public void setData(TripGroup group) {

        new LoadData().execute(group);


    }

    //Manage the check box and refresh the data
    private class OnListItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CheckedTextView cvt = (CheckedTextView)view.findViewById(R.id.checkedTextView);
            TripRow row = adapter.getItem(position);
            TripGroup group = row.tgroup;
            if (cvt.isChecked()) {
                cvt.setChecked(false);
                row.businessRelated = false;
                row.save();
                //Log.v("Item Clicked: ", row.address + "\n" + Boolean.toString(row.businessRelated));
            } else {
                cvt.setChecked(true);
                row.businessRelated = true;
                row.save();
                //Log.v("Item Clicked: ", row.address + "\n" + Boolean.toString(row.businessRelated));
            }

            mListener.onStopClicked(adapter.getTripRows());
            Log.v("Item Clicked", "CLICK");
        }
    }

    private class OnItemLongPressListener implements AdapterView.OnItemLongClickListener {
        //TODO: Add code to delete item here with a Dialog
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

            Log.v("Item Long Press: ",  "PRESS");
            return false;
        }
    }

    private class LoadData extends AsyncTask<TripGroup, Void, List<TripRow>> implements
            TripGroupProcessor.GroupProcessorInterface {

        private double miles =0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected List<TripRow> doInBackground(TripGroup... params) {
            TripGroup group = params[0];

            TripGroupProcessor processor = new TripGroupProcessor(TripStopsFragment.this.getActivity().getApplicationContext(), this);
            //processor.processTripGroup(group);

            String entries[] = {Long.toString(group.getId())};
            List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
            processor.processTripGroup(rows);

            miles = group.billableMileage;
            return rows;
        }

        @Override
        protected void onPostExecute(List<TripRow> tripRows) {
            super.onPostExecute(tripRows);
            numStopView.setText("Stops: " + Integer.toString(tripRows.size()));
            mileageView.setText("Miles: " + Double.toString((miles)));
            adapter.reloadRows(tripRows);
            adapter.notifyDataSetInvalidated();
            adapter.notifyDataSetChanged();
            loadingBar.setVisibility(ProgressBar.INVISIBLE);
            mListener.tripStopsDataLoaded(tripRows);

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        public void finishedGroupProcessing(List<TripRow> rows) {

        }

        @Override
        public void unableToProcessGroup(int failCode) {

        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnStopInteractionListener {
        // TODO: Update argument type and name
        public void onStopClicked(List<TripRow> rows);
        public void onStopLongPress(TripRow row);
        public void tripStopsDataLoaded(List<TripRow> rows);
    }

}
