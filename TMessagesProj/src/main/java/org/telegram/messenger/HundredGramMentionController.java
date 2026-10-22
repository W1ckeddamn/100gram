package org.telegram.messenger;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.URLSpanUserMention;

import java.util.ArrayList;
import java.util.Collections;

public class HundredGramMentionController {

    public static final int TARGET_ALL = 0;
    public static final int TARGET_ONLINE = 1;
    public static final int TARGET_ADMINS = 2;

    public static final int STYLE_HIDDEN = 0;
    public static final int STYLE_VISIBLE = 1;

    public static final String INVISIBLE_CHAR = "\u2060"; // Zero-width word joiner

    public static class MentionAction extends TLObject {
        public static final int ACTION_ALL = 0;
        public static final int ACTION_ADMINS = 1;

        public int action;
        public String title;
        public String tag;

        public MentionAction(int action, String title, String tag) {
            this.action = action;
            this.title = title;
            this.tag = tag;
        }
    }

    public static void loadGroupParticipants(int currentAccount, TLRPC.Chat chat, TLRPC.ChatFull chatFull, int target, Utilities.Callback<ArrayList<TLRPC.User>> callback) {
        if (chat == null) {
            if (callback != null) {
                callback.run(new ArrayList<>());
            }
            return;
        }

        long selfUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        ArrayList<TLRPC.User> result = new ArrayList<>();
        MessagesController mc = MessagesController.getInstance(currentAccount);

        if (ChatObject.isChannel(chat)) {
            // Supergroup / Channel
            TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
            req.channel = mc.getInputChannel(chat.id);
            if (target == TARGET_ADMINS) {
                req.filter = new TLRPC.TL_channelParticipantsAdmins();
            } else {
                req.filter = new TLRPC.TL_channelParticipantsRecent();
            }
            req.offset = 0;
            req.limit = 200;

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (response instanceof TLRPC.TL_channels_channelParticipants) {
                    TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
                    mc.putUsers(res.users, false);
                    int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();

                    for (TLRPC.User user : res.users) {
                        if (user == null || user.id == selfUserId || user.bot || user.deleted) {
                            continue;
                        }
                        if (target == TARGET_ONLINE) {
                            if (!isUserOnline(user, currentTime)) {
                                continue;
                            }
                        }
                        result.add(user);
                    }
                }
                if (callback != null) {
                    callback.run(result);
                }
            }));
        } else {
            // Legacy / basic group
            if (chatFull != null && chatFull.participants != null && chatFull.participants.participants != null) {
                int currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
                for (TLRPC.ChatParticipant participant : chatFull.participants.participants) {
                    if (participant == null || participant.user_id == selfUserId) {
                        continue;
                    }
                    if (target == TARGET_ADMINS) {
                        if (!(participant instanceof TLRPC.TL_chatParticipantAdmin || participant instanceof TLRPC.TL_chatParticipantCreator)) {
                            continue;
                        }
                    }
                    TLRPC.User user = mc.getUser(participant.user_id);
                    if (user == null || user.bot || user.deleted) {
                        continue;
                    }
                    if (target == TARGET_ONLINE) {
                        if (!isUserOnline(user, currentTime)) {
                            continue;
                        }
                    }
                    result.add(user);
                }
            }
            if (callback != null) {
                callback.run(result);
            }
        }
    }

    public static boolean isUserOnline(TLRPC.User user, int currentTime) {
        if (user == null || user.status == null) {
            return false;
        }
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return true;
        }
        if (user.status instanceof TLRPC.TL_userStatusRecently) {
            return true;
        }
        return false;
    }

    /**
     * Builds mentions formatted as Spannable.
     * Max 50 entities allowed by Telegram server per message.
     */
    public static CharSequence buildMentions(ArrayList<TLRPC.User> users, int style, int maxLimit) {
        if (users == null || users.isEmpty()) {
            return "";
        }
        int limit = maxLimit > 0 ? Math.min(users.size(), maxLimit) : Math.min(users.size(), 50);

        if (style == STYLE_HIDDEN) {
            SpannableString spannable = new SpannableString(String.join("", Collections.nCopies(limit, INVISIBLE_CHAR)));
            for (int i = 0; i < limit; i++) {
                TLRPC.User user = users.get(i);
                spannable.setSpan(new URLSpanUserMention(String.valueOf(user.id), 3), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        } else {
            StringBuilder sb = new StringBuilder();
            ArrayList<int[]> spanRanges = new ArrayList<>();
            ArrayList<Long> spanUserIds = new ArrayList<>();

            for (int i = 0; i < limit; i++) {
                TLRPC.User user = users.get(i);
                String publicUsername = UserObject.getPublicUsername(user);
                if (!TextUtils.isEmpty(publicUsername)) {
                    sb.append("@").append(publicUsername).append(" ");
                } else {
                    String name = UserObject.getFirstName(user, false);
                    if (TextUtils.isEmpty(name)) {
                        name = "User";
                    }
                    int start = sb.length();
                    sb.append(name).append(" ");
                    int end = sb.length() - 1;
                    spanRanges.add(new int[]{start, end});
                    spanUserIds.add(user.id);
                }
            }
            SpannableString spannable = new SpannableString(sb);
            for (int i = 0; i < spanRanges.size(); i++) {
                int[] range = spanRanges.get(i);
                long uid = spanUserIds.get(i);
                spannable.setSpan(new URLSpanUserMention(String.valueOf(uid), 3), range[0], range[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }
    }
}
