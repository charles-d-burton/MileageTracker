package com.charles.mileagetracker.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LearnLocationService extends Service {
    public LearnLocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int id = intent.getIntExtra("column_id", 0);
        return super.onStartCommand(intent, flags, startId);
    }

    private void learnWifi(){

    }

    private void learnBluetooth() {

    }

    private void learnTowers() {

    }
}
