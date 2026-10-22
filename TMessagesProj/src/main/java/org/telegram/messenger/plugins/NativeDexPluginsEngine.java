package org.telegram.messenger.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import dalvik.system.DexClassLoader;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.plugins.models.SettingItem;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.HundredGramPluginSettingsActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NativeDexPluginsEngine implements PluginsEngine {

    private final ConcurrentHashMap<String, IPlugin> activePluginInstances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PluginContext> pluginContexts = new ConcurrentHashMap<>();
    private File pluginsDir;

    public NativeDexPluginsEngine() {
        if (ApplicationLoader.applicationContext != null) {
            pluginsDir = new File(ApplicationLoader.getFilesDirFixed(), "plugins_dex");
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs();
            }
        }
    }

    @Override
    public boolean isPlugin(File file, MessageObject messageObject) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(PluginsConstants.NATIVE_EXT) || name.endsWith(PluginsConstants.DEX_EXT);
    }

    @Override
    public boolean isEngineAvailable() {
        return true;
    }

    @Override
    public void init(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void shutdown(Runnable callback) {
        for (IPlugin plugin : activePluginInstances.values()) {
            try {
                plugin.onDisable();
                plugin.onUnload();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        activePluginInstances.clear();
        pluginContexts.clear();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void loadPluginFromFile(String filePath, Plugin metadata, Utilities.Callback<String> callback) {
        try {
            File srcFile = new File(filePath);
            if (!srcFile.exists()) {
                if (callback != null) callback.run("File not found");
                return;
            }

            File destDir = new File(pluginsDir, metadata.getId());
            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            String dexPath = null;
            String mainClassName = null;

            if (filePath.endsWith(PluginsConstants.NATIVE_EXT) || filePath.endsWith(PluginsConstants.ZIP_EXT)) {
                // Extract zip archive safely (Zip Slip protection)
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(srcFile)))) {
                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    while ((entry = zis.getNextEntry()) != null) {
                        File destFile = new File(destDir, entry.getName());
                        String canonicalDest = destFile.getCanonicalPath();
                        if (!canonicalDest.startsWith(destDir.getCanonicalPath() + File.separator)) {
                            throw new SecurityException("Zip Slip detected: " + entry.getName());
                        }

                        if (entry.isDirectory()) {
                            destFile.mkdirs();
                        } else {
                            destFile.getParentFile().mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                            if (entry.getName().equals("plugin.json")) {
                                byte[] jsonBytes = new byte[(int) destFile.length()];
                                try (FileInputStream fis = new FileInputStream(destFile)) {
                                    fis.read(jsonBytes);
                                }
                                JSONObject json = new JSONObject(new String(jsonBytes, StandardCharsets.UTF_8));
                                mainClassName = json.optString("mainClass", null);
                                if (json.has("name")) metadata.setDescription(json.optString("description", metadata.getDescription()));
                                if (json.has("version")) metadata.setVersion(json.optString("version", metadata.getVersion()));
                                if (json.has("author")) metadata.setAuthor(json.optString("author", metadata.getAuthor()));
                            } else if (entry.getName().endsWith(".dex")) {
                                dexPath = destFile.getAbsolutePath();
                            }
                        }
                        zis.closeEntry();
                    }
                }
            } else if (filePath.endsWith(PluginsConstants.DEX_EXT)) {
                File destFile = new File(destDir, "classes.dex");
                copyFile(srcFile, destFile);
                dexPath = destFile.getAbsolutePath();
            }

            if (dexPath == null) {
                File defaultDex = new File(destDir, "classes.dex");
                if (defaultDex.exists()) {
                    dexPath = defaultDex.getAbsolutePath();
                }
            }

            if (dexPath == null) {
                if (callback != null) callback.run("No classes.dex found in plugin package");
                return;
            }

            File optDir = new File(ApplicationLoader.applicationContext.getCodeCacheDir(), "plugins_opt");
            if (!optDir.exists()) optDir.mkdirs();

            DexClassLoader classLoader = new DexClassLoader(dexPath, optDir.getAbsolutePath(), null, ApplicationLoader.applicationContext.getClassLoader());

            Class<?> pluginClass = null;
            if (mainClassName != null) {
                try {
                    pluginClass = classLoader.loadClass(mainClassName);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }

            if (pluginClass == null) {
                // Fallback: try finding class implementing IPlugin
                try {
                    pluginClass = classLoader.loadClass(metadata.getId() + ".PluginMain");
                } catch (Throwable ignored) {
                }
            }

            if (pluginClass != null && IPlugin.class.isAssignableFrom(pluginClass)) {
                IPlugin instance = (IPlugin) pluginClass.getDeclaredConstructor().newInstance();
                final PluginContext context = createPluginContext(metadata.getId());
                pluginContexts.put(metadata.getId(), context);
                instance.onLoad(context);
                activePluginInstances.put(metadata.getId(), instance);
                metadata.setEngine(PluginsConstants.ENGINE_NATIVE);
                if (callback != null) callback.run(null);
            } else {
                if (callback != null) callback.run("Plugin main class must implement IPlugin");
            }
        } catch (Throwable e) {
            FileLog.e("Failed to load native plugin", e);
            if (callback != null) callback.run(e.getMessage());
        }
    }

    private PluginContext createPluginContext(final String pluginId) {
        return new PluginContext() {
            @Override
            public String getPluginId() {
                return pluginId;
            }

            @Override
            public Context getAppContext() {
                return ApplicationLoader.applicationContext;
            }

            @Override
            public SharedPreferences getPreferences() {
                return ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE);
            }

            @Override
            public void registerMenuItem(String menuType, String title, String subtext, String iconName, int priority, Runnable onClick) {
                PluginsController.getInstance().addMenuItem(pluginId, new org.telegram.messenger.plugins.hooks.MenuItemRecord(
                        pluginId, menuType, title, subtext, iconName, priority, onClick
                ));
            }

            @Override
            public void registerAppEventHook(String eventName, boolean matchSubstring, int priority) {
                PluginsController.getInstance().addEventHook(pluginId, eventName, matchSubstring, priority);
            }
        };
    }

    private void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    @Override
    public void setPluginEnabled(String pluginId, boolean enabled, Utilities.Callback<String> callback) {
        IPlugin plugin = activePluginInstances.get(pluginId);
        if (plugin != null) {
            try {
                if (enabled) {
                    plugin.onEnable();
                } else {
                    plugin.onDisable();
                }
                if (callback != null) callback.run(null);
            } catch (Throwable th) {
                FileLog.e(th);
                if (callback != null) callback.run(th.getMessage());
            }
        } else {
            if (callback != null) callback.run(null);
        }
    }

    @Override
    public void deletePlugin(String pluginId, Utilities.Callback<String> callback) {
        IPlugin plugin = activePluginInstances.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.onDisable();
                plugin.onUnload();
            } catch (Throwable th) {
                FileLog.e(th);
            }
        }
        pluginContexts.remove(pluginId);
        File dir = new File(pluginsDir, pluginId);
        if (dir.exists()) {
            deleteRecursive(dir);
        }
        if (callback != null) callback.run(null);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    public String getPluginPath(String pluginId) {
        File dir = new File(pluginsDir, pluginId);
        return dir.exists() ? dir.getAbsolutePath() : null;
    }

    @Override
    public List<SettingItem> loadPluginSettings(String pluginId) {
        IPlugin plugin = activePluginInstances.get(pluginId);
        if (plugin != null) {
            try {
                return plugin.getSettings();
            } catch (Throwable th) {
                FileLog.e(th);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public Object getPluginSetting(String pluginId, String key, Object defaultValue) {
        PluginContext ctx = pluginContexts.get(pluginId);
        if (ctx != null) {
            SharedPreferences pref = ctx.getPreferences();
            if (defaultValue instanceof Boolean) {
                return pref.getBoolean(key, (Boolean) defaultValue);
            } else if (defaultValue instanceof Integer) {
                return pref.getInt(key, (Integer) defaultValue);
            } else if (defaultValue instanceof String) {
                return pref.getString(key, (String) defaultValue);
            }
        }
        return defaultValue;
    }

    @Override
    public void setPluginSetting(String pluginId, String key, Object value) {
        PluginContext ctx = pluginContexts.get(pluginId);
        if (ctx != null) {
            SharedPreferences.Editor editor = ctx.getPreferences().edit();
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
            editor.apply();
        }
    }

    @Override
    public void clearPluginSettings(String pluginId) {
        PluginContext ctx = pluginContexts.get(pluginId);
        if (ctx != null) {
            ctx.getPreferences().edit().clear().apply();
        }
    }

    @Override
    public Map<String, ?> getAllPluginSettings(String pluginId) {
        PluginContext ctx = pluginContexts.get(pluginId);
        if (ctx != null) {
            return ctx.getPreferences().getAll();
        }
        return Collections.emptyMap();
    }

    @Override
    public void openPluginSettings(String pluginId, BaseFragment fragment) {
        if (fragment != null) {
            fragment.presentFragment(new HundredGramPluginSettingsActivity(pluginId));
        }
    }

    @Override
    public void openPluginSetting(String pluginId, String linkAlias, BaseFragment fragment) {
        openPluginSettings(pluginId, fragment);
    }

    @Override
    public void executeOnAppEvent(String eventType) {
        // App lifecycle propagation
    }

    @Override
    public PluginsController.HookResult<TLObject> executePreRequestHook(String requestName, int account, TLObject request, String pluginId) {
        return new PluginsController.HookResult<>(PluginsConstants.Strategy.DEFAULT, request);
    }

    @Override
    public PluginsController.HookResult<PluginsHooks.PostRequestResult> executePostRequestHook(String requestName, int account, TLObject response, TLRPC.TL_error error, String pluginId) {
        return new PluginsController.HookResult<>(PluginsConstants.Strategy.DEFAULT, new PluginsHooks.PostRequestResult(response, error));
    }

    @Override
    public PluginsController.HookResult<SendMessagesHelper.SendMessageParams> executeSendMessageHook(int account, SendMessagesHelper.SendMessageParams params, String pluginId) {
        return new PluginsController.HookResult<>(PluginsConstants.Strategy.DEFAULT, params);
    }

    @Override
    public PluginsController.HookResult<TLRPC.Update> executeUpdateHook(String updateName, int account, TLRPC.Update update, String pluginId) {
        return new PluginsController.HookResult<>(PluginsConstants.Strategy.DEFAULT, update);
    }

    @Override
    public PluginsController.HookResult<TLRPC.Updates> executeUpdatesHook(String containerName, int account, TLRPC.Updates updates, String pluginId) {
        return new PluginsController.HookResult<>(PluginsConstants.Strategy.DEFAULT, updates);
    }
}
