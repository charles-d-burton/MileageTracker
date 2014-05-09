package com.charles.mileagetracker.app.services.intentservices;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.charles.mileagetracker.app.database.StartPoints;
import com.charles.mileagetracker.app.database.TrackerContentProvider;
import com.charles.mileagetracker.app.database.WifiAccessPoints;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


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

    //Load the JSON from the database
    private void loadJson() {

    }

    //Find the nearby WIFI access points
    private void getWifi() {
        HashMap wifiAttrs = getWifiMap(id);

        int state = wifiManager.getWifiState();
        boolean wasWifiEnabled = true;
        if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
            Log.v("WIFI STATE: ", "Wifi not on");
            //new DisplayToast(this, "Wifi off, location less precise");
            wifiManager.setWifiEnabled(true);
            wasWifiEnabled = false;
            return;
        }

        ArrayList<ScanResult> apList = (ArrayList)wifiManager.getScanResults();
        if (wifiAttrs.isEmpty()) {

        } else {
            Iterator it = wifiAttrs.values().iterator();
            while (it.hasNext()) {
                HashMap<String, String> apMap = (HashMap<String, String>)it.next();
                checkScanResult(apList, apMap);
            }

            if (!wasWifiEnabled) {
                wifiManager.setWifiEnabled(false);
            }
        }
    }

    //Get a bluetooth adapater and learn about the nearby bluetooth
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

    //Probably not going to do this one, it's really covered by the Geofence
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

    public HashMap getWifiMap(Integer id) {
        HashMap<Integer, HashMap<String,String>> wifiMap = new HashMap<Integer, HashMap<String, String>>();
        Uri uri = TrackerContentProvider.WIFI_URI;
        String projection[] = {
                WifiAccessPoints.COLUMN_ID,
                WifiAccessPoints.REFRENCE_ID,
                WifiAccessPoints.LAST_CONTACTED,
                WifiAccessPoints.SSID,
                WifiAccessPoints.BSSID
        };
        String selectionClause = WifiAccessPoints.REFRENCE_ID + "= ? ";
        String selectionArgs[] = {Integer.toString(id)};

        Cursor c = getContentResolver().query(uri, projection, selectionClause, selectionArgs, null);

        if (!(c == null) && !(c.getCount() < 1)) {
            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                HashMap wifiAttrs = new HashMap();
                Integer cId = c.getInt(c.getColumnIndexOrThrow(WifiAccessPoints.COLUMN_ID));
                Integer reference = c.getInt(c.getColumnIndexOrThrow(WifiAccessPoints.REFRENCE_ID));
                Log.v("DEBUG: ", "Reference ID: " + cId.toString());
                String bssid = c.getString(c.getColumnIndexOrThrow(WifiAccessPoints.BSSID));
                String ssid = c.getString(c.getColumnIndexOrThrow(WifiAccessPoints.SSID));
                Double lastContact = c.getDouble(c.getColumnIndexOrThrow(WifiAccessPoints.LAST_CONTACTED));
                Integer quality = c.getInt(c.getColumnIndexOrThrow(WifiAccessPoints.QUALITY));

                wifiAttrs.put("bssid", bssid);
                wifiAttrs.put("ssid", ssid);
                wifiAttrs.put("last_content", lastContact);
                wifiAttrs.put("quality", quality);
                wifiAttrs.put("reference", reference);

                wifiMap.put(cId, wifiAttrs); //Top Level HashMap
            }
        } else if (c.getCount() == 0) {
            Log.v("DEBUG: ", "Nothing in the database");
        } else {
            Log.v("DEBUG: ", "Something went horribly wrong getting Curstor");
        }
        return wifiMap;
    }

    public void checkScanResult(ArrayList<ScanResult> results, HashMap apMap) {
        for (ScanResult result : results ) {
            Iterator it = apMap.keySet().iterator();

        }
    }


}


