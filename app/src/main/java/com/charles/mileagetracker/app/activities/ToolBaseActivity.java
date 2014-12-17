package com.charles.mileagetracker.app.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.charles.mileagetracker.app.R;

/**
 * Created by charles on 12/5/14.
 */
public abstract class ToolBaseActivity extends ActionBarActivity {
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            Log.v("TOOLBAR: ", "ToolBar Not Null");
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            Log.v("TOOLBAR: ", "ToolBar NULL");
        }
    }

    protected abstract int getLayoutResource();

    protected void setActionBarIcon(int iconRes) {

        //toolbar.setNavigationIcon(iconRes);
    }
}
