package com.example.geofence_app2;

// MainActivity.java

/*references for this project mostly from: https://developer.android.com/develop,
https://stackoverflow.com/,
eclass - Lectures - labs

*/


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.maps.GoogleMap;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {


    private GoogleMap mMap;
    private LocationManager locationManager;

    private int sessionId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Intent intent = new Intent(MainActivity.this, ResultsMapActivity.class);
        intent.putExtra("SESSION_ID", -1);
        Button btnMap = findViewById(R.id.btnMap);
        Button btnStopTracking = findViewById(R.id.btnStopTracking);

        Button viewResultsButton = findViewById(R.id.btnViewResults);


        //23.1 επιπρόσθετη λειτουργια (Display Data) δεν χρειαζεται απλα την προσθεσα

        Button btnDisplayData = findViewById(R.id.btnDisplayData);
        btnDisplayData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Λειτουργία για εμφάνιση της νέας δραστηριότητας
                Intent intent = new Intent(MainActivity.this, DisplayDataActivity.class);
                startActivity(intent);
            }
        });


































        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open MapActivity
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("SESSION_ID", 123);
                startActivity(intent);
                stopLocationTrackingService();
                finish();
            }
        });

        btnStopTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationTrackingService();

                // Clear  map
                if (mMap != null) {
                    mMap.clear();
                }

                // Delete locations from  db
                DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                dbHelper.deleteLocationsForSession(sessionId);
                dbHelper.close();

                // Clear geofence circles
                MapsActivity.clearGeofenceCircles();
                MapsActivity.clearRemovedGeofenceCircles();

                // Return to the main activity
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();

                Toast.makeText(MainActivity.this, "Recording cancelled. Define your locations again!", Toast.LENGTH_SHORT).show();
            }

        });

        viewResultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch ResultsMapActivity to view results

                Intent intent = new Intent(MainActivity.this, ResultsMapActivity.class);

                // Assuming sessionId is the variable representing the current session ID
                intent.putExtra("SESSION_ID", 123);





                startActivity(intent);
            }
        });

    }

    private void stopLocationTrackingService() {
        //  logic to stop the LocationTrackingService
        Intent serviceIntent = new Intent(MainActivity.this, LocationTrackingService.class);
        stopService(serviceIntent);
    }

}
