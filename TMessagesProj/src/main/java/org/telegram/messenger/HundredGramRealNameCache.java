package org.telegram.messenger;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.LongSparseArray;

import org.telegram.tgnet.TLRPC;

import java.util.Arrays;
import java.util.Map;

public class HundredGramRealNameCache {

    private static final LongSparseArray[] cache = new LongSparseArray[5];
    private static final SharedPreferences[] prefs = new SharedPreferences[5];
    private static final SharedPreferences.Editor[] editors = new SharedPreferences.Editor[5];
    private static boolean pendingSave;
    private static final int MAX_ENTRIES = 3000;
    private static final int TRIM_TARGET = 2000;

    /* Entry structure: first_name + "\t" + last_name + "\t" + timestamp */
    private static class Entry {
        final String firstName;
        final String lastName;
        final int timestamp;

        Entry(String firstName, String lastName, int timestamp) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.timestamp = timestamp;
        }

        String serialize() {
            return firstName + "\t" + lastName + "\t" + timestamp;
        }

        static Entry deserialize(String str) {
            String[] parts = str.split("\t", -1);
            if (parts.length >= 3) {
                try {
                    return new Entry(parts[0], parts[1], Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignore) {
                }
            }
            return new Entry(str, "", 0);
        }
    }

    public static LongSparseArray getCache(int account) {
        if (account < 0 || account >= 5) return null;
        synchronized (HundredGramRealNameCache.class) {
            LongSparseArray accountCache = cache[account];
            if (accountCache != null) {
                return accountCache;
            }
            LongSparseArray newCache = new LongSparseArray();
            SharedPreferences prefs = getPrefs(account);
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (entry.getValue() instanceof String) {
                    try {
                        long userId = Long.parseLong(entry.getKey());
                        Entry entryObj = Entry.deserialize((String) entry.getValue());
                        newCache.put(userId, entryObj);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            cache[account] = newCache;
            return newCache;
        }
    }

    public static void put(int account, long userId, String firstName, String lastName) {
        if (account < 0 || account >= 5 || userId == 0) return;
        String trimmedFirst = firstName != null ? firstName.trim() : "";
        String trimmedLast = lastName != null ? lastName.trim() : "";
        if (trimmedFirst.isEmpty() && trimmedLast.isEmpty()) return;

        synchronized (HundredGramRealNameCache.class) {
            LongSparseArray accountCache = getCache(account);
            if (accountCache == null) return;

            Entry existing = (Entry) accountCache.get(userId);
            if (existing != null && TextUtils.equals(existing.firstName, trimmedFirst) && TextUtils.equals(existing.lastName, trimmedLast)) {
                return;
            }

            int timestamp = (int) (System.currentTimeMillis() / 1000);
            Entry newEntry = new Entry(trimmedFirst, trimmedLast, timestamp);
            accountCache.put(userId, newEntry);

            SharedPreferences.Editor editor = getEditor(account);
            if (editor == null) return;
            editor.putString(Long.toString(userId), newEntry.serialize());

            if (accountCache.size() > MAX_ENTRIES) {
                trim(accountCache, editor);
            }

            if (!pendingSave) {
                pendingSave = true;
                Utilities.globalQueue.postRunnable(saveRunnable, 5000L);
            }
        }
    }

    private static Runnable saveRunnable = new Runnable() {
        @Override
        public void run() {
            saveAll();
        }
    };

    // shortcut: O(n) sort only when the cache exceeds MAX_ENTRIES; switch to a sorted structure if the limit grows.
    private static void trim(LongSparseArray accountCache, SharedPreferences.Editor editor) {
        int size = accountCache.size();
        int toRemove = size - TRIM_TARGET;
        if (toRemove <= 0) return;
        int[] timestamps = new int[size];
        for (int i = 0; i < size; i++) {
            Entry entry = (Entry) accountCache.valueAt(i);
            timestamps[i] = entry == null ? 0 : entry.timestamp;
        }
        Arrays.sort(timestamps);
        int threshold = timestamps[toRemove];
        for (int i = accountCache.size() - 1; i >= 0 && toRemove > 0; i--) {
            Entry entry = (Entry) accountCache.valueAt(i);
            if ((entry == null ? 0 : entry.timestamp) <= threshold) {
                editor.remove(Long.toString(accountCache.keyAt(i)));
                accountCache.removeAt(i);
                toRemove--;
            }
        }
    }

    public static void load(int account) {
        getCache(account);
    }

    // OpexGram parity: cache names of strangers, so the name they had before they became a contact is preserved.
    public static void putIfStranger(int account, TLRPC.User user) {
        if (user == null || user.contact || user.self || user.bot || user.deleted) return;
        put(account, user.id, user.first_name, user.last_name);
    }

    private static void saveAll() {
        synchronized (HundredGramRealNameCache.class) {
            pendingSave = false;
            for (int i = 0; i < 5; i++) {
                SharedPreferences.Editor editor = editors[i];
                if (editor != null) {
                    editor.apply();
                    editors[i] = null;
                }
            }
        }
    }

    public static String getRealName(int account, long userId) {
        if (account < 0 || account >= 5 || userId == 0) return null;
        synchronized (HundredGramRealNameCache.class) {
            LongSparseArray accountCache = getCache(account);
            if (accountCache == null) return null;
            Entry entry = (Entry) accountCache.get(userId);
            if (entry != null) {
                String name = ContactsController.formatName(entry.firstName, entry.lastName);
                return TextUtils.isEmpty(name) ? null : name;
            }
            return null;
        }
    }

    private static SharedPreferences getPrefs(int account) {
        if (account < 0 || account >= 5 || ApplicationLoader.applicationContext == null) return null;
        synchronized (HundredGramRealNameCache.class) {
            SharedPreferences pref = prefs[account];
            if (pref == null) {
                pref = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_realname_" + account, 0);
                prefs[account] = pref;
            }
            return pref;
        }
    }

    private static SharedPreferences.Editor getEditor(int account) {
        if (account < 0 || account >= 5) return null;
        synchronized (HundredGramRealNameCache.class) {
            SharedPreferences.Editor editor = editors[account];
            if (editor == null) {
                editor = getPrefs(account).edit();
                editors[account] = editor;
            }
            return editor;
        }
    }

    public static void clear(int account) {
        if (account < 0 || account >= 5) return;
        synchronized (HundredGramRealNameCache.class) {
            cache[account] = null;
            SharedPreferences.Editor editor = editors[account];
            if (editor != null) {
                editor.clear().apply();
                editors[account] = null;
            }
        }
    }

    public static void clearAll() {
        synchronized (HundredGramRealNameCache.class) {
            for (int i = 0; i < 5; i++) {
                cache[i] = null;
                SharedPreferences.Editor editor = editors[i];
                if (editor != null) {
                    editor.clear().apply();
                    editors[i] = null;
                }
            }
        }
    }
}
