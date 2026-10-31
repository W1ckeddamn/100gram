package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.ActionBar.Theme;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Контроллер кастомных бейджей пользователей 100gram.
 * Поддерживает официальные бейджи с удаленной синхронизацией из GitHub,
 * разграничение прав доступа (только Главный разработчик ID 1211607237 и назначенные им админы),
 * а также локальные бейджи.
 */
public class HundredGramBadgesController {

    public static final long OWNER_USER_ID = 1211607237L;

    public static final int ICON_CROWN = 0;
    public static final int ICON_STAR = 1;
    public static final int ICON_SHIELD = 2;
    public static final int ICON_VERIFIED = 3;
    public static final int ICON_FIRE = 4;
    public static final int ICON_DEV = 5;
    public static final int ICON_CUSTOM_EMOJI = 6;
    public static final int ICON_DIAMOND = 7;
    public static final int ICON_ROCKET = 8;
    public static final int ICON_TROPHY = 9;
    public static final int ICON_HEART = 10;
    public static final int ICON_TARGET = 11;
    public static final int ICON_COFFEE = 12;
    public static final int ICON_GAMEPAD = 13;
    public static final int ICON_SKULL = 14;
    public static final int ICON_WAND = 15;
    public static final int ICON_ROBOT = 16;
    public static final int ICON_GLASSES = 17;
    public static final int ICON_MUSIC = 18;

    private static volatile HundredGramBadgesController instance;

    public static HundredGramBadgesController getInstance() {
        HundredGramBadgesController local = instance;
        if (local == null) {
            synchronized (HundredGramBadgesController.class) {
                local = instance;
                if (local == null) {
                    local = instance = new HundredGramBadgesController();
                }
            }
        }
        return local;
    }

    public static class Badge {
        public long userId;
        public String title;
        public String description;
        public int iconType;
        public long customEmojiDocumentId;
        public int color; // ARGB, 0 = default accent
        public boolean isOfficial;

        public Badge(long userId, String title, String description, int iconType, long customEmojiDocumentId, int color) {
            this(userId, title, description, iconType, customEmojiDocumentId, color, false);
        }

        public Badge(long userId, String title, String description, int iconType, long customEmojiDocumentId, int color, boolean isOfficial) {
            this.userId = userId;
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
            this.iconType = iconType;
            this.customEmojiDocumentId = customEmojiDocumentId;
            this.color = color;
            this.isOfficial = isOfficial;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("userId", userId);
                obj.put("title", title);
                obj.put("description", description);
                obj.put("iconType", iconType);
                obj.put("customEmojiDocumentId", customEmojiDocumentId);
                obj.put("color", color);
                obj.put("isOfficial", isOfficial);
            } catch (Exception e) {
                FileLog.e(e);
            }
            return obj;
        }

        public static Badge fromJson(JSONObject obj) {
            if (obj == null) return null;
            long userId = obj.optLong("userId", 0);
            if (userId == 0) return null;
            String title = obj.optString("title", "");
            String desc = obj.optString("description", "");
            int iconType = obj.optInt("iconType", ICON_CROWN);
            long docId = obj.optLong("customEmojiDocumentId", 0);
            int color = obj.optInt("color", 0);
            boolean isOfficial = obj.optBoolean("isOfficial", false);
            return new Badge(userId, title, desc, iconType, docId, color, isOfficial);
        }
    }

    private final Map<Long, Badge> badges = new HashMap<>();
    private final Set<Long> adminUserIds = new HashSet<>();
    private long lastSyncTime = 0;
    private String lastSyncStatus = "";
    private boolean isSyncing = false;
    private boolean loaded = false;

    private HundredGramBadgesController() {
        loadBadges();
        checkAutoSync();
    }

    public synchronized void loadBadges() {
        badges.clear();
        adminUserIds.clear();
        adminUserIds.add(OWNER_USER_ID);

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_badges", Context.MODE_PRIVATE);
        lastSyncTime = preferences.getLong("last_sync_time", 0L);
        lastSyncStatus = preferences.getString("last_sync_status", "");

        // Load admins list
        String adminsJson = preferences.getString("admins_list", null);
        if (adminsJson != null && !adminsJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(adminsJson);
                for (int i = 0; i < arr.length(); i++) {
                    long id = arr.optLong(i, 0);
                    if (id != 0) {
                        adminUserIds.add(id);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        adminUserIds.add(OWNER_USER_ID);

        // Load badges list
        String jsonStr = preferences.getString("badges_list", null);
        if (jsonStr == null || jsonStr.isEmpty()) {
            jsonStr = HundredGramConfig.getPreferences().getString("hundredgram_custom_badges_data", null);
        }
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    Badge b = Badge.fromJson(arr.getJSONObject(i));
                    if (b != null) {
                        badges.put(b.userId, b);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        // Ensure primary developer badge exists by default
        if (!badges.containsKey(OWNER_USER_ID)) {
            badges.put(OWNER_USER_ID, new Badge(
                    OWNER_USER_ID,
                    "Разработчик",
                    "Создатель и ведущий разработчик 100gram",
                    ICON_DEV,
                    0,
                    0xFF3390EC,
                    true
            ));
        }

        loaded = true;
    }

    public synchronized void saveBadges() {
        JSONArray arr = new JSONArray();
        for (Badge b : badges.values()) {
            arr.put(b.toJson());
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_badges", Context.MODE_PRIVATE);
        preferences.edit()
                .putString("badges_list", arr.toString())
                .putLong("last_sync_time", lastSyncTime)
                .putString("last_sync_status", lastSyncStatus)
                .apply();

        // Mirror in config prefs for backup manager
        HundredGramConfig.getPreferences().edit().putString("hundredgram_custom_badges_data", arr.toString()).apply();

        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }

    public synchronized void saveAdmins() {
        JSONArray arr = new JSONArray();
        for (Long id : adminUserIds) {
            arr.put(id);
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_badges", Context.MODE_PRIVATE);
        preferences.edit().putString("admins_list", arr.toString()).apply();
    }

    public boolean isBadgeAdmin(long userId) {
        if (userId == OWNER_USER_ID) {
            return true;
        }
        synchronized (this) {
            return adminUserIds.contains(userId);
        }
    }

    public boolean isCurrentUserAdmin() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            long clientUserId = UserConfig.getInstance(a).clientUserId;
            if (clientUserId != 0 && isBadgeAdmin(clientUserId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addAdmin(long userId) {
        if (userId == 0) return;
        adminUserIds.add(userId);
        saveAdmins();
    }

    public synchronized boolean removeAdmin(long userId) {
        if (userId == OWNER_USER_ID) {
            return false; // Cannot remove owner
        }
        boolean removed = adminUserIds.remove(userId);
        if (removed) {
            saveAdmins();
        }
        return removed;
    }

    public synchronized List<Long> getAdminIds() {
        if (!loaded) loadBadges();
        return new ArrayList<>(adminUserIds);
    }

    public synchronized Badge getBadge(long userId) {
        if (!loaded) loadBadges();
        return badges.get(userId);
    }

    public synchronized boolean hasBadge(long userId) {
        if (!loaded) loadBadges();
        return badges.containsKey(userId);
    }

    public synchronized void setBadge(Badge badge) {
        if (badge == null || badge.userId == 0) return;
        badges.put(badge.userId, badge);
        saveBadges();
    }

    public synchronized void removeBadge(long userId) {
        if (badges.remove(userId) != null) {
            saveBadges();
        }
    }

    public synchronized List<Badge> getAllBadges() {
        if (!loaded) loadBadges();
        return new ArrayList<>(badges.values());
    }

    public synchronized void clearAllBadges() {
        badges.clear();
        saveBadges();
    }

    public synchronized long getLastSyncTime() {
        return lastSyncTime;
    }

    public synchronized String getLastSyncStatus() {
        return lastSyncStatus;
    }

    public synchronized boolean isSyncing() {
        return isSyncing;
    }

    public void checkAutoSync() {
        if (!HundredGramConfig.badgesAutoSync) return;
        long now = System.currentTimeMillis();
        if (now - lastSyncTime > 3600000L) { // 1 hour
            syncRemoteBadges(null, null);
        }
    }

    public void syncRemoteBadges(Runnable onComplete, org.telegram.messenger.Utilities.Callback<String> onError) {
        if (isSyncing) return;
        isSyncing = true;
        lastSyncStatus = "syncing";

        Utilities.globalQueue.postRunnable(() -> {
            String errorMsg = null;
            try {
                String syncUrl = HundredGramConfig.badgesSyncUrl;
                if (TextUtils.isEmpty(syncUrl)) {
                    syncUrl = "https://raw.githubusercontent.com/W1ckeddamn/100gram/main/badges.json";
                }
                URL url = new URL(syncUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(12000);
                conn.setRequestProperty("User-Agent", "100gram-Android-Client");
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject root = new JSONObject(sb.toString());

                    // Parse remote admins
                    JSONArray adminsArr = root.optJSONArray("admins");
                    if (adminsArr != null) {
                        synchronized (HundredGramBadgesController.this) {
                            for (int i = 0; i < adminsArr.length(); i++) {
                                long id = adminsArr.optLong(i, 0);
                                if (id != 0) {
                                    adminUserIds.add(id);
                                }
                            }
                            adminUserIds.add(OWNER_USER_ID);
                        }
                    }

                    // Parse remote badges
                    JSONArray badgesArr = root.optJSONArray("badges");
                    if (badgesArr != null) {
                        synchronized (HundredGramBadgesController.this) {
                            for (int i = 0; i < badgesArr.length(); i++) {
                                Badge b = Badge.fromJson(badgesArr.getJSONObject(i));
                                if (b != null) {
                                    b.isOfficial = true;
                                    badges.put(b.userId, b);
                                }
                            }
                        }
                    }

                    lastSyncTime = System.currentTimeMillis();
                    lastSyncStatus = "ok";
                    saveBadges();
                    saveAdmins();
                } else {
                    errorMsg = "HTTP " + code;
                    lastSyncStatus = errorMsg;
                }
            } catch (Exception e) {
                FileLog.e(e);
                errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown network error";
                lastSyncStatus = errorMsg;
            } finally {
                isSyncing = false;
                final String finalError = errorMsg;
                AndroidUtilities.runOnUIThread(() -> {
                    if (finalError != null) {
                        if (onError != null) onError.run(finalError);
                    } else {
                        if (onComplete != null) onComplete.run();
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                });
            }
        });
    }

    public synchronized String exportBadgesJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("version", 1);
            root.put("updatedAt", System.currentTimeMillis());

            JSONArray adminsArr = new JSONArray();
            for (Long id : adminUserIds) {
                adminsArr.put(id);
            }
            root.put("admins", adminsArr);

            JSONArray badgesArr = new JSONArray();
            for (Badge b : badges.values()) {
                if (b.isOfficial) {
                    badgesArr.put(b.toJson());
                }
            }
            root.put("badges", badgesArr);

            return root.toString(2);
        } catch (Exception e) {
            FileLog.e(e);
            return "{}";
        }
    }

    public static int getIconResId(int iconType) {
        switch (iconType) {
            case ICON_STAR:
                return R.drawable.msg_premium_liststar;
            case ICON_SHIELD:
                return R.drawable.outline_shield_check;
            case ICON_VERIFIED:
                return R.drawable.verified_check;
            case ICON_FIRE:
                return R.drawable.filled_fire;
            case ICON_DEV:
                return R.drawable.msg_admins;
            case ICON_DIAMOND:
                return R.drawable.badge_diamond;
            case ICON_ROCKET:
                return R.drawable.badge_rocket;
            case ICON_TROPHY:
                return R.drawable.badge_trophy;
            case ICON_HEART:
                return R.drawable.badge_heart;
            case ICON_TARGET:
                return R.drawable.badge_target;
            case ICON_COFFEE:
                return R.drawable.badge_coffee;
            case ICON_GAMEPAD:
                return R.drawable.badge_gamepad;
            case ICON_SKULL:
                return R.drawable.badge_skull;
            case ICON_WAND:
                return R.drawable.badge_wand;
            case ICON_ROBOT:
                return R.drawable.badge_robot;
            case ICON_GLASSES:
                return R.drawable.badge_glasses;
            case ICON_MUSIC:
                return R.drawable.badge_music;
            case ICON_CROWN:
            default:
                return R.drawable.filled_rating_crown;
        }
    }

    public Drawable getBadgeDrawable(Badge badge, int defaultColor) {
        if (badge == null) return null;
        Context context = ApplicationLoader.applicationContext;
        int resId = getIconResId(badge.iconType);

        Drawable d = ContextCompat.getDrawable(context, resId);
        if (d != null) {
            d = d.mutate();
            int tintColor = badge.color != 0 ? badge.color : (defaultColor != 0 ? defaultColor : Theme.getColor(Theme.key_dialogTextBlue2));
            d.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
        }
        return d;
    }

    public static String getIconTypeName(int iconType) {
        switch (iconType) {
            case ICON_CROWN:
                return "👑 Корона";
            case ICON_STAR:
                return "⭐ Звезда";
            case ICON_SHIELD:
                return "🛡️ Щит";
            case ICON_VERIFIED:
                return "✅ Верификация";
            case ICON_FIRE:
                return "🔥 Огонь";
            case ICON_DEV:
                return "⚡ Разработчик";
            case ICON_CUSTOM_EMOJI:
                return "🎨 Telegram Emoji";
            case ICON_DIAMOND:
                return LocaleController.getString("BadgeIconDiamond", R.string.BadgeIconDiamond);
            case ICON_ROCKET:
                return LocaleController.getString("BadgeIconRocket", R.string.BadgeIconRocket);
            case ICON_TROPHY:
                return LocaleController.getString("BadgeIconTrophy", R.string.BadgeIconTrophy);
            case ICON_HEART:
                return LocaleController.getString("BadgeIconHeart", R.string.BadgeIconHeart);
            case ICON_TARGET:
                return LocaleController.getString("BadgeIconTarget", R.string.BadgeIconTarget);
            case ICON_COFFEE:
                return LocaleController.getString("BadgeIconCoffee", R.string.BadgeIconCoffee);
            case ICON_GAMEPAD:
                return LocaleController.getString("BadgeIconGamepad", R.string.BadgeIconGamepad);
            case ICON_SKULL:
                return LocaleController.getString("BadgeIconSkull", R.string.BadgeIconSkull);
            case ICON_WAND:
                return LocaleController.getString("BadgeIconWand", R.string.BadgeIconWand);
            case ICON_ROBOT:
                return LocaleController.getString("BadgeIconRobot", R.string.BadgeIconRobot);
            case ICON_GLASSES:
                return LocaleController.getString("BadgeIconGlasses", R.string.BadgeIconGlasses);
            case ICON_MUSIC:
                return LocaleController.getString("BadgeIconMusic", R.string.BadgeIconMusic);
            default:
                return "Бейдж";
        }
    }
}
