package com.charles.mileagetracker.app.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.charles.mileagetracker.app.database.orm.HomePoints;

/**
 * Created by charles on 3/29/14.
 */
public class TrackerSQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trips.db";
    private static final int DATABASE_VERSION = 29;
    private Context context = null;

    public TrackerSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StartPoints.onCreate(db);
        TripTable.onCreate(db);
        TripGroup.onCreate(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*StartPoints.onUpgrade(db, oldVersion, newVersion);
        TripTable.onUpgrade(db, oldVersion, newVersion);
        TripGroup.onUpgrade(db, oldVersion, newVersion);*/
        String projection[] = {StartPoints.START_LON, StartPoints.START_LAT, StartPoints.NAME};
        Cursor c = db.query(StartPoints.TABLE_START_POINTS, projection, null, null, null, null, null);

    }
}
