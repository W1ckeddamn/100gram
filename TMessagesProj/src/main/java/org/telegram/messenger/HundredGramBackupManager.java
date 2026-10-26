package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Менеджер резервного копирования и восстановления настроек 100gram.
 * Экспортирует и импортирует конфигурацию в формате JSON.
 */
public class HundredGramBackupManager {

    private static final String PREFS_NAME = "hundredgram_config";

    public static File exportSettingsToFile(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            return null;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            JSONObject root = new JSONObject();
            root.put("app", "100gram");
            root.put("schema_version", 1);
            root.put("export_timestamp", System.currentTimeMillis());

            JSONObject configObj = new JSONObject();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (key != null && val != null) {
                    configObj.put(key, val);
                }
            }
            root.put("config", configObj);

            File cacheDir = context.getExternalCacheDir();
            if (cacheDir == null) {
                cacheDir = context.getCacheDir();
            }
            File backupDir = new File(cacheDir, "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File backupFile = new File(backupDir, "100gram_settings_" + dateStr + ".json");

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(backupFile), StandardCharsets.UTF_8)) {
                writer.write(root.toString(2));
            }

            return backupFile;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    public static void shareBackupFile(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        Activity activity = fragment.getParentActivity();
        File file = exportSettingsToFile(activity);
        if (file == null || !file.exists()) {
            BulletinFactory.of(fragment).createErrorBulletin("Ошибка создания файла резервной копии").show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, "Экспорт настроек 100gram"));
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(fragment).createErrorBulletin("Не удалось отправить файл бэкапа").show();
        }
    }

    public static boolean importSettingsFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return false;
        }

        try (InputStream is = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JSONObject root = new JSONObject(sb.toString());
            if (!root.has("config")) {
                return false;
            }

            JSONObject configObj = root.getJSONObject("config");
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            Iterator<String> keys = configObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = configObj.get(key);
                if (val instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) val);
                } else if (val instanceof Integer) {
                    editor.putInt(key, (Integer) val);
                } else if (val instanceof Long) {
                    editor.putLong(key, (Long) val);
                } else if (val instanceof Double || val instanceof Float) {
                    editor.putFloat(key, ((Number) val).floatValue());
                } else if (val instanceof String) {
                    editor.putString(key, (String) val);
                } else if (val instanceof JSONArray) {
                    // string sets (e.g. pinnedPlugins) are exported as arrays
                    JSONArray arr = (JSONArray) val;
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        Object item = arr.opt(i);
                        if (item != null) {
                            set.add(item.toString());
                        }
                    }
                    editor.putStringSet(key, set);
                }
            }
            editor.apply();

            // Перезагружаем конфигурацию в память
            HundredGramConfig.loadConfig();

            // Уведомляем интерфейс об изменениях
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didUpdateIconPack);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pillStackSettingsChanged);
            HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);

            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void resetAllSettings(Context context) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        prefs.edit().clear().apply();
        HundredGramConfig.loadConfig();
        HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
    }
}
