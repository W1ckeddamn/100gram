package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class HundredGramRealNameCache {

    private static final Map<Integer, Map<Long, String>> cache = new HashMap<>();
    private static final Object cacheLock = new Object();

    public static String getRealName(int account, long userId) {
        synchronized (cacheLock) {
            Map<Long, String> accountCache = cache.get(account);
            if (accountCache != null) {
                return accountCache.get(userId);
            }
            return null;
        }
    }

    public static void putRealName(int account, long userId, String realName) {
        synchronized (cacheLock) {
            Map<Long, String> accountCache = cache.computeIfAbsent(account, k -> new HashMap<>());
            accountCache.put(userId, realName);
        }
    }

    public static void clear(int account) {
        synchronized (cacheLock) {
            cache.remove(account);
        }
    }

    public static void load(int account) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_realname_" + account, Activity.MODE_PRIVATE);
        Map<Long, String> accountCache = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            try {
                long userId = Long.parseLong(entry.getKey());
                String realName = (String) entry.getValue();
                if (!TextUtils.isEmpty(realName)) {
                    accountCache.put(userId, realName);
                }
            } catch (NumberFormatException ignore) {
            }
        }
        synchronized (cacheLock) {
            cache.put(account, accountCache);
        }
    }

    public static void save(int account) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_realname_" + account, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        synchronized (cacheLock) {
            Map<Long, String> accountCache = cache.get(account);
            if (accountCache != null) {
                editor.clear();
                for (Map.Entry<Long, String> entry : accountCache.entrySet()) {
                    editor.putString(String.valueOf(entry.getKey()), entry.getValue());
                }
                editor.apply();
            }
        }
    }

    public static void clearAll() {
        synchronized (cacheLock) {
            cache.clear();
        }
    }
}
