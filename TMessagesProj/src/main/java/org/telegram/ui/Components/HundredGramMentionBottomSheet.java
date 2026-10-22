package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramMentionController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramMentionBottomSheet extends BottomSheet {

    public interface OnMentionReadyListener {
        void onMentionReady(CharSequence mentions);
    }

    private final int currentAccount;
    private final TLRPC.Chat chat;
    private final TLRPC.ChatFull chatFull;
    private final OnMentionReadyListener listener;

    private int selectedTarget = HundredGramMentionController.TARGET_ALL;
    private int selectedStyle = HundredGramMentionController.STYLE_HIDDEN;

    private ArrayList<TLRPC.User> currentUsers = new ArrayList<>();
    private TextView countTextView;
    private TextView actionButton;
    private TextView limitNoticeTextView;

    private LinearLayout targetContainer;
    private LinearLayout styleContainer;

    public HundredGramMentionBottomSheet(Context context, int currentAccount, TLRPC.Chat chat, TLRPC.ChatFull chatFull, int initialTarget, OnMentionReadyListener listener, Theme.ResourcesProvider resourcesProvider) {
        super(context, true, resourcesProvider);
        this.currentAccount = currentAccount;
        this.chat = chat;
        this.chatFull = chatFull;
        this.selectedTarget = initialTarget;
        this.listener = listener;

        setApplyTopPadding(false);
        setApplyBottomPadding(false);

        NestedScrollView scrollView = new NestedScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(20));

        // Header Title
        TextView titleView = new TextView(context);
        titleView.setText(LocaleController.getString("MentionAllTitle", R.string.MentionAllTitle));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        contentLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 16));

        // Target Section Title
        TextView targetHeader = new TextView(context);
        targetHeader.setText(LocaleController.getString("MentionAllAudienceSection", R.string.MentionAllAudienceSection));
        targetHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        targetHeader.setTypeface(AndroidUtilities.bold());
        targetHeader.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        contentLayout.addView(targetHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Target Options
        targetContainer = new LinearLayout(context);
        targetContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(targetContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Style Section Title
        TextView styleHeader = new TextView(context);
        styleHeader.setText(LocaleController.getString("MentionAllStyleSection", R.string.MentionAllStyleSection));
        styleHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        styleHeader.setTypeface(AndroidUtilities.bold());
        styleHeader.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        contentLayout.addView(styleHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 0, 6));

        // Style Options
        styleContainer = new LinearLayout(context);
        styleContainer.setOrientation(LinearLayout.VERTICAL);
        contentLayout.addView(styleContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Limit / Count Notice
        limitNoticeTextView = new TextView(context);
        limitNoticeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        limitNoticeTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        limitNoticeTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        contentLayout.addView(limitNoticeTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 14));

        // Action Button: Insert into Message
        actionButton = new TextView(context);
        actionButton.setText(LocaleController.getString("MentionAllInsert", R.string.MentionAllInsert));
        actionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        actionButton.setTypeface(AndroidUtilities.bold());
        actionButton.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        actionButton.setGravity(Gravity.CENTER);
        actionButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        actionButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(10), getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButtonPressed)));
        actionButton.setOnClickListener(v -> {
            if (listener != null) {
                CharSequence mentions = HundredGramMentionController.buildMentions(currentUsers, selectedStyle, 50);
                listener.onMentionReady(mentions);
            }
            dismiss();
        });
        contentLayout.addView(actionButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        scrollView.addView(contentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        setCustomView(scrollView);

        renderTargetOptions();
        renderStyleOptions();
        loadParticipants();
    }

    private void renderTargetOptions() {
        targetContainer.removeAllViews();
        Context context = getContext();

        targetContainer.addView(createOptionRow(
                context,
                LocaleController.formatString("MentionAllTargetAll", R.string.MentionAllTargetAll, selectedTarget == HundredGramMentionController.TARGET_ALL ? currentUsers.size() : (chatFull != null ? chatFull.participants_count : 0)),
                LocaleController.getString("MentionAllSub", R.string.MentionAllSub),
                selectedTarget == HundredGramMentionController.TARGET_ALL,
                () -> {
                    selectedTarget = HundredGramMentionController.TARGET_ALL;
                    renderTargetOptions();
                    loadParticipants();
                }
        ));

        targetContainer.addView(createOptionRow(
                context,
                LocaleController.formatString("MentionAllTargetOnline", R.string.MentionAllTargetOnline, selectedTarget == HundredGramMentionController.TARGET_ONLINE ? currentUsers.size() : (chatFull != null ? chatFull.online_count : 0)),
                LocaleController.getString("Online", R.string.Online),
                selectedTarget == HundredGramMentionController.TARGET_ONLINE,
                () -> {
                    selectedTarget = HundredGramMentionController.TARGET_ONLINE;
                    renderTargetOptions();
                    loadParticipants();
                }
        ));

        targetContainer.addView(createOptionRow(
                context,
                LocaleController.formatString("MentionAllTargetAdmins", R.string.MentionAllTargetAdmins, selectedTarget == HundredGramMentionController.TARGET_ADMINS ? currentUsers.size() : (chatFull != null ? chatFull.admins_count : 0)),
                LocaleController.getString("MentionAdminsSub", R.string.MentionAdminsSub),
                selectedTarget == HundredGramMentionController.TARGET_ADMINS,
                () -> {
                    selectedTarget = HundredGramMentionController.TARGET_ADMINS;
                    renderTargetOptions();
                    loadParticipants();
                }
        ));
    }

    private void renderStyleOptions() {
        styleContainer.removeAllViews();
        Context context = getContext();

        styleContainer.addView(createOptionRow(
                context,
                LocaleController.getString("MentionAllStyleHidden", R.string.MentionAllStyleHidden),
                LocaleController.getString("MentionAllStyleHiddenDesc", R.string.MentionAllStyleHiddenDesc),
                selectedStyle == HundredGramMentionController.STYLE_HIDDEN,
                () -> {
                    selectedStyle = HundredGramMentionController.STYLE_HIDDEN;
                    renderStyleOptions();
                }
        ));

        styleContainer.addView(createOptionRow(
                context,
                LocaleController.getString("MentionAllStyleVisible", R.string.MentionAllStyleVisible),
                LocaleController.getString("MentionAllStyleVisibleDesc", R.string.MentionAllStyleVisibleDesc),
                selectedStyle == HundredGramMentionController.STYLE_VISIBLE,
                () -> {
                    selectedStyle = HundredGramMentionController.STYLE_VISIBLE;
                    renderStyleOptions();
                }
        ));
    }

    private View createOptionRow(Context context, String title, String subtitle, boolean isSelected, Runnable onClick) {
        FrameLayout row = new FrameLayout(context);
        row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        int bgColor = isSelected ? Theme.multAlpha(getThemedColor(Theme.key_featuredStickers_addButton), 0.12f) : Theme.multAlpha(getThemedColor(Theme.key_listSelector), 0.25f);
        row.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), bgColor, Theme.multAlpha(getThemedColor(Theme.key_listSelector), 0.5f)));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(isSelected ? getThemedColor(Theme.key_windowBackgroundWhiteBlueText) : getThemedColor(Theme.key_dialogTextBlack));
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (subtitle != null) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            textLayout.addView(subView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        row.addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, 0, 36, 0));

        if (isSelected) {
            ImageView check = new ImageView(context);
            check.setImageResource(R.drawable.ic_ab_done);
            check.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_featuredStickers_addButton), PorterDuff.Mode.MULTIPLY));
            row.addView(check, LayoutHelper.createFrame(20, 20, Gravity.CENTER_VERTICAL | Gravity.RIGHT));
        }

        row.setOnClickListener(v -> onClick.run());
        return row;
    }

    private void loadParticipants() {
        actionButton.setEnabled(false);
        actionButton.setAlpha(0.5f);
        limitNoticeTextView.setText(LocaleController.getString("Loading", R.string.Loading));

        HundredGramMentionController.loadGroupParticipants(currentAccount, chat, chatFull, selectedTarget, users -> {
            currentUsers.clear();
            if (users != null) {
                currentUsers.addAll(users);
            }

            int count = currentUsers.size();
            renderTargetOptions();

            if (count == 0) {
                limitNoticeTextView.setText(LocaleController.getString("MentionAllNoMembers", R.string.MentionAllNoMembers));
                actionButton.setEnabled(false);
                actionButton.setAlpha(0.5f);
            } else {
                if (count > 50) {
                    limitNoticeTextView.setText(LocaleController.formatString("MentionAllBatchesNotice", R.string.MentionAllBatchesNotice, (int) Math.ceil(count / 50.0)) + " (max 50 per msg)");
                } else {
                    limitNoticeTextView.setText("");
                }
                actionButton.setEnabled(true);
                actionButton.setAlpha(1.0f);
            }
        });
    }
}
