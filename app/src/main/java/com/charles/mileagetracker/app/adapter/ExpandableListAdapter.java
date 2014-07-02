package com.charles.mileagetracker.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by charles on 6/27/14.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context _context;

    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private LinkedHashMap<String, ArrayList<HashMap<String, String>>> _listDataChild;


    public ExpandableListAdapter(Context context, List<String> listDataHeader,
                                 LinkedHashMap<String, ArrayList<HashMap<String, String>>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }


    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {

        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        //String headerTitle = (String) getGroup(groupPosition);

        String groupId = (String)getGroup(groupPosition);
        ArrayList group = _listDataChild.get(groupId);

        String headerTitle = (String)((HashMap)group.get(group.size() - 1)).get("date");
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.trip_expand_header, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.list_header);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        //final String childText = (String) getChild(groupPosition, childPosition);
        final HashMap child = (HashMap)getChild(groupPosition, childPosition);
        String address = (String)child.get("address");
        String date = (String)child.get("date");

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.trip_list_item, null);
        }

        TextView addressView = (TextView) convertView
                .findViewById(R.id.end_trip_item_address);

        TextView dateView = (TextView) convertView
                .findViewById(R.id.end_trip_date_time);

        addressView.setText(address);
        dateView.setText(date);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
