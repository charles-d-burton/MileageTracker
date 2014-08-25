package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;
import com.charles.mileagetracker.app.fragments.ExpandableListFragment;
import com.charles.mileagetracker.app.fragments.SetHomeFragment;
import com.charles.mileagetracker.app.fragments.ShowTripsFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;

public class ShowLocation extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        ExpandableListFragment.ExpandableListInteractionListener,
        SetHomeFragment.OnShowHomeInteractionListener,
        ShowTripsFragment.OnShowTripsInteractionListener{

    private double lat;
    private double lon;
    private int id;

    private LinearLayout containerLayout = null;
    private ExpandableListFragment drawerFragment = null;
    private DrawerLayout drawerLayout = null;
    private ShowTripsFragment showTripFragment = null;
    private SetHomeFragment showHomesFragment = null;

    public static final int MAP_SHOW_TRIPS = 0;
    public static final int MAP_SHOW_HOMES = 1;
    private int CURRENT_MAP = MAP_SHOW_TRIPS;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        containerLayout = (LinearLayout) findViewById(R.id.map_container);
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        showTripFragment = ShowTripsFragment.newInstance();
        transaction.add(R.id.map_container, showTripFragment);
        transaction.commit();
        manager.executePendingTransactions();

        drawerFragment = (ExpandableListFragment) getFragmentManager().findFragmentById(R.id.drawer_view_map);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.show_location, menu);
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
    public void expandListItemTouch(ExpandListChild child) {
        if (child != null && showTripFragment != null) {
            showTripFragment.redrawLines(child.getExpandGroup());
        }
    }

    @Override
    public void expandListItemLongTouch(ExpandListGroup group) {
        drawerLayout.closeDrawer(drawerFragment.getView());
        if (showTripFragment != null && group != null) {
            Log.v("Redrawing Lines: ", group.getName());
            showTripFragment.redrawLines(group);
        } else {
            Log.v("Redrawing Lines: ", "Something is null");
        }
    }

    @Override
    public void switchMap(int map) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        switch (map) {
            case MAP_SHOW_HOMES:
                showHomesFragment = SetHomeFragment.newInstance();
                transaction.replace(R.id.map_container, showHomesFragment);
                transaction.commit();
                fragmentManager.executePendingTransactions();

                break;
            case MAP_SHOW_TRIPS:
                showTripFragment = ShowTripsFragment.newInstance();
                transaction.replace(R.id.map_container, showTripFragment);
                transaction.commit();
                fragmentManager.executePendingTransactions();
                break;
            default:
                throw new IllegalStateException("Unknown Map Type");
        }
    }

    @Override
    public void onShowHomeInteraction() {

    }

    @Override
    public void onShowTripInteraction() {

    }

}
