package com.charles.mileagetracker.app.database.orm;

import com.orm.SugarRecord;

/**
 * Created by charles on 10/27/14.
 */
public class TripGroup extends SugarRecord<TripGroup> {

    public boolean group_closed = false;
    public double totalMileage = 0;
    public double billableMileage = 0;


    public TripGroup() {

    }

    public TripGroup(boolean group_closed) {
        this.group_closed = group_closed;
    }
}
