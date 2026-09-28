package org.telegram.ui.Components;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramChatSettingsBottomSheet extends BottomSheetWithRecyclerListView {

    private static final int ITEM_GHOST_READ = 1;
    private static final int ITEM_GHOST_TYPING = 2;
    private static final int ITEM_HIDE_PHONE = 3;
    private static final int ITEM_ANTI_DELETE = 4;

    private static final int ITEM_MUTE = 10;
    private static final int ITEM_SILENT_SEND = 11;
    private static final int ITEM_AUTO_TRANSLATE = 12;

    private static final int ITEM_PHOTO_QUALITY = 20;
    private static final int ITEM_VIDEO_AS_DOC = 21;

    private static final int ITEM_AUTO_ARCHIVE = 30;
    private static final int ITEM_VAULT_MODE = 31;
    private static final int ITEM_VAULT_PHOTO = 32;
    private static final int ITEM_VAULT_VIDEO = 33;
    private static final int ITEM_VAULT_VOICE = 34;
    private static final int ITEM_VAULT_DOC = 35;
    private static final int ITEM_VAULT_MAX_SIZE = 36;
    private static final int ITEM_CACHE_LIMIT = 37;
    private static final int ITEM_CLEAR_CACHE = 38;
    private static final int ITEM_VAULT_NEVER_EXPIRE = 45;

    private static final int ITEM_RESET = 99;

    private final long dialogId;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;
    private final Runnable onDismissCallback;

    public HundredGramChatSettingsBottomSheet(BaseFragment fragment, long dialogId) {
        this(fragment, dialogId, null);
    }

    public HundredGramChatSettingsBottomSheet(BaseFragment fragment, long dialogId, Runnable onDismissCallback) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.dialogId = dialogId;
        this.onDismissCallback = onDismissCallback;

        topPadding = 0.25f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        recyclerListView.setOnItemClickListener((view, position, x, y) -> {
            if (adapter == null) return;
            UItem item = adapter.getItem(position - 1);
            if (item == null) return;

            switch (item.id) {
                case ITEM_GHOST_READ:
                    showThreeStateDialog(R.string.HundredGramChatOverrideGhostRead, "ghost_read", HundredGramConfig.ghostReadMessages, this::notifyChanged);
                    break;
                case ITEM_GHOST_TYPING:
                    showThreeStateDialog(R.string.HundredGramChatOverrideGhostTyping, "ghost_typing", HundredGramConfig.ghostTyping, this::notifyChanged);
                    break;
                case ITEM_HIDE_PHONE:
                    showThreeStateDialog(R.string.HundredGramChatOverrideHidePhone, "hide_phone", HundredGramConfig.hideOtherPhoneNumbers, this::notifyChanged);
                    break;
                case ITEM_ANTI_DELETE:
                    showThreeStateDialog(R.string.HundredGramChatOverrideAntiDelete, "anti_delete", true, this::notifyChanged);
                    break;
                case ITEM_MUTE: {
                    boolean muted = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0);
                    NotificationsController.getInstance(currentAccount).muteDialog(dialogId, 0, !muted);
                    adapter.update(true);
                    notifyChanged();
                    break;
                }
                case ITEM_SILENT_SEND:
                    showThreeStateDialog(R.string.HundredGramChatOverrideSilentSend, "silent_send", HundredGramConfig.silentSendByDefault, this::notifyChanged);
                    break;
                case ITEM_AUTO_TRANSLATE:
                    showThreeStateDialog(R.string.HundredGramChatOverrideAutoTranslate, "auto_translate", HundredGramConfig.unlockChatTranslation, () -> {
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, HundredGramConfig.isChatAutoTranslate(dialogId));
                        notifyChanged();
                    });
                    break;
                case ITEM_PHOTO_QUALITY:
                    showPhotoQualityDialog();
                    break;
                case ITEM_VIDEO_AS_DOC:
                    showThreeStateDialog(R.string.HundredGramChatOverrideVideoDoc, "video_as_doc", HundredGramConfig.sendVideoAsDocument, this::notifyChanged);
                    break;
                case ITEM_AUTO_ARCHIVE:
                    showThreeStateDialog(R.string.HundredGramChatOverrideAutoArchive, "auto_archive", HundredGramConfig.backgroundArchiverEnabled, this::notifyChanged);
                    break;
                case ITEM_VAULT_MODE:
                    showVaultModeDialog();
                    break;
                case ITEM_VAULT_PHOTO:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_VAULT_VIDEO:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_VAULT_VOICE:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_VAULT_DOC:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_VAULT_MAX_SIZE:
                    showVaultMaxSizeDialog();
                    break;
                case ITEM_CACHE_LIMIT:
                    showCacheLimitDialog();
                    break;
                case ITEM_CLEAR_CACHE:
                    showClearCacheConfirmDialog();
                    break;
                case ITEM_VAULT_NEVER_EXPIRE:
                    HundredGramConfig.setVaultNeverExpire(dialogId, !HundredGramConfig.isVaultNeverExpire(dialogId));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_RESET:
                    showResetConfirmDialog();
                    break;
            }
        });
    }

    private void notifyChanged() {
        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (onDismissCallback != null) {
            onDismissCallback.run();
        }
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString("HundredGramChatSettingsTitle", R.string.HundredGramChatSettingsTitle);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String chatName;
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            chatName = user != null ? ContactsController.formatName(user) : "User " + dialogId;
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            chatName = chat != null ? chat.title : "Chat " + (-dialogId);
        }

        items.add(UItem.asHeader(chatName));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramChatSettingsSub", R.string.HundredGramChatSettingsSub)));

        // 1. Ghost Mode & Privacy
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatOverrideSectionGhost", R.string.HundredGramChatOverrideSectionGhost)));
        items.add(UItem.asButton(ITEM_GHOST_READ, R.drawable.msg_views, LocaleController.getString("HundredGramChatOverrideGhostRead", R.string.HundredGramChatOverrideGhostRead), getThreeStateSubtitle("ghost_read", HundredGramConfig.ghostReadMessages)));
        items.add(UItem.asButton(ITEM_GHOST_TYPING, R.drawable.msg_edit, LocaleController.getString("HundredGramChatOverrideGhostTyping", R.string.HundredGramChatOverrideGhostTyping), getThreeStateSubtitle("ghost_typing", HundredGramConfig.ghostTyping)));
        if (dialogId > 0) {
            items.add(UItem.asButton(ITEM_HIDE_PHONE, R.drawable.msg_calls, LocaleController.getString("HundredGramChatOverrideHidePhone", R.string.HundredGramChatOverrideHidePhone), getThreeStateSubtitle("hide_phone", HundredGramConfig.hideOtherPhoneNumbers)));
        }
        items.add(UItem.asButton(ITEM_ANTI_DELETE, R.drawable.msg_delete, LocaleController.getString("HundredGramChatOverrideAntiDelete", R.string.HundredGramChatOverrideAntiDelete), getThreeStateSubtitle("anti_delete", true)));
        items.add(UItem.asShadow(null));

        // 2. Messaging & Notifications
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatOverrideSectionMessaging", R.string.HundredGramChatOverrideSectionMessaging)));
        boolean isMuted = MessagesController.getInstance(currentAccount).isDialogMuted(dialogId, 0);
        items.add(UItem.asCheck(ITEM_MUTE, LocaleController.getString("HundredGramChatOverrideMute", R.string.HundredGramChatOverrideMute)).setChecked(isMuted));
        items.add(UItem.asButton(ITEM_SILENT_SEND, R.drawable.input_notify_off, LocaleController.getString("HundredGramChatOverrideSilentSend", R.string.HundredGramChatOverrideSilentSend), getThreeStateSubtitle("silent_send", HundredGramConfig.silentSendByDefault)));
        items.add(UItem.asButton(ITEM_AUTO_TRANSLATE, R.drawable.msg_translate, LocaleController.getString("HundredGramChatOverrideAutoTranslate", R.string.HundredGramChatOverrideAutoTranslate), getThreeStateSubtitle("auto_translate", HundredGramConfig.unlockChatTranslation)));
        items.add(UItem.asShadow(null));

        // 3. Media Quality
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatOverrideSectionMedia", R.string.HundredGramChatOverrideSectionMedia)));
        items.add(UItem.asButton(ITEM_PHOTO_QUALITY, R.drawable.msg_photos, LocaleController.getString("HundredGramChatOverridePhotoQuality", R.string.HundredGramChatOverridePhotoQuality), getPhotoQualitySubtitle()));
        items.add(UItem.asButton(ITEM_VIDEO_AS_DOC, R.drawable.msg_video, LocaleController.getString("HundredGramChatOverrideVideoDoc", R.string.HundredGramChatOverrideVideoDoc), getThreeStateSubtitle("video_as_doc", HundredGramConfig.sendVideoAsDocument)));
        items.add(UItem.asShadow(null));

        // 4. Vault & Download Policy
        items.add(UItem.asHeader(LocaleController.getString("HundredGramChatOverrideSectionVault", R.string.HundredGramChatOverrideSectionVault)));
        items.add(UItem.asButton(ITEM_AUTO_ARCHIVE, R.drawable.msg_archive, LocaleController.getString("HundredGramChatOverrideAutoArchive", R.string.HundredGramChatOverrideAutoArchive), getThreeStateSubtitle("auto_archive", HundredGramConfig.backgroundArchiverEnabled)));

        int vaultMode = HundredGramConfig.getChatVaultMode(dialogId);
        items.add(UItem.asButton(ITEM_VAULT_MODE, R.drawable.msg_download_settings, LocaleController.getString("HundredGramChatVaultMode", R.string.HundredGramChatVaultMode), getVaultModeSubtitle()));

        if (vaultMode != HundredGramConfig.VAULT_MODE_NEVER) {
            items.add(UItem.asCheck(ITEM_VAULT_PHOTO, LocaleController.getString("HundredGramVaultPhotos", R.string.HundredGramVaultPhotos))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO)));
            items.add(UItem.asCheck(ITEM_VAULT_VIDEO, LocaleController.getString("HundredGramVaultVideos", R.string.HundredGramVaultVideos))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO)));
            items.add(UItem.asCheck(ITEM_VAULT_VOICE, LocaleController.getString("HundredGramVaultVoicesRounds", R.string.HundredGramVaultVoicesRounds))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND)));
            items.add(UItem.asCheck(ITEM_VAULT_DOC, LocaleController.getString("HundredGramVaultDocuments", R.string.HundredGramVaultDocuments))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT)));

            items.add(UItem.asButton(ITEM_VAULT_MAX_SIZE, R.drawable.msg_download, LocaleController.getString("HundredGramChatVaultMaxSize", R.string.HundredGramChatVaultMaxSize), getVaultMaxSizeSubtitle()));
        }

        items.add(UItem.asButton(ITEM_CACHE_LIMIT, R.drawable.msg_fave, LocaleController.getString("HundredGramChatOverrideCacheLimit", R.string.HundredGramChatOverrideCacheLimit), getCacheLimitSubtitle()));
        items.add(UItem.asButton(ITEM_CLEAR_CACHE, R.drawable.msg_clear, LocaleController.getString(R.string.ClearChatCache), ""));
        items.add(UItem.asCheck(ITEM_VAULT_NEVER_EXPIRE, LocaleController.getString("VaultNeverExpireChat", R.string.VaultNeverExpireChat))
                .setChecked(HundredGramConfig.isVaultNeverExpire(dialogId)));
        items.add(UItem.asShadow(LocaleController.getString("VaultNeverExpireChatInfo", R.string.VaultNeverExpireChatInfo)));

        // 5. Reset to defaults
        if (HundredGramConfig.hasChatOverrides(dialogId) || HundredGramConfig.hasChatVaultException(dialogId)) {
            items.add(UItem.asButton(ITEM_RESET, R.drawable.msg_delete, LocaleController.getString("HundredGramChatOverrideReset", R.string.HundredGramChatOverrideReset), ""));
            items.add(UItem.asShadow(null));
        }
    }

    private String getThreeStateSubtitle(String key, boolean globalValue) {
        int val = HundredGramConfig.getChatOverride(dialogId, key);
        if (val == HundredGramConfig.OVERRIDE_ENABLED) {
            return LocaleController.getString("HundredGramChatOverrideEnabled", R.string.HundredGramChatOverrideEnabled);
        } else if (val == HundredGramConfig.OVERRIDE_DISABLED) {
            return LocaleController.getString("HundredGramChatOverrideDisabled", R.string.HundredGramChatOverrideDisabled);
        } else {
            return LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + LocaleController.getString(globalValue ? "HundredGramChatOverrideEnabled" : "HundredGramChatOverrideDisabled", globalValue ? R.string.HundredGramChatOverrideEnabled : R.string.HundredGramChatOverrideDisabled) + ")";
        }
    }

    private String getPhotoQualitySubtitle() {
        int res = HundredGramConfig.getChatPhotoResolution(dialogId);
        if (res <= 0) {
            return LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + getGlobalPhotoResString() + ")";
        } else if (res <= 1280) {
            return LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280);
        } else if (res <= 2560) {
            return LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560);
        } else {
            return LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840);
        }
    }

    private String getGlobalPhotoResString() {
        int res = HundredGramConfig.photoMaxResolution;
        if (res <= 1280) {
            return LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280);
        } else if (res <= 2560) {
            return LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560);
        } else {
            return LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840);
        }
    }

    private String getVaultModeSubtitle() {
        int mode = HundredGramConfig.getChatVaultMode(dialogId);
        if (mode == HundredGramConfig.VAULT_MODE_ALWAYS) {
            return LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways);
        } else if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
            return LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever);
        } else {
            return LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + LocaleController.getString(HundredGramConfig.autoDownloadMediaVault ? "HundredGramChatOverrideEnabled" : "HundredGramChatOverrideDisabled", HundredGramConfig.autoDownloadMediaVault ? R.string.HundredGramChatOverrideEnabled : R.string.HundredGramChatOverrideDisabled) + ")";
        }
    }

    private String getVaultMaxSizeSubtitle() {
        int customMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
        if (customMb == 0) {
            return String.format(LocaleController.getString("HundredGramVaultSizeDefault", R.string.HundredGramVaultSizeDefault), HundredGramConfig.autoDownloadMaxSizeMb);
        } else if (customMb == -1) {
            return LocaleController.getString("HundredGramVaultSizeUnlimited", R.string.HundredGramVaultSizeUnlimited);
        } else {
            return customMb + " МБ";
        }
    }

    private String getCacheLimitSubtitle() {
        int limitMb = HundredGramConfig.getChatCacheLimitMb(dialogId);
        if (limitMb <= 0) {
            return LocaleController.getString("HundredGramChatOverrideNoLimit", R.string.HundredGramChatOverrideNoLimit);
        } else if (limitMb >= 1024) {
            return (limitMb / 1024) + " ГБ";
        } else {
            return limitMb + " МБ";
        }
    }

    private void showThreeStateDialog(int titleRes, String key, boolean globalValue, Runnable onDone) {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(titleRes));

        CharSequence[] items = new CharSequence[] {
                LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + LocaleController.getString(globalValue ? "HundredGramChatOverrideEnabled" : "HundredGramChatOverrideDisabled", globalValue ? R.string.HundredGramChatOverrideEnabled : R.string.HundredGramChatOverrideDisabled) + ")",
                LocaleController.getString("HundredGramChatOverrideEnabled", R.string.HundredGramChatOverrideEnabled),
                LocaleController.getString("HundredGramChatOverrideDisabled", R.string.HundredGramChatOverrideDisabled)
        };

        builder.setItems(items, (dialog, which) -> {
            int newVal = which == 1 ? HundredGramConfig.OVERRIDE_ENABLED : (which == 2 ? HundredGramConfig.OVERRIDE_DISABLED : HundredGramConfig.OVERRIDE_DEFAULT);
            HundredGramConfig.setChatOverride(dialogId, key, newVal);
            if (adapter != null) adapter.update(true);
            if (onDone != null) onDone.run();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showPhotoQualityDialog() {
        Context context = getContext();
        if (context == null) return;
        final int[] values = { 0, 1280, 2560, 3840 };
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + getGlobalPhotoResString() + ")",
                LocaleController.getString("PhotoMaxRes1280", R.string.PhotoMaxRes1280),
                LocaleController.getString("PhotoMaxRes2560", R.string.PhotoMaxRes2560),
                LocaleController.getString("PhotoMaxRes3840", R.string.PhotoMaxRes3840)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramChatOverridePhotoQuality", R.string.HundredGramChatOverridePhotoQuality));
        builder.setItems(items, (dialog, which) -> {
            HundredGramConfig.setChatPhotoResolution(dialogId, values[which]);
            if (adapter != null) adapter.update(true);
            notifyChanged();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showCacheLimitDialog() {
        Context context = getContext();
        if (context == null) return;
        final int[] values = { 0, 100, 500, 1024, 2048, 5120 };
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString("HundredGramChatOverrideNoLimit", R.string.HundredGramChatOverrideNoLimit),
                "100 МБ",
                "500 МБ",
                "1 ГБ",
                "2 ГБ",
                "5 ГБ"
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramChatOverrideCacheLimit", R.string.HundredGramChatOverrideCacheLimit));
        builder.setItems(items, (dialog, which) -> {
            HundredGramConfig.setChatCacheLimitMb(dialogId, values[which]);
            if (adapter != null) adapter.update(true);
            notifyChanged();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showVaultModeDialog() {
        Context context = getContext();
        if (context == null) return;
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString("HundredGramChatOverrideDefault", R.string.HundredGramChatOverrideDefault) + " (" + LocaleController.getString(HundredGramConfig.autoDownloadMediaVault ? "HundredGramChatOverrideEnabled" : "HundredGramChatOverrideDisabled", HundredGramConfig.autoDownloadMediaVault ? R.string.HundredGramChatOverrideEnabled : R.string.HundredGramChatOverrideDisabled) + ")",
                LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways),
                LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramChatVaultMode", R.string.HundredGramChatVaultMode));
        builder.setItems(items, (dialog, which) -> {
            int newMode = which == 1 ? HundredGramConfig.VAULT_MODE_ALWAYS : (which == 2 ? HundredGramConfig.VAULT_MODE_NEVER : HundredGramConfig.VAULT_MODE_DEFAULT);
            HundredGramConfig.setChatVaultMode(dialogId, newMode);
            if (adapter != null) adapter.update(true);
            notifyChanged();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showVaultMaxSizeDialog() {
        Context context = getContext();
        if (context == null) return;
        final int[] values = { 0, 10, 50, 100, 500, 1024, -1 };
        CharSequence[] items = new CharSequence[] {
                String.format(LocaleController.getString("HundredGramVaultSizeDefault", R.string.HundredGramVaultSizeDefault), HundredGramConfig.autoDownloadMaxSizeMb),
                "10 МБ",
                "50 МБ",
                "100 МБ",
                "500 МБ",
                "1 ГБ",
                LocaleController.getString("HundredGramVaultSizeUnlimited", R.string.HundredGramVaultSizeUnlimited)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramChatVaultMaxSize", R.string.HundredGramChatVaultMaxSize));
        builder.setItems(items, (dialog, which) -> {
            HundredGramConfig.setChatVaultMaxSizeMb(dialogId, values[which]);
            if (adapter != null) adapter.update(true);
            notifyChanged();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private void showClearCacheConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.ClearChatCache));
        builder.setMessage(LocaleController.getString(R.string.HundredGramVaultClearChatConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
            FileLoader.getInstance(currentAccount).cancelLoadAllFiles();
            FileLoader.getInstance(currentAccount).getFileDatabase().clearDialogFiles(dialogId, freedBytes -> {
                String sizeStr = AndroidUtilities.formatFileSize(freedBytes);
                if (BulletinFactory.canShowBulletin(fragment)) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_delete, LocaleController.formatString("ClearChatCacheDone", R.string.ClearChatCacheDone, sizeStr)).show();
                }
            });
            dismiss();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }

    private void showResetConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramChatOverrideReset", R.string.HundredGramChatOverrideReset));
        builder.setMessage(LocaleController.getString("HundredGramChatOverrideResetConfirm", R.string.HundredGramChatOverrideResetConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            HundredGramConfig.clearAllChatOverrides(dialogId);
            HundredGramConfig.clearChatVaultSettings(dialogId);
            if (adapter != null) adapter.update(true);
            notifyChanged();
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramChatOverrideResetDone", R.string.HundredGramChatOverrideResetDone)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }
}
