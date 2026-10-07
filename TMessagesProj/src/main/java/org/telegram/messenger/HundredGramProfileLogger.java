package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

import java.util.ArrayList;
import java.util.Locale;

public class HundredGramProfileLogger {

    private static volatile HundredGramProfileLogger Instance;

    public static HundredGramProfileLogger getInstance() {
        HundredGramProfileLogger local = Instance;
        if (local == null) {
            synchronized (HundredGramProfileLogger.class) {
                local = Instance;
                if (local == null) {
                    Instance = local = new HundredGramProfileLogger();
                }
            }
        }
        return local;
    }

    public static class ProfileChangeEntry {
        public final long userId;
        public final String fieldName; // "name", "username", "phone", "bio", "birthday", "gifts"
        public final String oldValue;
        public final String newValue;
        public final long date; // Unix timestamp in seconds

        public ProfileChangeEntry(long userId, String fieldName, String oldValue, String newValue, long date) {
            this.userId = userId;
            this.fieldName = fieldName;
            this.oldValue = oldValue != null ? oldValue : "";
            this.newValue = newValue != null ? newValue : "";
            this.date = date;
        }
    }

    public void checkAndLogProfileChanges(int account, TLRPC.User oldUser, TLRPC.User newUser) {
        if (!HundredGramConfig.profileHistoryEnabled) {
            return;
        }
        if (oldUser == null || newUser == null) {
            return;
        }
        if (newUser.self || newUser.bot || newUser.id == 777000 || newUser.id / 1000 == 333) {
            return;
        }

        final long userId = newUser.id;
        final long now = System.currentTimeMillis() / 1000L;

        // 1. Name change (first_name, last_name)
        String oldName = buildFullName(oldUser);
        String newName = buildFullName(newUser);
        if (!TextUtils.isEmpty(oldName) && !TextUtils.isEmpty(newName) && !oldName.equals(newName)) {
            logChange(account, userId, "name", oldName, newName, now);
        }

        // 2. Username change
        String oldUsername = oldUser.username != null ? oldUser.username : "";
        String newUsername = newUser.username != null ? newUser.username : "";
        if (!oldUsername.equalsIgnoreCase(newUsername) && (!TextUtils.isEmpty(oldUsername) || !TextUtils.isEmpty(newUsername))) {
            logChange(account, userId, "username",
                    TextUtils.isEmpty(oldUsername) ? "" : "@" + oldUsername,
                    TextUtils.isEmpty(newUsername) ? "" : "@" + newUsername,
                    now);
        }

        // 3. Phone change (visibility or number change)
        String oldPhone = oldUser.phone != null ? oldUser.phone.trim() : "";
        String newPhone = newUser.phone != null ? newUser.phone.trim() : "";
        if (!oldPhone.equals(newPhone)) {
            if (!TextUtils.isEmpty(oldPhone) || !TextUtils.isEmpty(newPhone)) {
                logChange(account, userId, "phone",
                        TextUtils.isEmpty(oldPhone) ? "Hidden" : "+" + oldPhone,
                        TextUtils.isEmpty(newPhone) ? "Hidden" : "+" + newPhone,
                        now);
            }
        }
    }

    public void checkAndLogUserFull(int account, TLRPC.UserFull oldFull, TLRPC.UserFull newFull) {
        if (oldFull == null || newFull == null || newFull.user == null) {
            return;
        }
        if (newFull.user.self || newFull.user.bot) {
            return;
        }

        final long userId = newFull.user.id;
        final long now = System.currentTimeMillis() / 1000L;

        // 1. Bio / About
        String oldBio = oldFull.about != null ? oldFull.about.trim() : "";
        String newBio = newFull.about != null ? newFull.about.trim() : "";
        if (!oldBio.equals(newBio)) {
            if (!TextUtils.isEmpty(oldBio) || !TextUtils.isEmpty(newBio)) {
                logChange(account, userId, "bio", oldBio, newBio, now);
            }
        }

        // 2. Birthday
        String oldBday = formatBirthday(oldFull.birthday);
        String newBday = formatBirthday(newFull.birthday);
        if (!oldBday.equals(newBday)) {
            if (!TextUtils.isEmpty(oldBday) || !TextUtils.isEmpty(newBday)) {
                logChange(account, userId, "birthday", oldBday, newBday, now);
            }
        }

        // 3. Star Gifts count
        int oldGifts = oldFull.stargifts_count;
        int newGifts = newFull.stargifts_count;
        if (oldGifts != newGifts && (oldGifts > 0 || newGifts > 0)) {
            logChange(account, userId, "gifts", String.valueOf(oldGifts), String.valueOf(newGifts), now);
        }
    }

    public void logChange(int account, long userId, String fieldName, String oldValue, String newValue, long date) {
        if (userId <= 0 || TextUtils.isEmpty(fieldName)) {
            return;
        }

        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteDatabase db = storage.getDatabase();
            if (db == null) {
                return;
            }

            SQLiteCursor cursor = null;
            try {
                // Check if last record for this user and field is already identical (avoid duplicates)
                cursor = db.queryFinalized(String.format(Locale.US,
                        "SELECT new_value FROM profile_changes_log WHERE user_id = %d AND field_name = '%s' ORDER BY date DESC LIMIT 1",
                        userId, fieldName));
                if (cursor.next()) {
                    String lastNewVal = cursor.stringValue(0);
                    if (newValue.equals(lastNewVal)) {
                        return; // Already logged this exact state
                    }
                }

                SQLitePreparedStatement state = db.executeFast(
                        "INSERT INTO profile_changes_log (user_id, field_name, old_value, new_value, date) VALUES (?, ?, ?, ?, ?)");
                state.requery();
                state.bindLong(1, userId);
                state.bindString(2, fieldName);
                state.bindString(3, oldValue != null ? oldValue : "");
                state.bindString(4, newValue != null ? newValue : "");
                state.bindLong(5, date);
                state.step();
                state.dispose();
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public void getProfileChanges(int account, long userId, int limit, Utilities.Callback<ArrayList<ProfileChangeEntry>> callback) {
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            ArrayList<ProfileChangeEntry> result = new ArrayList<>();
            SQLiteDatabase db = storage.getDatabase();
            if (db == null) {
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(result));
                }
                return;
            }

            SQLiteCursor cursor = null;
            try {
                cursor = db.queryFinalized(String.format(Locale.US,
                        "SELECT field_name, old_value, new_value, date FROM profile_changes_log WHERE user_id = %d ORDER BY date DESC LIMIT %d",
                        userId, limit > 0 ? limit : 200));
                while (cursor.next()) {
                    String field = cursor.stringValue(0);
                    String oldVal = cursor.stringValue(1);
                    String newVal = cursor.stringValue(2);
                    long date = cursor.longValue(3);
                    result.add(new ProfileChangeEntry(userId, field, oldVal, newVal, date));
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(result));
            }
        });
    }

    public void getProfileChangesCount(int account, long userId, Utilities.Callback<Integer> callback) {
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            int count = 0;
            SQLiteDatabase db = storage.getDatabase();
            if (db == null) {
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.run(0));
                }
                return;
            }

            SQLiteCursor cursor = null;
            try {
                cursor = db.queryFinalized(String.format(Locale.US,
                        "SELECT COUNT(*) FROM profile_changes_log WHERE user_id = %d", userId));
                if (cursor.next()) {
                    count = cursor.intValue(0);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            final int finalCount = count;
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.run(finalCount));
            }
        });
    }

    public void clearProfileChanges(int account, long userId, Runnable onDone) {
        MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteDatabase db = storage.getDatabase();
            if (db != null) {
                try {
                    db.executeFast(String.format(Locale.US,
                            "DELETE FROM profile_changes_log WHERE user_id = %d", userId)).stepThis().dispose();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
        });
    }

    private static String buildFullName(TLRPC.User user) {
        if (user == null) {
            return "";
        }
        String first = user.first_name != null ? user.first_name.trim() : "";
        String last = user.last_name != null ? user.last_name.trim() : "";
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private static String formatBirthday(TL_account.TL_birthday birthday) {
        if (birthday == null) {
            return "";
        }
        if (birthday.year > 0) {
            return String.format(Locale.US, "%02d.%02d.%04d", birthday.day, birthday.month, birthday.year);
        } else if (birthday.day > 0 && birthday.month > 0) {
            return String.format(Locale.US, "%02d.%02d", birthday.day, birthday.month);
        }
        return "";
    }
}
