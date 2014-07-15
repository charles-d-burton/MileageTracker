package com.charles.mileagetracker.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;
import com.charles.mileagetracker.app.adapter.containers.ExpandListGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by charles on 6/27/14.
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private ArrayList<ExpandListGroup> groups;


    public ExpandableListAdapter(Context context, ArrayList<ExpandListGroup> groups) {
        this.context = context;
        this.groups = groups;
    }


    @Override
    public int getGroupCount() {
        return this.groups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        ArrayList<ExpandListChild> children = groups.get(groupPosition).getItems();
        return children.size();
    }

    @Override
    public Object getGroup(int groupPosition) {

        return groups.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        ArrayList<ExpandListChild> children = groups.get(groupPosition).getItems();
        return children.get(childPosition);
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
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        ExpandListGroup group = (ExpandListGroup)getGroup(groupPosition);
        String headerTitle = group.getName();

        if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.trip_expand_header, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.list_header);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        ExpandListChild child = (ExpandListChild)getChild(groupPosition, childPosition);

        //if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.trip_list_item, null);
        //}


        if (child.isBusinessRelated() == 1) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.semi_lightblue));
        } else {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        TextView addressView = (TextView) convertView
                .findViewById(R.id.end_trip_item_address);

        TextView dateView = (TextView) convertView
                .findViewById(R.id.end_trip_date_time);

        addressView.setText(child.getAddress());
        dateView.setText(child.getDate());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
