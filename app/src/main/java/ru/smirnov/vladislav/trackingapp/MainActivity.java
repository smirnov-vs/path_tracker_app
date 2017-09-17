package ru.smirnov.vladislav.trackingapp;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        WakefulBroadcastReceiver broadcastReceiver = new TrackingService.TrackReciever();
        registerReceiver(broadcastReceiver, new IntentFilter(TrackingService.TRACK_ACTION));
        TrackingService.setTimer(this, 3000);
    }

}
