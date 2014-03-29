package com.charles.mileagetracker.app.database;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TripTable {

    public static final String TABLE_TRIPS = "trips_table";
    public static final String COLUMN_ID = "_id";
    public static final String TIME_START = "time_start";
    public static final String START_LAT = "start_lat";
    public static final String START_LON = "start_lon";
    public static final String TIME_END = "time_end";
    public static final String END_LAT = "end_lat";
    public static final String END_LON = "end_lon";
    public static final String TOTAL_DISTANCE = "total_distance";
    public static final String TOTAL_TIME = "total_time";



    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_TRIPS + "(" + COLUMN_ID
            + " integer primary key autoincrement, "
            + TIME_START + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + START_LAT + " DOUBLE, "
            + START_LON + " DOUBLE, "
            + TIME_END + " DATETIME DEFAULT CURRENT TIMESTAMP, "
            + END_LAT + " DOUBLE, "
            + END_LON + " DOUBLE, "
            + TOTAL_DISTANCE + " INT, "
            + TOTAL_TIME + " INT);";

    public static void onCreate(SQLiteDatabase database) {
        Log.v("Creating DB: ", "TRIP TABLE");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(db);
    }
}
