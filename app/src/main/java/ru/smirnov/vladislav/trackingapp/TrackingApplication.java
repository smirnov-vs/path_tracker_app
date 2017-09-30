package ru.smirnov.vladislav.trackingapp;

import android.app.Application;

public class TrackingApplication extends Application {
    private static final int TIMER_INTERVAL_MSEC = 5*60*1000;

    @Override
    public void onCreate() {
        super.onCreate();

        TrackingLocationProvider.initialize(this);
        TrackingReceiver.setTimer(this, TIMER_INTERVAL_MSEC);
    }
}
