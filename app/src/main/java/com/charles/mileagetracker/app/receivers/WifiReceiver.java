package com.charles.mileagetracker.app.receivers;

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
    public WifiReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
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
            //This tests to make sure there is no proxy or anything blocking the connection.
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    Intent connectedIntent = new Intent(context, CalcMileageService.class);
                    context.startService(connectedIntent);
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
