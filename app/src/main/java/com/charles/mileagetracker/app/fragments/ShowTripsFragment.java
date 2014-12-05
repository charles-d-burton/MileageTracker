package com.charles.mileagetracker.app.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.locationservices.AddressDistanceServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMarkerClickListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final String CLASS = ((Object)this).getClass().getName();

    private OnShowTripsInteractionListener mListener;
    private GoogleMap gmap = null;

    private static final String param1 = "group";
    private static final String param2 = "id";

    private TripGroup group = null;

    private boolean mapStart = false;

    private HashMap<Marker, TripRow> markerTracker = new HashMap<Marker,TripRow>();
    private HashMap<TripRow, List<LatLng>> polyLineTracker = new HashMap<TripRow, List<LatLng>>();

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ShowTripsFragment.
     */
    public static ShowTripsFragment newInstance(TripGroup group) {
        ShowTripsFragment fragment = new ShowTripsFragment();
        Bundle bundle = new Bundle();
        //bundle.putSerializable(param1, group);
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
        /*if (getArguments() != null) {
            if (getArguments().containsKey(param1)) {
                group = (ExpandListGroup)getArguments().getSerializable(param1);
            }
        }*/
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        gmap = getMap();
        LocationManager lm = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } else {
            loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        if (loc != null ) {
            gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 12));
        }

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
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {

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

    public void redrawLines(TripGroup group) {
        if (group != null) {
            TripRow child = group.getChildren().get(0);
            double lat = child.lat;
            double lon = child.lon;

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(13)
                    .build();
            if (mapStart) {
                //gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 12));
                mapStart = true;
            } else {
                gmap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

        }
        markerTracker.clear();
        polyLineTracker.clear();
        new DrawLines().execute(group);
    }


    /*
    This class takes a TripGroup and generates the PolyLine points in a background thread.  It then
    puts them on the map after it generates them.
     */
    private class DrawLines extends AsyncTask<TripGroup, Integer, TripGroup> {

        private AddressDistanceServices distanceServices;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            distanceServices = new AddressDistanceServices(ShowTripsFragment.this.getActivity());
            gmap.clear();
        }

        @Override
        protected TripGroup doInBackground(TripGroup... params) {

            TripGroup expandListGroup = params[0];
            ArrayList children = expandListGroup.getChildren();
            for (int i = 0; i < children.size(); i++) {
                if (i == children.size() - 1){
                    break;
                }
                TripRow point1 = (TripRow)children.get(i);
                TripRow point2 = (TripRow)children.get(i + 1);

                //Why do more work than necessary?  The points have already been generated, we need to just skip that part
                /*if (point1.getLinePoints().size() > 0) {
                    continue;
                }*/

                String url = distanceServices.getDirectionsURL(point1.lat, point1.lon, point2.lat, point2.lon);
                String result = distanceServices.getStringFromUrl(url);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray routeArray = json.getJSONArray("routes");
                    JSONObject routes = routeArray.getJSONObject(0);
                    JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
                    String encodedString = overviewPolylines.getString("points");
                    List lines = distanceServices.decodePoly(encodedString);
                    Log.v("Number of lines: " , Integer.toString(lines.size()));
                    polyLineTracker.put(point1, lines);
                    //point1.addAllPoints(lines);
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
        protected void onPostExecute(TripGroup group) {
            super.onPostExecute(group);
            ArrayList<TripRow> children = group.getChildren();
            //SetHomeDrawerAdapter drawerAdapter = new SetHomeDrawerAdapter(ShowTripsFragment.this.getActivity(), children);
            //drawerView.setAdapter(drawerAdapter);
            Iterator it = children.iterator();
            Polyline polyline = null;
            while (it.hasNext()) {

                TripRow child = (TripRow) it.next();

                if (polyline != null && child.businessRelated) {
                    polyline.setColor(Color.GREEN);
                }
                Marker marker = gmap.addMarker(
                        new MarkerOptions().position(new LatLng(child.lat, child.lon))
                                .draggable(false)
                                .title(child.address)
                                .flat(true)

                );

                markerTracker.put(marker, child);
                List<LatLng> points = polyLineTracker.get(child);
                //Log.v("POINTS: ", "Number of points: " + Integer.toString(points.size()));

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
        public void markerAdded();

        //public void onFragmentInteraction(Uri uri);
    }

}
