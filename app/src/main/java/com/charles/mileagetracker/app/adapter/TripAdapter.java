package com.charles.mileagetracker.app.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.PendingSegmentTable;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by charles on 5/27/14.
 */
public class TripAdapter extends CursorAdapter {

    public TripAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor,flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View retView = inflater.inflate(R.layout.trip_layout, parent, false);
        return retView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView itemAddress = (TextView)view.findViewById(R.id.end_trip_item_address);
        itemAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow(PendingSegmentTable.START_ADDRESS)));

        TextView itemTime = (TextView)view.findViewById(R.id.end_trip_date_time);
        Long time = cursor.getLong(cursor.getColumnIndexOrThrow(PendingSegmentTable.TIME_START));
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        String dateString = format.format(new Date(time));
        itemTime.setText(dateString);

    }
}
