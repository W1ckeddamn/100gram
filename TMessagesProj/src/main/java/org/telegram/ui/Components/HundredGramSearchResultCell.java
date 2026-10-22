package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramSettingsSearchIndex;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;

public class HundredGramSearchResultCell extends FrameLayout {

    private final FrameLayout iconBadgeLayout;
    private final ImageView iconView;
    private final TextView categoryTagView;
    private final TextView titleView;
    private final SimpleTextView subtitleView;
    private final ImageView arrowView;
    private final View divider;

    public HundredGramSearchResultCell(Context context) {
        super(context);

        setBackground(Theme.createSelectorWithBackgroundDrawable(Theme.getColor(Theme.key_windowBackgroundWhite), Theme.getColor(Theme.key_listSelector)));

        // Icon badge on left
        iconBadgeLayout = new FrameLayout(context);
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconBadgeLayout.addView(iconView, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        addView(iconBadgeLayout, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.START, 16, 10, 0, 10));

        // Center column for Category tag, Title, Subtitle
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        categoryTagView = new TextView(context);
        categoryTagView.setTextSize(11);
        categoryTagView.setTypeface(AndroidUtilities.bold());
        categoryTagView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
        categoryTagView.setSingleLine(true);
        categoryTagView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(categoryTagView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        titleView = new TextView(context);
        titleView.setTextSize(15);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        subtitleView = new SimpleTextView(context);
        subtitleView.setTextSize(13);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 68, 8, 48, 8));

        // Right arrow
        arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.msg_arrowright);
        arrowView.setColorFilter(new android.graphics.PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), android.graphics.PorterDuff.Mode.MULTIPLY));
        addView(arrowView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 16, 0));

        // Divider
        divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.BOTTOM, 68, 0, 0, 0));
    }

    public void setData(HundredGramSettingsSearchIndex.MatchResult matchResult, boolean needDivider) {
        if (matchResult == null || matchResult.entry == null) return;
        HundredGramSettingsSearchIndex.Entry entry = matchResult.entry;

        int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
        int alphaBg = Color.argb(35, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));

        GradientDrawable badgeDrawable = new GradientDrawable();
        badgeDrawable.setShape(GradientDrawable.RECTANGLE);
        badgeDrawable.setCornerRadius(AndroidUtilities.dp(10));
        badgeDrawable.setColor(alphaBg);
        iconBadgeLayout.setBackground(badgeDrawable);

        iconView.setImageResource(entry.categoryIcon != 0 ? entry.categoryIcon : R.drawable.msg_settings);
        iconView.setColorFilter(new android.graphics.PorterDuffColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN));

        // Category Tag
        categoryTagView.setText(entry.getCategoryTitle().toUpperCase());

        // Highlight matched text in title
        String rawTitle = entry.getTitle();
        if (matchResult.matchTitleStart >= 0 && matchResult.matchTitleEnd <= rawTitle.length() && matchResult.matchTitleStart < matchResult.matchTitleEnd) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(rawTitle);
            ssb.setSpan(new ForegroundColorSpan(accentColor), matchResult.matchTitleStart, matchResult.matchTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD), matchResult.matchTitleStart, matchResult.matchTitleEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            titleView.setText(ssb);
        } else {
            titleView.setText(rawTitle);
        }

        // Subtitle
        String sub = entry.getSubtitle();
        if (!TextUtils.isEmpty(sub)) {
            subtitleView.setVisibility(VISIBLE);
            subtitleView.setText(sub);
        } else {
            subtitleView.setVisibility(GONE);
        }

        divider.setVisibility(needDivider ? VISIBLE : GONE);
    }
}
