package org.telegram.messenger.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import org.telegram.messenger.plugins.models.SettingItem;

public interface IPlugin {
    void onLoad(PluginContext context);
    void onEnable();
    void onDisable();
    void onUnload();
    List<SettingItem> getSettings();
}
