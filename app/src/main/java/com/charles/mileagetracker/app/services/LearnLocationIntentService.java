package com.charles.mileagetracker.app.services;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class LearnLocationIntentService extends IntentService {

    private int id = 0;
    private Gson gson = new Gson();

    private WifiManager wifiManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private HashMap bluetoothMap = new HashMap();

    private HashMap locationInformation = new HashMap();

    public LearnLocationIntentService() {
        super("LearnLocationIntentService");
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        id = intent.getIntExtra("id", -1);
        if (intent != null && id != -1) {
            Log.v("LEARNING LOCATION: ", "Learning location for: " + Integer.toString(id));
            getWifi();
            getBluetooth();
        }
    }

    private void getWifi() {
        int state = wifiManager.getWifiState();
        boolean wasWifiEnabled = true;
        if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
            Log.v("WIFI STATE: ", "Wifi not on");
            //new DisplayToast(this, "Wifi off, location less precise");
            wifiManager.setWifiEnabled(true);
            wasWifiEnabled = false;
        }

        ArrayList<ScanResult> apList = (ArrayList)wifiManager.getScanResults();
        for (ScanResult result: apList) {
            //Log.v("SCAN Result: ", result.SSID);
        }

        if (!wasWifiEnabled) {
            wifiManager.setWifiEnabled(false);
        }
    }

    private void getBluetooth() {
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            new DisplayToast(this, "Bluetooth off or not available, location less precise");
        } else {
            //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            //unregisterReceiver(mReceiver);
            //registerReceiver(new BluetoothReceiver(), filter);
            mBtAdapter.startDiscovery();

        }

    }

    private void getTowerInfo() {
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        
    }


    /*
    Helper thread to display a toast message.
     */
    private class DisplayToast implements Runnable{
        private Context mContext = null;
        private String mText = null;

        public DisplayToast(Context mContext, String text) {
            this.mContext = mContext;
            this.mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
        }
    }


    /*public class BluetoothReceiver extends BroadcastReceiver {
        public BluetoothReceiver() {
        }

        private HashMap bluetoothMap = new HashMap();

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            Log.v("DEBUG: ", "Receiving Bluetooth Broadcasts");
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bluetoothMap.put(device.getAddress(), device.getName());
                Log.v("Device Address: " , device.getAddress());
                Log.v("Device Name: " , device.getName());
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            }
            Log.d("DEBUG LEARN LOCATION: ", "Unregistering");
            unregisterReceiver(this);
        }
    }*/
}


