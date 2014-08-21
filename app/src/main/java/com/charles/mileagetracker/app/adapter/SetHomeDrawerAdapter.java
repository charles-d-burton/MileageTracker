package com.charles.mileagetracker.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.charles.mileagetracker.app.R;
import com.charles.mileagetracker.app.adapter.containers.ExpandListChild;

import java.util.ArrayList;

/**
 * Created by charles on 8/21/14.
 */
public class SetHomeDrawerAdapter extends ArrayAdapter<ExpandListChild> {

    public SetHomeDrawerAdapter(Context context, ArrayList<ExpandListChild> children) {
        super(context, R.layout.set_home_drawer_item, children);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ExpandListChild child = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.set_home_drawer_item, parent,false);
        }

        TextView addressView = (TextView)convertView.findViewById(R.id.address_view);
        TextView dateView = (TextView)convertView.findViewById(R.id.date_view);

        String address = child.getAddress();
        address = address.substring(0, address.length() -5);
        addressView.setText(address);
        dateView.setText(child.getDate());

        return convertView;
    }
}
