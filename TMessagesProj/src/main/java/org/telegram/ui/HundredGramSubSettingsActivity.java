package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.HundredGramVaultManager;
import org.telegram.messenger.HundredGramVaultRotator;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramSubSettingsActivity extends BaseFragment {

    public static final int CATEGORY_VAULT = 0;
    public static final int CATEGORY_TRACKER = 1;
    public static final int CATEGORY_PRIVACY = 2;
    public static final int CATEGORY_INTERFACE = 3;
    public static final int CATEGORY_ACTIONS = 5;
    public static final int CATEGORY_MEDIA = 6;
    public static final int CATEGORY_GHOST = 7;
    public static final int CATEGORY_STREAMER = 8;

    // Item IDs for CATEGORY_STREAMER
    private static final int ITEM_STREAMER_MODE = 80;
    private static final int ITEM_STREAMER_FAKE_NAME = 81;
    private static final int ITEM_STREAMER_HIDE_PHONE = 82;
    private static final int ITEM_STREAMER_HIDE_USERNAMES = 83;
    private static final int ITEM_STREAMER_HIDE_AVATARS = 84;
    private static final int ITEM_STREAMER_HIDE_ID_DC = 85;
    private static final int ITEM_STREAMER_HIDE_NOTIF = 86;
    private static final int ITEM_STREAMER_HIDE_ACCOUNTS = 87;
    private static final int ITEM_STREAMER_PREVENT_CALLS = 88;
    private static final int ITEM_STREAMER_PREVENT_DELETE = 89;
    private static final int ITEM_PRIVACY_BLUR_MESSAGES = 90;
    private static final int ITEM_PRIVACY_BLUR_AVATARS = 91;
    private static final int ITEM_STREAMER_BLOCK_SCREENSHOTS = 180;
    private static final int ITEM_STREAMER_HIDE_DIALOG_MESSAGES = 183;

    // Item IDs for CATEGORY_GHOST
    private static final int ITEM_GHOST_READ_MESSAGES = 70;
    private static final int ITEM_GHOST_READ_ON_REPLY = 71;
    private static final int ITEM_GHOST_READ_MEDIA_CONTENTS = 72;
    private static final int ITEM_GHOST_SHOW_MARK_AS_READ = 73;
    private static final int ITEM_GHOST_TYPING = 74;
    private static final int ITEM_GHOST_STORIES_MODE = 75;
    private static final int ITEM_GHOST_UNLOCK_STORY_SAVING = 76;
    private static final int ITEM_AUTO_CHECK_DELETE_FOR_ALL = 77;
    private static final int ITEM_GHOST_ONLINE = 78;
    private static final int ITEM_GHOST_HIDE_READ_DATE = 79;

    // Item IDs for CATEGORY_VAULT
    private static final int ITEM_AUTO_DOWNLOAD_MEDIA_VAULT = 1;
    private static final int ITEM_AUTO_DOWNLOAD_MAX_SIZE = 2;
    private static final int ITEM_AUTO_DOWNLOAD_CHATS_SCOPE = 3;
    private static final int ITEM_AUTO_DOWNLOAD_ONLY_WIFI = 4;
    private static final int ITEM_AUTO_DOWNLOAD_EXCEPTIONS = 5;
    private static final int ITEM_BACKGROUND_ARCHIVER = 6;
    private static final int ITEM_CLEAR_ALL_VAULT = 7;
    private static final int ITEM_VAULT_BROWSE_FILES = 8;
    private static final int ITEM_VAULT_TTL = 9;
    private static final int ITEM_VAULT_MAX_SIZE_QUOTA = 12;
    private static final int ITEM_VAULT_CLEAN_EXPIRED_NOW = 13;
    private static final int ITEM_VAULT_INCLUDE_ARCHIVED = 184;
    private static final int ITEM_PROTECTED_VAULT_SIZE = 189;
    private static final int ITEM_CLEAR_PROTECTED_VAULT = 190;
    private static final int ITEM_VAULT_ARCHIVE_COMMENTS = 191;
    private static final int ITEM_BACKGROUND_ARCHIVER_MODE = 192;

    // Item IDs for CATEGORY_TRACKER
    private static final int ITEM_TRACK_ONLINE_STATUS = 10;
    private static final int ITEM_NOTIFY_ON_USER_ONLINE = 11;
    private static final int ITEM_NOTIFY_ALL_CONTACTS_ONLINE = 181;
    private static final int ITEM_TRACKED_USERS_LIST = 182;
    private static final int ITEM_TRACK_SELF_ACTIVITY = 187;
    private static final int ITEM_MY_ACTIVITY_LOG = 188;

    // Item IDs for CATEGORY_PRIVACY
    private static final int ITEM_HIDE_PHONE_NUMBER = 20;
    private static final int ITEM_HIDE_OTHER_PHONE_NUMBERS = 21;
    private static final int ITEM_HIDE_USERNAME = 22;
    private static final int ITEM_HIDE_OTHER_USERNAMES = 23;
    private static final int ITEM_SHOW_ID_AND_DC = 24;
    private static final int ITEM_KEEPALIVE_FOREGROUND_SERVICE = 25;
    private static final int ITEM_BATTERY_OPTIMIZATIONS = 26;

    // Item IDs for CATEGORY_INTERFACE
    private static final int ITEM_RELATIVE_LAST_SEEN = 30;
    private static final int ITEM_DISABLE_JUMP_TO_NEXT_CHANNEL = 31;
    private static final int ITEM_HIDE_KEYBOARD_ON_SCROLL = 32;
    private static final int ITEM_ADD_COMMA_AFTER_MENTION = 33;
    private static final int ITEM_DISABLE_NUMBER_ROUNDING = 34;
    private static final int ITEM_HIDE_ARCHIVE_FOLDER = 35;
    private static final int ITEM_HIDE_FLOATING_BUTTON = 36;
    private static final int ITEM_HIGHLIGHT_DELETED = 37;
    private static final int ITEM_CONFIRM_CALLS = 38;
    private static final int ITEM_CLEAN_URLS = 39;
    private static final int ITEM_UNLOCK_CHAT_TRANSLATION = 65;
    private static final int ITEM_HIDE_SIMILAR_CHANNELS = 93;
    private static final int ITEM_HIDE_STORIES_BAR = 94;
    private static final int ITEM_SHOW_SECONDS_IN_TIME = 95;
    private static final int ITEM_AVATAR_HISTORY_ENABLED = 97;

    // Item IDs for CATEGORY_ACTIONS
    private static final int ITEM_DOUBLE_TAP_INCOMING = 50;
    private static final int ITEM_DOUBLE_TAP_OUTGOING = 51;
    private static final int ITEM_SHOW_SAVE_MESSAGE = 52;
    private static final int ITEM_SHOW_DETAILS = 53;
    private static final int ITEM_SHOW_COPY_PHOTO = 54;
    private static final int ITEM_SHOW_EDIT_HISTORY = 55;
    private static final int ITEM_SILENT_SEND_BY_DEFAULT = 56;
    private static final int ITEM_ALWAYS_FORWARD_WITHOUT_AUTHOR = 57;
    private static final int ITEM_CONFIRM_SEND_STICKERS = 58;
    private static final int ITEM_CONFIRM_SEND_GIFS = 59;
    private static final int ITEM_SHOW_FORWARD_WITHOUT_AUTHOR = 64;

    // Item IDs for CATEGORY_MEDIA
    private static final int ITEM_ALWAYS_HD_PHOTOS = 60;
    private static final int ITEM_PHOTO_MAX_RESOLUTION = 61;
    private static final int ITEM_SEND_VIDEO_AS_DOCUMENT = 62;
    private static final int ITEM_STICKER_SIZE = 201;
    private static final int ITEM_GIF_SIZE = 202;

    // Item IDs for Developer Mode
    private static final int ITEM_DEV_MODE = 100;
    private static final int ITEM_COPY_CODE_BUTTON = 101;
    private static final int ITEM_PRETTY_JSON_VIEWER = 102;

    private final int category;
    private UniversalRecyclerView listView;

    public HundredGramSubSettingsActivity(int category) {
        super(createArgs(category));
        this.category = category;
    }

    private static Bundle createArgs(int category) {
        Bundle args = new Bundle();
        args.putInt("category", category);
        return args;
    }

    public HundredGramSubSettingsActivity(Bundle args) {
        super(args);
        this.category = args != null ? args.getInt("category", 0) : 0;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getCategoryTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, null);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private String getCategoryTitle() {
        switch (category) {
            case CATEGORY_VAULT:
                return LocaleController.getString("HundredGramCategoryVault", R.string.HundredGramCategoryVault);
            case CATEGORY_TRACKER:
                return LocaleController.getString("HundredGramCategoryTracker", R.string.HundredGramCategoryTracker);
            case CATEGORY_PRIVACY:
                return LocaleController.getString("HundredGramCategoryPrivacy", R.string.HundredGramCategoryPrivacy);
            case CATEGORY_INTERFACE:
                return LocaleController.getString("HundredGramCategoryInterface", R.string.HundredGramCategoryInterface);
            case CATEGORY_ACTIONS:
                return LocaleController.getString("HundredGramCategoryActions", R.string.HundredGramCategoryActions);
            case CATEGORY_MEDIA:
                return LocaleController.getString("HundredGramCategoryMediaQuality", R.string.HundredGramCategoryMediaQuality);
            case CATEGORY_GHOST:
                return LocaleController.getString("HundredGramCategoryGhost", R.string.HundredGramCategoryGhost);
            case CATEGORY_STREAMER:
                return LocaleController.getString("HundredGramCategoryStreamer", R.string.HundredGramCategoryStreamer);
            default:
                return "100gram";
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (category) {
            case CATEGORY_VAULT:
                fillVaultItems(items);
                break;
            case CATEGORY_TRACKER:
                fillTrackerItems(items);
                break;
            case CATEGORY_PRIVACY:
                fillPrivacyItems(items);
                break;
            case CATEGORY_GHOST:
                fillGhostItems(items);
                break;
            case CATEGORY_INTERFACE:
                fillInterfaceItems(items);
                break;
            case CATEGORY_ACTIONS:
                fillActionsItems(items);
                break;
            case CATEGORY_MEDIA:
                fillMediaItems(items);
                break;
            case CATEGORY_STREAMER:
                fillStreamerItems(items);
                break;
        }
    }

    private void fillVaultItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultHeader", R.string.HundredGramVaultHeader)));

        items.add(UItem.asButton(ITEM_VAULT_BROWSE_FILES, R.drawable.msg_folders,
                LocaleController.getString("VaultBrowseFiles", R.string.VaultBrowseFiles)));
        items.add(UItem.asShadow(LocaleController.getString("VaultBrowseFilesInfo", R.string.VaultBrowseFilesInfo)));

        items.add(UItem.asCheck(ITEM_AUTO_DOWNLOAD_MEDIA_VAULT, LocaleController.getString("HundredGramMediaVault", R.string.HundredGramMediaVault))
                .setChecked(HundredGramConfig.autoDownloadMediaVault));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramMediaVaultInfo", R.string.HundredGramMediaVaultInfo)));

        if (HundredGramConfig.autoDownloadMediaVault) {
            items.add(UItem.asButton(ITEM_AUTO_DOWNLOAD_MAX_SIZE, LocaleController.getString("HundredGramMediaVaultLimit", R.string.HundredGramMediaVaultLimit), HundredGramConfig.autoDownloadMaxSizeMb + " " + LocaleController.getString("HundredGramMB", R.string.HundredGramMB)));
            items.add(UItem.asShadow(LocaleController.getString("HundredGramMediaVaultLimitInfo", R.string.HundredGramMediaVaultLimitInfo)));

            String scopeName;
            if (HundredGramConfig.autoDownloadChatsScope == 0) {
                scopeName = LocaleController.getString("HundredGramMediaVaultScopePrivate", R.string.HundredGramMediaVaultScopePrivate);
            } else if (HundredGramConfig.autoDownloadChatsScope == 1) {
                scopeName = LocaleController.getString("HundredGramMediaVaultScopeGroups", R.string.HundredGramMediaVaultScopeGroups);
            } else {
                scopeName = LocaleController.getString("HundredGramMediaVaultScopeAll", R.string.HundredGramMediaVaultScopeAll);
            }
            items.add(UItem.asButton(ITEM_AUTO_DOWNLOAD_CHATS_SCOPE, LocaleController.getString("HundredGramMediaVaultScope", R.string.HundredGramMediaVaultScope), scopeName));
            items.add(UItem.asShadow(LocaleController.getString("HundredGramMediaVaultScopeInfo", R.string.HundredGramMediaVaultScopeInfo)));

            items.add(UItem.asCheck(ITEM_VAULT_INCLUDE_ARCHIVED, LocaleController.getString("VaultIncludeArchivedChats", R.string.VaultIncludeArchivedChats))
                    .setChecked(HundredGramConfig.vaultIncludeArchivedChats));
            items.add(UItem.asShadow(LocaleController.getString("VaultIncludeArchivedChatsInfo", R.string.VaultIncludeArchivedChatsInfo)));

            items.add(UItem.asCheck(ITEM_AUTO_DOWNLOAD_ONLY_WIFI, LocaleController.getString("HundredGramMediaVaultOnlyWifi", R.string.HundredGramMediaVaultOnlyWifi))
                    .setChecked(HundredGramConfig.autoDownloadOnlyWifi));
            items.add(UItem.asShadow(LocaleController.getString("HundredGramMediaVaultOnlyWifiInfo", R.string.HundredGramMediaVaultOnlyWifiInfo)));
        }

        ArrayList<Long> exceptions = HundredGramConfig.getChatVaultExceptions();
        int exceptionsCount = exceptions.size();
        String exceptionsValue = exceptionsCount > 0 ? LocaleController.formatPluralString("Chats", exceptionsCount) : LocaleController.getString("HundredGramVaultNoExceptions", R.string.HundredGramVaultNoExceptions);
        items.add(UItem.asButton(ITEM_AUTO_DOWNLOAD_EXCEPTIONS, LocaleController.getString("HundredGramVaultExceptions", R.string.HundredGramVaultExceptions), exceptionsValue));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramVaultExceptionsInfo", R.string.HundredGramVaultExceptionsInfo)));

        items.add(UItem.asCheck(ITEM_BACKGROUND_ARCHIVER, LocaleController.getString("HundredGramBackgroundArchiver", R.string.HundredGramBackgroundArchiver))
                .setChecked(HundredGramConfig.backgroundArchiverEnabled));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramBackgroundArchiverInfo", R.string.HundredGramBackgroundArchiverInfo)));

        if (HundredGramConfig.backgroundArchiverEnabled) {
            String modeText;
            if (HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_NEW_ONLY) {
                modeText = LocaleController.getString("HundredGramArchiverModeNewOnly", R.string.HundredGramArchiverModeNewOnly);
            } else {
                modeText = LocaleController.getString("HundredGramArchiverModeFullSync", R.string.HundredGramArchiverModeFullSync);
            }
            items.add(UItem.asButton(ITEM_BACKGROUND_ARCHIVER_MODE, LocaleController.getString("HundredGramArchiverMode", R.string.HundredGramArchiverMode), modeText));
            items.add(UItem.asShadow(HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_NEW_ONLY
                    ? LocaleController.getString("HundredGramArchiverModeNewOnlyInfo", R.string.HundredGramArchiverModeNewOnlyInfo)
                    : LocaleController.getString("HundredGramArchiverModeFullSyncInfo", R.string.HundredGramArchiverModeFullSyncInfo)));
        }

        items.add(UItem.asCheck(ITEM_VAULT_ARCHIVE_COMMENTS, LocaleController.getString("HundredGramVaultArchiveComments", R.string.HundredGramVaultArchiveComments))
                .setChecked(HundredGramConfig.vaultArchiveChannelComments));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramVaultArchiveCommentsInfo", R.string.HundredGramVaultArchiveCommentsInfo)));


        items.add(UItem.asHeader(LocaleController.getString("VaultTtl", R.string.VaultTtl)));

        String ttlText;
        if (HundredGramConfig.vaultTtlDays <= 0) {
            ttlText = LocaleController.getString("VaultTtlForever", R.string.VaultTtlForever);
        } else if (HundredGramConfig.vaultTtlDays == 7) {
            ttlText = LocaleController.getString("VaultTtl7Days", R.string.VaultTtl7Days);
        } else if (HundredGramConfig.vaultTtlDays == 30) {
            ttlText = LocaleController.getString("VaultTtl30Days", R.string.VaultTtl30Days);
        } else if (HundredGramConfig.vaultTtlDays == 60) {
            ttlText = LocaleController.getString("VaultTtl60Days", R.string.VaultTtl60Days);
        } else {
            ttlText = LocaleController.getString("VaultTtl90Days", R.string.VaultTtl90Days);
        }
        items.add(UItem.asButton(ITEM_VAULT_TTL, LocaleController.getString("VaultTtl", R.string.VaultTtl), ttlText));
        items.add(UItem.asShadow(LocaleController.getString("VaultTtlInfo", R.string.VaultTtlInfo)));

        String maxSizeText;
        if (HundredGramConfig.vaultMaxSizeGb <= 0) {
            maxSizeText = LocaleController.getString("VaultMaxSizeUnlimited", R.string.VaultMaxSizeUnlimited);
        } else if (HundredGramConfig.vaultMaxSizeGb == 2) {
            maxSizeText = LocaleController.getString("VaultMaxSize2Gb", R.string.VaultMaxSize2Gb);
        } else if (HundredGramConfig.vaultMaxSizeGb == 5) {
            maxSizeText = LocaleController.getString("VaultMaxSize5Gb", R.string.VaultMaxSize5Gb);
        } else if (HundredGramConfig.vaultMaxSizeGb == 10) {
            maxSizeText = LocaleController.getString("VaultMaxSize10Gb", R.string.VaultMaxSize10Gb);
        } else {
            maxSizeText = LocaleController.getString("VaultMaxSize20Gb", R.string.VaultMaxSize20Gb);
        }
        items.add(UItem.asButton(ITEM_VAULT_MAX_SIZE_QUOTA, LocaleController.getString("VaultMaxSize", R.string.VaultMaxSize), maxSizeText));
        items.add(UItem.asShadow(LocaleController.getString("VaultMaxSizeInfo", R.string.VaultMaxSizeInfo)));

        items.add(UItem.asButton(ITEM_VAULT_CLEAN_EXPIRED_NOW, R.drawable.msg_autodelete, LocaleController.getString("VaultCleanExpiredNow", R.string.VaultCleanExpiredNow), ""));
        items.add(UItem.asShadow(LocaleController.getString("VaultTtlInfo", R.string.VaultTtlInfo)));

        // Protected Media Vault section
        items.add(UItem.asHeader(LocaleController.getString("HundredGramProtectedVaultTitle", R.string.HundredGramProtectedVaultTitle)));
        long vaultSize = HundredGramVaultManager.getInstance(currentAccount).getVaultTotalSize();
        String sizeFormatted = AndroidUtilities.formatFileSize(vaultSize);
        items.add(UItem.asButton(ITEM_PROTECTED_VAULT_SIZE, R.drawable.msg_filled_storageusage,
                LocaleController.getString("HundredGramProtectedVaultSize", R.string.HundredGramProtectedVaultSize), sizeFormatted));
        items.add(UItem.asButton(ITEM_CLEAR_PROTECTED_VAULT, R.drawable.msg_clear,
                LocaleController.getString("HundredGramProtectedVaultClear", R.string.HundredGramProtectedVaultClear), ""));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramProtectedVaultInfo", R.string.HundredGramProtectedVaultInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultClearHeader", R.string.HundredGramVaultClearHeader)));
        items.add(UItem.asButton(ITEM_CLEAR_ALL_VAULT, R.drawable.msg_clear, LocaleController.getString("HundredGramVaultClearAll", R.string.HundredGramVaultClearAll), ""));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramVaultClearAllInfo", R.string.HundredGramVaultClearAllInfo)));
    }

    private void fillTrackerItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramTrackerHeader", R.string.HundredGramTrackerHeader)));

        items.add(UItem.asCheck(ITEM_TRACK_ONLINE_STATUS, LocaleController.getString("HundredGramTrackOnlineStatus", R.string.HundredGramTrackOnlineStatus))
                .setChecked(HundredGramConfig.trackOnlineStatus));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramTrackOnlineStatusInfo", R.string.HundredGramTrackOnlineStatusInfo)));

        items.add(UItem.asCheck(ITEM_NOTIFY_ON_USER_ONLINE, LocaleController.getString("HundredGramNotifyUserOnline", R.string.HundredGramNotifyUserOnline))
                .setChecked(HundredGramConfig.notifyOnUserOnline));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramNotifyUserOnlineInfo", R.string.HundredGramNotifyUserOnlineInfo)));

        if (HundredGramConfig.notifyOnUserOnline) {
            items.add(UItem.asCheck(ITEM_NOTIFY_ALL_CONTACTS_ONLINE, LocaleController.getString("HundredGramNotifyAllContactsOnline", R.string.HundredGramNotifyAllContactsOnline))
                    .setChecked(HundredGramConfig.notifyAllContactsOnline));
            items.add(UItem.asShadow(LocaleController.getString("HundredGramNotifyAllContactsOnlineInfo", R.string.HundredGramNotifyAllContactsOnlineInfo)));

            int trackedCount = HundredGramConfig.trackedOnlineUsers.size();
            items.add(UItem.asButton(ITEM_TRACKED_USERS_LIST, R.drawable.msg_usersearch,
                    LocaleController.getString("HundredGramTrackedUsersList", R.string.HundredGramTrackedUsersList),
                    String.format(LocaleController.getString("HundredGramTrackedUsersCount", R.string.HundredGramTrackedUsersCount), trackedCount)));
            items.add(UItem.asShadow(LocaleController.getString("HundredGramTrackOnlineStatusInfo", R.string.HundredGramTrackOnlineStatusInfo)));
        }

        items.add(UItem.asCheck(ITEM_TRACK_SELF_ACTIVITY, LocaleController.getString("HundredGramTrackSelfActivity", R.string.HundredGramTrackSelfActivity))
                .setChecked(HundredGramConfig.trackSelfActivity));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramTrackSelfActivityInfo", R.string.HundredGramTrackSelfActivityInfo)));

        items.add(UItem.asButton(ITEM_MY_ACTIVITY_LOG, R.drawable.msg_calendar2,
                LocaleController.getString("HundredGramMyActivityLog", R.string.HundredGramMyActivityLog),
                LocaleController.getString("HundredGramMyActivityLogDesc", R.string.HundredGramMyActivityLogDesc)));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("AvatarHistory", R.string.AvatarHistory)));
        items.add(UItem.asCheck(ITEM_AVATAR_HISTORY_ENABLED, LocaleController.getString("AvatarHistory", R.string.AvatarHistory))
                .setChecked(HundredGramConfig.avatarHistoryEnabled));
        items.add(UItem.asShadow(LocaleController.getString("AvatarHistoryInfo", R.string.AvatarHistoryInfo)));
    }

    private void fillPrivacyItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramPrivacyDataHiding", R.string.HundredGramPrivacyDataHiding)));

        items.add(UItem.asCheck(ITEM_HIDE_PHONE_NUMBER, LocaleController.getString("HundredGramHidePhoneNumber", R.string.HundredGramHidePhoneNumber))
                .setChecked(HundredGramConfig.hidePhoneNumber));
        items.add(UItem.asCheck(ITEM_HIDE_OTHER_PHONE_NUMBERS, LocaleController.getString("HundredGramHideOtherPhoneNumbers", R.string.HundredGramHideOtherPhoneNumbers))
                .setChecked(HundredGramConfig.hideOtherPhoneNumbers));
        items.add(UItem.asCheck(ITEM_HIDE_USERNAME, LocaleController.getString("HundredGramHideUsername", R.string.HundredGramHideUsername))
                .setChecked(HundredGramConfig.hideUsername));
        items.add(UItem.asCheck(ITEM_HIDE_OTHER_USERNAMES, LocaleController.getString("HundredGramHideOtherUsernames", R.string.HundredGramHideOtherUsernames))
                .setChecked(HundredGramConfig.hideOtherUsernames));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramPrivacyDataHidingInfo", R.string.HundredGramPrivacyDataHidingInfo)));

        items.add(UItem.asHeader(LocaleController.getString("PrivacyBlurSection", R.string.PrivacyBlurSection)));

        items.add(UItem.asCheck(ITEM_PRIVACY_BLUR_MESSAGES, LocaleController.getString("PrivacyBlurDialogMessages", R.string.PrivacyBlurDialogMessages))
                .setChecked(HundredGramConfig.privacyBlurDialogMessages));
        items.add(UItem.asShadow(LocaleController.getString("PrivacyBlurDialogMessagesInfo", R.string.PrivacyBlurDialogMessagesInfo)));

        items.add(UItem.asCheck(ITEM_PRIVACY_BLUR_AVATARS, LocaleController.getString("PrivacyBlurAvatars", R.string.PrivacyBlurAvatars))
                .setChecked(HundredGramConfig.privacyBlurAvatars));
        items.add(UItem.asShadow(LocaleController.getString("PrivacyBlurAvatarsInfo", R.string.PrivacyBlurAvatarsInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramProfileSection", R.string.HundredGramProfileSection)));
        String idDcValue;
        if (HundredGramConfig.showIdAndDc == 0) {
            idDcValue = LocaleController.getString("HundredGramShowIdDisabled", R.string.HundredGramShowIdDisabled);
        } else if (HundredGramConfig.showIdAndDc == 1) {
            idDcValue = LocaleController.getString("HundredGramShowIdOnly", R.string.HundredGramShowIdOnly);
        } else {
            idDcValue = LocaleController.getString("HundredGramShowIdAndDcValue", R.string.HundredGramShowIdAndDcValue);
        }
        items.add(UItem.asButton(ITEM_SHOW_ID_AND_DC, LocaleController.getString("HundredGramShowIdAndDc", R.string.HundredGramShowIdAndDc), idDcValue));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramShowIdAndDcInfo", R.string.HundredGramShowIdAndDcInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramBackgroundSyncSection", R.string.HundredGramBackgroundSyncSection)));

        items.add(UItem.asCheck(ITEM_KEEPALIVE_FOREGROUND_SERVICE, LocaleController.getString("KeepAliveForegroundService", R.string.KeepAliveForegroundService))
                .setChecked(HundredGramConfig.keepAliveForegroundService));
        items.add(UItem.asShadow(LocaleController.getString("KeepAliveForegroundServiceInfo", R.string.KeepAliveForegroundServiceInfo)));

        items.add(UItem.asButton(ITEM_BATTERY_OPTIMIZATIONS, LocaleController.getString("BatteryOptimization", R.string.BatteryOptimization)));
        items.add(UItem.asShadow(LocaleController.getString("BatteryOptimizationInfo", R.string.BatteryOptimizationInfo)));
    }

    private void fillGhostItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("GhostOnline", R.string.GhostOnline)));

        items.add(UItem.asCheck(ITEM_GHOST_ONLINE, LocaleController.getString("GhostOnline", R.string.GhostOnline))
                .setChecked(HundredGramConfig.ghostOnline));
        items.add(UItem.asShadow(LocaleController.getString("GhostOnlineInfo", R.string.GhostOnlineInfo)));

        items.add(UItem.asHeader(LocaleController.getString("GhostReadMessages", R.string.GhostReadMessages)));

        items.add(UItem.asCheck(ITEM_GHOST_READ_MESSAGES, LocaleController.getString("GhostReadMessages", R.string.GhostReadMessages))
                .setChecked(HundredGramConfig.ghostReadMessages));
        items.add(UItem.asShadow(LocaleController.getString("GhostReadMessagesInfo", R.string.GhostReadMessagesInfo)));

        if (HundredGramConfig.ghostReadMessages) {
            items.add(UItem.asCheck(ITEM_GHOST_READ_ON_REPLY, LocaleController.getString("GhostReadOnReply", R.string.GhostReadOnReply))
                    .setChecked(HundredGramConfig.ghostReadOnReply));
            items.add(UItem.asShadow(LocaleController.getString("GhostReadOnReplyInfo", R.string.GhostReadOnReplyInfo)));

            items.add(UItem.asCheck(ITEM_GHOST_READ_MEDIA_CONTENTS, LocaleController.getString("GhostReadMediaContents", R.string.GhostReadMediaContents))
                    .setChecked(HundredGramConfig.ghostReadMediaContents));
            items.add(UItem.asShadow(LocaleController.getString("GhostReadMediaContentsInfo", R.string.GhostReadMediaContentsInfo)));

            items.add(UItem.asCheck(ITEM_GHOST_SHOW_MARK_AS_READ, LocaleController.getString("GhostShowMarkAsRead", R.string.GhostShowMarkAsRead))
                    .setChecked(HundredGramConfig.ghostShowMarkAsReadOption));
            items.add(UItem.asShadow(LocaleController.getString("GhostShowMarkAsReadInfo", R.string.GhostShowMarkAsReadInfo)));
        }

        items.add(UItem.asCheck(ITEM_GHOST_HIDE_READ_DATE, LocaleController.getString("GhostHideReadDate", R.string.GhostHideReadDate))
                .setChecked(HundredGramConfig.ghostHideReadDate));
        items.add(UItem.asShadow(LocaleController.getString("GhostHideReadDateInfo", R.string.GhostHideReadDateInfo)));

        items.add(UItem.asCheck(ITEM_GHOST_TYPING, LocaleController.getString("GhostTyping", R.string.GhostTyping))
                .setChecked(HundredGramConfig.ghostTyping));
        items.add(UItem.asShadow(LocaleController.getString("GhostTypingInfo", R.string.GhostTypingInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramStoriesSection", R.string.HundredGramStoriesSection)));

        String storiesModeStr;
        if (HundredGramConfig.ghostStoriesMode == HundredGramConfig.GHOST_STORIES_OFF) {
            storiesModeStr = LocaleController.getString("GhostStoriesOff", R.string.GhostStoriesOff);
        } else if (HundredGramConfig.ghostStoriesMode == HundredGramConfig.GHOST_STORIES_ALWAYS) {
            storiesModeStr = LocaleController.getString("GhostStoriesAlways", R.string.GhostStoriesAlways);
        } else {
            storiesModeStr = LocaleController.getString("GhostStoriesPrompt", R.string.GhostStoriesPrompt);
        }
        items.add(UItem.asButton(ITEM_GHOST_STORIES_MODE, LocaleController.getString("GhostStoriesMode", R.string.GhostStoriesMode), storiesModeStr));
        items.add(UItem.asShadow(LocaleController.getString("GhostStoriesModeInfo", R.string.GhostStoriesModeInfo)));

        items.add(UItem.asCheck(ITEM_GHOST_UNLOCK_STORY_SAVING, LocaleController.getString("UnlockStorySaving", R.string.UnlockStorySaving))
                .setChecked(HundredGramConfig.unlockStorySaving));
        items.add(UItem.asShadow(LocaleController.getString("UnlockStorySavingInfo", R.string.UnlockStorySavingInfo)));
    }

    private void fillInterfaceItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatsAndMessagesSection", R.string.HundredGramChatsAndMessagesSection)));

        items.add(UItem.asCheck(ITEM_UNLOCK_CHAT_TRANSLATION, LocaleController.getString("UnlockChatTranslation", R.string.UnlockChatTranslation))
                .setChecked(HundredGramConfig.unlockChatTranslation));
        items.add(UItem.asShadow(LocaleController.getString("UnlockChatTranslationInfo", R.string.UnlockChatTranslationInfo)));

        items.add(UItem.asCheck(ITEM_RELATIVE_LAST_SEEN, LocaleController.getString("HundredGramRelativeLastSeen", R.string.HundredGramRelativeLastSeen))
                .setChecked(HundredGramConfig.relativeLastSeen));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramRelativeLastSeenInfo", R.string.HundredGramRelativeLastSeenInfo)));

        items.add(UItem.asCheck(ITEM_DISABLE_JUMP_TO_NEXT_CHANNEL, LocaleController.getString("HundredGramDisableJumpChannel", R.string.HundredGramDisableJumpChannel))
                .setChecked(HundredGramConfig.disableJumpToNextChannel));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramDisableJumpChannelInfo", R.string.HundredGramDisableJumpChannelInfo)));

        items.add(UItem.asCheck(ITEM_HIDE_KEYBOARD_ON_SCROLL, LocaleController.getString("HundredGramHideKeyboardOnScroll", R.string.HundredGramHideKeyboardOnScroll))
                .setChecked(HundredGramConfig.hideKeyboardOnScroll));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramHideKeyboardOnScrollInfo", R.string.HundredGramHideKeyboardOnScrollInfo)));

        items.add(UItem.asCheck(ITEM_ADD_COMMA_AFTER_MENTION, LocaleController.getString("HundredGramAddCommaAfterMention", R.string.HundredGramAddCommaAfterMention))
                .setChecked(HundredGramConfig.addCommaAfterMention));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramAddCommaAfterMentionInfo", R.string.HundredGramAddCommaAfterMentionInfo)));

        items.add(UItem.asCheck(ITEM_DISABLE_NUMBER_ROUNDING, LocaleController.getString("HundredGramDisableNumberRounding", R.string.HundredGramDisableNumberRounding))
                .setChecked(HundredGramConfig.disableNumberRounding));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramDisableNumberRoundingInfo", R.string.HundredGramDisableNumberRoundingInfo)));

        items.add(UItem.asCheck(ITEM_HIGHLIGHT_DELETED, LocaleController.getString("HundredGramHighlightDeleted", R.string.HundredGramHighlightDeleted))
                .setChecked(HundredGramConfig.highlightDeletedMessages));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramHighlightDeletedInfo", R.string.HundredGramHighlightDeletedInfo)));

        items.add(UItem.asCheck(ITEM_HIDE_SIMILAR_CHANNELS, LocaleController.getString("HideSimilarChannels", R.string.HideSimilarChannels))
                .setChecked(HundredGramConfig.hideSimilarChannels));
        items.add(UItem.asShadow(LocaleController.getString("HideSimilarChannelsInfo", R.string.HideSimilarChannelsInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_SECONDS_IN_TIME, LocaleController.getString("ShowSecondsInTime", R.string.ShowSecondsInTime))
                .setChecked(HundredGramConfig.showSecondsInTime));
        items.add(UItem.asShadow(LocaleController.getString("ShowSecondsInTimeInfo", R.string.ShowSecondsInTimeInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramMainScreenSection", R.string.HundredGramMainScreenSection)));

        items.add(UItem.asCheck(ITEM_HIDE_STORIES_BAR, LocaleController.getString("HideStoriesBar", R.string.HideStoriesBar))
                .setChecked(HundredGramConfig.hideStoriesBar));
        items.add(UItem.asShadow(LocaleController.getString("HideStoriesBarInfo", R.string.HideStoriesBarInfo)));

        items.add(UItem.asCheck(ITEM_HIDE_ARCHIVE_FOLDER, LocaleController.getString("HundredGramHideArchiveFolder", R.string.HundredGramHideArchiveFolder))
                .setChecked(HundredGramConfig.hideArchiveFolder));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramHideArchiveFolderInfo", R.string.HundredGramHideArchiveFolderInfo)));

        items.add(UItem.asCheck(ITEM_HIDE_FLOATING_BUTTON, LocaleController.getString("HundredGramHideFloatingButton", R.string.HundredGramHideFloatingButton))
                .setChecked(HundredGramConfig.hideFloatingButton));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramHideFloatingButtonInfo", R.string.HundredGramHideFloatingButtonInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramCallsAndLinksSection", R.string.HundredGramCallsAndLinksSection)));

        items.add(UItem.asCheck(ITEM_CONFIRM_CALLS, LocaleController.getString("ConfirmCalls", R.string.ConfirmCalls))
                .setChecked(HundredGramConfig.confirmCalls));
        items.add(UItem.asShadow(LocaleController.getString("ConfirmCallsInfo", R.string.ConfirmCallsInfo)));

        items.add(UItem.asCheck(ITEM_CLEAN_URLS, LocaleController.getString("CleanUrls", R.string.CleanUrls))
                .setChecked(HundredGramConfig.cleanUrls));
        items.add(UItem.asShadow(LocaleController.getString("CleanUrlsInfo", R.string.CleanUrlsInfo)));
    }

    private void fillActionsItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("DoubleTapHeader", R.string.DoubleTapHeader)));

        items.add(UItem.asButton(ITEM_DOUBLE_TAP_INCOMING, LocaleController.getString("DoubleTapActionIncoming", R.string.DoubleTapActionIncoming), getDoubleTapActionName(HundredGramConfig.doubleTapAction)));
        items.add(UItem.asShadow(LocaleController.getString("DoubleTapActionIncomingDesc", R.string.DoubleTapActionIncomingDesc)));

        items.add(UItem.asButton(ITEM_DOUBLE_TAP_OUTGOING, LocaleController.getString("DoubleTapActionOutgoing", R.string.DoubleTapActionOutgoing), getDoubleTapActionName(HundredGramConfig.doubleTapActionOutOwner)));
        items.add(UItem.asShadow(LocaleController.getString("DoubleTapActionOutgoingDesc", R.string.DoubleTapActionOutgoingDesc)));

        items.add(UItem.asHeader(LocaleController.getString("MessageMenuSection", R.string.MessageMenuSection)));

        items.add(UItem.asCheck(ITEM_SHOW_SAVE_MESSAGE, LocaleController.getString("ShowSaveMessageButton", R.string.ShowSaveMessageButton))
                .setChecked(HundredGramConfig.showSaveMessageButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowSaveMessageButtonInfo", R.string.ShowSaveMessageButtonInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_DETAILS, LocaleController.getString("ShowDetailsButton", R.string.ShowDetailsButton))
                .setChecked(HundredGramConfig.showDetailsButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowDetailsButtonInfo", R.string.ShowDetailsButtonInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_COPY_PHOTO, LocaleController.getString("ShowCopyPhotoButton", R.string.ShowCopyPhotoButton))
                .setChecked(HundredGramConfig.showCopyPhotoButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowCopyPhotoButtonInfo", R.string.ShowCopyPhotoButtonInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_EDIT_HISTORY, LocaleController.getString("ShowEditHistoryButton", R.string.ShowEditHistoryButton))
                .setChecked(HundredGramConfig.showEditHistoryButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowEditHistoryButtonInfo", R.string.ShowEditHistoryButtonInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_FORWARD_WITHOUT_AUTHOR, LocaleController.getString("ShowForwardWithoutAuthorButton", R.string.ShowForwardWithoutAuthorButton))
                .setChecked(HundredGramConfig.showForwardWithoutAuthorButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowForwardWithoutAuthorButtonInfo", R.string.ShowForwardWithoutAuthorButtonInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramDeleteSection", R.string.HundredGramDeleteSection)));
        items.add(UItem.asCheck(ITEM_AUTO_CHECK_DELETE_FOR_ALL, LocaleController.getString("AutoCheckDeleteForAll", R.string.AutoCheckDeleteForAll))
                .setChecked(HundredGramConfig.autoCheckDeleteForAll));
        items.add(UItem.asShadow(LocaleController.getString("AutoCheckDeleteForAllInfo", R.string.AutoCheckDeleteForAllInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramSendAndForwardSection", R.string.HundredGramSendAndForwardSection)));
        items.add(UItem.asCheck(ITEM_SILENT_SEND_BY_DEFAULT, LocaleController.getString(R.string.SilentSendByDefault))
                .setChecked(HundredGramConfig.silentSendByDefault));
        items.add(UItem.asShadow(LocaleController.getString(R.string.SilentSendByDefaultInfo)));

        items.add(UItem.asCheck(ITEM_ALWAYS_FORWARD_WITHOUT_AUTHOR, LocaleController.getString(R.string.AlwaysForwardWithoutAuthor))
                .setChecked(HundredGramConfig.alwaysForwardWithoutAuthor));
        items.add(UItem.asShadow(LocaleController.getString(R.string.AlwaysForwardWithoutAuthorInfo)));

        items.add(UItem.asCheck(ITEM_CONFIRM_SEND_STICKERS, LocaleController.getString(R.string.ConfirmSendStickers))
                .setChecked(HundredGramConfig.confirmSendStickers));
        items.add(UItem.asShadow(LocaleController.getString(R.string.ConfirmSendStickersInfo)));

        items.add(UItem.asCheck(ITEM_CONFIRM_SEND_GIFS, LocaleController.getString(R.string.ConfirmSendGifs))
                .setChecked(HundredGramConfig.confirmSendGifs));
        items.add(UItem.asShadow(LocaleController.getString(R.string.ConfirmSendGifsInfo)));

        items.add(UItem.asHeader(LocaleController.getString("DevCategory", R.string.DevCategory)));
        items.add(UItem.asCheck(ITEM_DEV_MODE, LocaleController.getString("DeveloperMode", R.string.DeveloperMode))
                .setChecked(HundredGramConfig.developerModeEnabled));
        items.add(UItem.asShadow(LocaleController.getString("DeveloperModeInfo", R.string.DeveloperModeInfo)));

        if (HundredGramConfig.developerModeEnabled) {
            items.add(UItem.asCheck(ITEM_COPY_CODE_BUTTON, LocaleController.getString("DevCopyCodeButton", R.string.DevCopyCodeButton))
                    .setChecked(HundredGramConfig.copyCodeButtonEnabled));
            items.add(UItem.asShadow(LocaleController.getString("DevCopyCodeButtonInfo", R.string.DevCopyCodeButtonInfo)));

            items.add(UItem.asCheck(ITEM_PRETTY_JSON_VIEWER, LocaleController.getString("PrettyJson", R.string.PrettyJson))
                    .setChecked(HundredGramConfig.prettyJsonViewerEnabled));
            items.add(UItem.asShadow(LocaleController.getString("PrettyJsonTitle", R.string.PrettyJsonTitle)));
        }
    }

    private void fillMediaItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramPhotosSection", R.string.HundredGramPhotosSection)));

        items.add(UItem.asCheck(ITEM_ALWAYS_HD_PHOTOS, LocaleController.getString("AlwaysHighQualityPhotos", R.string.AlwaysHighQualityPhotos))
                .setChecked(HundredGramConfig.alwaysHighQualityPhotos));
        items.add(UItem.asShadow(LocaleController.getString("AlwaysHighQualityPhotosInfo", R.string.AlwaysHighQualityPhotosInfo)));

        String resValue;
        if (HundredGramConfig.photoMaxResolution <= 1280) {
            resValue = LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280);
        } else if (HundredGramConfig.photoMaxResolution <= 2560) {
            resValue = LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560);
        } else {
            resValue = LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840);
        }
        items.add(UItem.asButton(ITEM_PHOTO_MAX_RESOLUTION, LocaleController.getString("PhotoMaxResolution", R.string.PhotoMaxResolution), resValue));
        items.add(UItem.asShadow(LocaleController.getString("PhotoMaxResolutionInfo", R.string.PhotoMaxResolutionInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramVideosSection", R.string.HundredGramVideosSection)));
        items.add(UItem.asCheck(ITEM_SEND_VIDEO_AS_DOCUMENT, LocaleController.getString("SendVideoAsDocument", R.string.SendVideoAsDocument))
                .setChecked(HundredGramConfig.sendVideoAsDocument));
        items.add(UItem.asShadow(LocaleController.getString("SendVideoAsDocumentInfo", R.string.SendVideoAsDocumentInfo)));

        items.add(UItem.asHeader(LocaleController.getString("StickersAndGifsHeader", R.string.StickersAndGifsHeader)));
        String stickerSizeValue;
        switch (HundredGramConfig.stickerSize) {
            case HundredGramConfig.MEDIA_SIZE_MINIMAL:
                stickerSizeValue = LocaleController.getString("MediaSizeMinimal", R.string.MediaSizeMinimal);
                break;
            case HundredGramConfig.MEDIA_SIZE_COMPACT:
                stickerSizeValue = LocaleController.getString("MediaSizeCompact", R.string.MediaSizeCompact);
                break;
            case HundredGramConfig.MEDIA_SIZE_LARGE:
                stickerSizeValue = LocaleController.getString("MediaSizeLarge", R.string.MediaSizeLarge);
                break;
            case HundredGramConfig.MEDIA_SIZE_HUGE:
                stickerSizeValue = LocaleController.getString("MediaSizeHuge", R.string.MediaSizeHuge);
                break;
            case HundredGramConfig.MEDIA_SIZE_DEFAULT:
            default:
                stickerSizeValue = LocaleController.getString("MediaSizeDefault", R.string.MediaSizeDefault);
                break;
        }
        items.add(UItem.asButton(ITEM_STICKER_SIZE, LocaleController.getString("StickerSize", R.string.StickerSize), stickerSizeValue));
        items.add(UItem.asShadow(LocaleController.getString("StickerSizeInfo", R.string.StickerSizeInfo)));

        String gifSizeValue;
        switch (HundredGramConfig.gifSize) {
            case HundredGramConfig.MEDIA_SIZE_MINIMAL:
                gifSizeValue = LocaleController.getString("MediaSizeMinimal", R.string.MediaSizeMinimal);
                break;
            case HundredGramConfig.MEDIA_SIZE_COMPACT:
                gifSizeValue = LocaleController.getString("MediaSizeCompact", R.string.MediaSizeCompact);
                break;
            case HundredGramConfig.MEDIA_SIZE_LARGE:
                gifSizeValue = LocaleController.getString("MediaSizeLarge", R.string.MediaSizeLarge);
                break;
            case HundredGramConfig.MEDIA_SIZE_HUGE:
                gifSizeValue = LocaleController.getString("MediaSizeHuge", R.string.MediaSizeHuge);
                break;
            case HundredGramConfig.MEDIA_SIZE_DEFAULT:
            default:
                gifSizeValue = LocaleController.getString("MediaSizeDefault", R.string.MediaSizeDefault);
                break;
        }
        items.add(UItem.asButton(ITEM_GIF_SIZE, LocaleController.getString("GifSize", R.string.GifSize), gifSizeValue));
        items.add(UItem.asShadow(LocaleController.getString("GifSizeInfo", R.string.GifSizeInfo)));
    }

    private void fillStreamerItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("StreamerMode", R.string.StreamerMode)));

        items.add(UItem.asCheck(ITEM_STREAMER_MODE, LocaleController.getString("StreamerMode", R.string.StreamerMode))
                .setChecked(HundredGramConfig.isStreamerMode()));
        items.add(UItem.asShadow(LocaleController.getString("StreamerModeInfo", R.string.StreamerModeInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_BLOCK_SCREENSHOTS, LocaleController.getString("StreamerBlockScreenshots", R.string.StreamerBlockScreenshots))
                .setChecked(HundredGramConfig.streamerBlockScreenshots));
        items.add(UItem.asShadow(LocaleController.getString("StreamerBlockScreenshotsInfo", R.string.StreamerBlockScreenshotsInfo)));

        items.add(UItem.asButton(ITEM_STREAMER_FAKE_NAME, LocaleController.getString("StreamerFakeName", R.string.StreamerFakeName),
                HundredGramConfig.streamerFakeName != null && !HundredGramConfig.streamerFakeName.isEmpty() ? HundredGramConfig.streamerFakeName : "Streamer"));
        items.add(UItem.asShadow(LocaleController.getString("StreamerFakeNameInfo", R.string.StreamerFakeNameInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_PHONE, LocaleController.getString("StreamerHidePhones", R.string.StreamerHidePhones))
                .setChecked(HundredGramConfig.streamerHidePhoneNumbers));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHidePhonesInfo", R.string.StreamerHidePhonesInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_USERNAMES, LocaleController.getString("StreamerHideUsernames", R.string.StreamerHideUsernames))
                .setChecked(HundredGramConfig.streamerHideUsernames));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideUsernamesInfo", R.string.StreamerHideUsernamesInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_AVATARS, LocaleController.getString("StreamerHideAvatars", R.string.StreamerHideAvatars))
                .setChecked(HundredGramConfig.streamerHideAvatars));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideAvatarsInfo", R.string.StreamerHideAvatarsInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_DIALOG_MESSAGES, LocaleController.getString("StreamerHideDialogMessages", R.string.StreamerHideDialogMessages))
                .setChecked(HundredGramConfig.streamerHideDialogMessages));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideDialogMessagesInfo", R.string.StreamerHideDialogMessagesInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_ID_DC, LocaleController.getString("StreamerHideIdDc", R.string.StreamerHideIdDc))
                .setChecked(HundredGramConfig.streamerHideIdAndDc));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideIdDcInfo", R.string.StreamerHideIdDcInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_NOTIF, LocaleController.getString("StreamerHideNotificationPreview", R.string.StreamerHideNotificationPreview))
                .setChecked(HundredGramConfig.streamerHideNotificationPreview));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideNotificationPreviewInfo", R.string.StreamerHideNotificationPreviewInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_ACCOUNTS, LocaleController.getString("StreamerHideOtherAccounts", R.string.StreamerHideOtherAccounts))
                .setChecked(HundredGramConfig.streamerHideOtherAccounts));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHideOtherAccountsInfo", R.string.StreamerHideOtherAccountsInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_PREVENT_CALLS, LocaleController.getString("StreamerPreventAccidentalCalls", R.string.StreamerPreventAccidentalCalls))
                .setChecked(HundredGramConfig.streamerPreventAccidentalCalls));
        items.add(UItem.asShadow(LocaleController.getString("StreamerPreventAccidentalCallsInfo", R.string.StreamerPreventAccidentalCallsInfo)));

        items.add(UItem.asCheck(ITEM_STREAMER_PREVENT_DELETE, LocaleController.getString("StreamerPreventAccidentalDelete", R.string.StreamerPreventAccidentalDelete))
                .setChecked(HundredGramConfig.streamerPreventAccidentalDelete));
        items.add(UItem.asShadow(LocaleController.getString("StreamerPreventAccidentalDeleteInfo", R.string.StreamerPreventAccidentalDeleteInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            // CATEGORY_VAULT
            case ITEM_AUTO_DOWNLOAD_MEDIA_VAULT:
                HundredGramConfig.toggleAutoDownloadMediaVault();
                item.setChecked(HundredGramConfig.autoDownloadMediaVault);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.autoDownloadMediaVault);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_AUTO_DOWNLOAD_MAX_SIZE:
                showMediaVaultLimitDialog();
                break;
            case ITEM_AUTO_DOWNLOAD_CHATS_SCOPE:
                showMediaVaultScopeDialog();
                break;
            case ITEM_VAULT_INCLUDE_ARCHIVED:
                HundredGramConfig.toggleVaultIncludeArchivedChats();
                item.setChecked(HundredGramConfig.vaultIncludeArchivedChats);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.vaultIncludeArchivedChats);
                }
                break;
            case ITEM_AUTO_DOWNLOAD_ONLY_WIFI:
                HundredGramConfig.toggleAutoDownloadOnlyWifi();
                item.setChecked(HundredGramConfig.autoDownloadOnlyWifi);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.autoDownloadOnlyWifi);
                }
                break;
            case ITEM_AUTO_DOWNLOAD_EXCEPTIONS:
                presentFragment(new HundredGramVaultExceptionsActivity());
                break;
            case ITEM_BACKGROUND_ARCHIVER:
                HundredGramConfig.toggleBackgroundArchiverEnabled();
                item.setChecked(HundredGramConfig.backgroundArchiverEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.backgroundArchiverEnabled);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_BACKGROUND_ARCHIVER_MODE:
                showArchiverModeDialog();
                break;
            case ITEM_VAULT_ARCHIVE_COMMENTS:
                HundredGramConfig.toggleVaultArchiveChannelComments();
                item.setChecked(HundredGramConfig.vaultArchiveChannelComments);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.vaultArchiveChannelComments);
                }
                break;
            case ITEM_VAULT_BROWSE_FILES:
                presentFragment(new HundredGramVaultViewerActivity());
                break;
            case ITEM_CLEAR_ALL_VAULT:
                showClearAllVaultConfirmDialog();
                break;
            case ITEM_PROTECTED_VAULT_SIZE:
                presentFragment(new HundredGramVaultViewerActivity());
                break;
            case ITEM_CLEAR_PROTECTED_VAULT:
                showClearProtectedVaultConfirmDialog();
                break;
            case ITEM_VAULT_TTL:
                showVaultTtlDialog();
                break;
            case ITEM_VAULT_MAX_SIZE_QUOTA:
                showVaultMaxSizeDialog();
                break;
            case ITEM_VAULT_CLEAN_EXPIRED_NOW:
                HundredGramVaultRotator.runRotation(currentAccount, (freedBytes, deletedCount) -> {
                    if (freedBytes > 0) {
                        String sizeStr = AndroidUtilities.formatFileSize(freedBytes);
                        BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(R.raw.done, LocaleController.formatString("VaultCleanExpiredDone", R.string.VaultCleanExpiredDone, sizeStr)).show();
                    } else {
                        BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("VaultCleanNoExpired", R.string.VaultCleanNoExpired)).show();
                    }
                });
                break;

            // CATEGORY_TRACKER
            case ITEM_TRACK_ONLINE_STATUS:
                HundredGramConfig.toggleTrackOnlineStatus();
                item.setChecked(HundredGramConfig.trackOnlineStatus);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.trackOnlineStatus);
                }
                break;
            case ITEM_NOTIFY_ON_USER_ONLINE:
                HundredGramConfig.toggleNotifyOnUserOnline();
                item.setChecked(HundredGramConfig.notifyOnUserOnline);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.notifyOnUserOnline);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_NOTIFY_ALL_CONTACTS_ONLINE:
                HundredGramConfig.toggleNotifyAllContactsOnline();
                item.setChecked(HundredGramConfig.notifyAllContactsOnline);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.notifyAllContactsOnline);
                }
                break;
            case ITEM_TRACKED_USERS_LIST:
                presentFragment(new HundredGramTrackedUsersActivity());
                break;
            case ITEM_TRACK_SELF_ACTIVITY:
                HundredGramConfig.toggleTrackSelfActivity();
                item.setChecked(HundredGramConfig.trackSelfActivity);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.trackSelfActivity);
                }
                break;
            case ITEM_MY_ACTIVITY_LOG:
                new org.telegram.ui.Components.HundredGramUserActivityBottomSheet(HundredGramSubSettingsActivity.this, getUserConfig().getClientUserId()).show();
                break;

            // CATEGORY_PRIVACY
            case ITEM_HIDE_PHONE_NUMBER:
                HundredGramConfig.toggleHidePhoneNumber();
                item.setChecked(HundredGramConfig.hidePhoneNumber);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hidePhoneNumber);
                }
                break;
            case ITEM_HIDE_OTHER_PHONE_NUMBERS:
                HundredGramConfig.toggleHideOtherPhoneNumbers();
                item.setChecked(HundredGramConfig.hideOtherPhoneNumbers);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideOtherPhoneNumbers);
                }
                break;
            case ITEM_HIDE_USERNAME:
                HundredGramConfig.toggleHideUsername();
                item.setChecked(HundredGramConfig.hideUsername);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideUsername);
                }
                break;
            case ITEM_HIDE_OTHER_USERNAMES:
                HundredGramConfig.toggleHideOtherUsernames();
                item.setChecked(HundredGramConfig.hideOtherUsernames);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideOtherUsernames);
                }
                break;
            case ITEM_SHOW_ID_AND_DC:
                HundredGramConfig.toggleShowIdAndDc();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_KEEPALIVE_FOREGROUND_SERVICE:
                HundredGramConfig.toggleKeepAliveForegroundService();
                item.setChecked(HundredGramConfig.keepAliveForegroundService);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.keepAliveForegroundService);
                }
                break;
            case ITEM_BATTERY_OPTIMIZATIONS:
                openBatteryOptimizationSettings();
                break;

            // CATEGORY_GHOST
            case ITEM_GHOST_ONLINE:
                HundredGramConfig.toggleGhostOnline();
                item.setChecked(HundredGramConfig.ghostOnline);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostOnline);
                }
                break;
            case ITEM_GHOST_READ_MESSAGES:
                HundredGramConfig.toggleGhostReadMessages();
                item.setChecked(HundredGramConfig.ghostReadMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostReadMessages);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_GHOST_READ_ON_REPLY:
                HundredGramConfig.toggleGhostReadOnReply();
                item.setChecked(HundredGramConfig.ghostReadOnReply);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostReadOnReply);
                }
                break;
            case ITEM_GHOST_READ_MEDIA_CONTENTS:
                HundredGramConfig.toggleGhostReadMediaContents();
                item.setChecked(HundredGramConfig.ghostReadMediaContents);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostReadMediaContents);
                }
                break;
            case ITEM_GHOST_HIDE_READ_DATE:
                HundredGramConfig.toggleGhostHideReadDate();
                item.setChecked(HundredGramConfig.ghostHideReadDate);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostHideReadDate);
                }
                break;
            case ITEM_GHOST_SHOW_MARK_AS_READ:
                HundredGramConfig.toggleGhostShowMarkAsReadOption();
                item.setChecked(HundredGramConfig.ghostShowMarkAsReadOption);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostShowMarkAsReadOption);
                }
                break;
            case ITEM_GHOST_TYPING:
                HundredGramConfig.toggleGhostTyping();
                item.setChecked(HundredGramConfig.ghostTyping);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostTyping);
                }
                break;
            case ITEM_GHOST_STORIES_MODE:
                showGhostStoriesModeDialog();
                break;
            case ITEM_GHOST_UNLOCK_STORY_SAVING:
                HundredGramConfig.toggleUnlockStorySaving();
                item.setChecked(HundredGramConfig.unlockStorySaving);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.unlockStorySaving);
                }
                break;
            case ITEM_AUTO_CHECK_DELETE_FOR_ALL:
                HundredGramConfig.toggleAutoCheckDeleteForAll();
                item.setChecked(HundredGramConfig.autoCheckDeleteForAll);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.autoCheckDeleteForAll);
                }
                break;

            // CATEGORY_INTERFACE
            case ITEM_UNLOCK_CHAT_TRANSLATION:
                HundredGramConfig.toggleUnlockChatTranslation();
                item.setChecked(HundredGramConfig.unlockChatTranslation);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.unlockChatTranslation);
                }
                break;
            case ITEM_RELATIVE_LAST_SEEN:
                HundredGramConfig.toggleRelativeLastSeen();
                item.setChecked(HundredGramConfig.relativeLastSeen);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.relativeLastSeen);
                }
                break;
            case ITEM_DISABLE_JUMP_TO_NEXT_CHANNEL:
                HundredGramConfig.toggleDisableJumpToNextChannel();
                item.setChecked(HundredGramConfig.disableJumpToNextChannel);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.disableJumpToNextChannel);
                }
                break;
            case ITEM_HIDE_KEYBOARD_ON_SCROLL:
                HundredGramConfig.toggleHideKeyboardOnScroll();
                item.setChecked(HundredGramConfig.hideKeyboardOnScroll);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideKeyboardOnScroll);
                }
                break;
            case ITEM_ADD_COMMA_AFTER_MENTION:
                HundredGramConfig.toggleAddCommaAfterMention();
                item.setChecked(HundredGramConfig.addCommaAfterMention);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.addCommaAfterMention);
                }
                break;
            case ITEM_DISABLE_NUMBER_ROUNDING:
                HundredGramConfig.toggleDisableNumberRounding();
                item.setChecked(HundredGramConfig.disableNumberRounding);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.disableNumberRounding);
                }
                break;
            case ITEM_HIDE_ARCHIVE_FOLDER:
                HundredGramConfig.toggleHideArchiveFolder();
                item.setChecked(HundredGramConfig.hideArchiveFolder);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideArchiveFolder);
                }
                break;
            case ITEM_HIDE_FLOATING_BUTTON:
                HundredGramConfig.toggleHideFloatingButton();
                item.setChecked(HundredGramConfig.hideFloatingButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideFloatingButton);
                }
                break;
            case ITEM_HIGHLIGHT_DELETED:
                HundredGramConfig.toggleHighlightDeletedMessages();
                item.setChecked(HundredGramConfig.highlightDeletedMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.highlightDeletedMessages);
                }
                break;
            case ITEM_HIDE_SIMILAR_CHANNELS:
                HundredGramConfig.toggleHideSimilarChannels();
                item.setChecked(HundredGramConfig.hideSimilarChannels);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideSimilarChannels);
                }
                break;
            case ITEM_HIDE_STORIES_BAR:
                HundredGramConfig.toggleHideStoriesBar();
                item.setChecked(HundredGramConfig.hideStoriesBar);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideStoriesBar);
                }
                break;
            case ITEM_SHOW_SECONDS_IN_TIME:
                HundredGramConfig.toggleShowSecondsInTime();
                item.setChecked(HundredGramConfig.showSecondsInTime);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showSecondsInTime);
                }
                break;
            case ITEM_AVATAR_HISTORY_ENABLED:
                HundredGramConfig.toggleAvatarHistoryEnabled();
                item.setChecked(HundredGramConfig.avatarHistoryEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.avatarHistoryEnabled);
                }
                break;

            // CATEGORY_ACTIONS
            case ITEM_DOUBLE_TAP_INCOMING:
                showDoubleTapDialog(false);
                break;
            case ITEM_DOUBLE_TAP_OUTGOING:
                showDoubleTapDialog(true);
                break;
            case ITEM_SHOW_SAVE_MESSAGE:
                HundredGramConfig.toggleShowSaveMessageButton();
                item.setChecked(HundredGramConfig.showSaveMessageButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showSaveMessageButton);
                }
                break;
            case ITEM_SHOW_DETAILS:
                HundredGramConfig.toggleShowDetailsButton();
                item.setChecked(HundredGramConfig.showDetailsButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showDetailsButton);
                }
                break;
            case ITEM_SHOW_COPY_PHOTO:
                HundredGramConfig.toggleShowCopyPhotoButton();
                item.setChecked(HundredGramConfig.showCopyPhotoButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showCopyPhotoButton);
                }
                break;
            case ITEM_SHOW_EDIT_HISTORY:
                HundredGramConfig.toggleShowEditHistoryButton();
                item.setChecked(HundredGramConfig.showEditHistoryButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showEditHistoryButton);
                }
                break;
            case ITEM_SHOW_FORWARD_WITHOUT_AUTHOR:
                HundredGramConfig.toggleShowForwardWithoutAuthorButton();
                item.setChecked(HundredGramConfig.showForwardWithoutAuthorButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showForwardWithoutAuthorButton);
                }
                break;
            case ITEM_SILENT_SEND_BY_DEFAULT:
                HundredGramConfig.toggleSilentSendByDefault();
                item.setChecked(HundredGramConfig.silentSendByDefault);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.silentSendByDefault);
                }
                break;
            case ITEM_ALWAYS_FORWARD_WITHOUT_AUTHOR:
                HundredGramConfig.toggleAlwaysForwardWithoutAuthor();
                item.setChecked(HundredGramConfig.alwaysForwardWithoutAuthor);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.alwaysForwardWithoutAuthor);
                }
                break;
            case ITEM_CONFIRM_SEND_STICKERS:
                HundredGramConfig.toggleConfirmSendStickers();
                item.setChecked(HundredGramConfig.confirmSendStickers);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.confirmSendStickers);
                }
                break;
            case ITEM_CONFIRM_SEND_GIFS:
                HundredGramConfig.toggleConfirmSendGifs();
                item.setChecked(HundredGramConfig.confirmSendGifs);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.confirmSendGifs);
                }
                break;
            case ITEM_DEV_MODE:
                HundredGramConfig.toggleDeveloperMode();
                item.setChecked(HundredGramConfig.developerModeEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.developerModeEnabled);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_COPY_CODE_BUTTON:
                HundredGramConfig.toggleCopyCodeButton();
                item.setChecked(HundredGramConfig.copyCodeButtonEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.copyCodeButtonEnabled);
                }
                break;
            case ITEM_PRETTY_JSON_VIEWER:
                HundredGramConfig.togglePrettyJsonViewer();
                item.setChecked(HundredGramConfig.prettyJsonViewerEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.prettyJsonViewerEnabled);
                }
                break;

            case ITEM_CONFIRM_CALLS:
                HundredGramConfig.toggleConfirmCalls();
                item.setChecked(HundredGramConfig.confirmCalls);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.confirmCalls);
                }
                break;
            case ITEM_CLEAN_URLS:
                HundredGramConfig.toggleCleanUrls();
                item.setChecked(HundredGramConfig.cleanUrls);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.cleanUrls);
                }
                break;

            // CATEGORY_MEDIA
            case ITEM_ALWAYS_HD_PHOTOS:
                HundredGramConfig.toggleAlwaysHighQualityPhotos();
                item.setChecked(HundredGramConfig.alwaysHighQualityPhotos);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.alwaysHighQualityPhotos);
                }
                break;
            case ITEM_PHOTO_MAX_RESOLUTION:
                showPhotoMaxResolutionDialog();
                break;
            case ITEM_SEND_VIDEO_AS_DOCUMENT:
                HundredGramConfig.toggleSendVideoAsDocument();
                item.setChecked(HundredGramConfig.sendVideoAsDocument);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.sendVideoAsDocument);
                }
                break;
            case ITEM_STICKER_SIZE:
                showStickerSizeDialog();
                break;
            case ITEM_GIF_SIZE:
                showGifSizeDialog();
                break;

            // CATEGORY_STREAMER
            case ITEM_STREAMER_MODE:
                HundredGramConfig.toggleStreamerMode();
                item.setChecked(HundredGramConfig.isStreamerMode());
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.isStreamerMode());
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_STREAMER_BLOCK_SCREENSHOTS:
                HundredGramConfig.toggleStreamerBlockScreenshots();
                item.setChecked(HundredGramConfig.streamerBlockScreenshots);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerBlockScreenshots);
                }
                break;
            case ITEM_STREAMER_FAKE_NAME:
                showStreamerFakeNameDialog();
                break;
            case ITEM_STREAMER_HIDE_PHONE:
                HundredGramConfig.toggleStreamerHidePhoneNumbers();
                item.setChecked(HundredGramConfig.streamerHidePhoneNumbers);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHidePhoneNumbers);
                }
                break;
            case ITEM_STREAMER_HIDE_USERNAMES:
                HundredGramConfig.toggleStreamerHideUsernames();
                item.setChecked(HundredGramConfig.streamerHideUsernames);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideUsernames);
                }
                break;
            case ITEM_STREAMER_HIDE_AVATARS:
                HundredGramConfig.toggleStreamerHideAvatars();
                item.setChecked(HundredGramConfig.streamerHideAvatars);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideAvatars);
                }
                break;
            case ITEM_STREAMER_HIDE_DIALOG_MESSAGES:
                HundredGramConfig.toggleStreamerHideDialogMessages();
                item.setChecked(HundredGramConfig.streamerHideDialogMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideDialogMessages);
                }
                break;
            case ITEM_STREAMER_HIDE_ID_DC:
                HundredGramConfig.toggleStreamerHideIdAndDc();
                item.setChecked(HundredGramConfig.streamerHideIdAndDc);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideIdAndDc);
                }
                break;
            case ITEM_STREAMER_HIDE_NOTIF:
                HundredGramConfig.toggleStreamerHideNotificationPreview();
                item.setChecked(HundredGramConfig.streamerHideNotificationPreview);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideNotificationPreview);
                }
                break;
            case ITEM_STREAMER_HIDE_ACCOUNTS:
                HundredGramConfig.toggleStreamerHideOtherAccounts();
                item.setChecked(HundredGramConfig.streamerHideOtherAccounts);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerHideOtherAccounts);
                }
                break;
            case ITEM_STREAMER_PREVENT_CALLS:
                HundredGramConfig.toggleStreamerPreventAccidentalCalls();
                item.setChecked(HundredGramConfig.streamerPreventAccidentalCalls);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerPreventAccidentalCalls);
                }
                break;
            case ITEM_STREAMER_PREVENT_DELETE:
                HundredGramConfig.toggleStreamerPreventAccidentalDelete();
                item.setChecked(HundredGramConfig.streamerPreventAccidentalDelete);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.streamerPreventAccidentalDelete);
                }
                break;
            case ITEM_PRIVACY_BLUR_MESSAGES:
                HundredGramConfig.togglePrivacyBlurDialogMessages();
                item.setChecked(HundredGramConfig.privacyBlurDialogMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.privacyBlurDialogMessages);
                }
                break;
            case ITEM_PRIVACY_BLUR_AVATARS:
                HundredGramConfig.togglePrivacyBlurAvatars();
                item.setChecked(HundredGramConfig.privacyBlurAvatars);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.privacyBlurAvatars);
                }
                break;
        }
    }

    private void showStreamerFakeNameDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("StreamerFakeName", R.string.StreamerFakeName));

        final android.widget.EditText editText = new android.widget.EditText(getParentActivity());
        editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setText(HundredGramConfig.streamerFakeName != null ? HundredGramConfig.streamerFakeName : "Streamer");
        editText.setSelection(editText.getText().length());
        editText.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, android.view.Gravity.CENTER, 24, 8, 24, 8));
        builder.setView(frameLayout);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            String newName = editText.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = "Streamer";
            }
            HundredGramConfig.setStreamerFakeName(newName);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showPhotoMaxResolutionDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280),
            LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560),
            LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840)
        };
        int[] values = new int[] { 1280, 2560, 3840 };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("PhotoMaxResolution", R.string.PhotoMaxResolution));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setPhotoMaxResolution(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.show();
    }

    private void showStickerSizeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("MediaSizeMinimal", R.string.MediaSizeMinimal),
            LocaleController.getString("MediaSizeCompact", R.string.MediaSizeCompact),
            LocaleController.getString("MediaSizeDefault", R.string.MediaSizeDefault),
            LocaleController.getString("MediaSizeLarge", R.string.MediaSizeLarge),
            LocaleController.getString("MediaSizeHuge", R.string.MediaSizeHuge)
        };
        int[] values = new int[] {
            HundredGramConfig.MEDIA_SIZE_MINIMAL,
            HundredGramConfig.MEDIA_SIZE_COMPACT,
            HundredGramConfig.MEDIA_SIZE_DEFAULT,
            HundredGramConfig.MEDIA_SIZE_LARGE,
            HundredGramConfig.MEDIA_SIZE_HUGE
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("StickerSize", R.string.StickerSize));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setStickerSize(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        builder.show();
    }

    private void showGifSizeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("MediaSizeMinimal", R.string.MediaSizeMinimal),
            LocaleController.getString("MediaSizeCompact", R.string.MediaSizeCompact),
            LocaleController.getString("MediaSizeDefault", R.string.MediaSizeDefault),
            LocaleController.getString("MediaSizeLarge", R.string.MediaSizeLarge),
            LocaleController.getString("MediaSizeHuge", R.string.MediaSizeHuge)
        };
        int[] values = new int[] {
            HundredGramConfig.MEDIA_SIZE_MINIMAL,
            HundredGramConfig.MEDIA_SIZE_COMPACT,
            HundredGramConfig.MEDIA_SIZE_DEFAULT,
            HundredGramConfig.MEDIA_SIZE_LARGE,
            HundredGramConfig.MEDIA_SIZE_HUGE
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("GifSize", R.string.GifSize));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setGifSize(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        builder.show();
    }

    private void showClearAllVaultConfirmDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultClearAll", R.string.HundredGramVaultClearAll));
        builder.setMessage(LocaleController.getString("HundredGramVaultClearAllConfirm", R.string.HundredGramVaultClearAllConfirm));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            FileLoader.getInstance(currentAccount).cancelLoadAllFiles();
            FileLoader.getInstance(currentAccount).getFileDatabase().clearAllVaultFiles(freedBytes -> {
                String sizeStr = AndroidUtilities.formatFileSize(freedBytes);
                if (BulletinFactory.canShowBulletin(HundredGramSubSettingsActivity.this)) {
                    BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(R.raw.ic_delete, LocaleController.formatString("HundredGramVaultClearedAll", R.string.HundredGramVaultClearedAll, sizeStr)).show();
                }
            });
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void showClearProtectedVaultConfirmDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramProtectedVaultClear", R.string.HundredGramProtectedVaultClear));
        builder.setMessage(LocaleController.getString("HundredGramProtectedVaultClearConfirm", R.string.HundredGramProtectedVaultClearConfirm));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            long totalSize = HundredGramVaultManager.getInstance(currentAccount).getVaultTotalSize();
            String sizeStr = AndroidUtilities.formatFileSize(totalSize);
            HundredGramVaultManager.getInstance(currentAccount).clearVault(0, () -> {
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                if (BulletinFactory.canShowBulletin(HundredGramSubSettingsActivity.this)) {
                    BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(
                            R.raw.ic_delete,
                            LocaleController.formatString("HundredGramProtectedVaultCleared", R.string.HundredGramProtectedVaultCleared, sizeStr)
                    ).show();
                }
            });
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void showMediaVaultLimitDialog() {
        if (getParentActivity() == null) return;
        String mb = LocaleController.getString("HundredGramMB", R.string.HundredGramMB);
        String[] options = new String[] { "5 " + mb, "15 " + mb, "50 " + mb, "100 " + mb, "250 " + mb };
        int[] values = new int[] { 5, 15, 50, 100, 250 };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramMediaVaultLimit", R.string.HundredGramMediaVaultLimit));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setAutoDownloadMaxSizeMb(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private void showMediaVaultScopeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("HundredGramMediaVaultScopePrivate", R.string.HundredGramMediaVaultScopePrivate),
            LocaleController.getString("HundredGramMediaVaultScopeGroups", R.string.HundredGramMediaVaultScopeGroups),
            LocaleController.getString("HundredGramMediaVaultScopeAll", R.string.HundredGramMediaVaultScopeAll)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramMediaVaultScope", R.string.HundredGramMediaVaultScope));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setAutoDownloadChatsScope(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private void showArchiverModeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("HundredGramArchiverModeNewOnly", R.string.HundredGramArchiverModeNewOnly),
            LocaleController.getString("HundredGramArchiverModeFullSync", R.string.HundredGramArchiverModeFullSync)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramArchiverMode", R.string.HundredGramArchiverMode));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setBackgroundArchiverMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private void showDoubleTapDialog(boolean outOwner) {
        if (getParentActivity() == null) return;
        String[] actions = new String[] {
            LocaleController.getString("DoubleTapNone", R.string.DoubleTapNone),
            LocaleController.getString("DoubleTapReaction", R.string.DoubleTapReaction),
            LocaleController.getString("DoubleTapReply", R.string.DoubleTapReply),
            LocaleController.getString("DoubleTapCopy", R.string.DoubleTapCopy),
            LocaleController.getString("DoubleTapForward", R.string.DoubleTapForward),
            LocaleController.getString("DoubleTapEdit", R.string.DoubleTapEdit),
            LocaleController.getString("DoubleTapSave", R.string.DoubleTapSave),
            LocaleController.getString("DoubleTapDelete", R.string.DoubleTapDelete)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(outOwner ? LocaleController.getString("DoubleTapActionOutgoing", R.string.DoubleTapActionOutgoing) : LocaleController.getString("DoubleTapActionIncoming", R.string.DoubleTapActionIncoming));
        builder.setItems(actions, (dialog, which) -> {
            if (outOwner) {
                HundredGramConfig.setDoubleTapActionOutOwner(which);
            } else {
                HundredGramConfig.setDoubleTapAction(which);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private String getDoubleTapActionName(int action) {
        switch (action) {
            case HundredGramConfig.DOUBLE_TAP_NONE: return LocaleController.getString("DoubleTapNone", R.string.DoubleTapNone);
            case HundredGramConfig.DOUBLE_TAP_REACTION: return LocaleController.getString("DoubleTapReaction", R.string.DoubleTapReaction);
            case HundredGramConfig.DOUBLE_TAP_REPLY: return LocaleController.getString("DoubleTapReply", R.string.DoubleTapReply);
            case HundredGramConfig.DOUBLE_TAP_COPY: return LocaleController.getString("DoubleTapCopy", R.string.DoubleTapCopy);
            case HundredGramConfig.DOUBLE_TAP_FORWARD: return LocaleController.getString("DoubleTapForward", R.string.DoubleTapForward);
            case HundredGramConfig.DOUBLE_TAP_EDIT: return LocaleController.getString("DoubleTapEdit", R.string.DoubleTapEdit);
            case HundredGramConfig.DOUBLE_TAP_SAVE: return LocaleController.getString("DoubleTapSave", R.string.DoubleTapSave);
            case HundredGramConfig.DOUBLE_TAP_DELETE: return LocaleController.getString("DoubleTapDelete", R.string.DoubleTapDelete);
            default: return LocaleController.getString("DoubleTapReaction", R.string.DoubleTapReaction);
        }
    }

    private void showGhostStoriesModeDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("GhostStoriesMode", R.string.GhostStoriesMode));
        String[] options = new String[]{
                LocaleController.getString("GhostStoriesOff", R.string.GhostStoriesOff),
                LocaleController.getString("GhostStoriesAlways", R.string.GhostStoriesAlways),
                LocaleController.getString("GhostStoriesPrompt", R.string.GhostStoriesPrompt)
        };
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setGhostStoriesMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openBatteryOptimizationSettings() {
        if (getParentActivity() == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = ApplicationLoader.applicationContext.getPackageName();
                PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                } else {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                }
                getParentActivity().startActivity(intent);
                return;
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            getParentActivity().startActivity(intent);
        } catch (Throwable e) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                getParentActivity().startActivity(intent);
            } catch (Throwable ex) {
                FileLog.e(ex);
            }
        }
    }

    private void showVaultTtlDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VaultTtl", R.string.VaultTtl));
        String[] options = new String[]{
                LocaleController.getString("VaultTtlForever", R.string.VaultTtlForever),
                LocaleController.getString("VaultTtl7Days", R.string.VaultTtl7Days),
                LocaleController.getString("VaultTtl30Days", R.string.VaultTtl30Days),
                LocaleController.getString("VaultTtl60Days", R.string.VaultTtl60Days),
                LocaleController.getString("VaultTtl90Days", R.string.VaultTtl90Days)
        };
        final int[] values = new int[]{0, 7, 30, 60, 90};
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setVaultTtlDays(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showVaultMaxSizeDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VaultMaxSize", R.string.VaultMaxSize));
        String[] options = new String[]{
                LocaleController.getString("VaultMaxSizeUnlimited", R.string.VaultMaxSizeUnlimited),
                LocaleController.getString("VaultMaxSize2Gb", R.string.VaultMaxSize2Gb),
                LocaleController.getString("VaultMaxSize5Gb", R.string.VaultMaxSize5Gb),
                LocaleController.getString("VaultMaxSize10Gb", R.string.VaultMaxSize10Gb),
                LocaleController.getString("VaultMaxSize20Gb", R.string.VaultMaxSize20Gb)
        };
        final int[] values = new int[]{0, 2, 5, 10, 20};
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setVaultMaxSizeGb(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
