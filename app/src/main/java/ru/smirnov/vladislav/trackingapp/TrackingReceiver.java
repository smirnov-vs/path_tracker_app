package ru.smirnov.vladislav.trackingapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrackingReceiver extends BroadcastReceiver {
    private static final String TAG = "TrackingReceiver";
    private static final String BACKEND_URL = "https://vladislavsmirnov.ru/api/log";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private class HttpTask extends AsyncTask<Object, Void, Boolean> {
        private final Context context;
        private final PowerManager.WakeLock wakeLock;

        @Override
        protected Boolean doInBackground(Object... params) {
            final Location location = (Location) params[0];
            final String token = (String) params[1];

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(@NonNull Chain chain) throws IOException {
                            final Request original = chain.request();

                            final Request authorized = original.newBuilder()
                                    .addHeader("Cookie", "token=" + token)
                                    .build();

                            return chain.proceed(authorized);
                        }
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
                }
            } catch (JSONException | IOException e) {
                return false;
            }
            Log.i(TAG, "Location is sent");
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) Toast.makeText(context, "Tracking send error", Toast.LENGTH_SHORT).show();
            wakeLock.release();
        }

        HttpTask(Context context, PowerManager.WakeLock wakeLock) {
            this.context = context;
            this.wakeLock = wakeLock;
        }
    }

    private static boolean timerEnabled;

    static void setTimer(Context context, long timeout) {
        final PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(context, TrackingReceiver.class), 0);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), timeout, intent);
        timerEnabled = true;
    }

    static void stopTimer(Context context) {
        final PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(context, TrackingReceiver.class), 0);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(intent);
        timerEnabled = true;
    }

    static boolean isEnabled() {
        return timerEnabled;
    }

    private void saveLocation() {

    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        final String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Log.e(TAG, "No token found");
            return;
        }

        final PowerManager pow = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pow.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        Log.i(TAG, "New location is requested");
        TrackingLocationProvider.getInstance().getNewLocation(new NewLocationCallback() {
            @Override
            public void onNewLocationCallback(Location location) {
                Log.i(TAG, "New location is received");
                Log.i(TAG, "GPS: " + location.getLatitude() + ", " + location.getLongitude());
                Log.i(TAG, "Accuracy: " + location.getAccuracy());
                Log.i(TAG, "Speed: " + location.getSpeed());

                String json = new Gson().toJson(location);
                sharedPreferences.edit().putString("lastLocation", json).apply();

                final HttpTask task = new HttpTask(context, wakeLock);
                task.execute(location, token);
            }
        });
    }
}
