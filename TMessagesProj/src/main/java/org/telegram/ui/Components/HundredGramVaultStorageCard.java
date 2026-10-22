package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.HundredGramSubSettingsActivity;

@SuppressLint("ViewConstructor")
public class HundredGramVaultStorageCard extends FrameLayout {

    private final TextView totalSizeText;
    private final TextView quotaSubText;
    private final View progressBar;
    private final BaseFragment parentFragment;
    private long cachedSize = 0;

    public HundredGramVaultStorageCard(Context context, BaseFragment fragment) {
        super(context);
        this.parentFragment = fragment;

        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        FrameLayout card = new FrameLayout(context);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        boolean isDark = Theme.isCurrentThemeDark();
        cardBg.setColor(isDark ? 0xFF192533 : 0xFFEDF4FB);
        cardBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x22FFFFFF : 0x1A000000);
        card.setBackground(cardBg);
        addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 48, 14, 6, 14, 10));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        card.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Top Row: Icon + Title + Size
        FrameLayout headerRow = new FrameLayout(context);

        LinearLayout leftTitle = new LinearLayout(context);
        leftTitle.setOrientation(LinearLayout.HORIZONTAL);
        leftTitle.setGravity(Gravity.CENTER_VERTICAL);

        ImageView vaultIcon = new ImageView(context);
        vaultIcon.setImageResource(R.drawable.msg_download);
        vaultIcon.setColorFilter(0xFF00B0FF);
        leftTitle.addView(vaultIcon, LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView title = new TextView(context);
        title.setText(LocaleController.getString("HundredGramVaultCardTitle", R.string.HundredGramVaultCardTitle));
        title.setTextSize(15);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        leftTitle.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        headerRow.addView(leftTitle, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));

        totalSizeText = new TextView(context);
        totalSizeText.setText("...");
        totalSizeText.setTextSize(15);
        totalSizeText.setTypeface(AndroidUtilities.bold());
        totalSizeText.setTextColor(0xFF00B0FF);
        headerRow.addView(totalSizeText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        content.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Multi-color Progress Bar
        progressBar = new View(context) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF rect = new RectF();

            @Override
            protected void onDraw(Canvas canvas) {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                rect.set(0, 0, w, h);
                paint.setColor(Theme.isCurrentThemeDark() ? 0x22FFFFFF : 0x1A000000);
                canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);

                long maxQuota = HundredGramConfig.vaultMaxSizeGb <= 0 ? (10L * 1024L * 1024L * 1024L) : ((long) HundredGramConfig.vaultMaxSizeGb * 1024L * 1024L * 1024L);
                float progress = Math.min(1.0f, Math.max(0.04f, (float) cachedSize / (float) maxQuota));

                float usedW = w * progress;
                rect.set(0, 0, usedW, h);
                paint.setColor(0xFF00B0FF);
                canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
            }
        };
        content.addView(progressBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8, 0, 0, 0, 8));

        // Subtitle row (Quota info + Tap to manage)
        FrameLayout subRow = new FrameLayout(context);

        quotaSubText = new TextView(context);
        updateQuotaText();
        quotaSubText.setTextSize(12);
        quotaSubText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subRow.addView(quotaSubText, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));

        TextView manageHint = new TextView(context);
        manageHint.setText(LocaleController.getString("HundredGramVaultManage", R.string.HundredGramVaultManage) + " ›");
        manageHint.setTextSize(12);
        manageHint.setTypeface(AndroidUtilities.bold());
        manageHint.setTextColor(0xFF00B0FF);
        subRow.addView(manageHint, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.END | Gravity.CENTER_VERTICAL));

        content.addView(subRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        card.setOnClickListener(v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_VAULT));
            }
        });

        loadVaultSize();
    }

    public void update() {
        loadVaultSize();
        updateQuotaText();
    }

    private void updateQuotaText() {
        if (quotaSubText == null) return;
        if (HundredGramConfig.vaultMaxSizeGb <= 0) {
            quotaSubText.setText(LocaleController.getString("VaultMaxSizeUnlimited", R.string.VaultMaxSizeUnlimited));
        } else {
            quotaSubText.setText(String.format(LocaleController.getString("HundredGramVaultQuotaInfo", R.string.HundredGramVaultQuotaInfo), HundredGramConfig.vaultMaxSizeGb * 1024));
        }
    }

    private void loadVaultSize() {
        int account = parentFragment != null ? parentFragment.getCurrentAccount() : UserConfig.selectedAccount;
        Utilities.globalQueue.postRunnable(() -> {
            long size = HundredGramVaultManager.getInstance(account).getVaultTotalSize();
            cachedSize = size;
            String formatted = AndroidUtilities.formatFileSize(size);
            AndroidUtilities.runOnUIThread(() -> {
                totalSizeText.setText(formatted);
                if (progressBar != null) {
                    progressBar.invalidate();
                }
            });
        });
    }
}
