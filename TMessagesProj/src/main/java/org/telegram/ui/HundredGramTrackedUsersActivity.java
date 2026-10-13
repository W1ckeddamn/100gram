package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.HundredGramActivityTracker;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.HundredGramUserActivityBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Set;

public class HundredGramTrackedUsersActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_ADD = 1;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private LinearLayout emptyView;
    private ImageView floatingButton;

    private final ArrayList<Long> userIds = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
        loadUsers();
        HundredGramActivityTracker.getInstance(currentAccount).requestTrackedUserStatuses(true);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void loadUsers() {
        userIds.clear();
        Set<Long> tracked = HundredGramConfig.getTrackedOnlineUsers();
        if (tracked != null) {
            userIds.addAll(tracked);
        }
        updateSubtitle();
        updateEmptyView();
    }

    private void updateSubtitle() {
        if (actionBar != null) {
            int count = userIds.size();
            String subtitle = String.format(LocaleController.getString("HundredGramTrackedUsersCount", R.string.HundredGramTrackedUsersCount), count);
            actionBar.setSubtitle(subtitle);
        }
    }

    private void updateEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(userIds.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramTrackedUsersList", R.string.HundredGramTrackedUsersList));
        updateSubtitle();

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == ITEM_ADD) {
                    openUserPicker();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(ITEM_ADD, R.drawable.msg_add);

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider()));

        listView = new RecyclerListView(context, getResourceProvider());
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= userIds.size()) return;
            long uid = userIds.get(position);
            new HundredGramUserActivityBottomSheet(HundredGramTrackedUsersActivity.this, uid).show();
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= userIds.size()) return false;
            long uid = userIds.get(position);
            showUserOptionsDialog(uid);
            return true;
        });

        // Empty state view
        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);

        ImageView emptyIcon = new ImageView(context);
        emptyIcon.setImageResource(R.drawable.msg_usersearch);
        emptyIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_emptyListPlaceholder, getResourceProvider()), PorterDuff.Mode.MULTIPLY));
        emptyView.addView(emptyIcon, LayoutHelper.createLinear(64, 64, Gravity.CENTER, 0, 0, 0, 16));

        TextView emptyTitle = new TextView(context);
        emptyTitle.setText(LocaleController.getString("HundredGramTrackedUsersEmpty", R.string.HundredGramTrackedUsersEmpty));
        emptyTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        emptyTitle.setTextSize(18);
        emptyTitle.setTypeface(AndroidUtilities.bold());
        emptyTitle.setGravity(Gravity.CENTER);
        emptyView.addView(emptyTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 8));

        TextView emptySub = new TextView(context);
        emptySub.setText(LocaleController.getString("HundredGramTrackOnlineStatusInfo", R.string.HundredGramTrackOnlineStatusInfo));
        emptySub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
        emptySub.setTextSize(14);
        emptySub.setGravity(Gravity.CENTER);
        emptyView.addView(emptySub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 24));

        TextView addButton = new TextView(context);
        addButton.setText(LocaleController.getString("HundredGramAddTrackedUser", R.string.HundredGramAddTrackedUser));
        addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, getResourceProvider()));
        addButton.setTextSize(15);
        addButton.setTypeface(AndroidUtilities.bold());
        addButton.setGravity(Gravity.CENTER);
        addButton.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        addButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed, getResourceProvider())));
        addButton.setOnClickListener(v -> openUserPicker());
        emptyView.addView(addButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        // Floating action button (+)
        floatingButton = new ImageView(context);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        Drawable fabDrawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56),
                Theme.getColor(Theme.key_chats_actionBackground, getResourceProvider()),
                Theme.getColor(Theme.key_chats_actionPressedBackground, getResourceProvider()));
        floatingButton.setBackground(fabDrawable);
        floatingButton.setImageResource(R.drawable.msg_add);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon, getResourceProvider()), PorterDuff.Mode.MULTIPLY));
        floatingButton.setElevation(AndroidUtilities.dp(6));
        floatingButton.setOnClickListener(v -> openUserPicker());
        frameLayout.addView(floatingButton, LayoutHelper.createFrame(56, 56, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, 16, 16, 16, 16));

        updateEmptyView();
        return fragmentView;
    }

    private void updateRows() {
        loadUsers();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void openUserPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowMultiple", true);
        args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_USERS_ONLY);
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((dialogsActivity, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null && !dids.isEmpty()) {
                int addedCount = 0;
                long lastAddedDid = 0;
                for (MessagesStorage.TopicKey topicKey : dids) {
                    long did = topicKey.dialogId;
                    if (did > 0 && did != UserConfig.getInstance(currentAccount).getClientUserId()) {
                        HundredGramConfig.setTrackedUser(did, true);
                        lastAddedDid = did;
                        addedCount++;
                    }
                }
                if (addedCount > 0) {
                    HundredGramActivityTracker.getInstance(currentAccount).requestTrackedUserStatuses(true);
                    updateRows();
                    if (BulletinFactory.canShowBulletin(HundredGramTrackedUsersActivity.this)) {
                        String text;
                        if (addedCount == 1) {
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(lastAddedDid);
                            String name = user != null ? ContactsController.formatName(user) : ("ID: " + lastAddedDid);
                            text = name + " — " + LocaleController.getString("HundredGramTrackActivity", R.string.HundredGramTrackActivity);
                        } else {
                            text = String.format(LocaleController.getString("HundredGramTrackedUsersAddedCount", R.string.HundredGramTrackedUsersAddedCount), addedCount);
                        }
                        BulletinFactory.of(HundredGramTrackedUsersActivity.this).createSimpleBulletin(
                                R.raw.chats_infotip,
                                text
                        ).show();
                    }
                }
            }
            dialogsActivity.finishFragment();
            return true;
        });
        presentFragment(fragment);
    }

    private void showUserOptionsDialog(long userId) {
        if (getParentActivity() == null) return;
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        String name = user != null ? ContactsController.formatName(user) : "User " + userId;

        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("UserActivityLog", R.string.UserActivityLog),
                LocaleController.getString("SendMessage", R.string.SendMessage),
                LocaleController.getString("ViewProfile", R.string.ViewProfile),
                LocaleController.getString("Delete", R.string.Delete)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(name);
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                new HundredGramUserActivityBottomSheet(HundredGramTrackedUsersActivity.this, userId).show();
            } else if (which == 1) {
                Bundle args = new Bundle();
                args.putLong("user_id", userId);
                presentFragment(new ChatActivity(args));
            } else if (which == 2) {
                Bundle args = new Bundle();
                args.putLong("user_id", userId);
                presentFragment(new ProfileActivity(args));
            } else if (which == 3) {
                confirmDeleteUser(userId);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void confirmDeleteUser(long userId) {
        if (getParentActivity() == null) return;
        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(userId);
        String name = user != null ? ContactsController.formatName(user) : "User " + userId;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage(name + "?");
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            HundredGramConfig.setTrackedUser(userId, false);
            loadUsers();
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            if (BulletinFactory.canShowBulletin(HundredGramTrackedUsersActivity.this)) {
                BulletinFactory.of(HundredGramTrackedUsersActivity.this).createSimpleBulletin(
                        R.raw.ic_delete,
                        LocaleController.getString("HundredGramTrackedUserDeleted", R.string.HundredGramTrackedUserDeleted)
                ).show();
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_STATUS);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ManageChatUserCell cell = new ManageChatUserCell(mContext, 7, 6, true);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
            cell.setCustomRightImage(R.drawable.msg_delete);
            cell.setDelegate((userCell, click) -> {
                if (click) {
                    Object tag = userCell.getTag();
                    if (tag instanceof Long) {
                        confirmDeleteUser((Long) tag);
                    }
                }
                return true;
            });
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ManageChatUserCell cell = (ManageChatUserCell) holder.itemView;
            long uid = userIds.get(position);
            cell.setTag(uid);

            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUser(uid);
                if (user != null) {
                    MessagesController.getInstance(currentAccount).putUser(user, true);
                }
            }
            if (user != null) {
                CharSequence status = LocaleController.formatUserStatus(currentAccount, user);
                cell.setData(user, null, status, position != userIds.size() - 1);
            } else {
                cell.setData(null, "User " + uid, "", position != userIds.size() - 1);
            }
        }
    }
}
