package com.exteragram.messenger.adblock.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.telegram.messenger.HundredGramConfig;
import com.exteragram.messenger.adblock.WebAdBlocker;
import com.exteragram.messenger.adblock.backend.AdBlockManager;
import java.util.Objects;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;

@SuppressLint({"ViewConstructor"})
public class AdBlockMenuItem extends ActionBarMenuSubItem {
    private static final int[] BLOCKED_COUNT_SAMPLES = {991, 992, 999};
    private final WebAdBlocker adBlocker;
    private final ItemOptions options;
    private CharSequence subtext;
    private final Switch switchView;
    private boolean toggled;
    private final Runnable updateRunnable;

    public static void addTo(ItemOptions itemOptions, WebAdBlocker webAdBlocker) {
        if (webAdBlocker == null || !AdBlockManager.isAvailable()) {
            return;
        }
        if (HundredGramConfig.enableAdBlock && !AdBlockManager.isActive()) {
            AdBlockManager.initialize();
        }
        AdBlockMenuItem adBlockMenuItem = new AdBlockMenuItem(itemOptions.getContext(), webAdBlocker, itemOptions);
        itemOptions.add(adBlockMenuItem);
        adBlockMenuItem.alignSwitch();
        adBlockMenuItem.fitWidth();
        itemOptions.addGap();
    }

    private AdBlockMenuItem(Context context, WebAdBlocker webAdBlocker, ItemOptions itemOptions) {
        super(context, false, false, (Theme.ResourcesProvider) null);
        this.updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (AdBlockMenuItem.this.toggled) {
                    return;
                }
                AdBlockMenuItem.this.update();
                AndroidUtilities.runOnUIThread(this, 500L);
            }
        };
        this.adBlocker = webAdBlocker;
        this.options = itemOptions;
        setTextAndIcon(LocaleController.getString("BlockAds", R.string.BlockAds), R.drawable.msg_policy);
        setClipToPadding(false);
        Switch r4 = new Switch(context);
        this.switchView = r4;
        int i = Theme.key_switchTrack;
        int i2 = Theme.key_switchTrackChecked;
        int i3 = Theme.key_windowBackgroundWhite;
        r4.setColors(i, i2, i3, i3);
        r4.setImportantForAccessibility(2);
        r4.setChecked(HundredGramConfig.enableAdBlock, false);
        if (HundredGramConfig.md3SwitchStyle) {
            r4.setScaleX(0.85f);
            r4.setScaleY(0.85f);
        }
        addView(r4, LayoutHelper.createFrame(44, 28, (LocaleController.isRTL ? 3 : 5) | 16));
        setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AdBlockMenuItem.this.toggle();
            }
        });
        update();
    }

    private void alignSwitch() {
        float dp;
        float dp2;
        if (HundredGramConfig.md3SwitchStyle) {
            dp = AndroidUtilities.dp(52.0f) * 0.85f;
            dp2 = AndroidUtilities.dp(32.0f) * 0.85f;
        } else {
            dp = AndroidUtilities.dp(37.0f);
            dp2 = AndroidUtilities.dp(20.0f);
        }
        float dp3 = (AndroidUtilities.dp(this.subtext != null ? 56.0f : 48.0f) - dp2) / 2.0f;
        float paddingLeft = LocaleController.isRTL ? getPaddingLeft() : getPaddingRight();
        int round = Math.round((((dp - AndroidUtilities.dp(44.0f)) / 2.0f) + dp3) - paddingLeft);
        int round2 = Math.round(((dp3 + dp) + AndroidUtilities.dp(12.0f)) - paddingLeft);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.switchView.getLayoutParams();
        if (LocaleController.isRTL) {
            layoutParams.leftMargin = round;
            AnimatedEmojiSpan.TextViewEmojis textViewEmojis = ((ActionBarMenuSubItem) this).textView;
            textViewEmojis.setPadding(round2, 0, textViewEmojis.getPaddingRight(), 0);
        } else {
            layoutParams.rightMargin = round;
            AnimatedEmojiSpan.TextViewEmojis textViewEmojis2 = ((ActionBarMenuSubItem) this).textView;
            textViewEmojis2.setPadding(textViewEmojis2.getPaddingLeft(), 0, round2, 0);
        }
        TextView textView = ((ActionBarMenuSubItem) this).subtextView;
        if (textView != null) {
            textView.setPadding(((ActionBarMenuSubItem) this).textView.getPaddingLeft(), 0, ((ActionBarMenuSubItem) this).textView.getPaddingRight(), 0);
        }
    }

    private void fitWidth() {
        int minimumWidth = getMinimumWidth();
        if (minimumWidth <= 0) {
            return;
        }
        TextPaint textPaint = new TextPaint(1);
        textPaint.setTextSize(AndroidUtilities.dp(13.0f));
        float measureText = textPaint.measureText(LocaleController.getString("AdBlockFiltersLoading", R.string.AdBlockFiltersLoading));
        for (int i : BLOCKED_COUNT_SAMPLES) {
            measureText = Math.max(measureText, textPaint.measureText(LocaleController.formatPluralString("BlockedRequests", i)));
        }
        float currentTextWidth = ((ActionBarMenuSubItem) this).textView != null && ((ActionBarMenuSubItem) this).textView.getText() != null
                ? ((ActionBarMenuSubItem) this).textView.getPaint().measureText(((ActionBarMenuSubItem) this).textView.getText().toString())
                : 0f;
        int totalWidth = (int) (getPaddingLeft() + getPaddingRight()
                + (((ActionBarMenuSubItem) this).textView != null ? ((ActionBarMenuSubItem) this).textView.getPaddingLeft() + ((ActionBarMenuSubItem) this).textView.getPaddingRight() : 0)
                + Math.ceil(Math.max(currentTextWidth, measureText)));

        if (Math.min(totalWidth, AndroidUtilities.displaySize.x - AndroidUtilities.dp(32.0f)) > minimumWidth) {
            int ceil = (int) Math.ceil(totalWidth / AndroidUtilities.density);
            this.options.setMinWidth(ceil);
            for (int i2 = 0; i2 < this.options.getItemsCount(); i2++) {
                View itemAt = this.options.getItemAt(i2);
                if (itemAt instanceof ActionBarMenuSubItem) {
                    itemAt.setMinimumWidth(AndroidUtilities.dp(ceil));
                    if (itemAt.getLayoutParams() != null) {
                        itemAt.getLayoutParams().width = AndroidUtilities.dp(ceil);
                    }
                }
            }
        }
    }

    private void update() {
        String str;
        if (!HundredGramConfig.enableAdBlock) {
            str = null;
        } else if (AdBlockManager.isActive()) {
            str = LocaleController.formatPluralString("BlockedRequests", this.adBlocker.getBlockedCount());
        } else {
            str = LocaleController.getString("AdBlockFiltersLoading", R.string.AdBlockFiltersLoading);
        }
        if (((ActionBarMenuSubItem) this).subtextView == null || !TextUtils.equals(this.subtext, str)) {
            this.subtext = str;
            setItemHeight(str != null ? 56 : 48);
            setSubtext(this.subtext);
        }
    }

    private void toggle() {
        if (this.toggled) {
            return;
        }
        this.toggled = true;
        boolean enableAdBlock = HundredGramConfig.enableAdBlock;
        this.switchView.setChecked(!enableAdBlock, true);
        if (!enableAdBlock) {
            final WebAdBlocker webAdBlocker = this.adBlocker;
            HundredGramConfig.toggleEnableAdBlock();
            AdBlockManager.setEnabled(true, new Runnable() {
                @Override
                public final void run() {
                    webAdBlocker.reload();
                }
            });
        } else {
            HundredGramConfig.toggleEnableAdBlock();
            AdBlockManager.setEnabled(false, null);
            this.adBlocker.reload();
        }
        final ItemOptions itemOptions = this.options;
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public final void run() {
                itemOptions.dismiss();
            }
        }, 200L);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        AndroidUtilities.runOnUIThread(this.updateRunnable, 500L);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AndroidUtilities.cancelRunOnUIThread(this.updateRunnable);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName("android.widget.Switch");
        accessibilityNodeInfo.setCheckable(true);
        accessibilityNodeInfo.setChecked(this.switchView.isChecked());
    }
}