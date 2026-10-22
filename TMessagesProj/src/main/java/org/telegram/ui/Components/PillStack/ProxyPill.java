package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProxyListActivity;

@SuppressLint({"ViewConstructor"})
public class ProxyPill extends BasePill implements NotificationCenter.NotificationCenterDelegate {

    private final ImageView iconView;
    private int lastAccount;
    private final LinearLayout layout;
    private final AnimatedTextView textView;

    @Override
    public long getRefreshInterval() {
        return 0L;
    }

    public ProxyPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
        LinearLayout linearLayout = new LinearLayout(context);
        this.layout = linearLayout;
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setMinimumWidth(AndroidUtilities.dp(48.0f));
        linearLayout.setPadding(AndroidUtilities.dp(8.0f), 0, AndroidUtilities.dp(10.0f), 0);
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
        onUpdateData(false);
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_PROXY;
    }

    @Override
    public void onUpdateData(boolean force) {
        String string;
        SharedConfig.ProxyInfo proxyInfo;
        boolean isProxyEnabled = SharedConfig.isProxyEnabled();
        int connectionState = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        boolean connected = connectionState == ConnectionsManager.ConnectionStateConnected || connectionState == ConnectionsManager.ConnectionStateUpdating;
        String currentText = this.textView.getText() != null ? this.textView.getText().toString() : "";

        if (!isProxyEnabled || (proxyInfo = SharedConfig.currentProxy) == null) {
            this.iconView.setImageResource(R.drawable.outline_shield_plain_24);
            string = LocaleController.getString("Proxy", R.string.Proxy);
            stopLoading();
        } else if (connected) {
            long ping = Utilities.clamp(proxyInfo.ping, 9999L, 0L);
            this.iconView.setImageResource(R.drawable.outline_shield_check);
            if (ping > 0) {
                string = ping + " ms";
            } else {
                string = LocaleController.getString("MenuProxyConnected", R.string.MenuProxyConnected);
            }
            stopLoading();
        } else {
            this.iconView.setImageResource(R.drawable.outline_shield_plain_24);
            string = LocaleController.getString("MenuProxyConnecting", R.string.MenuProxyConnecting);
            startLoading();
        }

        if (force || !TextUtils.equals(currentText, string)) {
            if (force) {
                animateSizeChange();
            }
            this.textView.setText(string, force);
        }
        updateColors();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        onUpdateData(true);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxyCheckDone);
        int account = UserConfig.selectedAccount;
        this.lastAccount = account;
        NotificationCenter.getInstance(account).addObserver(this, NotificationCenter.didUpdateConnectionState);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxyCheckDone);
        NotificationCenter.getInstance(this.lastAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.proxySettingsChanged || id == NotificationCenter.proxyCheckDone || id == NotificationCenter.didUpdateConnectionState) {
            onUpdateData(true);
        }
    }

    @Override
    public void onPillClicked() {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment != null) {
            fragment.presentFragment(new ProxyListActivity());
        }
    }

    @Override
    public boolean onPillLongClicked() {
        final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return false;
        }
        ItemOptions.makeOptions(fragment, this)
                .add(R.drawable.outline_shield_check, LocaleController.getString("ProxySettings", R.string.ProxySettings), () -> {
                    fragment.presentFragment(new ProxyListActivity());
                })
                .add(R.drawable.msg_retry, LocaleController.getString("Refresh", R.string.Refresh), () -> {
                    onUpdateData(true);
                })
                .setDrawScrim(false)
                .setDimAlpha(0)
                .show();
        return true;
    }

    @Override
    public void setPressed(boolean pressed) {
        if (this.loading) {
            pressed = false;
        }
        super.setPressed(pressed);
        this.layout.setPressed(pressed);
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        if (this.loading) {
            return;
        }
        super.drawableHotspotChanged(x, y);
        this.layout.drawableHotspotChanged(x - this.layout.getLeft(), y - this.layout.getTop());
    }

    @Override
    public void updateColors() {
        int themedColor;
        boolean isProxyEnabled = SharedConfig.isProxyEnabled();
        int connectionState = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        boolean connected = connectionState == ConnectionsManager.ConnectionStateConnected || connectionState == ConnectionsManager.ConnectionStateUpdating;
        if (isProxyEnabled && SharedConfig.currentProxy != null && connected) {
            themedColor = getThemedColor(Theme.key_windowBackgroundWhiteGreenText);
        } else {
            themedColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText, 0.75f);
        }
        this.layout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(14.0f),
                Theme.isCurrentThemeDark() ? getThemedColor(Theme.key_windowBackgroundWhite) : Theme.multAlpha(themedColor, 0.09f),
                Theme.multAlpha(themedColor, 0.15f)
        ));
        this.textView.setTextColor(themedColor);
        this.iconView.setColorFilter(new PorterDuffColorFilter(themedColor, PorterDuff.Mode.SRC_IN));
        updateLoadingColors();
    }
}
