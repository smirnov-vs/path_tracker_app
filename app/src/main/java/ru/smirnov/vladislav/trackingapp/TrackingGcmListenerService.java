package ru.smirnov.vladislav.trackingapp;

import android.app.NotificationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class TrackingGcmListenerService extends GcmListenerService {
    private static final String TAG = "TrackGcmListenerService";

    private static int notificationId = 0;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String user = data.getString("user");
        String area = data.getString("area");
        String message = String.format("User <%s> has just left area <%s>", user, area);
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "default")
                .setContentTitle(getString(R.string.gcm_title))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message);

        notificationManager.notify(notificationId++, notification.build());
    }
}
