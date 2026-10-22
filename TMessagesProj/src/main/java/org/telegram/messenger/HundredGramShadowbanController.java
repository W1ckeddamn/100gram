package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HundredGramShadowbanController {

    private static final Object LOCK = new Object();
    private static volatile Set<Long> shadowBannedUsers;
    private static final String PREF_NAME = "hundredgram_shadowban";
    private static final String KEY_USERS = "shadowbanned_user_ids";

    static {
        loadShadowBannedUsers();
    }

    private static void loadShadowBannedUsers() {
        synchronized (LOCK) {
            HashSet<Long> set = new HashSet<>();
            if (ApplicationLoader.applicationContext != null) {
                SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
                Set<String> stringSet = prefs.getStringSet(KEY_USERS, null);
                if (stringSet != null) {
                    for (String s : stringSet) {
                        try {
                            long id = Long.parseLong(s);
                            if (id != 0) {
                                set.add(id);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            shadowBannedUsers = Collections.unmodifiableSet(set);
        }
    }

    public static boolean isShadowBanned(long userId) {
        if (userId == 0) {
            return false;
        }
        Set<Long> set = shadowBannedUsers;
        return set != null && set.contains(userId);
    }

    public static void addShadowBan(long userId) {
        if (userId == 0) {
            return;
        }
        synchronized (LOCK) {
            HashSet<Long> newSet = new HashSet<>(shadowBannedUsers != null ? shadowBannedUsers : Collections.emptySet());
            if (newSet.add(userId)) {
                saveShadowBannedUsers(newSet);
            }
        }
    }

    public static void removeShadowBan(long userId) {
        if (userId == 0) {
            return;
        }
        synchronized (LOCK) {
            HashSet<Long> newSet = new HashSet<>(shadowBannedUsers != null ? shadowBannedUsers : Collections.emptySet());
            if (newSet.remove(userId)) {
                saveShadowBannedUsers(newSet);
            }
        }
    }

    public static boolean toggleShadowBan(long userId) {
        if (isShadowBanned(userId)) {
            removeShadowBan(userId);
            return false;
        } else {
            addShadowBan(userId);
            return true;
        }
    }

    public static Set<Long> getShadowBannedUsers() {
        Set<Long> set = shadowBannedUsers;
        return set != null ? set : Collections.emptySet();
    }

    public static int getShadowBannedCount() {
        Set<Long> set = shadowBannedUsers;
        return set != null ? set.size() : 0;
    }

    public static void clearAll() {
        synchronized (LOCK) {
            saveShadowBannedUsers(new HashSet<>());
        }
    }

    private static void saveShadowBannedUsers(Set<Long> set) {
        shadowBannedUsers = Collections.unmodifiableSet(set);
        if (ApplicationLoader.applicationContext != null) {
            HashSet<String> stringSet = new HashSet<>();
            for (Long id : set) {
                stringSet.add(String.valueOf(id));
            }
            ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY_USERS, stringSet)
                    .apply();
        }
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }
}
