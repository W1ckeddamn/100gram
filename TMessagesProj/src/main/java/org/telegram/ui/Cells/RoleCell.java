package org.telegram.ui.Cells;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ai.Role;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadioButton;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

@SuppressLint({"ViewConstructor"})
public class RoleCell extends FrameLayout {

    private final int currentAccount;
    private final BackupImageView emojiView;
    private boolean needDivider;
    private final RadioButton radioButton;
    private final TextView subtitleView;
    private final TextView titleView;

    public RoleCell(Context context, int account, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.currentAccount = account;
        setWillNotDraw(false);

        emojiView = new BackupImageView(context);
        addView(emojiView, LayoutHelper.createFrame(32, 32.0f, 19, 18.0f, 0.0f, 0.0f, 0.0f));

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 19, 68.0f, 0.0f, 62.0f, 0.0f));

        titleView = new TextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        titleView.setTextSize(1, 16.0f);
        titleView.setSingleLine(true);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setGravity(3);
        titleView.setIncludeFontPadding(false);
        textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        subtitleView.setTextSize(1, 13.0f);
        subtitleView.setSingleLine(true);
        subtitleView.setMaxLines(1);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setGravity(3);
        subtitleView.setIncludeFontPadding(false);
        textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0.0f, 4.0f, 0.0f, 0.0f));

        radioButton = new RadioButton(context);
        radioButton.setSize(AndroidUtilities.dp(20.0f));
        radioButton.setColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_radioBackgroundChecked));
        addView(radioButton, LayoutHelper.createFrame(48, 48.0f, 21, 0.0f, 0.0f, 8.0f, 0.0f));
    }

    public RadioButton getRadioButton() {
        return radioButton;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(62.0f), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(AndroidUtilities.dp(68.0f), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    public void set(Role role, boolean checked, boolean divider) {
        this.needDivider = divider;
        this.titleView.setText(role.getName());
        this.subtitleView.setText(role.getPrompt());
        this.subtitleView.setVisibility(View.VISIBLE);
        this.radioButton.setChecked(checked, true);
        this.emojiView.setAnimatedEmojiDrawable(new AnimatedEmojiDrawable(3, this.currentAccount, role.getEmojiId() != 0 ? role.getEmojiId() : 5359441070201513074L));
    }

    public static class Factory extends UItem.UItemFactory<RoleCell> {
        static {
            UItem.UItemFactory.setup(new Factory());
        }

        @Override
        public RoleCell createView(Context context, RecyclerListView recyclerListView, int currentAccount, int viewType, Theme.ResourcesProvider resourcesProvider) {
            return new RoleCell(context, currentAccount, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView recyclerView) {
            if (view instanceof RoleCell) {
                RoleCell roleCell = (RoleCell) view;
                if (item.object instanceof Role) {
                    roleCell.set((Role) item.object, item.checked, divider);
                    roleCell.getRadioButton().setOnClickListener(item.clickCallback);
                }
            }
        }

        @Override
        public boolean contentsEquals(UItem item1, UItem item2) {
            if (item1.object instanceof Role && item2.object instanceof Role) {
                Role r1 = (Role) item1.object;
                Role r2 = (Role) item2.object;
                return item1.checked == item2.checked &&
                        TextUtils.equals(r1.getName(), r2.getName()) &&
                        TextUtils.equals(r1.getPrompt(), r2.getPrompt()) &&
                        r1.getEmojiId() == r2.getEmojiId();
            }
            return false;
        }

        public static UItem asRoleCell(Role role, View.OnClickListener onRadioClick) {
            UItem item = UItem.ofFactory(Factory.class);
            item.object = role;
            item.checked = role.isSelected();
            item.clickCallback = onRadioClick;
            return item;
        }
    }
}
