package org.telegram.messenger.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class HundredGramAiConfig {

    private static SharedPreferences preferences;

    public static final Service DEFAULT_SERVICE = new Service(
            "default_gemini",
            "Gemini",
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "gemini-2.5-flash",
            "",
            false
    );

    public static final Service DEFAULT_OPENAI = new Service(
            "default_openai",
            "OpenAI",
            "https://api.openai.com/v1/chat/completions",
            "gpt-4o-mini",
            "",
            false
    );

    public static SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_ai", Context.MODE_PRIVATE);
        }
        return preferences;
    }

    public static boolean isAiEnabled() {
        return getPreferences().getBoolean("aiEnabled", false);
    }

    public static void setAiEnabled(boolean enabled) {
        getPreferences().edit().putBoolean("aiEnabled", enabled).apply();
    }

    public static void toggleAiEnabled() {
        setAiEnabled(!isAiEnabled());
    }

    public static boolean getSaveHistory() {
        return getPreferences().getBoolean("saveHistory", true);
    }

    public static void setSaveHistory(boolean save) {
        getPreferences().edit().putBoolean("saveHistory", save).apply();
    }

    public static boolean getResponseStreaming() {
        return getPreferences().getBoolean("responseStreaming", true);
    }

    public static void setResponseStreaming(boolean streaming) {
        getPreferences().edit().putBoolean("responseStreaming", streaming).apply();
    }

    public static float getTemperature() {
        return getPreferences().getFloat("temperature", 0.7f);
    }

    public static void setTemperature(float temp) {
        getPreferences().edit().putFloat("temperature", Math.max(0.0f, Math.min(2.0f, temp))).apply();
    }

    public static boolean getReplaceTelegramEditor() {
        return getPreferences().getBoolean("replaceTelegramEditor", true);
    }

    public static void setReplaceTelegramEditor(boolean replace) {
        getPreferences().edit().putBoolean("replaceTelegramEditor", replace).apply();
    }

    public static boolean getReplaceTelegramSummaries() {
        return getPreferences().getBoolean("replaceTelegramSummaries", true);
    }

    public static void setReplaceTelegramSummaries(boolean replace) {
        getPreferences().edit().putBoolean("replaceTelegramSummaries", replace).apply();
    }

    public static boolean getInsertAsQuote() {
        return getPreferences().getBoolean("insertAsQuote", false);
    }

    public static void setInsertAsQuote(boolean quote) {
        getPreferences().edit().putBoolean("insertAsQuote", quote).apply();
    }

    public static boolean getShowResponseOnly() {
        return getPreferences().getBoolean("showResponseOnly", false);
    }

    public static void setShowResponseOnly(boolean showOnly) {
        getPreferences().edit().putBoolean("showResponseOnly", showOnly).apply();
    }

    public static String getSelectedServiceId() {
        return getPreferences().getString("selectedServiceId", DEFAULT_SERVICE.getId());
    }

    public static void setSelectedServiceId(String serviceId) {
        getPreferences().edit().putString("selectedServiceId", serviceId).apply();
    }

    public static String getSelectedRole() {
        return getPreferences().getString("selectedRole", Suggestions.ASSISTANT.getRole().getName());
    }

    public static void setSelectedRole(String roleName) {
        getPreferences().edit().putString("selectedRole", roleName).apply();
    }

    public static ArrayList<Service> getServices() {
        String jsonStr = getPreferences().getString("services", null);
        ArrayList<Service> list = new ArrayList<>();
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Service s = new Service(
                            obj.optString("id"),
                            obj.optString("name"),
                            obj.optString("url"),
                            obj.optString("model"),
                            obj.optString("key"),
                            obj.optBoolean("reasoningEnabled", false)
                    );
                    list.add(s);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (list.isEmpty()) {
            list.add(DEFAULT_SERVICE);
            list.add(DEFAULT_OPENAI);
            saveServices(list);
        }
        return list;
    }

    public static void saveServices(List<Service> services) {
        if (services == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Service s : services) {
                JSONObject obj = new JSONObject();
                obj.put("id", s.getId());
                obj.put("name", s.getName());
                obj.put("url", s.getUrl());
                obj.put("model", s.getModel());
                obj.put("key", s.getKey());
                obj.put("reasoningEnabled", s.isReasoningEnabled());
                array.put(obj);
            }
            getPreferences().edit().putString("services", array.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static Service getSelectedService() {
        String selectedId = getSelectedServiceId();
        ArrayList<Service> services = getServices();
        for (Service s : services) {
            if (s.getId().equals(selectedId)) {
                return s;
            }
        }
        return services.isEmpty() ? DEFAULT_SERVICE : services.get(0);
    }

    public static void setSelectedService(Service service) {
        if (service != null) {
            setSelectedServiceId(service.getId());
        }
    }

    public static void addOrUpdateService(Service service) {
        if (service == null) return;
        service.ensureId();
        ArrayList<Service> services = getServices();
        boolean found = false;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).getId().equals(service.getId())) {
                services.set(i, service);
                found = true;
                break;
            }
        }
        if (!found) {
            services.add(service);
        }
        saveServices(services);
    }

    public static boolean deleteService(Service service) {
        if (service == null) return false;
        ArrayList<Service> services = getServices();
        if (services.size() <= 1) return false;
        boolean removed = services.remove(service);
        if (removed) {
            saveServices(services);
            if (service.isSelected()) {
                setSelectedService(services.get(0));
            }
        }
        return removed;
    }

    public static ArrayList<Role> getRoles() {
        String jsonStr = getPreferences().getString("roles", null);
        ArrayList<Role> list = new ArrayList<>();
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Role r = new Role(obj.optString("name"), obj.optString("prompt"));
                    r.setEmojiId(obj.optLong("emojiId", 0));
                    r.setSuggestion(obj.optBoolean("isSuggestion", false));
                    list.add(r);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return list;
    }

    public static void saveRoles(List<Role> roles) {
        if (roles == null) return;
        try {
            JSONArray array = new JSONArray();
            for (Role r : roles) {
                JSONObject obj = new JSONObject();
                obj.put("name", r.getName());
                obj.put("prompt", r.getPrompt());
                obj.put("emojiId", r.getEmojiId());
                obj.put("isSuggestion", r.isSuggestion());
                array.put(obj);
            }
            getPreferences().edit().putString("roles", array.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ArrayList<Message> getConversationHistory() {
        String jsonStr = getPreferences().getString("history", null);
        ArrayList<Message> list = new ArrayList<>();
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    list.add(new Message(obj.optString("role"), obj.optString("content")));
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return list;
    }

    public static void saveConversationHistory(List<Message> history) {
        if (history == null) return;
        try {
            JSONArray array = new JSONArray();
            int start = Math.max(0, history.size() - 32);
            for (int i = start; i < history.size(); i++) {
                Message m = history.get(i);
                JSONObject obj = new JSONObject();
                obj.put("role", m.role());
                obj.put("content", m.content());
                array.put(obj);
            }
            getPreferences().edit().putString("history", array.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void addMessageToHistory(Message message) {
        if (!getSaveHistory() || message == null || TextUtils.isEmpty(message.content())) {
            return;
        }
        ArrayList<Message> history = getConversationHistory();
        history.add(message);
        saveConversationHistory(history);
    }

    public static void clearConversationHistory() {
        getPreferences().edit().remove("history").apply();
    }
}
