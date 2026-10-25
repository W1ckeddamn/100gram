package org.telegram.messenger;

import android.util.LongSparseArray;
import android.util.Pair;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.telegram.tgnet.ConnectionsManager;

public class HundredGramReadHistoryManager {

    private static final HundredGramReadHistoryManager[] instances = new HundredGramReadHistoryManager[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] locks = new Object[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            locks[i] = new Object();
        }
    }

    private final int currentAccount;
    // dialogId -> (max_id -> read_date)
    private final LongSparseArray<Map<Integer, Integer>> dmReadHistoryCache = new LongSparseArray<>();
    // "dialogId_mid" -> list of (userId, readDate)
    private final HashMap<String, ArrayList<Pair<Long, Integer>>> groupReadHistoryCache = new HashMap<>();

    public static HundredGramReadHistoryManager getInstance(int account) {
        if (instances[account] == null) {
            synchronized (locks[account]) {
                if (instances[account] == null) {
                    instances[account] = new HundredGramReadHistoryManager(account);
                }
            }
        }
        return instances[account];
    }

    private HundredGramReadHistoryManager(int account) {
        this.currentAccount = account;
        initDatabase();
    }

    private void initDatabase() {
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
                if (database != null) {
                    database.executeFast("CREATE TABLE IF NOT EXISTS hundredgram_read_history (dialog_id INTEGER, mid INTEGER, user_id INTEGER, read_date INTEGER, PRIMARY KEY(dialog_id, mid, user_id))").stepThis().dispose();
                    database.executeFast("CREATE TABLE IF NOT EXISTS hundredgram_dm_read_history (dialog_id INTEGER, max_id INTEGER, read_date INTEGER, PRIMARY KEY(dialog_id, max_id))").stepThis().dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public synchronized void onOutboxRead(long dialogId, int maxId, int date) {
        if (date <= 0) {
            date = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        }
        Map<Integer, Integer> map = dmReadHistoryCache.get(dialogId);
        if (map == null) {
            map = new HashMap<>();
            dmReadHistoryCache.put(dialogId, map);
        }
        map.put(maxId, date);

        final int finalDate = date;
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
                if (database != null) {
                    SQLitePreparedStatement state = database.executeFast("INSERT OR REPLACE INTO hundredgram_dm_read_history VALUES(?, ?, ?)");
                    state.requery();
                    state.bindLong(1, dialogId);
                    state.bindInteger(2, maxId);
                    state.bindInteger(3, finalDate);
                    state.step();
                    state.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public synchronized int getMessageReadDate(long dialogId, int messageId) {
        Map<Integer, Integer> map = dmReadHistoryCache.get(dialogId);
        if (map != null) {
            int bestMaxId = Integer.MAX_VALUE;
            int bestDate = 0;
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int maxId = entry.getKey();
                if (maxId >= messageId && maxId <= bestMaxId) {
                    bestMaxId = maxId;
                    bestDate = entry.getValue();
                }
            }
            if (bestDate > 0) {
                return bestDate;
            }
        }

        // Query database synchronously if needed
        try {
            SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
            if (database != null) {
                SQLiteCursor cursor = database.queryFinalized("SELECT max_id, read_date FROM hundredgram_dm_read_history WHERE dialog_id = ? AND max_id >= ? ORDER BY max_id ASC LIMIT 1", dialogId, messageId);
                if (cursor.next()) {
                    int maxId = cursor.intValue(0);
                    int date = cursor.intValue(1);
                    cursor.dispose();
                    if (map == null) {
                        map = new HashMap<>();
                        dmReadHistoryCache.put(dialogId, map);
                    }
                    map.put(maxId, date);
                    return date;
                }
                cursor.dispose();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    public synchronized void saveGroupReadDates(long dialogId, int messageId, ArrayList<Pair<Long, Integer>> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        String key = dialogId + "_" + messageId;
        groupReadHistoryCache.put(key, new ArrayList<>(participants));

        ArrayList<Pair<Long, Integer>> copy = new ArrayList<>(participants);
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            SQLitePreparedStatement state = null;
            try {
                SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
                if (database != null) {
                    database.beginTransaction();
                    try {
                        state = database.executeFast("INSERT OR REPLACE INTO hundredgram_read_history VALUES(?, ?, ?, ?)");
                        for (Pair<Long, Integer> pair : copy) {
                            state.requery();
                            state.bindLong(1, dialogId);
                            state.bindInteger(2, messageId);
                            state.bindLong(3, pair.first);
                            state.bindInteger(4, pair.second);
                            state.step();
                        }
                    } finally {
                        // SQLiteDatabase has no rollback: closing the transaction is what keeps the
                        // shared storage DB usable after a failed bind/step.
                        database.commitTransaction();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (state != null) {
                    state.dispose();
                }
            }
        });
    }

    public synchronized ArrayList<Pair<Long, Integer>> getGroupReadDates(long dialogId, int messageId) {
        String key = dialogId + "_" + messageId;
        ArrayList<Pair<Long, Integer>> cached = groupReadHistoryCache.get(key);
        if (cached != null && !cached.isEmpty()) {
            return new ArrayList<>(cached);
        }

        ArrayList<Pair<Long, Integer>> result = new ArrayList<>();
        try {
            SQLiteDatabase database = MessagesStorage.getInstance(currentAccount).getDatabase();
            if (database != null) {
                SQLiteCursor cursor = database.queryFinalized("SELECT user_id, read_date FROM hundredgram_read_history WHERE dialog_id = ? AND mid = ?", dialogId, messageId);
                while (cursor.next()) {
                    result.add(new Pair<>(cursor.longValue(0), cursor.intValue(1)));
                }
                cursor.dispose();
                if (!result.isEmpty()) {
                    groupReadHistoryCache.put(key, new ArrayList<>(result));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }
}
