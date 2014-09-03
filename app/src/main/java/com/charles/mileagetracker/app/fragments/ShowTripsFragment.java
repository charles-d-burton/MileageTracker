package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.charles.mileagetracker.app.fragments.ShowTripsFragment.OnShowTripsInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ShowTripsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ShowTripsFragment extends MapFragment implements
        GoogleMap.OnMapLongClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private OnShowTripsInteractionListener mListener;
    private GoogleMap gmap = null;

    private static final String param1 = "group";

    private ExpandListGroup group = null;

    private boolean mapStart = false;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ShowTripsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ShowTripsFragment newInstance(ExpandListGroup group) {
        ShowTripsFragment fragment = new ShowTripsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(param1, group);
        return fragment;
    }

    public static ShowTripsFragment newInstance() {
        ShowTripsFragment fragment = new ShowTripsFragment();
        return fragment;
    }


    public ShowTripsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            group = (ExpandListGroup)getArguments().getSerializable(param1);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        gmap = getMap();
        gmap.setOnMapLongClickListener(this);
        gmap.setOnMarkerClickListener(this);
        gmap.setOnMarkerDragListener(this);
        if (group != null) redrawLines(group);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnShowTripsInteractionListener) activity;
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

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    public void redrawLines(ExpandListGroup group) {
        if (group != null) {
            ExpandListChild child = group.getListChildren().get(0);
            double lat = child.getLat();
            double lon = child.getLon();

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(13)
                    .build();
            if (mapStart) {
                gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mapStart = true;
            } else {
                gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

        }
        new DrawLines().execute(group);
    }

    private class DrawLines extends AsyncTask<ExpandListGroup, Integer, ExpandListGroup> {

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            distanceServices = new AddressDistanceServices(ShowTripsFragment.this.getActivity());
            gmap.clear();
        }

        @Override
        protected ExpandListGroup doInBackground(ExpandListGroup... params) {
            ExpandListGroup expandListGroup = params[0];
            ArrayList children = expandListGroup.getListChildren();

            for (int i = 0; i < children.size(); i++) {
                if (i == children.size() - 1){
                    break;
                }
                ExpandListChild point1 = (ExpandListChild)children.get(i);
                ExpandListChild point2 = (ExpandListChild)children.get(i + 1);
                //Why do more work than necessary?  The points have already been generated, we need to just skip that part
                if (point1.getLinePoints().size() > 0) {
                    continue;
                }

                String url = distanceServices.getDirectionsURL(point1.getLat(), point1.getLon(), point2.getLat(), point2.getLon());
                String result = distanceServices.getStringFromUrl(url);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray routeArray = json.getJSONArray("routes");
                    JSONObject routes = routeArray.getJSONObject(0);
                    JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                    String encodedString = overviewPolylines.getString("points");
                    LinkedList lines = distanceServices.decodePoly(encodedString);
                    Log.v("Number of lines: " , Integer.toString(lines.size()));
                    point1.addAllPoints(lines);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            return expandListGroup;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }


        /*
        Take the group, pull out all of the children and then process all of the points that they posses
        and draw them as a polyline on the screen.
         */
        @Override
        protected void onPostExecute(ExpandListGroup expandListGroup) {
            super.onPostExecute(expandListGroup);
            ArrayList<ExpandListChild> children = expandListGroup.getListChildren();
            //SetHomeDrawerAdapter drawerAdapter = new SetHomeDrawerAdapter(ShowTripsFragment.this.getActivity(), children);
            //drawerView.setAdapter(drawerAdapter);
            Iterator it = children.iterator();
            Polyline polyline = null;
            while (it.hasNext()) {

                ExpandListChild child = (ExpandListChild) it.next();

                if (polyline != null && child.isBusinessRelated() == 1) {
                    polyline.setColor(Color.GREEN);
                }

                gmap.addMarker(
                        new MarkerOptions().position(new LatLng(child.getLat(), child.getLon()))
                                .draggable(false)
                                .title(child.getAddress())
                                .flat(true)

                );
                LinkedList<LatLng> points = child.getLinePoints();
                Log.v("POINTS: ", "Number of points: " + Integer.toString(points.size()));

                if (points.size() > 0) {
                    polyline = gmap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED).geodesic(true));
                }
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
    public interface OnShowTripsInteractionListener {
        // TODO: Update argument type and name
        public void onShowTripInteraction();

        //public void onFragmentInteraction(Uri uri);
    }

}
