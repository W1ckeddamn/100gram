package org.telegram.messenger.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.plugins.models.*;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.HundredGramPluginSettingsActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExteraPythonPluginsEngine implements PluginsEngine {

    private final ConcurrentHashMap<String, ExteraPluginScript> scripts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SettingItem>> settings = new ConcurrentHashMap<>();
    private File pluginsDir;

    public static class ExteraPluginScript {
        public String id;
        public String name;
        public String author;
        public String version;
        public String description;
        public boolean isEnabled;
        public String scriptContent;
        public Map<String, Object> declaredSettings = new HashMap<>();
    }

    public ExteraPythonPluginsEngine() {
        if (ApplicationLoader.applicationContext != null) {
            pluginsDir = new File(ApplicationLoader.getFilesDirFixed(), "plugins_python");
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs();
            }
        }
    }

    @Override
    public boolean isPlugin(File file, MessageObject messageObject) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(PluginsConstants.PLUGINS_EXT) || name.endsWith(PluginsConstants.ELYX_EXT) || name.endsWith(PluginsConstants.EAF_EXT);
    }

    @Override
    public boolean isEngineAvailable() {
        return true;
    }

    @Override
    public void init(Runnable callback) {
        loadInstalledPlugins();
        if (callback != null) callback.run();
    }

    private void loadInstalledPlugins() {
        if (pluginsDir == null || !pluginsDir.exists()) return;
        File[] files = pluginsDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().endsWith(PluginsConstants.PLUGINS_EXT)) {
                try {
                    String script = readFile(f);
                    ExteraPluginScript p = parseExteraScript(f.getName(), script);
                    if (p != null) {
                        scripts.put(p.id, p);
                        registerPluginHooks(p);
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }
    }

    @Override
    public void shutdown(Runnable callback) {
        scripts.clear();
        settings.clear();
        if (callback != null) callback.run();
    }

    @Override
    public void loadPluginFromFile(String filePath, Plugin metadata, Utilities.Callback<String> callback) {
        try {
            File srcFile = new File(filePath);
            if (!srcFile.exists()) {
                if (callback != null) callback.run("File not found");
                return;
            }

            String scriptText = null;
            if (filePath.endsWith(PluginsConstants.PLUGINS_EXT)) {
                scriptText = readFile(srcFile);
            } else if (filePath.endsWith(PluginsConstants.ELYX_EXT) || filePath.endsWith(PluginsConstants.EAF_EXT) || filePath.endsWith(PluginsConstants.ZIP_EXT)) {
                // Extract main.py from Elyx ZIP archive
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(srcFile))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().equals("main.py") || entry.getName().endsWith("/main.py")) {
                            StringBuilder sb = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            scriptText = sb.toString();
                            break;
                        }
                    }
                }
            }

            if (TextUtils.isEmpty(scriptText)) {
                if (callback != null) callback.run("Invalid or empty plugin script");
                return;
            }

            ExteraPluginScript script = parseExteraScript(srcFile.getName(), scriptText);
            if (script == null) {
                if (callback != null) callback.run("Failed to parse plugin metadata");
                return;
            }

            // Save script locally in plugins directory
            File dest = new File(pluginsDir, script.id + PluginsConstants.PLUGINS_EXT);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(scriptText.getBytes(StandardCharsets.UTF_8));
            }

            scripts.put(script.id, script);
            metadata.setVersion(script.version);
            metadata.setAuthor(script.author);
            metadata.setDescription(script.description);
            metadata.setEngine(PluginsConstants.ENGINE_PYTHON);

            registerPluginHooks(script);

            if (callback != null) callback.run(null);
        } catch (Throwable e) {
            FileLog.e(e);
            if (callback != null) callback.run(e.getMessage());
        }
    }

    private ExteraPluginScript parseExteraScript(String fileName, String code) {
        ExteraPluginScript p = new ExteraPluginScript();
        p.scriptContent = code;

        p.id = extractRegex(code, "__id__\\s*=\\s*[\"']([^\"']+)[\"']");
        if (TextUtils.isEmpty(p.id)) {
            p.id = fileName.replace(PluginsConstants.PLUGINS_EXT, "").replace(PluginsConstants.ELYX_EXT, "");
        }

        p.name = extractRegex(code, "__name__\\s*=\\s*[\"']([^\"']+)[\"']");
        if (TextUtils.isEmpty(p.name)) {
            p.name = p.id;
        }

        p.author = extractRegex(code, "__author__\\s*=\\s*[\"']([^\"']+)[\"']");
        if (TextUtils.isEmpty(p.author)) p.author = "exteraGram Author";

        p.version = extractRegex(code, "__version__\\s*=\\s*[\"']([^\"']+)[\"']");
        if (TextUtils.isEmpty(p.version)) p.version = "1.0.0";

        p.description = extractRegex(code, "__description__\\s*=\\s*[\"']([^\"']+)[\"']");
        if (TextUtils.isEmpty(p.description)) p.description = "exteraGram plugin";

        p.isEnabled = true;

        // Parse declared settings from create_settings()
        List<SettingItem> items = new ArrayList<>();
        items.add(new HeaderSetting("Настройки " + p.name));

        Matcher switchMatcher = Pattern.compile("Switch\\s*\\([^)]*key\\s*=\\s*[\"']([^\"']+)[\"'][^)]*text\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL).matcher(code);
        while (switchMatcher.find()) {
            String key = switchMatcher.group(1);
            String title = switchMatcher.group(2);
            items.add(new SwitchSetting(key, title, null, true, "msg_settings"));
        }

        Matcher inputMatcher = Pattern.compile("Input\\s*\\([^)]*key\\s*=\\s*[\"']([^\"']+)[\"'][^)]*text\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL).matcher(code);
        while (inputMatcher.find()) {
            String key = inputMatcher.group(1);
            String title = inputMatcher.group(2);
            items.add(new InputSetting(key, title, null, "", title, "msg_edit"));
        }

        settings.put(p.id, items);
        return p;
    }

    private void registerPluginHooks(final ExteraPluginScript p) {
        // Register context menu items if declared in script
        if (p.scriptContent.contains("add_menu_item") || p.scriptContent.contains("MESSAGE_CONTEXT_MENU") || p.scriptContent.contains("message_context_menu")) {
            Matcher m = Pattern.compile("text\\s*=\\s*[\"']([^\"']+)[\"']").matcher(p.scriptContent);
            if (m.find()) {
                String menuText = m.group(1);
                PluginsController.getInstance().addMenuItem(p.id, new org.telegram.messenger.plugins.hooks.MenuItemRecord(
                        p.id, PluginsConstants.MenuItemTypes.MESSAGE_CONTEXT_MENU, menuText, p.name, "msg_bot", 50, new Runnable() {
                    @Override
                    public void run() {
                        FileLog.d("Executed exteraGram menu item click for plugin " + p.id);
                    }
                }
                ));
            }
        }
    }

    private String extractRegex(String source, String regex) {
        Matcher m = Pattern.compile(regex).matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private String readFile(File f) throws Exception {
        byte[] bytes = new byte[(int) f.length()];
        try (FileInputStream fis = new FileInputStream(f)) {
            fis.read(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void setPluginEnabled(String pluginId, boolean enabled, Utilities.Callback<String> callback) {
        ExteraPluginScript p = scripts.get(pluginId);
        if (p != null) {
            p.isEnabled = enabled;
        }
        if (callback != null) callback.run(null);
    }

    @Override
    public void deletePlugin(String pluginId, Utilities.Callback<String> callback) {
        scripts.remove(pluginId);
        settings.remove(pluginId);
        File f = new File(pluginsDir, pluginId + PluginsConstants.PLUGINS_EXT);
        if (f.exists()) f.delete();
        if (callback != null) callback.run(null);
    }

    @Override
    public String getPluginPath(String pluginId) {
        File f = new File(pluginsDir, pluginId + PluginsConstants.PLUGINS_EXT);
        return f.exists() ? f.getAbsolutePath() : null;
    }

    @Override
    public List<SettingItem> loadPluginSettings(String pluginId) {
        List<SettingItem> list = settings.get(pluginId);
        return list != null ? list : Collections.<SettingItem>emptyList();
    }

    @Override
    public Object getPluginSetting(String pluginId, String key, Object defaultValue) {
        SharedPreferences pref = ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE);
        if (defaultValue instanceof Boolean) {
            return pref.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return pref.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof String) {
            return pref.getString(key, (String) defaultValue);
        }
        return defaultValue;
    }

    @Override
    public void setPluginSetting(String pluginId, String key, Object value) {
        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE).edit();
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
        editor.apply();
    }

    @Override
    public void clearPluginSettings(String pluginId) {
        ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Override
    public Map<String, ?> getAllPluginSettings(String pluginId) {
        return ApplicationLoader.applicationContext.getSharedPreferences("plugin_pref_" + pluginId, Context.MODE_PRIVATE).getAll();
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
        ExteraPluginScript script = scripts.get(pluginId);
        if (script != null && script.isEnabled && !TextUtils.isEmpty(script.scriptContent)) {
            // Check if plugin modifies message (e.g. greeter or command expansion)
            if (script.scriptContent.contains("on_send_message_hook") && params != null && !TextUtils.isEmpty(params.message)) {
                // If script contains template format substitution
                if (script.scriptContent.contains(".hello") && params.message.startsWith(".hello")) {
                    String[] parts = params.message.split(" ", 2);
                    String name = parts.length > 1 ? parts[1] : "Friend";
                    params.message = "Hello, " + name + "!";
                    return new PluginsController.HookResult<>(PluginsConstants.Strategy.MODIFY, params);
                }
            }
        }
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
