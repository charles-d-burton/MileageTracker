package com.charles.mileagetracker.app.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by charles on 4/29/14.
 */
public class BluetoothDevices {
    public static final String TABLE_BLUETOOTH_DEVICES = "table_bluetooth";
    public static final String COLUMN_ID = "_id";
    public static final String REFRENCE_ID = "reference";
    public static final String DEVICE_ADDRESS = "address";
    public static final String DEVICE_NAME = "name";
    public static final String QUALITY = "quality";
    public static final String LAST_CONTACTED = "last_contact";

    public static final String DATABASE_CREATE = "create table " +
            TABLE_BLUETOOTH_DEVICES + "(" + COLUMN_ID
            + " integer primary key autoincrement, "
            + REFRENCE_ID + " INTEGER REFERENCES " + StartPoints.TABLE_START_POINTS + "(" + StartPoints.COLUMN_ID + ")"
            + " ON UPDATE CASCADE ON DELETE CASCADE, "
            + DEVICE_ADDRESS + " TEXT, "
            + DEVICE_NAME + " TEXT, "
            + QUALITY + " INTEGER, "
            + LAST_CONTACTED + " REAL); ";

    public static void onCreate(SQLiteDatabase database) {
        //Log.v("Creating DB: ", "Start Points");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLUETOOTH_DEVICES);
        onCreate(db);
    }

}
