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
                //Intent connectedIntent = new Intent(context, CalcMileageService.class);
                //context.startService(connectedIntent);
            }
        }
    }
}
