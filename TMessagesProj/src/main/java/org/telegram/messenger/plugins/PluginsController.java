package org.telegram.messenger.plugins;

import android.content.SharedPreferences;
import android.text.TextUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.plugins.hooks.EventHookRecord;
import org.telegram.messenger.plugins.hooks.HookRecord;
import org.telegram.messenger.plugins.hooks.MenuItemRecord;
import org.telegram.messenger.plugins.models.SettingItem;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginsController implements PluginsHooks {

    private static volatile PluginsController instance;

    public static PluginsController getInstance() {
        if (instance == null) {
            synchronized (PluginsController.class) {
                if (instance == null) {
                    instance = new PluginsController();
                }
            }
        }
        return instance;
    }

    public static class HookResult<T> {
        public final String strategy;
        public final T value;

        public HookResult(String strategy, T value) {
            this.strategy = strategy;
            this.value = value;
        }
    }

    private final ConcurrentHashMap<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PluginsEngine> engines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MenuItemRecord> menuItemsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<MenuItemRecord>> menuItemsByMenuType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<HookRecord>> hooks = new ConcurrentHashMap<>();
    private File pluginsDir;

    private PluginsController() {
        if (ApplicationLoader.applicationContext != null) {
            pluginsDir = new File(ApplicationLoader.getFilesDirFixed(), PluginsConstants.PLUGINS);
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs();
            }
        }
        engines.put(PluginsConstants.ENGINE_NATIVE, new NativeDexPluginsEngine());
        engines.put(PluginsConstants.ENGINE_PYTHON, new ExteraPythonPluginsEngine());
    }

    public ConcurrentHashMap<String, Plugin> getPlugins() {
        return plugins;
    }

    public ConcurrentHashMap<String, PluginsEngine> getEngines() {
        return engines;
    }

    public void init() {
        if (!HundredGramConfig.pluginsEngine) {
            return;
        }
        for (PluginsEngine engine : engines.values()) {
            try {
                engine.init(null);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        loadSavedPlugins();
    }

    private void loadSavedPlugins() {
        if (pluginsDir == null || !pluginsDir.exists()) return;
        File[] files = pluginsDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            PluginsEngine engine = getPluginEngine(f);
            if (engine != null) {
                String id = f.getName().substring(0, f.getName().lastIndexOf('.'));
                Plugin p = new Plugin(id, id);
                p.setEngine(engine instanceof NativeDexPluginsEngine ? PluginsConstants.ENGINE_NATIVE : PluginsConstants.ENGINE_PYTHON);
                plugins.put(id, p);
            }
        }
    }

    public PluginsEngine getPluginEngine(File file) {
        if (file == null) return null;
        for (PluginsEngine engine : engines.values()) {
            if (engine.isPlugin(file, null)) {
                return engine;
            }
        }
        return null;
    }

    public PluginsEngine getPluginEngine(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null && plugin.getEngine() != null) {
            return engines.get(plugin.getEngine());
        }
        return engines.get(PluginsConstants.ENGINE_NATIVE);
    }

    public boolean isPlugin(File file, MessageObject messageObject) {
        return getPluginEngine(file) != null;
    }

    public void installPlugin(final String filePath, final BaseFragment fragment, final Utilities.Callback<Boolean> callback) {
        // A plugin is native code with full app access: never install while plugins are off,
        // safe mode is on, or the user disallowed unknown sources.
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode || !HundredGramConfig.pluginsInstallUnknownSources) {
            if (callback != null) callback.run(false);
            return;
        }
        final File file = new File(filePath);
        final PluginsEngine engine = getPluginEngine(file);
        if (engine == null) {
            if (callback != null) callback.run(false);
            return;
        }

        String fileName = file.getName();
        int dot = fileName.lastIndexOf('.');
        String id = dot > 0 ? fileName.substring(0, dot) : fileName;

        final Plugin plugin = new Plugin(id, id);
        plugin.setEngine(engine instanceof NativeDexPluginsEngine ? PluginsConstants.ENGINE_NATIVE : PluginsConstants.ENGINE_PYTHON);

        engine.loadPluginFromFile(filePath, plugin, new Utilities.Callback<String>() {
            @Override
            public void run(String error) {
                if (error == null) {
                    plugins.put(plugin.getId(), plugin);
                    notifyPluginsChanged();
                    if (callback != null) callback.run(true);
                } else {
                    FileLog.e("Failed to install plugin: " + error);
                    if (callback != null) callback.run(false);
                }
            }
        });
    }

    public void setPluginEnabled(String pluginId, boolean enabled, Utilities.Callback<String> callback) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.setEnabled(enabled);
            PluginsEngine engine = getPluginEngine(pluginId);
            if (engine != null) {
                engine.setPluginEnabled(pluginId, enabled, callback);
            }
            notifyPluginsChanged();
        } else {
            if (callback != null) callback.run("Plugin not found");
        }
    }

    public void deletePlugin(String pluginId, Utilities.Callback<String> callback) {
        // Resolve the engine while the plugin is still registered, otherwise it falls back to
        // the native engine and a .plugin file is never removed from disk.
        PluginsEngine engine = getPluginEngine(pluginId);
        plugins.remove(pluginId);
        removeMenuItemsByPluginId(pluginId);
        removeHooksByPluginId(pluginId);
        if (engine != null) {
            engine.deletePlugin(pluginId, callback);
        } else {
            if (callback != null) callback.run(null);
        }
        notifyPluginsChanged();
    }

    public List<SettingItem> getPluginSettingsList(String pluginId) {
        PluginsEngine engine = getPluginEngine(pluginId);
        return engine != null ? engine.loadPluginSettings(pluginId) : Collections.<SettingItem>emptyList();
    }

    public boolean hasPluginSettings(String pluginId) {
        List<SettingItem> items = getPluginSettingsList(pluginId);
        return items != null && !items.isEmpty();
    }

    public Object getPluginSetting(String pluginId, String key, Object defaultValue) {
        PluginsEngine engine = getPluginEngine(pluginId);
        return engine != null ? engine.getPluginSetting(pluginId, key, defaultValue) : defaultValue;
    }

    public void setPluginSetting(String pluginId, String key, Object value) {
        PluginsEngine engine = getPluginEngine(pluginId);
        if (engine != null) {
            engine.setPluginSetting(pluginId, key, value);
        }
    }

    public void addMenuItem(String pluginId, MenuItemRecord item) {
        if (item == null || item.getMenuType() == null) return;
        menuItemsById.put(item.getItemId(), item);
        CopyOnWriteArrayList<MenuItemRecord> list = menuItemsByMenuType.get(item.getMenuType());
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            menuItemsByMenuType.put(item.getMenuType(), list);
        }
        list.add(item);
        // Sort by priority descending
        ArrayList<MenuItemRecord> sorted = new ArrayList<>(list);
        Collections.sort(sorted, new Comparator<MenuItemRecord>() {
            @Override
            public int compare(MenuItemRecord o1, MenuItemRecord o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority());
            }
        });
        menuItemsByMenuType.put(item.getMenuType(), new CopyOnWriteArrayList<>(sorted));
        notifyMenuItemsUpdated();
    }

    public void removeMenuItemsByPluginId(String pluginId) {
        for (Map.Entry<String, MenuItemRecord> entry : menuItemsById.entrySet()) {
            if (entry.getValue().getPluginId().equals(pluginId)) {
                menuItemsById.remove(entry.getKey());
                CopyOnWriteArrayList<MenuItemRecord> list = menuItemsByMenuType.get(entry.getValue().getMenuType());
                if (list != null) {
                    list.remove(entry.getValue());
                }
            }
        }
        notifyMenuItemsUpdated();
    }

    public List<MenuItemRecord> getMenuItemsForLocation(String menuType, Map<String, Object> contextData) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode) {
            return Collections.emptyList();
        }
        CopyOnWriteArrayList<MenuItemRecord> list = menuItemsByMenuType.get(menuType);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<MenuItemRecord> result = new ArrayList<>();
        for (MenuItemRecord record : list) {
            Plugin p = plugins.get(record.getPluginId());
            if (p != null && p.isEnabled() && record.checkCondition(contextData)) {
                result.add(record);
            }
        }
        return result;
    }

    public void addEventHook(String pluginId, String hookName, boolean matchSubstring, int priority) {
        Set<HookRecord> set = hooks.get(pluginId);
        if (set == null) {
            set = ConcurrentHashMap.newKeySet();
            hooks.put(pluginId, set);
        }
        set.add(new EventHookRecord(pluginId, hookName, matchSubstring, priority));
    }

    public void removeHooksByPluginId(String pluginId) {
        hooks.remove(pluginId);
    }

    public void notifyPluginsChanged() {
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pluginsUpdated);
    }

    public void notifyMenuItemsUpdated() {
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pluginMenuItemsUpdated);
    }

    @Override
    public PostRequestResult executePostRequestHook(String requestName, int account, TLObject response, TLRPC.TL_error error) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode) {
            return new PostRequestResult(response, error);
        }
        PostRequestResult current = new PostRequestResult(response, error);
        for (Plugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                PluginsEngine engine = getPluginEngine(plugin.getId());
                if (engine != null) {
                    try {
                        HookResult<PostRequestResult> res = engine.executePostRequestHook(requestName, account, current.getResponse(), current.getError(), plugin.getId());
                        if (res != null && res.value != null) {
                            current = res.value;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
        }
        return current;
    }

    @Override
    public TLObject executePreRequestHook(String requestName, int account, TLObject request) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode) {
            return request;
        }
        TLObject current = request;
        for (Plugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                PluginsEngine engine = getPluginEngine(plugin.getId());
                if (engine != null) {
                    try {
                        HookResult<TLObject> res = engine.executePreRequestHook(requestName, account, current, plugin.getId());
                        if (res != null && res.value != null) {
                            current = res.value;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
        }
        return current;
    }

    @Override
    public SendMessagesHelper.SendMessageParams executeSendMessageHook(int account, SendMessagesHelper.SendMessageParams params) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode || params == null) {
            return params;
        }
        SendMessagesHelper.SendMessageParams current = params;
        for (Plugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                PluginsEngine engine = getPluginEngine(plugin.getId());
                if (engine != null) {
                    try {
                        HookResult<SendMessagesHelper.SendMessageParams> res = engine.executeSendMessageHook(account, current, plugin.getId());
                        if (res != null && res.value != null) {
                            current = res.value;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
        }
        return current;
    }

    @Override
    public TLRPC.Update executeUpdateHook(String updateName, int account, TLRPC.Update update) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode) {
            return update;
        }
        TLRPC.Update current = update;
        for (Plugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                PluginsEngine engine = getPluginEngine(plugin.getId());
                if (engine != null) {
                    try {
                        HookResult<TLRPC.Update> res = engine.executeUpdateHook(updateName, account, current, plugin.getId());
                        if (res != null && res.value != null) {
                            current = res.value;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
        }
        return current;
    }

    @Override
    public TLRPC.Updates executeUpdatesHook(String containerName, int account, TLRPC.Updates updates) {
        if (!HundredGramConfig.pluginsEngine || HundredGramConfig.pluginsSafeMode) {
            return updates;
        }
        TLRPC.Updates current = updates;
        for (Plugin plugin : plugins.values()) {
            if (plugin.isEnabled()) {
                PluginsEngine engine = getPluginEngine(plugin.getId());
                if (engine != null) {
                    try {
                        HookResult<TLRPC.Updates> res = engine.executeUpdatesHook(containerName, account, current, plugin.getId());
                        if (res != null && res.value != null) {
                            current = res.value;
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }
        }
        return current;
    }
}
