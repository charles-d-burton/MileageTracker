package com.charles.mileagetracker.app.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.DrawFilter;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by charles on 3/29/14.
 */
public class TrackerContentProvider extends ContentProvider {

    private TrackerSQLiteHelper database;

    private static final int TRIPS = 10;
    private static final int TRIPS_ID = 20;
    private static final int STARTS = 30;
    private static final int STARTS_ID = 40;
    private static final int BLUETOOTH = 50;
    private static final int BLUETOOTH_ID = 60;
    private static final int WIFI = 70;
    private static final int WIFI_ID = 80;

    private static final String AUTHORITY = "com.charles.mileagetracker.app.database.TrackerContentProvider";

    private static final String TRIP_PATH = TripTable.TABLE_TRIPS;
    public static final Uri TRIP_URI = Uri.parse("content://" + AUTHORITY + "/" + TRIP_PATH);

    private static final String STARTS_PATH = StartPoints.TABLE_START_POINTS;
    public static final Uri STARTS_URI = Uri.parse("content://" + AUTHORITY + "/" + STARTS_PATH);

    private static final String BLUETOOTH_PATH = BluetoothDevices.TABLE_BLUETOOTH_DEVICES;
    public static final Uri BLUETOOTH_URI = Uri.parse("content://" + AUTHORITY + "/" + BLUETOOTH_PATH);

    private static final String WIFI_PATH = WifiAccessPoints.TABLE_WIFI;
    public static final Uri WIFI_URI = Uri.parse("content://" + AUTHORITY + "/" + WIFI_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, TRIP_PATH, TRIPS);
        sURIMatcher.addURI(AUTHORITY, TRIP_PATH + "/#", TRIPS_ID);
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH, STARTS);
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH + "/#", STARTS_ID);
        sURIMatcher.addURI(AUTHORITY, BLUETOOTH_PATH, BLUETOOTH);
        sURIMatcher.addURI(AUTHORITY, BLUETOOTH_PATH +"/#", BLUETOOTH_ID);
        sURIMatcher.addURI(AUTHORITY, WIFI_PATH, WIFI);
        sURIMatcher.addURI(AUTHORITY, WIFI_PATH + "/#", WIFI_ID);
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
            case BLUETOOTH_ID:
                queryBuilder.appendWhere(BluetoothDevices.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            case BLUETOOTH:
                queryBuilder.setTables(BluetoothDevices.TABLE_BLUETOOTH_DEVICES);
                break;
            case WIFI_ID:
                queryBuilder.appendWhere(WifiAccessPoints.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            case WIFI:
                queryBuilder.setTables(WifiAccessPoints.TABLE_WIFI);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        //make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        getContext().getContentResolver().notifyChange(uri, null);
        return cursor;

    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    //Insert statement that can also handle dynamic replace, it lets you make a decision about
    //whether or not you want to update the table or insert.
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        Log.v("CONTENT PROVIDER: ", Integer.toString(uriType));

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
            case BLUETOOTH:
                if (doReplace(values)) {
                    id = db.replace(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, null, values);
                } else {
                    id = db.insert(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, null, values);
                }
                break;
            case WIFI:
                if (doReplace(values)) {
                    id = db.replace(WifiAccessPoints.TABLE_WIFI, null, values);
                } else {
                    id = db.insert(WifiAccessPoints.TABLE_WIFI, null, values);
                }
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
            case BLUETOOTH:
                rowsDeleted = db.delete(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, selection, selectionArgs);
                break;
            case BLUETOOTH_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, BluetoothDevices.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, BluetoothDevices.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case WIFI:
                rowsDeleted = db.delete(WifiAccessPoints.TABLE_WIFI, selection, selectionArgs);
                break;
            case WIFI_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(WifiAccessPoints.TABLE_WIFI, WifiAccessPoints.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(WifiAccessPoints.TABLE_WIFI, WifiAccessPoints.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
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
                rowsUpdated = db.update(TripTable.TABLE_TRIPS, values, selection, null);
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
                rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, selection, null);
                break;
            case STARTS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, StartPoints.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(StartPoints.TABLE_START_POINTS, values, StartPoints.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case BLUETOOTH:
                rowsUpdated = db.update(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, values, selection, null);
                break;
            case BLUETOOTH_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, values, BluetoothDevices.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(BluetoothDevices.TABLE_BLUETOOTH_DEVICES, values, BluetoothDevices.COLUMN_ID + "=" +id + " + and " + selection, selectionArgs);
                }
                break;
            case WIFI:
                rowsUpdated = db.update(WifiAccessPoints.TABLE_WIFI, values, selection, null);
                break;
            case WIFI_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(WifiAccessPoints.TABLE_WIFI, values, WifiAccessPoints.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(WifiAccessPoints.TABLE_WIFI, values, WifiAccessPoints.COLUMN_ID + "=" + id + " and " +  selection, selectionArgs);
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
        StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS, StartPoints.NAME,
        WifiAccessPoints.COLUMN_ID, WifiAccessPoints.REFRENCE_ID, WifiAccessPoints.BSSID, WifiAccessPoints.SSID,
        WifiAccessPoints.LAST_CONTACTED, BluetoothDevices.COLUMN_ID, BluetoothDevices.REFRENCE_ID, BluetoothDevices.DEVICE_NAME,
        BluetoothDevices.DEVICE_ADDRESS, BluetoothDevices.LAST_CONTACTED};

        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown Columns in Projectsion");
            }
        }
    }

    //Variable that we use to determine whether hor not we're doing a replace or update
    public static final String SQL_INSERT_OR_REPLACE = "__sql_insert_or_replace__";


    private boolean doReplace(ContentValues values) {
        boolean replace = false;
        if ( values.containsKey( SQL_INSERT_OR_REPLACE )) {
            replace = values.getAsBoolean( SQL_INSERT_OR_REPLACE );

            // Clone the values object, so we don't modify the original.
            // This is not strictly necessary, but depends on your needs
            values = new ContentValues( values );

            // Remove the key, so we don't pass that on to db.insert() or db.replace()
            values.remove( SQL_INSERT_OR_REPLACE );
        }
        return replace;
    }
}
