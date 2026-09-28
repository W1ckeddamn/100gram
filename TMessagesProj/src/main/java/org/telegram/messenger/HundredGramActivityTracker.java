package org.telegram.messenger;

import androidx.collection.LongSparseArray;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public class HundredGramActivityTracker {

    public static final int TRIGGER_STATUS_ONLINE = 1;
    public static final int TRIGGER_TYPING = 2;
    public static final int TRIGGER_MESSAGE = 3;

    public static class ActivityEntry {
        public final long timeStart;
        public final long timeEnd;
        public final int triggerType;

        public ActivityEntry(long timeStart, long timeEnd, int triggerType) {
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.triggerType = triggerType;
        }

        public long getDuration() {
            if (timeEnd > timeStart) {
                return timeEnd - timeStart;
            }
            return 0;
        }
    }

    private static final HundredGramActivityTracker[] Instance = new HundredGramActivityTracker[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramActivityTracker getInstance(int num) {
        HundredGramActivityTracker localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (HundredGramActivityTracker.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new HundredGramActivityTracker(num);
                }
            }
        }
        return localInstance;
    }

    private final int currentAccount;
    private final LongSparseArray<Long> activeSessions = new LongSparseArray<>();
    private final LongSparseArray<Long> lastSeenCache = new LongSparseArray<>();
    private final LongSparseArray<Long> lastNotificationTimes = new LongSparseArray<>();
    private final LongSparseArray<Long> lastUserActivityTimes = new LongSparseArray<>();

    private HundredGramActivityTracker(int account) {
        this.currentAccount = account;
    }

    public void onUserStatusUpdate(long userId, TLRPC.UserStatus status) {
        if (!HundredGramConfig.trackOnlineStatus || userId == 0) {
            return;
        }
        if (userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;

        if (status instanceof TLRPC.TL_userStatusOnline) {
            onUserAction(userId, TRIGGER_STATUS_ONLINE);
        } else if (status instanceof TLRPC.TL_userStatusRecently || status instanceof TLRPC.TL_userStatusOffline || status instanceof TLRPC.TL_userStatusLastWeek || status instanceof TLRPC.TL_userStatusLastMonth) {
            Long startTime = activeSessions.get(userId);
            if (startTime != null && startTime > 0) {
                activeSessions.remove(userId);
                lastUserActivityTimes.remove(userId);
                long end = Math.max(startTime, now);
                lastSeenCache.put(userId, end);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, startTime, end);

                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
                });
            }
        }
    }

    public void onUserTyping(long userId) {
        onUserAction(userId, TRIGGER_TYPING);
    }

    public void onUserMessage(long userId) {
        onUserAction(userId, TRIGGER_MESSAGE);
    }

    public void onUserAction(long userId, int triggerType) {
        if (!HundredGramConfig.trackOnlineStatus || userId <= 0 || userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        Long existingStart = activeSessions.get(userId);
        Long lastAction = lastUserActivityTimes.get(userId);

        boolean isNewSession = false;
        if (existingStart == null || (lastAction != null && now - lastAction > 180)) {
            if (existingStart != null && lastAction != null) {
                long end = Math.max(existingStart, lastAction);
                lastSeenCache.put(userId, end);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, existingStart, end);
            }
            activeSessions.put(userId, now);
            lastSeenCache.put(userId, now);
            MessagesStorage.getInstance(currentAccount).saveUserActivityStart(userId, now, triggerType);
            isNewSession = true;
        }

        lastUserActivityTimes.put(userId, now);

        if (isNewSession) {
            checkAndNotifyOnline(userId, now);
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
            });
        }
    }

    private void checkAndNotifyOnline(long userId, long now) {
        if (!HundredGramConfig.notifyOnUserOnline) {
            return;
        }
        boolean isTracked = HundredGramConfig.isUserTrackedForOnline(userId);
        if (!isTracked && HundredGramConfig.notifyAllContactsOnline) {
            TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(userId);
            if (u != null && u.contact) {
                isTracked = true;
            }
        }
        if (isTracked) {
            Long lastNotif = lastNotificationTimes.get(userId);
            if (lastNotif == null || now - lastNotif > 120) {
                lastNotificationTimes.put(userId, now);
                AndroidUtilities.runOnUIThread(() -> {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
                    if (user != null && !user.bot) {
                        String name = ContactsController.formatName(user);
                        String message = String.format(LocaleController.getString("UserOnlineNow", R.string.UserOnlineNow), name);
                        try {
                            BulletinFactory.global().createSimpleBulletin(R.raw.chats_infotip, message).show();
                        } catch (Exception ignored) {}
                        postSystemOnlineNotification(user, message);
                    }
                });
            }
        }
    }

    private void postSystemOnlineNotification(TLRPC.User user, String message) {
        try {
            android.content.Context context = ApplicationLoader.applicationContext;
            if (context == null) return;
            android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            String channelId = "100gram_online_alerts";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = nm.getNotificationChannel(channelId);
                if (channel == null) {
                    channel = new android.app.NotificationChannel(channelId, LocaleController.getString("HundredGramOnlineNotificationChannel", R.string.HundredGramOnlineNotificationChannel), android.app.NotificationManager.IMPORTANCE_HIGH);
                    channel.enableVibration(true);
                    channel.enableLights(true);
                    nm.createNotificationChannel(channel);
                }
            }

            android.content.Intent intent = new android.content.Intent(context, org.telegram.ui.LaunchActivity.class);
            intent.setAction("com.tmessages.openchat" + Math.random());
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("userId", user.id);
            intent.putExtra("currentAccount", currentAccount);

            android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, (int) user.id, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0));

            androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.msg_usersearch)
                    .setContentTitle(LocaleController.getString("HundredGramOnlineNotificationChannel", R.string.HundredGramOnlineNotificationChannel))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL);

            nm.notify(100000 + (int) (user.id % 100000), builder.build());
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public long getLastSeen(long userId) {
        if (userId == 0) {
            return 0;
        }
        Long cached = lastSeenCache.get(userId);
        if (cached != null) {
            return cached;
        }
        long fromDb = MessagesStorage.getInstance(currentAccount).getLastSeenFromDb(userId);
        if (fromDb > 0) {
            lastSeenCache.put(userId, fromDb);
        }
        return fromDb;
    }

    public boolean isSessionActive(long userId) {
        Long start = activeSessions.get(userId);
        if (start == null) return false;
        Long last = lastUserActivityTimes.get(userId);
        long now = System.currentTimeMillis() / 1000L;
        return last == null || now - last <= 180;
    }

    public void clearHistory(long userId, Runnable onDone) {
        activeSessions.remove(userId);
        lastSeenCache.remove(userId);
        lastUserActivityTimes.remove(userId);
        MessagesStorage.getInstance(currentAccount).clearUserActivityLogs(userId, () -> {
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
        });
    }
}
