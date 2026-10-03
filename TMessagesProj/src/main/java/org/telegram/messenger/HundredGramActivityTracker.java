package org.telegram.messenger;

import androidx.collection.LongSparseArray;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Set;

public class HundredGramActivityTracker implements NotificationCenter.NotificationCenterDelegate {

    public static final int TRIGGER_STATUS_ONLINE = 1;
    public static final int TRIGGER_TYPING = 2;
    public static final int TRIGGER_MESSAGE = 3;
    public static final int TRIGGER_APP_OPEN = 4;

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
            return 15L;
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
    private final LongSparseArray<Integer> activeSessionTypes = new LongSparseArray<>();
    private final LongSparseArray<Long> lastSeenCache = new LongSparseArray<>();
    private final LongSparseArray<Long> lastNotificationTimes = new LongSparseArray<>();
    private final LongSparseArray<Long> lastUserActivityTimes = new LongSparseArray<>();
    private final LongSparseArray<Runnable> typingTimeoutRunnables = new LongSparseArray<>();

    private HundredGramActivityTracker(int account) {
        this.currentAccount = account;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
    }

    public void onUserStatusUpdate(long userId, TLRPC.UserStatus status) {
        if (!HundredGramConfig.trackOnlineStatus || userId <= 0 || status == null) {
            return;
        }
        if (userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;

        if (status instanceof TLRPC.TL_userStatusOnline) {
            cancelTypingTimeout(userId);
            onUserAction(userId, TRIGGER_STATUS_ONLINE);
        } else if (status instanceof TLRPC.TL_userStatusRecently || status instanceof TLRPC.TL_userStatusOffline || status instanceof TLRPC.TL_userStatusLastWeek || status instanceof TLRPC.TL_userStatusLastMonth) {
            cancelTypingTimeout(userId);
            Long startTime = activeSessions.get(userId);
            if (startTime != null && startTime > 0) {
                activeSessions.remove(userId);
                activeSessionTypes.remove(userId);
                lastUserActivityTimes.remove(userId);
                long end = Math.max(startTime + 15L, now);
                lastSeenCache.put(userId, end);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, startTime, end);

                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
                });
            }
        }
    }

    public boolean isTrackedUser(long userId) {
        if (!HundredGramConfig.trackOnlineStatus || userId <= 0) return false;
        if (HundredGramConfig.isUserTrackedForOnline(userId)) return true;
        if (HundredGramConfig.notifyAllContactsOnline) {
            TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(userId);
            if (u != null && u.contact) return true;
        }
        return false;
    }

    public void onUserActivityDetected(long userId, int triggerType) {
        if (triggerType == TRIGGER_TYPING) {
            onUserTyping(userId);
        } else if (triggerType == TRIGGER_MESSAGE) {
            onUserMessage(userId);
        } else {
            onUserAction(userId, triggerType);
        }
    }

    public void onUserTyping(long userId) {
        onUserAction(userId, TRIGGER_TYPING);
    }

    public void onUserMessage(long userId) {
        cancelTypingTimeout(userId);
        onUserAction(userId, TRIGGER_MESSAGE);
    }

    public void onUserAction(long userId, int triggerType) {
        if (!HundredGramConfig.trackOnlineStatus || userId <= 0 || userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }

        long now = System.currentTimeMillis() / 1000L;
        Long existingStart = activeSessions.get(userId);
        Long lastAction = lastUserActivityTimes.get(userId);
        Integer existingType = activeSessionTypes.get(userId);

        if (triggerType == TRIGGER_TYPING) {
            // Typing has a strict 6-second timeout and is NOT stitched into an arbitrary 180s session
            if (existingStart == null || existingType == null) {
                activeSessions.put(userId, now);
                activeSessionTypes.put(userId, TRIGGER_TYPING);
                lastUserActivityTimes.put(userId, now);
                lastSeenCache.put(userId, now);
                MessagesStorage.getInstance(currentAccount).saveUserActivityStart(userId, now, TRIGGER_TYPING);
                checkAndNotifyOnline(userId, now);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
                });
            } else if (existingType == TRIGGER_TYPING) {
                lastUserActivityTimes.put(userId, now);
            } else {
                // Already in a full online or message session
                lastUserActivityTimes.put(userId, now);
                return;
            }
            scheduleTypingTimeout(userId);
            return;
        }

        // For non-typing events (STATUS_ONLINE, MESSAGE, etc.)
        cancelTypingTimeout(userId);

        boolean isNewSession = false;
        if (existingStart == null || (lastAction != null && now - lastAction > 90)) {
            if (existingStart != null) {
                long estimatedEnd = (lastAction != null && lastAction > existingStart) ? lastAction : (existingStart + 45L);
                lastSeenCache.put(userId, estimatedEnd);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, existingStart, estimatedEnd);
            }
            activeSessions.put(userId, now);
            activeSessionTypes.put(userId, triggerType);
            lastSeenCache.put(userId, now);
            MessagesStorage.getInstance(currentAccount).saveUserActivityStart(userId, now, triggerType);
            isNewSession = true;
        } else {
            // Upgrade existing typing-only session to full session upon receiving message or online status
            if (existingType != null && existingType == TRIGGER_TYPING) {
                activeSessionTypes.put(userId, triggerType);
            }
        }

        lastUserActivityTimes.put(userId, now);

        if (isNewSession) {
            checkAndNotifyOnline(userId, now);
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
            });
        }
    }

    private void scheduleTypingTimeout(long userId) {
        cancelTypingTimeout(userId);
        Runnable runnable = () -> onTypingTimeout(userId);
        typingTimeoutRunnables.put(userId, runnable);
        AndroidUtilities.runOnUIThread(runnable, 6000);
    }

    private void cancelTypingTimeout(long userId) {
        Runnable runnable = typingTimeoutRunnables.get(userId);
        if (runnable != null) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
            typingTimeoutRunnables.remove(userId);
        }
    }

    private void onTypingTimeout(long userId) {
        typingTimeoutRunnables.remove(userId);
        Integer type = activeSessionTypes.get(userId);
        if (type != null && type == TRIGGER_TYPING) {
            Long startTime = activeSessions.get(userId);
            Long lastAction = lastUserActivityTimes.get(userId);
            if (startTime != null) {
                long end = (lastAction != null ? lastAction : startTime) + 6L;
                activeSessions.remove(userId);
                activeSessionTypes.remove(userId);
                lastUserActivityTimes.remove(userId);
                lastSeenCache.put(userId, end);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, startTime, end);

                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
                });
            }
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

    private long selfCurrentSessionStart = 0;

    public void onSelfAppResume() {
        if (!HundredGramConfig.trackSelfActivity) {
            return;
        }
        long myId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (myId <= 0) {
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        Long existingStart = activeSessions.get(myId);
        Long lastAction = lastUserActivityTimes.get(myId);

        if (existingStart == null || (lastAction != null && now - lastAction > 60)) {
            if (existingStart != null && lastAction != null && lastAction > existingStart) {
                long end = Math.max(existingStart + 15L, lastAction);
                lastSeenCache.put(myId, end);
                MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(myId, existingStart, end);
            }
            activeSessions.put(myId, now);
            lastSeenCache.put(myId, now);
            selfCurrentSessionStart = now;
            MessagesStorage.getInstance(currentAccount).saveUserActivityStart(myId, now, TRIGGER_APP_OPEN);
        } else {
            selfCurrentSessionStart = existingStart != null ? existingStart : now;
        }

        lastUserActivityTimes.put(myId, now);
        requestTrackedUserStatuses();
    }

    public void onSelfAppPause() {
        long myId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (myId <= 0) {
            return;
        }

        Long startTime = activeSessions.get(myId);
        if (startTime != null && startTime > 0) {
            long now = System.currentTimeMillis() / 1000L;
            long end = Math.max(startTime + 15L, now);
            lastSeenCache.put(myId, end);
            MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(myId, startTime, end);
            lastUserActivityTimes.put(myId, now);
            activeSessions.remove(myId);
            selfCurrentSessionStart = 0;
        }
    }

    public boolean isSessionActive(long userId) {
        if (userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return !ApplicationLoader.mainInterfacePaused;
        }
        Long start = activeSessions.get(userId);
        if (start == null) return false;
        Long last = lastUserActivityTimes.get(userId);
        long now = System.currentTimeMillis() / 1000L;
        Integer type = activeSessionTypes.get(userId);
        if (type != null && type == TRIGGER_TYPING) {
            return last != null && (now - last <= 6);
        }
        long lastAction = last != null ? last : start;
        if (now - lastAction > 90) {
            long estimatedEnd = (lastAction > start) ? lastAction : (start + 45L);
            lastSeenCache.put(userId, estimatedEnd);
            MessagesStorage.getInstance(currentAccount).saveUserActivityEnd(userId, start, estimatedEnd);
            activeSessions.remove(userId);
            activeSessionTypes.remove(userId);
            lastUserActivityTimes.remove(userId);
            return false;
        }
        return true;
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0 мин";
        }
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remMinutes = minutes % 60;
        if (hours > 0) {
            return hours + " ч " + remMinutes + " мин";
        }
        return Math.max(1, minutes) + " мин";
    }

    public long getTodayTotalOnlineTime(long userId) {
        if (userId <= 0) {
            return 0;
        }
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long dayStart = cal.getTimeInMillis() / 1000L;

        long total = MessagesStorage.getInstance(currentAccount).getTodayTotalOnlineTime(userId, dayStart);
        if (isSessionActive(userId)) {
            Long start = activeSessions.get(userId);
            if (start != null) {
                long now = System.currentTimeMillis() / 1000L;
                long effectiveStart = Math.max(start, dayStart);
                if (now > effectiveStart) {
                    total += (now - effectiveStart);
                }
            }
        }
        return total;
    }

    public void clearHistory(long userId, Runnable onDone) {
        cancelTypingTimeout(userId);
        activeSessions.remove(userId);
        activeSessionTypes.remove(userId);
        lastSeenCache.remove(userId);
        lastUserActivityTimes.remove(userId);
        if (userId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            selfCurrentSessionStart = 0;
        }
        MessagesStorage.getInstance(currentAccount).clearUserActivityLogs(userId, () ->
                AndroidUtilities.runOnUIThread(() -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
                }));
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateConnectionState) {
            int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
            if (state == ConnectionsManager.ConnectionStateConnected) {
                requestTrackedUserStatuses();
            }
        }
    }

    private long lastStatusSyncTime = 0;

    public void requestTrackedUserStatuses() {
        requestTrackedUserStatuses(false);
    }

    public void requestTrackedUserStatuses(boolean force) {
        if (!HundredGramConfig.trackOnlineStatus) {
            return;
        }
        Set<Long> trackedUsers = HundredGramConfig.trackedOnlineUsers;
        boolean hasTracked = (trackedUsers != null && !trackedUsers.isEmpty()) || HundredGramConfig.notifyAllContactsOnline;
        if (!hasTracked) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!force && now - lastStatusSyncTime < 2 * 60 * 1000L) {
            return;
        }

        int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
        if (state != ConnectionsManager.ConnectionStateConnected && state != ConnectionsManager.ConnectionStateUpdating) {
            return;
        }

        lastStatusSyncTime = now;
        TLRPC.TL_contacts_getStatuses req = new TLRPC.TL_contacts_getStatuses();
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (response instanceof Vector) {
                Vector vector = (Vector) response;
                if (vector.objects == null) {
                    return;
                }
                ArrayList<TLRPC.TL_contactStatus> statuses = new ArrayList<>();
                for (int a = 0; a < vector.objects.size(); a++) {
                    Object obj = vector.objects.get(a);
                    if (obj instanceof TLRPC.TL_contactStatus) {
                        statuses.add((TLRPC.TL_contactStatus) obj);
                    }
                }
                if (!statuses.isEmpty()) {
                    MessagesController.getInstance(currentAccount).processLoadedUserStatuses(statuses);
                }
            }
        });
    }
}
