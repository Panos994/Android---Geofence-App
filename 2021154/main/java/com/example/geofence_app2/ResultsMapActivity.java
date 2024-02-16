package com.example.geofence_app2;


import static com.example.geofence_app2.MapsActivity.getGeofenceCircles;



import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

public class ResultsMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btnZoomIn;
    private int sessionId;
    private Button btnZoomOut;
    private boolean isTrackingPaused = false; // Track tracking state

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        Log.d("ResultsMapActivity", "onCreate");



        databaseHelper = new DatabaseHelper(this);


        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sessionId = getIntent().getIntExtra("SESSION_ID", 1);
        Button btnPauseResume = findViewById(R.id.btnPauseResume);
        Button btnBackToMain = findViewById(R.id.btnBackToMain);
        isTrackingPaused = getTrackingState();
        updatePauseResumeButtonText();

        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomIn();
            }

        });

        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomOut();
            }

        });
        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement logic to pause or resume the LocationTrackingService
                toggleTrackingService();
            }

        });


        btnBackToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                Log.d("ResultsMapActivity", "btnBackToMain onClick");
                // Return to the main activity
                DatabaseHelper dbHelper = new DatabaseHelper(ResultsMapActivity.this);
                dbHelper.deleteLocationsForSession(sessionId); // Delete locations from the database
                dbHelper.close();

                MapsActivity.clearGeofenceCircles();
                MapsActivity.clearRemovedGeofenceCircles();

                Intent intent = new Intent(ResultsMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        isTrackingPaused = getTrackingState();
        updatePauseResumeButtonText();


    }


    private void zoomIn() {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
        }
    }

    private void zoomOut() {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.zoomOut());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mMap != null) {
            mMap.clear();
            // to retrieve geofence areas and recorded locations
            // I am using DatabaseHelper to fetch data and display on the map
            int sessionId = getCurrentSessionId();
            displayGeofenceAreas();
            displayRecordedLocations(sessionId);

            displayCurrentLocation();




        }
    }

    private void displayCurrentLocation() {
        // I use the FusedLocationProviderClient to get the current device location
        try {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            Task<Location> locationTask = fusedLocationClient.getLastLocation();

            locationTask.addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng currentLocation = new LatLng(latitude, longitude);


                        // I Add a marker για το current location

                                 mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));


                        // FOR MOVING the the camera to the current location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
                    }
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    private void displayGeofenceAreas() {

        mMap.clear();
        MapsActivity mapsActivity = new MapsActivity();
        ArrayList<Circle> geofenceCircles = getGeofenceCircles();
        ArrayList<Circle> removedGeofenceCircles = mapsActivity.getRemovedGeofenceCircles();
        for (Circle circle : geofenceCircles) {
            mMap.addCircle(new CircleOptions()
                    .center(circle.getCenter())
                    .radius(circle.getRadius())
                    .strokeWidth(2)
                    .strokeColor(getResources().getColor(R.color.colorStroke))
                    .fillColor(getResources().getColor(R.color.colorFill)));
        }
        // fainontai ta  removed geofence circles
        for (Circle circle : removedGeofenceCircles) {
            mMap.addCircle(new CircleOptions()
                    .center(circle.getCenter())
                    .radius(circle.getRadius())
                    .strokeWidth(2)
                    .strokeColor(getResources().getColor(R.color.colorStroke))  //  color for removed circles
                    .fillColor(getResources().getColor(R.color.colorStroke)));   //  color for removed circles to idio evala

        }
    }






   private HashSet<String> entryPoints = new HashSet<>();


private void displayRecordedLocations(int sessionId) {
        // Retrieve the recorded points from the database
        Cursor cursor = databaseHelper.getLocationsForSession(sessionId);

        if (cursor != null && cursor.moveToFirst()) {
            do {

                int latitudeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LATITUDE);
                int longitudeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LONGITUDE);
                int pointTypeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_POINT_TYPE);

                if (latitudeIndex >= 0 && longitudeIndex >= 0 && pointTypeIndex >= 0) {
                    double latitude = cursor.getDouble(latitudeIndex);
                    double longitude = cursor.getDouble(longitudeIndex);
                    int pointType = cursor.getInt(pointTypeIndex);


                    LatLng point = new LatLng(latitude, longitude);



                    // Handle ENTRY and EXIT points
                    if (pointType == DatabaseHelper.TYPE_ENTRY) {
                        // Display  circle for ENTRY point
                        mMap.addCircle(new CircleOptions()
                                .center(point)
                                .radius(DatabaseHelper.DEFAULT_RADIUS_METERS)
                                .strokeWidth(2)
                                .strokeColor(Color.BLUE)
                                .fillColor(Color.argb(0, 0, 0, 255)));

                        // to eixa prin allakso to xroma Add marker or perform other actions for ENTRY points
                       // mMap.addMarker(new MarkerOptions().position(point).title("ENTRY Point (my location is inside geofence)"));
                        addColoredMarker(mMap, point, "ENTRY Point (my location - inside geofence)", Color.GREEN); // Set your desired marker color
                    } else if (pointType == DatabaseHelper.TYPE_EXIT) {
                        // Display the circle for EXIT point
                        mMap.addCircle(new CircleOptions()
                                .center(point)
                                .radius(DatabaseHelper.DEFAULT_RADIUS_METERS)
                                .strokeWidth(2)
                                .strokeColor(Color.RED)
                                .fillColor(Color.argb(70, 200, 100, 0)));

                        // to eixa prin allakso to xroma Added marker or perform other actions for EXIT points
                        //mMap.addMarker(new MarkerOptions().position(point).title("EXIT Point OR random Circle"));

                        addColoredMarker(mMap, point, " Circle", Color.MAGENTA);

                    } else {
                    // For circles that are not ENTRY or EXIT points, I  chose to display them differently
                    mMap.addCircle(new CircleOptions()
                            .center(point)
                            .radius(DatabaseHelper.DEFAULT_RADIUS_METERS)
                            .strokeWidth(2)
                            .strokeColor(Color.GRAY)
                            .fillColor(Color.argb(70, 169, 169, 169)));
                }
            }


            } while (cursor.moveToNext());

            cursor.close();
        }
    }

















    //ignore δεν θα το χρησιμοποιησω
    private void addColoredMarker(GoogleMap map, LatLng position, String title, int markerColor) {
        // Create a Bitmap with the specified marker color
        Bitmap markerBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);
        Paint paint = new Paint();
        paint.setColor(markerColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(25, 25, 25, paint);

        // Create BitmapDescriptor from the Bitmap
        BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromBitmap(markerBitmap);

        // Add marker to the map
        map.addMarker(new MarkerOptions().position(position).title(title).icon(markerIcon));
    }




    private int getCurrentSessionId() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("SESSION_ID")) {
            return intent.getIntExtra("SESSION_ID", 1);
        } else {
            // If session ID is not provided, try fetching it from ContentProvider
            return getSessionIdFromContentProvider();
        }
    }
    private int getSessionIdFromContentProvider() {
        // Το URI του ContentProvider
        Uri contentProviderUri = Uri.parse("content://com.example.com.example.geofence_app2.MyContentProvider/sessions");

        // Ενα projection για να ορισω ποια columns θελω να κανω retrieve
        String[] projection = {"SESSION_ID"};

        // Selection and selectionArgs to filter the data an xreiastei
        String selection = null;
        String[] selectionArgs = null;

        try (Cursor cursor = getContentResolver().query(
                contentProviderUri,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                // Get the session ID from the cursor
                int sessionIdIndex = cursor.getColumnIndex("SESSION_ID");
                if (sessionIdIndex >= 0) {
                    return cursor.getInt(sessionIdIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return a default value if the session ID den ginei retrieve
        return -1;
    }




    private void toggleTrackingService() {
        if (isTrackingPaused) {
            // Αν η παρακολούθηση είναι παύση, επανεκκινηση
            resumeTrackingService();
        } else {
            // Αλλιώς, παύση
            pauseTrackingService();
        }
        // Αλλάξτε την κατάσταση παύσης / επανάνοσης
        isTrackingPaused = !isTrackingPaused;
        // Eνημέρωνω το κείμενο του κουμπιού με βάση τη νέα κατάσταση
        updatePauseResumeButtonText();
    }

    private void pauseTrackingService() {
        // Λογική για την παύση της υπηρεσίας
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
    }

    private void resumeTrackingService() {
        // Λογική για την επανέναρξη της υπηρεσίας
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        startService(serviceIntent);
    }


















    private boolean getTrackingState() {
        SharedPreferences prefs = getSharedPreferences("TrackingPrefs", Context.MODE_PRIVATE);
        // Retrieve the tracking state, me default  false if not found
        return prefs.getBoolean("trackingState", false);
    }


    private void setTrackingState(boolean isTrackingPaused) {
        SharedPreferences prefs = getSharedPreferences("TrackingPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("trackingState", isTrackingPaused);
        editor.apply();
    }
    private void updatePauseResumeButtonText() {
        // Kano Update to button text bme vasi to  tracking state
        Button btnPauseResume = findViewById(R.id.btnPauseResume);
        if (isTrackingPaused) {
            btnPauseResume.setText("Resume Tracking");
        } else {
            btnPauseResume.setText("Pause Tracking");
        }
    }



    @Override
    protected void onDestroy() {
        // Close the database helper when the activity is destroyed
        databaseHelper.close();
        super.onDestroy();
    }

}
