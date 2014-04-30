package com.charles.mileagetracker.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by charles on 3/29/14.
 */
public class TrackerSQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trips.db";
    private static final int DATABASE_VERSION = 9;

    public TrackerSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TripTable.onCreate(db);
        StartPoints.onCreate(db);
        BluetoothDevices.onCreate(db);
        WifiAccessPoints.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        TripTable.onUpgrade(db, oldVersion, newVersion);
        StartPoints.onUpgrade(db, oldVersion, newVersion);
        BluetoothDevices.onUpgrade(db, oldVersion, newVersion);
        WifiAccessPoints.onUpgrade(db, oldVersion, newVersion);
    }
}
