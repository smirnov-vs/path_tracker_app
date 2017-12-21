package ru.smirnov.vladislav.trackingapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.gson.Gson;

class TrackingLocationProvider {
    private static final float MIN_DISTANCE_METERS = 500.0F;
    private static final int LOCATION_INTERVAL_MSEC = 1000;
    private static final int LOCATION_EXPIRATION_DURATION = 40 * LOCATION_INTERVAL_MSEC;

    private static TrackingLocationProvider provider;

    private final FusedLocationProviderClient locationProviderClient;
    private Location lastLocation;
    private float lastDistance = -1.F;

    private class TrackingLocationCallback extends LocationCallback {
        private final NewLocationCallback callback;

        @Override
        public void onLocationResult(LocationResult locationResult) {
            locationProviderClient.removeLocationUpdates(this);

            final Location location = locationResult.getLastLocation();
            if (location != null && lastLocation != null) {
                lastDistance = location.distanceTo(lastLocation);
                if (lastDistance < MIN_DISTANCE_METERS) {
                    callback.onNewLocationCallback(null);
                    return;
                }
            }
            else if (location == null) {
                callback.onNewLocationCallback(null);
                return;
            }
            lastLocation = location;
            callback.onNewLocationCallback(location);
        }

        TrackingLocationCallback(NewLocationCallback callback) {
            this.callback = callback;
        }
    }

    static void initialize(Context context) {
        if (provider == null) provider = new TrackingLocationProvider(context);
    }

    static TrackingLocationProvider getInstance() {
        return provider;
    }

    private TrackingLocationProvider(Context context) {
        locationProviderClient = new FusedLocationProviderClient(context);

        final SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("lastLocation", null);
        if (json != null)
            lastLocation = new Gson().fromJson(json, Location.class);
    }

    @SuppressLint("MissingPermission")
    void getNewLocation(NewLocationCallback callback) {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(LOCATION_INTERVAL_MSEC)
                .setExpirationDuration(LOCATION_EXPIRATION_DURATION);
        TrackingLocationCallback trackingCallback = new TrackingLocationCallback(callback);
        locationProviderClient.requestLocationUpdates(locationRequest, trackingCallback, Looper.getMainLooper());
    }

    float getLastDistance() {
        return lastDistance;
    }
}
