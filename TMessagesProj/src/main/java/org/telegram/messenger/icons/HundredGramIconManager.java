package org.telegram.messenger.icons;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.SparseArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.NotificationCenter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Главный менеджер кастомных наборов иконок форка 100gram.
 * Обеспечивает кеширование, разрешение Drawable по ID ресурса и переключение активного пака.
 */
public class HundredGramIconManager {

    private static volatile HundredGramIconManager Instance;

    public static HundredGramIconManager getInstance() {
        HundredGramIconManager local = Instance;
        if (local == null) {
            synchronized (HundredGramIconManager.class) {
                local = Instance;
                if (local == null) {
                    Instance = local = new HundredGramIconManager();
                }
            }
        }
        return local;
    }

    private final LruCache<String, Drawable> memoryCache;
    private final ConcurrentHashMap<Integer, String> resIdToNameMap = new ConcurrentHashMap<>();
    private HundredGramIconPack activePack;
    private final List<HundredGramIconPack> installedPacks = new ArrayList<>();

    private HundredGramIconManager() {
        // Кеш на 150 Drawable
        memoryCache = new LruCache<String, Drawable>(150) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                return 1;
            }
        };
        reloadInstalledPacks();
    }

    public synchronized void reloadInstalledPacks() {
        installedPacks.clear();
        // Пак по умолчанию (системные ресурсы Telegram)
        installedPacks.add(new HundredGramIconPack("default", "По умолчанию (Telegram)", "Telegram", "1.0", null, null, null));
        // Загружаем пользовательские паки из хранилища
        List<HundredGramIconPack> onDisk = HundredGramIconPackStorage.getInstalledPacks();
        if (onDisk != null) {
            installedPacks.addAll(onDisk);
        }

        String selectedId = HundredGramConfig.selectedIconPack;
        activePack = findPackById(selectedId);
        if (activePack == null) {
            activePack = installedPacks.get(0);
        }
        clearCache();
    }

    public List<HundredGramIconPack> getInstalledPacks() {
        return new ArrayList<>(installedPacks);
    }

    public HundredGramIconPack getActivePack() {
        return activePack;
    }

    public HundredGramIconPack findPackById(String id) {
        if (TextUtils.isEmpty(id)) {
            return null;
        }
        for (HundredGramIconPack p : installedPacks) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    public synchronized void setActivePack(String id) {
        HundredGramConfig.setSelectedIconPack(id);
        activePack = findPackById(id);
        if (activePack == null && !installedPacks.isEmpty()) {
            activePack = installedPacks.get(0);
        }
        clearCache();
    }

    public void clearCache() {
        memoryCache.evictAll();
    }

    /**
     * Возвращает замененный Drawable для переданного resId или null, если замена не найдена.
     */
    public Drawable getDrawable(int resId, int density, Resources.Theme theme, Resources originalRes) {
        if (!HundredGramConfig.iconPacksEnabled || activePack == null || activePack.isDefault()) {
            return null;
        }
        if (originalRes == null) {
            return null;
        }

        String resName = resIdToNameMap.get(resId);
        if (resName == null) {
            try {
                resName = originalRes.getResourceEntryName(resId);
                resIdToNameMap.put(resId, resName);
            } catch (Exception e) {
                return null;
            }
        }

        String cacheKey = activePack.getId() + "_" + resName + "_" + density;
        Drawable cached = memoryCache.get(cacheKey);
        if (cached != null) {
            return cached.getConstantState() != null ? cached.getConstantState().newDrawable(originalRes) : cached;
        }

        Map<String, String> icons = activePack.getIcons();
        if (icons == null || !icons.containsKey(resName)) {
            return null;
        }

        String fileName = icons.get(resName);
        File packDir = activePack.getLocation();
        if (packDir == null || !packDir.exists()) {
            return null;
        }

        File iconFile = new File(packDir, fileName);
        if (!iconFile.exists()) {
            return null;
        }

        try {
            // Декодируем растровое изображение
            Bitmap bmp = BitmapFactory.decodeFile(iconFile.getAbsolutePath());
            if (bmp != null) {
                BitmapDrawable bd = new BitmapDrawable(originalRes, bmp);
                memoryCache.put(cacheKey, bd);
                return bd;
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        return null;
    }
}
