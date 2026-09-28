/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.LaunchActivity;

public class NotificationsService extends Service {

    public static final String KEEPALIVE_CHANNEL_ID = "hundredgram_keepalive";
    public static final int ID_KEEPALIVE_NOTIFICATION = 1001;

    private boolean isForeground;

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        if (HundredGramConfig.keepAliveForegroundService) {
            startForegroundInternal();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (HundredGramConfig.keepAliveForegroundService) {
            startForegroundInternal();
        } else if (isForeground) {
            try {
                stopForeground(true);
            } catch (Throwable ignore) {}
            isForeground = false;
        }
        return START_STICKY;
    }

    private void startForegroundInternal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = nm.getNotificationChannel(KEEPALIVE_CHANNEL_ID);
                    if (channel == null) {
                        channel = new NotificationChannel(
                            KEEPALIVE_CHANNEL_ID,
                            LocaleController.getString("KeepAliveServiceTitle", R.string.KeepAliveServiceTitle),
                            NotificationManager.IMPORTANCE_LOW
                        );
                        channel.setDescription("Background connection service for message delivery and archiving");
                        channel.enableLights(false);
                        channel.enableVibration(false);
                        channel.setSound(null, null);
                        channel.setShowBadge(false);
                        nm.createNotificationChannel(channel);
                    }
                }
            }

            Intent launchIntent = new Intent(this, LaunchActivity.class);
            launchIntent.setAction(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, KEEPALIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.getString("AppName", R.string.AppName))
                .setContentText(LocaleController.getString("KeepAliveServiceRunning", R.string.KeepAliveServiceRunning))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET);

            Notification notification = builder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ID_KEEPALIVE_NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(ID_KEEPALIVE_NOTIFICATION, notification);
            }
            isForeground = true;

            // Ensure MTProto push connection is active for all accounts
            for (int a : SharedConfig.activeAccounts) {
                try {
                    ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
                } catch (Throwable ignore) {}
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isForeground) {
            try {
                stopForeground(true);
            } catch (Throwable ignore) {}
            isForeground = false;
        }
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true) || HundredGramConfig.keepAliveForegroundService) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
