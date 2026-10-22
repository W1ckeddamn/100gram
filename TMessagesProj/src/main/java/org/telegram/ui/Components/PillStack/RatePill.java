package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LoadingDrawable;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.HundredGramPillStackSettingsActivity;
import org.telegram.ui.LaunchActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;

@SuppressLint({"ViewConstructor"})
public abstract class RatePill extends BasePill implements NotificationCenter.NotificationCenterDelegate {

    private final ColoredBackground background;
    private final String baseCurrency;
    private final RateCache cache;
    private final int iconResId;
    private final ImageView iconView;
    private final LinearLayout layout;
    private boolean requestInFlight;
    private final int scale;
    private final AnimatedTextView textView;

    public static final class RateCache {
        public final AtomicReference<String> cachedPrice = new AtomicReference<>();
        public final AtomicReference<String> cachedCurrency = new AtomicReference<>();
    }

    @Override
    public long getRefreshInterval() {
        return 300_000L; // 5 minutes
    }

    public abstract String getTargetSelection();
    public abstract void setTargetSelection(String str);

    public RatePill(Context context, Theme.ResourcesProvider resourcesProvider, RateCache rateCache, String baseCurrency, int scale, int iconResId, ColoredBackground coloredBackground) {
        super(context, resourcesProvider);
        this.cache = rateCache;
        this.baseCurrency = baseCurrency;
        this.scale = scale;
        this.iconResId = iconResId;
        this.background = coloredBackground;

        LinearLayout linearLayout = new LinearLayout(context);
        this.layout = linearLayout;
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setMinimumWidth(AndroidUtilities.dp(48.0f));
        linearLayout.setPadding(AndroidUtilities.dp(8.0f), 0, AndroidUtilities.dp(8.0f), 0);
        addView(linearLayout, LayoutHelper.createFrame(-2, 28, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL));

        ImageView imageView = new ImageView(context);
        this.iconView = imageView;
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        linearLayout.addView(imageView, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));

        AnimatedTextView animatedTextView = new AnimatedTextView(context, true, true, true);
        this.textView = animatedTextView;
        animatedTextView.setTextSize(AndroidUtilities.dp(13.0f));
        animatedTextView.setIncludeFontPadding(false);
        animatedTextView.setTypeface(AndroidUtilities.bold());
        animatedTextView.adaptWidth = true;
        linearLayout.addView(animatedTextView, LayoutHelper.createLinear(-2, -2, Gravity.CENTER_VERTICAL));

        setLoadingTargetView(linearLayout);
        updateColors();
        ScaleStateListAnimator.apply(linearLayout);

        String cachedStr = rateCache.cachedPrice.get();
        if (cachedStr != null) {
            setData(cachedStr, false);
        }
    }

    @Override
    public void onPillClicked() {
        if (iconView.getVisibility() == View.VISIBLE && textView.getText() != null && TextUtils.equals(textView.getText(), LocaleController.getString(R.string.Retry))) {
            onUpdateData(true);
        } else {
            onPillLongClicked();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pillStackSettingsChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pillStackSettingsChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.pillStackSettingsChanged && PillStackConfig.shouldUpdatePill(args, getPillId())) {
            if ("AUTO".equalsIgnoreCase(getTargetSelection())) {
                PillStackConfig.checkAndClearPendingUpdate(getPillId());
                onUpdateData(true);
            }
        }
    }

    @Override
    public boolean onPillLongClicked() {
        final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return false;
        }

        final ItemOptions options = ItemOptions.makeOptions(fragment, this, true);
        final ItemOptions currencySubmenu = options.makeSwipeback()
                .add(R.drawable.ic_ab_back, LocaleController.getString(R.string.Back), () -> options.closeSwipeback())
                .addGap();

        final String currentSelection = getTargetSelection();
        for (final String cur : getTargetCurrencies()) {
            boolean checked = cur.equalsIgnoreCase(currentSelection);
            currencySubmenu.addChecked(checked, PillStackCurrencies.getTargetCurrencyLabel(cur), () -> {
                options.dismiss();
                if (!cur.equalsIgnoreCase(currentSelection)) {
                    setTargetSelection(cur);
                    onUpdateData(false);
                }
            });
        }

        ActionBarMenuSubItem currencyItem = new ActionBarMenuSubItem(options.getContext(), false, false, resourcesProvider);
        currencyItem.setTextAndIcon(LocaleController.getString("CryptoPillTargetCurrency", R.string.CryptoPillTargetCurrency), R.drawable.msg_language);
        currencyItem.setSubtext(PillStackCurrencies.getTargetCurrencySubtext(getTargetSelection()));
        currencyItem.setItemHeight(56);
        currencyItem.setOnClickListener(v -> options.openSwipeback(currencySubmenu));

        options.add(currencyItem);
        options.addGap()
                .add(R.drawable.msg_retry, LocaleController.getString(R.string.Refresh), () -> onUpdateData(true))
                .add(R.drawable.msg_settings, LocaleController.getString(R.string.Settings), () -> fragment.presentFragment(new HundredGramPillStackSettingsActivity()))
                .setSwipebackGravity(!LocaleController.isRTL, false)
                .forceBottom(true)
                .setDrawScrim(false)
                .setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT)
                .setDimAlpha(0)
                .show();

        return true;
    }

    @Override
    public void onUpdateData(boolean force) {
        final String target = ExchangeRates.resolveTargetCurrency(UserConfig.selectedAccount, getTargetSelection());
        String cached = cache.cachedPrice.get();
        if (!TextUtils.equals(target, cache.cachedCurrency.get())) {
            cached = null;
        }
        if (!force && cached != null && !isRefreshDue()) {
            setData(cached, false);
            return;
        }
        if (requestInFlight) {
            return;
        }
        requestInFlight = true;
        if (force) {
            animateSizeChange();
        }
        startLoading();

        if (cached == null && cache.cachedPrice.get() == null) {
            iconView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        } else {
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
        }

        if (force) {
            ExchangeRates.clearCache();
        }

        ExchangeRates.fetch(state -> {
            requestInFlight = false;
            if (state == null) {
                String fallback = cache.cachedPrice.get();
                if (fallback != null) {
                    setData(fallback, true);
                } else {
                    setErrorState(true);
                }
                return;
            }
            BigDecimal rate = state.getRate(baseCurrency, target);
            if (rate == null) {
                String fallback = cache.cachedPrice.get();
                if (fallback != null) {
                    setData(fallback, true);
                } else {
                    setErrorState(true);
                }
                return;
            }
            String formatted = formatPrice(rate, target);
            cache.cachedPrice.set(formatted);
            cache.cachedCurrency.set(target);
            setData(formatted, true);
            markDataUpdated();
        });
    }

    public String formatPrice(BigDecimal rate, String target) {
        String fiat = PillStackCurrencies.formatFiatPrice(rate, target);
        if (fiat != null) {
            return fiat;
        }
        return rate.setScale(scale, RoundingMode.HALF_UP).toPlainString() + " " + target;
    }

    public String[] getTargetCurrencies() {
        return PillStackCurrencies.TARGET_CURRENCIES;
    }

    private void setErrorState(boolean animate) {
        stopLoading();
        if (animate) {
            animateSizeChange();
        }
        iconView.setImageResource(R.drawable.msg_retry);
        iconView.setVisibility(View.VISIBLE);
        textView.setText(LocaleController.getString(R.string.Retry), animate);
        textView.setVisibility(View.VISIBLE);
    }

    private void setData(String text, boolean animate) {
        stopLoading();
        if (animate) {
            animateSizeChange();
        }
        iconView.setImageResource(iconResId);
        iconView.setVisibility(View.VISIBLE);
        textView.setText(text, animate);
        textView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPressed(boolean pressed) {
        if (loading) {
            pressed = false;
        }
        super.setPressed(pressed);
        layout.setPressed(pressed);
    }

    @Override
    public void updateColors() {
        layout.setBackground(background);
        textView.setTextColor(0xFFFFFFFF);
        iconView.setColorFilter(0xFFFFFFFF);
        updateLoadingColors();
    }

    @Override
    public void updateLoadingColors() {
        if (loadingDrawable != null) {
            loadingDrawable.setColors(
                    Theme.multAlpha(0xFFFFFFFF, 0.1f),
                    Theme.multAlpha(0xFFFFFFFF, 0.3f),
                    Theme.multAlpha(0xFFFFFFFF, 0.2f),
                    Theme.multAlpha(0xFFFFFFFF, 0.45f)
            );
        }
    }
}
