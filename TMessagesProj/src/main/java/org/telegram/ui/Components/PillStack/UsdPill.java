package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint({"ViewConstructor"})
public class UsdPill extends RatePill {
    private static final RatePill.RateCache CACHE = new RatePill.RateCache();

    public UsdPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider, CACHE, "USD", 2, R.drawable.pillstack_usd, new ColoredBackground(-14840995, -15172775));
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_USD;
    }

    @Override
    public String getTargetSelection() {
        if ("USD".equalsIgnoreCase(PillStackConfig.getUsdTargetCurrency())) {
            return "AUTO";
        }
        return PillStackConfig.getUsdTargetCurrency();
    }

    @Override
    public void setTargetSelection(String str) {
        PillStackConfig.setUsdTargetCurrency(str);
    }

    @Override
    public String[] getTargetCurrencies() {
        return PillStackCurrencies.getTargetCurrencies("USD");
    }
}
