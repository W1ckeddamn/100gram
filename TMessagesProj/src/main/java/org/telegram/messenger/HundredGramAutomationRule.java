package org.telegram.messenger;

import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

public class HundredGramAutomationRule {

    public static final int MEDIA_ANY = 0;
    public static final int MEDIA_PHOTO = 1;
    public static final int MEDIA_VIDEO = 2;
    public static final int MEDIA_DOCUMENT = 3;
    public static final int MEDIA_VOICE_ROUND = 4;

    public long id;
    public String name;
    public boolean enabled = true;

    // Conditions (IF)
    public long dialogId; // 0 for any chat
    public String keyword; // empty for any
    public int mediaType = MEDIA_ANY;

    // Actions (THEN)
    public boolean saveToSaved;
    public boolean markAsRead;
    public boolean saveToGallery;
    public long forwardToChatId;
    public boolean priorityNotify;

    public HundredGramAutomationRule() {
        // Комбинируем время + случайное число во избежание коллизий ID
        this.id = System.currentTimeMillis() * 1000L + (long) (Math.random() * 1000);
    }

    public boolean matches(MessageObject message) {
        if (message == null || !enabled) {
            return false;
        }

        // Check dialogId
        if (dialogId != 0 && message.getDialogId() != dialogId) {
            return false;
        }

        // Check media type
        if (mediaType != MEDIA_ANY) {
            if (mediaType == MEDIA_PHOTO && !message.isPhoto()) {
                return false;
            } else if (mediaType == MEDIA_VIDEO && !message.isVideo()) {
                return false;
            } else if (mediaType == MEDIA_DOCUMENT && !message.isDocument()) {
                return false;
            } else if (mediaType == MEDIA_VOICE_ROUND && !message.isVoice() && !message.isRoundVideo()) {
                return false;
            }
        }

        // Check keyword
        if (!TextUtils.isEmpty(keyword)) {
            CharSequence text = message.messageText != null ? message.messageText : message.caption;
            if (text == null && message.messageOwner != null && message.messageOwner.message != null) {
                text = message.messageOwner.message;
            }
            if (text == null) {
                return false;
            }
            String content = text.toString().toLowerCase();
            String query = keyword.trim().toLowerCase();
            if (!content.contains(query)) {
                return false;
            }
        }

        return true;
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name != null ? name : "");
            obj.put("enabled", enabled);
            obj.put("dialogId", dialogId);
            obj.put("keyword", keyword != null ? keyword : "");
            obj.put("mediaType", mediaType);
            obj.put("saveToSaved", saveToSaved);
            obj.put("markAsRead", markAsRead);
            obj.put("saveToGallery", saveToGallery);
            obj.put("forwardToChatId", forwardToChatId);
            obj.put("priorityNotify", priorityNotify);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return obj;
    }

    public static HundredGramAutomationRule fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        HundredGramAutomationRule rule = new HundredGramAutomationRule();
        try {
            rule.id = obj.optLong("id", System.currentTimeMillis());
            rule.name = obj.optString("name", "");
            rule.enabled = obj.optBoolean("enabled", true);
            rule.dialogId = obj.optLong("dialogId", 0);
            rule.keyword = obj.optString("keyword", "");
            rule.mediaType = obj.optInt("mediaType", MEDIA_ANY);
            rule.saveToSaved = obj.optBoolean("saveToSaved", false);
            rule.markAsRead = obj.optBoolean("markAsRead", false);
            rule.saveToGallery = obj.optBoolean("saveToGallery", false);
            rule.forwardToChatId = obj.optLong("forwardToChatId", 0);
            rule.priorityNotify = obj.optBoolean("priorityNotify", false);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return rule;
    }
}
