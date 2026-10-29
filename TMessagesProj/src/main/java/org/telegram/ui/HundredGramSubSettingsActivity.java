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
import org.telegram.messenger.HundredGramActivitySimulator;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.HundredGramContentFilterController;
import org.telegram.messenger.HundredGramHaptics;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
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
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.AvatarCornersPreviewCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MessageBubblePreviewCell;
import org.telegram.ui.Components.StickerSizePreviewCell;
import org.telegram.ui.Components.SwipeActionsPreviewCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.io.File;
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
    private static final int ITEM_SEND_OFFLINE_AFTER_ONLINE = 237;
    private static final int ITEM_GHOST_UPLOAD_PROGRESS = 238;
    private static final int ITEM_TRACK_READ_DATE = 236;

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
    private static final int ITEM_VAULT_PRUNE_ORPHANS = 230;
    private static final int ITEM_VAULT_EXPORT_ALL_ZIP = 231;
    private static final int ITEM_VAULT_SAVE_FOR_BOTS = 235;

    // Item IDs for CATEGORY_VAULT Presets, Scopes & Data Types
    private static final int ITEM_SAVE_PRESET = 270;
    private static final int ITEM_SAVE_SCOPE_PRIVATE = 271;
    private static final int ITEM_SAVE_SCOPE_GROUPS = 272;
    private static final int ITEM_SAVE_SCOPE_CHANNELS = 273;
    private static final int ITEM_SAVE_SCOPE_BOTS = 274;
    private static final int ITEM_SAVE_SCOPE_ARCHIVED = 275;
    private static final int ITEM_SAVE_ANTI_DELETE = 276;
    private static final int ITEM_SAVE_EDIT_HISTORY = 277;
    private static final int ITEM_SAVE_MEDIA_PHOTOS = 278;
    private static final int ITEM_SAVE_MEDIA_VOICE_ROUNDS = 279;
    private static final int ITEM_SAVE_MEDIA_VIDEOS = 280;
    private static final int ITEM_SAVE_MEDIA_FILES = 281;

    // Item IDs for CATEGORY_TRACKER
    private static final int ITEM_TRACK_ONLINE_STATUS = 10;
    private static final int ITEM_NOTIFY_ON_USER_ONLINE = 11;
    private static final int ITEM_NOTIFY_ALL_CONTACTS_ONLINE = 181;
    private static final int ITEM_TRACKED_USERS_LIST = 182;
    private static final int ITEM_TRACK_SELF_ACTIVITY = 187;
    private static final int ITEM_MY_ACTIVITY_LOG = 188;
    private static final int ITEM_ACTIVITY_SIMULATOR = 390;
    private static final int ITEM_AVATAR_HISTORY_ENABLED = 97;
    private static final int ITEM_PROFILE_HISTORY_ENABLED = 98;

    // Item IDs for CATEGORY_PRIVACY
    private static final int ITEM_HIDE_PHONE_NUMBER = 20;
    private static final int ITEM_HIDE_OTHER_PHONE_NUMBERS = 21;
    private static final int ITEM_HIDE_USERNAME = 22;
    private static final int ITEM_HIDE_OTHER_USERNAMES = 23;
    private static final int ITEM_SHOW_ID_AND_DC = 24;
    private static final int ITEM_KEEPALIVE_FOREGROUND_SERVICE = 25;
    private static final int ITEM_BATTERY_OPTIMIZATIONS = 26;
    private static final int ITEM_SHADOWBAN = 195;
    private static final int ITEM_NEW_SLIDER_STYLE = 391;
    private static final int ITEM_FOLDER_SWIPE = 383;
    private static final int ITEM_FOLDER_SWIPE_SHRINK = 384;
    private static final int ITEM_FOLDER_SWIPE_SPRING = 385;
    private static final int ITEM_MESSAGE_MENU_ORDER = 392;
    private static final int ITEM_ADBLOCK = 350;

    // Item IDs for CATEGORY_INTERFACE
    private static final int ITEM_RELATIVE_LAST_SEEN = 30;
    private static final int ITEM_DISABLE_JUMP_TO_NEXT_CHANNEL = 31;
    private static final int ITEM_HIDE_KEYBOARD_ON_SCROLL = 32;
    private static final int ITEM_ADD_COMMA_AFTER_MENTION = 33;
    private static final int ITEM_DISABLE_GREETING_STICKER = 232;
    private static final int ITEM_SQUARE_FAB = 233;
    private static final int ITEM_INLINE_MATH = 234;
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
    private static final int ITEM_SHOW_WEEKDAY_IN_DATE = 300;
    private static final int ITEM_FORWARD_TIME = 301;
    private static final int ITEM_SELECTION_SPEED = 302;
    private static final int ITEM_SELECTION_NO_LIMIT = 303;
    private static final int ITEM_MEDIA_GLOW = 304;
    private static final int ITEM_THANOS_DELETE_EFFECT = 305;
    private static final int ITEM_PILL_STACK_ENABLED = 96;
    private static final int ITEM_PILL_STACK_SETTINGS = 193;
    private static final int ITEM_AVATAR_CORNERS_PREVIEW = 195;
    private static final int ITEM_MD3_SWITCH_STYLE = 196;
    private static final int ITEM_BOTTOM_NAV_HIDE_CALLS = 197;
    private static final int ITEM_BOTTOM_NAV_HIDE_CONTACTS = 198;
    private static final int ITEM_BOTTOM_NAV_HIDE_SETTINGS = 199;
    private static final int ITEM_BOTTOM_NAV_SHOW_CALLS = 200;
    private static final int ITEM_BOTTOM_NAV_SHOW_ARCHIVE = 380;
    private static final int ITEM_BOTTOM_NAV_SHOW_FEED = 381;
    private static final int ITEM_BOTTOM_NAV_SHOW_MUSIC = 382;
    private static final int ITEM_FILTER_ZALGO = 206;
    private static final int ITEM_REPLACE_EDITED_WITH_ICON = 207;
    private static final int ITEM_ACTION_BAR_TITLE_MODE = 208;
    private static final int ITEM_CENTER_ACTION_BAR_TITLE = 209;
    private static final int ITEM_TAB_TITLE_STYLE = 210;
    private static final int ITEM_TAB_COUNTER_MODE = 211;
    private static final int ITEM_REMOVE_MESSAGE_TAIL = 214;
    private static final int ITEM_WIDE_POSTS_IN_CHANNELS = 215;
    private static final int ITEM_HIDE_DIALOGS_SEARCH_BAR = 217;
    private static final int ITEM_QUICK_CHANNEL_TRANSITION = 219;
    private static final int ITEM_SHOW_FORWARDS_COUNT = 220;
    private static final int ITEM_HIDE_ACTION_BAR_STATUS = 221;
    private static final int ITEM_QUICK_TOPIC_TRANSITION = 223;
    private static final int ITEM_TRANSITION_ANIMATION = 224;
    private static final int ITEM_DIVIDER_STYLE = 225;
    private static final int ITEM_MESSAGE_BUBBLE_PREVIEW = 226;
    private static final int ITEM_SWIPE_ACTIONS_PREVIEW = 227;
    private static final int ITEM_STICKER_SIZE_PREVIEW = 228;
    private static final int ITEM_SENDER_MINI_AVATARS = 240;
    private static final int ITEM_HIDE_SEND_AS_PEER = 241;
    private static final int ITEM_HIDE_SHARE_BUTTON = 242;
    private static final int ITEM_HIDE_REACTIONS_IN_CHANNELS = 243;
    private static final int ITEM_HIDE_REACTIONS_IN_GROUPS = 244;
    private static final int ITEM_PAUSE_ON_MINIMIZE_VIDEO = 245;
    private static final int ITEM_PAUSE_ON_MINIMIZE_VOICE = 246;
    private static final int ITEM_PAUSE_ON_MINIMIZE_ROUND = 247;
    private static final int ITEM_DISABLE_NOTIFICATION_DELAY = 250;
    private static final int ITEM_ARCHIVE_ON_PULL = 251;
    private static final int ITEM_CHANNEL_BOTTOM_BUTTON = 252;
    private static final int ITEM_STICKER_TIME_MODE = 254;
    private static final int ITEM_DELETED_MARK_ICON = 295;
    private static final int ITEM_DELETED_MARK_COLOR = 298;
    private static final int ITEM_DELETED_MESSAGES_TRANSPARENT = 296;
    private static final int ITEM_CONTENT_FILTER = 297;

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
    private static final int ITEM_LOCAL_STT_ENABLED = 66;
    private static final int ITEM_LOCAL_STT_MODEL = 67;
    private static final int ITEM_CHAT_MESSAGE_SWIPE_ACTION = 144;
    private static final int ITEM_SHOW_REPEAT_MESSAGE = 203;
    private static final int ITEM_SHOW_RESULTS_BEFORE_VOTING = 204;
    private static final int ITEM_SHOW_CLEAR_INPUT = 205;
    private static final int ITEM_DISABLE_UNARCHIVE_SWIPE = 216;
    private static final int ITEM_SHOW_REAL_NAME = 299;
    private static final int ITEM_PINNED_DOWNLOADS = 330;
    private static final int ITEM_HIDE_ALL_CHATS_FOLDER = 331;

    // Item IDs for CATEGORY_MEDIA
    private static final int ITEM_ALWAYS_HD_PHOTOS = 60;
    private static final int ITEM_PHOTO_MAX_RESOLUTION = 61;
    private static final int ITEM_SEND_VIDEO_AS_DOCUMENT = 62;
    private static final int ITEM_UNMUTE_WITH_VOLUME_BUTTONS = 212;
    private static final int ITEM_SWIPE_TO_PIP = 213;
    private static final int ITEM_STICKER_SIZE = 201;
    private static final int ITEM_GIF_SIZE = 202;
    private static final int ITEM_EXTENDED_RECENT_STICKERS = 218;
    private static final int ITEM_UNLIMITED_RECENT_STICKERS = 253;
    private static final int ITEM_DOUBLE_TAP_SEEK_DURATION = 222;
    private static final int ITEM_REMEMBER_LAST_USED_CAMERA = 255;
    private static final int ITEM_PREFER_ORIGINAL_QUALITY = 256;

    // Batch 4: Visual & Proxy features
    private static final int ITEM_STICKER_SHAPE = 260;
    private static final int ITEM_REPLY_BACKGROUND = 261;
    private static final int ITEM_REPLY_COLORS = 262;
    private static final int ITEM_REPLY_EMOJI = 263;
    private static final int ITEM_CUSTOM_SAVE_PATH = 264;
    private static final int ITEM_CHAT_PAGE_UP = 265;
    private static final int ITEM_RESET_ALL = 266;
    private static final int ITEM_HIDE_STORIES_PROFILE = 267;
    private static final int ITEM_HIDE_STORIES_POSTING = 268;
    private static final int ITEM_DELETE_MY_MESSAGES = 269;
    private static final int ITEM_LOCK_ARCHIVE = 290;
    private static final int ITEM_LOCK_SECRET = 291;
    private static final int ITEM_LOCK_SAVED = 292;
    private static final int ITEM_LOCK_DELETE = 293;
    private static final int ITEM_LOCK_GRACE = 294;
    private static final int ITEM_HAPTICS = 306;
    private static final int ITEM_HAPTIC_STRENGTH = 307;
    private static final int ITEM_HAPTIC_ACTION = 310;

    // Item IDs for Developer Mode
    private static final int ITEM_DEV_MODE = 100;
    private static final int ITEM_COPY_CODE_BUTTON = 101;
    private static final int ITEM_PRETTY_JSON_VIEWER = 102;

    private final int category;
    private UniversalRecyclerView listView;
    private AvatarCornersPreviewCell avatarCornersPreviewCell;
    private MessageBubblePreviewCell messageBubblePreviewCell;
    private SwipeActionsPreviewCell swipeActionsPreviewCell;
    private StickerSizePreviewCell stickerSizePreviewCell;
    private int highlightItemId;

    public HundredGramSubSettingsActivity(int category) {
        this(category, 0);
    }

    public HundredGramSubSettingsActivity(int category, int highlightItemId) {
        super(createArgs(category));
        this.category = category;
        this.highlightItemId = highlightItemId;
    }

    private static Bundle createArgs(int category) {
        Bundle args = new Bundle();
        args.putInt("category", category);
        return args;
    }

    public HundredGramSubSettingsActivity(Bundle args) {
        super(args);
        this.category = args != null ? args.getInt("category", 0) : 0;
        this.highlightItemId = args != null ? args.getInt("highlightItemId", 0) : 0;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        super.onTransitionAnimationEnd(isOpen, backward);
        if (isOpen && !backward && highlightItemId != 0 && listView != null && listView.adapter != null) {
            UniversalAdapter adapter = listView.adapter;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                UItem item = adapter.getItem(i);
                if (item != null && item.id == highlightItemId) {
                    final int position = i;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listView != null) {
                            listView.smoothScrollToPosition(position);
                            listView.highlightRow(() -> position);
                        }
                    }, 150);
                    break;
                }
            }
        }
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
        items.add(UItem.asButton(ITEM_RESET_ALL, LocaleController.getString("ResetAllSettings", R.string.ResetAllSettings)).red());
        items.add(UItem.asShadow(LocaleController.getString("ResetAllSettingsInfo", R.string.ResetAllSettingsInfo)));
    }

    private String getSavePresetName(int preset) {
        switch (preset) {
            case HundredGramConfig.PRESET_DMS_ONLY:
                return LocaleController.getString("SavePresetDMsOnly", R.string.SavePresetDMsOnly);
            case HundredGramConfig.PRESET_ALL:
                return LocaleController.getString("SavePresetAll", R.string.SavePresetAll);
            case HundredGramConfig.PRESET_STORAGE_SAVER:
                return LocaleController.getString("SavePresetStorageSaver", R.string.SavePresetStorageSaver);
            case HundredGramConfig.PRESET_CUSTOM:
                return LocaleController.getString("SavePresetCustom", R.string.SavePresetCustom);
            case HundredGramConfig.PRESET_SMART:
            default:
                return LocaleController.getString("SavePresetSmart", R.string.SavePresetSmart);
        }
    }

    private void fillVaultItems(ArrayList<UItem> items) {
        // Section 1: Presets
        items.add(UItem.asHeader(LocaleController.getString("SavePreset", R.string.SavePreset)));
        items.add(UItem.asButton(ITEM_SAVE_PRESET, LocaleController.getString("SavePresetTitle", R.string.SavePresetTitle), getSavePresetName(HundredGramConfig.savePreset)));
        items.add(UItem.asShadow(LocaleController.getString("SavePresetCustomDesc", R.string.SavePresetCustomDesc)));

        // Section 2: Where to Save (Chat Scopes)
        items.add(UItem.asHeader(LocaleController.getString("SaveScopeHeader", R.string.SaveScopeHeader)));
        items.add(UItem.asCheck(ITEM_SAVE_SCOPE_PRIVATE, LocaleController.getString("SaveScopePrivate", R.string.SaveScopePrivate))
                .setChecked(HundredGramConfig.saveScopePrivate));
        items.add(UItem.asCheck(ITEM_SAVE_SCOPE_GROUPS, LocaleController.getString("SaveScopeGroups", R.string.SaveScopeGroups))
                .setChecked(HundredGramConfig.saveScopeGroups));
        items.add(UItem.asCheck(ITEM_SAVE_SCOPE_CHANNELS, LocaleController.getString("SaveScopeChannels", R.string.SaveScopeChannels))
                .setChecked(HundredGramConfig.saveScopeChannels));
        items.add(UItem.asCheck(ITEM_SAVE_SCOPE_BOTS, LocaleController.getString("SaveScopeBots", R.string.SaveScopeBots))
                .setChecked(HundredGramConfig.saveScopeBots));
        items.add(UItem.asCheck(ITEM_SAVE_SCOPE_ARCHIVED, LocaleController.getString("SaveScopeArchived", R.string.SaveScopeArchived))
                .setChecked(HundredGramConfig.saveScopeArchived));
        items.add(UItem.asShadow(LocaleController.getString("SaveScopeHeaderInfo", R.string.SaveScopeHeaderInfo)));

        // Section 3: What to Save (Data Types)
        items.add(UItem.asHeader(LocaleController.getString("SaveTypesHeader", R.string.SaveTypesHeader)));
        items.add(UItem.asCheck(ITEM_SAVE_ANTI_DELETE, LocaleController.getString("SaveAntiDeleteText", R.string.SaveAntiDeleteText))
                .setChecked(HundredGramConfig.saveAntiDeleteText));
        items.add(UItem.asCheck(ITEM_SAVE_EDIT_HISTORY, LocaleController.getString("SaveEditHistory", R.string.SaveEditHistory))
                .setChecked(HundredGramConfig.saveEditHistory));
        items.add(UItem.asCheck(ITEM_SAVE_MEDIA_PHOTOS, LocaleController.getString("SaveMediaPhotos", R.string.SaveMediaPhotos))
                .setChecked(HundredGramConfig.saveMediaPhotos));
        items.add(UItem.asCheck(ITEM_SAVE_MEDIA_VOICE_ROUNDS, LocaleController.getString("SaveMediaVoiceRounds", R.string.SaveMediaVoiceRounds))
                .setChecked(HundredGramConfig.saveMediaVoiceRounds));
        items.add(UItem.asCheck(ITEM_SAVE_MEDIA_VIDEOS, LocaleController.getString("SaveMediaVideos", R.string.SaveMediaVideos))
                .setChecked(HundredGramConfig.saveMediaVideos));
        items.add(UItem.asCheck(ITEM_SAVE_MEDIA_FILES, LocaleController.getString("SaveMediaFiles", R.string.SaveMediaFiles))
                .setChecked(HundredGramConfig.saveMediaFiles));
        items.add(UItem.asShadow(LocaleController.getString("SaveTypesHeaderInfo", R.string.SaveTypesHeaderInfo)));

        // Section 4: Media Vault Limits & Auto-Download
        items.add(UItem.asHeader(LocaleController.getString("HundredGramMediaVault", R.string.HundredGramMediaVault)));
        items.add(UItem.asCheck(ITEM_AUTO_DOWNLOAD_MEDIA_VAULT, LocaleController.getString("HundredGramMediaVault", R.string.HundredGramMediaVault))
                .setChecked(HundredGramConfig.autoDownloadMediaVault));

        if (HundredGramConfig.autoDownloadMediaVault) {
            String sizeStr;
            if (HundredGramConfig.autoDownloadMaxSizeMb <= 0) {
                sizeStr = LocaleController.getString("HundredGramVaultSizeUnlimited", R.string.HundredGramVaultSizeUnlimited);
            } else if (HundredGramConfig.autoDownloadMaxSizeMb >= 1024 && HundredGramConfig.autoDownloadMaxSizeMb % 1024 == 0) {
                sizeStr = (HundredGramConfig.autoDownloadMaxSizeMb / 1024) + " " + LocaleController.getString("HundredGramGB", R.string.HundredGramGB);
            } else {
                sizeStr = HundredGramConfig.autoDownloadMaxSizeMb + " " + LocaleController.getString("HundredGramMB", R.string.HundredGramMB);
            }
            items.add(UItem.asButton(ITEM_AUTO_DOWNLOAD_MAX_SIZE, LocaleController.getString("HundredGramMediaVaultLimit", R.string.HundredGramMediaVaultLimit), sizeStr));
            items.add(UItem.asCheck(ITEM_AUTO_DOWNLOAD_ONLY_WIFI, LocaleController.getString("HundredGramMediaVaultOnlyWifi", R.string.HundredGramMediaVaultOnlyWifi))
                    .setChecked(HundredGramConfig.autoDownloadOnlyWifi));
        }

        ArrayList<Long> exceptions = HundredGramConfig.getChatVaultExceptions();
        int exceptionsCount = exceptions.size();
        String exceptionsValue = exceptionsCount > 0 ? LocaleController.formatPluralString("Chats", exceptionsCount) : LocaleController.getString("HundredGramVaultNoExceptions", R.string.HundredGramVaultNoExceptions);
        items.add(UItem.asButton(ITEM_AUTO_DOWNLOAD_EXCEPTIONS, LocaleController.getString("HundredGramVaultExceptions", R.string.HundredGramVaultExceptions), exceptionsValue));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramMediaVaultInfo", R.string.HundredGramMediaVaultInfo)));

        // Section 5: Background Archiver
        items.add(UItem.asHeader(LocaleController.getString("HundredGramBackgroundArchiver", R.string.HundredGramBackgroundArchiver)));
        items.add(UItem.asCheck(ITEM_BACKGROUND_ARCHIVER, LocaleController.getString("HundredGramBackgroundArchiver", R.string.HundredGramBackgroundArchiver))
                .setChecked(HundredGramConfig.backgroundArchiverEnabled));

        if (HundredGramConfig.backgroundArchiverEnabled) {
            String modeText;
            if (HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_NEW_ONLY) {
                modeText = LocaleController.getString("HundredGramArchiverModeNewOnly", R.string.HundredGramArchiverModeNewOnly);
            } else {
                modeText = LocaleController.getString("HundredGramArchiverModeFullSync", R.string.HundredGramArchiverModeFullSync);
            }
            items.add(UItem.asButton(ITEM_BACKGROUND_ARCHIVER_MODE, LocaleController.getString("HundredGramArchiverMode", R.string.HundredGramArchiverMode), modeText));
        }

        items.add(UItem.asCheck(ITEM_VAULT_ARCHIVE_COMMENTS, LocaleController.getString("HundredGramVaultArchiveComments", R.string.HundredGramVaultArchiveComments))
                .setChecked(HundredGramConfig.vaultArchiveChannelComments));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramBackgroundArchiverInfo", R.string.HundredGramBackgroundArchiverInfo)));

        // Section 6: Limits & Storage Quota
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
        items.add(UItem.asButton(ITEM_VAULT_CLEAN_EXPIRED_NOW, R.drawable.msg_autodelete, LocaleController.getString("VaultCleanExpiredNow", R.string.VaultCleanExpiredNow), ""));
        items.add(UItem.asShadow(LocaleController.getString("VaultMaxSizeInfo", R.string.VaultMaxSizeInfo)));

        // Section 7: Protected Storage Actions
        items.add(UItem.asHeader(LocaleController.getString("HundredGramProtectedVaultTitle", R.string.HundredGramProtectedVaultTitle)));
        long vaultSize = HundredGramVaultManager.getInstance(currentAccount).getVaultTotalSize();
        String sizeFormatted = AndroidUtilities.formatFileSize(vaultSize);
        items.add(UItem.asButton(ITEM_PROTECTED_VAULT_SIZE, R.drawable.msg_folders,
                LocaleController.getString("VaultBrowseFiles", R.string.VaultBrowseFiles), sizeFormatted));
        items.add(UItem.asButton(ITEM_VAULT_EXPORT_ALL_ZIP, R.drawable.msg_share,
                LocaleController.getString("HundredGramVaultExportAllZip", R.string.HundredGramVaultExportAllZip), ""));
        items.add(UItem.asButton(ITEM_VAULT_PRUNE_ORPHANS, R.drawable.msg_retry,
                LocaleController.getString("HundredGramVaultPruneOrphans", R.string.HundredGramVaultPruneOrphans), ""));
        items.add(UItem.asButton(ITEM_CLEAR_PROTECTED_VAULT, R.drawable.msg_clear,
                LocaleController.getString("HundredGramProtectedVaultClear", R.string.HundredGramProtectedVaultClear), ""));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramProtectedVaultInfo", R.string.HundredGramProtectedVaultInfo)));
    }

    private void fillTrackerItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramTrackerHeader", R.string.HundredGramTrackerHeader)));

        items.add(UItem.asCheck(ITEM_TRACK_ONLINE_STATUS, LocaleController.getString("HundredGramTrackOnlineStatus", R.string.HundredGramTrackOnlineStatus))
                .setChecked(HundredGramConfig.trackOnlineStatus));

        int trackedCount = HundredGramConfig.trackedOnlineUsers.size();
        items.add(UItem.asButton(ITEM_TRACKED_USERS_LIST, R.drawable.msg_usersearch,
                LocaleController.getString("HundredGramTrackedUsersList", R.string.HundredGramTrackedUsersList),
                String.format(LocaleController.getString("HundredGramTrackedUsersCount", R.string.HundredGramTrackedUsersCount), trackedCount)));

        items.add(UItem.asCheck(ITEM_NOTIFY_ON_USER_ONLINE, LocaleController.getString("HundredGramNotifyUserOnline", R.string.HundredGramNotifyUserOnline))
                .setChecked(HundredGramConfig.notifyOnUserOnline));

        if (HundredGramConfig.notifyOnUserOnline) {
            items.add(UItem.asCheck(ITEM_NOTIFY_ALL_CONTACTS_ONLINE, LocaleController.getString("HundredGramNotifyAllContactsOnline", R.string.HundredGramNotifyAllContactsOnline))
                    .setChecked(HundredGramConfig.notifyAllContactsOnline));
        }
        items.add(UItem.asShadow(LocaleController.getString("HundredGramTrackOnlineStatusInfo", R.string.HundredGramTrackOnlineStatusInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramTrackSelfActivity", R.string.HundredGramTrackSelfActivity)));
        items.add(UItem.asCheck(ITEM_TRACK_SELF_ACTIVITY, LocaleController.getString("HundredGramTrackSelfActivity", R.string.HundredGramTrackSelfActivity))
                .setChecked(HundredGramConfig.trackSelfActivity));
        items.add(UItem.asButton(ITEM_MY_ACTIVITY_LOG, R.drawable.msg_calendar2,
                LocaleController.getString("HundredGramMyActivityLog", R.string.HundredGramMyActivityLog),
                LocaleController.getString("HundredGramMyActivityLogDesc", R.string.HundredGramMyActivityLogDesc)));
        boolean trackerSimRunning = HundredGramActivitySimulator.getInstance(currentAccount).isRunning();
        items.add(UItem.asButton(ITEM_ACTIVITY_SIMULATOR, R.drawable.msg_usersearch,
                LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle),
                trackerSimRunning
                        ? LocaleController.getString("ActivitySimulatorActiveBadge", R.string.ActivitySimulatorActiveBadge)
                        : (HundredGramConfig.activitySimulatorScheduleEnabled ? LocaleController.getString("ActivitySimulatorScheduleEnabledShort", R.string.ActivitySimulatorScheduleEnabledShort) : "")));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("AvatarHistory", R.string.AvatarHistory)));
        items.add(UItem.asCheck(ITEM_AVATAR_HISTORY_ENABLED, LocaleController.getString("AvatarHistory", R.string.AvatarHistory))
                .setChecked(HundredGramConfig.avatarHistoryEnabled));
        items.add(UItem.asCheck(ITEM_PROFILE_HISTORY_ENABLED, LocaleController.getString("ProfileHistoryToggle", R.string.ProfileHistoryToggle))
                .setChecked(HundredGramConfig.profileHistoryEnabled));
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

        items.add(UItem.asHeader(LocaleController.getString("Shadowban", R.string.Shadowban)));
        int shadowBannedCount = org.telegram.messenger.HundredGramShadowbanController.getShadowBannedCount();
        items.add(UItem.asButton(ITEM_SHADOWBAN, R.drawable.msg_block2,
                LocaleController.getString("Shadowban", R.string.Shadowban),
                LocaleController.formatPluralString("ShadowbanUsersCount", shadowBannedCount, shadowBannedCount)));
        items.add(UItem.asShadow(LocaleController.getString("ShadowbanDesc", R.string.ShadowbanDesc)));

        items.add(UItem.asHeader(LocaleController.getString("ContentFilterTitle", R.string.ContentFilterTitle)));
        int filterRulesCount = HundredGramContentFilterController.getRulesCount();
        items.add(UItem.asButton(ITEM_CONTENT_FILTER, R.drawable.menu_tag_filter,
                LocaleController.getString("ContentFilterTitle", R.string.ContentFilterTitle),
                HundredGramContentFilterController.isFilterEnabled() ? LocaleController.formatPluralString("ContentFilterRulesCount", filterRulesCount, filterRulesCount) : LocaleController.getString("Disabled", R.string.Disabled)));
        items.add(UItem.asShadow(LocaleController.getString("ContentFilterDesc", R.string.ContentFilterDesc)));

        items.add(UItem.asHeader(LocaleController.getString("BlockAds", R.string.BlockAds)));
        items.add(UItem.asCheck(ITEM_ADBLOCK, LocaleController.getString("BlockAds", R.string.BlockAds))
                .setChecked(HundredGramConfig.enableAdBlock));
        items.add(UItem.asShadow(LocaleController.getString("BlockAdsInfo", R.string.BlockAdsInfo)));

        items.add(UItem.asHeader(LocaleController.getString("LockHeader", R.string.LockHeader)));
        items.add(UItem.asCheck(ITEM_LOCK_ARCHIVE, LocaleController.getString("LockArchive", R.string.LockArchive))
                .setChecked(HundredGramConfig.lockArchive));
        items.add(UItem.asCheck(ITEM_LOCK_SECRET, LocaleController.getString("LockSecret", R.string.LockSecret))
                .setChecked(HundredGramConfig.lockSecret));
        items.add(UItem.asCheck(ITEM_LOCK_SAVED, LocaleController.getString("LockSaved", R.string.LockSaved))
                .setChecked(HundredGramConfig.lockSaved));
        items.add(UItem.asCheck(ITEM_LOCK_DELETE, LocaleController.getString("LockDelete", R.string.LockDelete))
                .setChecked(HundredGramConfig.lockDelete));
        items.add(UItem.asButton(ITEM_LOCK_GRACE,
                LocaleController.getString("LockGrace", R.string.LockGrace),
                getLockGraceName()));
        items.add(UItem.asShadow(LocaleController.getString("LockInfo", R.string.LockInfo)));

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
        items.add(UItem.asButton(ITEM_BATTERY_OPTIMIZATIONS, LocaleController.getString("BatteryOptimization", R.string.BatteryOptimization)));
        items.add(UItem.asShadow(LocaleController.getString("KeepAliveForegroundServiceInfo", R.string.KeepAliveForegroundServiceInfo)));
    }

    private void fillGhostItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("GhostOnline", R.string.GhostOnline)));
        items.add(UItem.asCheck(ITEM_GHOST_ONLINE, LocaleController.getString("GhostOnline", R.string.GhostOnline))
                .setChecked(HundredGramConfig.ghostOnline));
        if (HundredGramConfig.ghostOnline) {
            items.add(UItem.asCheck(ITEM_SEND_OFFLINE_AFTER_ONLINE, LocaleController.getString("SendOfflineAfterOnline", R.string.SendOfflineAfterOnline))
                    .setChecked(HundredGramConfig.sendOfflineAfterOnline));
        }
        items.add(UItem.asCheck(ITEM_GHOST_TYPING, LocaleController.getString("GhostTyping", R.string.GhostTyping))
                .setChecked(HundredGramConfig.ghostTyping));
        items.add(UItem.asCheck(ITEM_GHOST_UPLOAD_PROGRESS, LocaleController.getString("GhostUploadProgress", R.string.GhostUploadProgress))
                .setChecked(HundredGramConfig.ghostUploadProgress));
        boolean ghostSimRunning = HundredGramActivitySimulator.getInstance(currentAccount).isRunning();
        items.add(UItem.asButton(ITEM_ACTIVITY_SIMULATOR, R.drawable.msg_usersearch,
                LocaleController.getString("ActivitySimulatorTitle", R.string.ActivitySimulatorTitle),
                ghostSimRunning
                        ? LocaleController.getString("ActivitySimulatorActiveBadge", R.string.ActivitySimulatorActiveBadge)
                        : (HundredGramConfig.activitySimulatorScheduleEnabled ? LocaleController.getString("ActivitySimulatorScheduleEnabledShort", R.string.ActivitySimulatorScheduleEnabledShort) : "")));
        items.add(UItem.asShadow(LocaleController.getString("GhostOnlineInfo", R.string.GhostOnlineInfo)));

        items.add(UItem.asHeader(LocaleController.getString("GhostReadMessages", R.string.GhostReadMessages)));
        items.add(UItem.asCheck(ITEM_GHOST_READ_MESSAGES, LocaleController.getString("GhostReadMessages", R.string.GhostReadMessages))
                .setChecked(HundredGramConfig.ghostReadMessages));
        if (HundredGramConfig.ghostReadMessages) {
            items.add(UItem.asCheck(ITEM_GHOST_READ_ON_REPLY, LocaleController.getString("GhostReadOnReply", R.string.GhostReadOnReply))
                    .setChecked(HundredGramConfig.ghostReadOnReply));
            items.add(UItem.asCheck(ITEM_GHOST_READ_MEDIA_CONTENTS, LocaleController.getString("GhostReadMediaContents", R.string.GhostReadMediaContents))
                    .setChecked(HundredGramConfig.ghostReadMediaContents));
            items.add(UItem.asCheck(ITEM_GHOST_SHOW_MARK_AS_READ, LocaleController.getString("GhostShowMarkAsRead", R.string.GhostShowMarkAsRead))
                    .setChecked(HundredGramConfig.ghostShowMarkAsReadOption));
        }
        items.add(UItem.asCheck(ITEM_TRACK_READ_DATE, LocaleController.getString("TrackReadDate", R.string.TrackReadDate))
                .setChecked(HundredGramConfig.trackReadDate));
        items.add(UItem.asCheck(ITEM_GHOST_HIDE_READ_DATE, LocaleController.getString("GhostHideReadDate", R.string.GhostHideReadDate))
                .setChecked(HundredGramConfig.ghostHideReadDate));
        items.add(UItem.asShadow(LocaleController.getString("GhostReadMessagesInfo", R.string.GhostReadMessagesInfo)));

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
        items.add(UItem.asCheck(ITEM_GHOST_UNLOCK_STORY_SAVING, LocaleController.getString("UnlockStorySaving", R.string.UnlockStorySaving))
                .setChecked(HundredGramConfig.unlockStorySaving));
        items.add(UItem.asShadow(LocaleController.getString("GhostStoriesModeInfo", R.string.GhostStoriesModeInfo)));
    }

    private void fillInterfaceItems(ArrayList<UItem> items) {
        // Section 1: Appearance & Avatars
        items.add(UItem.asHeader(LocaleController.getString("HundredGramAppearanceSection", R.string.HundredGramAppearanceSection)));
        if (avatarCornersPreviewCell == null) {
            avatarCornersPreviewCell = new AvatarCornersPreviewCell(getParentActivity(), this);
        } else {
            avatarCornersPreviewCell.updateProgress();
        }
        items.add(UItem.asCustom(ITEM_AVATAR_CORNERS_PREVIEW, avatarCornersPreviewCell));
        items.add(UItem.asCheck(ITEM_MD3_SWITCH_STYLE, LocaleController.getString("Md3SwitchStyle", R.string.Md3SwitchStyle))
                .setChecked(HundredGramConfig.md3SwitchStyle));
        items.add(UItem.asCheck(ITEM_NEW_SLIDER_STYLE, LocaleController.getString("NewSliderStyle", R.string.NewSliderStyle))
                .setChecked(HundredGramConfig.newSliderStyle));
        items.add(UItem.asCheck(ITEM_SQUARE_FAB, LocaleController.getString("SquareFab", R.string.SquareFab))
                .setChecked(HundredGramConfig.squareFab));
        items.add(UItem.asShadow(LocaleController.getString("AvatarCornersInfo", R.string.AvatarCornersInfo)));

        // Section 2: Transitions & Dividers
        items.add(UItem.asHeader(LocaleController.getString("TransitionsAndDividersSection", R.string.TransitionsAndDividersSection)));
        String transitionName;
        switch (HundredGramConfig.transitionAnimation) {
            case HundredGramConfig.TRANSITION_SPRING:
                transitionName = LocaleController.getString("TransitionSpring", R.string.TransitionSpring);
                break;
            case HundredGramConfig.TRANSITION_AOSP:
                transitionName = LocaleController.getString("TransitionAosp", R.string.TransitionAosp);
                break;
            case HundredGramConfig.TRANSITION_DEFAULT:
            default:
                transitionName = LocaleController.getString("TransitionDefault", R.string.TransitionDefault);
                break;
        }
        items.add(UItem.asButton(ITEM_TRANSITION_ANIMATION, LocaleController.getString("TransitionAnimation", R.string.TransitionAnimation), transitionName));

        String dividerName;
        switch (HundredGramConfig.dividerStyle) {
            case HundredGramConfig.DIVIDER_SEGMENTS:
                dividerName = LocaleController.getString("DividerStyleSegments", R.string.DividerStyleSegments);
                break;
            case HundredGramConfig.DIVIDER_HIDDEN:
                dividerName = LocaleController.getString("DividerStyleHidden", R.string.DividerStyleHidden);
                break;
            case HundredGramConfig.DIVIDER_LINE:
            default:
                dividerName = LocaleController.getString("DividerStyleLine", R.string.DividerStyleLine);
                break;
        }
        items.add(UItem.asButton(ITEM_DIVIDER_STYLE, LocaleController.getString("DividerStyle", R.string.DividerStyle), dividerName));
        items.add(UItem.asCheck(ITEM_FOLDER_SWIPE, LocaleController.getString("FolderSwipe", R.string.FolderSwipe))
                .setChecked(HundredGramConfig.folderSwipe));
        if (HundredGramConfig.folderSwipe) {
            items.add(UItem.asButton(ITEM_FOLDER_SWIPE_SHRINK, LocaleController.getString("FolderSwipeShrink", R.string.FolderSwipeShrink), HundredGramConfig.folderSwipeShrink + "%"));
            items.add(UItem.asButton(ITEM_FOLDER_SWIPE_SPRING, LocaleController.getString("FolderSwipeSpring", R.string.FolderSwipeSpring), getFolderSwipeSpringName()));
        }
        items.add(UItem.asShadow(LocaleController.getString("FolderSwipeInfo", R.string.FolderSwipeInfo)));
        items.add(UItem.asCheck(ITEM_HIDE_ALL_CHATS_FOLDER, LocaleController.getString("HideAllChatsFolder", R.string.HideAllChatsFolder))
                .setChecked(HundredGramConfig.hideAllChatsFolder));
        items.add(UItem.asCheck(ITEM_PINNED_DOWNLOADS, LocaleController.getString("PinnedDownloads", R.string.PinnedDownloads))
                .setChecked(HundredGramConfig.pinnedDownloads));
        items.add(UItem.asShadow(LocaleController.getString("PinnedDownloadsInfo", R.string.PinnedDownloadsInfo)));
        items.add(UItem.asShadow(LocaleController.getString("TransitionsAndDividersInfo", R.string.TransitionsAndDividersInfo)));

        // Section 3: Message Bubbles (LIVE PREVIEW DIRECTLY WITH ITS 5 CONTROLS!)
        items.add(UItem.asHeader(LocaleController.getString("HundredGramMessageBubblesSection", R.string.HundredGramMessageBubblesSection)));
        if (messageBubblePreviewCell == null) {
            messageBubblePreviewCell = new MessageBubblePreviewCell(getParentActivity());
        } else {
            messageBubblePreviewCell.update();
        }
        items.add(UItem.asCustom(ITEM_MESSAGE_BUBBLE_PREVIEW, messageBubblePreviewCell));
        items.add(UItem.asCheck(ITEM_REMOVE_MESSAGE_TAIL, LocaleController.getString("RemoveMessageTail", R.string.RemoveMessageTail))
                .setChecked(HundredGramConfig.removeMessageTail));
        items.add(UItem.asCheck(ITEM_REPLACE_EDITED_WITH_ICON, LocaleController.getString("ReplaceEditedWithIcon", R.string.ReplaceEditedWithIcon))
                .setChecked(HundredGramConfig.replaceEditedWithIcon));
        items.add(UItem.asCheck(ITEM_SHOW_SECONDS_IN_TIME, LocaleController.getString("ShowSecondsInTime", R.string.ShowSecondsInTime))
                .setChecked(HundredGramConfig.showSecondsInTime));
        items.add(UItem.asCheck(ITEM_SHOW_WEEKDAY_IN_DATE, LocaleController.getString("ShowWeekdayInDate", R.string.ShowWeekdayInDate))
                .setChecked(HundredGramConfig.showWeekdayInDate));
        items.add(UItem.asCheck(ITEM_FORWARD_TIME, LocaleController.getString("ForwardTime", R.string.ForwardTime))
                .setChecked(HundredGramConfig.forwardTime));
        items.add(UItem.asCheck(ITEM_SHOW_FORWARDS_COUNT, LocaleController.getString("ShowForwardsCount", R.string.ShowForwardsCount))
                .setChecked(HundredGramConfig.showForwardsCount));
        items.add(UItem.asCheck(ITEM_WIDE_POSTS_IN_CHANNELS, LocaleController.getString("WidePostsInChannels", R.string.WidePostsInChannels))
                .setChecked(HundredGramConfig.widePostsInChannels));
        items.add(UItem.asCheck(ITEM_HIDE_SHARE_BUTTON, LocaleController.getString("HideShareButton", R.string.HideShareButton))
                .setChecked(HundredGramConfig.hideShareButton));
        items.add(UItem.asCheck(ITEM_HIDE_REACTIONS_IN_CHANNELS, LocaleController.getString("HideReactionsInChannels", R.string.HideReactionsInChannels))
                .setChecked(HundredGramConfig.hideReactionsInChannels));
        items.add(UItem.asCheck(ITEM_HIDE_REACTIONS_IN_GROUPS, LocaleController.getString("HideReactionsInGroups", R.string.HideReactionsInGroups))
                .setChecked(HundredGramConfig.hideReactionsInGroups));
        items.add(UItem.asShadow(LocaleController.getString("MessageBubblePreviewInfo", R.string.MessageBubblePreviewInfo)));

        items.add(UItem.asHeader(LocaleController.getString("SelectionHeader", R.string.SelectionHeader)));
        items.add(UItem.asButton(ITEM_SELECTION_SPEED, LocaleController.getString("SelectionSpeed", R.string.SelectionSpeed), getSelectionSpeedName()));
        items.add(UItem.asCheck(ITEM_SELECTION_NO_LIMIT, LocaleController.getString("SelectionNoLimit", R.string.SelectionNoLimit))
                .setChecked(HundredGramConfig.selectionNoLimit));
        items.add(UItem.asShadow(LocaleController.getString("SelectionNoLimitInfo", R.string.SelectionNoLimitInfo)));

        // Reply Styling
        items.add(UItem.asHeader(LocaleController.getString("ReplyBackground", R.string.ReplyBackground)));
        items.add(UItem.asCheck(ITEM_REPLY_BACKGROUND, LocaleController.getString("ReplyBackground", R.string.ReplyBackground))
                .setChecked(HundredGramConfig.replyBackground));
        items.add(UItem.asCheck(ITEM_REPLY_COLORS, LocaleController.getString("ReplyColors", R.string.ReplyColors))
                .setChecked(HundredGramConfig.replyColors));
        items.add(UItem.asCheck(ITEM_REPLY_EMOJI, LocaleController.getString("ReplyEmoji", R.string.ReplyEmoji))
                .setChecked(HundredGramConfig.replyEmoji));
        items.add(UItem.asShadow(LocaleController.getString("ReplyBackgroundInfo", R.string.ReplyBackgroundInfo)));

        // Section 3.1: Stickers and GIFs Size (Live Chat Window Preview)
        items.add(UItem.asHeader(LocaleController.getString("StickersAndGifsHeader", R.string.StickersAndGifsHeader)));
        if (stickerSizePreviewCell == null) {
            stickerSizePreviewCell = new StickerSizePreviewCell(getParentActivity());
        } else {
            stickerSizePreviewCell.update();
        }
        items.add(UItem.asCustom(ITEM_STICKER_SIZE_PREVIEW, stickerSizePreviewCell));

        items.add(UItem.asButton(ITEM_STICKER_TIME_MODE, LocaleController.getString("StickerTimeMode", R.string.StickerTimeMode), getStickerTimeModeName(HundredGramConfig.stickerTimeMode)));
        items.add(UItem.asCheck(ITEM_EXTENDED_RECENT_STICKERS, LocaleController.getString("ExtendedRecentStickers", R.string.ExtendedRecentStickers))
                .setChecked(HundredGramConfig.extendedRecentStickers));
        items.add(UItem.asCheck(ITEM_UNLIMITED_RECENT_STICKERS, LocaleController.getString("UnlimitedRecentStickers", R.string.UnlimitedRecentStickers))
                .setChecked(HundredGramConfig.unlimitedRecentStickers));
        String stickerShapeValue;
        switch (HundredGramConfig.stickerShape) {
            case HundredGramConfig.STICKER_SHAPE_ROUNDED:
                stickerShapeValue = LocaleController.getString("StickerShapeRounded", R.string.StickerShapeRounded);
                break;
            case HundredGramConfig.STICKER_SHAPE_BUBBLE:
                stickerShapeValue = LocaleController.getString("StickerShapeBubble", R.string.StickerShapeBubble);
                break;
            default:
                stickerShapeValue = LocaleController.getString("StickerShapeDefault", R.string.StickerShapeDefault);
                break;
        }
        items.add(UItem.asButton(ITEM_STICKER_SHAPE, LocaleController.getString("StickerShape", R.string.StickerShape), stickerShapeValue));
        items.add(UItem.asShadow(LocaleController.getString("UnlimitedRecentStickersInfo", R.string.UnlimitedRecentStickersInfo)));

        // Section 4: Chat Behavior & Features
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatBehaviorSection", R.string.HundredGramChatBehaviorSection)));
        items.add(UItem.asCheck(ITEM_UNLOCK_CHAT_TRANSLATION, LocaleController.getString("UnlockChatTranslation", R.string.UnlockChatTranslation))
                .setChecked(HundredGramConfig.unlockChatTranslation));
        items.add(UItem.asCheck(ITEM_RELATIVE_LAST_SEEN, LocaleController.getString("HundredGramRelativeLastSeen", R.string.HundredGramRelativeLastSeen))
                .setChecked(HundredGramConfig.relativeLastSeen));
        items.add(UItem.asCheck(ITEM_DISABLE_JUMP_TO_NEXT_CHANNEL, LocaleController.getString("HundredGramDisableJumpChannel", R.string.HundredGramDisableJumpChannel))
                .setChecked(HundredGramConfig.disableJumpToNextChannel));
        items.add(UItem.asCheck(ITEM_QUICK_CHANNEL_TRANSITION, LocaleController.getString("QuickChannelTransition", R.string.QuickChannelTransition))
                .setChecked(HundredGramConfig.quickChannelTransition));
        items.add(UItem.asCheck(ITEM_QUICK_TOPIC_TRANSITION, LocaleController.getString("QuickTopicTransition", R.string.QuickTopicTransition))
                .setChecked(HundredGramConfig.quickTopicTransition));
        items.add(UItem.asCheck(ITEM_HIDE_KEYBOARD_ON_SCROLL, LocaleController.getString("HundredGramHideKeyboardOnScroll", R.string.HundredGramHideKeyboardOnScroll))
                .setChecked(HundredGramConfig.hideKeyboardOnScroll));
        items.add(UItem.asCheck(ITEM_CHAT_PAGE_UP, LocaleController.getString("ChatPageUpButton", R.string.ChatPageUpButton))
                .setChecked(HundredGramConfig.chatPageUpButton));
        items.add(UItem.asShadow(LocaleController.getString("ChatPageUpInfo", R.string.ChatPageUpInfo)));
        items.add(UItem.asCheck(ITEM_ADD_COMMA_AFTER_MENTION, LocaleController.getString("HundredGramAddCommaAfterMention", R.string.HundredGramAddCommaAfterMention))
                .setChecked(HundredGramConfig.addCommaAfterMention));
        items.add(UItem.asCheck(ITEM_INLINE_MATH, LocaleController.getString("InlineMathResult", R.string.InlineMathResult))
                .setChecked(HundredGramConfig.inlineMath));
        items.add(UItem.asCheck(ITEM_DISABLE_GREETING_STICKER, LocaleController.getString("DisableGreetingSticker", R.string.DisableGreetingSticker))
                .setChecked(HundredGramConfig.disableGreetingSticker));
        items.add(UItem.asCheck(ITEM_DISABLE_NUMBER_ROUNDING, LocaleController.getString("HundredGramDisableNumberRounding", R.string.HundredGramDisableNumberRounding))
                .setChecked(HundredGramConfig.disableNumberRounding));
        items.add(UItem.asCheck(ITEM_HIGHLIGHT_DELETED, LocaleController.getString("HundredGramHighlightDeleted", R.string.HundredGramHighlightDeleted))
                .setChecked(HundredGramConfig.highlightDeletedMessages));
        if (HundredGramConfig.highlightDeletedMessages) {
            items.add(UItem.asButton(ITEM_DELETED_MARK_ICON, LocaleController.getString("DeletedMarkIcon", R.string.DeletedMarkIcon), getDeletedMarkIconTitle(HundredGramConfig.deletedMarkIcon)));
            items.add(UItem.asButton(ITEM_DELETED_MARK_COLOR, LocaleController.getString("DeletedMarkColor", R.string.DeletedMarkColor), getDeletedMarkColorTitle(HundredGramConfig.deletedMarkColor)));
            items.add(UItem.asCheck(ITEM_THANOS_DELETE_EFFECT, LocaleController.getString("ThanosDeleteEffect", R.string.ThanosDeleteEffect))
                    .setChecked(HundredGramConfig.thanosDeleteEffect));
        }
        items.add(UItem.asCheck(ITEM_HIDE_SIMILAR_CHANNELS, LocaleController.getString("HideSimilarChannels", R.string.HideSimilarChannels))
                .setChecked(HundredGramConfig.hideSimilarChannels));
        items.add(UItem.asCheck(ITEM_FILTER_ZALGO, LocaleController.getString("FilterZalgo", R.string.FilterZalgo))
                .setChecked(HundredGramConfig.filterZalgo));
        items.add(UItem.asButton(ITEM_CONTENT_FILTER, R.drawable.menu_tag_filter,
                LocaleController.getString("ContentFilterTitle", R.string.ContentFilterTitle),
                HundredGramContentFilterController.isFilterEnabled() ? LocaleController.getString("Enabled", R.string.Enabled) : LocaleController.getString("Disabled", R.string.Disabled)));
        items.add(UItem.asCheck(ITEM_HIDE_ACTION_BAR_STATUS, LocaleController.getString("HideActionBarStatus", R.string.HideActionBarStatus))
                .setChecked(HundredGramConfig.hideActionBarStatus));
        items.add(UItem.asCheck(ITEM_HIDE_SEND_AS_PEER, LocaleController.getString("HideSendAsPeer", R.string.HideSendAsPeer))
                .setChecked(HundredGramConfig.hideSendAsPeer));
        items.add(UItem.asCheck(ITEM_DISABLE_NOTIFICATION_DELAY, LocaleController.getString("DisableNotificationDelay", R.string.DisableNotificationDelay))
                .setChecked(HundredGramConfig.disableNotificationDelay));
        items.add(UItem.asButton(ITEM_CHANNEL_BOTTOM_BUTTON, LocaleController.getString("ChannelBottomButton", R.string.ChannelBottomButton), getChannelBottomButtonName(HundredGramConfig.channelBottomButton)));
        items.add(UItem.asShadow(null));

        // Section 5: Main Screen & Navigation
        items.add(UItem.asHeader(LocaleController.getString("HundredGramMainScreenSection", R.string.HundredGramMainScreenSection)));
        items.add(UItem.asCheck(ITEM_SENDER_MINI_AVATARS, LocaleController.getString("SenderMiniAvatars", R.string.SenderMiniAvatars))
                .setChecked(HundredGramConfig.senderMiniAvatars));
        items.add(UItem.asButton(ITEM_ACTION_BAR_TITLE_MODE,
                LocaleController.getString("ActionBarTitleMode", R.string.ActionBarTitleMode),
                getActionBarTitleModeName(HundredGramConfig.actionBarTitleMode)));
        items.add(UItem.asCheck(ITEM_CENTER_ACTION_BAR_TITLE, LocaleController.getString("CenterActionBarTitle", R.string.CenterActionBarTitle))
                .setChecked(HundredGramConfig.centerActionBarTitle));
        items.add(UItem.asButton(ITEM_TAB_TITLE_STYLE,
                LocaleController.getString("TabTitleStyle", R.string.TabTitleStyle),
                getTabTitleStyleName(HundredGramConfig.tabTitleStyle)));
        items.add(UItem.asButton(ITEM_TAB_COUNTER_MODE,
                LocaleController.getString("TabCounterMode", R.string.TabCounterMode),
                getTabCounterModeName(HundredGramConfig.tabCounterMode)));
        items.add(UItem.asCheck(ITEM_HIDE_STORIES_BAR, LocaleController.getString("HideStoriesBar", R.string.HideStoriesBar))
                .setChecked(HundredGramConfig.hideStoriesBar));
        items.add(UItem.asCheck(ITEM_HIDE_STORIES_PROFILE, LocaleController.getString("HideStoriesProfile", R.string.HideStoriesProfile))
                .setChecked(HundredGramConfig.hideStoriesProfile));
        items.add(UItem.asCheck(ITEM_HIDE_STORIES_POSTING, LocaleController.getString("HideStoriesPosting", R.string.HideStoriesPosting))
                .setChecked(HundredGramConfig.hideStoriesPosting));
        items.add(UItem.asCheck(ITEM_HIDE_ARCHIVE_FOLDER, LocaleController.getString("HundredGramHideArchiveFolder", R.string.HundredGramHideArchiveFolder))
                .setChecked(HundredGramConfig.hideArchiveFolder));
        items.add(UItem.asCheck(ITEM_ARCHIVE_ON_PULL, LocaleController.getString("ArchiveOnPull", R.string.ArchiveOnPull))
                .setChecked(HundredGramConfig.archiveOnPull));
        items.add(UItem.asCheck(ITEM_HIDE_FLOATING_BUTTON, LocaleController.getString("HundredGramHideFloatingButton", R.string.HundredGramHideFloatingButton))
                .setChecked(HundredGramConfig.hideFloatingButton));
        items.add(UItem.asCheck(ITEM_HIDE_DIALOGS_SEARCH_BAR, LocaleController.getString("HideDialogsSearchBar", R.string.HideDialogsSearchBar))
                .setChecked(HundredGramConfig.hideDialogsSearchBar));
        items.add(UItem.asCheck(ITEM_PILL_STACK_ENABLED, LocaleController.getString("HundredGramPillStack", R.string.HundredGramPillStack))
                .setChecked(HundredGramConfig.pillStackEnabled));
        if (HundredGramConfig.pillStackEnabled) {
            items.add(UItem.asButton(ITEM_PILL_STACK_SETTINGS, R.drawable.msg_settings,
                    LocaleController.getString("PillStackPills", R.string.PillStackPills),
                    null));
            items.add(UItem.asShadow(LocaleController.getString("PillStackPillsSettingsInfo", R.string.PillStackPillsSettingsInfo)));
        } else {
            items.add(UItem.asShadow(null));
        }

        // Section 5.1: Bottom Navigation Bar
        items.add(UItem.asHeader(LocaleController.getString("BottomNavHeader", R.string.BottomNavHeader)));
        items.add(UItem.asCheck(ITEM_BOTTOM_NAV_SHOW_ARCHIVE, LocaleController.getString("BottomNavShowArchive", R.string.BottomNavShowArchive))
                .setChecked(HundredGramConfig.bottomNavArchiveTab));
        items.add(UItem.asCheck(ITEM_BOTTOM_NAV_SHOW_FEED, LocaleController.getString("BottomNavShowFeed", R.string.BottomNavShowFeed))
                .setChecked(HundredGramConfig.enableFeedTab));
        items.add(UItem.asCheck(ITEM_BOTTOM_NAV_SHOW_MUSIC, LocaleController.getString("BottomNavShowMusic", R.string.BottomNavShowMusic))
                .setChecked(HundredGramConfig.bottomNavMusicTab));
        items.add(UItem.asCheck(ITEM_BOTTOM_NAV_HIDE_CONTACTS, LocaleController.getString("BottomNavHideContacts", R.string.BottomNavHideContacts))
                .setChecked(HundredGramConfig.bottomNavHideContactsTab));
        items.add(UItem.asCheck(ITEM_BOTTOM_NAV_HIDE_SETTINGS, LocaleController.getString("BottomNavHideSettings", R.string.BottomNavHideSettings))
                .setChecked(HundredGramConfig.bottomNavHideSettingsTab));
        items.add(UItem.asShadow(LocaleController.getString("BottomNavSectionInfo", R.string.BottomNavSectionInfo)));

        // Section 6: Calls & Links
        items.add(UItem.asHeader(LocaleController.getString("HundredGramCallsAndLinksSection", R.string.HundredGramCallsAndLinksSection)));
        items.add(UItem.asCheck(ITEM_CONFIRM_CALLS, LocaleController.getString("ConfirmCalls", R.string.ConfirmCalls))
                .setChecked(HundredGramConfig.confirmCalls));
        items.add(UItem.asCheck(ITEM_CLEAN_URLS, LocaleController.getString("CleanUrls", R.string.CleanUrls))
                .setChecked(HundredGramConfig.cleanUrls));
        items.add(UItem.asShadow(LocaleController.getString("ConfirmCallsInfo", R.string.ConfirmCallsInfo)));
    }

    private void fillActionsItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("SwipeActionHeader", R.string.SwipeActionHeader)));
        items.add(UItem.asButton(ITEM_CHAT_MESSAGE_SWIPE_ACTION, LocaleController.getString("SwipeActions", R.string.SwipeActions), LocaleController.getString("SwipeActionsInfo", R.string.SwipeActionsInfo)));
        items.add(UItem.asCheck(ITEM_DISABLE_UNARCHIVE_SWIPE, LocaleController.getString("DisableUnarchiveSwipe", R.string.DisableUnarchiveSwipe))
                .setChecked(HundredGramConfig.disableUnarchiveSwipe));
        items.add(UItem.asShadow(LocaleController.getString("ChatMessageSwipeActionDesc", R.string.ChatMessageSwipeActionDesc)));

        items.add(UItem.asHeader(LocaleController.getString("DoubleTapHeader", R.string.DoubleTapHeader)));
        items.add(UItem.asButton(ITEM_DOUBLE_TAP_INCOMING, LocaleController.getString("DoubleTapActionIncoming", R.string.DoubleTapActionIncoming), getDoubleTapActionName(HundredGramConfig.doubleTapAction)));
        items.add(UItem.asButton(ITEM_DOUBLE_TAP_OUTGOING, LocaleController.getString("DoubleTapActionOutgoing", R.string.DoubleTapActionOutgoing), getDoubleTapActionName(HundredGramConfig.doubleTapActionOutOwner)));
        items.add(UItem.asShadow(LocaleController.getString("DoubleTapActionIncomingDesc", R.string.DoubleTapActionIncomingDesc)));

        items.add(UItem.asHeader(LocaleController.getString("MessageMenuSection", R.string.MessageMenuSection)));
        items.add(UItem.asButton(ITEM_MESSAGE_MENU_ORDER, R.drawable.msg_list, LocaleController.getString("MessageMenuCustomize", R.string.MessageMenuCustomize)));
        items.add(UItem.asCheck(ITEM_SHOW_SAVE_MESSAGE, LocaleController.getString("ShowSaveMessageButton", R.string.ShowSaveMessageButton))
                .setChecked(HundredGramConfig.showSaveMessageButton));
        items.add(UItem.asCheck(ITEM_SHOW_DETAILS, LocaleController.getString("ShowDetailsButton", R.string.ShowDetailsButton))
                .setChecked(HundredGramConfig.showDetailsButton));
        items.add(UItem.asCheck(ITEM_SHOW_COPY_PHOTO, LocaleController.getString("ShowCopyPhotoButton", R.string.ShowCopyPhotoButton))
                .setChecked(HundredGramConfig.showCopyPhotoButton));
        items.add(UItem.asCheck(ITEM_SHOW_EDIT_HISTORY, LocaleController.getString("ShowEditHistoryButton", R.string.ShowEditHistoryButton))
                .setChecked(HundredGramConfig.showEditHistoryButton));
        items.add(UItem.asCheck(ITEM_SHOW_FORWARD_WITHOUT_AUTHOR, LocaleController.getString("ShowForwardWithoutAuthorButton", R.string.ShowForwardWithoutAuthorButton))
                .setChecked(HundredGramConfig.showForwardWithoutAuthorButton));
        items.add(UItem.asCheck(ITEM_SHOW_REPEAT_MESSAGE, LocaleController.getString("ShowRepeatMessageButton", R.string.ShowRepeatMessageButton))
                .setChecked(HundredGramConfig.showRepeatMessageButton));
        items.add(UItem.asShadow(LocaleController.getString("ShowSaveMessageButtonInfo", R.string.ShowSaveMessageButtonInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramSendAndForwardSection", R.string.HundredGramSendAndForwardSection)));
        items.add(UItem.asCheck(ITEM_SHOW_CLEAR_INPUT, LocaleController.getString("ShowClearInputButton", R.string.ShowClearInputButton))
                .setChecked(HundredGramConfig.showClearInputButton));
        items.add(UItem.asCheck(ITEM_SHOW_RESULTS_BEFORE_VOTING, LocaleController.getString("ShowResultsBeforeVoting", R.string.ShowResultsBeforeVoting))
                .setChecked(HundredGramConfig.showResultsBeforeVoting));
        items.add(UItem.asCheck(ITEM_SILENT_SEND_BY_DEFAULT, LocaleController.getString(R.string.SilentSendByDefault))
                .setChecked(HundredGramConfig.silentSendByDefault));
        items.add(UItem.asCheck(ITEM_ALWAYS_FORWARD_WITHOUT_AUTHOR, LocaleController.getString(R.string.AlwaysForwardWithoutAuthor))
                .setChecked(HundredGramConfig.alwaysForwardWithoutAuthor));
        items.add(UItem.asCheck(ITEM_CONFIRM_SEND_STICKERS, LocaleController.getString(R.string.ConfirmSendStickers))
                .setChecked(HundredGramConfig.confirmSendStickers));
        items.add(UItem.asCheck(ITEM_CONFIRM_SEND_GIFS, LocaleController.getString(R.string.ConfirmSendGifs))
                .setChecked(HundredGramConfig.confirmSendGifs));
        items.add(UItem.asCheck(ITEM_AUTO_CHECK_DELETE_FOR_ALL, LocaleController.getString("AutoCheckDeleteForAll", R.string.AutoCheckDeleteForAll))
                .setChecked(HundredGramConfig.autoCheckDeleteForAll));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString("LocalSttHeader", R.string.LocalSttHeader)));
        items.add(UItem.asCheck(ITEM_LOCAL_STT_ENABLED, LocaleController.getString("LocalSttEnabled", R.string.LocalSttEnabled))
                .setChecked(HundredGramConfig.localSttEnabled));
        boolean modelDownloaded = org.telegram.messenger.HundredGramSttManager.isModelDownloaded();
        String modelStatus = modelDownloaded
                ? LocaleController.getString("LocalSttModelReadyStatus", R.string.LocalSttModelReadyStatus)
                : LocaleController.getString("LocalSttModelNotDownloaded", R.string.LocalSttModelNotDownloaded);
        items.add(UItem.asButton(ITEM_LOCAL_STT_MODEL,
                LocaleController.getString("LocalSttModelTitle", R.string.LocalSttModelTitle),
                modelStatus));
        items.add(UItem.asShadow(LocaleController.getString("LocalSttEnabledInfo", R.string.LocalSttEnabledInfo)));

        items.add(UItem.asCheck(ITEM_SHOW_REAL_NAME, LocaleController.getString("HundredGramShowRealName", R.string.HundredGramShowRealName))
                .setChecked(HundredGramConfig.showRealName));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramShowRealNameInfo", R.string.HundredGramShowRealNameInfo)));

        items.add(UItem.asButton(ITEM_DELETE_MY_MESSAGES, R.drawable.msg_chats_remove, LocaleController.getString("DeleteMyMessages", R.string.DeleteMyMessages)).red());
        items.add(UItem.asShadow(LocaleController.getString("DeleteMyMessagesInfo", R.string.DeleteMyMessagesInfo)));

        items.add(UItem.asCheck(ITEM_HAPTICS, LocaleController.getString("Haptics", R.string.Haptics))
                .setChecked(HundredGramConfig.hapticsEnabled));
        if (HundredGramConfig.hapticsEnabled) {
            items.add(UItem.asButton(ITEM_HAPTIC_STRENGTH,
                    LocaleController.getString("HapticStrength", R.string.HapticStrength),
                    HundredGramConfig.hapticStrength + "%"));
            for (int i = 0; i < HundredGramHaptics.ACTION_NAMES.length; i++) {
                String action = HundredGramHaptics.ACTION_NAMES[i];
                items.add(UItem.asButton(ITEM_HAPTIC_ACTION + i,
                        getHapticActionName(action),
                        HundredGramConfig.isHapticActionEnabled(action)
                                ? getHapticEffectName(HundredGramConfig.getHapticEffect(action))
                                : LocaleController.getString("Disabled", R.string.Disabled)));
            }
        }
        items.add(UItem.asShadow(LocaleController.getString("HapticsInfo", R.string.HapticsInfo)));

        items.add(UItem.asHeader(LocaleController.getString("DevCategory", R.string.DevCategory)));
        items.add(UItem.asCheck(ITEM_DEV_MODE, LocaleController.getString("DeveloperMode", R.string.DeveloperMode))
                .setChecked(HundredGramConfig.developerModeEnabled));
        if (HundredGramConfig.developerModeEnabled) {
            items.add(UItem.asCheck(ITEM_COPY_CODE_BUTTON, LocaleController.getString("DevCopyCodeButton", R.string.DevCopyCodeButton))
                    .setChecked(HundredGramConfig.copyCodeButtonEnabled));
            items.add(UItem.asCheck(ITEM_PRETTY_JSON_VIEWER, LocaleController.getString("PrettyJson", R.string.PrettyJson))
                    .setChecked(HundredGramConfig.prettyJsonViewerEnabled));
        }
        items.add(UItem.asShadow(LocaleController.getString("DeveloperModeInfo", R.string.DeveloperModeInfo)));
    }

    private void fillMediaItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("HundredGramPhotosSection", R.string.HundredGramPhotosSection)));
        items.add(UItem.asCheck(ITEM_ALWAYS_HD_PHOTOS, LocaleController.getString("AlwaysHighQualityPhotos", R.string.AlwaysHighQualityPhotos))
                .setChecked(HundredGramConfig.alwaysHighQualityPhotos));

        String resValue;
        if (HundredGramConfig.photoMaxResolution <= 1280) {
            resValue = LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280);
        } else if (HundredGramConfig.photoMaxResolution <= 2560) {
            resValue = LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560);
        } else {
            resValue = LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840);
        }
        items.add(UItem.asButton(ITEM_PHOTO_MAX_RESOLUTION, LocaleController.getString("PhotoMaxResolution", R.string.PhotoMaxResolution), resValue));
        items.add(UItem.asButton(ITEM_MEDIA_GLOW, LocaleController.getString("MediaGlow", R.string.MediaGlow), getMediaGlowName()));
        items.add(UItem.asShadow(LocaleController.getString("AlwaysHighQualityPhotosInfo", R.string.AlwaysHighQualityPhotosInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramVideosSection", R.string.HundredGramVideosSection)));
        items.add(UItem.asCheck(ITEM_SEND_VIDEO_AS_DOCUMENT, LocaleController.getString("SendVideoAsDocument", R.string.SendVideoAsDocument))
                .setChecked(HundredGramConfig.sendVideoAsDocument));
        items.add(UItem.asCheck(ITEM_UNMUTE_WITH_VOLUME_BUTTONS, LocaleController.getString("UnmuteWithVolumeButtons", R.string.UnmuteWithVolumeButtons))
                .setChecked(HundredGramConfig.unmuteWithVolumeButtons));
        items.add(UItem.asCheck(ITEM_SWIPE_TO_PIP, LocaleController.getString("SwipeToPip", R.string.SwipeToPip))
                .setChecked(HundredGramConfig.swipeToPip));
        items.add(UItem.asCheck(ITEM_PREFER_ORIGINAL_QUALITY, LocaleController.getString("PreferOriginalQuality", R.string.PreferOriginalQuality))
                .setChecked(HundredGramConfig.preferOriginalQuality));
        items.add(UItem.asCheck(ITEM_REMEMBER_LAST_USED_CAMERA, LocaleController.getString("RememberLastUsedCamera", R.string.RememberLastUsedCamera))
                .setChecked(HundredGramConfig.rememberLastUsedCamera));
        items.add(UItem.asButton(ITEM_DOUBLE_TAP_SEEK_DURATION,
                LocaleController.getString("DoubleTapSeekDuration", R.string.DoubleTapSeekDuration),
                String.format(LocaleController.getString("DoubleTapSeekSeconds", R.string.DoubleTapSeekSeconds), HundredGramConfig.doubleTapSeekDuration)));
        items.add(UItem.asShadow(LocaleController.getString("DoubleTapSeekDurationInfo", R.string.DoubleTapSeekDurationInfo)));

        items.add(UItem.asHeader(LocaleController.getString("PauseOnMinimize", R.string.PauseOnMinimize)));
        items.add(UItem.asCheck(ITEM_PAUSE_ON_MINIMIZE_VIDEO, LocaleController.getString("PauseOnMinimizeVideo", R.string.PauseOnMinimizeVideo))
                .setChecked(HundredGramConfig.pauseOnMinimizeVideo));
        items.add(UItem.asCheck(ITEM_PAUSE_ON_MINIMIZE_VOICE, LocaleController.getString("PauseOnMinimizeVoice", R.string.PauseOnMinimizeVoice))
                .setChecked(HundredGramConfig.pauseOnMinimizeVoice));
        items.add(UItem.asCheck(ITEM_PAUSE_ON_MINIMIZE_ROUND, LocaleController.getString("PauseOnMinimizeRound", R.string.PauseOnMinimizeRound))
                .setChecked(HundredGramConfig.pauseOnMinimizeRound));
        items.add(UItem.asShadow(LocaleController.getString("PauseOnMinimizeVideoInfo", R.string.PauseOnMinimizeVideoInfo)));

        // Custom save path
        String savePathValue = android.text.TextUtils.isEmpty(HundredGramConfig.customSavePath)
                ? LocaleController.getString("CustomSavePathDefault", R.string.CustomSavePathDefault)
                : HundredGramConfig.customSavePath;
        items.add(UItem.asButton(ITEM_CUSTOM_SAVE_PATH, LocaleController.getString("CustomSavePath", R.string.CustomSavePath), savePathValue));
        items.add(UItem.asShadow(LocaleController.getString("CustomSavePathInfo", R.string.CustomSavePathInfo)));
    }

    private void fillStreamerItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(LocaleController.getString("StreamerMode", R.string.StreamerMode)));
        items.add(UItem.asCheck(ITEM_STREAMER_MODE, LocaleController.getString("StreamerMode", R.string.StreamerMode))
                .setChecked(HundredGramConfig.isStreamerMode()));
        items.add(UItem.asCheck(ITEM_STREAMER_BLOCK_SCREENSHOTS, LocaleController.getString("StreamerBlockScreenshots", R.string.StreamerBlockScreenshots))
                .setChecked(HundredGramConfig.streamerBlockScreenshots));
        items.add(UItem.asButton(ITEM_STREAMER_FAKE_NAME, LocaleController.getString("StreamerFakeName", R.string.StreamerFakeName),
                HundredGramConfig.streamerFakeName != null && !HundredGramConfig.streamerFakeName.isEmpty() ? HundredGramConfig.streamerFakeName : "Streamer"));
        items.add(UItem.asShadow(LocaleController.getString("StreamerModeInfo", R.string.StreamerModeInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramPrivacyDataHiding", R.string.HundredGramPrivacyDataHiding)));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_PHONE, LocaleController.getString("StreamerHidePhones", R.string.StreamerHidePhones))
                .setChecked(HundredGramConfig.streamerHidePhoneNumbers));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_USERNAMES, LocaleController.getString("StreamerHideUsernames", R.string.StreamerHideUsernames))
                .setChecked(HundredGramConfig.streamerHideUsernames));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_AVATARS, LocaleController.getString("StreamerHideAvatars", R.string.StreamerHideAvatars))
                .setChecked(HundredGramConfig.streamerHideAvatars));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_DIALOG_MESSAGES, LocaleController.getString("StreamerHideDialogMessages", R.string.StreamerHideDialogMessages))
                .setChecked(HundredGramConfig.streamerHideDialogMessages));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_ID_DC, LocaleController.getString("StreamerHideIdDc", R.string.StreamerHideIdDc))
                .setChecked(HundredGramConfig.streamerHideIdAndDc));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_NOTIF, LocaleController.getString("StreamerHideNotificationPreview", R.string.StreamerHideNotificationPreview))
                .setChecked(HundredGramConfig.streamerHideNotificationPreview));
        items.add(UItem.asCheck(ITEM_STREAMER_HIDE_ACCOUNTS, LocaleController.getString("StreamerHideOtherAccounts", R.string.StreamerHideOtherAccounts))
                .setChecked(HundredGramConfig.streamerHideOtherAccounts));
        items.add(UItem.asShadow(LocaleController.getString("StreamerHidePhonesInfo", R.string.StreamerHidePhonesInfo)));

        items.add(UItem.asHeader(LocaleController.getString("HundredGramDeleteSection", R.string.HundredGramDeleteSection)));
        items.add(UItem.asCheck(ITEM_STREAMER_PREVENT_CALLS, LocaleController.getString("StreamerPreventAccidentalCalls", R.string.StreamerPreventAccidentalCalls))
                .setChecked(HundredGramConfig.streamerPreventAccidentalCalls));
        items.add(UItem.asCheck(ITEM_STREAMER_PREVENT_DELETE, LocaleController.getString("StreamerPreventAccidentalDelete", R.string.StreamerPreventAccidentalDelete))
                .setChecked(HundredGramConfig.streamerPreventAccidentalDelete));
        items.add(UItem.asShadow(LocaleController.getString("StreamerPreventAccidentalCallsInfo", R.string.StreamerPreventAccidentalCallsInfo)));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ITEM_HAPTIC_ACTION && item.id < ITEM_HAPTIC_ACTION + HundredGramHaptics.ACTION_NAMES.length) {
            showHapticEffectDialog(HundredGramHaptics.ACTION_NAMES[item.id - ITEM_HAPTIC_ACTION]);
            return;
        }
        switch (item.id) {
            // CATEGORY_VAULT
            case ITEM_SAVE_PRESET:
                showSavePresetDialog();
                break;
            case ITEM_SAVE_SCOPE_PRIVATE:
                HundredGramConfig.toggleSaveScopePrivate();
                item.setChecked(HundredGramConfig.saveScopePrivate);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveScopePrivate);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_SCOPE_GROUPS:
                HundredGramConfig.toggleSaveScopeGroups();
                item.setChecked(HundredGramConfig.saveScopeGroups);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveScopeGroups);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_SCOPE_CHANNELS:
                HundredGramConfig.toggleSaveScopeChannels();
                item.setChecked(HundredGramConfig.saveScopeChannels);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveScopeChannels);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_SCOPE_BOTS:
            case ITEM_VAULT_SAVE_FOR_BOTS:
                HundredGramConfig.toggleSaveScopeBots();
                item.setChecked(HundredGramConfig.saveScopeBots);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveScopeBots);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_SCOPE_ARCHIVED:
            case ITEM_VAULT_INCLUDE_ARCHIVED:
                HundredGramConfig.toggleSaveScopeArchived();
                item.setChecked(HundredGramConfig.saveScopeArchived);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveScopeArchived);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_ANTI_DELETE:
                HundredGramConfig.toggleSaveAntiDeleteText();
                item.setChecked(HundredGramConfig.saveAntiDeleteText);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveAntiDeleteText);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_EDIT_HISTORY:
                HundredGramConfig.toggleSaveEditHistory();
                item.setChecked(HundredGramConfig.saveEditHistory);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveEditHistory);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_MEDIA_PHOTOS:
                HundredGramConfig.toggleSaveMediaPhotos();
                item.setChecked(HundredGramConfig.saveMediaPhotos);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveMediaPhotos);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_MEDIA_VOICE_ROUNDS:
                HundredGramConfig.toggleSaveMediaVoiceRounds();
                item.setChecked(HundredGramConfig.saveMediaVoiceRounds);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveMediaVoiceRounds);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_MEDIA_VIDEOS:
                HundredGramConfig.toggleSaveMediaVideos();
                item.setChecked(HundredGramConfig.saveMediaVideos);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveMediaVideos);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SAVE_MEDIA_FILES:
                HundredGramConfig.toggleSaveMediaFiles();
                item.setChecked(HundredGramConfig.saveMediaFiles);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.saveMediaFiles);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
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
            case ITEM_PROTECTED_VAULT_SIZE:
                presentFragment(new HundredGramVaultViewerActivity());
                break;
            case ITEM_CLEAR_ALL_VAULT:
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
            case ITEM_VAULT_PRUNE_ORPHANS:
                HundredGramVaultManager.getInstance(currentAccount).pruneOrphanDatabaseEntries(cleanedCount -> {
                    BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(
                            R.raw.done,
                            LocaleController.formatString("HundredGramVaultPruneOrphansDone", R.string.HundredGramVaultPruneOrphansDone, cleanedCount)
                    ).show();
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                });
                break;
            case ITEM_VAULT_EXPORT_ALL_ZIP:
                showExportAllVaultZipPrompt();
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
            case ITEM_ACTIVITY_SIMULATOR:
                presentFragment(new HundredGramActivitySimulatorActivity());
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
                showIdAndDcDialog();
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
            case ITEM_SHADOWBAN:
                presentFragment(new HundredGramShadowbanActivity());
                break;
            case ITEM_ADBLOCK:
                HundredGramConfig.toggleEnableAdBlock();
                item.setChecked(HundredGramConfig.enableAdBlock);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.enableAdBlock);
                }
                break;

            // CATEGORY_GHOST
            case ITEM_GHOST_ONLINE:
                HundredGramConfig.toggleGhostOnline();
                item.setChecked(HundredGramConfig.ghostOnline);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostOnline);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_SEND_OFFLINE_AFTER_ONLINE:
                HundredGramConfig.toggleSendOfflineAfterOnline();
                item.setChecked(HundredGramConfig.sendOfflineAfterOnline);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.sendOfflineAfterOnline);
                }
                break;
            case ITEM_GHOST_UPLOAD_PROGRESS:
                HundredGramConfig.toggleGhostUploadProgress();
                item.setChecked(HundredGramConfig.ghostUploadProgress);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.ghostUploadProgress);
                }
                break;
            case ITEM_TRACK_READ_DATE:
                HundredGramConfig.toggleTrackReadDate();
                item.setChecked(HundredGramConfig.trackReadDate);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.trackReadDate);
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
            case ITEM_QUICK_CHANNEL_TRANSITION:
                HundredGramConfig.toggleQuickChannelTransition();
                item.setChecked(HundredGramConfig.quickChannelTransition);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.quickChannelTransition);
                }
                break;
            case ITEM_HIDE_KEYBOARD_ON_SCROLL:
                HundredGramConfig.toggleHideKeyboardOnScroll();
                item.setChecked(HundredGramConfig.hideKeyboardOnScroll);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideKeyboardOnScroll);
                }
                break;
            case ITEM_CHAT_PAGE_UP:
                HundredGramConfig.toggleChatPageUpButton();
                item.setChecked(HundredGramConfig.chatPageUpButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.chatPageUpButton);
                }
                break;
            case ITEM_RESET_ALL:
                showResetAllDialog();
                break;
            case ITEM_DELETE_MY_MESSAGES:
                openDeleteMyMessagesPicker();
                break;
            case ITEM_LOCK_ARCHIVE:
                if (lockAllowed(HundredGramConfig.lockArchive)) {
                    HundredGramConfig.toggleLockArchive();
                }
                item.setChecked(HundredGramConfig.lockArchive);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.lockArchive);
                }
                break;
            case ITEM_LOCK_SECRET:
                if (lockAllowed(HundredGramConfig.lockSecret)) {
                    HundredGramConfig.toggleLockSecret();
                }
                item.setChecked(HundredGramConfig.lockSecret);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.lockSecret);
                }
                break;
            case ITEM_LOCK_SAVED:
                if (lockAllowed(HundredGramConfig.lockSaved)) {
                    HundredGramConfig.toggleLockSaved();
                }
                item.setChecked(HundredGramConfig.lockSaved);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.lockSaved);
                }
                break;
            case ITEM_LOCK_DELETE:
                if (lockAllowed(HundredGramConfig.lockDelete)) {
                    HundredGramConfig.toggleLockDelete();
                }
                item.setChecked(HundredGramConfig.lockDelete);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.lockDelete);
                }
                break;
            case ITEM_LOCK_GRACE:
                showLockGraceDialog();
                break;
            case ITEM_HAPTICS:
                HundredGramConfig.toggleHaptics();
                item.setChecked(HundredGramConfig.hapticsEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hapticsEnabled);
                }
                if (HundredGramConfig.hapticsEnabled) {
                    HundredGramHaptics.playEffect(view, 10);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_HAPTIC_STRENGTH:
                showHapticStrengthDialog();
                break;
            case ITEM_ADD_COMMA_AFTER_MENTION:
                HundredGramConfig.toggleAddCommaAfterMention();
                item.setChecked(HundredGramConfig.addCommaAfterMention);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.addCommaAfterMention);
                }
                break;
            case ITEM_INLINE_MATH:
                HundredGramConfig.toggleInlineMath();
                item.setChecked(HundredGramConfig.inlineMath);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.inlineMath);
                }
                break;
            case ITEM_DISABLE_GREETING_STICKER:
                HundredGramConfig.toggleDisableGreetingSticker();
                item.setChecked(HundredGramConfig.disableGreetingSticker);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.disableGreetingSticker);
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
            case ITEM_HIDE_DIALOGS_SEARCH_BAR:
                HundredGramConfig.toggleHideDialogsSearchBar();
                item.setChecked(HundredGramConfig.hideDialogsSearchBar);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideDialogsSearchBar);
                }
                break;
            case ITEM_HIGHLIGHT_DELETED:
                HundredGramConfig.toggleHighlightDeletedMessages();
                item.setChecked(HundredGramConfig.highlightDeletedMessages);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.highlightDeletedMessages);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_DELETED_MARK_ICON:
                showDeletedMarkIconDialog();
                break;
            case ITEM_DELETED_MARK_COLOR:
                showDeletedMarkColorDialog();
                break;
            case ITEM_DELETED_MESSAGES_TRANSPARENT:
                HundredGramConfig.toggleDeletedMessagesTransparent();
                item.setChecked(HundredGramConfig.isDeletedMessagesTransparent());
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.isDeletedMessagesTransparent());
                }
                break;
            case ITEM_CONTENT_FILTER:
                presentFragment(new HundredGramContentFilterActivity());
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
            case ITEM_HIDE_STORIES_PROFILE:
                HundredGramConfig.toggleHideStoriesProfile();
                item.setChecked(HundredGramConfig.hideStoriesProfile);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideStoriesProfile);
                }
                break;
            case ITEM_HIDE_STORIES_POSTING:
                HundredGramConfig.toggleHideStoriesPosting();
                item.setChecked(HundredGramConfig.hideStoriesPosting);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideStoriesPosting);
                }
                break;
            case ITEM_SHOW_SECONDS_IN_TIME:
                HundredGramConfig.toggleShowSecondsInTime();
                item.setChecked(HundredGramConfig.showSecondsInTime);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showSecondsInTime);
                }
                if (messageBubblePreviewCell != null) {
                    messageBubblePreviewCell.update();
                }
                break;
            case ITEM_SHOW_WEEKDAY_IN_DATE:
                HundredGramConfig.toggleShowWeekdayInDate();
                item.setChecked(HundredGramConfig.showWeekdayInDate);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showWeekdayInDate);
                }
                break;
            case ITEM_FORWARD_TIME:
                HundredGramConfig.toggleForwardTime();
                item.setChecked(HundredGramConfig.forwardTime);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.forwardTime);
                }
                break;
            case ITEM_SELECTION_SPEED:
                showSelectionSpeedDialog();
                break;
            case ITEM_SELECTION_NO_LIMIT:
                HundredGramConfig.toggleSelectionNoLimit();
                item.setChecked(HundredGramConfig.selectionNoLimit);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.selectionNoLimit);
                }
                break;
            case ITEM_MEDIA_GLOW:
                showMediaGlowDialog();
                break;
            case ITEM_THANOS_DELETE_EFFECT:
                HundredGramConfig.toggleThanosDeleteEffect();
                item.setChecked(HundredGramConfig.thanosDeleteEffect);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.thanosDeleteEffect);
                }
                break;
            case ITEM_PILL_STACK_ENABLED:
                HundredGramConfig.togglePillStackEnabled();
                item.setChecked(HundredGramConfig.pillStackEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.pillStackEnabled);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_PILL_STACK_SETTINGS:
                presentFragment(new HundredGramPillStackSettingsActivity());
                break;
            case ITEM_MD3_SWITCH_STYLE:
                HundredGramConfig.toggleMd3SwitchStyle();
                item.setChecked(HundredGramConfig.md3SwitchStyle);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.md3SwitchStyle);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_NEW_SLIDER_STYLE:
                HundredGramConfig.toggleNewSliderStyle();
                item.setChecked(HundredGramConfig.newSliderStyle);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.newSliderStyle);
                }
                if (avatarCornersPreviewCell != null) {
                    avatarCornersPreviewCell.updateSliderStyle();
                }
                break;
            case ITEM_SQUARE_FAB:
                HundredGramConfig.toggleSquareFab();
                item.setChecked(HundredGramConfig.squareFab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.squareFab);
                }
                break;
            case ITEM_BOTTOM_NAV_HIDE_CONTACTS:
                HundredGramConfig.toggleBottomNavHideContactsTab();
                item.setChecked(HundredGramConfig.bottomNavHideContactsTab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.bottomNavHideContactsTab);
                }
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.callTabsVisibleToggled);
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_BOTTOM_NAV_HIDE_SETTINGS:
                HundredGramConfig.toggleBottomNavHideSettingsTab();
                item.setChecked(HundredGramConfig.bottomNavHideSettingsTab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.bottomNavHideSettingsTab);
                }
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.callTabsVisibleToggled);
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_BOTTOM_NAV_SHOW_ARCHIVE:
                HundredGramConfig.toggleBottomNavArchiveTab();
                item.setChecked(HundredGramConfig.bottomNavArchiveTab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.bottomNavArchiveTab);
                }
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.callTabsVisibleToggled);
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_BOTTOM_NAV_SHOW_FEED:
                HundredGramConfig.toggleEnableFeedTab();
                item.setChecked(HundredGramConfig.enableFeedTab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.enableFeedTab);
                }
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.feedTabVisibleToggled);
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_BOTTOM_NAV_SHOW_MUSIC:
                HundredGramConfig.toggleBottomNavMusicTab();
                item.setChecked(HundredGramConfig.bottomNavMusicTab);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.bottomNavMusicTab);
                }
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.feedTabVisibleToggled);
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_AVATAR_HISTORY_ENABLED:
                HundredGramConfig.toggleAvatarHistoryEnabled();
                item.setChecked(HundredGramConfig.avatarHistoryEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.avatarHistoryEnabled);
                }
                break;
            case ITEM_PROFILE_HISTORY_ENABLED:
                HundredGramConfig.toggleProfileHistoryEnabled();
                item.setChecked(HundredGramConfig.profileHistoryEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.profileHistoryEnabled);
                }
                break;
            case ITEM_FILTER_ZALGO:
                HundredGramConfig.toggleFilterZalgo();
                item.setChecked(HundredGramConfig.filterZalgo);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.filterZalgo);
                }
                break;
            case ITEM_REPLACE_EDITED_WITH_ICON:
                HundredGramConfig.toggleReplaceEditedWithIcon();
                item.setChecked(HundredGramConfig.replaceEditedWithIcon);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.replaceEditedWithIcon);
                }
                if (messageBubblePreviewCell != null) {
                    messageBubblePreviewCell.update();
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_REMOVE_MESSAGE_TAIL:
                HundredGramConfig.toggleRemoveMessageTail();
                item.setChecked(HundredGramConfig.removeMessageTail);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.removeMessageTail);
                }
                if (messageBubblePreviewCell != null) {
                    messageBubblePreviewCell.update();
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_WIDE_POSTS_IN_CHANNELS:
                HundredGramConfig.toggleWidePostsInChannels();
                item.setChecked(HundredGramConfig.widePostsInChannels);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.widePostsInChannels);
                }
                break;
            case ITEM_HIDE_SHARE_BUTTON:
                HundredGramConfig.hideShareButton = !HundredGramConfig.hideShareButton;
                HundredGramConfig.save("hideShareButton", HundredGramConfig.hideShareButton);
                item.setChecked(HundredGramConfig.hideShareButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideShareButton);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_HIDE_REACTIONS_IN_CHANNELS:
                HundredGramConfig.hideReactionsInChannels = !HundredGramConfig.hideReactionsInChannels;
                HundredGramConfig.save("hideReactionsInChannels", HundredGramConfig.hideReactionsInChannels);
                item.setChecked(HundredGramConfig.hideReactionsInChannels);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideReactionsInChannels);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_HIDE_REACTIONS_IN_GROUPS:
                HundredGramConfig.hideReactionsInGroups = !HundredGramConfig.hideReactionsInGroups;
                HundredGramConfig.save("hideReactionsInGroups", HundredGramConfig.hideReactionsInGroups);
                item.setChecked(HundredGramConfig.hideReactionsInGroups);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideReactionsInGroups);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_HIDE_SEND_AS_PEER:
                HundredGramConfig.hideSendAsPeer = !HundredGramConfig.hideSendAsPeer;
                HundredGramConfig.save("hideSendAsPeer", HundredGramConfig.hideSendAsPeer);
                item.setChecked(HundredGramConfig.hideSendAsPeer);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideSendAsPeer);
                }
                break;
            case ITEM_SENDER_MINI_AVATARS:
                HundredGramConfig.senderMiniAvatars = !HundredGramConfig.senderMiniAvatars;
                HundredGramConfig.save("senderMiniAvatars", HundredGramConfig.senderMiniAvatars);
                item.setChecked(HundredGramConfig.senderMiniAvatars);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.senderMiniAvatars);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_PAUSE_ON_MINIMIZE_VIDEO:
                HundredGramConfig.pauseOnMinimizeVideo = !HundredGramConfig.pauseOnMinimizeVideo;
                HundredGramConfig.save("pauseOnMinimizeVideo", HundredGramConfig.pauseOnMinimizeVideo);
                item.setChecked(HundredGramConfig.pauseOnMinimizeVideo);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.pauseOnMinimizeVideo);
                }
                break;
            case ITEM_PAUSE_ON_MINIMIZE_VOICE:
                HundredGramConfig.pauseOnMinimizeVoice = !HundredGramConfig.pauseOnMinimizeVoice;
                HundredGramConfig.save("pauseOnMinimizeVoice", HundredGramConfig.pauseOnMinimizeVoice);
                item.setChecked(HundredGramConfig.pauseOnMinimizeVoice);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.pauseOnMinimizeVoice);
                }
                break;
            case ITEM_PAUSE_ON_MINIMIZE_ROUND:
                HundredGramConfig.pauseOnMinimizeRound = !HundredGramConfig.pauseOnMinimizeRound;
                HundredGramConfig.save("pauseOnMinimizeRound", HundredGramConfig.pauseOnMinimizeRound);
                item.setChecked(HundredGramConfig.pauseOnMinimizeRound);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.pauseOnMinimizeRound);
                }
                break;
            case ITEM_SHOW_FORWARDS_COUNT:
                HundredGramConfig.toggleShowForwardsCount();
                item.setChecked(HundredGramConfig.showForwardsCount);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showForwardsCount);
                }
                if (messageBubblePreviewCell != null) {
                    messageBubblePreviewCell.update();
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_HIDE_ACTION_BAR_STATUS:
                HundredGramConfig.toggleHideActionBarStatus();
                item.setChecked(HundredGramConfig.hideActionBarStatus);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideActionBarStatus);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_QUICK_TOPIC_TRANSITION:
                HundredGramConfig.toggleQuickTopicTransition();
                item.setChecked(HundredGramConfig.quickTopicTransition);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.quickTopicTransition);
                }
                break;
            case ITEM_ACTION_BAR_TITLE_MODE:
                showActionBarTitleModeDialog();
                break;
            case ITEM_CENTER_ACTION_BAR_TITLE:
                HundredGramConfig.toggleCenterActionBarTitle();
                item.setChecked(HundredGramConfig.centerActionBarTitle);
                setCellChecked(view, HundredGramConfig.centerActionBarTitle);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ITEM_TAB_TITLE_STYLE:
                showTabTitleStyleDialog();
                break;
            case ITEM_TAB_COUNTER_MODE:
                showTabCounterModeDialog();
                break;
            case ITEM_TRANSITION_ANIMATION:
                showTransitionAnimationDialog();
                break;
            case ITEM_DIVIDER_STYLE:
                showDividerStyleDialog();
                break;
            case ITEM_FOLDER_SWIPE:
                HundredGramConfig.toggleFolderSwipe();
                item.setChecked(HundredGramConfig.folderSwipe);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.folderSwipe);
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_FOLDER_SWIPE_SHRINK:
                showFolderSwipeShrinkDialog();
                break;
            case ITEM_FOLDER_SWIPE_SPRING:
                showFolderSwipeSpringDialog();
                break;
            case ITEM_DISABLE_NOTIFICATION_DELAY:
                HundredGramConfig.toggleDisableNotificationDelay();
                item.setChecked(HundredGramConfig.disableNotificationDelay);
                setCellChecked(view, HundredGramConfig.disableNotificationDelay);
                break;
            case ITEM_ARCHIVE_ON_PULL:
                HundredGramConfig.toggleArchiveOnPull();
                item.setChecked(HundredGramConfig.archiveOnPull);
                setCellChecked(view, HundredGramConfig.archiveOnPull);
                break;
            case ITEM_CHANNEL_BOTTOM_BUTTON:
                showChannelBottomButtonDialog();
                break;
            case ITEM_STICKER_TIME_MODE:
                showStickerTimeModeDialog();
                break;

            // CATEGORY_ACTIONS
            case ITEM_MESSAGE_MENU_ORDER:
                presentFragment(new HundredGramMessageMenuActivity());
                break;
            case ITEM_DOUBLE_TAP_INCOMING:
                showDoubleTapDialog(false);
                break;
            case ITEM_DOUBLE_TAP_OUTGOING:
                showDoubleTapDialog(true);
                break;
            case ITEM_CHAT_MESSAGE_SWIPE_ACTION:
                presentFragment(new HundredGramSwipeActionsActivity());
                break;
            case ITEM_DISABLE_UNARCHIVE_SWIPE:
                HundredGramConfig.toggleDisableUnarchiveSwipe();
                item.setChecked(HundredGramConfig.disableUnarchiveSwipe);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.disableUnarchiveSwipe);
                }
                break;
            case ITEM_SHOW_REAL_NAME:
                HundredGramConfig.toggleShowRealName();
                item.setChecked(HundredGramConfig.showRealName);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showRealName);
                }
                break;
            case ITEM_PINNED_DOWNLOADS:
                HundredGramConfig.togglePinnedDownloads();
                item.setChecked(HundredGramConfig.pinnedDownloads);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.pinnedDownloads);
                }
                break;
            case ITEM_HIDE_ALL_CHATS_FOLDER:
                HundredGramConfig.toggleHideAllChatsFolder();
                item.setChecked(HundredGramConfig.hideAllChatsFolder);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.hideAllChatsFolder);
                }
                break;
            case ITEM_SHOW_SAVE_MESSAGE:
                HundredGramConfig.toggleShowSaveMessageButton();
                item.setChecked(HundredGramConfig.showSaveMessageButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showSaveMessageButton);
                }
                break;
            case ITEM_SHOW_REPEAT_MESSAGE:
                HundredGramConfig.toggleShowRepeatMessageButton();
                item.setChecked(HundredGramConfig.showRepeatMessageButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showRepeatMessageButton);
                }
                break;
            case ITEM_SHOW_CLEAR_INPUT:
                HundredGramConfig.toggleShowClearInputButton();
                item.setChecked(HundredGramConfig.showClearInputButton);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showClearInputButton);
                }
                break;
            case ITEM_SHOW_RESULTS_BEFORE_VOTING:
                HundredGramConfig.toggleShowResultsBeforeVoting();
                item.setChecked(HundredGramConfig.showResultsBeforeVoting);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.showResultsBeforeVoting);
                }
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
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
            case ITEM_LOCAL_STT_ENABLED:
                HundredGramConfig.toggleLocalStt();
                item.setChecked(HundredGramConfig.localSttEnabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.localSttEnabled);
                }
                if (HundredGramConfig.localSttEnabled && !org.telegram.messenger.HundredGramSttManager.isModelDownloaded()) {
                    org.telegram.messenger.HundredGramSttManager.showDownloadDialog(HundredGramSubSettingsActivity.this, () -> {
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    });
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ITEM_LOCAL_STT_MODEL:
                if (org.telegram.messenger.HundredGramSttManager.isModelDownloaded()) {
                    showModelManageDialog();
                } else {
                    org.telegram.messenger.HundredGramSttManager.showDownloadDialog(HundredGramSubSettingsActivity.this, () -> {
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    });
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
            case ITEM_UNMUTE_WITH_VOLUME_BUTTONS:
                HundredGramConfig.toggleUnmuteWithVolumeButtons();
                item.setChecked(HundredGramConfig.unmuteWithVolumeButtons);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.unmuteWithVolumeButtons);
                }
                break;
            case ITEM_SWIPE_TO_PIP:
                HundredGramConfig.toggleSwipeToPip();
                item.setChecked(HundredGramConfig.swipeToPip);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.swipeToPip);
                }
                break;
            case ITEM_DOUBLE_TAP_SEEK_DURATION:
                showDoubleTapSeekDurationDialog();
                break;
            case ITEM_EXTENDED_RECENT_STICKERS:
                HundredGramConfig.toggleExtendedRecentStickers();
                item.setChecked(HundredGramConfig.extendedRecentStickers);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.extendedRecentStickers);
                }
                break;
            case ITEM_UNLIMITED_RECENT_STICKERS:
                HundredGramConfig.toggleUnlimitedRecentStickers();
                item.setChecked(HundredGramConfig.unlimitedRecentStickers);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.unlimitedRecentStickers);
                }
                break;
            case ITEM_PREFER_ORIGINAL_QUALITY:
                HundredGramConfig.togglePreferOriginalQuality();
                item.setChecked(HundredGramConfig.preferOriginalQuality);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.preferOriginalQuality);
                }
                break;
            case ITEM_REMEMBER_LAST_USED_CAMERA:
                HundredGramConfig.toggleRememberLastUsedCamera();
                item.setChecked(HundredGramConfig.rememberLastUsedCamera);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.rememberLastUsedCamera);
                }
                break;

            // Batch 4: Visual features
            case ITEM_STICKER_SHAPE:
                showStickerShapeDialog();
                break;
            case ITEM_REPLY_BACKGROUND:
                HundredGramConfig.toggleReplyBackground();
                item.setChecked(HundredGramConfig.replyBackground);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.replyBackground);
                }
                break;
            case ITEM_REPLY_COLORS:
                HundredGramConfig.toggleReplyColors();
                item.setChecked(HundredGramConfig.replyColors);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.replyColors);
                }
                break;
            case ITEM_REPLY_EMOJI:
                HundredGramConfig.toggleReplyEmoji();
                item.setChecked(HundredGramConfig.replyEmoji);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(HundredGramConfig.replyEmoji);
                }
                break;
            case ITEM_CUSTOM_SAVE_PATH:
                showCustomSavePathDialog();
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

    private void showDoubleTapSeekDurationDialog() {
        if (getParentActivity() == null) return;
        int[] values = new int[] { 5, 10, 15, 20 };
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            options[i] = String.format(LocaleController.getString("DoubleTapSeekSeconds", R.string.DoubleTapSeekSeconds), values[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DoubleTapSeekDuration", R.string.DoubleTapSeekDuration));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setDoubleTapSeekDuration(values[which]);
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
            if (stickerSizePreviewCell != null) {
                stickerSizePreviewCell.update();
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        builder.show();
    }

    private void showStickerShapeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("StickerShapeDefault", R.string.StickerShapeDefault),
            LocaleController.getString("StickerShapeRounded", R.string.StickerShapeRounded),
            LocaleController.getString("StickerShapeBubble", R.string.StickerShapeBubble)
        };
        int[] values = new int[] {
            HundredGramConfig.STICKER_SHAPE_DEFAULT,
            HundredGramConfig.STICKER_SHAPE_ROUNDED,
            HundredGramConfig.STICKER_SHAPE_BUBBLE
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("StickerShape", R.string.StickerShape));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setStickerShape(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        builder.show();
    }

    private void showCustomSavePathDialog() {
        if (getParentActivity() == null) return;
        org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("CustomSavePathDialogTitle", R.string.CustomSavePathDialogTitle));
        final android.widget.EditText editText = new android.widget.EditText(getParentActivity());
        editText.setHint("100gram");
        editText.setText(HundredGramConfig.customSavePath);
        editText.setSelection(editText.getText().length());
        editText.setSingleLine(true);
        builder.setView(editText);
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setNeutralButton(LocaleController.getString("Default", R.string.Default), (dialog, which) -> {
            HundredGramConfig.setCustomSavePath("");
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            String path = editText.getText().toString().trim();
            HundredGramConfig.setCustomSavePath(path);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
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

    private void showClearProtectedVaultConfirmDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramProtectedVaultClear", R.string.HundredGramProtectedVaultClear));
        builder.setMessage(LocaleController.getString("HundredGramProtectedVaultClearConfirm", R.string.HundredGramProtectedVaultClearConfirm));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            long totalSize = HundredGramVaultManager.getInstance(currentAccount).getVaultTotalSize();
            String sizeStr = AndroidUtilities.formatFileSize(totalSize);
            FileLoader.getInstance(currentAccount).cancelLoadAllFiles();
            FileLoader.getInstance(currentAccount).getFileDatabase().clearAllVaultFiles(freedDbBytes -> {});
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

    private void showExportAllVaultZipPrompt() {
        if (getParentActivity() == null) return;
        File vaultDir = HundredGramVaultManager.getInstance(currentAccount).getVaultDir();
        if (vaultDir == null || !vaultDir.exists()) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramVaultNoFiles", R.string.HundredGramVaultNoFiles)).show();
            return;
        }
        File[] filesArr = vaultDir.listFiles();
        if (filesArr == null || filesArr.length == 0) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramVaultNoFiles", R.string.HundredGramVaultNoFiles)).show();
            return;
        }
        ArrayList<File> files = new ArrayList<>();
        for (File f : filesArr) {
            if (f != null && f.isFile() && f.length() > 0) {
                files.add(f);
            }
        }
        if (files.isEmpty()) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramVaultNoFiles", R.string.HundredGramVaultNoFiles)).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_LOADING);
        progressDialog.setMessage(LocaleController.getString("HundredGramVaultExportingZip", R.string.HundredGramVaultExportingZip));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        showDialog(progressDialog);

        File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
        File destZip = new File(cacheDir, "100gram_vault_backup_" + System.currentTimeMillis() + ".zip");
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
                            ApplicationLoader.applicationContext,
                            ApplicationLoader.getApplicationId() + ".provider",
                            zipFile
                    );
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getParentActivity().startActivity(Intent.createChooser(intent, LocaleController.getString("HundredGramVaultExportZip", R.string.HundredGramVaultExportZip)));
                    BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(
                            R.raw.done,
                            LocaleController.getString("HundredGramVaultZipSuccess", R.string.HundredGramVaultZipSuccess)
                    ).show();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        });
    }

    private void showSavePresetDialog() {
        if (getParentActivity() == null) return;
        CharSequence[] options = new CharSequence[] {
            AndroidUtilities.replaceTags("<b>" + LocaleController.getString("SavePresetSmart", R.string.SavePresetSmart) + "</b>\n" + LocaleController.getString("SavePresetSmartDesc", R.string.SavePresetSmartDesc)),
            AndroidUtilities.replaceTags("<b>" + LocaleController.getString("SavePresetDMsOnly", R.string.SavePresetDMsOnly) + "</b>\n" + LocaleController.getString("SavePresetDMsOnlyDesc", R.string.SavePresetDMsOnlyDesc)),
            AndroidUtilities.replaceTags("<b>" + LocaleController.getString("SavePresetAll", R.string.SavePresetAll) + "</b>\n" + LocaleController.getString("SavePresetAllDesc", R.string.SavePresetAllDesc)),
            AndroidUtilities.replaceTags("<b>" + LocaleController.getString("SavePresetStorageSaver", R.string.SavePresetStorageSaver) + "</b>\n" + LocaleController.getString("SavePresetStorageSaverDesc", R.string.SavePresetStorageSaverDesc)),
            LocaleController.getString("SavePresetCustom", R.string.SavePresetCustom)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("SavePresetTitle", R.string.SavePresetTitle));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setSavePreset(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private void showMediaVaultLimitDialog() {
        if (getParentActivity() == null) return;
        String mb = LocaleController.getString("HundredGramMB", R.string.HundredGramMB);
        String gb = LocaleController.getString("HundredGramGB", R.string.HundredGramGB);
        String[] options = new String[] {
            "5 " + mb,
            "15 " + mb,
            "25 " + mb,
            "50 " + mb,
            "100 " + mb,
            "250 " + mb,
            "500 " + mb,
            "1 " + gb,
            LocaleController.getString("HundredGramVaultSizeUnlimited", R.string.HundredGramVaultSizeUnlimited)
        };
        int[] values = new int[] { 5, 15, 25, 50, 100, 250, 500, 1024, -1 };
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

    private void showSwipeActionDialog() {
        if (getParentActivity() == null) return;
        String[] actions = new String[] {
            LocaleController.getString("DoubleTapReply", R.string.DoubleTapReply),
            LocaleController.getString("DoubleTapSave", R.string.DoubleTapSave),
            LocaleController.getString("DoubleTapCopy", R.string.DoubleTapCopy),
            LocaleController.getString("DoubleTapForward", R.string.DoubleTapForward),
            LocaleController.getString("DoubleTapEdit", R.string.DoubleTapEdit),
            LocaleController.getString("DoubleTapDelete", R.string.DoubleTapDelete)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ChatMessageSwipeAction", R.string.ChatMessageSwipeAction));
        builder.setItems(actions, (dialog, which) -> {
            HundredGramConfig.setChatMessageSwipeAction(which);
            if (swipeActionsPreviewCell != null) {
                swipeActionsPreviewCell.update();
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private String getSwipeActionName(int action) {
        switch (action) {
            case HundredGramConfig.SWIPE_ACTION_SAVE: return LocaleController.getString("DoubleTapSave", R.string.DoubleTapSave);
            case HundredGramConfig.SWIPE_ACTION_COPY: return LocaleController.getString("DoubleTapCopy", R.string.DoubleTapCopy);
            case HundredGramConfig.SWIPE_ACTION_FORWARD: return LocaleController.getString("DoubleTapForward", R.string.DoubleTapForward);
            case HundredGramConfig.SWIPE_ACTION_EDIT: return LocaleController.getString("DoubleTapEdit", R.string.DoubleTapEdit);
            case HundredGramConfig.SWIPE_ACTION_DELETE: return LocaleController.getString("DoubleTapDelete", R.string.DoubleTapDelete);
            case HundredGramConfig.SWIPE_ACTION_REPLY:
            default:
                return LocaleController.getString("DoubleTapReply", R.string.DoubleTapReply);
        }
    }

    private void setCellChecked(View cell, boolean checked) {
        if (cell instanceof TextCheckCell) {
            ((TextCheckCell) cell).setChecked(checked);
        } else if (cell instanceof NotificationsCheckCell) {
            ((NotificationsCheckCell) cell).setChecked(checked);
        }
    }

    private void showChannelBottomButtonDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("ChannelBottomButtonMute", R.string.ChannelBottomButtonMute),
            LocaleController.getString("ChannelBottomButtonDiscuss", R.string.ChannelBottomButtonDiscuss),
            LocaleController.getString("ChannelBottomButtonHide", R.string.ChannelBottomButtonHide)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ChannelBottomButton", R.string.ChannelBottomButton));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setChannelBottomButton(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        showDialog(builder.create());
    }

    private String getChannelBottomButtonName(int mode) {
        switch (mode) {
            case HundredGramConfig.CHANNEL_BOTTOM_BUTTON_DISCUSS:
                return LocaleController.getString("ChannelBottomButtonDiscuss", R.string.ChannelBottomButtonDiscuss);
            case HundredGramConfig.CHANNEL_BOTTOM_BUTTON_HIDE:
                return LocaleController.getString("ChannelBottomButtonHide", R.string.ChannelBottomButtonHide);
            case HundredGramConfig.CHANNEL_BOTTOM_BUTTON_MUTE:
            default:
                return LocaleController.getString("ChannelBottomButtonMute", R.string.ChannelBottomButtonMute);
        }
    }

    private void showStickerTimeModeDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("StickerTimeModeDefault", R.string.StickerTimeModeDefault),
            LocaleController.getString("StickerTimeModeSide", R.string.StickerTimeModeSide),
            LocaleController.getString("StickerTimeModeHidden", R.string.StickerTimeModeHidden)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("StickerTimeMode", R.string.StickerTimeMode));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setStickerTimeMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        showDialog(builder.create());
    }

    private String getStickerTimeModeName(int mode) {
        switch (mode) {
            case HundredGramConfig.STICKER_TIME_SIDE:
                return LocaleController.getString("StickerTimeModeSide", R.string.StickerTimeModeSide);
            case HundredGramConfig.STICKER_TIME_HIDDEN:
                return LocaleController.getString("StickerTimeModeHidden", R.string.StickerTimeModeHidden);
            case HundredGramConfig.STICKER_TIME_DEFAULT:
            default:
                return LocaleController.getString("StickerTimeModeDefault", R.string.StickerTimeModeDefault);
        }
    }

    private void showActionBarTitleModeDialog() {
        if (getParentActivity() == null) return;
        String[] modes = new String[] {
            LocaleController.getString("ActionBarTitleAppName", R.string.ActionBarTitleAppName),
            LocaleController.getString("ActionBarTitleUsername", R.string.ActionBarTitleUsername),
            LocaleController.getString("ActionBarTitleName", R.string.ActionBarTitleName),
            LocaleController.getString("ActionBarTitleFilterChats", R.string.ActionBarTitleFilterChats)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ActionBarTitleMode", R.string.ActionBarTitleMode));
        builder.setItems(modes, (dialog, which) -> {
            HundredGramConfig.setActionBarTitleMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        showDialog(builder.create());
    }

    private String getActionBarTitleModeName(int mode) {
        switch (mode) {
            case HundredGramConfig.ACTION_BAR_TITLE_USERNAME:
                return LocaleController.getString("ActionBarTitleUsername", R.string.ActionBarTitleUsername);
            case HundredGramConfig.ACTION_BAR_TITLE_NAME:
                return LocaleController.getString("ActionBarTitleName", R.string.ActionBarTitleName);
            case HundredGramConfig.ACTION_BAR_TITLE_FILTER_CHATS:
                return LocaleController.getString("ActionBarTitleFilterChats", R.string.ActionBarTitleFilterChats);
            case HundredGramConfig.ACTION_BAR_TITLE_APP_NAME:
            default:
                return LocaleController.getString("ActionBarTitleAppName", R.string.ActionBarTitleAppName);
        }
    }

    private void showTabTitleStyleDialog() {
        if (getParentActivity() == null) return;
        String[] styles = new String[] {
            LocaleController.getString("TabTitleStyleTextAndIcons", R.string.TabTitleStyleTextAndIcons),
            LocaleController.getString("TabTitleStyleTextOnly", R.string.TabTitleStyleTextOnly),
            LocaleController.getString("TabTitleStyleIconsOnly", R.string.TabTitleStyleIconsOnly)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TabTitleStyle", R.string.TabTitleStyle));
        builder.setItems(styles, (dialog, which) -> {
            HundredGramConfig.setTabTitleStyle(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        showDialog(builder.create());
    }

    private String getTabTitleStyleName(int style) {
        switch (style) {
            case HundredGramConfig.TAB_TITLE_STYLE_TEXT_ONLY:
                return LocaleController.getString("TabTitleStyleTextOnly", R.string.TabTitleStyleTextOnly);
            case HundredGramConfig.TAB_TITLE_STYLE_ICONS_ONLY:
                return LocaleController.getString("TabTitleStyleIconsOnly", R.string.TabTitleStyleIconsOnly);
            case HundredGramConfig.TAB_TITLE_STYLE_TEXT_AND_ICONS:
            default:
                return LocaleController.getString("TabTitleStyleTextAndIcons", R.string.TabTitleStyleTextAndIcons);
        }
    }

    private void showTabCounterModeDialog() {
        if (getParentActivity() == null) return;
        String[] modes = new String[] {
            LocaleController.getString("TabCounterModeAll", R.string.TabCounterModeAll),
            LocaleController.getString("TabCounterModeUnmuted", R.string.TabCounterModeUnmuted),
            LocaleController.getString("TabCounterModeHide", R.string.TabCounterModeHide)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TabCounterMode", R.string.TabCounterMode));
        builder.setItems(modes, (dialog, which) -> {
            HundredGramConfig.setTabCounterMode(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
        showDialog(builder.create());
    }

    private String getTabCounterModeName(int mode) {
        switch (mode) {
            case HundredGramConfig.TAB_COUNTER_MODE_UNMUTED:
                return LocaleController.getString("TabCounterModeUnmuted", R.string.TabCounterModeUnmuted);
            case HundredGramConfig.TAB_COUNTER_MODE_HIDE:
                return LocaleController.getString("TabCounterModeHide", R.string.TabCounterModeHide);
            case HundredGramConfig.TAB_COUNTER_MODE_ALL:
            default:
                return LocaleController.getString("TabCounterModeAll", R.string.TabCounterModeAll);
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

    private void showModelManageDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("LocalSttModelTitle", R.string.LocalSttModelTitle));
        builder.setMessage(LocaleController.getString("LocalSttModelManageDesc", R.string.LocalSttModelManageDesc));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            org.telegram.messenger.HundredGramSttManager.deleteModel();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (BulletinFactory.canShowBulletin(HundredGramSubSettingsActivity.this)) {
                BulletinFactory.of(HundredGramSubSettingsActivity.this).createSimpleBulletin(
                        R.raw.ic_delete,
                        LocaleController.getString("LocalSttModelDeleted", R.string.LocalSttModelDeleted)
                ).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void showIdAndDcDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("HundredGramShowIdDisabled", R.string.HundredGramShowIdDisabled),
            LocaleController.getString("HundredGramShowIdOnly", R.string.HundredGramShowIdOnly),
            LocaleController.getString("HundredGramShowIdAndDcValue", R.string.HundredGramShowIdAndDcValue)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramShowIdAndDc", R.string.HundredGramShowIdAndDc));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setShowIdAndDc(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTransitionAnimationDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("TransitionDefault", R.string.TransitionDefault),
            LocaleController.getString("TransitionAosp", R.string.TransitionAosp),
            LocaleController.getString("TransitionSpring", R.string.TransitionSpring)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TransitionAnimation", R.string.TransitionAnimation));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setTransitionAnimation(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getFolderSwipeSpringName() {
        switch (HundredGramConfig.folderSwipeSpring) {
            case 1:
                return LocaleController.getString("FolderSwipeSpringStandard", R.string.FolderSwipeSpringStandard);
            case 2:
                return LocaleController.getString("FolderSwipeSpringExpressive", R.string.FolderSwipeSpringExpressive);
            default:
                return LocaleController.getString("Disabled", R.string.Disabled);
        }
    }

    private void showFolderSwipeShrinkDialog() {
        if (getParentActivity() == null) return;
        final int[] values = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            options[i] = values[i] + "%";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("FolderSwipeShrink", R.string.FolderSwipeShrink));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setFolderSwipeShrink(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showFolderSwipeSpringDialog() {
        if (getParentActivity() == null) return;
        String[] options = new String[] {
            LocaleController.getString("Disabled", R.string.Disabled),
            LocaleController.getString("FolderSwipeSpringStandard", R.string.FolderSwipeSpringStandard),
            LocaleController.getString("FolderSwipeSpringExpressive", R.string.FolderSwipeSpringExpressive)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("FolderSwipeSpring", R.string.FolderSwipeSpring));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setFolderSwipeSpring(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDividerStyleDialog() {
        if (getParentActivity() == null) return;        String[] options = new String[] {
            LocaleController.getString("DividerStyleLine", R.string.DividerStyleLine),
            LocaleController.getString("DividerStyleSegments", R.string.DividerStyleSegments),
            LocaleController.getString("DividerStyleHidden", R.string.DividerStyleHidden)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DividerStyle", R.string.DividerStyle));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setDividerStyle(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getDeletedMarkIconTitle(int iconType) {
        switch (iconType) {
            case HundredGramConfig.DELETED_MARK_CROSS:
                return LocaleController.getString("DeletedMarkCross", R.string.DeletedMarkCross);
            case HundredGramConfig.DELETED_MARK_EYE:
                return LocaleController.getString("DeletedMarkEye", R.string.DeletedMarkEye);
            case HundredGramConfig.DELETED_MARK_TRASH:
            default:
                return LocaleController.getString("DeletedMarkTrash", R.string.DeletedMarkTrash);
        }
    }

    private String getSelectionSpeedName() {
        return String.format(LocaleController.getString("SelectionSpeedValue", R.string.SelectionSpeedValue), HundredGramConfig.selectionSpeed);
    }

    private void openDeleteMyMessagesPicker() {
        if (getParentActivity() == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("allowMultiple", true);
        DialogsActivity fragment = new DialogsActivity(args);
        fragment.setDelegate((f, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            ArrayList<Long> ids = new ArrayList<>();
            for (MessagesStorage.TopicKey key : dids) {
                if (key.dialogId != 0 && key.topicId == 0) {
                    ids.add(key.dialogId);
                }
            }
            if (!ids.isEmpty()) {
                HundredGramDeleteMyMessages.show(HundredGramSubSettingsActivity.this, ids);
            }
            return true;
        });
        presentFragment(fragment);
    }

    private void showResetAllDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("ResetAllSettingsTitle", R.string.ResetAllSettingsTitle));
        builder.setMessage(LocaleController.getString("ResetAllSettingsInfo", R.string.ResetAllSettingsInfo));
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            HundredGramConfig.resetAllSettings();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, LocaleController.getString("ResetAllSettingsDone", R.string.ResetAllSettingsDone)).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getHapticActionName(String action) {
        switch (action) {
            case "send_message":
                return LocaleController.getString("HapticSendMessage", R.string.HapticSendMessage);
            case "send_media":
                return LocaleController.getString("HapticSendMedia", R.string.HapticSendMedia);
            case "attach_pick":
                return LocaleController.getString("HapticAttachPick", R.string.HapticAttachPick);
            case "message_incoming":
                return LocaleController.getString("HapticMessageIncoming", R.string.HapticMessageIncoming);
            case "reaction_set":
                return LocaleController.getString("HapticReactionSet", R.string.HapticReactionSet);
            case "select_message":
                return LocaleController.getString("HapticSelectMessage", R.string.HapticSelectMessage);
            case "message_action":
                return LocaleController.getString("HapticMessageAction", R.string.HapticMessageAction);
            case "swipe_reply":
                return LocaleController.getString("HapticSwipeReply", R.string.HapticSwipeReply);
            case "tab_switch":
                return LocaleController.getString("HapticTabSwitch", R.string.HapticTabSwitch);
            case "pull_archive":
                return LocaleController.getString("HapticPullArchive", R.string.HapticPullArchive);
            case "account_switch":
                return LocaleController.getString("HapticAccountSwitch", R.string.HapticAccountSwitch);
            case "slider_step":
                return LocaleController.getString("HapticSliderStep", R.string.HapticSliderStep);
            case "menu_select":
                return LocaleController.getString("HapticMenuSelect", R.string.HapticMenuSelect);
            case "player_track":
                return LocaleController.getString("HapticPlayerTrack", R.string.HapticPlayerTrack);
            case "player_speed":
                return LocaleController.getString("HapticPlayerSpeed", R.string.HapticPlayerSpeed);
            case "photo_swipe":
                return LocaleController.getString("HapticPhotoSwipe", R.string.HapticPhotoSwipe);
            case "photo_dismiss":
                return LocaleController.getString("HapticPhotoDismiss", R.string.HapticPhotoDismiss);
            case "zoom_edge":
                return LocaleController.getString("HapticZoomEdge", R.string.HapticZoomEdge);
            case "error":
                return LocaleController.getString("HapticError", R.string.HapticError);
            default:
                return LocaleController.getString("HapticSuccess", R.string.HapticSuccess);
        }
    }

    private String getHapticEffectName(int effect) {
        switch (effect) {
            case 1:
                return LocaleController.getString("HapticEffectSelection", R.string.HapticEffectSelection);
            case 2:
                return LocaleController.getString("HapticEffectTick", R.string.HapticEffectTick);
            case 3:
                return LocaleController.getString("HapticEffectSoft", R.string.HapticEffectSoft);
            case 4:
                return LocaleController.getString("HapticEffectClick", R.string.HapticEffectClick);
            case 5:
                return LocaleController.getString("HapticEffectRigid", R.string.HapticEffectRigid);
            case 6:
                return LocaleController.getString("HapticEffectHeavy", R.string.HapticEffectHeavy);
            case 7:
                return LocaleController.getString("HapticEffectDouble", R.string.HapticEffectDouble);
            case 8:
                return LocaleController.getString("HapticEffectRise", R.string.HapticEffectRise);
            case 9:
                return LocaleController.getString("HapticEffectFall", R.string.HapticEffectFall);
            case 10:
                return LocaleController.getString("HapticEffectSuccess", R.string.HapticEffectSuccess);
            case 11:
                return LocaleController.getString("HapticEffectWarning", R.string.HapticEffectWarning);
            case 12:
                return LocaleController.getString("HapticEffectError", R.string.HapticEffectError);
            default:
                return LocaleController.getString("Disabled", R.string.Disabled);
        }
    }

    private void showHapticStrengthDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final int[] values = {20, 40, 60, 80, 100, 120, 150, 200};
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            options[i] = values[i] + "%";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HapticStrength", R.string.HapticStrength));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setHapticStrength(values[which]);
            HundredGramHaptics.playEffect(null, 4);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showHapticEffectDialog(String action) {
        if (getParentActivity() == null) {
            return;
        }
        String[] options = new String[HundredGramHaptics.getEffectCount()];
        for (int i = 0; i < options.length; i++) {
            options[i] = getHapticEffectName(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getHapticActionName(action));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                HundredGramConfig.setHapticActionEnabled(action, false);
            } else {
                HundredGramConfig.setHapticActionEnabled(action, true);
                HundredGramConfig.setHapticEffect(action, which);
                HundredGramHaptics.playEffect(null, which);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private boolean lockAllowed(boolean current) {        if (!current && !HundredGramLocks.isSecure()) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString("LockUnavailable", R.string.LockUnavailable)).show();
            return false;
        }
        return true;
    }

    private String getLockGraceName() {
        if (HundredGramConfig.lockGrace <= 0) {
            return LocaleController.getString("LockGraceOff", R.string.LockGraceOff);
        }
        if (HundredGramConfig.lockGrace % 3600 == 0) {
            return LocaleController.formatPluralString("Hours", HundredGramConfig.lockGrace / 3600);
        }
        return LocaleController.formatPluralString("Minutes", HundredGramConfig.lockGrace / 60);
    }

    private void showLockGraceDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final int[] values = {0, 60, 300, 900, 3600};
        String[] options = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) {
                options[i] = LocaleController.getString("LockGraceOff", R.string.LockGraceOff);
            } else if (values[i] % 3600 == 0) {
                options[i] = LocaleController.formatPluralString("Hours", values[i] / 3600);
            } else {
                options[i] = LocaleController.formatPluralString("Minutes", values[i] / 60);
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("LockGrace", R.string.LockGrace));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setLockGrace(values[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showSelectionSpeedDialog() {        if (getParentActivity() == null) {
            return;
        }
        String[] options = new String[7];
        for (int i = 0; i < options.length; i++) {
            options[i] = String.format(LocaleController.getString("SelectionSpeedValue", R.string.SelectionSpeedValue), i + 2);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("SelectionSpeed", R.string.SelectionSpeed));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setSelectionSpeed(which + 2);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getMediaGlowName() {
        switch (HundredGramConfig.mediaGlowLevel) {
            case 1:
                return LocaleController.getString("MediaGlowLow", R.string.MediaGlowLow);
            case 2:
                return LocaleController.getString("MediaGlowMedium", R.string.MediaGlowMedium);
            case 3:
                return LocaleController.getString("MediaGlowHigh", R.string.MediaGlowHigh);
            default:
                return LocaleController.getString("Disabled", R.string.Disabled);
        }
    }

    private void showMediaGlowDialog() {
        if (getParentActivity() == null) {
            return;
        }
        String[] options = new String[]{
                LocaleController.getString("Disabled", R.string.Disabled),
                LocaleController.getString("MediaGlowLow", R.string.MediaGlowLow),
                LocaleController.getString("MediaGlowMedium", R.string.MediaGlowMedium),
                LocaleController.getString("MediaGlowHigh", R.string.MediaGlowHigh)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("MediaGlow", R.string.MediaGlow));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setMediaGlowLevel(which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDeletedMarkIconDialog() {
        if (getParentActivity() == null) {
            return;
        }
        String[] options = new String[]{
                LocaleController.getString("DeletedMarkTrash", R.string.DeletedMarkTrash),
                LocaleController.getString("DeletedMarkCross", R.string.DeletedMarkCross),
                LocaleController.getString("DeletedMarkEye", R.string.DeletedMarkEye)
        };
        int[] types = new int[]{
                HundredGramConfig.DELETED_MARK_TRASH,
                HundredGramConfig.DELETED_MARK_CROSS,
                HundredGramConfig.DELETED_MARK_EYE
        };
        int selected = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == HundredGramConfig.deletedMarkIcon) {
                selected = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DeletedMarkIcon", R.string.DeletedMarkIcon));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setDeletedMarkIcon(types[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private String getDeletedMarkColorTitle(int colorType) {
        switch (colorType) {
            case HundredGramConfig.DELETED_MARK_COLOR_RED:
                return LocaleController.getString("DeletedMarkColorRed", R.string.DeletedMarkColorRed);
            case HundredGramConfig.DELETED_MARK_COLOR_THEME:
            default:
                return LocaleController.getString("DeletedMarkColorTheme", R.string.DeletedMarkColorTheme);
        }
    }

    private void showDeletedMarkColorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        String[] options = new String[]{
                LocaleController.getString("DeletedMarkColorTheme", R.string.DeletedMarkColorTheme),
                LocaleController.getString("DeletedMarkColorRed", R.string.DeletedMarkColorRed)
        };
        int[] types = new int[]{
                HundredGramConfig.DELETED_MARK_COLOR_THEME,
                HundredGramConfig.DELETED_MARK_COLOR_RED
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("DeletedMarkColor", R.string.DeletedMarkColor));
        builder.setItems(options, (dialog, which) -> {
            HundredGramConfig.setDeletedMarkColor(types[which]);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
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
