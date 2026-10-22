package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint({"ViewConstructor"})
public class GramPill extends RatePill {
    private static final RatePill.RateCache CACHE = new RatePill.RateCache();

    public GramPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider, CACHE, "TON", 3, R.drawable.mini_gram_16, new ColoredBackground());
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_GRAM;
    }

    @Override
    public String getTargetSelection() {
        return PillStackConfig.getGramTargetCurrency();
    }

    @Override
    public void setTargetSelection(String str) {
        PillStackConfig.setGramTargetCurrency(str);
    }
}
