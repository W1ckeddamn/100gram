package org.telegram.messenger;

import android.content.Context;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class BillingController {

    private static final String CURRENCY_FILE = "currencies.json";
    private static final String CURRENCY_EXP = "exp";
    private final Map<String, Integer> currencyExpMap = new HashMap<>();

    private static volatile BillingController instance;

    public static BillingController getInstance() {
        if (instance == null) {
            synchronized (BillingController.class) {
                if (instance == null) {
                    instance = new BillingController();
                }
            }
        }
        return instance;
    }

    private BillingController() {
        extractCurrencyExp();
    }

    private void extractCurrencyExp() {
        if (!currencyExpMap.isEmpty()) {
            return;
        }
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;
            InputStream in = ctx.getAssets().open(CURRENCY_FILE);
            byte[] bytes = new byte[in.available()];
            int read = 0;
            while (read < bytes.length) {
                int r = in.read(bytes, read, bytes.length - read);
                if (r == -1) break;
                read += r;
            }
            in.close();
            JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            Iterator<String> it = obj.keys();
            while (it.hasNext()) {
                String key = it.next();
                JSONObject currency = obj.optJSONObject(key);
                if (currency != null) {
                    currencyExpMap.put(key, currency.optInt(CURRENCY_EXP));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public int getCurrencyExp(String currency) {
        if (currency == null) return 0;
        extractCurrencyExp();
        return currencyExpMap.getOrDefault(currency, 0);
    }

    public String formatCurrency(long amount, String currency) {
        return formatCurrency(amount, currency, getCurrencyExp(currency));
    }

    public String formatCurrency(long amount, String currency, int exp) {
        return formatCurrency(amount, currency, exp, false);
    }

    public String formatCurrency(long amount, String currency, int exp, boolean rounded) {
        if (currency == null || currency.isEmpty()) {
            return String.valueOf(amount);
        }
        if ("TON".equalsIgnoreCase(currency)) {
            double val = amount / 1_000_000_000.0;
            if (rounded) {
                return "TON " + Math.round(val);
            }
            return "TON " + String.format(Locale.US, val >= 1.5 ? "%.2f" : "%.4f", val);
        }
        if ("XTR".equalsIgnoreCase(currency)) {
            return "⭐️ " + LocaleController.formatNumber(amount, ',');
        }
        try {
            Currency cur = Currency.getInstance(currency);
            if (cur != null) {
                NumberFormat format = NumberFormat.getCurrencyInstance(LocaleController.getInstance().getCurrentLocale());
                format.setCurrency(cur);
                if (rounded) {
                    format.setMaximumFractionDigits(0);
                    format.setMinimumFractionDigits(0);
                    return format.format(Math.round(amount / Math.pow(10, exp)));
                }
                int defaultFractionDigits = exp > 0 ? exp : cur.getDefaultFractionDigits();
                format.setMinimumFractionDigits(defaultFractionDigits);
                format.setMaximumFractionDigits(defaultFractionDigits);
                return format.format(amount / Math.pow(10, exp));
            }
        } catch (Exception ignore) {}
        return amount + " " + currency;
    }
}
