package com.charles.mileagetracker.app.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by charles on 6/11/14.
 */
public class TripTable {

    public static final String TRIP_TABLE = "trip_table";
    public static final String COLUMN_ID = "_id";
    public static final String FENCE_RELATION = "fence_id";
    public static final String CLOSED = "closed";
    public static final String TRIP_KEY = "trip_key";
    public static final String TIME = "time_start";
    public static final String LAT = "start_lat";
    public static final String LON = "start_lon";
    public static final String ADDRESS = "start_address";
    public static final String DISTANCE = "total_distance";
    public static final String BUSINESS_RELATED = "business_related";
    public static final String FINALIZED = "finalized";
    //public static final String TIME_STAMP_MILLIS = "timestamp";

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TRIP_TABLE + "(" + COLUMN_ID
            + " integer primary KEY autoincrement, "
            + FENCE_RELATION + " INTEGER REFERENCES " + StartPoints.TABLE_START_POINTS + "(" + StartPoints.COLUMN_ID + ")"
            + " ON UPDATE CASCADE ON DELETE CASCADE, "
            + CLOSED + " INTEGER REFERENCES " + TripGroup.TRIP_GROUP + "(" + TripGroup.GROUP_ID + ")"
            + " ON UPDATE CASCADE ON DELETE CASCADE, "
            + TRIP_KEY + " INTEGER, "
            + TIME + " INTEGER, "
            + LAT + " REAL, "
            + LON + " REAL, "
            + DISTANCE + " REAL DEFAULT 0, "
            + ADDRESS + " TEXT DEFAULT NULL, "
            + FINALIZED + " INTEGER DEFAULT 0, "
            + BUSINESS_RELATED + " INTEGER DEFAULT 0);";

    public static void onCreate(SQLiteDatabase database) {
        Log.v("Creating DB: ", "TRIP TABLE");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TRIP_TABLE);
        onCreate(db);
    }
}
