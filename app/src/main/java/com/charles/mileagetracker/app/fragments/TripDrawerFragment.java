package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapters.TripListAdapter;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processors.AddressDistanceServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TripDrawerFragment.OnTripFragmentInteraction} interface
 * to handle interaction events.
 * Use the {@link TripDrawerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripDrawerFragment extends Fragment {

    private ListView tripList = null;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ProgressBar loadingBar;

    private OnTripFragmentInteraction mListener;
    private TripListAdapter adapter = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TripFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TripDrawerFragment newInstance(String param1, String param2) {
        TripDrawerFragment fragment = new TripDrawerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TripDrawerFragment() {
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
            view = inflater.inflate(R.layout.trip_drawer_fragment, container, false);
        }
        loadingBar = (ProgressBar)view.findViewById(R.id.trip_load_progress);
        adapter = new TripListAdapter(this.getActivity(), R.layout.trip_list_item);
        tripList = (ListView)view.findViewById(R.id.trip_list);
        tripList.setAdapter(adapter);
        tripList.setOnItemClickListener(new OnListItemClickedListener());
        tripList.setOnItemLongClickListener(new OnListItemLongPressListener());
        // Inflate the layout for this fragment
        new LoadTripData().execute();
        this.setRetainInstance(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onDrawerFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTripFragmentInteraction) activity;
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

    private class OnListItemClickedListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            TripRow row = adapter.getItem(position);
            if (row.tgroup == null) {
                new LoadTripData().execute();
            }
            mListener.onDrawerFragmentItemTouch(row);
            //Log.v("Adapter Click Address:", row.address);

        }
    }

    private class OnListItemLongPressListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            TripGroup group = adapter.getItem(position).tgroup;
            TripRow row = adapter.getItem(position);
            mListener.onDrawerItemLongPress(row, TripDrawerFragment.this);
            return true;
        }
    }

    public void tripDeleted(TripRow row) {
        adapter.getAllRows().remove(row);
        adapter.notifyDataSetChanged();
        row.tgroup.delete();
    }

    private class LoadTripData extends AsyncTask<Void, Float, Void>{
        private ProgressDialog loadingDialog;
        private ArrayList<TripRow> listRows = new ArrayList();
        private AddressDistanceServices addressDistanceServices = null;
        private Integer maxValue = 0;

        //Start a Loading dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingBar.setVisibility(ProgressBar.VISIBLE);
            /*if (showLoadingDialog) {
                mListener.onDrawerFragmentStartLoad();
            }*/
        }

        /*
        Load up the list of groups, then select the data from the ORM that is related to each group.
        Take that data and load the first row into a storage object then send that to the Adapter and
        notify that the data set has been changed.
         */
        @Override
        protected Void doInBackground(Void... params) {
            addressDistanceServices = new AddressDistanceServices(TripDrawerFragment.this.getActivity().getApplicationContext());
            //List<TripGroup> groupsList = TripGroup.listAll(TripGroup.class);
            List<TripGroup> groupsList = TripGroup.find(TripGroup.class, null, null, null, "id DESC", null);
            maxValue = groupsList.size();
            Log.v("Address: ", "Trip Group Size=" + Integer.toString(groupsList.size()));
            float counter = 1f;
            for (TripGroup group : groupsList) {
                onProgressUpdate(counter);
                counter = counter + 1;
                String entries[] = {Long.toString(group.getId())};

                List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC LIMIT 1", null);
                if (rows != null && rows.size() ==1) {
                    TripRow row = rows.get(0);
                    String address = row.address;
                    if (address == null || address.trim().length() == 0) {
                        try {
                            address = addressDistanceServices.getAddressFromLatLng(new LatLng(row.lat, row.lon));
                            row.address = address;
                            row.save();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    listRows.add(row);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
            Float update = values[0];
            Log.v("Update %: ", update.toString());
            Integer percent = (int)((update/maxValue)*100);
            //mListener.onDrawerFragmentProgressUpdate(percent);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //loadingDialog.dismiss();
            //Log.v("Address Post Execute List Size: ", Integer.toString(listRows.size()));
            adapter.setData(listRows);
            adapter.notifyDataSetChanged();
            loadingBar.setVisibility(ProgressBar.INVISIBLE);
            /*if (mListener != null) {
                mListener.onDrawerFragmentFinishedLoad();
            }*/
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
    public interface OnTripFragmentInteraction {
        public void onDrawerFragmentInteraction();
        public void onDrawerFragmentStartLoad();
        public void onDrawerFragmentProgressUpdate(Integer tripNum);
        public void onDrawerFragmentFinishedLoad();
        public void onDrawerFragmentItemTouch(TripRow row);
        public void onDrawerItemLongPress(TripRow row, TripDrawerFragment callback);
    }

}
