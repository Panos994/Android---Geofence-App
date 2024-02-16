package com.example.geofence_app2;

import static com.google.android.gms.maps.CameraUpdateFactory.zoomIn;
import static com.google.android.gms.maps.CameraUpdateFactory.zoomOut;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Serializable {
    //10.1
    private boolean isInsideGeofence = false; // ignore



    private GoogleMap mMap;
    private Button geofenceButton;
    private Button btnCancel;
    private Button btnStart;
    private int sessionId = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private ContentResolver contentResolver;


    //2 lists for the added with long click and the removed with no click kyklous
    private static ArrayList<Circle> geofenceCircles = new ArrayList<>();
    private static ArrayList<Circle> removedGeofenceCircles = new ArrayList<>();



    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Button btnZoomIn;

    private Button btnZoomOut;

     private Button btnCancelled;

   //για να σταματαει οταν δεν εχω σημα
    private GpsStatusReceiver gpsStatusReceiver;










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);




//φιταχνω αντικειμενο του gpsStatusReveiver του receiver Μου, και οταν αλλαξει το GPS SIGNAL απο
        //extended controls του emulator τοτε σταματαει και η καταγραφη τοποθεσιας οταν το ενεργοποιω
        //και παλι βρισκει την τρεχουσα τοποθεσια.

        gpsStatusReceiver = new GpsStatusReceiver();
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsStatusReceiver, filter);


        //



        databaseHelper = new DatabaseHelper(this);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        sessionId = getIntent().getIntExtra("SESSION_ID", 1);
        geofenceButton = findViewById(R.id.geofenceButton);
        btnCancel = findViewById(R.id.btnCancel);
        btnStart = findViewById(R.id.btnStart);
         btnCancelled = findViewById(R.id.btnCancelled);
        geofenceCircles = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);





        btnCancelled.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                mMap.clear();
                Log.d("MapsActivity", "btnCancelled onClick");
                // Return to the main activity
                DatabaseHelper dbHelper = new DatabaseHelper(MapsActivity.this);
                dbHelper.deleteLocationsForSession(sessionId); // Delete locations from the database
                dbHelper.close();

                MapsActivity.clearGeofenceCircles();
                MapsActivity.clearRemovedGeofenceCircles();

                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnZoomIn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                zoomIn();
            }

        });

        btnZoomOut.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                zoomOut();
            }

        });

        geofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordGeofenceLocations();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelGeofence();
                returnToMainActivity();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecordingAndService();

            }
        });
        if (checkLocationPermissions()) {
            requestLocationUpdates();
        }


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


    private void queryDatabaseAndLogResults() {
        // Get the database cursor for the current session
        Cursor cursor = databaseHelper.getLocationsForSession(this.sessionId);

        // Check if the cursor is not null and contains data
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int latitudeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LATITUDE);
                int longitudeIndex = cursor.getColumnIndex(DatabaseHelper.KEY_LONGITUDE);

                // επιβεβαιωνω οτι τα columns υπάρχουν στον Cursor (before  accessing them)
                if (latitudeIndex >= 0 && longitudeIndex >= 0) {
                    double latitude = cursor.getDouble(latitudeIndex);
                    double longitude = cursor.getDouble(longitudeIndex);

                    // Log the retrieved location
                    Log.d("DatabaseQuery", "Latitude: " + latitude + ", Longitude: " + longitude);
                }
            } while (cursor.moveToNext());

            // Close the cursor ( αποφυγή memory leaks)
            cursor.close();
        } else {
            // Log μηνυμα αν cursor is null or empty
            Log.d("DatabaseQuery", "No locations found in the database.");
        }
    }
    private boolean checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }





























    private Circle previousCircle;
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Check for location permissions before requesting updates
        if (checkLocationPermissions()) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));

                        for (Circle circle : geofenceCircles) {
                            LatLng circleCenter = circle.getCenter();
                            double radius = circle.getRadius();

                            if (isLocationInsideGeofence(location, circleCenter, radius)) {
                                // The current location is inside a geofence circle
                                // Record an entry point
                                showToast("Entry point");

                                // Record the entry point in the database
                                int pointType = DatabaseHelper.TYPE_ENTRY;
                                databaseHelper.insertLocation(sessionId, circleCenter.latitude, circleCenter.longitude, radius, pointType);

                                // Log the event
                                Log.d("GeofenceLog", "Entry point");
                            }

                    }

                /* * * * I will not use this onLocationResults method
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (android.location.Location location : locationResult.getLocations()) {
                        LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));


                        databaseHelper.insertPoint(location.getLatitude(), location.getLongitude());



                        if (!geofenceCircles.isEmpty()) {
                            Circle lastCircle = geofenceCircles.get(geofenceCircles.size() - 1);
                            LatLng circleCenter = lastCircle.getCenter();
                            double radius = lastCircle.getRadius();

                            if (isLocationInsideGeofence(location, circleCenter, radius)) {
                                showToast("ENTRY POINT");
                                Log.d("GeofenceLog", "Entry point");
//
                                previousCircle = lastCircle;





                            } else {
                                if (previousCircle != null) {
                                    LatLng prevCircleCenter = previousCircle.getCenter();
                                    double prevRadius = previousCircle.getRadius();

                                    if (!isLocationInsideGeofence(location, prevCircleCenter, prevRadius)) {
                                        showToast("EXIT POINT");
                                        Log.d("GeofenceLog", "Exit point");


                                        previousCircle = null; // Reset the previous circle


                                    }
                                }
                            }


                        //







                            //}
                        }

                        */

                        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12)); αν θελω να κανει το map Launch σε συγκεκριμενο zoom!
                        //fusedLocationClient.removeLocationUpdates(this); //  'this' as the callback reference δεν το χρειαζομαι! Ηταν αν δεν θελω locationupdates
                    }
                }

            };

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } catch (SecurityException e) {
                e.printStackTrace(); // Handle the SecurityException Μου  οπως χρειαζεται
            }
        }
    }





    private void returnToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // My existing startRecordingAndService method...
    private void startRecordingAndService() {
        contentResolver = getContentResolver();
        for (Circle circle : geofenceCircles) {
            LatLng center = circle.getCenter();
            double radius = circle.getRadius();

            int pointType = determinePointType(center, radius);

            // Call the updated insertLocation method απο τον  DatabaseHelper
            databaseHelper.insertLocation(sessionId, center.latitude, center.longitude, radius, pointType); // Assuming session ID is 1


        }
        startLocationTrackingService();
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }




    private int determinePointType(LatLng center, double radius) {
        // Your logic to determine if it's an ENTRY or EXIT point based on the current location
        // You might need to update this based on your specific requirements
        if (isLocationInsideGeofence(getLastKnownLocation(), center, radius)) {
            return DatabaseHelper.TYPE_ENTRY;
        } else {
            return DatabaseHelper.TYPE_EXIT;
        }
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // ζητάω location updates όταν ο map is ready
        requestLocationUpdates();



        // I Set up long click listener to add or remove geofence circles
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                toggleGeofenceCircle(latLng, 100);
            }
        });




    }
    private void cancelGeofence() {
        for (Circle circle : geofenceCircles) {
            circle.remove();
            removedGeofenceCircles.add(circle);

        }
        geofenceCircles.clear();
        Toast.makeText(this, "Geofence back", Toast.LENGTH_SHORT).show();
    }
/*8.2

    private void toggleGeofenceCircle(LatLng center, double radius) {
        Circle existingCircle = findExistingCircle(center);

        //
        // Get the current location
        Location currentLocation = getLastKnownLocation();

        if (existingCircle != null) {
            existingCircle.remove();
            geofenceCircles.remove(existingCircle);
            removedGeofenceCircles.add(existingCircle);





            if (isLocationInsideGeofence(currentLocation, center, radius)) {
                showToast("Exit point");
            }



        } else {
            drawGeofenceCircle(center, radius);



            //added !!! simantiko (Extra leitougria poy evala gia allagi xromatos)
            // Tsekarei na dei an o kiklos einai entry point diladi iparxei mesa to current location
            //kai ton prasinizei


            if (isLocationInsideGeofence(currentLocation, center, radius)) {
                // The current location is inside the geofence
                changeCircleColorToGreen();
                showToast("Current location is inside the geofence!");


                //11.1
                databaseHelper.insertPoint(center.latitude, center.longitude);


                Log.d("GeofenceLog", "Entry point");
            }

        }
    }

    */

    //

    private void toggleGeofenceCircle(LatLng center, double radius) {
        Circle existingCircle = findExistingCircle(center);

        // Get the current location
        Location currentLocation = getLastKnownLocation();


        //

        if (existingCircle != null) {
            existingCircle.remove();
            geofenceCircles.remove(existingCircle);
            removedGeofenceCircles.add(existingCircle);

            // Check if the location moved from inside to outside the circle
            if (isLocationInsideGeofence(currentLocation, center, radius)) {
                // Record an exit point if the location moved outside the circle
                showToast("Exit point");

                // Record the exit point in the database
                int pointType = DatabaseHelper.TYPE_EXIT;
                databaseHelper.insertLocation(sessionId, center.latitude, center.longitude, radius, pointType);

                // Log the event
                Log.d("GeofenceLog", "Exit point");
            }
        } else {
            drawGeofenceCircle(center, radius);

            // Check if the location moved from outside to inside the circle
            if (isLocationInsideGeofence(currentLocation, center, radius)) {
                // Record an entry point if the location moved inside the circle
                showToast("Entry point");

                // Record the entry point in the database
                int pointType = DatabaseHelper.TYPE_ENTRY;
                databaseHelper.insertLocation(sessionId, center.latitude, center.longitude, radius, pointType);

                // Log the event
                Log.d("GeofenceLog", "Entry point");
            }
        }
    }

    //





/* 15.2
  if (existingCircle != null) {
            existingCircle.remove();
            geofenceCircles.remove(existingCircle);
            removedGeofenceCircles.add(existingCircle);
        } else {
            drawGeofenceCircle(center, radius);

            if (isLocationInsideGeofence(currentLocation, center, radius)) {
                // The current location is inside the geofence
                showToast("Entry point");

                // Record the entry point in the database
                int pointType = DatabaseHelper.TYPE_ENTRY;
                databaseHelper.insertLocation(sessionId, center.latitude, center.longitude, radius, pointType);

                // Log the event
                Log.d("GeofenceLog", "Entry point");
            }
        }
    }
 */











    //that allaksei to xroma se prasino an o kiklos exei mesa to current location
    //ignore δεν θα το χρησιμοποιησω
    private void changeCircleColorToGreen() {
        if (!geofenceCircles.isEmpty()) {
            Circle lastCircle = geofenceCircles.get(geofenceCircles.size() - 1);
            lastCircle.setStrokeColor(getResources().getColor(R.color.colorRemovedStroke));
            lastCircle.setFillColor(getResources().getColor(R.color.colorRemovedFill));
        }
    }




    //that deikei to Toast moy to ekana se diaforetiki methodo

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }








//tsek an to location (to current apo ton emulator poy orisa sta Extended controls einai mesa kai prasinizei to kiklo)

    public static boolean isLocationInsideGeofence(Location location, LatLng geofenceCenter, double radius) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), geofenceCenter.latitude, geofenceCenter.longitude, results);
        return results[0] <= radius;
    }







//pairnei to teleutaio location current
private Location getLastKnownLocation() {
    // to get the last known location
    // mporo isos na to kano kai me FusedLocationProviderClient

    try {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        if (provider != null) {
            return locationManager.getLastKnownLocation(provider);
        }
    } catch (SecurityException e) {
        e.printStackTrace();
    }
    return null;
}


//os edo !! gia allagi xromatos an einai mesa to current location


    private Circle findExistingCircle(LatLng center) {
        for (Circle circle : geofenceCircles) {
            LatLng circleCenter = circle.getCenter();
            double distance = calculateDistance(center, circleCenter);
            if (distance < circle.getRadius()) {
                return circle;
            }
        }
        return null;
    }
//βάσει haversine τυπος
    private double calculateDistance(LatLng point1, LatLng point2) {
        double lat1 = point1.latitude;
        double lon1 = point1.longitude;
        double lat2 = point2.latitude;
        double lon2 = point2.longitude;

        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));

        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515 * 1609.344;

        return dist;
    }

    private void drawGeofenceCircle(LatLng center, double radius) {
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(2)
                .strokeColor(getResources().getColor(R.color.colorStroke))
                .fillColor(getResources().getColor(R.color.colorFill)));

        geofenceCircles.add(circle);
    }
    private int pointType;
    private void recordGeofenceLocations() {
        //contentResolver = getContentResolver();
        for (Circle circle : geofenceCircles) {
            LatLng center = circle.getCenter();
            double radius = circle.getRadius();





            addGeofenceToProvider(center.latitude, center.longitude, radius,pointType);
        }
        startLocationTrackingService();
    }
     // δεν χρειαζεται
    double latitude = 37.7749; // ...
    double longitude = -122.4194;
    private DatabaseHelper databaseHelper;
    private void addGeofenceToProvider(double latitude, double longitude, double radius, int pointType) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_LATITUDE, latitude);
        values.put(DatabaseHelper.KEY_LONGITUDE, longitude);
        values.put(DatabaseHelper.COLUMN_RADIUS, radius); // Include the radius parameter
        values.put(DatabaseHelper.COLUMN_POINT_TYPE,pointType);

        databaseHelper.insertLocation(this.sessionId, latitude, longitude, radius,pointType); // Pass the radius parameter
        queryDatabaseAndLogResults();
        contentResolver.insert(MyContentProvider.CONTENT_URI, values);

        Log.d("MapsActivity", "Geofence added to ContentProvider");
    }

    private void startLocationTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        startService(serviceIntent);
    }

    public static ArrayList<Circle> getGeofenceCircles() {
        return geofenceCircles;
    }

    public static ArrayList<Circle> getRemovedGeofenceCircles() {
        return removedGeofenceCircles;
    }



    public static void clearGeofenceCircles() {
        geofenceCircles.clear();
    }

    public static void clearRemovedGeofenceCircles() {
        removedGeofenceCircles.clear();
    }



    // μεθοδος για να σταματησω καταγραφη τρεχουσας τοποθεσιας
    // - edo gia an δεν εχω σημα σταματαει το service της αλλαγης τρεχουσας τοποθεσιας
    //το χρησιμοποιω οταν ο Receiver μετα απο disable GPS signal να σταματησει την καταγραφη
    public void stopLocationTrackingService() {

        Intent serviceIntent = new Intent(MapsActivity.this, LocationTrackingService.class);
        stopService(serviceIntent);
    }


    //edo gia an δεν εχω σημα σταματαει το service της αλλαγης τρεχουσας τοποθεσιας
    @Override
    protected void onDestroy() {
        unregisterReceiver(gpsStatusReceiver);
        stopLocationTrackingService();
        super.onDestroy();
    }



}
