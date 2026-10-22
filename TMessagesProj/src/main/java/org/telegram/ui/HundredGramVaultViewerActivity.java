package org.telegram.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import androidx.core.widget.NestedScrollView;
import org.telegram.ui.Cells.HeaderCell;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
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
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.RecyclerViewItemRangeSelector;
import org.telegram.ui.Components.ViewPagerFixed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class HundredGramVaultViewerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public static final int TAB_PHOTOS = 0;
    public static final int TAB_VIDEOS = 1;
    public static final int TAB_VOICE = 2;
    public static final int TAB_FILES = 3;

    private static final int ITEM_SEARCH = 1;
    private static final int ITEM_FILTER = 2;
    private static final int ITEM_SCAN = 3;
    private static final int ITEM_SORT = 4;
    private static final int ITEM_SELECT_MODE = 5;

    private static final int ITEM_SELECT_ALL = 10;
    private static final int ITEM_BATCH_SAVE = 11;
    private static final int ITEM_BATCH_ZIP = 12;
    private static final int ITEM_BATCH_DELETE = 13;

    public static final int SORT_DATE_DESC = 0;
    public static final int SORT_DATE_ASC = 1;
    public static final int SORT_SIZE_DESC = 2;
    public static final int SORT_SIZE_ASC = 3;
    public static final int SORT_NAME_ASC = 4;

    public static class VaultItem {
        public int mid;
        public long uid;
        public String fileName;
        public String vaultPath;
        public File file;
        public long size;
        public long date;
        public String chatTitle;
        private MessageObject messageObject;

        public boolean isPhoto() {
            if (fileName == null) return false;
            String fn = fileName.toLowerCase();
            return fn.endsWith(".jpg") || fn.endsWith(".jpeg") || fn.endsWith(".png") || fn.endsWith(".webp") || fn.endsWith(".heic");
        }

        public boolean isVideo() {
            if (fileName == null) return false;
            String fn = fileName.toLowerCase();
            return fn.endsWith(".mp4") || fn.endsWith(".mkv") || fn.endsWith(".mov") || fn.endsWith(".avi") || fn.endsWith(".webm") || fn.endsWith(".3gp");
        }

        public boolean isVoice() {
            if (fileName == null) return false;
            String fn = fileName.toLowerCase();
            return fn.endsWith(".ogg") || fn.endsWith(".oga") || fn.endsWith(".opus") || fn.endsWith(".mp3") || fn.endsWith(".m4a") || fn.endsWith(".wav") || fn.endsWith(".aac") || fn.endsWith(".flac");
        }

        public int getTabType() {
            if (isPhoto()) return TAB_PHOTOS;
            if (isVideo()) return TAB_VIDEOS;
            if (isVoice()) return TAB_VOICE;
            return TAB_FILES;
        }

        public MessageObject getMessageObject(int currentAccount) {
            if (messageObject == null) {
                TLRPC.TL_message msg = new TLRPC.TL_message();
                msg.id = mid;
                msg.peer_id = MessagesController.getInstance(currentAccount).getPeer(uid);
                msg.dialog_id = uid;
                msg.media = new TLRPC.TL_messageMediaDocument();
                msg.media.document = new TLRPC.TL_document();
                msg.media.document.id = mid;
                msg.media.document.file_name = fileName != null ? fileName : "audio.ogg";
                msg.media.document.size = size;
                msg.media.document.mime_type = isVoice() ? "audio/ogg" : "audio/mpeg";
                TLRPC.TL_documentAttributeAudio audioAttr = new TLRPC.TL_documentAttributeAudio();
                audioAttr.voice = isVoice();
                audioAttr.title = fileName;
                msg.media.document.attributes.add(audioAttr);
                msg.attachPath = vaultPath;
                messageObject = new MessageObject(currentAccount, msg, false, false);
                messageObject.attachPathExists = true;
            }
            return messageObject;
        }
    }

    private final ArrayList<VaultItem> allItems = new ArrayList<>();
    private final ArrayList<VaultItem> photosList = new ArrayList<>();
    private final ArrayList<VaultItem> videosList = new ArrayList<>();
    private final ArrayList<VaultItem> voiceList = new ArrayList<>();
    private final ArrayList<VaultItem> filesList = new ArrayList<>();

    private final PageAdapter[] pageAdapters = new PageAdapter[4];
    private final RecyclerListView[] listViews = new RecyclerListView[4];
    private final EmptyTextProgressView[] emptyViews = new EmptyTextProgressView[4];

    private ViewPagerFixed viewPager;
    private ViewPagerFixed.TabsView tabsView;
    private boolean loading = true;
    private String searchQuery = null;
    private long selectedDialogId = 0;
    private long minFileSizeBytes = 0;
    private int currentSortMode = SORT_DATE_DESC;
    private long totalVaultSize = 0;
    private long photosTotalSize = 0;
    private long videosTotalSize = 0;
    private long voiceTotalSize = 0;
    private long filesTotalSize = 0;
    private final HashMap<Long, Long> chatTotalSizes = new HashMap<>();
    private final HashMap<Long, Integer> chatFileCounts = new HashMap<>();

    private FrameLayout statsBar;
    private TextView statsTextView;

    private boolean selectionMode = false;
    private final HashSet<VaultItem> selectedItems = new HashSet<>();
    private NumberTextView selectedCountTextView;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        loadData();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
            if (pageAdapters[TAB_VOICE] != null) {
                pageAdapters[TAB_VOICE].notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (actionBar != null && actionBar.isActionModeShowed()) {
            if (invoked) {
                exitSelectionMode();
            }
            return false;
        }
        return super.onBackPressed(invoked);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramVaultMediaTitle", R.string.HundredGramVaultMediaTitle));
        actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));

        ActionBarMenu actionMode = actionBar.createActionMode();
        selectedCountTextView = new NumberTextView(actionMode.getContext());
        selectedCountTextView.setTextSize(18);
        selectedCountTextView.setTypeface(AndroidUtilities.bold());
        selectedCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));

        actionMode.addItemWithWidth(ITEM_SELECT_ALL, R.drawable.msg_select, AndroidUtilities.dp(54), LocaleController.getString("HundredGramVaultSelectAll", R.string.HundredGramVaultSelectAll));
        actionMode.addItemWithWidth(ITEM_BATCH_SAVE, R.drawable.msg_download, AndroidUtilities.dp(54), LocaleController.getString("HundredGramVaultSaveAll", R.string.HundredGramVaultSaveAll));
        actionMode.addItemWithWidth(ITEM_BATCH_ZIP, R.drawable.msg_share, AndroidUtilities.dp(54), LocaleController.getString("HundredGramVaultExportZip", R.string.HundredGramVaultExportZip));
        actionMode.addItemWithWidth(ITEM_BATCH_DELETE, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        exitSelectionMode();
                    } else {
                        finishFragment();
                    }
                } else if (id == ITEM_SELECT_MODE) {
                    enterSelectionMode(null);
                } else if (id == ITEM_SORT) {
                    showSortDialog();
                } else if (id == ITEM_FILTER) {
                    showChatsAndStorageBottomSheet();
                } else if (id == ITEM_SCAN) {
                    showDeepScanDialog();
                } else if (id == ITEM_SELECT_ALL) {
                    selectAllInCurrentTab();
                } else if (id == ITEM_BATCH_SAVE) {
                    batchSaveSelected();
                } else if (id == ITEM_BATCH_ZIP) {
                    batchExportZipSelected();
                } else if (id == ITEM_BATCH_DELETE) {
                    batchDeleteSelected();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(ITEM_SEARCH, R.drawable.outline_header_search).setIsSearchField(true);
        searchItem.setContentDescription(LocaleController.getString(R.string.Search));
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchCollapse() {
                searchQuery = null;
                filterAll();
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString().trim().toLowerCase();
                searchQuery = TextUtils.isEmpty(text) ? null : text;
                filterAll();
            }
        });

        ActionBarMenuItem selectModeItem = menu.addItem(ITEM_SELECT_MODE, R.drawable.msg_select);
        selectModeItem.setContentDescription(LocaleController.getString("HundredGramVaultSelectMode", R.string.HundredGramVaultSelectMode));

        ActionBarMenuItem sortItem = menu.addItem(ITEM_SORT, R.drawable.msg_actions);
        sortItem.setContentDescription(LocaleController.getString("HundredGramVaultSort", R.string.HundredGramVaultSort));

        ActionBarMenuItem filterItem = menu.addItem(ITEM_FILTER, R.drawable.msg_openprofile);
        filterItem.setContentDescription(LocaleController.getString("HundredGramVaultFilterByChat", R.string.HundredGramVaultFilterByChat));

        ActionBarMenuItem scanItem = menu.addItem(ITEM_SCAN, R.drawable.msg_retry);
        scanItem.setContentDescription(LocaleController.getString("HundredGramVaultDeepScanTitle", R.string.HundredGramVaultDeepScanTitle));

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        viewPager = new ViewPagerFixed(context);
        tabsView = viewPager.createTabsView(true, 8);
        tabsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        root.addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        statsBar = new FrameLayout(context);
        statsBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        statsBar.setOnClickListener(v -> showChatsAndStorageBottomSheet());

        statsTextView = new TextView(context);
        statsTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        statsTextView.setTypeface(AndroidUtilities.bold());
        statsTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statsTextView.setGravity(Gravity.CENTER);
        statsTextView.setSingleLine(true);
        statsTextView.setEllipsize(TextUtils.TruncateAt.END);
        statsBar.addView(statsTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 12, 0, 12, 0));
        root.addView(statsBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 30, Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 44, 0, 0));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        root.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP, 0, 74, 0, 0));

        root.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 75, 0, 0));

        viewPager.setAdapter(new ViewPagerFixed.Adapter() {
            @Override
            public int getItemCount() {
                return 4;
            }

            @Override
            public CharSequence getItemTitle(int position) {
                switch (position) {
                    case TAB_PHOTOS:
                        return LocaleController.getString("HundredGramVaultTabPhotos", R.string.HundredGramVaultTabPhotos) + (photosTotalSize > 0 ? " (" + AndroidUtilities.formatFileSize(photosTotalSize) + ")" : "");
                    case TAB_VIDEOS:
                        return LocaleController.getString("HundredGramVaultTabVideos", R.string.HundredGramVaultTabVideos) + (videosTotalSize > 0 ? " (" + AndroidUtilities.formatFileSize(videosTotalSize) + ")" : "");
                    case TAB_VOICE:
                        return LocaleController.getString("HundredGramVaultTabVoice", R.string.HundredGramVaultTabVoice) + (voiceTotalSize > 0 ? " (" + AndroidUtilities.formatFileSize(voiceTotalSize) + ")" : "");
                    case TAB_FILES:
                    default:
                        return LocaleController.getString("HundredGramVaultTabFiles", R.string.HundredGramVaultTabFiles) + (filesTotalSize > 0 ? " (" + AndroidUtilities.formatFileSize(filesTotalSize) + ")" : "");
                }
            }

            @Override
            public View createView(int viewType) {
                FrameLayout pageFrame = new FrameLayout(context);
                pageFrame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

                RecyclerListView listView = new RecyclerListView(context);
                listView.setVerticalScrollBarEnabled(true);
                listView.setClipToPadding(false);

                EmptyTextProgressView emptyView = new EmptyTextProgressView(context);
                emptyView.setText(LocaleController.getString("HundredGramVaultNoFiles", R.string.HundredGramVaultNoFiles));
                if (loading) {
                    emptyView.showProgress();
                } else {
                    emptyView.showTextView();
                }
                listView.setEmptyView(emptyView);

                pageFrame.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
                pageFrame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                return pageFrame;
            }

            @Override
            public void bindView(View view, int position, int viewType) {
                FrameLayout pageFrame = (FrameLayout) view;
                RecyclerListView listView = (RecyclerListView) pageFrame.getChildAt(1);
                EmptyTextProgressView emptyView = (EmptyTextProgressView) pageFrame.getChildAt(0);

                listViews[position] = listView;
                emptyViews[position] = emptyView;

                if (position == TAB_PHOTOS || position == TAB_VIDEOS) {
                    GridLayoutManager layoutManager = new GridLayoutManager(context, 3);
                    listView.setLayoutManager(layoutManager);
                    listView.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(2), AndroidUtilities.dp(2));
                } else {
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
                    listView.setLayoutManager(layoutManager);
                    listView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
                }

                PageAdapter adapter = new PageAdapter(position);
                pageAdapters[position] = adapter;
                listView.setAdapter(adapter);

                final int tabIndex = position;
                listView.addOnItemTouchListener(new VaultEdgeDragSelector(tabIndex));

                listView.setOnItemClickListener((v, pos) -> {
                    ArrayList<VaultItem> currentList = getListForTab(position);
                    if (pos < 0 || pos >= currentList.size()) return;
                    VaultItem item = currentList.get(pos);

                    if (selectionMode) {
                        toggleItemSelection(item);
                        return;
                    }

                    if (position == TAB_PHOTOS || position == TAB_VIDEOS) {
                        openPhotoViewer(currentList, pos);
                    } else if (position == TAB_VOICE) {
                        togglePlayAudio(item);
                    } else {
                        AndroidUtilities.openForView(item.file, item.fileName, null, getParentActivity(), null, false);
                    }
                });

                listView.setOnItemLongClickListener((v, pos) -> {
                    ArrayList<VaultItem> currentList = getListForTab(position);
                    if (pos >= 0 && pos < currentList.size()) {
                        if (v != null) {
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                        }
                        VaultItem item = currentList.get(pos);
                        if (selectionMode) {
                            toggleItemSelection(item);
                        } else {
                            showItemContextMenu(item);
                        }
                        return true;
                    }
                    return false;
                });
            }
        });

        return fragmentView = root;
    }

    private ArrayList<VaultItem> getListForTab(int tab) {
        switch (tab) {
            case TAB_PHOTOS:
                return photosList;
            case TAB_VIDEOS:
                return videosList;
            case TAB_VOICE:
                return voiceList;
            case TAB_FILES:
            default:
                return filesList;
        }
    }

    private void loadData() {
        loading = true;
        updateEmptyViews();

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            ArrayList<VaultItem> items = new ArrayList<>();
            ArrayList<Object[]> toPrune = new ArrayList<>();
            File vaultDir = HundredGramVaultManager.getInstance(currentAccount).getVaultDir();
            HashSet<Long> uniqueUids = new HashSet<>();

            SQLiteCursor cursor = null;
            try {
                cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT mid, uid, file_name, vault_path FROM hundredgram_vault");
                while (cursor.next()) {
                    int mid = cursor.intValue(0);
                    long uid = cursor.longValue(1);
                    String fileName = cursor.stringValue(2);
                    String vaultPath = cursor.stringValue(3);

                    File file = null;
                    if (!TextUtils.isEmpty(vaultPath)) {
                        file = new File(vaultPath);
                    }
                    if ((file == null || !file.exists()) && vaultDir != null && !TextUtils.isEmpty(fileName)) {
                        File fallback = new File(vaultDir, fileName);
                        if (fallback.exists()) {
                            file = fallback;
                            vaultPath = fallback.getAbsolutePath();
                        }
                    }

                    if (file != null && file.exists() && file.length() > 0) {
                        VaultItem it = new VaultItem();
                        it.mid = mid;
                        it.uid = uid;
                        it.fileName = fileName;
                        it.vaultPath = vaultPath;
                        it.file = file;
                        it.size = file.length();
                        it.date = file.lastModified();
                        items.add(it);
                        uniqueUids.add(uid);
                    } else {
                        toPrune.add(new Object[] { mid, uid, fileName });
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) cursor.dispose();
            }

            if (!toPrune.isEmpty()) {
                for (Object[] p : toPrune) {
                    try {
                        MessagesStorage.getInstance(currentAccount).deleteVaultEntry((Integer) p[0], (Long) p[1], (String) p[2]);
                    } catch (Throwable ignore) {}
                }
            }

            ArrayList<Long> unknownUsers = new ArrayList<>();
            ArrayList<Long> unknownChats = new ArrayList<>();
            for (Long uid : uniqueUids) {
                if (uid != 0 && MessagesController.getInstance(currentAccount).getUserOrChat(uid) == null) {
                    if (uid > 0) {
                        unknownUsers.add(uid);
                    } else {
                        unknownChats.add(uid);
                    }
                }
            }

            ArrayList<TLRPC.User> users = new ArrayList<>();
            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
            if (!unknownUsers.isEmpty()) {
                try {
                    MessagesStorage.getInstance(currentAccount).getUsersInternal(unknownUsers, users);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (!unknownChats.isEmpty()) {
                try {
                    MessagesStorage.getInstance(currentAccount).getChatsInternal(TextUtils.join(",", unknownChats), chats);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }

            applySort(items);

            AndroidUtilities.runOnUIThread(() -> {
                loading = false;
                if (!users.isEmpty()) {
                    MessagesController.getInstance(currentAccount).putUsers(users, true);
                }
                if (!chats.isEmpty()) {
                    MessagesController.getInstance(currentAccount).putChats(chats, true);
                }

                for (VaultItem it : items) {
                    TLObject obj = MessagesController.getInstance(currentAccount).getUserOrChat(it.uid);
                    it.chatTitle = DialogObject.setDialogPhotoTitle(null, obj);
                    if (TextUtils.isEmpty(it.chatTitle)) {
                        it.chatTitle = it.uid > 0 ? "User " + it.uid : "Chat " + it.uid;
                    }
                }

                allItems.clear();
                allItems.addAll(items);
                filterAll();
            });
        });
    }

    private void filterAll() {
        photosList.clear();
        videosList.clear();
        voiceList.clear();
        filesList.clear();

        totalVaultSize = 0;
        photosTotalSize = 0;
        videosTotalSize = 0;
        voiceTotalSize = 0;
        filesTotalSize = 0;
        chatTotalSizes.clear();
        chatFileCounts.clear();

        for (VaultItem it : allItems) {
            totalVaultSize += it.size;
            chatTotalSizes.put(it.uid, chatTotalSizes.getOrDefault(it.uid, 0L) + it.size);
            chatFileCounts.put(it.uid, chatFileCounts.getOrDefault(it.uid, 0) + 1);

            if (it.isPhoto()) photosTotalSize += it.size;
            else if (it.isVideo()) videosTotalSize += it.size;
            else if (it.isVoice()) voiceTotalSize += it.size;
            else filesTotalSize += it.size;

            if (selectedDialogId != 0 && it.uid != selectedDialogId) {
                continue;
            }

            if (minFileSizeBytes > 0 && it.size < minFileSizeBytes) {
                continue;
            }

            if (!TextUtils.isEmpty(searchQuery)) {
                boolean matchName = it.fileName != null && it.fileName.toLowerCase().contains(searchQuery);
                boolean matchChat = it.chatTitle != null && it.chatTitle.toLowerCase().contains(searchQuery);
                if (!matchName && !matchChat) {
                    continue;
                }
            }

            switch (it.getTabType()) {
                case TAB_PHOTOS:
                    photosList.add(it);
                    break;
                case TAB_VIDEOS:
                    videosList.add(it);
                    break;
                case TAB_VOICE:
                    voiceList.add(it);
                    break;
                case TAB_FILES:
                    filesList.add(it);
                    break;
            }
        }

        updateActionBarSubtitle();
        updateStatsBar();
        updateEmptyViews();

        if (viewPager != null) {
            viewPager.fillTabs(false);
        }

        for (int i = 0; i < pageAdapters.length; i++) {
            if (pageAdapters[i] != null) {
                pageAdapters[i].notifyDataSetChanged();
            }
        }
    }

    private void updateStatsBar() {
        if (statsTextView == null) return;
        if (loading) {
            statsTextView.setText(LocaleController.getString("Loading", R.string.Loading));
            return;
        }
        String pStr = "📷 " + AndroidUtilities.formatFileSize(photosTotalSize);
        String vStr = "🎬 " + AndroidUtilities.formatFileSize(videosTotalSize);
        String aStr = "🎙 " + AndroidUtilities.formatFileSize(voiceTotalSize);
        String fStr = "📁 " + AndroidUtilities.formatFileSize(filesTotalSize);
        statsTextView.setText(pStr + "   " + vStr + "   " + aStr + "   " + fStr);
    }

    private void updateActionBarSubtitle() {
        if (actionBar == null) return;
        if (loading) {
            actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));
            return;
        }

        String sizeStr = AndroidUtilities.formatFileSize(totalVaultSize);
        StringBuilder sb = new StringBuilder();
        if (selectedDialogId != 0) {
            TLObject obj = MessagesController.getInstance(currentAccount).getUserOrChat(selectedDialogId);
            String title = DialogObject.setDialogPhotoTitle(null, obj);
            if (TextUtils.isEmpty(title)) {
                title = selectedDialogId > 0 ? "User " + selectedDialogId : "Chat " + selectedDialogId;
            }
            sb.append(title);
        }
        if (minFileSizeBytes > 0) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append("> ").append(AndroidUtilities.formatFileSize(minFileSizeBytes));
        }
        if (sb.length() > 0) {
            sb.append(" • ").append(sizeStr);
            actionBar.setSubtitle(sb.toString());
        } else {
            actionBar.setSubtitle(sizeStr);
        }
    }

    private void updateEmptyViews() {
        for (int i = 0; i < emptyViews.length; i++) {
            EmptyTextProgressView ev = emptyViews[i];
            if (ev != null) {
                if (loading) {
                    ev.showProgress();
                } else {
                    ev.showTextView();
                }
            }
        }
    }

    private void openPhotoViewer(ArrayList<VaultItem> list, int selectedIndex) {
        if (list.isEmpty() || selectedIndex < 0 || selectedIndex >= list.size()) {
            return;
        }
        PhotoViewer.getInstance().setParentActivity(this);
        ArrayList<Object> photoEntries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            VaultItem it = list.get(i);
            photoEntries.add(new MediaController.PhotoEntry(0, 0, 0, it.vaultPath, 0, it.isVideo(), 0, 0, 0));
        }
        PhotoViewer.getInstance().openPhotoForSelect(photoEntries, selectedIndex, PhotoViewer.SELECT_TYPE_NO_SELECT, false, new PhotoViewer.EmptyPhotoViewerProvider(), null);
    }

    private void togglePlayAudio(VaultItem item) {
        MessageObject mo = item.getMessageObject(currentAccount);
        if (MediaController.getInstance().isPlayingMessage(mo)) {
            if (!MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().pauseMessage(mo);
            } else {
                MediaController.getInstance().playMessage(mo);
            }
        } else {
            MediaController.getInstance().playMessage(mo);
        }
        if (pageAdapters[TAB_VOICE] != null) {
            pageAdapters[TAB_VOICE].notifyDataSetChanged();
        }
    }

    private void showItemContextMenu(VaultItem item) {
        if (getParentActivity() == null || item == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(item.fileName != null ? item.fileName : item.chatTitle, true);

        ArrayList<CharSequence> items = new ArrayList<>();
        ArrayList<Integer> icons = new ArrayList<>();
        final ArrayList<Integer> actions = new ArrayList<>();

        items.add(LocaleController.getString("HundredGramVaultShare", R.string.HundredGramVaultShare));
        icons.add(R.drawable.msg_share);
        actions.add(0);

        if (item.isPhoto() || item.isVideo()) {
            items.add(LocaleController.getString("HundredGramVaultSaveGallery", R.string.HundredGramVaultSaveGallery));
            icons.add(R.drawable.msg_gallery);
            actions.add(1);
        } else {
            items.add(LocaleController.getString("HundredGramVaultSaveDownloads", R.string.HundredGramVaultSaveDownloads));
            icons.add(R.drawable.msg_download);
            actions.add(1);
        }

        if (item.mid > 0 && item.uid != 0) {
            items.add(LocaleController.getString("HundredGramVaultGoToMessage", R.string.HundredGramVaultGoToMessage));
            icons.add(R.drawable.msg_message);
            actions.add(2);
        }

        items.add(LocaleController.getString("HundredGramVaultDetails", R.string.HundredGramVaultDetails));
        icons.add(R.drawable.msg_info);
        actions.add(5);

        items.add(LocaleController.getString("HundredGramVaultDelete", R.string.HundredGramVaultDelete));
        icons.add(R.drawable.msg_delete);
        actions.add(3);

        items.add(LocaleController.getString("HundredGramVaultSelectMode", R.string.HundredGramVaultSelectMode));
        icons.add(R.drawable.msg_select);
        actions.add(4);

        builder.setItems(items.toArray(new CharSequence[0]), AndroidUtilities.toIntArray(icons), (dialog, which) -> {
            int action = actions.get(which);
            if (action == 0) {
                shareVaultFile(item);
            } else if (action == 1) {
                saveVaultFile(item);
            } else if (action == 2) {
                goToMessageInChat(item);
            } else if (action == 5) {
                showFileDetailsDialog(item);
            } else if (action == 3) {
                showDeleteFilePrompt(item);
            } else if (action == 4) {
                enterSelectionMode(item);
            }
        });

        showDialog(builder.create());
    }

    private void showFileDetailsDialog(VaultItem item) {
        if (getParentActivity() == null || item == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultDetails", R.string.HundredGramVaultDetails), true);

        ArrayList<CharSequence> titles = new ArrayList<>();
        ArrayList<CharSequence> subtitles = new ArrayList<>();
        ArrayList<Integer> detailIcons = new ArrayList<>();

        // File Name
        titles.add(LocaleController.getString("FileName", R.string.FileName));
        subtitles.add(item.fileName != null ? item.fileName : "-");
        detailIcons.add(R.drawable.msg_download);

        // File Size
        titles.add(LocaleController.getString("FileSize", R.string.FileSize));
        subtitles.add(AndroidUtilities.formatFileSize(item.size));
        detailIcons.add(R.drawable.msg_download);

        // Date Saved
        titles.add(LocaleController.getString("Date", R.string.Date));
        subtitles.add(LocaleController.formatDateTime(item.date / 1000L, false));
        detailIcons.add(R.drawable.msg_calendar2);

        // Chat / Source Dialog
        titles.add("Чат / Источник");
        String chatDisplay = item.chatTitle != null ? item.chatTitle : ("ID: " + item.uid);
        if (item.uid != 0) {
            chatDisplay += " (" + item.uid + ")";
        }
        subtitles.add(chatDisplay);
        detailIcons.add(item.uid > 0 ? R.drawable.msg_usersearch : R.drawable.msg_channel);

        // Message ID
        if (item.mid > 0) {
            titles.add("ID сообщения");
            subtitles.add(String.valueOf(item.mid));
            detailIcons.add(R.drawable.msg_message);
        }

        // Vault Local Path
        if (item.vaultPath != null) {
            titles.add(LocaleController.getString("FilePath", R.string.FilePath));
            subtitles.add(item.vaultPath);
            detailIcons.add(R.drawable.msg_openin);
        }

        CharSequence[] itemsArray = new CharSequence[titles.size()];
        for (int i = 0; i < titles.size(); i++) {
            itemsArray[i] = titles.get(i) + "\n" + subtitles.get(i);
        }

        builder.setItems(itemsArray, AndroidUtilities.toIntArray(detailIcons), (dialog, which) -> {
            if (which >= 0 && which < subtitles.size()) {
                CharSequence textToCopy = subtitles.get(which);
                if (!TextUtils.isEmpty(textToCopy)) {
                    AndroidUtilities.addToClipboard(textToCopy.toString());
                    BulletinFactory.of(HundredGramVaultViewerActivity.this).createCopyBulletin(
                            LocaleController.getString("TextCopied", R.string.TextCopied)
                    ).show();
                }
            }
        });

        showDialog(builder.create());
    }

    private void shareVaultFile(VaultItem item) {
        if (getParentActivity() == null || item.file == null || !item.file.exists()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            String ext = FileLoader.getFileExtension(item.file);
            String mime = !TextUtils.isEmpty(ext) ? android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase()) : null;
            intent.setType(mime != null ? mime : "*/*");
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    org.telegram.messenger.ApplicationLoader.applicationContext,
                    org.telegram.messenger.ApplicationLoader.getApplicationId() + ".provider",
                    item.file
            );
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getParentActivity().startActivity(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)));
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void saveVaultFile(VaultItem item) {
        if (getParentActivity() == null || item.file == null || !item.file.exists()) return;
        int type = item.isPhoto() ? 0 : (item.isVideo() ? 1 : (item.isVoice() ? 3 : 2));
        MediaController.saveFile(item.file.getAbsolutePath(), getParentActivity(), type, item.fileName, null, uri -> {
            if (uri != null) {
                BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                        R.raw.ic_save_to_gallery,
                        LocaleController.getString("HundredGramVaultFileSaved", R.string.HundredGramVaultFileSaved)
                ).show();
            }
        }, true);
    }

    private void goToMessageInChat(VaultItem item) {
        if (item.mid <= 0 || item.uid == 0) return;
        Bundle args = new Bundle();
        if (item.uid > 0) {
            args.putLong("user_id", item.uid);
        } else {
            args.putLong("chat_id", -item.uid);
        }
        args.putInt("message_id", item.mid);
        presentFragment(new ChatActivity(args));
    }

    private void showDeleteFilePrompt(VaultItem item) {
        if (getParentActivity() == null || item == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultDelete", R.string.HundredGramVaultDelete));
        builder.setMessage(LocaleController.getString("HundredGramVaultDeleteFileConfirm", R.string.HundredGramVaultDeleteFileConfirm));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> {
            HundredGramVaultManager.getInstance(currentAccount).deleteVaultFile(item.mid, item.uid, item.fileName, item.vaultPath);
            allItems.remove(item);
            filterAll();
            BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                    R.raw.ic_delete,
                    LocaleController.getString("HundredGramVaultDeletedFile", R.string.HundredGramVaultDeletedFile)
            ).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void enterSelectionMode(VaultItem initialItem) {
        enterSelectionMode(initialItem, null);
    }

    private void enterSelectionMode(VaultItem initialItem, View initialView) {
        selectionMode = true;
        selectedItems.clear();
        if (initialItem != null) {
            selectedItems.add(initialItem);
        }
        if (actionBar != null) {
            actionBar.showActionMode();
        }
        updateSelectionCount();

        int currentTab = viewPager != null ? viewPager.getCurrentPosition() : 0;
        RecyclerListView lv = listViews[currentTab];
        if (lv != null) {
            for (int i = 0; i < lv.getChildCount(); i++) {
                View child = lv.getChildAt(i);
                if (child instanceof VaultMediaCell) {
                    ((VaultMediaCell) child).setSelectionMode(true);
                } else if (child instanceof VaultFileCell) {
                    ((VaultFileCell) child).setSelectionMode(true);
                }
            }
        }
        if (initialView instanceof VaultMediaCell) {
            ((VaultMediaCell) initialView).setChecked(true, true);
        } else if (initialView instanceof VaultFileCell) {
            ((VaultFileCell) initialView).setChecked(true, true);
        } else {
            notifyAllAdapters();
        }
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectedItems.clear();
        if (actionBar != null) {
            actionBar.hideActionMode();
        }
        notifyAllAdapters();
    }

    private void toggleItemSelection(VaultItem item) {
        if (item == null) return;
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
        } else {
            selectedItems.add(item);
        }
        updateSelectionCount();
        notifyAllAdapters();
    }

    private void updateSelectionCount() {
        if (selectedCountTextView != null) {
            selectedCountTextView.setNumber(selectedItems.size(), true);
        }
    }

    private void notifyAllAdapters() {
        for (int i = 0; i < pageAdapters.length; i++) {
            if (pageAdapters[i] != null) {
                pageAdapters[i].notifyDataSetChanged();
            }
        }
    }

    private void selectAllInCurrentTab() {
        int currentTab = viewPager != null ? viewPager.getCurrentPosition() : 0;
        ArrayList<VaultItem> currentList = getListForTab(currentTab);
        if (currentList.isEmpty()) return;

        boolean allSelected = true;
        for (VaultItem it : currentList) {
            if (!selectedItems.contains(it)) {
                allSelected = false;
                break;
            }
        }

        if (allSelected) {
            for (VaultItem it : currentList) {
                selectedItems.remove(it);
            }
        } else {
            selectedItems.addAll(currentList);
        }
        updateSelectionCount();
        notifyAllAdapters();
    }

    private void batchSaveSelected() {
        if (selectedItems.isEmpty() || getParentActivity() == null) {
            return;
        }
        ArrayList<VaultItem> itemsToSave = new ArrayList<>(selectedItems);
        int total = itemsToSave.size();
        int[] savedCount = new int[1];

        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setMessage(LocaleController.getString("Loading", R.string.Loading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        showDialog(progressDialog);

        for (int i = 0; i < total; i++) {
            VaultItem item = itemsToSave.get(i);
            int type = item.isPhoto() ? 0 : (item.isVideo() ? 1 : (item.isVoice() ? 3 : 2));
            MediaController.saveFile(item.file.getAbsolutePath(), getParentActivity(), type, item.fileName, null, uri -> {
                savedCount[0]++;
                if (savedCount[0] >= total) {
                    try {
                        progressDialog.dismiss();
                    } catch (Throwable ignore) {}
                    BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                            R.raw.ic_save_to_gallery,
                            LocaleController.formatString("HundredGramVaultBatchSaved", R.string.HundredGramVaultBatchSaved, total)
                    ).show();
                    exitSelectionMode();
                }
            }, false);
        }
    }

    private void batchExportZipSelected() {
        if (selectedItems.isEmpty() || getParentActivity() == null) {
            return;
        }
        ArrayList<File> files = new ArrayList<>();
        for (VaultItem it : selectedItems) {
            if (it.file != null && it.file.exists() && it.file.length() > 0) {
                files.add(it.file);
            }
        }
        if (files.isEmpty()) return;

        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setMessage(LocaleController.getString("HundredGramVaultExportingZip", R.string.HundredGramVaultExportingZip));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        showDialog(progressDialog);

        File cacheDir = org.telegram.messenger.ApplicationLoader.applicationContext.getCacheDir();
        File destZip = new File(cacheDir, "100gram_vault_selection_" + System.currentTimeMillis() + ".zip");
        HundredGramVaultManager.getInstance(currentAccount).exportVaultToZip(destZip, files, progress -> {
            if (progressDialog.isShowing()) {
                progressDialog.setMessage(LocaleController.getString("HundredGramVaultExportingZip", R.string.HundredGramVaultExportingZip) + " (" + (int)(progress * 100) + "%)");
            }
        }, zipFile -> {
            try {
                progressDialog.dismiss();
            } catch (Throwable ignore) {}
            if (zipFile != null && zipFile.exists() && getParentActivity() != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/zip");
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            org.telegram.messenger.ApplicationLoader.applicationContext,
                            org.telegram.messenger.ApplicationLoader.getApplicationId() + ".provider",
                            zipFile
                    );
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getParentActivity().startActivity(Intent.createChooser(intent, LocaleController.getString("HundredGramVaultExportZip", R.string.HundredGramVaultExportZip)));
                    BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                            R.raw.done,
                            LocaleController.getString("HundredGramVaultZipSuccess", R.string.HundredGramVaultZipSuccess)
                    ).show();
                    exitSelectionMode();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        });
    }

    private void batchDeleteSelected() {
        if (selectedItems.isEmpty() || getParentActivity() == null) {
            return;
        }
        final ArrayList<VaultItem> itemsToDelete = new ArrayList<>(selectedItems);
        final int count = itemsToDelete.size();

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultDelete", R.string.HundredGramVaultDelete));
        builder.setMessage(LocaleController.formatString("HundredGramVaultBatchDeleteConfirm", R.string.HundredGramVaultBatchDeleteConfirm, count));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (d, w) -> {
            for (VaultItem item : itemsToDelete) {
                HundredGramVaultManager.getInstance(currentAccount).deleteVaultFile(item.mid, item.uid, item.fileName, item.vaultPath);
                allItems.remove(item);
            }
            exitSelectionMode();
            filterAll();
            BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                    R.raw.ic_delete,
                    LocaleController.formatString("HundredGramVaultBatchDeleted", R.string.HundredGramVaultBatchDeleted, count)
            ).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void showSortDialog() {
        if (getParentActivity() == null) return;
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultSort", R.string.HundredGramVaultSort), true);

        CharSequence[] options = new CharSequence[] {
                (currentSortMode == SORT_DATE_DESC ? "✓ " : "") + LocaleController.getString("HundredGramVaultSortDateDesc", R.string.HundredGramVaultSortDateDesc),
                (currentSortMode == SORT_DATE_ASC ? "✓ " : "") + LocaleController.getString("HundredGramVaultSortDateAsc", R.string.HundredGramVaultSortDateAsc),
                (currentSortMode == SORT_SIZE_DESC ? "✓ " : "") + LocaleController.getString("HundredGramVaultSortSizeDesc", R.string.HundredGramVaultSortSizeDesc),
                (currentSortMode == SORT_SIZE_ASC ? "✓ " : "") + LocaleController.getString("HundredGramVaultSortSizeAsc", R.string.HundredGramVaultSortSizeAsc),
                (currentSortMode == SORT_NAME_ASC ? "✓ " : "") + LocaleController.getString("HundredGramVaultSortNameAsc", R.string.HundredGramVaultSortNameAsc)
        };

        builder.setItems(options, (dialog, which) -> {
            currentSortMode = which;
            applySort(allItems);
            filterAll();
        });
        showDialog(builder.create());
    }

    private void applySort(ArrayList<VaultItem> list) {
        if (list == null || list.isEmpty()) return;
        switch (currentSortMode) {
            case SORT_DATE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.date, a.date));
                break;
            case SORT_DATE_ASC:
                Collections.sort(list, (a, b) -> Long.compare(a.date, b.date));
                break;
            case SORT_SIZE_DESC:
                Collections.sort(list, (a, b) -> Long.compare(b.size, a.size));
                break;
            case SORT_SIZE_ASC:
                Collections.sort(list, (a, b) -> Long.compare(a.size, b.size));
                break;
            case SORT_NAME_ASC:
                Collections.sort(list, (a, b) -> {
                    String an = a.fileName != null ? a.fileName : "";
                    String bn = b.fileName != null ? b.fileName : "";
                    return an.compareToIgnoreCase(bn);
                });
                break;
        }
    }

    private void showChatsAndStorageBottomSheet() {
        if (getParentActivity() == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultStorageAndFilters", R.string.HundredGramVaultStorageAndFilters), true);

        LinearLayout container = new LinearLayout(getParentActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        LinearLayout statsCard = new LinearLayout(getParentActivity());
        statsCard.setOrientation(LinearLayout.VERTICAL);
        statsCard.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(12), Theme.getColor(Theme.key_windowBackgroundGray)));
        statsCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        TextView totalVaultText = new TextView(getParentActivity());
        totalVaultText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        totalVaultText.setTypeface(AndroidUtilities.bold());
        totalVaultText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        totalVaultText.setText(LocaleController.formatString("HundredGramVaultTotalStats", R.string.HundredGramVaultTotalStats, AndroidUtilities.formatFileSize(totalVaultSize), allItems.size()));
        statsCard.addView(totalVaultText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        TextView typesDetailText = new TextView(getParentActivity());
        typesDetailText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        typesDetailText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        String typesLine1 = "📷 " + LocaleController.getString("HundredGramVaultTabPhotos", R.string.HundredGramVaultTabPhotos) + ": " + AndroidUtilities.formatFileSize(photosTotalSize) + " (" + photosList.size() + ")"
                + "  •  🎬 " + LocaleController.getString("HundredGramVaultTabVideos", R.string.HundredGramVaultTabVideos) + ": " + AndroidUtilities.formatFileSize(videosTotalSize) + " (" + videosList.size() + ")";
        String typesLine2 = "🎙 " + LocaleController.getString("HundredGramVaultTabVoice", R.string.HundredGramVaultTabVoice) + ": " + AndroidUtilities.formatFileSize(voiceTotalSize) + " (" + voiceList.size() + ")"
                + "  •  📁 " + LocaleController.getString("HundredGramVaultTabFiles", R.string.HundredGramVaultTabFiles) + ": " + AndroidUtilities.formatFileSize(filesTotalSize) + " (" + filesList.size() + ")";
        typesDetailText.setText(typesLine1 + "\n" + typesLine2);
        statsCard.addView(typesDetailText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        container.addView(statsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 0, 16, 12));

        HeaderCell sizeHeader = new HeaderCell(getParentActivity());
        sizeHeader.setText(LocaleController.getString("HundredGramVaultFilterSize", R.string.HundredGramVaultFilterSize));
        container.addView(sizeHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout sizeRow = new LinearLayout(getParentActivity());
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        long[] sizes = new long[] { 0, 10L * 1024L * 1024L, 50L * 1024L * 1024L, 100L * 1024L * 1024L };
        String[] sizeLabels = new String[] {
                LocaleController.getString("HundredGramVaultFilterSizeAll", R.string.HundredGramVaultFilterSizeAll),
                LocaleController.getString("HundredGramVaultFilterSize10Mb", R.string.HundredGramVaultFilterSize10Mb),
                LocaleController.getString("HundredGramVaultFilterSize50Mb", R.string.HundredGramVaultFilterSize50Mb),
                LocaleController.getString("HundredGramVaultFilterSize100Mb", R.string.HundredGramVaultFilterSize100Mb)
        };
        for (int i = 0; i < sizes.length; i++) {
            final long s = sizes[i];
            TextView chip = new TextView(getParentActivity());
            chip.setText(sizeLabels[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            boolean isSelected = minFileSizeBytes == s;
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(8), isSelected ? Theme.getColor(Theme.key_featuredStickers_addButton) : Theme.getColor(Theme.key_windowBackgroundGray)));
            chip.setTextColor(isSelected ? Color.WHITE : Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
            chip.setOnClickListener(v -> {
                minFileSizeBytes = s;
                filterAll();
                builder.getDismissRunnable().run();
            });
            sizeRow.addView(chip, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        }
        HorizontalScrollView hsv = new HorizontalScrollView(getParentActivity());
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.addView(sizeRow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START, 16, 0, 16, 0));
        container.addView(hsv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        HeaderCell chatsHeader = new HeaderCell(getParentActivity());
        chatsHeader.setText(LocaleController.getString("HundredGramVaultByChats", R.string.HundredGramVaultByChats));
        container.addView(chatsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ArrayList<Long> uids = new ArrayList<>(chatTotalSizes.keySet());
        Collections.sort(uids, (a, b) -> Long.compare(chatTotalSizes.getOrDefault(b, 0L), chatTotalSizes.getOrDefault(a, 0L)));

        ArrayList<CharSequence> chatDisplayList = new ArrayList<>();
        ArrayList<Long> chatDialogIdList = new ArrayList<>();

        chatDisplayList.add((selectedDialogId == 0 ? "✓ " : "") + LocaleController.getString("HundredGramVaultAllChats", R.string.HundredGramVaultAllChats)
                + " — " + LocaleController.formatString("HundredGramVaultChatSpace", R.string.HundredGramVaultChatSpace, allItems.size(), AndroidUtilities.formatFileSize(totalVaultSize)));
        chatDialogIdList.add(0L);

        for (Long uid : uids) {
            TLObject obj = MessagesController.getInstance(currentAccount).getUserOrChat(uid);
            String title = DialogObject.setDialogPhotoTitle(null, obj);
            if (TextUtils.isEmpty(title)) {
                title = uid > 0 ? "User " + uid : "Chat " + uid;
            }
            long cSize = chatTotalSizes.getOrDefault(uid, 0L);
            int cCount = chatFileCounts.getOrDefault(uid, 0);
            boolean isCur = selectedDialogId == uid;
            chatDisplayList.add((isCur ? "✓ " : "") + title + " — " + LocaleController.formatString("HundredGramVaultChatSpace", R.string.HundredGramVaultChatSpace, cCount, AndroidUtilities.formatFileSize(cSize)));
            chatDialogIdList.add(uid);
        }

        for (int i = 0; i < chatDisplayList.size(); i++) {
            final long did = chatDialogIdList.get(i);
            TextView itemText = new TextView(getParentActivity());
            itemText.setText(chatDisplayList.get(i));
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            itemText.setTextColor(Theme.getColor(did == selectedDialogId ? Theme.key_featuredStickers_addButton : Theme.key_windowBackgroundWhiteBlackText));
            itemText.setBackground(Theme.getSelectorDrawable(false));
            itemText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
            itemText.setOnClickListener(v -> {
                selectedDialogId = did;
                filterAll();
                builder.getDismissRunnable().run();
            });
            container.addView(itemText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        NestedScrollView scrollView = new NestedScrollView(getParentActivity());
        scrollView.addView(container);
        builder.setCustomView(scrollView);
        showDialog(builder.create());
    }

    private void showDeepScanDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultDeepScanTitle", R.string.HundredGramVaultDeepScanTitle));
        CharSequence[] options = new CharSequence[] {
            LocaleController.formatString("HundredGramVaultScanPerChat", R.string.HundredGramVaultScanPerChat, 500),
            LocaleController.formatString("HundredGramVaultScanPerChat", R.string.HundredGramVaultScanPerChat, 1000),
            LocaleController.formatString("HundredGramVaultScanPerChat", R.string.HundredGramVaultScanPerChat, 5000),
            LocaleController.getString("HundredGramScanDepthAll", R.string.HundredGramScanDepthAll)
        };
        int[] limits = new int[] { 500, 1000, 5000, 0 };
        builder.setItems(options, (dialog, which) -> {
            startGlobalDeepScan(limits[which]);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void startGlobalDeepScan(int limitPerChat) {
        if (getParentActivity() == null) return;
        final AlertDialog[] progressDialog = new AlertDialog[1];
        try {
            progressDialog[0] = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
            progressDialog[0].setMessage(LocaleController.getString("HundredGramVaultDeepScanStarted", R.string.HundredGramVaultDeepScanStarted));
            progressDialog[0].setCanceledOnTouchOutside(false);
            progressDialog[0].setCancelable(true);
            showDialog(progressDialog[0]);
        } catch (Throwable e) {
            FileLog.e(e);
        }

        BulletinFactory.of(this).createSimpleBulletin(
            R.drawable.msg_download,
            LocaleController.getString("HundredGramVaultDeepScanStarted", R.string.HundredGramVaultDeepScanStarted)
        ).show();

        org.telegram.messenger.HundredGramBackgroundArchiver.getInstance(currentAccount).forceDeepScanAll(limitPerChat, progress -> {
            if (progress == null) {
                return;
            }
            if (progress.completed) {
                if (progressDialog[0] != null) {
                    try {
                        progressDialog[0].dismiss();
                    } catch (Throwable ignore) {}
                    progressDialog[0] = null;
                }
                if (getParentActivity() != null) {
                    BulletinFactory.of(HundredGramVaultViewerActivity.this).createSimpleBulletin(
                        R.drawable.msg_download,
                        LocaleController.formatString("HundredGramVaultDeepScanFinished", R.string.HundredGramVaultDeepScanFinished, progress.mediaEnqueued)
                    ).show();
                    loadData();
                }
            } else {
                if (progressDialog[0] != null && progressDialog[0].isShowing()) {
                    progressDialog[0].setMessage(LocaleController.formatString(
                        "HundredGramVaultDeepScanProgress",
                        R.string.HundredGramVaultDeepScanProgress,
                        progress.currentChat,
                        progress.totalChats,
                        progress.mediaEnqueued
                    ));
                }
            }
        });
    }

    private class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int tabIndex;

        public PageAdapter(int tabIndex) {
            this.tabIndex = tabIndex;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (tabIndex == TAB_PHOTOS || tabIndex == TAB_VIDEOS) {
                view = new VaultMediaCell(parent.getContext());
            } else {
                view = new VaultFileCell(parent.getContext(), tabIndex == TAB_VOICE);
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ArrayList<VaultItem> list = getListForTab(tabIndex);
            if (position < 0 || position >= list.size()) return;
            VaultItem item = list.get(position);
            boolean isSelected = selectedItems.contains(item);

            if (holder.itemView instanceof VaultMediaCell) {
                ((VaultMediaCell) holder.itemView).setItem(item, selectionMode, isSelected);
            } else if (holder.itemView instanceof VaultFileCell) {
                ((VaultFileCell) holder.itemView).setItem(item, selectionMode, isSelected);
            }
        }

        @Override
        public int getItemCount() {
            return getListForTab(tabIndex).size();
        }
    }

    public class VaultMediaCell extends FrameLayout {
        private final BackupImageView imageView;
        private final View selectionOverlay;
        private final FrameLayout videoBadge;
        private final TextView videoDurationText;
        private final CheckBox2 checkBox;

        public VaultMediaCell(@NonNull Context context) {
            super(context);

            imageView = new BackupImageView(context);
            imageView.setAspectFit(false);
            addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 1, 1, 1, 1));

            selectionOverlay = new View(context);
            selectionOverlay.setBackgroundColor(0x55000000);
            selectionOverlay.setVisibility(View.GONE);
            addView(selectionOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            videoBadge = new FrameLayout(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x88000000);
            bg.setCornerRadius(AndroidUtilities.dp(10));
            videoBadge.setBackground(bg);
            videoBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));

            LinearLayout badgeLayout = new LinearLayout(context);
            badgeLayout.setOrientation(LinearLayout.HORIZONTAL);
            badgeLayout.setGravity(Gravity.CENTER_VERTICAL);

            ImageView playIcon = new ImageView(context);
            playIcon.setImageResource(R.drawable.ic_play);
            playIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            badgeLayout.addView(playIcon, LayoutHelper.createLinear(12, 12, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));

            videoDurationText = new TextView(context);
            videoDurationText.setTextColor(Color.WHITE);
            videoDurationText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            videoDurationText.setText("VIDEO");
            badgeLayout.addView(videoDurationText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            videoBadge.addView(badgeLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
            addView(videoBadge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START, 6, 0, 0, 6));

            checkBox = new CheckBox2(context, 21);
            checkBox.setColor(Theme.key_checkbox, Theme.key_checkboxDisabled, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(true);
            checkBox.setVisibility(View.GONE);
            addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.TOP | Gravity.END, 0, 6, 6, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY));
        }

        public void setSelectionMode(boolean active) {
            checkBox.setVisibility(active ? View.VISIBLE : View.GONE);
        }

        public void setChecked(boolean isSelected, boolean animated) {
            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(isSelected, animated);
        }

        public void setItem(VaultItem item, boolean selectionMode, boolean isSelected) {
            if (item == null || item.vaultPath == null) return;
            imageView.setImage(ImageLocation.getForPath(item.vaultPath), "150_150", (Drawable) null, null);
            if (item.isVideo()) {
                videoBadge.setVisibility(View.VISIBLE);
            } else {
                videoBadge.setVisibility(View.GONE);
            }
            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            checkBox.setChecked(isSelected, false);
        }
    }

    public class VaultFileCell extends FrameLayout {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final CheckBox2 checkBox;
        private final ImageView optionsView;
        private final boolean isVoice;
        private VaultItem currentItem;

        public VaultFileCell(@NonNull Context context, boolean isVoice) {
            super(context);
            this.isVoice = isVoice;
            setBackground(Theme.getSelectorDrawable(false));

            FrameLayout iconContainer = new FrameLayout(context);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Theme.getColor(isVoice ? Theme.key_featuredStickers_addButton : Theme.key_chat_inBubble));
            iconContainer.setBackground(bg);

            iconView = new ImageView(context);
            if (isVoice) {
                iconView.setImageResource(R.drawable.ic_play);
                iconView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            } else {
                iconView.setImageResource(R.drawable.media_doc_blue);
            }
            iconContainer.addView(iconView, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
            addView(iconContainer, LayoutHelper.createFrame(44, 44, Gravity.CENTER_VERTICAL | Gravity.START, 16, 0, 0, 0));

            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setSingleLine(true);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 72, 0, 52, 0));

            checkBox = new CheckBox2(context, 21);
            checkBox.setColor(Theme.key_checkbox, Theme.key_checkboxDisabled, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(true);
            checkBox.setVisibility(View.GONE);
            addView(checkBox, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 16, 0));

            optionsView = new ImageView(context);
            optionsView.setImageResource(R.drawable.ic_ab_other);
            optionsView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.SRC_IN);
            optionsView.setScaleType(ImageView.ScaleType.CENTER);
            optionsView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1));
            optionsView.setOnClickListener(v -> {
                if (currentItem != null) {
                    showItemContextMenu(currentItem);
                }
            });
            addView(optionsView, LayoutHelper.createFrame(40, 40, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 8, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
        }

        public void setSelectionMode(boolean active) {
            checkBox.setVisibility(active ? View.VISIBLE : View.GONE);
            optionsView.setVisibility(active ? View.GONE : View.VISIBLE);
        }

        public void setChecked(boolean isSelected, boolean animated) {
            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            optionsView.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
            checkBox.setChecked(isSelected, animated);
        }

        public void setItem(VaultItem item, boolean selectionMode, boolean isSelected) {
            this.currentItem = item;
            if (item == null) return;

            titleView.setText(item.fileName != null ? item.fileName : "file");
            String sizeStr = AndroidUtilities.formatFileSize(item.size);
            String dateStr = LocaleController.formatDateForBan(item.date / 1000L);
            subtitleView.setText(sizeStr + " • " + dateStr + " • " + (item.chatTitle != null ? item.chatTitle : ""));

            if (isVoice) {
                MessageObject mo = item.getMessageObject(currentAccount);
                boolean playing = MediaController.getInstance().isPlayingMessage(mo) && !MediaController.getInstance().isMessagePaused();
                iconView.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
                iconView.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }

            checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            optionsView.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
            checkBox.setChecked(isSelected, false);
        }
    }

    public class VaultEdgeDragSelector implements RecyclerView.OnItemTouchListener {
        private final int tabIndex;
        private float downX, downY;
        private boolean isDragSelecting;
        private int lastSelectedPos = -1;
        private RecyclerView currentRv;
        private boolean inTopHotspot, inBottomHotspot;
        private static final int AUTO_SCROLL_DELAY = 16;

        private final Runnable autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentRv == null || !isDragSelecting) return;
                if (inTopHotspot) {
                    currentRv.scrollBy(0, -AndroidUtilities.dp(12));
                    processTouchAt(currentRv, downX, AndroidUtilities.dp(85));
                    AndroidUtilities.runOnUIThread(this, AUTO_SCROLL_DELAY);
                } else if (inBottomHotspot) {
                    currentRv.scrollBy(0, AndroidUtilities.dp(12));
                    processTouchAt(currentRv, downX, currentRv.getHeight() - AndroidUtilities.dp(85));
                    AndroidUtilities.runOnUIThread(this, AUTO_SCROLL_DELAY);
                }
            }
        };

        public VaultEdgeDragSelector(int tabIndex) {
            this.tabIndex = tabIndex;
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            currentRv = rv;
            float x = e.getX();
            float y = e.getY();
            int action = e.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                downX = x;
                downY = y;
                isDragSelecting = false;
                lastSelectedPos = -1;
                inTopHotspot = false;
                inBottomHotspot = false;
                AndroidUtilities.cancelRunOnUIThread(autoScrollRunnable);
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (isDragSelecting) {
                    return true;
                }
                float dx = x - downX;
                float dy = y - downY;
                float edgeThreshold = rv.getWidth() - AndroidUtilities.dp(64);
                boolean reachedEdge = (x >= edgeThreshold) || (dx > AndroidUtilities.dp(50) && x >= rv.getWidth() - AndroidUtilities.dp(80));
                boolean verticalMovement = Math.abs(dy) > AndroidUtilities.dp(14);

                if (reachedEdge && verticalMovement) {
                    isDragSelecting = true;
                    rv.requestDisallowInterceptTouchEvent(true);
                    if (viewPager != null) {
                        viewPager.requestDisallowInterceptTouchEvent(true);
                    }
                    if (parentLayout != null && parentLayout.getView() != null) {
                        parentLayout.getView().requestDisallowInterceptTouchEvent(true);
                    }
                    if (!selectionMode) {
                        enterSelectionMode(null);
                    }
                    processTouchAt(rv, downX, downY);
                    processTouchAt(rv, x, y);
                    return true;
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                stopDragSelection();
            }
            return isDragSelecting;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            currentRv = rv;
            float x = e.getX();
            float y = e.getY();
            int action = e.getActionMasked();

            if (action == MotionEvent.ACTION_MOVE) {
                if (!isDragSelecting) return;

                int hotspot = AndroidUtilities.dp(80);
                if (y < hotspot) {
                    inBottomHotspot = false;
                    if (!inTopHotspot) {
                        inTopHotspot = true;
                        AndroidUtilities.runOnUIThread(autoScrollRunnable);
                    }
                } else if (y > rv.getHeight() - hotspot) {
                    inTopHotspot = false;
                    if (!inBottomHotspot) {
                        inBottomHotspot = true;
                        AndroidUtilities.runOnUIThread(autoScrollRunnable);
                    }
                } else {
                    inTopHotspot = false;
                    inBottomHotspot = false;
                    AndroidUtilities.cancelRunOnUIThread(autoScrollRunnable);
                }

                processTouchAt(rv, x, y);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                stopDragSelection();
            }
        }

        private void processTouchAt(RecyclerView rv, float x, float y) {
            float clampedX = Math.min(Math.max(x, AndroidUtilities.dp(10)), rv.getWidth() - AndroidUtilities.dp(10));
            View child = rv.findChildViewUnder(clampedX, y);
            if (child != null) {
                int pos = rv.getChildAdapterPosition(child);
                ArrayList<VaultItem> currentList = getListForTab(tabIndex);
                if (pos >= 0 && pos < currentList.size() && pos != lastSelectedPos) {
                    lastSelectedPos = pos;
                    VaultItem it = currentList.get(pos);
                    if (!selectedItems.contains(it)) {
                        selectedItems.add(it);
                        if (child instanceof VaultMediaCell) {
                            ((VaultMediaCell) child).setChecked(true, true);
                        } else if (child instanceof VaultFileCell) {
                            ((VaultFileCell) child).setChecked(true, true);
                        }
                        try {
                            AndroidUtilities.vibrateCursor(rv);
                        } catch (Throwable ignore) {}
                        updateSelectionCount();
                    }
                }
            }
        }

        private void stopDragSelection() {
            isDragSelecting = false;
            inTopHotspot = false;
            inBottomHotspot = false;
            AndroidUtilities.cancelRunOnUIThread(autoScrollRunnable);
            notifyAllAdapters();
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    }
}
