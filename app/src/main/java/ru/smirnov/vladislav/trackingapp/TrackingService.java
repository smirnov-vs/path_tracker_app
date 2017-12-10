package ru.smirnov.vladislav.trackingapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private static final String TRACKING_ENABLED_KEY = "trackingEnabled";
    private static final String PENDING_LOCATIONS_KEY = "pendingLocations";

    private static final String BACKEND_URL = "https://vladislavsmirnov.ru/api/log";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> locationHandler = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Boolean isTrackingEnabled = sharedPreferences.getBoolean(TRACKING_ENABLED_KEY, false);

        if (isTrackingEnabled) {
            locationHandler = scheduler.scheduleAtFixedRate(this::askLocation, 0, 60, TimeUnit.SECONDS);
            Log.i(TAG, "Started sticky");
            return START_STICKY;
        } else {
            stopSelf();
            Log.i(TAG, "Started not sticky and stopped");
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "Stopped");
        if (locationHandler != null) locationHandler.cancel(true);
    }

    private class PendingLocation {
        Location location;
        long time;

        PendingLocation(Location location, long time) {
            this.location = location;
            this.time = time;
        }
    }

    private class SendLogTask extends AsyncTask<Void, Void, Boolean> {
        private final PowerManager.WakeLock wakeLock;
        private final Location location;
        private final String token;

        private void sendPendingLocations(OkHttpClient client) {
            final SharedPreferences sharedPreferences = TrackingService.this.getSharedPreferences(TrackingService.this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            Set<String> pendingLocations = sharedPreferences.getStringSet(PENDING_LOCATIONS_KEY, new HashSet<>());
            Iterator<String> i = pendingLocations.iterator();

            while (i.hasNext()) {
                String jsonLocation = i.next();
                PendingLocation location = new Gson().fromJson(jsonLocation, PendingLocation.class);

                final JSONObject json = new JSONObject();
                try {
                    json.put("time", location.time);
                    json.put("latitude", location.location.getLatitude());
                    json.put("longitude", location.location.getLongitude());
                    json.put("accuracy", location.location.getAccuracy());
                    json.put("speed", location.location.getSpeed());

                    final Request request = new Request.Builder().url(BACKEND_URL).post(RequestBody.create(JSON, json.toString())).build();
                    Response response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Can't write pending log for token " + token);
                        continue;
                    }
                } catch (JSONException | IOException e) {
                    Log.e(TAG, "Can't write pending log for token " + token);
                    continue;
                }

                i.remove();
            }

            sharedPreferences.edit().putStringSet(PENDING_LOCATIONS_KEY, pendingLocations).apply();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .addInterceptor(chain -> {
                        final Request original = chain.request();

                        final Request authorized = original.newBuilder()
                                .addHeader("Cookie", "token=" + token)
                                .build();

                        return chain.proceed(authorized);
                    }).build();

            final JSONObject json = new JSONObject();
            try {
                json.put("latitude", location.getLatitude());
                json.put("longitude", location.getLongitude());
                json.put("accuracy", location.getAccuracy());
                json.put("speed", location.getSpeed());

                final Request request = new Request.Builder().url(BACKEND_URL).post(RequestBody.create(JSON, json.toString())).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Can't write log for token " + token);
                    return false;
                }
                sendPendingLocations(client);
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Can't write pending log for token " + token);
                return false;
            }
            Log.i(TAG, "Location is sent");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                final SharedPreferences sharedPreferences = TrackingService.this.getSharedPreferences(TrackingService.this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                Set<String> pendingLocations = sharedPreferences.getStringSet(PENDING_LOCATIONS_KEY, new HashSet<>());

                PendingLocation location = new PendingLocation(this.location, System.currentTimeMillis() / 1000);
                String json = new Gson().toJson(location);
                pendingLocations.add(json);

                sharedPreferences.edit().putStringSet(PENDING_LOCATIONS_KEY, pendingLocations).apply();
            }
            wakeLock.release();
        }

        SendLogTask(PowerManager.WakeLock wakeLock, Location location, String token) {
            this.wakeLock = wakeLock;
            this.location = location;
            this.token = token;
        }
    }

    private void askLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Log.e(TAG, "No token found");
            return;
        }

        final PowerManager pow = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (pow == null) return;
        final PowerManager.WakeLock wakeLock = pow.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire(10*60*1000L /*10 minutes*/);

        Log.i(TAG, "New location is requested");
        TrackingLocationProvider.getInstance().getNewLocation(location -> {
            if (location == null) {
                Log.i(TAG, "No new location");
                wakeLock.release();
                return;
            }
            Log.i(TAG, "New location is received");
            Log.i(TAG, "GPS: " + location.getLatitude() + ", " + location.getLongitude());
            Log.i(TAG, "Accuracy: " + location.getAccuracy());
            Log.i(TAG, "Speed: " + location.getSpeed());

            String json = new Gson().toJson(location);
            sharedPreferences.edit().putString("lastLocation", json).apply();

            final SendLogTask task = new SendLogTask(wakeLock, location, token);
            task.execute();
        });
    }
}
