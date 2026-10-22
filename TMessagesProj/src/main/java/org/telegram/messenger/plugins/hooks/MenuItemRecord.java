package org.telegram.messenger.plugins.hooks;

import android.text.TextUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import java.util.Map;
import java.util.UUID;

public class MenuItemRecord {
    private final String pluginId;
    private final String itemId;
    private final String menuType;
    private final String text;
    private final String subtext;
    private final String iconName;
    private int iconResId;
    private final String conditionString;
    private final int priority;
    private Runnable onClick;
    private volatile boolean removed;

    public MenuItemRecord(String pluginId, String menuType, String text, String subtext, String iconName, int priority, Runnable onClick) {
        this.pluginId = pluginId;
        this.itemId = UUID.randomUUID().toString();
        this.menuType = menuType;
        this.text = text;
        this.subtext = subtext;
        this.iconName = iconName;
        this.conditionString = null;
        this.priority = priority;
        this.onClick = onClick;
        resolveIconResId();
    }

    public MenuItemRecord(String pluginId, Map<String, Object> data, Runnable onClick) {
        this.pluginId = pluginId;
        this.menuType = data.get("menu_type") != null ? String.valueOf(data.get("menu_type")) : null;
        this.text = data.get("text") != null ? String.valueOf(data.get("text")) : "";
        this.subtext = data.get("subtext") != null ? String.valueOf(data.get("subtext")) : null;
        this.iconName = data.get("icon") != null ? String.valueOf(data.get("icon")) : null;
        this.conditionString = data.get("condition") != null ? String.valueOf(data.get("condition")) : null;
        
        Object idObj = data.get("item_id");
        this.itemId = idObj != null && !TextUtils.isEmpty(String.valueOf(idObj)) ? String.valueOf(idObj) : UUID.randomUUID().toString();
        
        Object prioObj = data.get("priority");
        int prio = 0;
        if (prioObj instanceof Number) {
            prio = ((Number) prioObj).intValue();
        }
        this.priority = prio;
        this.onClick = onClick;
        resolveIconResId();
    }

    private void resolveIconResId() {
        if (!TextUtils.isEmpty(iconName) && ApplicationLoader.applicationContext != null) {
            try {
                this.iconResId = ApplicationLoader.applicationContext.getResources().getIdentifier(iconName, "drawable", ApplicationLoader.applicationContext.getPackageName());
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getMenuType() {
        return menuType;
    }

    public String getText() {
        return text;
    }

    public String getSubtext() {
        return subtext;
    }

    public String getIconName() {
        return iconName;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getPriority() {
        return priority;
    }

    public String getConditionString() {
        return conditionString;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markRemoved() {
        this.removed = true;
    }

    public void executeClick() {
        if (onClick != null) {
            onClick.run();
        }
    }

    public boolean checkCondition(Map<String, Object> contextData) {
        if (TextUtils.isEmpty(conditionString)) {
            return true;
        }
        // Basic safe condition evaluator (checks existence or equality)
        try {
            if (contextData == null) return false;
            if (conditionString.contains("==")) {
                String[] parts = conditionString.split("==");
                String key = parts[0].trim();
                String expected = parts[1].trim().replace("\"", "").replace("'", "");
                Object val = contextData.get(key);
                return val != null && String.valueOf(val).equals(expected);
            } else if (conditionString.contains("!=")) {
                String[] parts = conditionString.split("!=");
                String key = parts[0].trim();
                String expected = parts[1].trim().replace("\"", "").replace("'", "");
                Object val = contextData.get(key);
                return val == null || !String.valueOf(val).equals(expected);
            } else {
                Object val = contextData.get(conditionString.trim());
                if (val instanceof Boolean) {
                    return (Boolean) val;
                }
                return val != null;
            }
        } catch (Throwable th) {
            FileLog.e(th);
            return true;
        }
    }
}
