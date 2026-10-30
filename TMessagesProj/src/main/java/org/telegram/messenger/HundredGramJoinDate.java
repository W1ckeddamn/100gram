package org.telegram.messenger;

import android.util.LongSparseArray;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/**
 * Join date of the current user in a group/channel, shown in the profile info block.
 * Ported from OpexGram (ac.v1). In-memory only: a miss costs one request per chat per launch.
 */
public class HundredGramJoinDate {

    private static final LongSparseArray<Integer>[] cache = new LongSparseArray[5];
    private static final LongSparseArray<Boolean>[] loading = new LongSparseArray[5];

    static {
        for (int a = 0; a < 5; a++) {
            cache[a] = new LongSparseArray<>();
            loading[a] = new LongSparseArray<>();
        }
    }

    public static int get(int account, long chatId) {
        if (account < 0 || account >= 5) {
            return 0;
        }
        final Integer date = cache[account].get(chatId);
        return date == null ? 0 : date;
    }

    public static boolean has(int account, long chatId) {
        return HundredGramConfig.showJoinDate && get(account, chatId) > 0;
    }

    public static String format(int date) {
        return date <= 0 ? "" : LocaleController.getInstance().getChatFullDate().format(date * 1000L);
    }

    public static void load(int account, TLRPC.Chat chat, TLRPC.ChatFull chatFull, Runnable onLoaded) {
        if (!HundredGramConfig.showJoinDate || chat == null || account < 0 || account >= 5) {
            return;
        }
        final long chatId = chat.id;
        if (chatId <= 0 || cache[account].get(chatId) != null) {
            return;
        }
        if (ChatObject.isNotInChat(chat)) {
            put(account, chatId, 0, onLoaded);
            return;
        }
        if (ChatObject.isChannel(chat)) {
            if (loading[account].get(chatId) != null) {
                return;
            }
            loading[account].put(chatId, Boolean.TRUE);
            TLRPC.TL_channels_getParticipant req = new TLRPC.TL_channels_getParticipant();
            req.channel = MessagesController.getInputChannel(chat);
            req.participant = new TLRPC.TL_inputPeerSelf();
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                loading[account].remove(chatId);
                int date = 0;
                if (response instanceof TLRPC.TL_channels_channelParticipant) {
                    TLRPC.ChannelParticipant participant = ((TLRPC.TL_channels_channelParticipant) response).participant;
                    if (participant != null) {
                        date = Math.max(0, participant.date);
                    }
                }
                put(account, chatId, date, onLoaded);
            }));
            return;
        }
        if (chatFull == null || chatFull.participants == null) {
            return;
        }
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        int date = 0;
        for (int a = 0, N = chatFull.participants.participants.size(); a < N; a++) {
            TLRPC.ChatParticipant participant = chatFull.participants.participants.get(a);
            if (participant != null && participant.user_id == selfId) {
                date = Math.max(0, participant.date);
                break;
            }
        }
        put(account, chatId, date, onLoaded);
    }

    private static void put(int account, long chatId, int date, Runnable onLoaded) {
        cache[account].put(chatId, date);
        if (date > 0 && onLoaded != null) {
            onLoaded.run();
        }
    }
}
