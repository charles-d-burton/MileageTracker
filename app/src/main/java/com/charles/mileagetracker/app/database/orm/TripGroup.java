package com.charles.mileagetracker.app.database.orm;

import com.orm.SugarRecord;

/**
 * Created by charles on 10/27/14.
 */
public class TripGroup extends SugarRecord<TripGroup> {
    int group_id;
    boolean group_closed;
    double totalMileage;
    double billableMileage;

    //Relationship to Start and End Point for the trip_group
    HomePoints startPoint;
    HomePoints endPoint;

    public TripGroup() {
    }

    public TripGroup(int group_id, boolean group_closed, double mileageValue, double billableMileage, HomePoints startPoint) {
        this.group_id = group_id;
        this.group_closed = group_closed;
        this.totalMileage = mileageValue;
        this.billableMileage = billableMileage;
        this.startPoint = startPoint;
    }
}
