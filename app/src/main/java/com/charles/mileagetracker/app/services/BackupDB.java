package com.charles.mileagetracker.app.services;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;

import java.io.File;

/**
 * Created by charles on 3/25/15.
 */
public class BackupDB extends BackupAgentHelper {
    private static final String DB_NAME = "sugar_trip.db";



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
