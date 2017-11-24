package ru.smirnov.vladislav.trackingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.gson.Gson;

class TrackingLocationProvider {
    private static final float MIN_DISTANCE_METERS = 250.0F;
    private static final int LOCATION_INTERVAL_MSEC = 1000;
    private static final int LOCATION_EXPIRATION_DURATION = 40 * LOCATION_INTERVAL_MSEC;

    private static TrackingLocationProvider provider;

    private final FusedLocationProviderClient locationProviderClient;
    private Location lastLocation;

    private class TrackingLocationCallback extends LocationCallback {
        private final NewLocationCallback callback;

        @Override
        public void onLocationResult(LocationResult locationResult) {
            locationProviderClient.removeLocationUpdates(this);

            final Location location = locationResult.getLastLocation();
            if (location != null && lastLocation != null) {
                if (location.distanceTo(lastLocation) < MIN_DISTANCE_METERS)
                    return;
            }
            else if (location == null) {
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

    void getNewLocation(NewLocationCallback callback) {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                       .setInterval(LOCATION_INTERVAL_MSEC)
                       .setExpirationDuration(LOCATION_EXPIRATION_DURATION);
        TrackingLocationCallback trackingCallback = new TrackingLocationCallback(callback);
        locationProviderClient.requestLocationUpdates(locationRequest, trackingCallback, null);
    }
}
