package org.telegram.ui.Components.PillStack;

import android.content.Context;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PillRegistry {

    public interface PillCreator {
        BasePill create(Context context, Theme.ResourcesProvider resourcesProvider);
    }

    public static final class PillInfo {
        public final int id;
        public final CharSequence name;
        public final int iconRes;
        public final int iconColorTop;
        public final int iconColorBottom;
        public final PillCreator creator;

        public PillInfo(int id, CharSequence name, int iconRes, int iconColorTop, int iconColorBottom, PillCreator creator) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.iconColorTop = iconColorTop;
            this.iconColorBottom = iconColorBottom;
            this.creator = creator;
        }
    }

    private static final Map<Integer, PillInfo> registry = new LinkedHashMap<>();

    static {
        registerDefaultPills();
    }

    public static void register(PillInfo info) {
        if (info != null) {
            registry.put(info.id, info);
        }
    }

    public static BasePill create(int type, Context context, Theme.ResourcesProvider resourcesProvider) {
        PillInfo info = registry.get(type);
        if (info != null && info.creator != null) {
            return info.creator.create(context, resourcesProvider);
        }
        return null;
    }

    public static Collection<PillInfo> getRegisteredPills() {
        return registry.values();
    }

    public static PillInfo getPillInfo(int type) {
        return registry.get(type);
    }

    public static List<Integer> getAllRegisteredPillIds() {
        return new ArrayList<>(registry.keySet());
    }

    private static void registerDefaultPills() {
        // PROXY
        register(new PillInfo(
                PillType.TYPE_PROXY,
                LocaleController.getString("Proxy", R.string.Proxy),
                R.drawable.outline_shield_plain_24,
                0xFF2A8EE8, 0xFF1475D6,
                ProxyPill::new
        ));

        // CACHE
        register(new PillInfo(
                PillType.TYPE_CACHE,
                LocaleController.getString("StorageUsage", R.string.StorageUsage),
                R.drawable.msg_filled_storageusage,
                0xFF2990E8, 0xFF1068BF,
                CachePill::new
        ));

        // WEATHER
        register(new PillInfo(
                PillType.TYPE_WEATHER,
                LocaleController.getString("WeatherPill", R.string.WeatherPill),
                R.drawable.weather_cloudy,
                0xFF3D9BE9, 0xFF1A75D0,
                WeatherPill::new
        ));

        // GRAM (TON)
        register(new PillInfo(
                PillType.TYPE_GRAM,
                "TON",
                R.drawable.settings_gram_24,
                0xFF2990E8, 0xFF1676D6,
                GramPill::new
        ));

        // BTC
        register(new PillInfo(
                PillType.TYPE_BTC,
                "BTC",
                R.drawable.pillstack_btc_settings,
                0xFFF7931A, 0xFFE07E0A,
                BtcPill::new
        ));

        // USD
        register(new PillInfo(
                PillType.TYPE_USD,
                "USD",
                R.drawable.pillstack_usd_settings,
                0xFF2E7D32, 0xFF1B5E20,
                UsdPill::new
        ));
    }
}
