package org.telegram.messenger.antispam;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class HundredGramAntiSpam implements NotificationCenter.NotificationCenterDelegate {

    private static volatile HundredGramAntiSpam[] Instance = new HundredGramAntiSpam[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramAntiSpam getInstance(int account) {
        HundredGramAntiSpam localInstance = Instance[account];
        if (localInstance == null) {
            synchronized (HundredGramAntiSpam.class) {
                localInstance = Instance[account];
                if (localInstance == null) {
                    Instance[account] = localInstance = new HundredGramAntiSpam(account);
                }
            }
        }
        return localInstance;
    }

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(https?://|www\\.|t\\.me/|telegram\\.me/)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    private final int currentAccount;
    private boolean started = false;

    private HundredGramAntiSpam(int account) {
        this.currentAccount = account;
    }

    public synchronized void start() {
        if (started) return;
        started = true;
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);
        });
    }

    public synchronized void stop() {
        if (!started) return;
        started = false;
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        });
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            if (!HundredGramConfig.antiSpamEnabled) {
                return;
            }
            if (args != null && args.length > 1 && args[1] instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<MessageObject> messages = (ArrayList<MessageObject>) args[1];
                processIncomingMessages(messages);
            }
        }
    }

    private void processIncomingMessages(ArrayList<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) return;

        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (msg == null || msg.isOutOwner()) continue;

            long dialogId = msg.getDialogId();
            if (!DialogObject.isUserDialog(dialogId)) {
                // Only filter private Direct Messages (not channels or groups)
                continue;
            }

            long fromId = msg.getFromChatId();
            if (fromId == 0 && msg.messageOwner != null && msg.messageOwner.from_id != null) {
                fromId = msg.messageOwner.from_id.user_id;
            }
            if (fromId <= 0) continue;

            // Whitelist contacts
            if (ContactsController.getInstance(currentAccount).isContact(fromId)) {
                continue;
            }

            // Whitelist verified accounts & bots
            TLRPC.User sender = MessagesController.getInstance(currentAccount).getUser(fromId);
            if (sender != null && (sender.verified || sender.bot || sender.self || sender.contact)) {
                continue;
            }

            if (isSpam(msg)) {
                handleSpam(msg, sender, fromId, dialogId);
            }
        }
    }

    private boolean isSpam(MessageObject msg) {
        CharSequence messageText = msg.messageText;
        String text = messageText != null ? messageText.toString() : "";

        // 1. Check for links from non-contacts
        if (HundredGramConfig.antiSpamBlockUnknownLinks) {
            if (hasLinks(msg, text)) {
                return true;
            }
        }

        // 2. Check for keywords
        if (!TextUtils.isEmpty(HundredGramConfig.antiSpamKeywords)) {
            String lowerText = text.toLowerCase();
            String[] keywords = HundredGramConfig.antiSpamKeywords.split("[,;\\n]+");
            for (String kw : keywords) {
                String trimmed = kw.trim().toLowerCase();
                if (!trimmed.isEmpty() && lowerText.contains(trimmed)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasLinks(MessageObject msg, String text) {
        if (msg.messageOwner != null && msg.messageOwner.entities != null) {
            for (int i = 0; i < msg.messageOwner.entities.size(); i++) {
                TLRPC.MessageEntity entity = msg.messageOwner.entities.get(i);
                if (entity instanceof TLRPC.TL_messageEntityUrl || entity instanceof TLRPC.TL_messageEntityTextUrl) {
                    return true;
                }
            }
        }
        return URL_PATTERN.matcher(text).find();
    }

    private void handleSpam(MessageObject msg, TLRPC.User sender, long userId, long dialogId) {
        FileLog.d("HundredGramAntiSpam: Detected spam from user " + userId + " in dialog " + dialogId);
        HundredGramConfig.incrementAntiSpamBlockedCount();

        int action = HundredGramConfig.antiSpamAction;
        int date = msg.messageOwner != null ? msg.messageOwner.date : (int) (System.currentTimeMillis() / 1000);

        if (action == HundredGramConfig.ANTI_SPAM_ACTION_MUTE) {
            // Mute & mark read
            org.telegram.messenger.NotificationsController.getInstance(currentAccount).muteUntil(dialogId, 0, Integer.MAX_VALUE);
            MessagesController.getInstance(currentAccount).markDialogAsRead(dialogId, msg.getId(), 0, date, false, 0, 0, true, 0);
            MessagesController.getInstance(currentAccount).markDialogAsReadServer(dialogId, msg.getId(), date, 0);
        } else if (action == HundredGramConfig.ANTI_SPAM_ACTION_ARCHIVE_AND_MUTE) {
            // Move to Archive folder (folder_id = 1), mute & mark read
            MessagesController.getInstance(currentAccount).addDialogToFolder(dialogId, 1, -1, 0L);
            org.telegram.messenger.NotificationsController.getInstance(currentAccount).muteUntil(dialogId, 0, Integer.MAX_VALUE);
            MessagesController.getInstance(currentAccount).markDialogAsRead(dialogId, msg.getId(), 0, date, false, 0, 0, true, 0);
            MessagesController.getInstance(currentAccount).markDialogAsReadServer(dialogId, msg.getId(), date, 0);
        } else if (action == HundredGramConfig.ANTI_SPAM_ACTION_BLOCK) {
            // Block user & delete dialog
            MessagesController.getInstance(currentAccount).blockPeer(userId);
            MessagesController.getInstance(currentAccount).deleteDialog(dialogId, 0, false);
        }

        // Show informational Bulletin notification
        AndroidUtilities.runOnUIThread(() -> {
            try {
                BulletinFactory factory = BulletinFactory.global();
                if (factory != null) {
                    String senderName = sender != null ? sender.first_name : "Unknown";
                    String info = LocaleController.formatString("AntiSpamBlockedAlert", R.string.AntiSpamBlockedAlert, senderName);
                    factory.createSimpleBulletin(R.raw.chats_infotip, info).show();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
