package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Intent;


/**
 *This service will be called when the user wants to record every path segment.
 */
public class RecordAllPathSegments extends IntentService {

    public RecordAllPathSegments() {
        super("RecordAllPathSegments");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

        }
    }
}
