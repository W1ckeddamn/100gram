package org.telegram.messenger.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

public interface PluginContext {
    String getPluginId();
    Context getAppContext();
    SharedPreferences getPreferences();
    void registerMenuItem(String menuType, String title, String subtext, String iconName, int priority, Runnable onClick);
    void registerAppEventHook(String eventName, boolean matchSubstring, int priority);
}
