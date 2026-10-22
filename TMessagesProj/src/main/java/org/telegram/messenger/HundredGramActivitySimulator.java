package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.tl.TL_account;

import java.util.Calendar;
import java.util.Locale;

public class HundredGramActivitySimulator {

    public static final String ACTION_START_ACTIVITY_SIMULATION = "org.telegram.messenger.ACTION_START_ACTIVITY_SIMULATION";
    public static final String ACTION_STOP_ACTIVITY_SIMULATION = "org.telegram.messenger.ACTION_STOP_ACTIVITY_SIMULATION";
    public static final String ACTION_PING_ACTIVITY_SIMULATION = "org.telegram.messenger.ACTION_PING_ACTIVITY_SIMULATION";

    private static final String NOTIFICATION_CHANNEL_ID = "hundredgram_simulator_channel";
    private static final int NOTIFICATION_ID_BASE = 880000;
    private static final long PING_INTERVAL_MS = 50_000L; // 50 seconds

    private static final HundredGramActivitySimulator[] Instance = new HundredGramActivitySimulator[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramActivitySimulator getInstance(int num) {
        HundredGramActivitySimulator localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (HundredGramActivitySimulator.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new HundredGramActivitySimulator(num);
                }
            }
        }
        return localInstance;
    }

    public static void onBoot() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                getInstance(a).checkScheduleOnBoot();
            }
        }
    }

    private final int currentAccount;
    private final SharedPreferences preferences;

    private boolean isRunning;
    private long startTime;
    private long endTime;
    private boolean isMicroBreakActive;
    private long microBreakEndTime;
    private long lastMicroBreakTime;

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now >= endTime) {
                stopSimulation(true);
                return;
            }

            handlePeriodicTick(now);
            AndroidUtilities.runOnUIThread(this, PING_INTERVAL_MS);
        }
    };

    private HundredGramActivitySimulator(int account) {
        this.currentAccount = account;
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                "hundredgram_simulator_" + currentAccount, Context.MODE_PRIVATE);

        // Restore active simulation if app was killed
        boolean savedActive = preferences.getBoolean("sim_is_active", false);
        long savedStart = preferences.getLong("sim_start_time", 0);
        long savedEnd = preferences.getLong("sim_end_time", 0);
        long now = System.currentTimeMillis();

        if (savedActive && savedEnd > now) {
            this.isRunning = true;
            this.startTime = savedStart;
            this.endTime = savedEnd;
            this.lastMicroBreakTime = savedStart;
            AndroidUtilities.runOnUIThread(pingRunnable, 1000);
            updateNotification();
        } else if (savedActive) {
            preferences.edit().putBoolean("sim_is_active", false).apply();
        }
    }

    public synchronized boolean isSimulationActive() {
        if (!isRunning) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= endTime) {
            AndroidUtilities.runOnUIThread(() -> stopSimulation(true));
            return false;
        }
        return !isMicroBreakActive;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized long getStartTime() {
        return startTime;
    }

    public synchronized long getEndTime() {
        return endTime;
    }

    public synchronized long getRemainingSeconds() {
        if (!isRunning) {
            return 0;
        }
        long now = System.currentTimeMillis();
        return Math.max(0, (endTime - now) / 1000L);
    }

    public synchronized void startSimulation(int durationMinutes) {
        if (durationMinutes <= 0) {
            durationMinutes = 60;
        }
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.endTime = now + (durationMinutes * 60 * 1000L);
        this.isRunning = true;
        this.isMicroBreakActive = false;
        this.lastMicroBreakTime = now;

        preferences.edit()
                .putBoolean("sim_is_active", true)
                .putLong("sim_start_time", startTime)
                .putLong("sim_end_time", endTime)
                .apply();

        HundredGramConfig.setActivitySimulatorQuickDuration(durationMinutes);

        // Resume network immediately and send online presence
        ConnectionsManager.getInstance(currentAccount).resumeNetworkMaybe();
        sendOnlineStatus();

        // Schedule stop alarm for exact end time
        scheduleAlarm(ACTION_STOP_ACTIVITY_SIMULATION, endTime);
        scheduleAlarm(ACTION_PING_ACTIVITY_SIMULATION, now + PING_INTERVAL_MS);

        // Start periodic runnable for foreground execution
        AndroidUtilities.cancelRunOnUIThread(pingRunnable);
        AndroidUtilities.runOnUIThread(pingRunnable, PING_INTERVAL_MS);

        updateNotification();

        if (HundredGramConfig.activitySimulatorNotify) {
            showToastNotification(LocaleController.getString("ActivitySimulatorStarted", R.string.ActivitySimulatorStarted));
        }

        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.activitySimulationUpdated);
    }

    public synchronized void stopSimulation(boolean naturallyFinished) {
        if (!isRunning) {
            return;
        }
        this.isRunning = false;
        this.isMicroBreakActive = false;

        preferences.edit()
                .putBoolean("sim_is_active", false)
                .putLong("sim_start_time", 0)
                .putLong("sim_end_time", 0)
                .apply();

        AndroidUtilities.cancelRunOnUIThread(pingRunnable);
        cancelAlarm(ACTION_STOP_ACTIVITY_SIMULATION);
        cancelAlarm(ACTION_PING_ACTIVITY_SIMULATION);

        // Send offline to server
        sendOfflineStatus();

        dismissNotification();

        if (naturallyFinished) {
            if (HundredGramConfig.activitySimulatorScheduleEnabled) {
                if (HundredGramConfig.activitySimulatorScheduleRepeat == 0) {
                    HundredGramConfig.setActivitySimulatorScheduleEnabled(false);
                } else if (HundredGramConfig.activitySimulatorScheduleRepeat == 1) {
                    // Re-schedule for next day
                    scheduleNextAlarmForConfig();
                }
            }
            if (HundredGramConfig.activitySimulatorNotify) {
                showToastNotification(LocaleController.getString("ActivitySimulatorCompleted", R.string.ActivitySimulatorCompleted));
            }
        }

        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.activitySimulationUpdated);
    }

    public synchronized void scheduleSession(int startHour, int startMinute, int durationMinutes, int repeatMode) {
        HundredGramConfig.setActivitySimulatorSchedule(true, startHour, startMinute, durationMinutes, repeatMode);
        scheduleNextAlarmForConfig();
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.activitySimulationUpdated);
    }

    public synchronized void cancelSchedule() {
        HundredGramConfig.setActivitySimulatorScheduleEnabled(false);
        cancelAlarm(ACTION_START_ACTIVITY_SIMULATION);
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.activitySimulationUpdated);
    }

    public synchronized void checkScheduleOnBoot() {
        if (HundredGramConfig.activitySimulatorScheduleEnabled) {
            scheduleNextAlarmForConfig();
        }

        boolean savedActive = preferences.getBoolean("sim_is_active", false);
        long savedEnd = preferences.getLong("sim_end_time", 0);
        long now = System.currentTimeMillis();

        if (savedActive && savedEnd > now) {
            this.isRunning = true;
            this.startTime = preferences.getLong("sim_start_time", now);
            this.endTime = savedEnd;
            sendOnlineStatus();
            scheduleAlarm(ACTION_STOP_ACTIVITY_SIMULATION, endTime);
            scheduleAlarm(ACTION_PING_ACTIVITY_SIMULATION, now + PING_INTERVAL_MS);
            AndroidUtilities.runOnUIThread(pingRunnable, PING_INTERVAL_MS);
            updateNotification();
        } else if (savedActive) {
            stopSimulation(true);
        }
    }

    public synchronized void onAlarmReceived(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (ACTION_START_ACTIVITY_SIMULATION.equals(action)) {
            if (!isRunning) {
                startSimulation(HundredGramConfig.activitySimulatorScheduleDurationMinutes);
            }
        } else if (ACTION_STOP_ACTIVITY_SIMULATION.equals(action)) {
            if (isRunning) {
                stopSimulation(true);
            }
        } else if (ACTION_PING_ACTIVITY_SIMULATION.equals(action)) {
            if (isRunning) {
                long now = System.currentTimeMillis();
                if (now >= endTime) {
                    stopSimulation(true);
                } else {
                    handlePeriodicTick(now);
                    scheduleAlarm(ACTION_PING_ACTIVITY_SIMULATION, now + PING_INTERVAL_MS);
                }
            }
        }
    }

    private void handlePeriodicTick(long now) {
        // Natural micro-breaks simulation (e.g. 1.5 min break after 15 min of online)
        if (HundredGramConfig.activitySimulatorMicroBreaks) {
            if (isMicroBreakActive) {
                if (now >= microBreakEndTime) {
                    isMicroBreakActive = false;
                    lastMicroBreakTime = now;
                    sendOnlineStatus();
                } else {
                    sendOfflineStatus();
                    return;
                }
            } else if (now - lastMicroBreakTime >= 15 * 60 * 1000L) {
                // Enter micro break for 90 seconds
                isMicroBreakActive = true;
                microBreakEndTime = now + 90 * 1000L;
                sendOfflineStatus();
                return;
            }
        }

        sendOnlineStatus();
        updateNotification();
    }

    private void sendOnlineStatus() {
        try {
            TL_account.updateStatus req = new TL_account.updateStatus();
            req.offline = false;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("HundredGramActivitySimulator: sent online status, error=" + error);
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void sendOfflineStatus() {
        try {
            TL_account.updateStatus req = new TL_account.updateStatus();
            req.offline = true;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("HundredGramActivitySimulator: sent offline status, error=" + error);
                }
            });
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void scheduleNextAlarmForConfig() {
        if (!HundredGramConfig.activitySimulatorScheduleEnabled) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, HundredGramConfig.activitySimulatorScheduleStartHour);
        cal.set(Calendar.MINUTE, HundredGramConfig.activitySimulatorScheduleStartMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        long triggerAt = cal.getTimeInMillis();
        scheduleAlarm(ACTION_START_ACTIVITY_SIMULATION, triggerAt);
    }

    private void scheduleAlarm(String action, long triggerAtMillis) {
        Context context = ApplicationLoader.applicationContext;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, HundredGramActivitySimulatorReceiver.class);
        intent.setAction(action);
        intent.putExtra("account", currentAccount);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = getRequestCode(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void cancelAlarm(String action) {
        Context context = ApplicationLoader.applicationContext;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, HundredGramActivitySimulatorReceiver.class);
        intent.setAction(action);
        intent.putExtra("account", currentAccount);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = getRequestCode(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        try {
            am.cancel(pi);
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private int getRequestCode(String action) {
        if (ACTION_START_ACTIVITY_SIMULATION.equals(action)) {
            return 8000 + currentAccount * 10 + 1;
        } else if (ACTION_STOP_ACTIVITY_SIMULATION.equals(action)) {
            return 8000 + currentAccount * 10 + 2;
        } else {
            return 8000 + currentAccount * 10 + 3;
        }
    }

    private void updateNotification() {
        if (!isRunning || !HundredGramConfig.activitySimulatorNotify) {
            return;
        }
        try {
            Context context = ApplicationLoader.applicationContext;
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle),
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(LocaleController.getString("ActivitySimulatorSubtitle", R.string.ActivitySimulatorSubtitle));
                channel.enableVibration(false);
                channel.enableLights(false);
                nm.createNotificationChannel(channel);
            }

            long remainingSec = getRemainingSeconds();
            long mins = remainingSec / 60;
            long secs = remainingSec % 60;
            String timeStr = String.format(Locale.US, "%02d:%02d", mins, secs);

            String statusText = isMicroBreakActive
                    ? LocaleController.getString("ActivitySimulatorMicroBreak", R.string.ActivitySimulatorMicroBreak)
                    : String.format(LocaleController.getString("ActivitySimulatorRemaining", R.string.ActivitySimulatorRemaining), timeStr);

            Intent openIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (openIntent == null) {
                openIntent = new Intent(Intent.ACTION_MAIN);
            }

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent openPi = PendingIntent.getActivity(context, 8900 + currentAccount, openIntent, flags);

            Intent stopIntent = new Intent(context, HundredGramActivitySimulatorReceiver.class);
            stopIntent.setAction(ACTION_STOP_ACTIVITY_SIMULATION);
            stopIntent.putExtra("account", currentAccount);
            PendingIntent stopPi = PendingIntent.getBroadcast(context, getRequestCode(ACTION_STOP_ACTIVITY_SIMULATION), stopIntent, flags);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.msg_usersearch)
                    .setContentTitle(LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle))
                    .setContentText(statusText)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(openPi)
                    .addAction(R.drawable.msg_cancel, LocaleController.getString("ActivitySimulatorStop", R.string.ActivitySimulatorStop), stopPi);

            nm.notify(NOTIFICATION_ID_BASE + currentAccount, builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void dismissNotification() {
        try {
            Context context = ApplicationLoader.applicationContext;
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID_BASE + currentAccount);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void showToastNotification(String message) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                android.widget.Toast.makeText(ApplicationLoader.applicationContext, message, android.widget.Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
    }

    public String getFormattedRemainingTime() {
        long remainingSec = getRemainingSeconds();
        long hours = remainingSec / 3600;
        long minutes = (remainingSec % 3600) / 60;
        long seconds = remainingSec % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedScheduleTime() {
        return String.format(Locale.US, "%02d:%02d",
                HundredGramConfig.activitySimulatorScheduleStartHour,
                HundredGramConfig.activitySimulatorScheduleStartMinute);
    }
}
