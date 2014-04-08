package com.charles.mileagetracker.app.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by charles on 3/29/14.
 */
public class TrackerContentProvider extends ContentProvider {

    private TrackerSQLiteHelper database;

    private static final int TRIPS = 10;
    private static final int TRIPS_ID = 20;
    private static final int STARTS = 30;
    private static final int STARTS_ID = 40;

    private static final String AUTHORITY = "com.charles.mileagetracker.app.database.TrackerContentProvider";

    private static final String TRIP_PATH = TripTable.TABLE_TRIPS;
    public static final Uri TRIP_URI = Uri.parse("content://" + AUTHORITY + "/" + TRIP_PATH);

    private static final String STARTS_PATH = StartPoints.TABLE_START_POINTS;
    public static final Uri STARTS_URI = Uri.parse("content://" + AUTHORITY + "/" + STARTS_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, TRIP_PATH, TRIPS);
        sURIMatcher.addURI(AUTHORITY, TRIP_PATH + "/#", TRIPS_ID);
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH, STARTS);
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH + "/#", STARTS_ID);
    }


    public boolean onCreate() {
        Log.v("DATABASE: ", "Content Provider Created");
        database = new TrackerSQLiteHelper(getContext());
        database.getWritableDatabase();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        checkColumns(projection);

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        int uriType = sURIMatcher.match(uri);
        switch (uriType){
            case TRIPS_ID:
                //adding the ID to the original query
                queryBuilder.appendWhere(TripTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                //Fall through
            case TRIPS:
                queryBuilder.setTables(TripTable.TABLE_TRIPS);
                break;
            case STARTS_ID:
                queryBuilder.appendWhere(StartPoints.COLUMN_ID + "=" + uri.getLastPathSegment());
                //Fall through
            case STARTS:
                queryBuilder.setTables(StartPoints.TABLE_START_POINTS);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        //make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;

    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = database.getWritableDatabase();
        int rowsDeleted = 0;
        Uri returnUri = null;
        long id = 0;
        switch (uriType) {
            case TRIPS:
                id = db.insert(TripTable.TABLE_TRIPS, null, values);
                returnUri = Uri.parse(TRIPS + "/" + id);
                break;
            case STARTS:
                id = db.insert(StartPoints.TABLE_START_POINTS, null, values);
                returnUri = Uri.parse(STARTS + "/" + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        Log.v("Insertion", "Successful Insertion");
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = database.getWritableDatabase();
        int rowsDeleted = 0;
        String id = null;

        switch (uriType) {
            case TRIPS:
                rowsDeleted = db.delete(TripTable.TABLE_TRIPS, selection, selectionArgs);
                break;
            case TRIPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(TripTable.TABLE_TRIPS, TripTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(TripTable.TABLE_TRIPS, TripTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case STARTS:
                rowsDeleted = db.delete(StartPoints.TABLE_START_POINTS, selection, selectionArgs);
                break;
            case STARTS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(StartPoints.TABLE_START_POINTS, StartPoints.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(StartPoints.TABLE_START_POINTS, StartPoints.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);


        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase db = database.getWritableDatabase();
        String id = null;
        int rowsUpdated = 0;

        switch (uriType) {
            case TRIPS:
                rowsUpdated = db.update(TripTable.TABLE_TRIPS, values, TripTable.COLUMN_ID + "=" + id, null);
                break;
            case TRIPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(TripTable.TABLE_TRIPS, values, TripTable.COLUMN_ID + "=" + id, null );
                } else {
                    rowsUpdated = db.update(TripTable.TABLE_TRIPS, values, TripTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case STARTS:
                rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, StartPoints.COLUMN_ID + "=" + id, null);
                break;
            case STARTS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, StartPoints.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, StartPoints.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private final void checkColumns(String projection[]) {
        String available[] = {TripTable.COLUMN_ID, TripTable.END_LAT, TripTable.END_LON, TripTable.START_LAT,
        TripTable.START_LON, TripTable.TIME_START, TripTable.TIME_END, TripTable.TOTAL_DISTANCE, TripTable.TOTAL_TIME,
        StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS, StartPoints.NAME};

        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown Columns in Projectsion");
            }
        }
    }
}
