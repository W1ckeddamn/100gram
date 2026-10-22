package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint({"ViewConstructor"})
public class BtcPill extends RatePill {
    private static final RatePill.RateCache CACHE = new RatePill.RateCache();

    public BtcPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider, CACHE, "BTC", 2, R.drawable.pillstack_btc, new ColoredBackground(-1071598, -1608430));
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_BTC;
    }

    @Override
    public String getTargetSelection() {
        return PillStackConfig.getBtcTargetCurrency();
    }

    @Override
    public void setTargetSelection(String str) {
        PillStackConfig.setBtcTargetCurrency(str);
    }
}
