package com.example.bluechatpro;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class BlueChatProApplication extends Application {

    public static final String CHANNEL_ID = "bluetooth_chat_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Chat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for Bluetooth chat messages");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}