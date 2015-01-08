package com.charles.mileagetracker.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;

import java.text.SimpleDateFormat;

/**
 * Created by charles on 1/6/15.
 */
public class TripListAdapter extends ArrayAdapter<TripRow> {
    private Context mContext;
    private int layoutResourceId;
    private TripRow[] tripRows;

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    public TripListAdapter(Context mContext, int layoutResourceId, TripRow[] tripRows) {
        super(mContext, layoutResourceId, tripRows);
        this.mContext = mContext;
        this.layoutResourceId = layoutResourceId;
        this.tripRows = tripRows;
    }

    @Override
    public View getView(int position,View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        TripRow row = tripRows[position];
        TextView item = (TextView)convertView.findViewById(R.id.list_header);
        item.setText(format.format(row.timeStart));
        return convertView;
    }
}
