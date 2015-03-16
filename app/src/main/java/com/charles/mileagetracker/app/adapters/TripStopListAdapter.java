package com.charles.mileagetracker.app.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.database.orm.TripRow;
import com.charles.mileagetracker.app.processors.TripGroupProcessor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by charles on 1/12/15.
 */
public class TripStopListAdapter extends BaseAdapter {

    private Context mContext;
    private int layoutResourceId;
    private List<TripRow> tripRows =  new ArrayList<TripRow>();

    private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm a yyyy");

    public TripStopListAdapter(Context mContext, int layoutResourceId) {
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

    public List<TripRow> getTripRows() {
        return this.tripRows;
    }

    @Override
    public View getView(int position,View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        TripRow row = tripRows.get(position);
        CheckedTextView checkedTextView = (CheckedTextView)convertView.findViewById(R.id.checkedTextView);
        if (row.businessRelated) {
            checkedTextView.setChecked(true);
        } else {
            checkedTextView.setChecked(false);
        }
        String text = format.format(row.timeStart) + "\n" + row.address;
        checkedTextView.setText(text);
        //checkedTextView.setOnClickListener(new OnListItemClickedListener(checkedTextView, row));
        return convertView;
    }

    public void reloadRows(List<TripRow> tripRows){
        this.tripRows.clear();
        this.tripRows.addAll(tripRows);
    }

    public void clear() {
        tripRows.clear();
    }


    private class OnListItemClickedListener implements CheckedTextView.OnClickListener{
        private CheckedTextView cvt = null;
        private TripRow row = null;

        protected OnListItemClickedListener(CheckedTextView cvt, TripRow row) {
            this.cvt = cvt;
            this.row = row;
        }

        @Override
        public void onClick(View v) {


            if (cvt.isChecked()) {
                cvt.setChecked(false);
                row.businessRelated = false;
                row.save();
                Log.v("Item Clicked: ", row.address + "\n" + Boolean.toString(row.businessRelated));
            } else {
                cvt.setChecked(true);
                row.businessRelated = true;
                row.save();
                Log.v("Item Clicked: ", row.address + "\n" + Boolean.toString(row.businessRelated));
            }

        }
    }
}
