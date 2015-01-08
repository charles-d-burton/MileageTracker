package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.charles.mileagetracker.app.fragments.TripFragment.OnTripFragmentInteraction} interface
 * to handle interaction events.
 * Use the {@link TripFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripFragment extends Fragment {

    private ListView tripList = null;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnTripFragmentInteraction mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TripFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TripFragment newInstance(String param1, String param2) {
        TripFragment fragment = new TripFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public TripFragment() {
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
            view = inflater.inflate(R.layout.fragment_trip, container, false);
        }
        tripList = (ListView)view.findViewById(R.id.trip_list);
        // Inflate the layout for this fragment
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction();
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
        new LoadTripData(getActivity()).execute(null, null, null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class LoadTripData extends AsyncTask<Void, Void, Void> {
        private ProgressDialog loadingDialog;
        private Context context;

        public LoadTripData(Context context) {
            this.context = context;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListener.onTripFragmentStartLoad();
            /*Log.v("Loading Data: ", "Started");
            if (loadingDialog != null) {
                loadingDialog.dismiss();
                loadingDialog.cancel();
                loadingDialog = null;
            }
            loadingDialog = new ProgressDialog(context);
            loadingDialog.setMessage("Loading Trips....");
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setIndeterminate(true);
            loadingDialog.show();*/
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<TripGroup> groupsList = TripGroup.listAll(TripGroup.class);
            for (TripGroup group :groupsList) {
                //Temporary until I can resolve the issue with the ORM
                ArrayList<TripRow> rowList = new ArrayList();
                //Log.v("GROUP: ", Long.toString(tgroup.getId()));
                String entries[] = {Long.toString(group.getId())};
                //List<TripRow> rows = TripRow.listAll(TripRow.class);
                List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, " id ASC", null);
                //List<TripRow> rows = TripRow.find(TripRow.class, "trip_group = ? ORDER BY id DESC", entries);
                for (TripRow row: rows) {
                    if (row.tgroup.getId() == group.getId()) {
                        rowList.add(row);
                        if (row.address == null){
                            row = getAddress(row);
                        }
                    }
                }
                //Also temporary while I figure out the ORM problem
                Collections.sort(rows, new Comparator<TripRow>() {

                    @Override
                    public int compare(TripRow lhs, TripRow rhs) {
                        if (lhs.getId() == rhs.getId()) {
                            return 0;
                        }
                        return lhs.getId() < rhs.getId() ? -1 :1;

                    }
                });

                for (TripRow row : rows){
                    Log.v("Address: ", row.address);
                };
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //loadingDialog.dismiss();
            mListener.onTripFragmentFinishLoad();
        }

        private TripRow getAddress(TripRow row) {
            AddressDistanceServices addressDistanceServices = new AddressDistanceServices(TripFragment.this.getActivity());
            try {
                String address = addressDistanceServices.getAddressFromLatLng(new LatLng(row.lat, row.lon));
                row.address = address;
                row.save();
            } catch (IOException ioe) {

            }
            return row;
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
        // TODO: Update argument type and name
        public void onFragmentInteraction();
        public void onTripFragmentStartLoad();
        public void onTripFragmentFinishLoad();
    }

}
