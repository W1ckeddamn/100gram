package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramActivitySimulator;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.HundredGramAiSettingsActivity;
import org.telegram.ui.HundredGramPluginsActivity;
import org.telegram.ui.HundredGramSubSettingsActivity;

@SuppressLint("ViewConstructor")
public class HundredGramSettingsHeroCell extends FrameLayout {

    private final BackupImageView avatarImageView;
    private final AvatarDrawable avatarDrawable;
    private final TextView nameView;
    private final TextView usernameView;
    private final TextView versionBadge;
    private final LinearLayout chipsContainer;
    private final BaseFragment parentFragment;

    public HundredGramSettingsHeroCell(Context context, BaseFragment fragment) {
        super(context);
        this.parentFragment = fragment;

        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        // Card container
        FrameLayout card = new FrameLayout(context);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(AndroidUtilities.dp(16));
        boolean isDark = Theme.isCurrentThemeDark();
        cardBg.setColor(isDark ? 0xFF1C2733 : 0xFFF0F5FA);
        cardBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x22FFFFFF : 0x1A000000);
        card.setBackground(cardBg);
        addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 48, 14, 10, 14, 10));

        // Top section: Avatar + Name + Username + Version
        FrameLayout topSection = new FrameLayout(context);
        card.addView(topSection, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 48, 14, 14, 14, 0));

        // Avatar
        avatarDrawable = new AvatarDrawable();
        avatarImageView = new BackupImageView(context);
        float cornerRadius = HundredGramConfig.getAvatarCorners(56.0f);
        avatarImageView.setRoundRadius((int) cornerRadius);
        topSection.addView(avatarImageView, LayoutHelper.createFrame(56, 56, Gravity.START | Gravity.CENTER_VERTICAL));

        // Texts layout
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);

        nameView = new TextView(context);
        nameView.setTextSize(17);
        nameView.setTypeface(AndroidUtilities.bold());
        nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameView.setSingleLine(true);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(nameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        usernameView = new TextView(context);
        usernameView.setTextSize(13.5f);
        usernameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
        usernameView.setSingleLine(true);
        usernameView.setEllipsize(TextUtils.TruncateAt.END);
        textLayout.addView(usernameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        LinearLayout badgeRow = new LinearLayout(context);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        versionBadge = new TextView(context);
        versionBadge.setText("100gram v1.2 • Graher Edition");
        versionBadge.setTextSize(11);
        versionBadge.setTypeface(AndroidUtilities.bold());
        versionBadge.setTextColor(isDark ? 0xFF64B5F6 : 0xFF1976D2);
        badgeRow.addView(versionBadge);

        ImageView checkIcon = new ImageView(context);
        checkIcon.setImageResource(R.drawable.msg_check_s);
        checkIcon.setColorFilter(isDark ? 0xFF64B5F6 : 0xFF1976D2);
        badgeRow.addView(checkIcon, LayoutHelper.createLinear(13, 13, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

        textLayout.addView(badgeRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        topSection.addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.START, 70, 0, 0, 0));

        // Bottom section: Horizontal scrollable status pills
        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);

        chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        scrollView.addView(chipsContainer, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16));

        card.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 48, 0, 76, 0, 0));

        updateUserData();
        updateStatusChips();
    }

    public void update() {
        updateUserData();
        updateStatusChips();
    }

    private void updateUserData() {
        int currentAccount = parentFragment != null ? parentFragment.getCurrentAccount() : UserConfig.selectedAccount;
        TLRPC.User currentUser = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (currentUser != null) {
            nameView.setText(UserObject.getUserName(currentUser));
            if (!TextUtils.isEmpty(currentUser.username)) {
                usernameView.setText("@" + currentUser.username);
            } else if (!TextUtils.isEmpty(currentUser.phone)) {
                usernameView.setText("+" + currentUser.phone);
            } else {
                usernameView.setText(LocaleController.getString("HundredGramSettings", R.string.HundredGramSettings));
            }
            avatarDrawable.setInfo(currentUser);
            avatarImageView.setForUserOrChat(currentUser, avatarDrawable);
        } else {
            nameView.setText("100gram");
            usernameView.setText("@w1ckeddamn");
            avatarDrawable.setInfo(5, "100", "gram");
            avatarImageView.setImageDrawable(avatarDrawable);
        }
        avatarImageView.setRoundRadius((int) HundredGramConfig.getAvatarCorners(56.0f));
    }

    private void updateStatusChips() {
        chipsContainer.removeAllViews();

        // 1. AI Chip
        boolean aiOn = org.telegram.messenger.ai.HundredGramAiConfig.isAiEnabled();
        String aiText = aiOn ? "AI: " + org.telegram.messenger.ai.HundredGramAiConfig.getSelectedService().getName() : "AI: Off";
        int aiColor = aiOn ? 0xFF00C853 : 0xFF757575;
        addStatusChip(R.drawable.msg_bot, aiText, aiColor, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new HundredGramAiSettingsActivity());
            }
        });

        // 2. Vault Chip
        String vaultText = "Vault: " + (HundredGramConfig.autoDownloadMediaVault ? "Active" : "Ready");
        addStatusChip(R.drawable.msg_download, vaultText, 0xFF00B0FF, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_VAULT));
            }
        });

        // 3. Ghost Chip
        int ghostCount = 0;
        if (HundredGramConfig.isGhostModeActive()) ghostCount++;
        if (HundredGramConfig.ghostTyping) ghostCount++;
        if (HundredGramConfig.ghostStoriesMode != HundredGramConfig.GHOST_STORIES_OFF) ghostCount++;
        if (HundredGramConfig.ghostHideReadDate) ghostCount++;
        String ghostText = ghostCount > 0 ? "Ghost: " + ghostCount + " masks" : "Ghost: Off";
        int ghostColor = ghostCount > 0 ? 0xFFAA00FF : 0xFF757575;
        addStatusChip(R.drawable.msg_secret, ghostText, ghostColor, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new HundredGramSubSettingsActivity(HundredGramSubSettingsActivity.CATEGORY_GHOST));
            }
        });

        // 4. Plugins Chip
        int pluginsCount = org.telegram.messenger.plugins.PluginsController.getInstance().getPlugins().size();
        String pluginsText = "Plugins: " + (HundredGramConfig.pluginsEngine ? pluginsCount : "Off");
        int pluginsColor = HundredGramConfig.pluginsEngine ? 0xFFFF9100 : 0xFF757575;
        addStatusChip(R.drawable.msg_bot, pluginsText, pluginsColor, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new HundredGramPluginsActivity());
            }
        });

        // 5. Simulator Chip
        int account = parentFragment != null ? parentFragment.getCurrentAccount() : UserConfig.selectedAccount;
        boolean simRunning = HundredGramActivitySimulator.getInstance(account).isRunning();
        String simText = simRunning ? "Simulator: Online" : (HundredGramConfig.activitySimulatorScheduleEnabled ? "Simulator: Sched" : "Simulator: Off");
        int simColor = simRunning ? 0xFF00C853 : (HundredGramConfig.activitySimulatorScheduleEnabled ? 0xFF29B6F6 : 0xFF757575);
        addStatusChip(R.drawable.msg_usersearch, simText, simColor, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new org.telegram.ui.HundredGramActivitySimulatorActivity());
            }
        });

        // 6. Network Boost Chip
        boolean netBoost = HundredGramConfig.downloadBoost != HundredGramConfig.DOWNLOAD_BOOST_OFF;
        String netText = "Boost: " + (HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_ULTRA ? "Ultra" : (HundredGramConfig.downloadBoost == HundredGramConfig.DOWNLOAD_BOOST_FAST ? "Fast" : "Off"));
        int netColor = netBoost ? 0xFF00E676 : 0xFF757575;
        addStatusChip(R.drawable.msg_stats, netText, netColor, v -> {
            if (parentFragment != null) {
                parentFragment.presentFragment(new org.telegram.ui.HundredGramNetworkBoostActivity());
            }
        });
    }

    private void addStatusChip(int iconRes, String text, int color, OnClickListener onClickListener) {
        LinearLayout chip = new LinearLayout(getContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(14));
        boolean isDark = Theme.isCurrentThemeDark();
        bg.setColor(isDark ? 0x22FFFFFF : 0x14000000);
        bg.setStroke(AndroidUtilities.dp(1), Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
        chip.setBackground(bg);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(color);
        chip.addView(icon, LayoutHelper.createLinear(14, 14, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        chip.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        chip.setOnClickListener(onClickListener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = AndroidUtilities.dp(8);
        chipsContainer.addView(chip, lp);
    }
}
