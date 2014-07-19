package com.charles.mileagetracker.app.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by charles on 4/2/14.
 */
public class StartPoints {
    public static final String TABLE_START_POINTS = "table_start_points";
    public static final String COLUMN_ID = "_id";
    public static final String NAME = "name";
    public static final String START_LAT = "lat";
    public static final String START_LON = "lon";
    public static final String ATTRS = "attrs";

    public static final String DATABASE_CREATE = "create table "
            + TABLE_START_POINTS + "(" + COLUMN_ID
            + " integer primary KEY autoincrement, "
            + NAME + " TEXT, "
            + START_LAT + " REAL, "
            + START_LON + " REAL, "
            + ATTRS + " TEXT DEFAULT NULL);";

    public static void onCreate(SQLiteDatabase database) {
        Log.v("Creating DB: ", "Start Points");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_START_POINTS);
        onCreate(db);*/
    }

}
