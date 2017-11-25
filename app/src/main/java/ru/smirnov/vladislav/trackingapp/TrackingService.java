package ru.smirnov.vladislav.trackingapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public class TrackingService extends Service {
    private static final String TRACKING_ENABLED_KEY = "trackingEnabled";
    private static final int TIMER_INTERVAL_MSEC = 5 * 60 * 1000;

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
            if (!TrackingReceiver.isEnabled()) TrackingReceiver.setTimer(this, TIMER_INTERVAL_MSEC);
            return START_STICKY;
        } else {
            if (TrackingReceiver.isEnabled()) TrackingReceiver.stopTimer(this);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (TrackingReceiver.isEnabled()) TrackingReceiver.stopTimer(this);
    }
}
