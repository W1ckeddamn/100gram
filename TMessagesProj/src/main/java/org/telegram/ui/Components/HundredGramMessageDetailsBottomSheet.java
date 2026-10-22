package org.telegram.ui.Components;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.HundredGramUtils;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.util.ArrayList;

public class HundredGramMessageDetailsBottomSheet extends BottomSheetWithRecyclerListView {

    private final MessageObject messageObject;
    private final BaseFragment fragment;
    private UniversalAdapter adapter;

    public HundredGramMessageDetailsBottomSheet(BaseFragment fragment, MessageObject messageObject) {
        super(fragment.getContext(), fragment, false, false, false, true, ActionBarType.SLIDING, fragment.getResourceProvider());
        this.fragment = fragment;
        this.messageObject = messageObject;
        topPadding = 0.35f;
        fixNavigationBar();
        setBackgroundColor(Theme.getColor(Theme.key_dialogBackgroundGray, resourcesProvider));
        setSlidingActionBar();
        setShowHandle(true);
        if (adapter != null) {
            adapter.update(false);
        }
        recyclerListView.setOnItemClickListener((view, position, x, y) -> {
            if (adapter == null) return;
            UItem item = adapter.getItem(position - 1);
            if (item != null && (item.id == 5 || item.id == 17)) {
                dismiss();
                new HundredGramEditHistoryBottomSheet(fragment, messageObject).show();
                return;
            }
            if (item != null && !TextUtils.isEmpty(item.textValue)) {
                AndroidUtilities.addToClipboard(item.textValue);
                if (BulletinFactory.canShowBulletin(fragment)) {
                    BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString("TextCopied", R.string.TextCopied)).show();
                }
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return LocaleController.getString("Details", R.string.Details);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        items.add(UItem.asHeader(LocaleController.getString("Details", R.string.Details)));

        // Message ID
        items.add(UItem.asButton(1, R.drawable.msg_info, "Message ID", String.valueOf(messageObject.getId())));

        // Chat / Dialog ID
        long dialogId = messageObject.getDialogId();
        String chatName = null;
        if (dialogId < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
            if (chat != null) {
                chatName = chat.title;
            }
        }
        if (chatName != null) {
            items.add(UItem.asButton(2, R.drawable.msg_channel, "Chat", chatName + " (" + dialogId + ")"));
        } else {
            items.add(UItem.asButton(2, R.drawable.msg_channel, "Chat ID", String.valueOf(dialogId)));
        }

        // Sender / Author
        long senderId = messageObject.getFromChatId();
        if (senderId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(senderId);
            if (user != null) {
                String name = ContactsController.formatName(user);
                items.add(UItem.asButton(3, R.drawable.msg_usersearch, "From", name + " (" + user.id + ")"));
            } else {
                items.add(UItem.asButton(3, R.drawable.msg_usersearch, "From ID", String.valueOf(senderId)));
            }
        } else if (senderId < 0) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-senderId);
            if (chat != null) {
                items.add(UItem.asButton(3, R.drawable.msg_channel, "From", chat.title + " (" + chat.id + ")"));
            } else {
                items.add(UItem.asButton(3, R.drawable.msg_channel, "From ID", String.valueOf(senderId)));
            }
        }

        // Date
        if (messageObject.messageOwner.date > 0) {
            items.add(UItem.asButton(4, R.drawable.msg_calendar2, LocaleController.getString("Date", R.string.Date), LocaleController.formatDateTime(messageObject.messageOwner.date, false)));
        }

        // Exact Read Date (AyuGram Spy)
        if (messageObject.isOutOwner() && !messageObject.isUnread()) {
            int readDate = org.telegram.messenger.MessagesStorage.getInstance(currentAccount).getMessageReadDateSync(dialogId, messageObject.getId());
            if (readDate > 0) {
                String readText = LocaleController.formatDateTime(readDate, false);
                int delta = readDate - messageObject.messageOwner.date;
                if (delta > 0) {
                    readText += " (+" + formatDeltaTime(delta) + ")";
                }
                items.add(UItem.asButton(18, R.drawable.msg_views, LocaleController.getString("MessageReadDate", R.string.MessageReadDate), readText));
            }
        }

        // Edited Date
        if (messageObject.messageOwner.edit_date > 0 && messageObject.messageOwner.edit_date != messageObject.messageOwner.date && !messageObject.messageOwner.edit_hide) {
            items.add(UItem.asButton(5, R.drawable.msg_edit, LocaleController.getString("EditedDate", R.string.EditedDate), LocaleController.formatDateTime(messageObject.messageOwner.edit_date, false)));
        }

        // Edit History
        if (messageObject.isEdited() || messageObject.messageOwner.edit_date > 0 || messageObject.messageOwner.isDeleted) {
            items.add(UItem.asButton(17, R.drawable.msg_log, LocaleController.getString("MessageEditHistory", R.string.MessageEditHistory), LocaleController.getString("Open", R.string.Open)));
        }

        // Forward info
        if (messageObject.messageOwner.fwd_from != null) {
            if (messageObject.messageOwner.fwd_from.date > 0) {
                items.add(UItem.asButton(6, R.drawable.msg_forward, LocaleController.getString("ForwardedDate", R.string.ForwardedDate), LocaleController.formatDateTime(messageObject.messageOwner.fwd_from.date, false)));
            }
            if (messageObject.messageOwner.fwd_from.from_name != null) {
                items.add(UItem.asButton(7, R.drawable.msg_forward, "Forward Author", messageObject.messageOwner.fwd_from.from_name));
            }
        }

        // Views & Shares
        if (messageObject.messageOwner.views > 0) {
            items.add(UItem.asButton(8, R.drawable.msg_stats, "Views", String.valueOf(messageObject.messageOwner.views)));
        }
        if (messageObject.messageOwner.forwards > 0) {
            items.add(UItem.asButton(9, R.drawable.msg_shareout, "Shares", String.valueOf(messageObject.messageOwner.forwards)));
        }

        // Media Info
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media != null && !(media instanceof TLRPC.TL_messageMediaEmpty)) {
            items.add(UItem.asShadow(null));
            items.add(UItem.asHeader("Media"));

            // File Size
            long size = messageObject.getSize();
            if (size > 0) {
                items.add(UItem.asButton(10, R.drawable.msg_download, LocaleController.getString("FileSize", R.string.FileSize), AndroidUtilities.formatFileSize(size)));
            }

            // MIME Type
            String mime = messageObject.getMimeType();
            if (!TextUtils.isEmpty(mime)) {
                items.add(UItem.asButton(11, R.drawable.msg_link, LocaleController.getString("MimeType", R.string.MimeType), mime));
            }

            // Document file name
            if (messageObject.getDocument() != null) {
                String fileName = FileLoader.getDocumentFileName(messageObject.getDocument());
                if (!TextUtils.isEmpty(fileName)) {
                    items.add(UItem.asButton(12, R.drawable.msg_download, LocaleController.getString("FileName", R.string.FileName), fileName));
                }
            }

            // Resolution
            int w = 0, h = 0;
            if (messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
                TLRPC.PhotoSize sz = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 2000);
                if (sz != null) {
                    w = sz.w;
                    h = sz.h;
                }
            } else if (messageObject.getDocument() != null) {
                for (TLRPC.DocumentAttribute attr : messageObject.getDocument().attributes) {
                    if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                        w = attr.w;
                        h = attr.h;
                        break;
                    } else if (attr instanceof TLRPC.TL_documentAttributeImageSize) {
                        w = attr.w;
                        h = attr.h;
                        break;
                    }
                }
            }
            if (w > 0 && h > 0) {
                items.add(UItem.asButton(13, R.drawable.msg_gallery, LocaleController.getString("Resolution", R.string.Resolution), w + " x " + h));
            }

            // Duration
            int duration = (int) messageObject.getDuration();
            if (duration > 0) {
                int mins = duration / 60;
                int secs = duration % 60;
                items.add(UItem.asButton(14, R.drawable.msg_calls, "Duration", String.format(java.util.Locale.ROOT, "%02d:%02d", mins, secs)));
            }

            // Datacenter
            String dc = HundredGramUtils.getDC(messageObject);
            if (dc != null) {
                items.add(UItem.asButton(15, R.drawable.msg_home, LocaleController.getString("Datacenter", R.string.Datacenter), dc));
            }

            // Local File Path
            String localPath = messageObject.messageOwner.attachPath;
            if (!TextUtils.isEmpty(localPath) && new File(localPath).exists()) {
                // valid
            } else {
                File f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner);
                if (f != null && f.exists()) {
                    localPath = f.getAbsolutePath();
                } else if (messageObject.getDocument() != null) {
                    f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(messageObject.getDocument(), true);
                    if (f != null && f.exists()) {
                        localPath = f.getAbsolutePath();
                    } else {
                        localPath = null;
                    }
                } else {
                    localPath = null;
                }
            }
            if (!TextUtils.isEmpty(localPath)) {
                items.add(UItem.asButton(16, R.drawable.msg_openin, LocaleController.getString("FilePath", R.string.FilePath), localPath));
            }
        }

        items.add(UItem.asShadow(null));
    }

    private String formatDeltaTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        if (minutes < 60) {
            int remSec = seconds % 60;
            return remSec > 0 ? (minutes + "m " + remSec + "s") : (minutes + "m");
        }
        int hours = minutes / 60;
        int remMin = minutes % 60;
        if (hours < 24) {
            return remMin > 0 ? (hours + "h " + remMin + "m") : (hours + "h");
        }
        int days = hours / 24;
        int remHours = hours % 24;
        return remHours > 0 ? (days + "d " + remHours + "h") : (days + "d");
    }
}
