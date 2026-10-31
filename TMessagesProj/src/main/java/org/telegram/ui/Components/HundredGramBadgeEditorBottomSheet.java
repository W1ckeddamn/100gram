package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramBadgesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;

/**
 * BottomSheet для создания, редактирования и удаления кастомных бейджей пользователей.
 */
public class HundredGramBadgeEditorBottomSheet extends BottomSheet {

    private final BaseFragment parentFragment;
    private final long initialUserId;
    private final HundredGramBadgesController.Badge existingBadge;

    private EditTextBoldCursor userIdInput;
    private EditTextBoldCursor titleInput;
    private EditTextBoldCursor descInput;

    private int selectedIconType = HundredGramBadgesController.ICON_CROWN;
    private int selectedColor = 0; // 0 = default accent
    private boolean isOfficial = false;

    private static final int[] PRESET_COLORS = new int[]{
            0,                  // Default
            0xFFFFD700,         // Gold
            0xFF3390EC,         // Telegram Blue
            0xFF4CAF50,         // Emerald Green
            0xFFE53935,         // Crimson Red
            0xFF8E24AA,         // Purple
            0xFFFB8C00,         // Amber Orange
            0xFF00ACC1          // Cyan
    };

    private static final int[] ICON_TYPES = new int[]{
            HundredGramBadgesController.ICON_CROWN,
            HundredGramBadgesController.ICON_STAR,
            HundredGramBadgesController.ICON_SHIELD,
            HundredGramBadgesController.ICON_VERIFIED,
            HundredGramBadgesController.ICON_FIRE,
            HundredGramBadgesController.ICON_DEV,
            HundredGramBadgesController.ICON_DIAMOND,
            HundredGramBadgesController.ICON_ROCKET,
            HundredGramBadgesController.ICON_TROPHY,
            HundredGramBadgesController.ICON_HEART,
            HundredGramBadgesController.ICON_TARGET,
            HundredGramBadgesController.ICON_COFFEE,
            HundredGramBadgesController.ICON_GAMEPAD,
            HundredGramBadgesController.ICON_SKULL,
            HundredGramBadgesController.ICON_WAND,
            HundredGramBadgesController.ICON_ROBOT,
            HundredGramBadgesController.ICON_GLASSES,
            HundredGramBadgesController.ICON_MUSIC,
            HundredGramBadgesController.ICON_BOLT,
            HundredGramBadgesController.ICON_BOOK,
            HundredGramBadgesController.ICON_CODE,
            HundredGramBadgesController.ICON_GHOST,
            HundredGramBadgesController.ICON_INFINITY,
            HundredGramBadgesController.ICON_PUZZLE
    };

    private final View[] iconViews = new View[ICON_TYPES.length];
    private final View[] colorViews = new View[PRESET_COLORS.length];

    public HundredGramBadgeEditorBottomSheet(BaseFragment fragment, long userId, HundredGramBadgesController.Badge badge) {
        super(fragment.getParentActivity(), false);
        this.parentFragment = fragment;
        this.initialUserId = userId;
        this.existingBadge = badge != null ? badge : HundredGramBadgesController.getInstance().getBadge(userId);

        if (existingBadge != null) {
            this.selectedIconType = existingBadge.iconType;
            this.selectedColor = existingBadge.color;
            this.isOfficial = existingBadge.isOfficial;
        } else {
            this.isOfficial = HundredGramBadgesController.getInstance().isCurrentUserAdmin();
        }

        Context context = getContext();
        setApplyBottomPadding(false);
        fixNavigationBar(getThemedColor(Theme.key_dialogBackground));

        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
        container.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));

        // Drag handle
        View handleView = new View(context);
        handleView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(2), getThemedColor(Theme.key_sheet_scrollUp)));
        container.addView(handleView, LayoutHelper.createLinear(36, 4, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        // Header Title
        TextView headerTitle = new TextView(context);
        headerTitle.setText(existingBadge != null ? LocaleController.getString("CustomBadgeEdit", R.string.CustomBadgeEdit) : LocaleController.getString("CustomBadgeNew", R.string.CustomBadgeNew));
        headerTitle.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        headerTitle.setTypeface(AndroidUtilities.bold());
        container.addView(headerTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // User ID (if new)
        if (initialUserId == 0) {
            container.addView(createSectionHeader(context, LocaleController.getString("CustomBadgeUserId", R.string.CustomBadgeUserId)));
            userIdInput = createInputField(context, "123456789", false);
            userIdInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            container.addView(userIdInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));
        }

        // Badge Title input
        container.addView(createSectionHeader(context, LocaleController.getString("CustomBadgeTitle", R.string.CustomBadgeTitle)));
        titleInput = createInputField(context, LocaleController.getString("CustomBadgeTitleHint", R.string.CustomBadgeTitleHint), false);
        if (existingBadge != null && !TextUtils.isEmpty(existingBadge.title)) {
            titleInput.setText(existingBadge.title);
        }
        container.addView(titleInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // Badge Description input
        container.addView(createSectionHeader(context, LocaleController.getString("CustomBadgeDesc", R.string.CustomBadgeDesc)));
        descInput = createInputField(context, LocaleController.getString("CustomBadgeDescHint", R.string.CustomBadgeDescHint), true);
        if (existingBadge != null && !TextUtils.isEmpty(existingBadge.description)) {
            descInput.setText(existingBadge.description);
        }
        container.addView(descInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Icon Picker
        container.addView(createSectionHeader(context, LocaleController.getString("CustomBadgeIcon", R.string.CustomBadgeIcon)));
        HorizontalScrollView iconScroll = new HorizontalScrollView(context);
        iconScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout iconRow = new LinearLayout(context);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));

        for (int i = 0; i < ICON_TYPES.length; i++) {
            final int type = ICON_TYPES[i];
            final int index = i;
            FrameLayout chip = new FrameLayout(context);
            chip.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(52), AndroidUtilities.dp(52)));
            ((LinearLayout.LayoutParams) chip.getLayoutParams()).setMargins(0, 0, AndroidUtilities.dp(10), 0);

            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.CENTER);
            Drawable d = getPreviewIcon(context, type);
            iv.setImageDrawable(d);
            chip.addView(iv, LayoutHelper.createFrame(32, 32, Gravity.CENTER));

            chip.setOnClickListener(v -> {
                selectedIconType = type;
                updateIconSelection();
            });

            iconViews[i] = chip;
            iconRow.addView(chip);
        }
        updateIconSelection();
        iconScroll.addView(iconRow);
        container.addView(iconScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Color Picker
        container.addView(createSectionHeader(context, LocaleController.getString("CustomBadgeColor", R.string.CustomBadgeColor)));
        HorizontalScrollView colorScroll = new HorizontalScrollView(context);
        colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int color = PRESET_COLORS[i];
            final int index = i;
            FrameLayout colorCircle = new FrameLayout(context);
            colorCircle.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42)));
            ((LinearLayout.LayoutParams) colorCircle.getLayoutParams()).setMargins(0, 0, AndroidUtilities.dp(10), 0);

            View circle = new View(context);
            int displayColor = color != 0 ? color : getThemedColor(Theme.key_dialogTextBlue2);
            circle.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), displayColor));
            colorCircle.addView(circle, LayoutHelper.createFrame(32, 32, Gravity.CENTER));

            ImageView check = new ImageView(context);
            check.setImageResource(R.drawable.verified_check);
            check.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            check.setScaleType(ImageView.ScaleType.FIT_CENTER);
            check.setVisibility(View.GONE);
            colorCircle.addView(check, LayoutHelper.createFrame(18, 18, Gravity.CENTER));

            colorCircle.setOnClickListener(v -> {
                selectedColor = color;
                updateColorSelection();
            });

            colorViews[i] = colorCircle;
            colorRow.addView(colorCircle);
        }
        updateColorSelection();
        colorScroll.addView(colorRow);
        container.addView(colorScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 24));

        if (HundredGramBadgesController.getInstance().isCurrentUserAdmin()) {
            TextCheckCell officialCell = new TextCheckCell(context);
            officialCell.setTextAndCheck(LocaleController.getString("CustomBadgeIsOfficial", R.string.CustomBadgeIsOfficial), isOfficial, false);
            officialCell.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_windowBackgroundGray)));
            officialCell.setOnClickListener(v -> {
                isOfficial = !isOfficial;
                officialCell.setChecked(isOfficial);
            });
            container.addView(officialCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));
        }

        // Save Button
        TextView saveBtn = new TextView(context);
        saveBtn.setText(LocaleController.getString("Save", R.string.Save));
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        saveBtn.setTypeface(AndroidUtilities.bold());
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_featuredStickers_addButton), getThemedColor(Theme.key_featuredStickers_addButtonPressed)));
        saveBtn.setOnClickListener(v -> saveBadge());
        container.addView(saveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 0, 0, 0, 8));

        // Delete button (if existing)
        if (existingBadge != null) {
            TextView deleteBtn = new TextView(context);
            deleteBtn.setText(LocaleController.getString("Delete", R.string.Delete));
            deleteBtn.setTextColor(getThemedColor(Theme.key_text_RedBold));
            deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            deleteBtn.setTypeface(AndroidUtilities.bold());
            deleteBtn.setGravity(Gravity.CENTER);
            deleteBtn.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), 1, AndroidUtilities.dp(8)));
            deleteBtn.setOnClickListener(v -> deleteBadge());
            container.addView(deleteBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }

        scrollView.addView(container);
        setCustomView(scrollView);
    }

    private TextView createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTypeface(AndroidUtilities.bold());
        return tv;
    }

    private EditTextBoldCursor createInputField(Context context, String hint, boolean multiline) {
        EditTextBoldCursor et = new EditTextBoldCursor(context);
        et.setHint(hint);
        et.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        et.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        et.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), getThemedColor(Theme.key_windowBackgroundGray)));
        et.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        et.setCursorColor(getThemedColor(Theme.key_dialogTextBlack));
        if (multiline) {
            et.setMinLines(2);
            et.setMaxLines(4);
            et.setGravity(Gravity.TOP | Gravity.LEFT);
        } else {
            et.setSingleLine(true);
        }
        return et;
    }

    private Drawable getPreviewIcon(Context context, int type) {
        int resId = HundredGramBadgesController.getIconResId(type);
        Drawable d = ContextCompat.getDrawable(context, resId);
        if (d != null) {
            d = d.mutate();
            d.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogTextBlack), PorterDuff.Mode.SRC_IN));
        }
        return d;
    }

    private void updateIconSelection() {
        for (int i = 0; i < ICON_TYPES.length; i++) {
            boolean isSelected = ICON_TYPES[i] == selectedIconType;
            View view = iconViews[i];
            if (view != null) {
                if (isSelected) {
                    GradientDrawable gd = new GradientDrawable();
                    gd.setCornerRadius(AndroidUtilities.dp(12));
                    gd.setColor(Color.argb(35, 51, 144, 236));
                    gd.setStroke(AndroidUtilities.dp(2), getThemedColor(Theme.key_dialogTextBlue2));
                    view.setBackground(gd);
                } else {
                    view.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), getThemedColor(Theme.key_windowBackgroundGray)));
                }
            }
        }
    }

    private void updateColorSelection() {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            boolean isSelected = PRESET_COLORS[i] == selectedColor;
            FrameLayout frame = (FrameLayout) colorViews[i];
            if (frame != null) {
                ImageView check = (ImageView) frame.getChildAt(1);
                if (check != null) {
                    check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }
            }
        }
    }

    private void saveBadge() {
        long targetId = initialUserId;
        if (targetId == 0 && userIdInput != null) {
            String text = userIdInput.getText().toString().trim();
            try {
                targetId = Long.parseLong(text);
            } catch (Exception e) {
                targetId = 0;
            }
        }

        if (targetId == 0) {
            if (parentFragment != null) {
                BulletinFactory.of(parentFragment).createErrorBulletin(LocaleController.getString("CustomBadgeInvalidId", R.string.CustomBadgeInvalidId)).show();
            }
            return;
        }

        String title = titleInput != null ? titleInput.getText().toString().trim() : "";
        String desc = descInput != null ? descInput.getText().toString().trim() : "";

        HundredGramBadgesController.Badge b = new HundredGramBadgesController.Badge(
                targetId,
                title,
                desc,
                selectedIconType,
                0,
                selectedColor,
                isOfficial
        );

        HundredGramBadgesController.getInstance().setBadge(b);
        dismiss();

        if (parentFragment != null) {
            BulletinFactory.of(parentFragment).createSimpleBulletin(
                    R.drawable.verified_check,
                    LocaleController.getString("CustomBadgeSaved", R.string.CustomBadgeSaved)
            ).show();
        }
    }

    private void deleteBadge() {
        long targetId = initialUserId != 0 ? initialUserId : (existingBadge != null ? existingBadge.userId : 0);
        if (targetId != 0) {
            HundredGramBadgesController.getInstance().removeBadge(targetId);
            dismiss();
            if (parentFragment != null) {
                BulletinFactory.of(parentFragment).createSimpleBulletin(
                        R.drawable.msg_delete,
                        LocaleController.getString("CustomBadgeDeleted", R.string.CustomBadgeDeleted)
                ).show();
            }
        }
    }
}
