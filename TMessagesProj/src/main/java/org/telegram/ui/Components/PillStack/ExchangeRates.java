package org.telegram.ui.Components.PillStack;

import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public abstract class ExchangeRates {

    private static volatile State cacheValue;
    private static volatile long cacheTime;
    private static boolean remoteInFlight;
    private static final long CACHE_DURATION = 300_000L; // 5 minutes

    public static final String[] MAIN_CURRENCIES = {
            "USD", "EUR", "RUB", "GBP", "KZT", "TRY", "UAH", "PLN", "AED", "CNY", "JPY", "BYN", "ILS", "CZK", "INR", "TON", "BTC", "ETH", "SOL"
    };

    private static final ArrayList<Utilities.Callback<State>> pendingCallbacks = new ArrayList<>();

    static {
        NotificationCenter.getGlobalInstance().addObserver((id, account, args) -> {
            if (id == NotificationCenter.pillStackSettingsChanged && PillStackConfig.shouldUpdatePill(args, PillType.TYPE_GRAM, PillType.TYPE_BTC, PillType.TYPE_USD)) {
                clearCache();
            }
        }, NotificationCenter.pillStackSettingsChanged);
    }

    public static final class State {
        private final Map<String, BigDecimal> usdRates;

        public State(Map<String, BigDecimal> usdRates) {
            this.usdRates = usdRates != null ? usdRates : new HashMap<>();
        }

        public Map<String, BigDecimal> getUsdRates() {
            return usdRates;
        }

        public BigDecimal getUsdRate(String currency) {
            if (currency == null) {
                return null;
            }
            return usdRates.get(ExchangeRates.normalize(currency));
        }

        public BigDecimal getRate(String baseCurrency, String targetCurrency) {
            BigDecimal usdRateBase = getUsdRate(baseCurrency);
            BigDecimal usdRateTarget = getUsdRate(targetCurrency);
            if (usdRateBase == null || usdRateTarget == null || usdRateTarget.signum() == 0) {
                return null;
            }
            return usdRateBase.divide(usdRateTarget, 12, RoundingMode.HALF_UP);
        }

        public String toJson() {
            try {
                JSONObject obj = new JSONObject();
                for (Map.Entry<String, BigDecimal> entry : usdRates.entrySet()) {
                    obj.put(entry.getKey(), entry.getValue().toPlainString());
                }
                return obj.toString();
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }

        public static State fromJson(String json) {
            if (json == null || json.isEmpty()) {
                return null;
            }
            try {
                JSONObject obj = new JSONObject(json);
                Map<String, BigDecimal> map = new HashMap<>();
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    map.put(k, new BigDecimal(obj.getString(k)));
                }
                return new State(map);
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }
    }

    public static State getCached() {
        if (cacheValue == null) {
            try {
                String cachedJson = PillStackConfig.getPreferences() != null ? PillStackConfig.getPreferences().getString("exchangeRatesCache", null) : null;
                if (cachedJson != null) {
                    cacheValue = State.fromJson(cachedJson);
                    cacheTime = PillStackConfig.getPreferences().getLong("exchangeRatesTimestamp", 0L);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return cacheValue;
    }

    public static void clearCache() {
        cacheTime = 0L;
        cacheValue = null;
        if (PillStackConfig.getEditor() != null) {
            PillStackConfig.getEditor().putLong("exchangeRatesTimestamp", 0L).apply();
        }
    }

    public static boolean isSupportedCurrency(String str) {
        if (str == null) {
            return false;
        }
        String norm = normalize(str);
        for (String c : MAIN_CURRENCIES) {
            if (c.equals(norm)) {
                return true;
            }
        }
        return false;
    }

    public static String resolveTargetCurrency(int account, String targetSelection) {
        String norm = normalize(targetSelection);
        if (!"AUTO".equals(norm)) {
            return (TextUtils.isEmpty(norm) || !isSupportedCurrency(norm)) ? "USD" : norm;
        }
        try {
            java.util.Currency cur = java.util.Currency.getInstance(LocaleController.getInstance().getCurrentLocale());
            if (cur != null) {
                String normAuto = normalize(cur.getCurrencyCode());
                if (isSupportedCurrency(normAuto)) {
                    return normAuto;
                }
            }
        } catch (Exception ignored) {
        }
        return "USD";
    }

    public static void fetch(Utilities.Callback<State> callback) {
        if (callback == null) {
            return;
        }
        State cached = getCached();
        long now = System.currentTimeMillis();
        if (cached != null && now - cacheTime < CACHE_DURATION) {
            callback.run(cached);
            return;
        }

        synchronized (pendingCallbacks) {
            pendingCallbacks.add(callback);
            if (remoteInFlight) {
                return;
            }
            remoteInFlight = true;
        }

        Utilities.globalQueue.postRunnable(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            State newState = null;
            try {
                URL url = new URL("https://api.coinbase.com/v2/exchange-rates?currency=USD");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "100gram/Android");
                conn.connect();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    newState = parseCoinbaseJson(sb.toString());
                }
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {}
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }

            final State finalState = newState != null ? newState : getCached();
            if (newState != null) {
                cacheValue = newState;
                cacheTime = System.currentTimeMillis();
                try {
                    if (PillStackConfig.getEditor() != null) {
                        String json = newState.toJson();
                        PillStackConfig.getEditor().putString("exchangeRatesCache", json)
                                .putLong("exchangeRatesTimestamp", cacheTime)
                                .apply();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            AndroidUtilities.runOnUIThread(() -> completeRemote(finalState));
        });
    }

    private static void completeRemote(State state) {
        ArrayList<Utilities.Callback<State>> callbacks;
        synchronized (pendingCallbacks) {
            remoteInFlight = false;
            callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
        }
        for (Utilities.Callback<State> cb : callbacks) {
            cb.run(state);
        }
    }

    private static State parseCoinbaseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                return null;
            }
            JSONObject rates = data.optJSONObject("rates");
            if (rates == null) {
                return null;
            }

            Map<String, BigDecimal> map = new HashMap<>();
            for (String cur : MAIN_CURRENCIES) {
                if ("USD".equals(cur)) {
                    map.put("USD", BigDecimal.ONE);
                    continue;
                }
                String valStr = rates.optString(cur, null);
                if (valStr != null && !valStr.isEmpty()) {
                    try {
                        BigDecimal val = new BigDecimal(valStr);
                        if (val.signum() != 0) {
                            map.put(cur, BigDecimal.ONE.divide(val, 16, RoundingMode.HALF_UP));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            if (!map.isEmpty()) {
                return new State(map);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String normalize(String str) {
        if (str == null) {
            return "";
        }
        return str.trim().toUpperCase(Locale.ROOT);
    }
}
