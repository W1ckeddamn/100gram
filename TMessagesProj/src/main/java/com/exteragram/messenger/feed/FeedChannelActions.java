package com.exteragram.messenger.feed;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.ItemOptions;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class FeedChannelActions {

    public static boolean canLeave(TLRPC.Chat chat) {
        return chat != null && !chat.creator && !ChatObject.isNotInChat(chat);
    }

    public static void showAvatarMenu(final ChatActivity chatActivity, ChatMessageCell chatMessageCell, final TLRPC.Chat chat, Runnable runnable, final Runnable runnable2, final Consumer<ArrayList<Integer>> consumer) {
        if (chatActivity == null || chatMessageCell == null || chat == null) {
            return;
        }
        ItemOptions makeOptions = ItemOptions.makeOptions(chatActivity, chatMessageCell);
        boolean z = chat.broadcast;
        makeOptions.add(z ? R.drawable.msg_channel : R.drawable.msg_discussion, LocaleController.getString(z ? R.string.OpenChannel2 : R.string.OpenGroup2), runnable)
                .add(R.drawable.msg_cancel, LocaleController.getString(R.string.FeedHideChannel), () -> {
                    FeedConfig.getInstance(chatActivity.getCurrentAccount()).setExcluded(-chat.id, true);
                })
                .addIf(canLeave(chat), R.drawable.msg_leave, LocaleController.getString(chat.broadcast ? R.string.LeaveChannelMenu : R.string.LeaveMegaMenu), true, () -> {
                    leaveChannel(chatActivity, chat, runnable2, consumer);
                })
                .setDrawScrim(false)
                .setGravity(3)
                .forceBottom(true)
                .show();
    }

    public static void leaveChannel(final BaseFragment baseFragment, final TLRPC.Chat chat, final Runnable runnable, final Consumer<ArrayList<Integer>> consumer) {
        if (baseFragment == null || chat == null || baseFragment.getParentActivity() == null) {
            return;
        }
        AlertsCreator.createClearOrDeleteDialogAlert(baseFragment, false, chat, null, false, true, false, false, new MessagesStorage.BooleanCallback() {
            @Override
            public void run(boolean z) {
                long dialogId = -chat.id;
                if (ChatObject.isNotInChat(chat)) {
                    baseFragment.getMessagesController().deleteDialog(dialogId, 0, z);
                } else {
                    baseFragment.getMessagesController().deleteParticipantFromChat(chat.id, baseFragment.getMessagesController().getUser(baseFragment.getUserConfig().getClientUserId()), null, z, z);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }
}