package com.charles.mileagetracker.app.fragments;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.charles.mileagetracker.app.R;

/**
 * Created by charles on 3/31/14.
 */
public class DrawerListFragment extends ListFragment implements LoaderManager.LoaderCallbacks {
    private static View mainView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.drawer_list_view, null);
        mainView =v;
        return v;
    }
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
