package org.telegram.ui.Components;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;

import java.util.ArrayList;

/** Media size controls with examples rendered by the chat itself. */
public class StickerSizePreviewCell extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {
    private final int account = UserConfig.selectedAccount;
    private final LinearLayout preview;
    private final LinearLayout[] chips = new LinearLayout[2];
    private final TextView[] values = new TextView[2];
    private final ChatMessageCell[] cells = new ChatMessageCell[2];
    private final MessageObject[] messages = new MessageObject[2];
    private final TextView[] empty = new TextView[2];
    private final long[] documentIds = new long[2];
    private boolean attached;

    public interface OnStickerSizeChangedListener { void onSizeChanged(int size); }
    public interface OnMediaSizeChangedListener { void onSizeChanged(int type, int size); }
    private OnStickerSizeChangedListener listener;
    private OnMediaSizeChangedListener mediaListener;
    public void setOnStickerSizeChangedListener(OnStickerSizeChangedListener listener) { this.listener = listener; }
    public void setOnMediaSizeChangedListener(OnMediaSizeChangedListener listener) { this.mediaListener = listener; }

    public StickerSizePreviewCell(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        preview = new LinearLayout(context);
        preview.setOrientation(VERTICAL);
        preview.setBackgroundColor(Theme.getColor(Theme.key_chat_wallpaper));
        preview.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 14, 10, 14, 4));
        for (int i = 0; i < 2; i++) {
            cells[i] = new ChatMessageCell(context, account, false, null, null);
            cells[i].isChat = false;
            cells[i].setFullyDraw(true);
            preview.addView(cells[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            empty[i] = new TextView(context);
            empty[i].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            empty[i].setTextSize(14);
            empty[i].setGravity(Gravity.CENTER);
            empty[i].setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18));
            empty[i].setText(LocaleController.getString(i == 0 ? R.string.MediaPreviewNoSticker : R.string.MediaPreviewNoGif));
            preview.addView(empty[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(VERTICAL);
        controls.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        addView(controls, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        for (int i = 0; i < 2; i++) {
            LinearLayout header = new LinearLayout(context);
            header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(context);
            title.setText(LocaleController.getString(i == 0 ? R.string.StickerSize : R.string.GifSize));
            title.setTextSize(14);
            title.setTypeface(AndroidUtilities.bold());
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            header.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
            values[i] = new TextView(context);
            values[i].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
            header.addView(values[i], LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            controls.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 6));
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setHorizontalScrollBarEnabled(false);
            chips[i] = new LinearLayout(context);
            scroll.addView(chips[i]);
            controls.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        update();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        NotificationCenter.getInstance(account).addObserver(this, NotificationCenter.recentDocumentsDidLoad);
        MediaDataController controller = MediaDataController.getInstance(account);
        controller.loadRecents(MediaDataController.TYPE_IMAGE, false, true, false);
        controller.loadRecents(MediaDataController.TYPE_IMAGE, true, true, false);
        update();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        NotificationCenter.getInstance(account).removeObserver(this, NotificationCenter.recentDocumentsDidLoad);
        super.onDetachedFromWindow();
    }

    @Override public void didReceivedNotification(int id, int currentAccount, Object... args) {
        if (attached && id == NotificationCenter.recentDocumentsDidLoad) update();
    }

    private MessageObject createMessage(TLRPC.Document document, boolean outgoing) {
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = outgoing ? 2 : 1;
        message.dialog_id = 1;
        message.date = (int) (System.currentTimeMillis() / 1000) - 60;
        message.flags = 257 | 512;
        message.message = "";
        message.out = outgoing;
        message.peer_id = new TLRPC.TL_peerUser();
        message.from_id = new TLRPC.TL_peerUser();
        if (outgoing) message.from_id.user_id = UserConfig.getInstance(account).getClientUserId();
        else message.peer_id.user_id = UserConfig.getInstance(account).getClientUserId();
        TLRPC.TL_messageMediaDocument media = new TLRPC.TL_messageMediaDocument();
        media.flags = 1;
        media.document = document;
        message.media = media;
        MessageObject result = new MessageObject(account, message, true, false);
        result.eventId = 1;
        return result;
    }

    public void update() {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        preview.setBackgroundColor(Theme.getColor(Theme.key_chat_wallpaper));
        MediaDataController controller = MediaDataController.getInstance(account);
        ArrayList<TLRPC.Document> stickers = controller.getRecentStickersNoCopy(MediaDataController.TYPE_IMAGE);
        ArrayList<TLRPC.Document> gifs = controller.getRecentGifs();
        for (int i = 0; i < 2; i++) {
            ArrayList<TLRPC.Document> list = i == 0 ? stickers : gifs;
            TLRPC.Document doc = list == null || list.isEmpty() ? null : list.get(0);
            cells[i].setVisibility(doc == null ? View.GONE : View.VISIBLE);
            empty[i].setVisibility(doc == null ? View.VISIBLE : View.GONE);
            if (doc != null) {
                if (messages[i] == null || documentIds[i] != doc.id) {
                    messages[i] = createMessage(doc, i == 1);
                    documentIds[i] = doc.id;
                }
                messages[i].forceUpdate = true;
                cells[i].setMessageObject(messages[i], null, false, false, false);
                cells[i].requestLayout();
                cells[i].invalidate();
            } else {
                messages[i] = null;
            }
            rebuildChips(i);
        }
    }

    private void rebuildChips(final int type) {
        chips[type].removeAllViews();
        int current = type == 0 ? HundredGramConfig.stickerSize : HundredGramConfig.gifSize;
        String[] labels = {"50%", "75%", "100%", "125%", "150%"};
        int index = Math.max(0, Math.min(labels.length - 1, current));
        values[type].setText(labels[index]);
        for (int i = 0; i < labels.length; i++) {
            final int value = i;
            boolean selected = value == current;
            TextView chip = new TextView(getContext());
            chip.setText(labels[i]);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
            chip.setTextColor(Theme.getColor(selected ? Theme.key_featuredStickers_buttonText : Theme.key_windowBackgroundWhiteBlackText));
            chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), Theme.getColor(selected ? Theme.key_featuredStickers_addButton : Theme.key_windowBackgroundGray)));
            chip.setOnClickListener(v -> {
                if (type == 0) {
                    HundredGramConfig.setStickerSize(value);
                    if (listener != null) listener.onSizeChanged(value);
                } else HundredGramConfig.setGifSize(value);
                if (mediaListener != null) mediaListener.onSizeChanged(type, value);
                update();
                HundredGramConfig.postNotificationToAllAccounts(NotificationCenter.updateInterfaces, org.telegram.messenger.MessagesController.UPDATE_MASK_ALL);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.rightMargin = AndroidUtilities.dp(6);
            chips[type].addView(chip, lp);
        }
    }
}
