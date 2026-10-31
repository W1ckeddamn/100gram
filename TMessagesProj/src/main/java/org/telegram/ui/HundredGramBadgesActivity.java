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
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.HundredGramBadgeAlert;
import org.telegram.ui.Components.HundredGramBadgeEditorBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Экран управления кастомными бейджами пользователей 100gram.
 * Включает синхронизацию с GitHub, панель разработчика (ID 1211607237),
 * управление списком администраторов и экспорт badges.json.
 */
public class HundredGramBadgesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private final List<HundredGramBadgesController.Badge> badgeList = new ArrayList<>();

    private int rowCount;
    private int syncRow;
    private int statusRow;
    private int manageAdminsRow;
    private int exportJsonRow;
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
        syncRow = rowCount++;
        statusRow = rowCount++;

        boolean isAdmin = HundredGramBadgesController.getInstance().isCurrentUserAdmin();
        if (isAdmin) {
            manageAdminsRow = rowCount++;
            exportJsonRow = rowCount++;
            assignSelfRow = rowCount++;
            addBadgeRow = rowCount++;
        } else {
            manageAdminsRow = -1;
            exportJsonRow = -1;
            assignSelfRow = -1;
            addBadgeRow = -1;
        }

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
            if (position == syncRow) {
                triggerSync();
            } else if (position == manageAdminsRow) {
                showManageAdminsDialog();
            } else if (position == exportJsonRow) {
                exportBadgesForGithub();
            } else if (position == assignSelfRow) {
                long myId = UserConfig.getInstance(currentAccount).getClientUserId();
                HundredGramBadgesController.Badge b = HundredGramBadgesController.getInstance().getBadge(myId);
                if (b == null) {
                    b = new HundredGramBadgesController.Badge(
                            myId,
                            LocaleController.getString("CustomBadgeDevTitle", R.string.CustomBadgeDevTitle),
                            LocaleController.getString("CustomBadgeDevDesc", R.string.CustomBadgeDevDesc),
                            HundredGramBadgesController.ICON_DEV,
                            0,
                            0xFF3390EC,
                            true
                    );
                    HundredGramBadgesController.getInstance().setBadge(b);
                    loadData();
                    BulletinFactory.of(HundredGramBadgesActivity.this).createSimpleBulletin(
                            R.drawable.verified_check,
                            LocaleController.getString("CustomBadgeSaved", R.string.CustomBadgeSaved)
                    ).show();
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

    private void triggerSync() {
        if (HundredGramBadgesController.getInstance().isSyncing()) {
            return;
        }
        if (listAdapter != null) {
            listAdapter.notifyItemChanged(syncRow);
        }
        HundredGramBadgesController.getInstance().syncRemoteBadges(() -> {
            loadData();
            BulletinFactory.of(HundredGramBadgesActivity.this).createSimpleBulletin(
                    R.drawable.verified_check,
                    LocaleController.getString("CustomBadgeSyncSuccess", R.string.CustomBadgeSyncSuccess)
            ).show();
        }, error -> {
            if (listAdapter != null) {
                listAdapter.notifyItemChanged(syncRow);
            }
            String msg = LocaleController.formatString("CustomBadgeSyncError", R.string.CustomBadgeSyncError, error);
            BulletinFactory.of(HundredGramBadgesActivity.this).createErrorBulletin(msg).show();
        });
    }

    private void exportBadgesForGithub() {
        String json = HundredGramBadgesController.getInstance().exportBadgesJson();
        AndroidUtilities.addToClipboard(json);
        BulletinFactory.of(HundredGramBadgesActivity.this).createSimpleBulletin(
                R.drawable.msg_copy,
                LocaleController.getString("CustomBadgeExportCopied", R.string.CustomBadgeExportCopied)
        ).show();
    }

    private void showManageAdminsDialog() {
        if (getParentActivity() == null) return;
        List<Long> adminIds = HundredGramBadgesController.getInstance().getAdminIds();

        List<CharSequence> items = new ArrayList<>();
        List<Long> itemUserIds = new ArrayList<>();

        for (Long adminId : adminIds) {
            TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(adminId);
            String name = u != null ? UserObject.getUserName(u) : ("ID: " + adminId);
            if (adminId == HundredGramBadgesController.OWNER_USER_ID) {
                items.add("👑 " + name + " (ID " + adminId + " — Главный разработчик)");
            } else {
                items.add("⚡ " + name + " (ID " + adminId + ") — Нажмите для удаления");
            }
            itemUserIds.add(adminId);
        }
        items.add("+ " + LocaleController.getString("CustomBadgeAddAdmin", R.string.CustomBadgeAddAdmin));
        itemUserIds.add(-1L);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("CustomBadgeManageAdmins", R.string.CustomBadgeManageAdmins));
        builder.setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
            long target = itemUserIds.get(which);
            if (target == -1L) {
                showAddAdminDialog();
            } else if (target == HundredGramBadgesController.OWNER_USER_ID) {
                String msg = LocaleController.formatString("CustomBadgeCannotRemoveOwner", R.string.CustomBadgeCannotRemoveOwner, target);
                BulletinFactory.of(HundredGramBadgesActivity.this).createErrorBulletin(msg).show();
            } else {
                showRemoveAdminConfirmDialog(target);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showAddAdminDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("CustomBadgeAddAdmin", R.string.CustomBadgeAddAdmin));
        builder.setMessage(LocaleController.getString("CustomBadgeEnterAdminId", R.string.CustomBadgeEnterAdminId));

        final EditTextBoldCursor input = new EditTextBoldCursor(getParentActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("123456789");
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray3));

        FrameLayout container = new FrameLayout(getParentActivity());
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
        container.addView(input, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton(LocaleController.getString("Save", R.string.Save), (dialog, which) -> {
            String text = input.getText().toString().trim();
            long newId;
            try {
                newId = Long.parseLong(text);
            } catch (Exception e) {
                newId = 0;
            }
            if (newId != 0) {
                HundredGramBadgesController.getInstance().addAdmin(newId);
                BulletinFactory.of(HundredGramBadgesActivity.this).createSimpleBulletin(
                        R.drawable.verified_check,
                        LocaleController.getString("CustomBadgeAdminAdded", R.string.CustomBadgeAdminAdded)
                ).show();
                updateRows();
            } else {
                BulletinFactory.of(HundredGramBadgesActivity.this).createErrorBulletin(
                        LocaleController.getString("CustomBadgeInvalidId", R.string.CustomBadgeInvalidId)
                ).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showRemoveAdminConfirmDialog(long adminId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("CustomBadgeManageAdmins", R.string.CustomBadgeManageAdmins));
        builder.setMessage(LocaleController.formatString("CustomBadgeRemoveAdminConfirm", R.string.CustomBadgeRemoveAdminConfirm, adminId));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            if (HundredGramBadgesController.getInstance().removeAdmin(adminId)) {
                BulletinFactory.of(HundredGramBadgesActivity.this).createSimpleBulletin(
                        R.drawable.msg_delete,
                        LocaleController.getString("CustomBadgeAdminRemoved", R.string.CustomBadgeAdminRemoved)
                ).show();
                updateRows();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
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
            return pos == syncRow || pos == manageAdminsRow || pos == exportJsonRow || pos == assignSelfRow || pos == addBadgeRow || (pos >= badgesStartRow && pos < badgesEndRow);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == syncRow) {
                return 0; // Sync card
            } else if (position == statusRow) {
                return 1; // Status badge text
            } else if (position == badgesHeaderRow) {
                return 2; // Header
            } else if (position >= badgesStartRow && position < badgesEndRow) {
                return 3; // Badge item cell
            } else {
                return 4; // Action rows (manageAdmins, exportJson, assignSelf, addBadge)
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0: { // Sync card
                    LinearLayout card = new LinearLayout(mContext);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundWhite)));
                    card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

                    RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(6));
                    card.setLayoutParams(lp);

                    LinearLayout topRow = new LinearLayout(mContext);
                    topRow.setOrientation(LinearLayout.HORIZONTAL);
                    topRow.setGravity(Gravity.CENTER_VERTICAL);

                    ImageView syncIcon = new ImageView(mContext);
                    syncIcon.setImageResource(R.drawable.msg_download);
                    syncIcon.setColorFilter(new PorterDuffColorFilter(0xFF3390EC, PorterDuff.Mode.SRC_IN));
                    topRow.addView(syncIcon, LayoutHelper.createLinear(24, 24, 0, 0, 12, 0));

                    LinearLayout textCol = new LinearLayout(mContext);
                    textCol.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(mContext);
                    title.setText(LocaleController.getString("CustomBadgeSyncNow", R.string.CustomBadgeSyncNow));
                    title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                    title.setTypeface(AndroidUtilities.bold());
                    title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    textCol.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                    TextView subtitle = new TextView(mContext);
                    subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                    subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
                    textCol.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

                    topRow.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

                    ImageView arrow = new ImageView(mContext);
                    arrow.setImageResource(R.drawable.arrow_more);
                    arrow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN));
                    topRow.addView(arrow, LayoutHelper.createLinear(16, 16));

                    card.addView(topRow);
                    view = card;
                    break;
                }
                case 1: { // Status text
                    TextView tv = new TextView(mContext);
                    tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(8), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
                    view = tv;
                    break;
                }
                case 2: { // Header
                    TextView tv = new TextView(mContext);
                    tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(8));
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                    tv.setTypeface(AndroidUtilities.bold());
                    tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                    view = tv;
                    break;
                }
                case 3: { // Badge Item Cell
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
                    titleRow.addView(badgeIcon, LayoutHelper.createLinear(18, 18, 0, 0, 6, 0));

                    ImageView officialCheck = new ImageView(mContext);
                    officialCheck.setImageResource(R.drawable.verified_check);
                    officialCheck.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    titleRow.addView(officialCheck, LayoutHelper.createLinear(14, 14));

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
                case 4:
                default: { // Action row
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
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == 0) { // Sync card
                LinearLayout card = (LinearLayout) holder.itemView;
                LinearLayout topRow = (LinearLayout) card.getChildAt(0);
                ImageView syncIcon = (ImageView) topRow.getChildAt(0);
                LinearLayout textCol = (LinearLayout) topRow.getChildAt(1);
                TextView title = (TextView) textCol.getChildAt(0);
                TextView subtitle = (TextView) textCol.getChildAt(1);

                boolean isSyncing = HundredGramBadgesController.getInstance().isSyncing();
                if (isSyncing) {
                    title.setText(LocaleController.getString("CustomBadgeSyncing", R.string.CustomBadgeSyncing));
                    syncIcon.setColorFilter(new PorterDuffColorFilter(0xFFFFB300, PorterDuff.Mode.SRC_IN));
                } else {
                    title.setText(LocaleController.getString("CustomBadgeSyncNow", R.string.CustomBadgeSyncNow));
                    syncIcon.setColorFilter(new PorterDuffColorFilter(0xFF3390EC, PorterDuff.Mode.SRC_IN));
                }

                long lastTime = HundredGramBadgesController.getInstance().getLastSyncTime();
                if (lastTime > 0) {
                    String formatted = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(lastTime));
                    subtitle.setText(LocaleController.formatString("CustomBadgeLastSync", R.string.CustomBadgeLastSync, formatted));
                } else {
                    subtitle.setText(LocaleController.getString("CustomBadgeNeverSynced", R.string.CustomBadgeNeverSynced));
                }
            } else if (viewType == 1) { // Status text
                TextView tv = (TextView) holder.itemView;
                boolean isAdmin = HundredGramBadgesController.getInstance().isCurrentUserAdmin();
                if (isAdmin) {
                    tv.setText(LocaleController.getString("CustomBadgeStatusDev", R.string.CustomBadgeStatusDev));
                    tv.setTextColor(0xFF3390EC);
                } else {
                    tv.setText(LocaleController.getString("CustomBadgeStatusUser", R.string.CustomBadgeStatusUser));
                    tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
                }
            } else if (viewType == 2) { // Header
                TextView tv = (TextView) holder.itemView;
                tv.setText(LocaleController.getString("CustomBadgesAssigned", R.string.CustomBadgesAssigned) + " (" + badgeList.size() + ")");
            } else if (viewType == 3) { // Badge Item Cell
                int index = position - badgesStartRow;
                if (index >= 0 && index < badgeList.size()) {
                    HundredGramBadgesController.Badge b = badgeList.get(index);
                    LinearLayout item = (LinearLayout) holder.itemView;
                    BackupImageView avatar = (BackupImageView) item.getChildAt(0);
                    LinearLayout textLayout = (LinearLayout) item.getChildAt(1);
                    LinearLayout titleRow = (LinearLayout) textLayout.getChildAt(0);
                    TextView name = (TextView) titleRow.getChildAt(0);
                    ImageView badgeIcon = (ImageView) titleRow.getChildAt(1);
                    ImageView officialCheck = (ImageView) titleRow.getChildAt(2);
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

                    officialCheck.setVisibility(b.isOfficial ? View.VISIBLE : View.GONE);

                    String sub = !TextUtils.isEmpty(b.title) ? b.title : ("ID: " + b.userId);
                    if (!TextUtils.isEmpty(b.description)) {
                        sub += " — " + b.description;
                    }
                    subtitle.setText(sub);
                }
            } else if (viewType == 4) { // Action row
                FrameLayout container = (FrameLayout) holder.itemView;
                LinearLayout row = (LinearLayout) container.getChildAt(0);
                ImageView icon = (ImageView) row.getChildAt(0);
                TextView text = (TextView) row.getChildAt(1);

                if (position == manageAdminsRow) {
                    icon.setImageResource(R.drawable.msg_admins);
                    icon.setColorFilter(new PorterDuffColorFilter(0xFF3390EC, PorterDuff.Mode.SRC_IN));
                    int adminCount = HundredGramBadgesController.getInstance().getAdminIds().size();
                    text.setText(LocaleController.getString("CustomBadgeManageAdmins", R.string.CustomBadgeManageAdmins) + " (" + adminCount + ")");
                } else if (position == exportJsonRow) {
                    icon.setImageResource(R.drawable.msg_copy);
                    icon.setColorFilter(new PorterDuffColorFilter(0xFF4CAF50, PorterDuff.Mode.SRC_IN));
                    text.setText(LocaleController.getString("CustomBadgeExportGithub", R.string.CustomBadgeExportGithub));
                } else if (position == assignSelfRow) {
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
            }
        }
    }
}
