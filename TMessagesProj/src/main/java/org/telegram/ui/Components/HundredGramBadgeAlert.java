package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramBadgesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

/**
 * BottomSheet для просмотра информации о кастомном бейдже пользователя:
 * отображает большую иконку, название бейджа, подробное описание и информацию о владельце.
 */
public class HundredGramBadgeAlert extends BottomSheet {

    private final BaseFragment parentFragment;
    private final long userId;
    private final HundredGramBadgesController.Badge badge;

    public static void showAlert(BaseFragment fragment, long userId) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        HundredGramBadgesController.Badge badge = HundredGramBadgesController.getInstance().getBadge(userId);
        if (badge == null) {
            return;
        }
        new HundredGramBadgeAlert(fragment, userId, badge).show();
    }

    public HundredGramBadgeAlert(BaseFragment fragment, long userId, HundredGramBadgesController.Badge badge) {
        super(fragment.getParentActivity(), false);
        this.parentFragment = fragment;
        this.userId = userId;
        this.badge = badge;

        Context context = getContext();
        setApplyBottomPadding(false);
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        container.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));

        // Drag handle
        View handleView = new View(context);
        handleView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), getThemedColor(Theme.key_sheet_scrollUp)));
        container.addView(handleView, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Close button (top right)
        FrameLayout topBar = new FrameLayout(context);
        ImageView closeBtn = new ImageView(context);
        closeBtn.setImageResource(R.drawable.msg_close);
        closeBtn.setScaleType(ImageView.ScaleType.CENTER);
        closeBtn.setColorFilter(getThemedColor(Theme.key_dialogIcon));
        closeBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(16)));
        closeBtn.setOnClickListener(v -> dismiss());
        topBar.addView(closeBtn, LayoutHelper.createFrame(36, 36, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        container.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        // Badge Icon Container
        FrameLayout iconContainer = new FrameLayout(context);
        int tintColor = badge.color != 0 ? badge.color : getThemedColor(Theme.key_dialogTextBlue2);
        int bgAlphaColor = Color.argb(40, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor));
        iconContainer.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(36), bgAlphaColor));

        if (badge.iconType == HundredGramBadgesController.ICON_CUSTOM_EMOJI && badge.customEmojiDocumentId != 0) {
            BackupImageView emojiView = new BackupImageView(context);
            AnimatedEmojiDrawable animatedEmoji = AnimatedEmojiDrawable.make(currentAccount, AnimatedEmojiDrawable.getCacheTypeForEnterView(), badge.customEmojiDocumentId);
            emojiView.setImageDrawable(animatedEmoji);
            iconContainer.addView(emojiView, LayoutHelper.createFrame(44, 44, Gravity.CENTER));
        } else {
            ImageView iconView = new ImageView(context);
            Drawable d = HundredGramBadgesController.getInstance().getBadgeDrawable(badge, tintColor);
            iconView.setImageDrawable(d);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconContainer.addView(iconView, LayoutHelper.createFrame(40, 40, Gravity.CENTER));
        }
        container.addView(iconContainer, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        // Badge Title
        TextView titleView = new TextView(context);
        titleView.setText(!TextUtils.isEmpty(badge.title) ? badge.title : LocaleController.getString("CustomBadge", R.string.CustomBadge));
        titleView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        // Status tag (Official vs Personal)
        TextView tagView = new TextView(context);
        if (badge.isOfficial) {
            tagView.setText("✓ " + LocaleController.getString("CustomBadgeOfficialTag", R.string.CustomBadgeOfficialTag));
            tagView.setTextColor(0xFF3390EC);
            tagView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), 0x223390EC));
        } else {
            tagView.setText(LocaleController.getString("CustomBadgePersonalTag", R.string.CustomBadgePersonalTag));
            tagView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            tagView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundGray)));
        }
        tagView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tagView.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        tagView.setTypeface(AndroidUtilities.bold());
        container.addView(tagView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        // Badge Description
        if (!TextUtils.isEmpty(badge.description)) {
            TextView descView = new TextView(context);
            descView.setText(badge.description);
            descView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            descView.setGravity(Gravity.CENTER);
            descView.setLineSpacing(AndroidUtilities.dp(3), 1.15f);
            container.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 16));
        } else {
            TextView descView = new TextView(context);
            descView.setText(LocaleController.getString("CustomBadgeDefaultDesc", R.string.CustomBadgeDefaultDesc));
            descView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            descView.setGravity(Gravity.CENTER);
            container.addView(descView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 0, 12, 16));
        }

        // User info card
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        LinearLayout userCard = new LinearLayout(context);
        userCard.setOrientation(LinearLayout.HORIZONTAL);
        userCard.setGravity(Gravity.CENTER_VERTICAL);
        userCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundGray)));
        userCard.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        BackupImageView avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(20));
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
            avatarImageView.setForUserOrChat(user, avatarDrawable);
        } else {
            avatarDrawable.setInfo(userId, "User", null);
            avatarImageView.setImageDrawable(avatarDrawable);
        }
        userCard.addView(avatarImageView, LayoutHelper.createLinear(40, 40, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout userTextLayout = new LinearLayout(context);
        userTextLayout.setOrientation(LinearLayout.VERTICAL);

        TextView userNameView = new TextView(context);
        userNameView.setText(user != null ? UserObject.getUserName(user) : ("ID: " + userId));
        userNameView.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        userNameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        userNameView.setTypeface(AndroidUtilities.bold());
        userNameView.setSingleLine(true);
        userNameView.setEllipsize(TextUtils.TruncateAt.END);
        userTextLayout.addView(userNameView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView userSubView = new TextView(context);
        String sub = user != null && !TextUtils.isEmpty(user.username) ? ("@" + user.username + " • ID: " + userId) : ("ID: " + userId);
        userSubView.setText(sub);
        userSubView.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
        userSubView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        userSubView.setSingleLine(true);
        userSubView.setEllipsize(TextUtils.TruncateAt.END);
        userTextLayout.addView(userSubView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        userCard.addView(userTextLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        container.addView(userCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Edit button
        boolean canEdit = HundredGramBadgesController.getInstance().isCurrentUserAdmin() || !badge.isOfficial;
        if (canEdit) {
            TextView editBtn = new TextView(context);
            editBtn.setText(LocaleController.getString("CustomBadgeEdit", R.string.CustomBadgeEdit));
            editBtn.setTextColor(Color.WHITE);
            editBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            editBtn.setTypeface(AndroidUtilities.bold());
            editBtn.setGravity(Gravity.CENTER);
            editBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButtonPressed)));
            editBtn.setOnClickListener(v -> {
                dismiss();
                new HundredGramBadgeEditorBottomSheet(parentFragment, userId, badge).show();
            });
            container.addView(editBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 8));
        }

        setCustomView(container);
    }
}
