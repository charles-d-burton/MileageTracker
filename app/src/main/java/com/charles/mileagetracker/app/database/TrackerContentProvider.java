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
    private static final int GROUPS = 50;
    private static final int GROUPS_ID = 60;

    private static final String AUTHORITY = "com.charles.mileagetracker.app.database.TrackerContentProvider";

    private static final String STARTS_PATH = StartPoints.TABLE_START_POINTS;
    public static final Uri STARTS_URI = Uri.parse("content://" + AUTHORITY + "/" + STARTS_PATH);

    private static final String TRIP_PATH = TripTable.TRIP_TABLE;
    public static final Uri TRIP_URI = Uri.parse("content://" + AUTHORITY + "/" + TRIP_PATH);

    private static final String GROUP_PATH = TripGroup.TRIP_GROUP;
    public static final Uri GROUP_URI = Uri.parse("content://" + AUTHORITY + "/" + GROUP_PATH);


    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH, STARTS);
        sURIMatcher.addURI(AUTHORITY, STARTS_PATH + "/#", STARTS_ID);

        sURIMatcher.addURI(AUTHORITY, TRIP_PATH, TRIPS);
        sURIMatcher.addURI(AUTHORITY, TRIP_PATH + "/#", TRIPS_ID);

        sURIMatcher.addURI(AUTHORITY, GROUP_PATH, GROUPS);
        sURIMatcher.addURI(AUTHORITY, GROUP_PATH + "/#", GROUPS_ID);
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
            case STARTS_ID:
                queryBuilder.appendWhere(StartPoints.COLUMN_ID + "=" + uri.getLastPathSegment());
                //Fall through
            case STARTS:
                queryBuilder.setTables(StartPoints.TABLE_START_POINTS);
                break;
            case TRIPS_ID:
                queryBuilder.appendWhere(TripTable.COLUMN_ID + "=" + uri.getLastPathSegment());
            case TRIPS:
                queryBuilder.setTables(TripTable.TRIP_TABLE);
                break;
            case GROUPS_ID:
                queryBuilder.appendWhere(TripGroup.GROUP_ID + "=" + uri.getLastPathSegment());
            case GROUPS:
                queryBuilder.setTables(TripGroup.TRIP_GROUP);
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
            case STARTS:
                id = db.insert(StartPoints.TABLE_START_POINTS, null, values);
                returnUri = Uri.parse(STARTS + "/" + id);
                break;
            case TRIPS:
                id = db.insert(TripTable.TRIP_TABLE, null, values);
                returnUri = Uri.parse(TRIPS_ID + "/" + id);
                break;
            case GROUPS:
                id = db.insert(TripGroup.TRIP_GROUP, null, values);
                returnUri = Uri.parse(GROUPS_ID + "/" + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
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
            case TRIPS:
                rowsDeleted = db.delete(TripTable.TRIP_TABLE, selection, selectionArgs);
                break;
            case TRIPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(TripTable.TRIP_TABLE, TripTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(TripTable.TRIP_TABLE, TripTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case GROUPS:
                rowsDeleted = db.delete(TripGroup.TRIP_GROUP, selection, selectionArgs);
                break;
            case GROUPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(TripGroup.TRIP_GROUP, TripGroup.GROUP_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(TripGroup.TRIP_GROUP, TripGroup.GROUP_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);


        }
        db.close();
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
            case TRIPS:
                rowsUpdated = db.update(TripTable.TRIP_TABLE, values, selection, null);
                break;
            case TRIPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(TripTable.TRIP_TABLE, values, TripTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(TripTable.TRIP_TABLE, values, TripTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            case GROUPS:
                rowsUpdated = db.update(TripGroup.TRIP_GROUP, values,selection, null);
                break;
            case GROUPS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(TripGroup.TRIP_GROUP, values, TripGroup.GROUP_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(TripGroup.TRIP_GROUP, values, TripGroup.GROUP_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return rowsUpdated;
    }

    private final void checkColumns(String projection[]) {
        String available[] = {
        StartPoints.COLUMN_ID, StartPoints.START_LAT, StartPoints.START_LON, StartPoints.ATTRS, StartPoints.NAME,

        TripTable.FENCE_RELATION, TripTable.ADDRESS,TripTable.COLUMN_ID, TripTable.DISTANCE,TripTable.FENCE_RELATION,
        TripTable.LAT, TripTable.LON, TripTable.TIME, TripTable.TRIP_KEY, TripTable.TIME, TripTable.CLOSED, TripTable.BUSINESS_RELATED,

        TripGroup.GROUP_ID, TripGroup.GROUP_CLOSED

        };

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

            // Remove the KEY, so we don't pass that on to db.insert() or db.replace()
            values.remove( SQL_INSERT_OR_REPLACE );
        }
        return replace;
    }
}
