package com.example.geofence_app2;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import com.google.android.gms.location.Geofence;
import android.os.IBinder;
import android.util.Log;
import android.location.LocationManager;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



import com.google.android.gms.maps.model.LatLng;

public class LocationTrackingService extends Service {
    private static final float LOCATION_CHANGE_THRESHOLD = 50;

    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private static final String TAG = "LocationTrackingService";
    private LocationTracker locationTracker;
    private ContentResolver contentResolver;
    private Location lastLocation;


    public static final String GPS_ENABLED_ACTION = "com.example.geofence_app2.GPS_ENABLED";
    public static final String GPS_DISABLED_ACTION = "com.example.geofence_app2.GPS_DISABLED";




    private BroadcastReceiver gpsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(GPS_ENABLED_ACTION)) {
                    // GPS is enabled, start η συνεχισε το service
                    startLocationUpdates();
                } else if (intent.getAction().equals(GPS_DISABLED_ACTION)) {
                    // GPS is disabled, stop  service
                    stopSelf();
                }
            }
        }
    };

//ignore isGpsEnabled here
    private boolean isGpsEnabled(Context context) {
      //δεν χρειαζεται ignore

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }




    @Override
    public void onCreate() {
        super.onCreate();


        IntentFilter filter = new IntentFilter("android.location.PROVIDERS_CHANGED");
        registerReceiver(gpsStatusReceiver, filter);




        locationTracker = new LocationTracker(getApplicationContext());
        locationTracker.setLocationChangeListener(new LocationTracker.LocationChangeListener() {
            @Override
            public void onLocationChanged(Location location) {
                handleLocationUpdate(location);
            }
        });
    }


//ξεκινα το service με την onStartCommand kai ginetai enarksi ton enimeroseon topothesias
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if the service is started explicitly by the "Start" button --->
        if (intent != null && intent.getBooleanExtra("start_service", false)) {
            startLocationUpdates();
        }
        return START_STICKY;
    }

    private void startLocationUpdates() {

        locationTracker.startTrackingLocation();
    }


//handleLocationUpdate() την επεξεργασία των ενημερώσεων τοποθεσίας και την καταγραφή τους εάν η αλλαγή είναι σημαντική.
    private void handleLocationUpdate(Location location) {

        Log.d(TAG, "Location Update: " + location.getLatitude() + ", " + location.getLongitude());

        // Check if location change is more than 50 meters

        if (isLocationChangeSignificant(location)) {
            // Retrieve geofence centers από τον ContentProvider
            contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(MyContentProvider.CONTENT_URI, null, null, null, null);

            if (cursor != null) {
                int latitudeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE);
                int longitudeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE);
                int radiusIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_RADIUS);

                while (cursor.moveToNext()) {
                    double geofenceLat = cursor.getDouble(latitudeIndex);
                    double geofenceLon = cursor.getDouble(longitudeIndex);
                    float radius = cursor.getFloat(radiusIndex);

                    LatLng geofenceCenter = new LatLng(geofenceLat, geofenceLon);


                    if (isLocationInsideGeofence(location, geofenceCenter, radius)) {
                        // αν το Location ειναι inside the geofence, record the point
                        recordGeofencePoint(location.getLatitude(), location.getLongitude());
                    }
                }

                cursor.close();
            }
        }
         lastLocation = location;


    }




    private boolean isLocationChangeSignificant(Location location) {
        //  logic to check if location change is more than 50 meters in the last 5 seconds
        if (lastLocation == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long locationTime = location.getTime();
        float distance = location.distanceTo(lastLocation);

        return (currentTime - locationTime <= LOCATION_UPDATE_INTERVAL) && (distance > LOCATION_CHANGE_THRESHOLD);
    }


    private boolean isLocationInsideGeofence(Location location, LatLng geofenceCenter, float radius) {
        //  logic to check if location is inside the geofence
        // I am using the Location.distanceTo() method for distance calculation
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), geofenceCenter.latitude, geofenceCenter.longitude, results);
        return results[0] <= radius;
    }

    private void recordGeofencePoint(double latitude, double longitude) {
        //  logic to record the point inside the geofence
        contentResolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LATITUDE, latitude);
        values.put(DatabaseHelper.COLUMN_LONGITUDE, longitude);
        // I may want to include additional information like session ID, timestamp.... etc
        // based on my requirements
        contentResolver.insert(MyContentProvider.CONTENT_URI_POINTS, values);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(gpsStatusReceiver);
        locationTracker.stopTrackingLocation();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
