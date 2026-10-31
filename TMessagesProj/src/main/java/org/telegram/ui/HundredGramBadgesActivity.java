package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramBadgesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.HundredGramBadgeAlert;
import org.telegram.ui.Components.HundredGramBadgeEditorBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран управления кастомными бейджами пользователей.
 * Позволяет просматривать список выданных бейджей, выдавать новые и настраивать их.
 */
public class HundredGramBadgesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private final List<HundredGramBadgesController.Badge> badgeList = new ArrayList<>();

    private int rowCount;
    private int infoRow;
    private int assignSelfRow;
    private int addBadgeRow;
    private int badgesHeaderRow;
    private int badgesStartRow;
    private int badgesEndRow;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.updateInterfaces);
        loadData();
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    private void loadData() {
        badgeList.clear();
        badgeList.addAll(HundredGramBadgesController.getInstance().getAllBadges());
        updateRows();
    }

    private void updateRows() {
        rowCount = 0;
        infoRow = rowCount++;
        assignSelfRow = rowCount++;
        addBadgeRow = rowCount++;

        if (!badgeList.isEmpty()) {
            badgesHeaderRow = rowCount++;
            badgesStartRow = rowCount;
            rowCount += badgeList.size();
            badgesEndRow = rowCount;
        } else {
            badgesHeaderRow = -1;
            badgesStartRow = -1;
            badgesEndRow = -1;
        }

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("CustomBadges", R.string.CustomBadges));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position) -> {
            if (position == assignSelfRow) {
                long myId = UserConfig.getInstance(currentAccount).getClientUserId();
                HundredGramBadgesController.Badge b = HundredGramBadgesController.getInstance().getBadge(myId);
                if (b == null) {
                    b = new HundredGramBadgesController.Badge(
                            myId,
                            LocaleController.getString("CustomBadgeDevTitle", R.string.CustomBadgeDevTitle),
                            LocaleController.getString("CustomBadgeDevDesc", R.string.CustomBadgeDevDesc),
                            HundredGramBadgesController.ICON_CROWN,
                            0,
                            0xFFFFD700
                    );
                    HundredGramBadgesController.getInstance().setBadge(b);
                    loadData();
                } else {
                    new HundredGramBadgeAlert(HundredGramBadgesActivity.this, myId, b).show();
                }
            } else if (position == addBadgeRow) {
                new HundredGramBadgeEditorBottomSheet(HundredGramBadgesActivity.this, 0, null).show();
            } else if (position >= badgesStartRow && position < badgesEndRow) {
                int index = position - badgesStartRow;
                if (index >= 0 && index < badgeList.size()) {
                    HundredGramBadgesController.Badge b = badgeList.get(index);
                    new HundredGramBadgeAlert(HundredGramBadgesActivity.this, b.userId, b).show();
                }
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            loadData();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == assignSelfRow || pos == addBadgeRow || (pos >= badgesStartRow && pos < badgesEndRow);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == infoRow) {
                return 0; // Info
            } else if (position == assignSelfRow || position == addBadgeRow) {
                return 1; // Action button row
            } else if (position == badgesHeaderRow) {
                return 2; // Header
            } else {
                return 3; // Badge item row
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0: { // Info text
                    TextView tv = new TextView(mContext);
                    tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(12));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
                    tv.setText(LocaleController.getString("CustomBadgesInfo", R.string.CustomBadgesInfo));
                    view = tv;
                    break;
                }
                case 1: { // Action row
                    FrameLayout container = new FrameLayout(mContext);
                    container.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    container.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(50)));

                    LinearLayout row = new LinearLayout(mContext);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);

                    ImageView icon = new ImageView(mContext);
                    icon.setScaleType(ImageView.ScaleType.CENTER);
                    row.addView(icon, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

                    TextView text = new TextView(mContext);
                    text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
                    row.addView(text, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    container.addView(row, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                    view = container;
                    break;
                }
                case 2: { // Header
                    TextView tv = new TextView(mContext);
                    tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    tv.setTypeface(AndroidUtilities.bold());
                    tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                    tv.setText(LocaleController.getString("CustomBadgesAssigned", R.string.CustomBadgesAssigned));
                    view = tv;
                    break;
                }
                case 3:
                default: { // Badge Item Cell
                    LinearLayout item = new LinearLayout(mContext);
                    item.setOrientation(LinearLayout.HORIZONTAL);
                    item.setGravity(Gravity.CENTER_VERTICAL);
                    item.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    item.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
                    item.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    AvatarDrawable avatarDrawable = new AvatarDrawable();
                    BackupImageView avatar = new BackupImageView(mContext);
                    avatar.setRoundRadius(AndroidUtilities.dp(22));
                    item.addView(avatar, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));

                    LinearLayout textLayout = new LinearLayout(mContext);
                    textLayout.setOrientation(LinearLayout.VERTICAL);

                    LinearLayout titleRow = new LinearLayout(mContext);
                    titleRow.setOrientation(LinearLayout.HORIZONTAL);
                    titleRow.setGravity(Gravity.CENTER_VERTICAL);

                    TextView name = new TextView(mContext);
                    name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    name.setTypeface(AndroidUtilities.bold());
                    name.setSingleLine(true);
                    name.setEllipsize(TextUtils.TruncateAt.END);
                    titleRow.addView(name, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 6, 0));

                    ImageView badgeIcon = new ImageView(mContext);
                    badgeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    titleRow.addView(badgeIcon, LayoutHelper.createLinear(18, 18));

                    textLayout.addView(titleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    TextView subtitle = new TextView(mContext);
                    subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                    subtitle.setSingleLine(true);
                    subtitle.setEllipsize(TextUtils.TruncateAt.END);
                    textLayout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

                    item.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

                    ImageView chevron = new ImageView(mContext);
                    chevron.setImageResource(R.drawable.arrow_more);
                    chevron.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN));
                    item.addView(chevron, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

                    view = item;
                    break;
                }
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == 1) {
                FrameLayout container = (FrameLayout) holder.itemView;
                LinearLayout row = (LinearLayout) container.getChildAt(0);
                ImageView icon = (ImageView) row.getChildAt(0);
                TextView text = (TextView) row.getChildAt(1);

                if (position == assignSelfRow) {
                    icon.setImageResource(R.drawable.filled_rating_crown);
                    icon.setColorFilter(new PorterDuffColorFilter(0xFFFFD700, PorterDuff.Mode.SRC_IN));
                    long myId = UserConfig.getInstance(currentAccount).getClientUserId();
                    boolean hasSelf = HundredGramBadgesController.getInstance().hasBadge(myId);
                    text.setText(hasSelf ? LocaleController.getString("CustomBadgeMyBadge", R.string.CustomBadgeMyBadge) : LocaleController.getString("CustomBadgeAssignSelf", R.string.CustomBadgeAssignSelf));
                } else if (position == addBadgeRow) {
                    icon.setImageResource(R.drawable.msg_add);
                    icon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2), PorterDuff.Mode.SRC_IN));
                    text.setText(LocaleController.getString("CustomBadgeAdd", R.string.CustomBadgeAdd));
                }
            } else if (viewType == 3) {
                int index = position - badgesStartRow;
                if (index >= 0 && index < badgeList.size()) {
                    HundredGramBadgesController.Badge b = badgeList.get(index);
                    LinearLayout item = (LinearLayout) holder.itemView;
                    BackupImageView avatar = (BackupImageView) item.getChildAt(0);
                    LinearLayout textLayout = (LinearLayout) item.getChildAt(1);
                    LinearLayout titleRow = (LinearLayout) textLayout.getChildAt(0);
                    TextView name = (TextView) titleRow.getChildAt(0);
                    ImageView badgeIcon = (ImageView) titleRow.getChildAt(1);
                    TextView subtitle = (TextView) textLayout.getChildAt(1);

                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(b.userId);
                    AvatarDrawable avatarDrawable = new AvatarDrawable();
                    if (user != null) {
                        avatarDrawable.setInfo(currentAccount, user);
                        avatar.setForUserOrChat(user, avatarDrawable);
                        name.setText(UserObject.getUserName(user));
                    } else {
                        avatarDrawable.setInfo(b.userId, "User", null);
                        avatar.setImageDrawable(avatarDrawable);
                        name.setText("ID: " + b.userId);
                    }

                    Drawable d = HundredGramBadgesController.getInstance().getBadgeDrawable(b, 0);
                    badgeIcon.setImageDrawable(d);

                    String sub = !TextUtils.isEmpty(b.title) ? b.title : ("ID: " + b.userId);
                    if (!TextUtils.isEmpty(b.description)) {
                        sub += " — " + b.description;
                    }
                    subtitle.setText(sub);
                }
            }
        }
    }
}
