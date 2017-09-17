package ru.smirnov.vladislav.trackingapp;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class TrackingService extends IntentService {

    public static final String TRACK_ACTION = "tracking_app.TRACK";

    static class TrackReciever extends WakefulBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TRACK_ACTION)) {
                Log.i("TrackReciever", "Starting service @ " + SystemClock.elapsedRealtime());
                startWakefulService(context, new Intent(context, TrackingService.class));
            }
        }
    }

    public static void setTimer(Context context, long timeout) {
        PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(TRACK_ACTION), 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeout, intent);
    }

    public TrackingService() {
        super("TrackingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        for (int i=0; i<5; i++) {
            Log.i("SimpleWakefulReceiver", "Running service " + (i+1)
                    + "/5 @ " + SystemClock.elapsedRealtime());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        Log.i("SimpleWakefulReceiver", "Completed service @ " + SystemClock.elapsedRealtime());

        setTimer(this, 3000);
        TrackReciever.completeWakefulIntent(intent);
    }
}
