package org.telegram.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class HundredGramBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (context == null || intent == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                    || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                    || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                ApplicationLoader.postInitApplication();
                if (HundredGramConfig.isPersistentBackgroundEnabled()) {
                    Intent serviceIntent = new Intent(context, NotificationsService.class);
                    ContextCompat.startForegroundService(context, serviceIntent);
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
}
