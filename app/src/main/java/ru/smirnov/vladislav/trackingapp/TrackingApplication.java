package ru.smirnov.vladislav.trackingapp;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class TrackingApplication extends Application {
    private static final String PROJECT_ID = "352659015918";

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("default",
                "Push",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TrackingLocationProvider.initialize(this);
        initChannels(this);

        new Thread(() -> {
            InstanceID instanceID = InstanceID.getInstance(TrackingApplication.this);
            try {
                String token = instanceID.getToken(PROJECT_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                sharedPreferences.edit().putString("gcm_token", token).apply();
            } catch (IOException e) {
                Log.e("GCM", e.getMessage());
            }
        }).start();
    }
}
