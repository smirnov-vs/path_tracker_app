package ru.smirnov.vladislav.trackingapp;

import android.location.Location;

interface NewLocationCallback {
    void onNewLocationCallback(Location location);
}
