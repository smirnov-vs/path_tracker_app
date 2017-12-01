package ru.smirnov.vladislav.trackingapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.ToggleButton;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String TRACKING_ENABLED_KEY = "trackingEnabled";
    private static final String SESSION_URL = "https://vladislavsmirnov.ru/api/session";

    private class LogoutTask extends AsyncTask<Void, Void, Void> {
        private final String token;

        @Override
        protected Void doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .addInterceptor(chain -> {
                        final Request original = chain.request();

                        final Request authorized = original.newBuilder()
                                .addHeader("Cookie", "token=" + token)
                                .build();

                        return chain.proceed(authorized);
                    }).build();

            try {
                final Request request = new Request.Builder().url(SESSION_URL).delete().build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Can't logout " + token);
                }
            } catch (IOException e) {
                Log.e(TAG, "Can't connect when logout " + token);
            }

            return null;
        }

        private LogoutTask(String token) {
            this.token = token;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Boolean isTrackingEnabled = sharedPreferences.getBoolean(TRACKING_ENABLED_KEY, false);

        final String token = sharedPreferences.getString("token", null);
        if (token == null) {
            Intent i = new Intent(this, LoginActivity.class);
            finish();
            startActivity(i);
        }

        ToggleButton toggle = findViewById(R.id.toggleButton);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(MainActivity.this, TrackingService.class);
            if (isChecked) {
                startService(intent);
            } else {
                stopService(intent);
            }

            sharedPreferences.edit().putBoolean(TRACKING_ENABLED_KEY, isChecked).apply();
        });
        toggle.setChecked(isTrackingEnabled);

        buttonLogout.setOnClickListener(v -> {
            logout();
        });
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    checkLocationPermission();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkLocationPermission();
    }

    private void logout() {
        final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);
        sharedPreferences.edit().remove("token").apply();

        Intent i = new Intent(this, LoginActivity.class);
        finish();
        startActivity(i);

        if (token == null) {
            return;
        }

        LogoutTask task = new LogoutTask(token);
        task.execute();
    }
}
