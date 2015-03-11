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
            if(info.isConnected() && hasInternetAccess() && checkDataStatus()){
                Intent connectedIntent = new Intent(context, CalcMileageService.class);
                context.startService(connectedIntent);
            }
        }
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
