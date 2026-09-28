package org.telegram.messenger;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.Locale;

public class HundredGramUtils {

    public static String getDC(TLRPC.User user) {
        return getDC(user, null);
    }

    public static String getDC(TLRPC.Chat chat) {
        return getDC(null, chat);
    }

    public static String getDC(TLRPC.User user, TLRPC.Chat chat) {
        int DC = 0;
        int myDC = ConnectionsManager.getInstance(UserConfig.selectedAccount).getCurrentDatacenterId();
        if (user != null) {
            if (UserObject.isUserSelf(user) && myDC != -1) {
                DC = myDC;
            } else {
                DC = user.photo != null ? user.photo.dc_id : -1;
            }
        } else if (chat != null) {
            DC = chat.photo != null ? chat.photo.dc_id : -1;
        }
        if (DC <= 0) {
            return "DC unknown";
        } else {
            String name = getDCName(DC);
            return name != null ? String.format(Locale.ROOT, "DC%d, %s", DC, name) : String.format(Locale.ROOT, "DC%d", DC);
        }
    }

    public static int getDCId(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) return 0;
        TLRPC.MessageMedia media = messageObject.messageOwner.media;
        if (media != null) {
            if (media.photo != null && media.photo.dc_id > 0) return media.photo.dc_id;
            if (media.document != null && media.document.dc_id > 0) return media.document.dc_id;
            if (media.webpage != null) {
                if (media.webpage.photo != null && media.webpage.photo.dc_id > 0) return media.webpage.photo.dc_id;
                if (media.webpage.document != null && media.webpage.document.dc_id > 0) return media.webpage.document.dc_id;
            }
        }
        return 0;
    }

    public static String getDC(MessageObject messageObject) {
        int dc = getDCId(messageObject);
        if (dc <= 0) {
            return null;
        }
        String name = getDCName(dc);
        return name != null ? String.format(Locale.ROOT, "DC%d, %s", dc, name) : String.format(Locale.ROOT, "DC%d", dc);
    }

    public static String getDCName(int dc) {
        switch (dc) {
            case 1:
            case 3:
                return "Miami FL, USA";
            case 2:
            case 4:
                return "Amsterdam, NL";
            case 5:
                return "Singapore, SG";
            default:
                return null;
        }
    }

    public static void copyPhotoToClipboard(MessageObject messageObject, Runnable onCopied) {
        if (messageObject == null) return;
        File file = null;
        String path = messageObject.messageOwner != null ? messageObject.messageOwner.attachPath : null;
        if (!TextUtils.isEmpty(path)) {
            File f = new File(path);
            if (f.exists()) file = f;
        }
        if (file == null && messageObject.messageOwner != null) {
            File f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner);
            if (f != null && f.exists()) file = f;
        }
        if (file == null && messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
            TLRPC.PhotoSize closest = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
            if (closest != null) {
                File f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(closest, true);
                if (f != null && f.exists()) file = f;
            }
        }
        if (file != null && file.exists()) {
            try {
                Context context = ApplicationLoader.applicationContext;
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                Uri uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file);
                ClipData clip = ClipData.newUri(context.getContentResolver(), "Photo", uri);
                clipboard.setPrimaryClip(clip);
                if (onCopied != null) {
                    onCopied.run();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }
}
