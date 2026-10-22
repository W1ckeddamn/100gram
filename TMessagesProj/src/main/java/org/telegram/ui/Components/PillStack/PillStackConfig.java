package org.telegram.ui.Components.PillStack;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PillStackConfig {

    private static final String PREFS_NAME = "pillstackconfig";
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    private static final HashSet<Integer> pendingUpdates = new HashSet<>();

    private static List<Integer> activePills = new ArrayList<>();
    private static List<Integer> hiddenPills = new ArrayList<>();
    private static boolean infiniteScrolling = true;
    private static int lastActivePillId = PillType.TYPE_PROXY;

    private static boolean useCurrentLocation = true;
    private static String customWeatherLocation = null;
    private static String customWeatherAddress = null;

    private static String gramTargetCurrency = "AUTO";
    private static String btcTargetCurrency = "AUTO";
    private static String usdTargetCurrency = "AUTO";

    static {
        loadConfig();
    }

    public static SharedPreferences getPreferences() {
        if (preferences == null && ApplicationLoader.applicationContext != null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            editor = preferences.edit();
        }
        return preferences;
    }

    public static SharedPreferences.Editor getEditor() {
        if (editor == null) {
            getPreferences();
        }
        return editor;
    }

    public static synchronized void loadConfig() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            initDefaultPills();
            return;
        }

        infiniteScrolling = prefs.getBoolean("infiniteScrolling", true);
        lastActivePillId = prefs.getInt("lastActivePillId", PillType.TYPE_PROXY);

        useCurrentLocation = prefs.getBoolean("useCurrentLocation", true);
        customWeatherLocation = prefs.getString("customWeatherLocation", null);
        customWeatherAddress = prefs.getString("customWeatherAddress", null);

        gramTargetCurrency = prefs.getString("gramTargetCurrency", "AUTO");
        btcTargetCurrency = prefs.getString("btcTargetCurrency", "AUTO");
        usdTargetCurrency = prefs.getString("usdTargetCurrency", "AUTO");

        String activeStr = prefs.getString("activePills", "1,2,3,4");
        activePills = parsePills(activeStr);

        String hiddenStr = prefs.getString("hiddenPills", "5,6");
        hiddenPills = parsePills(hiddenStr);

        if (activePills.isEmpty() && hiddenPills.isEmpty()) {
            initDefaultPills();
        }
    }

    private static void initDefaultPills() {
        activePills.clear();
        activePills.add(PillType.TYPE_PROXY);
        activePills.add(PillType.TYPE_CACHE);
        activePills.add(PillType.TYPE_WEATHER);
        activePills.add(PillType.TYPE_GRAM);

        hiddenPills.clear();
        hiddenPills.add(PillType.TYPE_BTC);
        hiddenPills.add(PillType.TYPE_USD);
    }

    private static List<Integer> parsePills(String str) {
        List<Integer> list = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) {
            return list;
        }
        String[] parts = str.split(",");
        for (String p : parts) {
            try {
                list.add(Integer.parseInt(p.trim()));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private static String serializePills(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    public static synchronized List<Integer> getActivePills() {
        if (activePills.isEmpty() && hiddenPills.isEmpty()) {
            initDefaultPills();
        }
        return new ArrayList<>(activePills);
    }

    public static synchronized List<Integer> getHiddenPills() {
        return new ArrayList<>(hiddenPills);
    }

    public static synchronized void savePillsLayout(List<Integer> active, List<Integer> hidden) {
        if (active != null) {
            activePills = new ArrayList<>(active);
        }
        if (hidden != null) {
            hiddenPills = new ArrayList<>(hidden);
        }
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("activePills", serializePills(activePills))
                    .putString("hiddenPills", serializePills(hiddenPills))
                    .apply();
        }
        notifySettingsChanged();
    }

    public static boolean getInfiniteScrolling() {
        return infiniteScrolling;
    }

    public static void setInfiniteScrolling(boolean enabled) {
        infiniteScrolling = enabled;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putBoolean("infiniteScrolling", enabled).apply();
        }
    }

    public static int getLastActivePillId() {
        return lastActivePillId;
    }

    public static void saveLastActivePillId(int pillId) {
        lastActivePillId = pillId;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putInt("lastActivePillId", pillId).apply();
        }
    }

    public static boolean getUseCurrentLocation() {
        return useCurrentLocation;
    }

    public static void setUseCurrentLocation(boolean use) {
        useCurrentLocation = use;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putBoolean("useCurrentLocation", use).apply();
        }
        notifySettingsChanged(PillType.TYPE_WEATHER);
    }

    public static String getCustomWeatherLocation() {
        return customWeatherLocation;
    }

    public static void setCustomWeatherLocation(String loc) {
        customWeatherLocation = loc;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("customWeatherLocation", loc).apply();
        }
        notifySettingsChanged(PillType.TYPE_WEATHER);
    }

    public static String getCustomWeatherAddress() {
        return customWeatherAddress;
    }

    public static void setCustomWeatherAddress(String addr) {
        customWeatherAddress = addr;
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("customWeatherAddress", addr).apply();
        }
    }

    public static String getGramTargetCurrency() {
        return gramTargetCurrency;
    }

    public static void setGramTargetCurrency(String cur) {
        gramTargetCurrency = cur != null ? cur : "AUTO";
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("gramTargetCurrency", gramTargetCurrency).apply();
        }
        notifySettingsChanged(PillType.TYPE_GRAM);
    }

    public static String getBtcTargetCurrency() {
        return btcTargetCurrency;
    }

    public static void setBtcTargetCurrency(String cur) {
        btcTargetCurrency = cur != null ? cur : "AUTO";
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("btcTargetCurrency", btcTargetCurrency).apply();
        }
        notifySettingsChanged(PillType.TYPE_BTC);
    }

    public static String getUsdTargetCurrency() {
        return usdTargetCurrency;
    }

    public static void setUsdTargetCurrency(String cur) {
        usdTargetCurrency = cur != null ? cur : "AUTO";
        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.putString("usdTargetCurrency", usdTargetCurrency).apply();
        }
        notifySettingsChanged(PillType.TYPE_USD);
    }

    public static synchronized void resetToDefault() {
        initDefaultPills();
        infiniteScrolling = true;
        useCurrentLocation = true;
        customWeatherLocation = null;
        customWeatherAddress = null;
        gramTargetCurrency = "AUTO";
        btcTargetCurrency = "AUTO";
        usdTargetCurrency = "AUTO";

        SharedPreferences.Editor ed = getEditor();
        if (ed != null) {
            ed.clear()
                    .putString("activePills", serializePills(activePills))
                    .putString("hiddenPills", serializePills(hiddenPills))
                    .putBoolean("infiniteScrolling", true)
                    .putBoolean("useCurrentLocation", true)
                    .putString("gramTargetCurrency", "AUTO")
                    .putString("btcTargetCurrency", "AUTO")
                    .putString("usdTargetCurrency", "AUTO")
                    .apply();
        }
        notifySettingsChanged();
    }

    public static boolean checkAndClearPendingUpdate(int pillId) {
        synchronized (pendingUpdates) {
            return pendingUpdates.remove(pillId);
        }
    }

    public static void notifySettingsChanged(int... pillIds) {
        if (pillIds != null && pillIds.length > 0) {
            synchronized (pendingUpdates) {
                for (int id : pillIds) {
                    pendingUpdates.add(id);
                }
            }
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.pillStackSettingsChanged, (Object) pillIds);
    }

    public static boolean shouldUpdatePill(Object[] args, int... pillIds) {
        if (args == null || args.length == 0 || args[0] == null) {
            return true;
        }
        if (args[0] instanceof int[]) {
            int[] arr = (int[]) args[0];
            if (arr.length == 0) {
                return true;
            }
            for (int incoming : arr) {
                for (int expected : pillIds) {
                    if (incoming == expected) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }
}
