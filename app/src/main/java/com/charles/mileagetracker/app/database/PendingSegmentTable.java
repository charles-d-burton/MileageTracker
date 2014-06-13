package com.charles.mileagetracker.app.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by charles on 5/9/14.
 */
public class PendingSegmentTable {

    public static final String PENDING_TABLE = "pending_table";
    public static final String COLUMN_ID = "_id";
    public static final String TIME_START = "time_start";
    public static final String START_LAT = "start_lat";
    public static final String START_LON = "start_lon";
    public static final String START_ADDRESS = "start_address";
    public static final String TIME_END = "time_end";
    public static final String END_LAT = "end_lat";
    public static final String END_LON = "end_lon";
    public static final String END_ADDRESS = "end_address";
    public static final String TOTAL_DISTANCE = "total_distance";
    public static final String TOTAL_TIME = "total_time";



    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + PENDING_TABLE + "(" + COLUMN_ID
            + " integer primary KEY autoincrement, "
            + TIME_START + " INTEGER, "
            + START_LAT + " REAL, "
            + START_LON + " REAL, "
            + START_ADDRESS + " TEXT, "
            + TIME_END + " INTEGER, "
            + END_LAT + " REAL, "
            + END_LON + " REAL, "
            + END_ADDRESS + " TEXT, "
            + TOTAL_DISTANCE + " INTEGER, "
            + TOTAL_TIME + " INTEGER);";

    public static void onCreate(SQLiteDatabase database) {
        Log.v("Creating DB: ", "TRIP TABLE");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + PENDING_TABLE);
        onCreate(db);
    }
}
