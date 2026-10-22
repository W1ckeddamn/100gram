package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.location.Location;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.HundredGramPillStackSettingsActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.Stories.recorder.Weather;

@SuppressLint({"ViewConstructor"})
public class WeatherPill extends BasePill implements NotificationCenter.NotificationCenterDelegate {

    private final ImageView iconView;
    private final LinearLayout layout;
    private boolean showingWeather;
    private final AnimatedTextView textView;

    @Override
    public long getRefreshInterval() {
        return 1_200_000L; // 20 minutes
    }

    public WeatherPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
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
        animatedTextView.setTypeface(AndroidUtilities.bold());
        animatedTextView.setIncludeFontPadding(false);
        animatedTextView.adaptWidth = true;
        NotificationCenter.listenEmojiLoading(animatedTextView);
        linearLayout.addView(animatedTextView, LayoutHelper.createLinear(-2, -2, Gravity.CENTER_VERTICAL));

        setLoadingTargetView(linearLayout);
        updateColors();
        ScaleStateListAnimator.apply(linearLayout);

        Weather.State cached = Weather.getCached();
        if (cached != null) {
            setData(cached, false);
        }
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_WEATHER;
    }

    @Override
    public void onPillClicked() {
        if (PillStackConfig.getUseCurrentLocation() && !showingWeather && (!Weather.isLocationPermissionGranted() || !Weather.isLocationEnabled())) {
            requestLocationAndUpdate();
        } else {
            onPillLongClicked();
        }
    }

    @Override
    public boolean onPillLongClicked() {
        final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return false;
        }
        ItemOptions.makeOptions(fragment, this)
                .add(R.drawable.msg_retry, LocaleController.getString(R.string.Refresh), () -> onUpdateData(true))
                .add(R.drawable.msg_settings, LocaleController.getString(R.string.Settings), () -> fragment.presentFragment(new HundredGramPillStackSettingsActivity()))
                .setDrawScrim(false)
                .setDimAlpha(0)
                .show();
        return true;
    }

    @Override
    public void onUpdateData(boolean force) {
        if (PillStackConfig.getUseCurrentLocation()) {
            if (!Weather.isLocationPermissionGranted()) {
                setLocationState(R.string.WeatherLocationPermissionGrant, showingWeather);
                return;
            } else if (!Weather.isLocationEnabled()) {
                setLocationState(R.string.WeatherLocationServicesEnable, showingWeather);
                return;
            }
        }
        if (force) {
            Weather.clearCache();
        }
        startLoading();
        Weather.fetchExtera(state -> {
            if (state != null) {
                markDataUpdated();
                postDelayed(() -> setData(state, true), 300L);
            } else {
                postDelayed(this::setErrorState, 300L);
            }
        });
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
            PillStackConfig.checkAndClearPendingUpdate(getPillId());
            onUpdateData(true);
        }
    }

    private void setLocationState(int resId, boolean animate) {
        stopLoading();
        if (animate) {
            animateSizeChange();
        }
        iconView.setImageResource(R.drawable.filled_location);
        iconView.setVisibility(View.VISIBLE);
        textView.setText(LocaleController.getString(resId), animate);
        showingWeather = false;
    }

    private void requestLocationAndUpdate() {
        Weather.getUserLocation(true, location -> {
            if (location != null) {
                onUpdateData(true);
            }
        });
    }

    private void setErrorState() {
        stopLoading();
        animateSizeChange();
        iconView.setImageResource(R.drawable.msg_retry);
        iconView.setVisibility(View.VISIBLE);
        textView.setText(LocaleController.getString(R.string.Retry), true);
        showingWeather = false;
    }

    public void setData(Weather.State state, boolean animate) {
        stopLoading();
        if (state == null) {
            return;
        }
        if (animate) {
            animateSizeChange();
        }
        int iconRes = getWeatherIconRes(state.getEmoji());
        if (iconRes != 0) {
            iconView.setImageResource(iconRes);
            iconView.setVisibility(View.VISIBLE);
            textView.setText(state.getTemperature(), animate);
        } else {
            iconView.setVisibility(View.GONE);
            String emojiStr = state.getEmoji() != null ? state.getEmoji() : "";
            textView.setText(Emoji.replaceEmoji(String.format("%s %s", emojiStr, state.getTemperature()).trim(), textView.getPaint().getFontMetricsInt(), true), animate);
        }
        showingWeather = true;
    }

    private int getWeatherIconRes(String emoji) {
        if (emoji == null) {
            return 0;
        }
        switch (emoji) {
            case "☁":
                return R.drawable.weather_cloudy;
            case "⛅":
            case "🌤":
                return R.drawable.weather_partly_cloudy;
            case "☀":
                return R.drawable.weather_sunny;
            case "🌧":
            case "🌦":
                return R.drawable.weather_rainy;
            case "❄":
            case "🌨":
                return R.drawable.weather_snowy;
            case "⚡":
            case "⛈":
                return R.drawable.weather_thunderstorm;
            case "😶‍🌫":
                return R.drawable.weather_foggy;
            case "🌚":
            case "🌜":
            case "🌛":
            case "🌝":
            case "🌓":
            case "🌗":
            case "🌔":
            case "🌖":
                return R.drawable.weather_night;
            default:
                return 0;
        }
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
    public void drawableHotspotChanged(float x, float y) {
        if (loading) {
            return;
        }
        super.drawableHotspotChanged(x, y);
        layout.drawableHotspotChanged(x - layout.getLeft(), y - layout.getTop());
    }

    @Override
    public void updateColors() {
        int themedColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText, 0.75f);
        layout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(14.0f),
                Theme.isCurrentThemeDark() ? getThemedColor(Theme.key_windowBackgroundWhite) : Theme.multAlpha(themedColor, 0.09f),
                Theme.multAlpha(themedColor, 0.1f)
        ));
        textView.setTextColor(themedColor);
        iconView.setColorFilter(new PorterDuffColorFilter(themedColor, PorterDuff.Mode.MULTIPLY));
        updateLoadingColors();
    }
}
