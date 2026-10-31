package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер локальных кастомных бейджей пользователей.
 * Позволяет назначать специальные бейджи с иконкой, названием и описанием
 * конкретным пользователям (например, Создателю, Разработчику, VIP, Друзьям).
 */
public class HundredGramBadgesController {

    public static final int ICON_CROWN = 0;
    public static final int ICON_STAR = 1;
    public static final int ICON_SHIELD = 2;
    public static final int ICON_VERIFIED = 3;
    public static final int ICON_FIRE = 4;
    public static final int ICON_DEV = 5;
    public static final int ICON_CUSTOM_EMOJI = 6;

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

        public Badge(long userId, String title, String description, int iconType, long customEmojiDocumentId, int color) {
            this.userId = userId;
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
            this.iconType = iconType;
            this.customEmojiDocumentId = customEmojiDocumentId;
            this.color = color;
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
            return new Badge(userId, title, desc, iconType, docId, color);
        }
    }

    private final Map<Long, Badge> badges = new HashMap<>();
    private boolean loaded = false;

    private HundredGramBadgesController() {
        loadBadges();
    }

    public synchronized void loadBadges() {
        badges.clear();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_badges", Context.MODE_PRIVATE);
        String jsonStr = preferences.getString("badges_list", null);
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
        loaded = true;
    }

    public synchronized void saveBadges() {
        JSONArray arr = new JSONArray();
        for (Badge b : badges.values()) {
            arr.put(b.toJson());
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_badges", Context.MODE_PRIVATE);
        preferences.edit().putString("badges_list", arr.toString()).apply();

        // Also save to HundredGramConfig preferences so backup manager exports it
        HundredGramConfig.getPreferences().edit().putString("hundredgram_custom_badges_data", arr.toString()).apply();

        // Post notification to update all cells and dialogs
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }

    public synchronized Badge getBadge(long userId) {
        if (!loaded) {
            loadBadges();
        }
        return badges.get(userId);
    }

    public synchronized boolean hasBadge(long userId) {
        if (!loaded) {
            loadBadges();
        }
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
        if (!loaded) {
            loadBadges();
        }
        return new ArrayList<>(badges.values());
    }

    public synchronized void clearAllBadges() {
        badges.clear();
        saveBadges();
    }

    public Drawable getBadgeDrawable(Badge badge, int defaultColor) {
        if (badge == null) return null;
        Context context = ApplicationLoader.applicationContext;
        int resId;
        switch (badge.iconType) {
            case ICON_STAR:
                resId = R.drawable.msg_premium_liststar;
                break;
            case ICON_SHIELD:
                resId = R.drawable.outline_shield_check;
                break;
            case ICON_VERIFIED:
                resId = R.drawable.verified_check;
                break;
            case ICON_FIRE:
                resId = R.drawable.filled_fire;
                break;
            case ICON_DEV:
                resId = R.drawable.msg_admins;
                break;
            case ICON_CROWN:
            default:
                resId = R.drawable.filled_rating_crown;
                break;
        }

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
            default:
                return "Бейдж";
        }
    }
}
