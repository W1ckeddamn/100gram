package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.HundredGramChatVaultBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HundredGramVaultExceptionsActivity extends BaseFragment {

    private static final int ITEM_ADD_EXCEPTION = 1;

    private UniversalRecyclerView listView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("HundredGramVaultExceptions", R.string.HundredGramVaultExceptions));
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

        listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        listView.setSections();
        listView.adapter.setApplyBackground(false);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(ITEM_ADD_EXCEPTION, R.drawable.msg_addcontact,
                LocaleController.getString("HundredGramVaultAddException", R.string.HundredGramVaultAddException)));
        items.add(UItem.asShadow(null));

        ArrayList<Long> exceptions = HundredGramConfig.getChatVaultExceptions();
        if (exceptions.isEmpty()) {
            items.add(UItem.asShadow(LocaleController.getString("HundredGramVaultExceptionsEmpty", R.string.HundredGramVaultExceptionsEmpty)));
            return;
        }

        items.add(UItem.asHeader(LocaleController.getString("HundredGramVaultExceptions", R.string.HundredGramVaultExceptions)));

        for (int i = 0; i < exceptions.size(); i++) {
            long did = exceptions.get(i);
            String name;
            int icon;
            if (did > 0) {
                TLRPC.User user = getMessagesController().getUser(did);
                name = user != null ? ContactsController.formatName(user) : "User " + did;
                icon = R.drawable.msg_usersearch;
            } else {
                TLRPC.Chat chat = getMessagesController().getChat(-did);
                name = chat != null ? chat.title : "Chat " + (-did);
                icon = R.drawable.msg_groups;
            }

            int mode = HundredGramConfig.getChatVaultMode(did);
            String modeStr;
            if (mode == HundredGramConfig.VAULT_MODE_ALWAYS) {
                modeStr = LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways);
            } else if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
                modeStr = LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever);
            } else {
                modeStr = LocaleController.getString("HundredGramVaultTypesHeader", R.string.HundredGramVaultTypesHeader);
            }

            items.add(UItem.asButton(100 + i, icon, name, modeStr));
        }

        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ITEM_ADD_EXCEPTION) {
            Bundle args = new Bundle();
            args.putBoolean("onlySelect", true);
            args.putBoolean("checkCanWrite", false);
            args.putBoolean("allowMultiple", true);
            DialogsActivity activity = new DialogsActivity(args);
            activity.setDelegate((fragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                if (dids != null && !dids.isEmpty()) {
                    fragment.finishFragment();
                    if (dids.size() == 1) {
                        long did = dids.get(0).dialogId;
                        new HundredGramChatVaultBottomSheet(this, did, () -> {
                            if (listView != null && listView.adapter != null) {
                                listView.adapter.update(true);
                            }
                        }).show();
                    } else {
                        showBatchVaultModeDialog(dids);
                    }
                }
                return true;
            });
            presentFragment(activity);
        } else if (item.id >= 100) {
            int index = item.id - 100;
            ArrayList<Long> exceptions = HundredGramConfig.getChatVaultExceptions();
            if (index >= 0 && index < exceptions.size()) {
                long did = exceptions.get(index);
                new HundredGramChatVaultBottomSheet(this, did, () -> {
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                }).show();
            }
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            ArrayList<Long> exceptions = HundredGramConfig.getChatVaultExceptions();
            if (index >= 0 && index < exceptions.size()) {
                long did = exceptions.get(index);
                showDeleteConfirmDialog(did);
                return true;
            }
        }
        return false;
    }

    private void showDeleteConfirmDialog(long did) {
        Context context = getContext();
        if (context == null) return;

        String name;
        if (did > 0) {
            TLRPC.User user = getMessagesController().getUser(did);
            name = user != null ? ContactsController.formatName(user) : "User " + did;
        } else {
            TLRPC.Chat chat = getMessagesController().getChat(-did);
            name = chat != null ? chat.title : "Chat " + (-did);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LocaleController.getString("HundredGramVaultResetDefault", R.string.HundredGramVaultResetDefault));
        builder.setMessage(LocaleController.formatString("HundredGramVaultDeleteConfirm", R.string.HundredGramVaultDeleteConfirm, name));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            HundredGramConfig.clearChatVaultSettings(did);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (BulletinFactory.canShowBulletin(this)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString("HundredGramVaultDeleted", R.string.HundredGramVaultDeleted)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    private void showBatchVaultModeDialog(ArrayList<MessagesStorage.TopicKey> dids) {
        if (getParentActivity() == null || dids == null || dids.isEmpty()) return;
        String[] options = new String[] {
            LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever),
            LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("HundredGramVaultModeHeader", R.string.HundredGramVaultModeHeader));
        builder.setItems(options, (dialog, which) -> {
            int mode = (which == 0) ? HundredGramConfig.VAULT_MODE_NEVER : HundredGramConfig.VAULT_MODE_ALWAYS;
            for (MessagesStorage.TopicKey key : dids) {
                HundredGramConfig.setChatVaultMode(key.dialogId, mode);
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (BulletinFactory.canShowBulletin(this)) {
                String modeName = (which == 0)
                        ? LocaleController.getString("HundredGramVaultModeNever", R.string.HundredGramVaultModeNever)
                        : LocaleController.getString("HundredGramVaultModeAlways", R.string.HundredGramVaultModeAlways);
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        String.format(LocaleController.getString("HundredGramVaultExceptionsAddedCount", R.string.HundredGramVaultExceptionsAddedCount), dids.size(), modeName)
                ).show();
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
