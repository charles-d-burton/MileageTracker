package com.charles.mileagetracker.app.receivers;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

public class BluetoothReceiver extends BroadcastReceiver {
    public BluetoothReceiver() {
        Log.v("Starting Bluetooth Listener: ", "Listener Started");
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
        //Log.d("DEBUG LEARN LOCATION: ", "Unregistering");
        //unregisterReceiver(this);
    }
}
