package org.telegram.messenger.icons;

import android.content.Context;
import android.text.TextUtils;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Хранилище и распаковщик пакетов иконок (.icons) форка 100gram.
 */
public class HundredGramIconPackStorage {

    private static final String DIR_NAME = "hundredgram_iconpacks";

    public static File getPacksDirectory() {
        Context ctx = ApplicationLoader.applicationContext;
        if (ctx == null) {
            return null;
        }
        File dir = new File(ctx.getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<HundredGramIconPack> getInstalledPacks() {
        List<HundredGramIconPack> list = new ArrayList<>();
        File root = getPacksDirectory();
        if (root == null || !root.exists()) {
            return list;
        }
        File[] subdirs = root.listFiles();
        if (subdirs == null) {
            return list;
        }
        for (File dir : subdirs) {
            if (dir.isDirectory()) {
                HundredGramIconPack pack = loadPackFromDirectory(dir);
                if (pack != null) {
                    list.add(pack);
                }
            }
        }
        return list;
    }

    public static HundredGramIconPack loadPackFromDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        File manifest = new File(dir, "pack.json");
        String id = dir.getName();
        String name = id;
        String author = "100gram";
        String version = "1.0";
        Map<String, String> icons = new HashMap<>();

        if (manifest.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifest), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                if (json.has("id")) id = json.getString("id");
                if (json.has("name")) name = json.getString("name");
                if (json.has("author")) author = json.getString("author");
                if (json.has("version")) version = json.getString("version");

                if (json.has("icons")) {
                    JSONObject iconsObj = json.getJSONObject("icons");
                    Iterator<String> keys = iconsObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        icons.put(key, iconsObj.getString(key));
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        // Если манифеста с явным списком icons нет, просканируем файлы напрямую
        if (icons.isEmpty()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String fname = f.getName();
                    if (!"pack.json".equalsIgnoreCase(fname) && !f.isDirectory()) {
                        int dot = fname.lastIndexOf('.');
                        if (dot > 0) {
                            String baseName = fname.substring(0, dot);
                            icons.put(baseName, fname);
                        }
                    }
                }
            }
        }

        return new HundredGramIconPack(id, name, author, version, icons, null, dir);
    }

    public static HundredGramIconPack installPackFromZip(InputStream zipStream, String fallbackId) {
        if (zipStream == null) {
            return null;
        }
        File root = getPacksDirectory();
        if (root == null) {
            return null;
        }
        try {
            File tempDir = new File(root, "temp_" + System.currentTimeMillis());
            tempDir.mkdirs();

            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    // Защита от Zip Slip уязвимости
                    File target = new File(tempDir, name);
                    if (!target.getCanonicalPath().startsWith(tempDir.getCanonicalPath() + File.separator)) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        target.mkdirs();
                    } else {
                        File parent = target.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(target)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            HundredGramIconPack tempPack = loadPackFromDirectory(tempDir);
            String targetId = (tempPack != null && !TextUtils.isEmpty(tempPack.getId())) ? tempPack.getId() : fallbackId;
            if (TextUtils.isEmpty(targetId)) {
                targetId = "pack_" + System.currentTimeMillis();
            }

            File finalDir = safeChild(root, targetId);
            if (finalDir == null) {
                deleteRecursive(tempDir);
                return null;
            }
            if (finalDir.exists()) {
                deleteRecursive(finalDir);
            }
            tempDir.renameTo(finalDir);

            return loadPackFromDirectory(finalDir);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    public static boolean deletePack(String packId) {
        if (TextUtils.isEmpty(packId) || "default".equals(packId)) {
            return false;
        }
        File root = getPacksDirectory();
        if (root == null) {
            return false;
        }
        File packDir = safeChild(root, packId);
        if (packDir != null && packDir.exists()) {
            return deleteRecursive(packDir);
        }
        return false;
    }

    /**
     * Resolves a pack id to a directory strictly inside the packs root. Returns null for ids containing
     * path separators or "..", so a pack.json cannot point deleteRecursive() outside our own folder.
     */
    private static File safeChild(File root, String name) {
        if (root == null || TextUtils.isEmpty(name) || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return null;
        }
        try {
            File child = new File(root, name);
            if (!child.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
                return null;
            }
            return child;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return true;
        }
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }
}
