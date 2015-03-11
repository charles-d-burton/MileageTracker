package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.charles.mileagetracker.app.database.orm.TripGroup;
import com.charles.mileagetracker.app.processingservices.TripGroupProcessor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
            //Process the Trip Data in the background
            Executors.newSingleThreadExecutor().execute(new ProcessTripData());
        }
    }

    private class ProcessTripData implements Runnable {

        @Override
        public void run() {
            if (hasInternetAccess() && checkDataStatus()){
                Context context = getApplicationContext();
                TripGroupProcessor processor = new TripGroupProcessor(context, CalcMileageService.this);
                List<TripGroup> groups = TripGroup.listAll(TripGroup.class);
                for (TripGroup group: groups) {
                    if (!group.processed) {
                        processor.processTripGroup(group);
                    }
                }
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

    private boolean hasInternetAccess() {
        try {
            HttpURLConnection urlc = (HttpURLConnection)
                    (new URL("http://clients3.google.com/generate_204")
                            .openConnection());
            urlc.setRequestProperty("User-Agent", "Android");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(3000);
            urlc.connect();
            return (urlc.getResponseCode() == 204 &&
                    urlc.getContentLength() == 0);
        } catch (IOException e) {
            Log.e("Error: ", "Error checking internet connection", e);
        }
        return false;
    }

    private boolean checkDataStatus() {
        boolean isConnected = false;
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
            urlc.setConnectTimeout(3000);
            urlc.connect();
            if (urlc.getResponseCode() == 200) {
                isConnected = true;
            }
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected;
    }
}
