package ru.smirnov.vladislav.trackingapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final int TIMER_INTERVAL_MSEC = 5000; //5 * 60 * 1000;
    private static final String TRACKING_ENABLED_KEY = "trackingEnabled";

    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Boolean isTrackingEnabled = sharedPreferences.getBoolean(TRACKING_ENABLED_KEY, false);

        ToggleButton toggle = (ToggleButton) findViewById(R.id.toggleButton);
        buttonLogout = (Button) findViewById(R.id.buttonLogout);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !TrackingReceiver.isEnabled()) {
                    TrackingReceiver.setTimer(MainActivity.this, TIMER_INTERVAL_MSEC);
                } else if (TrackingReceiver.isEnabled()) {
                    TrackingReceiver.stopTimer(MainActivity.this);
                }

                sharedPreferences.edit().putBoolean(TRACKING_ENABLED_KEY, isChecked).apply();
            }
        });
        toggle.setChecked(isTrackingEnabled);

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: logout
            }
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
}
