package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;

public class HundredGramCardCell extends FrameLayout {

    private final FrameLayout iconBadgeLayout;
    private final ImageView iconView;
    private final SimpleTextView titleView;
    private final SimpleTextView subtitleView;
    private final TextView badgeView;
    private final ImageView arrowView;
    private final View divider;

    public HundredGramCardCell(Context context) {
        super(context);

        setBackground(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));

        // Icon badge on left
        iconBadgeLayout = new FrameLayout(context);
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconBadgeLayout.addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        addView(iconBadgeLayout, LayoutHelper.createFrame(42, 42, Gravity.CENTER_VERTICAL | Gravity.START, 16, 10, 0, 10));

        // Texts in center
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        titleView = new SimpleTextView(context);
        titleView.setTextSize(16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        subtitleView = new SimpleTextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 72, 0, 70, 0));

        // Right container for badge & arrow
        LinearLayout rightLayout = new LinearLayout(context);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        rightLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        badgeView = new TextView(context);
        badgeView.setTextSize(12);
        badgeView.setTypeface(AndroidUtilities.bold());
        badgeView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        badgeView.setVisibility(GONE);
        rightLayout.addView(badgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.msg_arrowright);
        arrowView.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        rightLayout.addView(arrowView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        addView(rightLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 16, 0));

        // Divider
        divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 72, 0, 0, 0));
    }

    public void setData(int iconRes, int badgeBgColor, int iconColor, CharSequence title, CharSequence subtitle, CharSequence badge, boolean needDivider) {
        // Soft rounded badge for icon
        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.RECTANGLE);
        badgeDrawable.setCornerRadius(AndroidUtilities.dp(12));
        badgeDrawable.setColor(badgeBgColor);
        iconBadgeLayout.setBackground(badgeDrawable);

        iconView.setImageResource(iconRes);
        iconView.setColorFilter(iconColor);

        titleView.setText(title);

        if (!TextUtils.isEmpty(subtitle)) {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(VISIBLE);
        } else {
            subtitleView.setVisibility(GONE);
        }

        if (!TextUtils.isEmpty(badge)) {
            badgeView.setText(badge);
            badgeView.setVisibility(VISIBLE);

            GradientDrawable pillDrawable = new GradientDrawable();
            pillDrawable.setShape(GradientDrawable.RECTANGLE);
            pillDrawable.setCornerRadius(AndroidUtilities.dp(12));
            boolean isDark = Theme.isCurrentThemeDark();
            pillDrawable.setColor(isDark ? 0x22FFFFFF : 0x14000000);
            badgeView.setBackground(pillDrawable);
            badgeView.setTextColor(iconColor);
        } else {
            badgeView.setVisibility(GONE);
        }

        if (org.telegram.messenger.HundredGramConfig.dividerStyle == org.telegram.messenger.HundredGramConfig.DIVIDER_HIDDEN) {
            divider.setVisibility(GONE);
        } else if (org.telegram.messenger.HundredGramConfig.dividerStyle == org.telegram.messenger.HundredGramConfig.DIVIDER_SEGMENTS) {
            divider.setVisibility(needDivider ? VISIBLE : GONE);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) divider.getLayoutParams();
            if (lp != null) {
                lp.leftMargin = AndroidUtilities.dp(72);
                lp.rightMargin = AndroidUtilities.dp(16);
                divider.setLayoutParams(lp);
            }
        } else {
            divider.setVisibility(needDivider ? VISIBLE : GONE);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) divider.getLayoutParams();
            if (lp != null) {
                lp.leftMargin = AndroidUtilities.dp(72);
                lp.rightMargin = 0;
                divider.setLayoutParams(lp);
            }
        }
    }
}
