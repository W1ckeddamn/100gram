package org.telegram.messenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class HundredGramActivitySimulatorReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        PowerManager.WakeLock wakeLock = null;
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "100gram:ActivitySimulatorWakeLock");
                wakeLock.acquire(15000L); // 15 seconds max
            }

            int account = intent.getIntExtra("account", UserConfig.selectedAccount);
            if (account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT) {
                HundredGramActivitySimulator.getInstance(account).onAlarmReceived(intent);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
