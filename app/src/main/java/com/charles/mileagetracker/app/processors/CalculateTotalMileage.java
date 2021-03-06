package com.charles.mileagetracker.app.processors;

import android.content.Context;
import android.os.AsyncTask;

import com.charles.mileagetracker.app.database.orm.TripGroup;

import java.util.Iterator;

/**
 * Created by charles on 3/3/15.
 * This class goes through all of the current trip groups and calculates the total mileage that is
 * business related so far.  It then updates a callback with the new information.
 */
public class CalculateTotalMileage extends AsyncTask<Void, Void, String> {

    private Context context;
    private CalcTotalMileageCallback callback;

    public CalculateTotalMileage(Context context, CalcTotalMileageCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... params) {
        Iterator<TripGroup> groups = TripGroup.findAll(TripGroup.class);
        int totalMileage = 0;
        if (groups != null) {
            while (groups.hasNext()) {
                TripGroup group = groups.next();
                int miles = (int)(group.billableMileage * .621);
                totalMileage = totalMileage + miles;
            }
        }
        String message = Integer.toString(totalMileage) + " Miles";
        return message;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (s != null) {
            callback.receiveMileageString(s);
        }

    }

    public interface CalcTotalMileageCallback {
        public void receiveMileageString(String mileageString);
    }

}
