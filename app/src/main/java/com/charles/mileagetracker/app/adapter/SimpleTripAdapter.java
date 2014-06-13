package com.charles.mileagetracker.app.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import java.text.SimpleDateFormat;

/**
 * Created by charles on 5/27/14.
 */
public class SimpleTripAdapter extends SimpleCursorAdapter {

    private Context context;

    private int layout;

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

    public SimpleTripAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
        this.layout = layout;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        /*Cursor c = getCursor();
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout,parent, false);

        String endAddress = c.getString(c.getColumnIndexOrThrow(PendingSegmentTable.END_ADDRESS));
        long endTime = c.getLong(c.getColumnIndexOrThrow(PendingSegmentTable.TIME_END));

        String endDate = format.format(new Date(endTime));


        TextView endAddressView = (TextView)v.findViewById(R.id.end_trip_item_address);
        if (endAddressView != null) {
            endAddressView.setText(endAddress);
        } else {
            Log.v("DEBUG: ", "EndAdressView null in newView");
        }

        TextView endTimeView = (TextView)v.findViewById(R.id.end_trip_date_time);
        if (endTimeView != null) {
            endTimeView.setText(endDate);
        } else {
            Log.v("DEBUG: ", "EndTimeView null in newView");
        }

        return v;*/
        return null;
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {

        /*String endAddress = cursor.getString(cursor.getColumnIndexOrThrow(PendingSegmentTable.END_ADDRESS));
        long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(PendingSegmentTable.TIME_END));

        String endDate = format.format(new Date(endTime));

        TextView endAddressView = (TextView)v.findViewById(R.id.end_trip_item_address);
        if (endAddressView != null) {
            endAddressView.setText(endAddress);
        } else {
            Log.v("DEBUG: ", "EndAdressView null in bindView");
        }

        TextView endTimeView = (TextView)v.findViewById(R.id.end_trip_date_time);
        if (endTimeView != null) {
            endTimeView.setText(endDate);
        } else {
            Log.v("DEBUG: ", "EndTimeView null in bindView");
        }*/

    }

    /*@Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return super.runQueryOnBackgroundThread(constraint);
    }*/
}
