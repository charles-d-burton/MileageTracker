package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;

import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class CalcMileageService extends IntentService implements TripGroupProcessor.GroupProcessorInterface {

    private final String CLASS_NAME = ((Object)this).getClass().getName();

    public CalcMileageService() {
        super("CalcMileageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Context context = getApplicationContext();
            TripGroupProcessor processor = new TripGroupProcessor(context, this);
            List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
            for (TripGroup group: groups) {
                processor.processTripGroup(group);
            }
        }
    }

    @Override
    public void finishedGroupProcessing(java.util.List rows) {
        Log.v(CLASS_NAME, "Finished Processing Group");
    }

    @Override
    public void unableToProcessGroup(int failCode) {
        String message = "";
        switch (failCode) {
            case TripGroupProcessor.CONNECT_FAILED:
                message = "Connection Failure";
                break;
            case TripGroupProcessor.UNKOWN_FAILURE:
                message = "Unkown Failure";
                break;
            case TripGroupProcessor.INVALID_GROUP:
                message = "Group Invalidated";
                break;
        }
        Log.v(CLASS_NAME, message);
    }
}
