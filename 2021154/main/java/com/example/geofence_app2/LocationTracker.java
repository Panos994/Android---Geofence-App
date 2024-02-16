package com.example.geofence_app2;



import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationTracker {

    //Ignore mContext
    private Context mContext;
    private LocationManager mLocationManager;
    private LocationChangeListener mLocationChangeListener;

    public LocationTracker(Context context) {
        this.mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public interface LocationChangeListener {
        void onLocationChanged(Location location);
    }

    public void setLocationChangeListener(LocationChangeListener listener) {
        this.mLocationChangeListener = listener;
    }


    // μέθοδος startTrackingLocation() αιτείται ενημερώσεις τοποθεσίας από το LocationManager. Καλείται για να ξεκινήσει την καταγραφή της τοποθεσίας
    public void startTrackingLocation() {
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    mLocationListener
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void stopTrackingLocation() {
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }



    //Αντιμετωπίζει ενημερώσεις τοποθεσίας που λαμβάνονται από το LocationManager με locationListener
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (mLocationChangeListener != null) {
                mLocationChangeListener.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };
}

