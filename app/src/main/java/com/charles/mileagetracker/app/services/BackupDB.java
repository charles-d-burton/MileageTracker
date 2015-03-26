package com.charles.mileagetracker.app.services;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.FileBackupHelper;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.charles.mileagetracker.app.services.intentservices.PostBootGeofenceService;

import java.io.File;
import java.io.IOException;

/**
 * Created by charles on 3/25/15.
 */
public class BackupDB extends BackupAgentHelper {
    private static final String DB_NAME = "sugar_trip.db";


    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        super.onRestore(data, appVersionCode, newState);
        Log.v("Data Restored", "Data Restored");
        Context context = getApplicationContext();
        Intent geoFenceIntent = new Intent(context, PostBootGeofenceService.class);
        context.startService(geoFenceIntent);
    }

    @Override
    public void onCreate(){
        FileBackupHelper dbs = new FileBackupHelper(this, DB_NAME);
        addHelper("dbs", dbs);
    }

    @Override
    public File getFilesDir(){
        File path = getDatabasePath(DB_NAME);
        return path.getParentFile();
    }
}
