package com.charles.mileagetracker.app.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by charles on 6/13/14.
 */
public class TripGroup {

    public static final String TRIP_GROUP = "trip_group";
    public static final String GROUP_ID = "_id";
    public static final String GROUP_CLOSED = "closed";
    public static final String MILEAGE_VALUE = "mileage_value";

    private static final String DATABASE_CREATE = "create table "
            + TRIP_GROUP + "(" + GROUP_ID
            + " integer primary KEY autoincrement, "
            + GROUP_CLOSED + " INTEGER DEFAULT 0);";



    public static void onCreate(SQLiteDatabase database) {
        Log.v("Creating DB: ", "TRIP TABLE");
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("ALTER TABLE " + TRIP_GROUP + " ADD COLUMN " + MILEAGE_VALUE + " INTEGER DEFAULT 55");
        /*Log.w(TripTable.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TRIP_GROUP);
        onCreate(db);*/
    }
}
