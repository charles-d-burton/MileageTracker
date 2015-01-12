package com.charles.mileagetracker.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by charles on 1/6/15.
 */
public class TripListAdapter extends BaseAdapter {
    private Context mContext;
    private int layoutResourceId;
    private ArrayList<TripRow> tripRows =  new ArrayList<TripRow>();

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    public TripListAdapter(Context mContext, int layoutResourceId) {
        this.mContext = mContext;
        this.layoutResourceId = layoutResourceId;
    }

    @Override
    public int getCount() {
        return tripRows.size();
    }

    @Override
    public TripRow getItem(int position) {
        return tripRows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position,View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        TripRow row = tripRows.get(position);
        TextView dateTime = (TextView)convertView.findViewById(R.id.end_trip_date_time);
        TextView address = (TextView)convertView.findViewById(R.id.end_trip_item_address);
        address.setText(row.address);
        dateTime.setText(format.format(row.timeStart));
        return convertView;
    }

    public void setData(ArrayList<TripRow> tripRows) {
        this.tripRows = tripRows;
    }
}
