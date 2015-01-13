package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.TripListAdapter;
import com.charles.mileagetracker.app.adapter.TripStopListAdapter;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;

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
        String entries[] = {Long.toString(group.getId())};
        List<TripRow> rows = TripRow.find(TripRow.class, "tgroup = ? ", entries, null, null, null);
        numStopView.setText("Stops: " + Integer.toString(rows.size()));
        double miles = 0;
        for (TripRow row : rows) {
            if (row.businessRelated) {
                miles = miles + row.distance;
            }
        }
        mileageView.setText("Miles: " + Double.toString((miles)));
        adapter.reloadRows(rows);
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
        public void onStopInteraction(TripRow row);
    }

}
