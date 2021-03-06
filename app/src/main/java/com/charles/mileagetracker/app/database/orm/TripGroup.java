package com.charles.mileagetracker.app.database.orm;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;

/**
 * Created by charles on 10/27/14.
 */
public class TripGroup extends SugarRecord<TripGroup> {

    public boolean group_closed = false;
    public double totalMileage = 0;
    public double billableMileage = 0;
    public boolean processed = false;

    @Ignore
    private ArrayList<TripRow> children;


    public TripGroup() {

    }

    public TripGroup(boolean group_closed) {
        this.group_closed = group_closed;
    }

    public ArrayList<TripRow> getChildren() {
        return children;
    }

    public boolean setChildren(java.util.List children) {
        if (children == null) {
            children = new ArrayList<TripRow>();
        } else {
            children.clear();
        }
        return this.children.addAll(children);
    }
}
