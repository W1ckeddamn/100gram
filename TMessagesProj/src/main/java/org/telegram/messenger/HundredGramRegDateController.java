package org.telegram.messenger;

import android.content.Context;
import androidx.collection.LruCache;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

/**
 * Calculates and formats estimated Telegram user registration dates
 * based on user ID interpolation tables and dialog photo timestamps.
 * Ported and improved from exteraGram.
 */
public class HundredGramRegDateController {

    private static final HundredGramRegDateController[] instances = new HundredGramRegDateController[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] lockObjects = new Object[UserConfig.MAX_ACCOUNT_COUNT];

    private static long[] regIds;
    private static long[] regDates;
    private static final Object initLock = new Object();

    private final int currentAccount;
    private final LruCache<Long, String> regDateCache = new LruCache<>(256);

    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            lockObjects[i] = new Object();
        }
    }

    public static HundredGramRegDateController getInstance(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            account = UserConfig.selectedAccount;
        }
        HundredGramRegDateController local = instances[account];
        if (local == null) {
            synchronized (lockObjects[account]) {
                local = instances[account];
                if (local == null) {
                    local = new HundredGramRegDateController(account);
                    instances[account] = local;
                }
            }
        }
        return local;
    }

    private HundredGramRegDateController(int account) {
        this.currentAccount = account;
    }

    private static void ensureLoaded() {
        if (regIds != null && regDates != null) {
            return;
        }
        synchronized (initLock) {
            if (regIds != null && regDates != null) {
                return;
            }
            try {
                Context context = ApplicationLoader.applicationContext;
                if (context == null) {
                    return;
                }
                InputStream is = context.getAssets().open("registration_dates.json");
                byte[] buffer = new byte[is.available()];
                int read = is.read(buffer);
                is.close();

                String jsonStr = new String(buffer, 0, read, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(jsonStr);

                ArrayList<long[]> list = new ArrayList<>();
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        long id = Long.parseLong(key);
                        long date = json.getLong(key);
                        list.add(new long[]{id, date});
                    } catch (Exception ignore) {}
                }

                Collections.sort(list, new Comparator<long[]>() {
                    @Override
                    public int compare(long[] a, long[] b) {
                        return Long.compare(a[0], b[0]);
                    }
                });

                int size = list.size();
                long[] ids = new long[size];
                long[] dates = new long[size];
                for (int i = 0; i < size; i++) {
                    long[] pair = list.get(i);
                    ids[i] = pair[0];
                    dates[i] = pair[1];
                }
                regIds = ids;
                regDates = dates;
            } catch (Exception e) {
                FileLog.e("HundredGramRegDateController: failed to load registration_dates.json", e);
            }
        }
    }

    /**
     * Estimates registration timestamp in seconds for a user ID.
     */
    public long findUserRegistrationDate(long userId) {
        ensureLoaded();

        long estimatedDate = 0;
        if (regIds != null && regDates != null && regIds.length > 0) {
            if (userId <= regIds[0]) {
                estimatedDate = regDates[0];
            } else if (userId >= regIds[regIds.length - 1]) {
                estimatedDate = regDates[regDates.length - 1];
            } else {
                int index = Arrays.binarySearch(regIds, userId);
                if (index >= 0) {
                    estimatedDate = regDates[index];
                } else {
                    int insertionPoint = -index - 1;
                    int prev = insertionPoint - 1;
                    int next = insertionPoint;
                    long id0 = regIds[prev];
                    long id1 = regIds[next];
                    long t0 = regDates[prev];
                    long t1 = regDates[next];
                    if (id1 > id0) {
                        double fraction = (double) (userId - id0) / (double) (id1 - id0);
                        estimatedDate = (long) (t0 + (t1 - t0) * fraction);
                    } else {
                        estimatedDate = t0;
                    }
                }
            }
        }

        // Cross-verify with earliest known profile photo timestamp
        int earliestPhotoDate = Integer.MAX_VALUE;
        try {
            MessagesController mc = MessagesController.getInstance(currentAccount);
            TLRPC.UserFull userFull = mc != null ? mc.getUserFull(userId) : null;
            if (userFull != null && userFull.profile_photo != null && userFull.profile_photo.date > 0) {
                earliestPhotoDate = Math.min(earliestPhotoDate, userFull.profile_photo.date);
            }
            MessagesController.DialogPhotos dialogPhotos = mc != null ? mc.getDialogPhotos(userId) : null;
            if (dialogPhotos != null && dialogPhotos.photos != null && !dialogPhotos.photos.isEmpty()) {
                for (TLRPC.Photo photo : dialogPhotos.photos) {
                    if (photo != null && photo.date > 0) {
                        earliestPhotoDate = Math.min(earliestPhotoDate, photo.date);
                    }
                }
            }
        } catch (Exception ignore) {}

        if (earliestPhotoDate != Integer.MAX_VALUE && earliestPhotoDate > 0) {
            if (estimatedDate == 0 || earliestPhotoDate < estimatedDate) {
                estimatedDate = earliestPhotoDate;
            }
        }

        return estimatedDate;
    }

    /**
     * Formats user registration date into a localized descriptive string.
     */
    public String getUserRegistrationDate(long userId) {
        String cached = regDateCache.get(userId);
        if (cached != null) {
            return cached;
        }

        long regTimestamp = findUserRegistrationDate(userId);
        if (regTimestamp == 0) {
            return null;
        }

        String dateFormatted = LocaleController.formatYearMont(regTimestamp, true);
        if (dateFormatted == null || dateFormatted.isEmpty()) {
            dateFormatted = LocaleController.formatDateChat(regTimestamp);
        }

        MessagesController mc = MessagesController.getInstance(currentAccount);
        TLRPC.User user = mc != null ? mc.getUser(userId) : null;
        String name = user != null ? ContactsController.formatName(user) : "";

        long selfUserId = UserConfig.getInstance(currentAccount).clientUserId;
        String result;

        if (userId == selfUserId) {
            if (regDates != null && regDates.length > 0 && regTimestamp <= regDates[0]) {
                result = LocaleController.formatString("CreationDateSelfEarlier", R.string.CreationDateSelfEarlier, dateFormatted);
            } else if (regDates != null && regDates.length > 0 && regTimestamp >= regDates[regDates.length - 1]) {
                result = LocaleController.formatString("CreationDateSelfLater", R.string.CreationDateSelfLater, dateFormatted);
            } else {
                result = LocaleController.formatString("CreationDateSelfApproximately", R.string.CreationDateSelfApproximately, dateFormatted);
            }
        } else {
            if (regDates != null && regDates.length > 0 && regTimestamp <= regDates[0]) {
                result = LocaleController.formatString("CreationDateUserEarlier", R.string.CreationDateUserEarlier, name, dateFormatted);
            } else if (regDates != null && regDates.length > 0 && regTimestamp >= regDates[regDates.length - 1]) {
                result = LocaleController.formatString("CreationDateUserLater", R.string.CreationDateUserLater, name, dateFormatted);
            } else {
                result = LocaleController.formatString("CreationDateUserApproximately", R.string.CreationDateUserApproximately, name, dateFormatted);
            }
        }

        regDateCache.put(userId, result);
        return result;
    }

    /**
     * Formats chat creation date into a localized string.
     */
    public String getChatCreationDate(TLRPC.Chat chat) {
        if (chat == null || chat.date == 0) {
            return null;
        }
        String dateFormatted = LocaleController.formatYearMont(chat.date, true);
        if (dateFormatted == null || dateFormatted.isEmpty()) {
            dateFormatted = LocaleController.formatDateChat(chat.date);
        }
        String title = chat.title != null ? chat.title : "";
        return LocaleController.formatString("CreationDateChat", R.string.CreationDateChat, title, dateFormatted);
    }
}
