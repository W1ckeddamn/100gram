package org.telegram.messenger.plugins.hooks;

import android.text.TextUtils;

public class EventHookRecord implements HookRecord {
    private final String pluginId;
    private final String hookName;
    private final boolean matchSubstring;
    private final int priority;

    public EventHookRecord(String pluginId, String hookName, boolean matchSubstring, int priority) {
        this.pluginId = pluginId;
        this.hookName = hookName;
        this.matchSubstring = matchSubstring;
        this.priority = priority;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getHookName() {
        return hookName;
    }

    public boolean isMatchSubstring() {
        return matchSubstring;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean matches(Object criteria) {
        if (!(criteria instanceof String) || hookName == null) {
            return false;
        }
        String str = (String) criteria;
        if (matchSubstring) {
            return !TextUtils.isEmpty(hookName) && str.contains(hookName);
        }
        return hookName.equals(str);
    }
}
