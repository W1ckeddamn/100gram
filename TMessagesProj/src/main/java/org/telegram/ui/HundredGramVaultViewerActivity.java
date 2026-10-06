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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ViewPagerFixed;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HundredGramVaultViewerActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    public static final int TAB_PHOTOS = 0;
    public static final int TAB_VIDEOS = 1;
    public static final int TAB_VOICE = 2;
    public static final int TAB_FILES = 3;

    private static final int ITEM_SEARCH = 1;
    private static final int ITEM_FILTER = 2;
    private static final int ITEM_SCAN = 3;

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
    private long totalVaultSize = 0;

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
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramVaultMediaTitle", R.string.HundredGramVaultMediaTitle));
        actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == ITEM_FILTER) {
                    showChatFilterDialog();
                } else if (id == ITEM_SCAN) {
                    showDeepScanDialog();
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

        ActionBarMenuItem scanItem = menu.addItem(ITEM_SCAN, R.drawable.msg_retry);
        scanItem.setContentDescription(LocaleController.getString("HundredGramVaultDeepScanTitle", R.string.HundredGramVaultDeepScanTitle));

        ActionBarMenuItem filterItem = menu.addItem(ITEM_FILTER, R.drawable.msg_openprofile);
        filterItem.setContentDescription(LocaleController.getString("HundredGramVaultFilterByChat", R.string.HundredGramVaultFilterByChat));

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        viewPager = new ViewPagerFixed(context);
        tabsView = viewPager.createTabsView(true, 8);
        tabsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        root.addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        root.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP, 0, 48, 0, 0));

        root.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 0, 49, 0, 0));

        viewPager.setAdapter(new ViewPagerFixed.Adapter() {
            @Override
            public int getItemCount() {
                return 4;
            }

            @Override
            public CharSequence getItemTitle(int position) {
                switch (position) {
                    case TAB_PHOTOS:
                        return LocaleController.getString("HundredGramVaultTabPhotos", R.string.HundredGramVaultTabPhotos);
                    case TAB_VIDEOS:
                        return LocaleController.getString("HundredGramVaultTabVideos", R.string.HundredGramVaultTabVideos);
                    case TAB_VOICE:
                        return LocaleController.getString("HundredGramVaultTabVoice", R.string.HundredGramVaultTabVoice);
                    case TAB_FILES:
                    default:
                        return LocaleController.getString("HundredGramVaultTabFiles", R.string.HundredGramVaultTabFiles);
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

                listView.setOnItemClickListener((v, pos) -> {
                    ArrayList<VaultItem> currentList = getListForTab(position);
                    if (pos < 0 || pos >= currentList.size()) return;
                    VaultItem item = currentList.get(pos);

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
                        showItemContextMenu(currentList.get(pos));
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
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) cursor.dispose();
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

            Collections.sort(items, (a, b) -> Long.compare(b.date, a.date));

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
        for (VaultItem it : allItems) {
            totalVaultSize += it.size;

            if (selectedDialogId != 0 && it.uid != selectedDialogId) {
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
        updateEmptyViews();

        for (int i = 0; i < pageAdapters.length; i++) {
            if (pageAdapters[i] != null) {
                pageAdapters[i].notifyDataSetChanged();
            }
        }
    }

    private void updateActionBarSubtitle() {
        if (actionBar == null) return;
        if (loading) {
            actionBar.setSubtitle(LocaleController.getString("Loading", R.string.Loading));
            return;
        }

        String sizeStr = AndroidUtilities.formatFileSize(totalVaultSize);
        if (selectedDialogId != 0) {
            TLObject obj = MessagesController.getInstance(currentAccount).getUserOrChat(selectedDialogId);
            String title = DialogObject.setDialogPhotoTitle(null, obj);
            actionBar.setSubtitle(title + " • " + sizeStr);
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

        items.add(LocaleController.getString("HundredGramVaultDelete", R.string.HundredGramVaultDelete));
        icons.add(R.drawable.msg_delete);
        actions.add(3);

        builder.setItems(items.toArray(new CharSequence[0]), AndroidUtilities.toIntArray(icons), (dialog, which) -> {
            int action = actions.get(which);
            if (action == 0) {
                shareVaultFile(item);
            } else if (action == 1) {
                saveVaultFile(item);
            } else if (action == 2) {
                goToMessageInChat(item);
            } else if (action == 3) {
                showDeleteFilePrompt(item);
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

    private void showChatFilterDialog() {
        if (getParentActivity() == null) return;

        HashMap<Long, Integer> chatCounts = new HashMap<>();
        for (VaultItem it : allItems) {
            chatCounts.put(it.uid, chatCounts.getOrDefault(it.uid, 0) + 1);
        }

        ArrayList<Long> uids = new ArrayList<>(chatCounts.keySet());
        Collections.sort(uids, (a, b) -> Integer.compare(chatCounts.get(b), chatCounts.get(a)));

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultFilterByChat", R.string.HundredGramVaultFilterByChat), true);

        ArrayList<CharSequence> names = new ArrayList<>();
        ArrayList<Long> dialogIds = new ArrayList<>();

        names.add(LocaleController.getString("HundredGramVaultAllChats", R.string.HundredGramVaultAllChats) + " (" + allItems.size() + ")");
        dialogIds.add(0L);

        for (Long uid : uids) {
            TLObject obj = MessagesController.getInstance(currentAccount).getUserOrChat(uid);
            String title = DialogObject.setDialogPhotoTitle(null, obj);
            if (TextUtils.isEmpty(title)) {
                title = uid > 0 ? "User " + uid : "Chat " + uid;
            }
            names.add(title + " (" + chatCounts.get(uid) + ")");
            dialogIds.add(uid);
        }

        builder.setItems(names.toArray(new CharSequence[0]), (dialog, which) -> {
            selectedDialogId = dialogIds.get(which);
            filterAll();
        });

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

            if (holder.itemView instanceof VaultMediaCell) {
                ((VaultMediaCell) holder.itemView).setItem(item);
            } else if (holder.itemView instanceof VaultFileCell) {
                ((VaultFileCell) holder.itemView).setItem(item);
            }
        }

        @Override
        public int getItemCount() {
            return getListForTab(tabIndex).size();
        }
    }

    public static class VaultMediaCell extends FrameLayout {
        private final BackupImageView imageView;
        private final FrameLayout videoBadge;
        private final TextView videoDurationText;

        public VaultMediaCell(@NonNull Context context) {
            super(context);

            imageView = new BackupImageView(context);
            imageView.setAspectFit(false);
            addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL, 1, 1, 1, 1));

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
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY));
        }

        public void setItem(VaultItem item) {
            if (item == null || item.vaultPath == null) return;
            imageView.setImage(ImageLocation.getForPath(item.vaultPath), "150_150", (Drawable) null, null);
            if (item.isVideo()) {
                videoBadge.setVisibility(View.VISIBLE);
            } else {
                videoBadge.setVisibility(View.GONE);
            }
        }
    }

    public class VaultFileCell extends FrameLayout {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;
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

            addView(textContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 72, 0, 16, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
        }

        public void setItem(VaultItem item) {
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
        }
    }
}
