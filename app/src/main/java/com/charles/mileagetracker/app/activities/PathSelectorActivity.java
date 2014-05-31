package com.charles.mileagetracker.app.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.SimpleTripAdapter;
import com.charles.mileagetracker.app.database.PendingSegmentTable;
import com.charles.mileagetracker.app.database.TrackerContentProvider;

public class PathSelectorActivity extends ListActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    private static final int LOADER_ID = 4;
    private SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_path_selector);
        fillData();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.path_selector, menu);
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
    public Loader onCreateLoader(int id, Bundle args) {
        Uri uri = TrackerContentProvider.PENDING_URI;
        String[] projection = {PendingSegmentTable.START_ADDRESS, PendingSegmentTable.TIME_START,
                PendingSegmentTable.END_ADDRESS, PendingSegmentTable.TIME_END, PendingSegmentTable.START_LAT,
                PendingSegmentTable.START_LON, PendingSegmentTable.END_LAT, PendingSegmentTable.END_LON,
                PendingSegmentTable.COLUMN_ID, PendingSegmentTable.TOTAL_DISTANCE, PendingSegmentTable.TOTAL_TIME};

        return new CursorLoader(this, uri,projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID:
                mAdapter.swapCursor(data);
                break;

        }

    }

    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }

    private void fillData() {
        String[] from = new String[] {PendingSegmentTable.END_ADDRESS, PendingSegmentTable.TIME_END};

        int [] to = new int[] {R.id.end_trip_item_address, R.id.end_trip_date_time};

        getLoaderManager().initLoader(LOADER_ID, null, this);
        mAdapter = new SimpleTripAdapter(this, R.layout.trip_layout, null, from, to, 0);
        setListAdapter(mAdapter);
    }
}
