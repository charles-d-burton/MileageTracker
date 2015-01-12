package com.charles.mileagetracker.app.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.charles.mileagetracker.app.services.intentservices.CalcMileageService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WifiReceiver extends BroadcastReceiver {


    private Context context = null;
    public WifiReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        Log.v("WIFI: ", "Wifi connection changed");
        if(info!=null){

            if(info.isConnected()){
                new CheckNetwork().execute(context);
            }
        }
    }

    private class CheckNetwork extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... params) {
            Context context = params[0];
            Log.v("Connected To Wifi: ", "CONNECTED");
            //This tests to make sure there is nothing blocking the connection.  I might make a retry loop here
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    //Intent connectedIntent = new Intent(context, CalcMileageService.class);
                    //context.startService(connectedIntent);
                    calculateDistanceInBackground();
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void calculateDistanceInBackground() {
        /*boolean alreadyRunning = false;
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        //Iterate through all the running services to try and find the CalcMileageService
        for (ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (CalcMileageService.class.getName().equals(service.service.getClassName())) {
                alreadyRunning = true;
                break;
            }
        }

        //If the service isn't running, start it.
        if (!alreadyRunning) {
            Intent connectedIntent = new Intent(context, CalcMileageService.class);
            context.startService(connectedIntent);
        }*/
    }
}
