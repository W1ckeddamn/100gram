package org.telegram.ui.Components;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class HundredGramChatVaultBottomSheet extends BottomSheetWithRecyclerListView {

    private static final int ITEM_MODE_DEFAULT = 1;
    private static final int ITEM_MODE_ALWAYS = 2;
    private static final int ITEM_MODE_NEVER = 3;

    private static final int ITEM_TYPE_PHOTO = 10;
    private static final int ITEM_TYPE_VIDEO = 11;
    private static final int ITEM_TYPE_VOICE_ROUND = 12;
    private static final int ITEM_TYPE_DOCUMENT = 13;

    private static final int ITEM_MAX_SIZE = 20;
    private static final int ITEM_CLEAR_VAULT = 30;
    private static final int ITEM_RESET = 99;

    private final long dialogId;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;
    private final Runnable onDismissCallback;

    public HundredGramChatVaultBottomSheet(BaseFragment fragment, long dialogId) {
        this(fragment, dialogId, null);
    }

    public HundredGramChatVaultBottomSheet(BaseFragment fragment, long dialogId, Runnable onDismissCallback) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.dialogId = dialogId;
        this.onDismissCallback = onDismissCallback;

        topPadding = 0.3f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);

        recyclerListView.setOnItemClickListener((view, position, x, y) -> {
            if (adapter == null) return;
            UItem item = adapter.getItem(position - 1);
            if (item == null) return;

            switch (item.id) {
                case ITEM_MODE_DEFAULT:
                    HundredGramConfig.setChatVaultMode(dialogId, HundredGramConfig.VAULT_MODE_DEFAULT);
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_MODE_ALWAYS:
                    HundredGramConfig.setChatVaultMode(dialogId, HundredGramConfig.VAULT_MODE_ALWAYS);
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_MODE_NEVER:
                    HundredGramConfig.setChatVaultMode(dialogId, HundredGramConfig.VAULT_MODE_NEVER);
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_TYPE_PHOTO:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_TYPE_VIDEO:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_TYPE_VOICE_ROUND:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_TYPE_DOCUMENT:
                    HundredGramConfig.setChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT,
                            !HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT));
                    adapter.update(true);
                    notifyChanged();
                    break;
                case ITEM_MAX_SIZE:
                    showMaxSizeDialog();
                    break;
                case ITEM_CLEAR_VAULT:
                    showClearVaultConfirmDialog();
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
        return LocaleController.getString("HundredGramChatVaultTitle", R.string.HundredGramChatVaultTitle);
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
        items.add(UItem.asShadow(LocaleController.getString("HundredGramChatVaultTitle", R.string.HundredGramChatVaultTitle)));

        // Mode section
        items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultModeHeader", R.string.HundredGramVaultModeHeader)));
        int mode = HundredGramConfig.getChatVaultMode(dialogId);
        items.add(UItem.asRadio(ITEM_MODE_DEFAULT, LocaleController.getString("HundredGramVaultModeDefault", R.string.HundredGramVaultModeDefault))
                .setChecked(mode == HundredGramConfig.VAULT_MODE_DEFAULT));
        items.add(UItem.asRadio(ITEM_MODE_ALWAYS, LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways))
                .setChecked(mode == HundredGramConfig.VAULT_MODE_ALWAYS));
        items.add(UItem.asRadio(ITEM_MODE_NEVER, LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever))
                .setChecked(mode == HundredGramConfig.VAULT_MODE_NEVER));
        items.add(UItem.asShadow(null));

        if (mode != HundredGramConfig.VAULT_MODE_NEVER) {
            // Media types section
            items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultTypesHeader", R.string.HundredGramVaultTypesHeader)));
            items.add(UItem.asCheck(ITEM_TYPE_PHOTO, LocaleController.getString("HundredGramVaultPhotos", R.string.HundredGramVaultPhotos))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO)));
            items.add(UItem.asCheck(ITEM_TYPE_VIDEO, LocaleController.getString("HundredGramVaultVideos", R.string.HundredGramVaultVideos))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO)));
            items.add(UItem.asCheck(ITEM_TYPE_VOICE_ROUND, LocaleController.getString("HundredGramVaultVoicesRounds", R.string.HundredGramVaultVoicesRounds))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND)));
            items.add(UItem.asCheck(ITEM_TYPE_DOCUMENT, LocaleController.getString("HundredGramVaultDocuments", R.string.HundredGramVaultDocuments))
                    .setChecked(HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT)));
            items.add(UItem.asShadow(null));

            // Max size section
            items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultLimitHeader", R.string.HundredGramVaultLimitHeader)));
            int customMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
            String sizeStr;
            if (customMb == 0) {
                sizeStr = String.format(LocaleController.getString("HundredGramVaultSizeDefault", R.string.HundredGramVaultSizeDefault), HundredGramConfig.autoDownloadMaxSizeMb);
            } else if (customMb == -1) {
                sizeStr = LocaleController.getString("HundredGramVaultSizeUnlimited", R.string.HundredGramVaultSizeUnlimited);
            } else {
                sizeStr = customMb + " МБ";
            }
            items.add(UItem.asButton(ITEM_MAX_SIZE, R.drawable.msg_download, LocaleController.getString("HundredGramMediaVaultLimit", R.string.HundredGramMediaVaultLimit), sizeStr));
            items.add(UItem.asShadow(null));
        }

        items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultClearHeader", R.string.HundredGramVaultClearHeader)));
        items.add(UItem.asButton(ITEM_CLEAR_VAULT, R.drawable.msg_clear, LocaleController.getString("HundredGramVaultClearChat", R.string.HundredGramVaultClearChat), ""));
        items.add(UItem.asShadow(LocaleController.getString("HundredGramVaultClearChatInfo", R.string.HundredGramVaultClearChatInfo)));

        if (HundredGramConfig.hasChatVaultException(dialogId)) {
            items.add(UItem.asButton(ITEM_RESET, R.drawable.msg_delete, LocaleController.getString("HundredGramVaultResetDefault", R.string.HundredGramVaultResetDefault), ""));
            items.add(UItem.asShadow(null));
        }
    }

    private void showMaxSizeDialog() {
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
        int currentMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
        int selected = 0;
        for (int a = 0; a < values.length; a++) {
            if (values[a] == currentMb) {
                selected = a;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramMediaVaultLimit", R.string.HundredGramMediaVaultLimit));
        builder.setItems(items, (dialog, which) -> {
            HundredGramConfig.setChatVaultMaxSizeMb(dialogId, values[which]);
            if (adapter != null) {
                adapter.update(true);
            }
            notifyChanged();
            dialog.dismiss();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void showResetConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramVaultResetDefault", R.string.HundredGramVaultResetDefault));
        builder.setMessage(LocaleController.getString("HundredGramVaultResetConfirm", R.string.HundredGramVaultResetConfirm));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialog, which) -> {
            HundredGramConfig.clearChatVaultSettings(dialogId);
            if (adapter != null) {
                adapter.update(true);
            }
            notifyChanged();
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramVaultSaved", R.string.HundredGramVaultSaved)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void showClearVaultConfirmDialog() {
        Context context = getContext();
        if (context == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("HundredGramVaultClearChat", R.string.HundredGramVaultClearChat));
        builder.setMessage(LocaleController.getString("HundredGramVaultClearChatConfirm", R.string.HundredGramVaultClearChatConfirm));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> {
            FileLoader.getInstance(currentAccount).cancelLoadAllFiles();
            FileLoader.getInstance(currentAccount).getFileDatabase().clearDialogFiles(dialogId, freedBytes -> {
                String sizeStr = AndroidUtilities.formatFileSize(freedBytes);
                if (BulletinFactory.canShowBulletin(fragment)) {
                    BulletinFactory.of(fragment).createSimpleBulletin(R.raw.ic_delete, LocaleController.formatString("HundredGramVaultClearedChat", R.string.HundredGramVaultClearedChat, sizeStr)).show();
                }
            });
            dismiss();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.redPositive();
    }
}
