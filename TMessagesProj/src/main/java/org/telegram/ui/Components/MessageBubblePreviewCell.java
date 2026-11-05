package org.telegram.ui.Components;

import android.content.Context;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

/** Settings preview rendered by the same message cells as a chat. */
public class MessageBubblePreviewCell extends LinearLayout {
    private final ChatMessageCell[] cells = new ChatMessageCell[2];
    private final MessageObject[] messages = new MessageObject[2];

    public MessageBubblePreviewCell(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        int account = UserConfig.selectedAccount;
        for (int i = 0; i < cells.length; i++) {
            boolean outgoing = i == 1;
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.message = LocaleController.getString(outgoing ? R.string.BubblePreviewOutText : R.string.BubblePreviewInText);
            message.date = (int) (System.currentTimeMillis() / 1000) - 60;
            message.dialog_id = 1;
            message.flags = 257;
            message.id = i + 1;
            message.media = new TLRPC.TL_messageMediaEmpty();
            message.out = outgoing;
            message.peer_id = new TLRPC.TL_peerUser();
            message.from_id = new TLRPC.TL_peerUser();
            if (outgoing) {
                message.from_id.user_id = UserConfig.getInstance(account).getClientUserId();
                message.edit_date = message.date;
                message.flags |= 32768;
            } else {
                message.flags |= TLRPC.MESSAGE_FLAG_HAS_VIEWS;
                message.views = 42;
                message.forwards = 14;
                message.peer_id.user_id = UserConfig.getInstance(account).getClientUserId();
            }
            messages[i] = new MessageObject(account, message, true, false);
            messages[i].eventId = 1;
            ChatMessageCell cell = new ChatMessageCell(context, account, false, null, null);
            cell.isChat = false;
            cell.setFullyDraw(true);
            cells[i] = cell;
            addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        update();
    }

    public void update() {
        setBackgroundColor(Theme.getColor(Theme.key_chat_wallpaper));
        for (int i = 0; i < cells.length; i++) {
            messages[i].forceUpdate = true;
            cells[i].setMessageObject(messages[i], null, false, false, false);
            cells[i].requestLayout();
            cells[i].invalidate();
        }
    }
}
