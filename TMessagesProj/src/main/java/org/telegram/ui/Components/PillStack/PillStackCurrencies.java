package org.telegram.ui.Components.PillStack;

import org.telegram.messenger.BillingController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public abstract class PillStackCurrencies {

    private static final HashSet<String> AMBIGUOUS_SYMBOLS = new HashSet<>(Arrays.asList("$", "kr", "Fr", "₩"));
    private static final Map<String, CurrencyInfo> CURRENCIES = new HashMap<>();
    public static final String[] TARGET_CURRENCIES;

    public static final class CurrencyInfo {
        public final String code;
        public final int nameResId;
        public final String defaultName;
        public final String symbolOverride;
        public final boolean suffixSymbol;

        public CurrencyInfo(String code, int nameResId, String defaultName, String symbolOverride, boolean suffixSymbol) {
            this.code = code;
            this.nameResId = nameResId;
            this.defaultName = defaultName;
            this.symbolOverride = symbolOverride;
            this.suffixSymbol = suffixSymbol;
        }
    }

    static {
        addCurrency("USD", R.string.CryptoCurrencyUsd, "US Dollar", "$", false);
        addCurrency("EUR", R.string.CryptoCurrencyEur, "Euro", "€", false);
        addCurrency("RUB", R.string.CryptoCurrencyRub, "Russian Ruble", "₽", true);
        addCurrency("GBP", R.string.CryptoCurrencyGbp, "British Pound", "£", false);
        addCurrency("KZT", R.string.CryptoCurrencyKzt, "Kazakhstani Tenge", "₸", true);
        addCurrency("TRY", R.string.CryptoCurrencyTry, "Turkish Lira", "₺", true);
        addCurrency("UAH", R.string.CryptoCurrencyUah, "Ukrainian Hryvnia", "₴", true);
        addCurrency("PLN", R.string.CryptoCurrencyPln, "Polish Zloty", "zł", true);
        addCurrency("AED", R.string.CryptoCurrencyAed, "UAE Dirham", "AED", false);
        addCurrency("CNY", R.string.CryptoCurrencyCny, "Chinese Yuan", "CN¥", false);
        addCurrency("JPY", R.string.CryptoCurrencyJpy, "Japanese Yen", "¥", false);
        addCurrency("BYN", R.string.CryptoCurrencyByn, "Belarusian Ruble", "Br", true);
        addCurrency("ILS", R.string.CryptoCurrencyIls, "Israeli New Shekel", "₪", false);
        addCurrency("CZK", R.string.CryptoCurrencyCzk, "Czech Koruna", "Kč", true);
        addCurrency("INR", R.string.CryptoCurrencyInr, "Indian Rupee", "₹", false);

        String[] strArr = new String[16];
        TARGET_CURRENCIES = strArr;
        strArr[0] = "AUTO";
        System.arraycopy(new String[]{"AED", "BYN", "CNY", "CZK", "EUR", "GBP", "ILS", "INR", "JPY", "KZT", "PLN", "RUB", "TRY", "UAH", "USD"}, 0, strArr, 1, 15);
    }

    private static void addCurrency(String code, int nameResId, String defaultName, String symbolOverride, boolean suffixSymbol) {
        String norm = normalize(code);
        if (norm.isEmpty()) {
            return;
        }
        CURRENCIES.put(norm, new CurrencyInfo(norm, nameResId, defaultName, symbolOverride, suffixSymbol));
    }

    public static CharSequence getTargetCurrencyLabel(String str) {
        if (str == null || "AUTO".equalsIgnoreCase(str)) {
            return LocaleController.getString(R.string.Auto);
        }
        return getCurrencyLabelWithCode(str);
    }

    public static CharSequence getTargetCurrencySubtext(String str) {
        if (str == null || "AUTO".equalsIgnoreCase(str)) {
            return LocaleController.getString(R.string.Auto);
        }
        return getCurrencyName(str);
    }

    public static String getCurrencyName(String str) {
        String norm = normalize(str);
        CurrencyInfo info = CURRENCIES.get(norm);
        if (info == null) {
            return norm;
        }
        try {
            return LocaleController.getString(info.nameResId);
        } catch (Exception e) {
            return info.defaultName != null ? info.defaultName : norm;
        }
    }

    public static String getCurrencyLabelWithCode(String str) {
        String norm = normalize(str);
        CurrencyInfo info = CURRENCIES.get(norm);
        if (info == null) {
            return norm;
        }
        String name = getCurrencyName(norm);
        return name + " — " + norm;
    }

    public static String[] getTargetCurrencies(String exclude) {
        if (exclude == null || exclude.isEmpty()) {
            return TARGET_CURRENCIES;
        }
        int count = 0;
        for (String c : TARGET_CURRENCIES) {
            if (!exclude.equalsIgnoreCase(c)) {
                count++;
            }
        }
        String[] res = new String[count];
        int idx = 0;
        for (String c : TARGET_CURRENCIES) {
            if (!exclude.equalsIgnoreCase(c)) {
                res[idx++] = c;
            }
        }
        return res;
    }

    public static String formatFiatPrice(BigDecimal bigDecimal, String str) {
        if (bigDecimal != null && str != null && !str.isEmpty()) {
            try {
                int exp = Math.max(0, BillingController.getInstance().getCurrencyExp(str));
                BigDecimal scaled = bigDecimal.setScale(exp, RoundingMode.HALF_UP);
                Locale locale = Locale.US;
                NumberFormat format = NumberFormat.getNumberInstance(locale);
                format.setGroupingUsed(true);
                format.setMinimumFractionDigits(exp);
                format.setMaximumFractionDigits(exp);
                String formatted = format.format(scaled);

                String norm = normalize(str);
                CurrencyInfo info = CURRENCIES.get(norm);
                String symbol = info != null ? info.symbolOverride : null;
                boolean hasOverride = symbol != null;
                if (!hasOverride) {
                    try {
                        symbol = Currency.getInstance(norm).getSymbol(locale);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (symbol != null && !symbol.isEmpty() && !symbol.equalsIgnoreCase(norm)) {
                    if (!hasOverride && AMBIGUOUS_SYMBOLS.contains(symbol)) {
                        return formatted + " " + str;
                    }
                    if (info != null && info.suffixSymbol) {
                        return formatted + " " + symbol;
                    }
                    return symbol + formatted;
                }
                return formatted + " " + str;
            } catch (Exception ignored) {
            }
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
