package ru.smirnov.vladislav.trackingapp;

import android.app.Application;

public class TrackingApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        TrackingLocationProvider.initialize(this);
    }
}
