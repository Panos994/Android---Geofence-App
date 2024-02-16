
package com.example.geofence_app2;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "geofence_app.db";


    private static final int DATABASE_VERSION = 10;
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    // Table name

    public static final String COLUMN_RADIUS = "radius";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String TABLE_POINTS = "entrypoints";

    //  column names
    public static final String KEY_ID = "id";
    public static final String KEY_SESSION_ID = "session_id";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";



//
public static final String COLUMN_POINT_TYPE = "point_type";
    public static final int TYPE_ENTRY = 1;
    public static final int TYPE_EXIT = 2;
    //


    public static final String KEY_POINT_TYPE = "point_type"; // Added this line for the point types
    public static final int DEFAULT_RADIUS_METERS = 100;
    // Create table query
    private static final String CREATE_TABLE_LOCATIONS =
            "CREATE TABLE " + TABLE_LOCATIONS + "(" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    KEY_SESSION_ID + " INTEGER," +
                    KEY_LATITUDE + " REAL," +
                    KEY_LONGITUDE + " REAL," +
                    COLUMN_RADIUS + " REAL," +
                    KEY_POINT_TYPE + " INTEGER DEFAULT 0" + // Add this line
                    ");";














    private static final String CREATE_TABLE_POINTS =
            "CREATE TABLE " + TABLE_POINTS + "(" +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    KEY_LATITUDE + " REAL," +
                    KEY_LONGITUDE + " REAL" +
                    ");";


//δεν χρειαζεται - ignore insertPoint
    public void insertPoint(double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LATITUDE, latitude);
        values.put(KEY_LONGITUDE, longitude);
        long rowId = db.insert(TABLE_POINTS, null, values);
        db.close();
        Log.d("DatabaseHelper", "Inserted point with ID: " + rowId);
    }





    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LOCATIONS);
        db.execSQL(CREATE_TABLE_POINTS);


    }
/*
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POINTS);
        onCreate(db);
    }


*/


@Override
public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 10) {
        // Add the new column 'point_type' to the 'locations' table
        db.execSQL("ALTER TABLE " + TABLE_LOCATIONS + " ADD COLUMN " + KEY_POINT_TYPE + " INTEGER DEFAULT 0;");
    } else {
        // Handle other upgrades or simply drop and recreate all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POINTS);
        onCreate(db);
    }
}



    // Insert location in geofence.db
    public void insertLocation(int sessionId, double latitude, double longitude, double radius,int pointType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_SESSION_ID, sessionId);
        values.put(KEY_LATITUDE, latitude);
        values.put(KEY_LONGITUDE, longitude);
        values.put(COLUMN_RADIUS, radius);

        values.put(COLUMN_POINT_TYPE, pointType);

//


        long rowId = db.insert(TABLE_LOCATIONS, null, values);

        db.close();
        Log.d("DatabaseHelper", "Inserted row with ID: " + rowId);
    }

    // Na lavo ta Locations gia current session
    public Cursor getLocationsForSession(int sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {KEY_LATITUDE, KEY_LONGITUDE, COLUMN_POINT_TYPE};
        String selection = KEY_SESSION_ID + "=?";
        String[] selectionArgs = {String.valueOf(sessionId)};
        return db.query(TABLE_LOCATIONS, columns, selection, selectionArgs, null, null, null);
    }


    public void deleteLocationsForSession(int sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "SESSION_ID = ?";
        String[] whereArgs = {String.valueOf(sessionId)};
        db.delete(TABLE_LOCATIONS, whereClause, whereArgs);
        db.close();
    }







}
