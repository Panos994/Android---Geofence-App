package com.example.geofence_app2;

import static com.example.geofence_app2.DatabaseHelper.KEY_ID;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.content.UriMatcher;
import android.content.ContentUris;
import android.text.TextUtils;

import java.util.List;

public class MyContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.geofence_app2.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/locations");
    public static final Uri CONTENT_URI_POINTS = Uri.parse("content://" + AUTHORITY + "/points");

    private static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.example.locations";
    private static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.example.locations";

    private static final UriMatcher sUriMatcher;
    private static final int LOCATIONS = 1;
    private static final int LOCATION_ID = 2;
    private static final int POINTS = 3;
    private static final String TABLE_POINTS = "points";


    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "locations", LOCATIONS);
        sUriMatcher.addURI(AUTHORITY, "locations/#", LOCATION_ID);
        sUriMatcher.addURI(AUTHORITY, "points", POINTS);
    }

    private DatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                return CONTENT_TYPE;
            case LOCATION_ID:
                return CONTENT_ITEM_TYPE;
            case POINTS:
                return CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                cursor = db.query(DatabaseHelper.TABLE_LOCATIONS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
                case LOCATION_ID:
                    String locationId = uri.getLastPathSegment();
                    cursor = db.query(DatabaseHelper.TABLE_LOCATIONS, projection, DatabaseHelper.KEY_ID + "=" + locationId, selectionArgs, null, null, sortOrder);
                    break;
                    //BaseColumns._ID
                case POINTS:
                    cursor = db.query(DatabaseHelper.TABLE_POINTS, projection, selection, selectionArgs, null, null, sortOrder);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId;

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                rowId = db.insert(DatabaseHelper.TABLE_LOCATIONS, null, values);
                break;
            case POINTS:
                rowId = db.insert(TABLE_POINTS, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (rowId > 0) {
            Uri locationUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(locationUri, null);
            return locationUri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                count = db.delete(DatabaseHelper.TABLE_LOCATIONS, selection, selectionArgs);
                break;
            case LOCATION_ID:
                String locationId = uri.getLastPathSegment();
                count = db.delete(DatabaseHelper.TABLE_LOCATIONS, DatabaseHelper.KEY_ID + "=" + locationId + (selection != null ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
                //BaseColumns._ID
            case POINTS:
                count = db.delete(DatabaseHelper.TABLE_POINTS, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                count = db.update(DatabaseHelper.TABLE_LOCATIONS, values, selection, selectionArgs);
                break;
            case LOCATION_ID:
                String locationId = uri.getLastPathSegment();
                count = db.update(DatabaseHelper.TABLE_LOCATIONS, values, DatabaseHelper.KEY_ID + "=" + locationId + (selection != null ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
                    //BaseColumns._ID
                case POINTS:
                    count = db.update(DatabaseHelper.TABLE_POINTS, values, selection, selectionArgs);
                    break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


}