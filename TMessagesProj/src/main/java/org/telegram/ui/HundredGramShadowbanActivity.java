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
import org.telegram.messenger.HundredGramShadowbanController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ManageChatUserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Set;

public class HundredGramShadowbanActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int ITEM_CLEAR_ALL = 1;
    private static final int ITEM_ADD = 2;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private LinearLayout emptyView;
    private ImageView floatingButton;

    private final ArrayList<Long> bannedIds = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.updateInterfaces);
        loadBannedEntities();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.updateInterfaces);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBannedEntities();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void loadBannedEntities() {
        bannedIds.clear();
        Set<Long> set = HundredGramShadowbanController.getShadowBannedUsers();
        if (set != null) {
            bannedIds.addAll(set);
        }
        updateSubtitle();
        updateEmptyView();
    }

    private void updateSubtitle() {
        if (actionBar != null) {
            int count = bannedIds.size();
            String subtitle = LocaleController.formatPluralString("ShadowbanUsersCount", count, count);
            actionBar.setSubtitle(subtitle);
        }
    }

    private void updateEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(bannedIds.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Shadowban", R.string.Shadowban));
        updateSubtitle();

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == ITEM_CLEAR_ALL) {
                    confirmClearAll();
                } else if (id == ITEM_ADD) {
                    openPicker();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(ITEM_CLEAR_ALL, R.drawable.msg_delete);
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
            if (position < 0 || position >= bannedIds.size()) return;
            long id = bannedIds.get(position);
            showEntityOptionsDialog(id);
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= bannedIds.size()) return false;
            long id = bannedIds.get(position);
            showEntityOptionsDialog(id);
            return true;
        });

        // Empty state
        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);

        ImageView emptyIcon = new ImageView(context);
        emptyIcon.setImageResource(R.drawable.msg_block2);
        emptyIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_emptyListPlaceholder, getResourceProvider()), PorterDuff.Mode.MULTIPLY));
        emptyView.addView(emptyIcon, LayoutHelper.createLinear(64, 64, Gravity.CENTER, 0, 0, 0, 16));

        TextView emptyTitle = new TextView(context);
        emptyTitle.setText(LocaleController.getString("ShadowbanEmpty", R.string.ShadowbanEmpty));
        emptyTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        emptyTitle.setTextSize(18);
        emptyTitle.setTypeface(AndroidUtilities.bold());
        emptyTitle.setGravity(Gravity.CENTER);
        emptyView.addView(emptyTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 8));

        TextView emptySub = new TextView(context);
        emptySub.setText(LocaleController.getString("ShadowbanDesc", R.string.ShadowbanDesc));
        emptySub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
        emptySub.setTextSize(14);
        emptySub.setGravity(Gravity.CENTER);
        emptyView.addView(emptySub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 24));

        TextView addButton = new TextView(context);
        addButton.setText(LocaleController.getString("ShadowbanAdd", R.string.ShadowbanAdd));
        addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText, getResourceProvider()));
        addButton.setTextSize(15);
        addButton.setTypeface(AndroidUtilities.bold());
        addButton.setGravity(Gravity.CENTER);
        addButton.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        addButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_featuredStickers_addButton, getResourceProvider()),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed, getResourceProvider())));
        addButton.setOnClickListener(v -> openPicker());
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
        floatingButton.setOnClickListener(v -> openPicker());
        frameLayout.addView(floatingButton, LayoutHelper.createFrame(56, 56, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, 16, 16, 16, 16));

        updateEmptyView();
        return fragmentView;
    }

    private void updateRows() {
        loadBannedEntities();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void openPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowMultiple", true);
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((dialogsActivity, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null && !dids.isEmpty()) {
                int count = 0;
                long lastAddedId = 0;
                for (MessagesStorage.TopicKey topicKey : dids) {
                    long did = topicKey.dialogId;
                    if (did != 0 && did != getUserConfig().getClientUserId()) {
                        HundredGramShadowbanController.addShadowBan(did);
                        lastAddedId = did;
                        count++;
                    }
                }
                if (count > 0) {
                    updateRows();
                    if (BulletinFactory.canShowBulletin(HundredGramShadowbanActivity.this)) {
                        String name = getEntityName(lastAddedId);
                        String text = count == 1
                                ? LocaleController.formatString("ShadowbanAddedToast", R.string.ShadowbanAddedToast, name)
                                : LocaleController.formatPluralString("ShadowbanUsersCount", count, count);
                        BulletinFactory.of(HundredGramShadowbanActivity.this).createSimpleBulletin(
                                R.drawable.msg_block2,
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

    private String getEntityName(long id) {
        if (id > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(id);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUser(id);
            }
            if (user != null) {
                return ContactsController.formatName(user.first_name, user.last_name);
            }
            return "User " + id;
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-id);
            if (chat == null) {
                chat = MessagesStorage.getInstance(currentAccount).getChat(-id);
            }
            if (chat != null) {
                return chat.title;
            }
            return "Channel " + (-id);
        }
    }

    private void showEntityOptionsDialog(long id) {
        if (getParentActivity() == null) return;
        String name = getEntityName(id);

        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("ViewProfile", R.string.ViewProfile),
                LocaleController.getString("ShadowbanRemove", R.string.ShadowbanRemove)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(name);
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                Bundle args = new Bundle();
                if (id > 0) {
                    args.putLong("user_id", id);
                } else {
                    args.putLong("chat_id", -id);
                }
                presentFragment(new ProfileActivity(args));
            } else if (which == 1) {
                confirmRemoveBan(id);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void confirmRemoveBan(long id) {
        if (getParentActivity() == null) return;
        String name = getEntityName(id);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString("ShadowbanRemove", R.string.ShadowbanRemove));
        builder.setMessage(name + "?");
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            HundredGramShadowbanController.removeShadowBan(id);
            updateRows();
            if (BulletinFactory.canShowBulletin(HundredGramShadowbanActivity.this)) {
                BulletinFactory.of(HundredGramShadowbanActivity.this).createSimpleBulletin(
                        R.drawable.msg_block2,
                        LocaleController.formatString("ShadowbanRemovedToast", R.string.ShadowbanRemovedToast, name)
                ).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void confirmClearAll() {
        if (getParentActivity() == null || bannedIds.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString("ShadowbanClearAll", R.string.ShadowbanClearAll));
        builder.setMessage(LocaleController.getString("ShadowbanClearAllAlert", R.string.ShadowbanClearAllAlert));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            HundredGramShadowbanController.clearAll();
            updateRows();
            if (BulletinFactory.canShowBulletin(HundredGramShadowbanActivity.this)) {
                BulletinFactory.of(HundredGramShadowbanActivity.this).createSimpleBulletin(
                        R.drawable.msg_block2,
                        LocaleController.getString("ShadowbanClearAll", R.string.ShadowbanClearAll)
                ).show();
            }
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
            return bannedIds.size();
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
                        confirmRemoveBan((Long) tag);
                    }
                }
                return true;
            });
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ManageChatUserCell cell = (ManageChatUserCell) holder.itemView;
            long id = bannedIds.get(position);
            cell.setTag(id);

            boolean isLast = position == bannedIds.size() - 1;
            if (id > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(id);
                if (user == null) {
                    user = MessagesStorage.getInstance(currentAccount).getUser(id);
                    if (user != null) {
                        MessagesController.getInstance(currentAccount).putUser(user, true);
                    }
                }
                if (user != null) {
                    CharSequence status = LocaleController.formatUserStatus(currentAccount, user);
                    cell.setData(user, null, status, !isLast);
                } else {
                    cell.setData(null, "User " + id, "ID: " + id, !isLast);
                }
            } else {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-id);
                if (chat == null) {
                    chat = MessagesStorage.getInstance(currentAccount).getChat(-id);
                    if (chat != null) {
                        MessagesController.getInstance(currentAccount).putChat(chat, true);
                    }
                }
                if (chat != null) {
                    cell.setData(chat, null, "ID: " + id, !isLast);
                } else {
                    cell.setData(null, "Channel " + (-id), "ID: " + id, !isLast);
                }
            }
        }
    }
}
