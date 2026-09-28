package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FilePathDatabase;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Storage.CacheModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class HundredGramVaultViewerActivity extends BaseFragment {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_ARCHIVED = 1;
    public static final int FILTER_PRIVATE = 2;
    public static final int FILTER_GROUPS = 3;

    private static final int VIEW_TYPE_HEADER_STATS = 0;
    private static final int VIEW_TYPE_HEADER_FILTERS = 1;
    private static final int VIEW_TYPE_HEADER_TITLE = 2;
    private static final int VIEW_TYPE_CHAT_ITEM = 3;
    private static final int VIEW_TYPE_LOADING = 4;
    private static final int VIEW_TYPE_EMPTY = 5;

    private static final int HEADER_ROWS = 3;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private TextView[] filterChips = new TextView[4];

    private final ArrayList<CacheControlActivity.DialogFileEntities> allEntities = new ArrayList<>();
    private final ArrayList<CacheControlActivity.DialogFileEntities> filteredEntities = new ArrayList<>();
    private final HashSet<Long> archivedDialogIds = new HashSet<>();

    private int currentFilter = FILTER_ALL;
    private boolean loading = true;
    private String searchQuery = null;
    private boolean isSearching = false;

    private long totalSize;
    private int totalFiles;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        getMessagesController().loadDialogs(1, 0, 100, true);
        loadData();
        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("VaultTitle", R.string.VaultTitle));
        actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showClearAllPrompt();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(2, R.drawable.outline_header_search).setIsSearchField(true);
        searchItem.setContentDescription(LocaleController.getString(R.string.Search));
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                isSearching = true;
            }

            @Override
            public void onSearchCollapse() {
                isSearching = false;
                searchQuery = null;
                filterAndSort();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                searchQuery = editText.getText().toString().trim().toLowerCase();
                filterAndSort();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        ActionBarMenuItem clearAllItem = menu.addItem(1, R.drawable.msg_delete);
        clearAllItem.setContentDescription(LocaleController.getString("HundredGramVaultClearAll", R.string.HundredGramVaultClearAll));

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        adapter = new ListAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position >= HEADER_ROWS && !loading && !filteredEntities.isEmpty()) {
                int entityIndex = position - HEADER_ROWS;
                if (entityIndex < filteredEntities.size()) {
                    showDialogOptionsSheet(filteredEntities.get(entityIndex));
                }
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position >= HEADER_ROWS && !loading && !filteredEntities.isEmpty()) {
                int entityIndex = position - HEADER_ROWS;
                if (entityIndex < filteredEntities.size()) {
                    showDeleteDialogPrompt(filteredEntities.get(entityIndex));
                    return true;
                }
            }
            return false;
        });

        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void loadData() {
        loading = true;
        getFileLoader().getFileDatabase().getQueue().postRunnable(() -> {
            getFileLoader().getFileDatabase().ensureDatabaseCreated();
            LongSparseArray<CacheControlActivity.DialogFileEntities> dialogsFilesEntities = new LongSparseArray<>();

            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE), CacheControlActivity.TYPE_OTHER, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE), CacheControlActivity.TYPE_PHOTOS, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE_PUBLIC), CacheControlActivity.TYPE_PHOTOS, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO), CacheControlActivity.TYPE_VIDEOS, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO_PUBLIC), CacheControlActivity.TYPE_VIDEOS, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_AUDIO), CacheControlActivity.TYPE_VOICE, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_STORIES), CacheControlActivity.TYPE_OTHER, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), CacheControlActivity.TYPE_DOCUMENTS, dialogsFilesEntities);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_FILES), CacheControlActivity.TYPE_DOCUMENTS, dialogsFilesEntities);

            ArrayList<CacheControlActivity.DialogFileEntities> entities = new ArrayList<>();
            ArrayList<Long> unknownUsers = new ArrayList<>();
            ArrayList<Long> unknownChats = new ArrayList<>();

            for (int i = 0; i < dialogsFilesEntities.size(); i++) {
                CacheControlActivity.DialogFileEntities dialogEntities = dialogsFilesEntities.valueAt(i);
                if (dialogEntities.totalSize > 0) {
                    entities.add(dialogEntities);
                    if (getMessagesController().getUserOrChat(dialogEntities.dialogId) == null) {
                        if (dialogEntities.dialogId > 0) {
                            unknownUsers.add(dialogEntities.dialogId);
                        } else {
                            unknownChats.add(dialogEntities.dialogId);
                        }
                    }
                }
            }

            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                HashSet<Long> archivedSet = new HashSet<>();
                try {
                    SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT did FROM dialogs WHERE folder_id = 1");
                    while (cursor.next()) {
                        archivedSet.add(cursor.longValue(0));
                    }
                    cursor.dispose();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                if (!unknownUsers.isEmpty()) {
                    try {
                        getMessagesStorage().getUsersInternal(unknownUsers, users);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (!unknownChats.isEmpty()) {
                    try {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", unknownChats), chats);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                Collections.sort(entities, (o1, o2) -> Long.compare(o2.totalSize, o1.totalSize));

                AndroidUtilities.runOnUIThread(() -> {
                    loading = false;
                    archivedDialogIds.clear();
                    archivedDialogIds.addAll(archivedSet);
                    getMessagesController().putUsers(users, true);
                    getMessagesController().putChats(chats, true);
                    allEntities.clear();
                    allEntities.addAll(entities);
                    filterAndSort();
                    updateStats();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
            });
        });
    }

    private void fillDialogsEntitiesRecursive(final File fromFolder, int type, LongSparseArray<CacheControlActivity.DialogFileEntities> dialogsFilesEntities) {
        if (fromFolder == null) {
            return;
        }
        File[] files = fromFolder.listFiles();
        if (files == null) {
            return;
        }
        for (final File fileEntry : files) {
            if (fileEntry.isDirectory()) {
                fillDialogsEntitiesRecursive(fileEntry, type, dialogsFilesEntities);
            } else {
                if (fileEntry.getName().equals(".nomedia")) {
                    continue;
                }
                FilePathDatabase.FileMeta fileMetadata = getFileLoader().getFileDatabase().getFileDialogId(fileEntry, null);
                int addToType = type;
                String fileName = fileEntry.getName().toLowerCase();
                if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a")) {
                    addToType = CacheControlActivity.TYPE_MUSIC;
                }
                CacheModel.FileInfo fileInfo = new CacheModel.FileInfo(fileEntry);
                fileInfo.size = fileEntry.length();
                if (fileInfo.size <= 0) {
                    continue;
                }
                if (fileMetadata != null) {
                    fileInfo.dialogId = fileMetadata.dialogId;
                    fileInfo.messageId = fileMetadata.messageId;
                    fileInfo.messageType = fileMetadata.messageType;
                    if (fileInfo.messageType == MessageObject.TYPE_STORY && fileInfo.size > 0) {
                        addToType = CacheControlActivity.TYPE_STORIES;
                    }
                }
                fileInfo.type = addToType;

                if (fileInfo.dialogId != 0) {
                    CacheControlActivity.DialogFileEntities dialogEntities = dialogsFilesEntities.get(fileInfo.dialogId, null);
                    if (dialogEntities == null) {
                        dialogEntities = new CacheControlActivity.DialogFileEntities(fileInfo.dialogId);
                        dialogsFilesEntities.put(fileInfo.dialogId, dialogEntities);
                    }
                    dialogEntities.addFile(fileInfo, addToType);
                }
            }
        }
    }

    private void filterAndSort() {
        filteredEntities.clear();
        for (int i = 0; i < allEntities.size(); i++) {
            CacheControlActivity.DialogFileEntities entity = allEntities.get(i);
            if (entity.totalSize <= 0) {
                continue;
            }
            if (currentFilter == FILTER_ARCHIVED) {
                if (!isArchivedDialog(entity.dialogId)) {
                    continue;
                }
            } else if (currentFilter == FILTER_PRIVATE) {
                if (entity.dialogId <= 0 || entity.dialogId == CacheControlActivity.UNKNOWN_CHATS_DIALOG_ID) {
                    continue;
                }
            } else if (currentFilter == FILTER_GROUPS) {
                if (entity.dialogId >= 0) {
                    continue;
                }
            }

            if (!TextUtils.isEmpty(searchQuery)) {
                TLObject object = getMessagesController().getUserOrChat(entity.dialogId);
                String title = DialogObject.setDialogPhotoTitle(null, object);
                if (title == null || !title.toLowerCase().contains(searchQuery)) {
                    continue;
                }
            }

            filteredEntities.add(entity);
        }
        Collections.sort(filteredEntities, (o1, o2) -> Long.compare(o2.totalSize, o1.totalSize));
    }

    private boolean isArchivedDialog(long dialogId) {
        if (archivedDialogIds.contains(dialogId)) {
            return true;
        }
        TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogId);
        return dialog != null && dialog.folder_id == 1;
    }

    private void updateStats() {
        totalSize = 0;
        totalFiles = 0;

        for (int i = 0; i < allEntities.size(); i++) {
            CacheControlActivity.DialogFileEntities entity = allEntities.get(i);
            totalSize += entity.totalSize;
            totalFiles += entity.filesCount;
        }

        if (actionBar != null) {
            if (loading) {
                actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));
            } else {
                actionBar.setSubtitle(LocaleController.formatString("VaultFilesCount", R.string.VaultFilesCount, totalFiles, AndroidUtilities.formatFileSize(totalSize)));
            }
        }

        updateFilterChipCounts();
    }

    private void updateFilterChipCounts() {
        if (filterChips[0] == null) return;
        int allCount = allEntities.size();
        int archivedCount = 0;
        int privateCount = 0;
        int groupsCount = 0;
        for (int i = 0; i < allEntities.size(); i++) {
            CacheControlActivity.DialogFileEntities e = allEntities.get(i);
            if (isArchivedDialog(e.dialogId)) {
                archivedCount++;
            }
            if (e.dialogId > 0 && e.dialogId != CacheControlActivity.UNKNOWN_CHATS_DIALOG_ID) {
                privateCount++;
            } else if (e.dialogId < 0) {
                groupsCount++;
            }
        }
        filterChips[0].setText(LocaleController.getString("VaultFilterAll", R.string.VaultFilterAll) + (allCount > 0 ? " (" + allCount + ")" : ""));
        filterChips[1].setText(LocaleController.getString("VaultFilterArchived", R.string.VaultFilterArchived) + (archivedCount > 0 ? " (" + archivedCount + ")" : ""));
        filterChips[2].setText(LocaleController.getString("VaultFilterPrivate", R.string.VaultFilterPrivate) + (privateCount > 0 ? " (" + privateCount + ")" : ""));
        filterChips[3].setText(LocaleController.getString("VaultFilterGroupsChannels", R.string.VaultFilterGroupsChannels) + (groupsCount > 0 ? " (" + groupsCount + ")" : ""));
        updateFilterChipsStyle();
    }

    private void updateFilterChipsStyle() {
        for (int i = 0; i < filterChips.length; i++) {
            TextView chip = filterChips[i];
            if (chip == null) continue;
            boolean selected = (currentFilter == i);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(AndroidUtilities.dp(16));
            if (selected) {
                bg.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
                chip.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            } else {
                bg.setColor(Theme.getColor(Theme.key_chat_inBubble));
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            chip.setBackground(bg);
        }
    }

    private void showDialogOptionsSheet(CacheControlActivity.DialogFileEntities entity) {
        if (entity == null || getParentActivity() == null) {
            return;
        }
        if (entity.dialogId == CacheControlActivity.UNKNOWN_CHATS_DIALOG_ID) {
            showDeleteDialogPrompt(entity);
            return;
        }
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        TLObject object = getMessagesController().getUserOrChat(entity.dialogId);
        String title = DialogObject.setDialogPhotoTitle(null, object);
        if (TextUtils.isEmpty(title)) {
            title = LocaleController.getString("Chats", R.string.Chats);
        }
        builder.setTitle(title, true);
        CharSequence[] items = new CharSequence[]{
                LocaleController.getString("Open", R.string.Open),
                LocaleController.getString("OpenProfile", R.string.OpenProfile),
                LocaleController.getString("HundredGramChatSettingsTitle", R.string.HundredGramChatSettingsTitle),
                LocaleController.getString("HundredGramVaultClearChat", R.string.HundredGramVaultClearChat)
        };
        int[] icons = new int[]{
                R.drawable.msg_message,
                R.drawable.msg_info,
                R.drawable.msg_settings,
                R.drawable.msg_delete
        };
        builder.setItems(items, icons, (dialog, which) -> {
            if (which == 0) {
                Bundle args = new Bundle();
                if (entity.dialogId > 0) {
                    args.putLong("user_id", entity.dialogId);
                } else {
                    args.putLong("chat_id", -entity.dialogId);
                }
                presentFragment(new ChatActivity(args));
            } else if (which == 1) {
                Bundle args = new Bundle();
                if (entity.dialogId > 0) {
                    args.putLong("user_id", entity.dialogId);
                } else {
                    args.putLong("chat_id", -entity.dialogId);
                }
                presentFragment(new ProfileActivity(args, null));
            } else if (which == 2) {
                showDialog(new org.telegram.ui.Components.HundredGramChatVaultBottomSheet(HundredGramVaultViewerActivity.this, entity.dialogId));
            } else if (which == 3) {
                showDeleteDialogPrompt(entity);
            }
        });
        showDialog(builder.create());
    }

    private void showDeleteDialogPrompt(CacheControlActivity.DialogFileEntities entity) {
        if (entity == null || getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultClearChat", R.string.HundredGramVaultClearChat));
        builder.setMessage(LocaleController.formatString("ClearChatCacheConfirm", R.string.ClearChatCacheConfirm, AndroidUtilities.formatFileSize(entity.totalSize)));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            getFileLoader().getFileDatabase().clearDialogFiles(entity.dialogId, freedBytes -> {
                allEntities.remove(entity);
                filterAndSort();
                updateStats();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                        R.raw.ic_delete,
                        LocaleController.formatString("HundredGramVaultClearedChat", R.string.HundredGramVaultClearedChat, AndroidUtilities.formatFileSize(freedBytes))
                ).show();
            });
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        alert.show();
        alert.redPositive();
    }

    private void showClearAllPrompt() {
        if (getParentActivity() == null || totalSize <= 0) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultClearAll", R.string.HundredGramVaultClearAll));
        builder.setMessage(LocaleController.getString("HundredGramVaultClearAllConfirm", R.string.HundredGramVaultClearAllConfirm));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            final AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
            progressDialog.setCanCancel(false);
            progressDialog.showDelayed(300);

            getFileLoader().getFileDatabase().clearAllVaultFiles(freedBytes -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignored) {}
                allEntities.clear();
                filteredEntities.clear();
                updateStats();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                        R.raw.ic_delete,
                        LocaleController.formatString("HundredGramVaultClearedAll", R.string.HundredGramVaultClearedAll, AndroidUtilities.formatFileSize(freedBytes))
                ).show();
            });
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        alert.show();
        alert.redPositive();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_CHAT_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            Context context = parent.getContext();
            switch (viewType) {
                case VIEW_TYPE_HEADER_STATS:
                    LinearLayout statsContainer = new LinearLayout(context);
                    statsContainer.setOrientation(LinearLayout.HORIZONTAL);
                    statsContainer.setGravity(Gravity.CENTER_VERTICAL);
                    statsContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    statsContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

                    ImageView statsIcon = new ImageView(context);
                    statsIcon.setImageResource(R.drawable.msg_folders);
                    statsIcon.setColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton));
                    statsContainer.addView(statsIcon, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 16, 0));

                    LinearLayout textLayout = new LinearLayout(context);
                    textLayout.setOrientation(LinearLayout.VERTICAL);

                    TextView titleView = new TextView(context);
                    titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    titleView.setText(LocaleController.getString("VaultTitle", R.string.VaultTitle));
                    textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

                    TextView subtitleView = new TextView(context);
                    subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                    if (loading) {
                        subtitleView.setText(LocaleController.getString("Loading", R.string.Loading));
                    } else {
                        subtitleView.setText(LocaleController.formatString("VaultFilesCount", R.string.VaultFilesCount, totalFiles, AndroidUtilities.formatFileSize(totalSize)));
                    }
                    textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

                    statsContainer.addView(textLayout, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
                    view = statsContainer;
                    break;
                case VIEW_TYPE_HEADER_FILTERS:
                    HorizontalScrollView filterScrollView = new HorizontalScrollView(context);
                    filterScrollView.setHorizontalScrollBarEnabled(false);
                    filterScrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
                    LinearLayout chipsContainer = new LinearLayout(context);
                    chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
                    chipsContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

                    for (int i = 0; i < 4; i++) {
                        final int filterIndex = i;
                        TextView chip = new TextView(context);
                        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                        chip.setGravity(Gravity.CENTER);
                        chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(7), AndroidUtilities.dp(14), AndroidUtilities.dp(7));
                        chip.setOnClickListener(v -> {
                            if (currentFilter != filterIndex) {
                                currentFilter = filterIndex;
                                updateFilterChipsStyle();
                                filterAndSort();
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                        filterChips[i] = chip;
                        chipsContainer.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 4, 0, 4, 0));
                    }
                    updateFilterChipCounts();
                    filterScrollView.addView(chipsContainer, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
                    view = filterScrollView;
                    break;
                case VIEW_TYPE_HEADER_TITLE:
                    HeaderCell headerCell = new HeaderCell(context);
                    headerCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = headerCell;
                    break;
                case VIEW_TYPE_LOADING:
                    FlickerLoadingView flicker = new FlickerLoadingView(context);
                    flicker.setViewType(FlickerLoadingView.DIALOG_CACHE_CONTROL);
                    flicker.setIsSingleCell(true);
                    flicker.setItemsCount(3);
                    flicker.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = flicker;
                    break;
                case VIEW_TYPE_EMPTY:
                    LinearLayout emptyView = new LinearLayout(context);
                    emptyView.setOrientation(LinearLayout.VERTICAL);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    emptyView.setPadding(AndroidUtilities.dp(32), AndroidUtilities.dp(48), AndroidUtilities.dp(32), AndroidUtilities.dp(48));

                    ImageView icon = new ImageView(context);
                    icon.setImageResource(R.drawable.msg_folders);
                    icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon));
                    emptyView.addView(icon, LayoutHelper.createLinear(56, 56, Gravity.CENTER, 0, 0, 0, 16));

                    TextView emptyText = new TextView(context);
                    emptyText.setText(LocaleController.getString("VaultNoFiles", R.string.VaultNoFiles));
                    emptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    emptyText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
                    emptyText.setGravity(Gravity.CENTER);
                    emptyView.addView(emptyText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

                    view = emptyView;
                    break;
                case VIEW_TYPE_CHAT_ITEM:
                default:
                    CacheControlActivity.UserCell userCell = new CacheControlActivity.UserCell(context, null);
                    userCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    view = userCell;
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HEADER_STATS:
                    LinearLayout statsLayout = (LinearLayout) holder.itemView;
                    LinearLayout texts = (LinearLayout) statsLayout.getChildAt(1);
                    TextView sub = (TextView) texts.getChildAt(1);
                    if (loading) {
                        sub.setText(LocaleController.getString("Loading", R.string.Loading));
                    } else {
                        sub.setText(LocaleController.formatString("VaultFilesCount", R.string.VaultFilesCount, totalFiles, AndroidUtilities.formatFileSize(totalSize)));
                    }
                    break;
                case VIEW_TYPE_HEADER_TITLE:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(LocaleController.formatPluralString("Chats", filteredEntities.size()));
                    break;
                case VIEW_TYPE_CHAT_ITEM:
                    int entityIndex = position - HEADER_ROWS;
                    if (entityIndex >= 0 && entityIndex < filteredEntities.size()) {
                        CacheControlActivity.UserCell userCell = (CacheControlActivity.UserCell) holder.itemView;
                        CacheControlActivity.DialogFileEntities entity = filteredEntities.get(entityIndex);
                        TLObject object = getMessagesController().getUserOrChat(entity.dialogId);
                        String title;
                        if (entity.dialogId == CacheControlActivity.UNKNOWN_CHATS_DIALOG_ID) {
                            title = LocaleController.getString(R.string.CacheOtherChats);
                            userCell.getImageView().getAvatarDrawable().setAvatarType(AvatarDrawable.AVATAR_TYPE_OTHER_CHATS);
                            userCell.getImageView().setForUserOrChat(null, userCell.getImageView().getAvatarDrawable());
                        } else {
                            title = DialogObject.setDialogPhotoTitle(userCell.getImageView(), object);
                        }
                        if (isArchivedDialog(entity.dialogId)) {
                            title = "📁 " + title;
                        }
                        userCell.dialogFileEntities = entity;
                        userCell.getImageView().setRoundRadius(AndroidUtilities.dp(object instanceof TLRPC.Chat && ((TLRPC.Chat) object).forum ? 12 : 19));
                        String filesStr = LocaleController.formatString("VaultFilesCount", R.string.VaultFilesCount, entity.filesCount, AndroidUtilities.formatFileSize(entity.totalSize));
                        userCell.setTextAndValue(title, filesStr, entityIndex < filteredEntities.size() - 1);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return VIEW_TYPE_HEADER_STATS;
            if (position == 1) return VIEW_TYPE_HEADER_FILTERS;
            if (position == 2) return VIEW_TYPE_HEADER_TITLE;
            if (loading) return VIEW_TYPE_LOADING;
            if (filteredEntities.isEmpty()) return VIEW_TYPE_EMPTY;
            return VIEW_TYPE_CHAT_ITEM;
        }

        @Override
        public int getItemCount() {
            if (loading || filteredEntities.isEmpty()) {
                return HEADER_ROWS + 1;
            }
            return HEADER_ROWS + filteredEntities.size();
        }
    }
}
