package com.example.geofence_app2;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.JobIntentService;

import com.example.geofence_app2.LocationTrackingService;

public class GpsStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            if (isGpsEnabled(context)) {
                // GPS is enabled
            } else {
                // GPS is disabled,  stamataei το service
                ((MapsActivity) context).stopLocationTrackingService();
            }
        }
    }

    private boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

/*

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            if (isGpsEnabled(context)) {
                // GPS is enabled, send a broadcast to the service to start or continue
                Intent serviceIntent = new Intent(context, LocationTrackingService.class);
                serviceIntent.setAction(LocationTrackingService.GPS_ENABLED_ACTION);
                context.startService(serviceIntent);
            } else {
                // GPS is disabled, send a broadcast to the service to stop
                Intent serviceIntent = new Intent(context, LocationTrackingService.class);
                serviceIntent.setAction(LocationTrackingService.GPS_DISABLED_ACTION);
                context.startService(serviceIntent);
            }
        }
    }





    private boolean isGpsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }


*/

}