package com.exteragram.messenger.feed;

import android.text.TextUtils;
import androidx.collection.LongSparseArray;
import com.exteragram.messenger.feed.FeedConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

/* loaded from: D:\extragram\dex\classes.dex */
final class FeedTimelineLoader {
    private final AtomicInteger channelCacheEpoch = new AtomicInteger();
    private volatile ChannelSet channelSetCache;
    private final int currentAccount;

    public static final class ChannelEnumeration {
        int cacheEpoch;
        int configGeneration;
        boolean failed;
        boolean hasChannels;
        final ArrayList<ChannelSnapshot> included = new ArrayList<>();
        final ArrayList<TLRPC.Chat> channels = new ArrayList<>();
    }

    /* loaded from: D:\extragram\dex\classes4.dex */
    public static final class NewerPage {
        boolean failed;
        boolean hasMore;
        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        final ArrayList<TLRPC.User> users = new ArrayList<>();
        final ArrayList<TLRPC.Chat> chats = new ArrayList<>();
        final Cursor first = new Cursor();
    }

    /* loaded from: D:\extragram\dex\classes4.dex */
    public static final class OlderPage {
        boolean failed;
        boolean hasIncomplete;
        int lastChunkRowCount;
        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        final ArrayList<TLRPC.User> users = new ArrayList<>();
        final ArrayList<TLRPC.Chat> chats = new ArrayList<>();
        final ArrayList<long[]> backfillCandidates = new ArrayList<>();
        final Cursor last = new Cursor();
        final Cursor first = new Cursor();
    }

    /* loaded from: D:\extragram\dex\classes4.dex */
    public static final class WindowPage {
        boolean failed;
        boolean truncated;
        final ArrayList<TLRPC.Message> messages = new ArrayList<>();
        final ArrayList<TLRPC.User> users = new ArrayList<>();
        final ArrayList<TLRPC.Chat> chats = new ArrayList<>();
    }

    public FeedTimelineLoader(int i) {
        this.currentAccount = i;
    }

    public static final class Cursor {
        int date;
        int mid;
        long uid;

        public boolean isEmpty() {
            return this.date == 0;
        }

        public void set(int i, long j, int i2) {
            this.date = i;
            this.uid = j;
            this.mid = i2;
        }
    }

    public static final class ChannelSnapshot {
        int depthDate;
        int depthMid;
        final long dialogId;
        boolean hasCached;
        boolean hasHole;
        int holeEnd;
        boolean incomplete;
        boolean localStartReached;
        final int readInboxMax;
        final int topMessage;
        final int unreadCount;

        public ChannelSnapshot(long j, int i, int i2, int i3) {
            this.dialogId = j;
            this.readInboxMax = i;
            this.unreadCount = i2;
            this.topMessage = i3;
        }
    }

    public static final class ChannelSet {
        final int configGen;
        boolean failed;
        boolean hasChannels;
        final int sessionGen;
        final ArrayList<long[]> includedRows = new ArrayList<>();
        final ArrayList<TLRPC.Chat> channels = new ArrayList<>();

        public ChannelSet(int i, int i2) {
            this.sessionGen = i;
            this.configGen = i2;
        }
    }

    public synchronized void invalidateChannelCache() {
        this.channelCacheEpoch.incrementAndGet();
        this.channelSetCache = null;
    }

    public ChannelEnumeration enumerateChannels(FeedConfig feedConfig, int i, boolean z) {
        ChannelSet channelSet;
        int i2;
        boolean z2 = z;
        int i3 = 0;
        while (true) {
            synchronized (this) {
                channelSet = this.channelSetCache;
                i2 = this.channelCacheEpoch.get();
            }
            FeedConfig.Snapshot snapshot = feedConfig.snapshot();
            int generation = snapshot.getGeneration();
            if (z2 || channelSet == null || channelSet.sessionGen != i || channelSet.configGen != generation) {
                channelSet = buildChannelSet(snapshot.getIncludeArchived(), new HashSet<>(snapshot.getExcludedChannels()), i, generation);
                if (!channelSet.failed) {
                    synchronized (this) {
                        try {
                            if (i2 == this.channelCacheEpoch.get()) {
                                this.channelSetCache = channelSet;
                            }
                        } finally {
                        }
                    }
                }
            }
            if (channelSet.failed || i3 >= 3) {
                break;
            }
            synchronized (this) {
                try {
                    if (i2 == this.channelCacheEpoch.get()) {
                        break;
                    }
                } finally {
                }
            }
            i3++;
            z2 = true;
        }
        ChannelEnumeration channelEnumeration = new ChannelEnumeration();
        channelEnumeration.hasChannels = channelSet.hasChannels;
        channelEnumeration.failed = channelSet.failed;
        channelEnumeration.configGeneration = channelSet.configGen;
        channelEnumeration.cacheEpoch = i2;
        channelEnumeration.channels.addAll(channelSet.channels);
        for (int i4 = 0; i4 < channelSet.includedRows.size(); i4++) {
            long[] jArr = channelSet.includedRows.get(i4);
            channelEnumeration.included.add(new ChannelSnapshot(jArr[0], (int) jArr[1], (int) jArr[2], (int) jArr[3]));
        }
        return channelEnumeration;
    }

    public synchronized int getChannelCacheEpoch() {
        return this.channelCacheEpoch.get();
    }

    public synchronized boolean isEnumerationCurrent(ChannelEnumeration channelEnumeration) {
        boolean z = false;
        if (channelEnumeration != null) {
            z = channelEnumeration.cacheEpoch == this.channelCacheEpoch.get();
        }
        return z;
    }

    private ChannelSet buildChannelSet(boolean z, HashSet<Long> hashSet, int i, int i2) {
        MessagesStorage messagesStorage = MessagesStorage.getInstance(this.currentAccount);
        ChannelSet channelSet = new ChannelSet(i, i2);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        try {
            SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized("SELECT did, inbox_max, unread_count, last_mid, folder_id FROM dialogs WHERE did < 0", new Object[0]);
            while (queryFinalized.next()) {
                long longValue = queryFinalized.longValue(0);
                if (DialogObject.isChatDialog(longValue)) {
                    arrayList.add(new long[]{longValue, queryFinalized.intValue(1), queryFinalized.intValue(2), queryFinalized.intValue(3), queryFinalized.intValue(4)});
                    arrayList2.add(Long.valueOf(-longValue));
                }
            }
            queryFinalized.dispose();
            if (!arrayList.isEmpty()) {
                ArrayList<TLRPC.Chat> arrayList3 = new ArrayList<>();
                try {
                    messagesStorage.getChatsInternal(TextUtils.join(",", arrayList2), arrayList3);
                    LongSparseArray longSparseArray = new LongSparseArray();
                    for (int i3 = 0; i3 < arrayList3.size(); i3++) {
                        longSparseArray.put(arrayList3.get(i3).id, arrayList3.get(i3));
                    }
                    for (int i4 = 0; i4 < arrayList.size(); i4++) {
                        long[] jArr = (long[]) arrayList.get(i4);
                        long j = jArr[0];
                        TLRPC.Chat chat = (TLRPC.Chat) longSparseArray.get(-j);
                        if (FeedController.isEligibleChannel(chat) && (jArr[4] != 1 || z)) {
                            channelSet.hasChannels = true;
                            channelSet.channels.add(chat);
                            boolean isIncluded = CustomFeedManager.getInstance(this.currentAccount).isChannelIncludedInFeed(j);
                            if (isIncluded) {
                                channelSet.includedRows.add(new long[]{j, jArr[1], jArr[2], jArr[3]});
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    channelSet.failed = true;
                    return channelSet;
                }
            }
            return channelSet;
        } catch (Exception e2) {
            FileLog.e(e2);
            channelSet.failed = true;
            return channelSet;
        }
    }

    public OlderPage loadOlderPage(ArrayList<ChannelSnapshot> channels, Cursor cursor, HashSet<Long> excluded) {
        OlderPage olderPage = new OlderPage();
        if (channels == null || channels.isEmpty()) {
            return olderPage;
        }
        try {
            MessagesStorage messagesStorage = MessagesStorage.getInstance(this.currentAccount);
            ArrayList<Long> userIds = new ArrayList<>();
            ArrayList<Long> chatIds = new ArrayList<>();

            ArrayList<Long> channelIds = new ArrayList<>(channels.size());
            for (int i = 0; i < channels.size(); i++) {
                ChannelSnapshot cs = channels.get(i);
                if (excluded == null || !excluded.contains(cs.dialogId)) {
                    channelIds.add(cs.dialogId);
                }
            }
            if (channelIds.isEmpty()) {
                return olderPage;
            }

            if (cursor != null && !cursor.isEmpty()) {
                olderPage.first.set(cursor.date, cursor.uid, cursor.mid);
                olderPage.last.set(cursor.date, cursor.uid, cursor.mid);
            }

            int minDate = 0;
            String channelsStr = TextUtils.join(",", channelIds);
            int rowsLoaded = loadChunk(messagesStorage, channelsStr, minDate, olderPage, userIds, chatIds);
            olderPage.lastChunkRowCount = rowsLoaded;

            if (!userIds.isEmpty()) {
                messagesStorage.getUsersInternal(userIds, olderPage.users);
            }
            if (!chatIds.isEmpty()) {
                messagesStorage.getChatsInternal(TextUtils.join(",", chatIds), olderPage.chats);
            }
            clusterGroupedMessages(olderPage.messages);
        } catch (Exception e) {
            FileLog.e(e);
            olderPage.failed = true;
        }
        return olderPage;
    }

    private int loadChunk(MessagesStorage messagesStorage, String str, int i, OlderPage olderPage, ArrayList<Long> arrayList, ArrayList<Long> arrayList2) {
        StringBuilder sb = new StringBuilder("SELECT data, mid, date, uid FROM messages_v2 WHERE uid IN (");
        sb.append(str);
        sb.append(") AND mid > 0");
        if (i > 0) {
            sb.append(" AND date >= ");
            sb.append(i);
        }
        int i2 = 0;
        if (!olderPage.last.isEmpty()) {
            appendCursorBound(sb, olderPage.last, true, false);
        }
        sb.append(" ORDER BY date DESC, uid DESC, mid DESC LIMIT ");
        sb.append(30);
        try {
            SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized(sb.toString(), new Object[0]);
            try {
                while (queryFinalized.next()) {
                    i2++;
                    olderPage.last.set(queryFinalized.intValue(2), queryFinalized.longValue(3), queryFinalized.intValue(1));
                    if (olderPage.first.isEmpty()) {
                        Cursor cursor = olderPage.first;
                        Cursor cursor2 = olderPage.last;
                        cursor.set(cursor2.date, cursor2.uid, cursor2.mid);
                    }
                    TLRPC.Message readMessage = readMessage(queryFinalized);
                    if (readMessage != null) {
                        olderPage.messages.add(readMessage);
                        MessagesStorage.addUsersAndChatsFromMessage(readMessage, arrayList, arrayList2, null);
                    }
                }
            } finally {
                queryFinalized.dispose();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return i2;
    }

    private Cursor findUnreadBoundary(MessagesStorage messagesStorage, ArrayList<ChannelSnapshot> arrayList, int i) {
        StringBuilder sb = new StringBuilder();
        Cursor cursor = null;
        int i2 = 0;
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            ChannelSnapshot channelSnapshot = arrayList.get(i3);
            if (channelSnapshot.topMessage > channelSnapshot.readInboxMax || channelSnapshot.unreadCount > 0) {
                if (sb.length() > 0) {
                    sb.append(" OR ");
                }
                sb.append("uid = ");
                sb.append(channelSnapshot.dialogId);
                sb.append(" AND mid > ");
                sb.append(channelSnapshot.readInboxMax);
                i2++;
            }
            if (i2 > 0 && (i2 == 64 || i3 == arrayList.size() - 1)) {
                Cursor queryUnreadBoundary = queryUnreadBoundary(messagesStorage, sb, i);
                if (queryUnreadBoundary != null && (cursor == null || compareDesc(queryUnreadBoundary, cursor) > 0)) {
                    cursor = queryUnreadBoundary;
                }
                sb.setLength(0);
                i2 = 0;
            }
        }
        return cursor;
    }

    private Cursor queryUnreadBoundary(MessagesStorage messagesStorage, StringBuilder sb, int i) {
        StringBuilder sb2 = new StringBuilder("SELECT date, uid, mid FROM messages_v2 WHERE mid > 0 AND (");
        sb2.append((CharSequence) sb);
        sb2.append(")");
        if (i > 0) {
            sb2.append(" AND date >= ");
            sb2.append(i);
        }
        sb2.append(" ORDER BY date ASC, uid ASC, mid ASC LIMIT 1");
        try {
            SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized(sb2.toString(), new Object[0]);
            try {
                if (!queryFinalized.next()) {
                    queryFinalized.dispose();
                    return null;
                }
                Cursor cursor = new Cursor();
                cursor.set(queryFinalized.intValue(0), queryFinalized.longValue(1), queryFinalized.intValue(2));
                queryFinalized.dispose();
                return cursor;
            } catch (Throwable th) {
                queryFinalized.dispose();
                throw th;
            }
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static void appendCursorBound(StringBuilder sb, Cursor cursor, boolean z, boolean z2) {
        String str = z ? "<" : ">";
        sb.append(" AND (date ");
        sb.append(str);
        sb.append(' ');
        sb.append(cursor.date);
        sb.append(" OR date = ");
        sb.append(cursor.date);
        sb.append(" AND (uid ");
        sb.append(str);
        sb.append(' ');
        sb.append(cursor.uid);
        sb.append(" OR uid = ");
        sb.append(cursor.uid);
        sb.append(" AND mid ");
        sb.append(str);
        sb.append(z2 ? "= " : " ");
        sb.append(cursor.mid);
        sb.append("))");
    }

    private static int compareDesc(Cursor cursor, Cursor cursor2) {
        int i = cursor.date;
        int i2 = cursor2.date;
        if (i != i2) {
            return i > i2 ? -1 : 1;
        }
        long j = cursor.uid;
        long j2 = cursor2.uid;
        if (j != j2) {
            return j > j2 ? -1 : 1;
        }
        return -Integer.compare(cursor.mid, cursor2.mid);
    }

    public NewerPage loadNewerPage(ArrayList<ChannelSnapshot> arrayList, Cursor cursor) {
        NewerPage newerPage = new NewerPage();
        newerPage.first.set(cursor.date, cursor.uid, cursor.mid);
        try {
            ArrayList arrayList2 = new ArrayList(arrayList.size());
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                ChannelSnapshot channelSnapshot = arrayList.get(i);
                i++;
                arrayList2.add(Long.valueOf(channelSnapshot.dialogId));
            }
            MessagesStorage messagesStorage = MessagesStorage.getInstance(this.currentAccount);
            ArrayList<Long> arrayList3 = new ArrayList<>();
            ArrayList arrayList4 = new ArrayList();
            StringBuilder sb = new StringBuilder("SELECT data, mid, date, uid FROM messages_v2 WHERE uid IN (");
            sb.append(TextUtils.join(",", arrayList2));
            sb.append(") AND mid > 0");
            appendCursorBound(sb, cursor, false, false);
            sb.append(" ORDER BY date ASC, uid ASC, mid ASC LIMIT ");
            sb.append(50);
            SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized(sb.toString(), new Object[0]);
            int i2 = 0;
            while (queryFinalized.next()) {
                i2++;
                newerPage.first.set(queryFinalized.intValue(2), queryFinalized.longValue(3), queryFinalized.intValue(1));
                TLRPC.Message readMessage = readMessage(queryFinalized);
                if (readMessage != null) {
                    newerPage.messages.add(readMessage);
                    MessagesStorage.addUsersAndChatsFromMessage(readMessage, arrayList3, arrayList4, null);
                }
            }
            queryFinalized.dispose();
            newerPage.hasMore = i2 == 50;
            if (!arrayList3.isEmpty()) {
                messagesStorage.getUsersInternal(arrayList3, newerPage.users);
            }
            if (!arrayList4.isEmpty()) {
                messagesStorage.getChatsInternal(TextUtils.join(",", arrayList4), newerPage.chats);
            }
        } catch (Exception e) {
            FileLog.e(e);
            newerPage.failed = true;
        }
        clusterGroupedMessages(newerPage.messages);
        return newerPage;
    }

    public WindowPage loadChannelWindow(ArrayList<Long> arrayList, Cursor cursor, Cursor cursor2) {
        WindowPage windowPage = new WindowPage();
        if (!arrayList.isEmpty() && !cursor.isEmpty() && !cursor2.isEmpty()) {
            try {
                MessagesStorage messagesStorage = MessagesStorage.getInstance(this.currentAccount);
                ArrayList<Long> arrayList2 = new ArrayList<>();
                ArrayList arrayList3 = new ArrayList();
                StringBuilder sb = new StringBuilder("SELECT data, mid, date, uid FROM messages_v2 WHERE uid IN (");
                sb.append(TextUtils.join(",", arrayList));
                sb.append(") AND mid > 0");
                appendCursorBound(sb, cursor, true, true);
                int i = 0;
                appendCursorBound(sb, cursor2, false, true);
                sb.append(" ORDER BY date DESC, uid DESC, mid DESC LIMIT ");
                sb.append(501);
                SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized(sb.toString(), new Object[0]);
                while (true) {
                    if (!queryFinalized.next()) {
                        break;
                    }
                    i++;
                    if (i > 500) {
                        windowPage.truncated = true;
                        break;
                    }
                    TLRPC.Message readMessage = readMessage(queryFinalized);
                    if (readMessage != null) {
                        windowPage.messages.add(readMessage);
                        MessagesStorage.addUsersAndChatsFromMessage(readMessage, arrayList2, arrayList3, null);
                    }
                }
                queryFinalized.dispose();
                if (!arrayList2.isEmpty()) {
                    messagesStorage.getUsersInternal(arrayList2, windowPage.users);
                }
                if (!arrayList3.isEmpty()) {
                    messagesStorage.getChatsInternal(TextUtils.join(",", arrayList3), windowPage.chats);
                }
            } catch (Exception e) {
                FileLog.e(e);
                windowPage.failed = true;
                windowPage.messages.clear();
                windowPage.users.clear();
                windowPage.chats.clear();
            }
            clusterGroupedMessages(windowPage.messages);
        }
        return windowPage;
    }

    private void completeTrailingAlbum(MessagesStorage messagesStorage, OlderPage olderPage, ArrayList<Long> arrayList, ArrayList<Long> arrayList2) {
        TLRPC.Message readMessage;
        if (olderPage.messages.isEmpty()) {
            return;
        }
        TLRPC.Message message = olderPage.messages.get(olderPage.messages.size() - 1);
        if (message.grouped_id == 0) {
            return;
        }
        try {
            SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized("SELECT data, mid, date, uid FROM messages_v2 WHERE uid = " + message.dialog_id + " AND mid > 0 AND mid < " + message.id + " ORDER BY date DESC, mid DESC LIMIT 9", new Object[0]);
            try {
                while (queryFinalized.next() && (readMessage = readMessage(queryFinalized)) != null && readMessage.grouped_id == message.grouped_id) {
                    olderPage.messages.add(readMessage);
                    MessagesStorage.addUsersAndChatsFromMessage(readMessage, arrayList, arrayList2, null);
                }
            } finally {
                queryFinalized.dispose();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private TLRPC.Message readMessage(SQLiteCursor sQLiteCursor) {
        try {
            NativeByteBuffer byteBufferValue = sQLiteCursor.byteBufferValue(0);
            if (byteBufferValue == null) {
                return null;
            }
            TLRPC.Message TLdeserialize = TLRPC.Message.TLdeserialize(byteBufferValue, byteBufferValue.readInt32(false), false);
            if (TLdeserialize == null) {
                byteBufferValue.reuse();
                return null;
            }
            TLdeserialize.readAttachPath(byteBufferValue, UserConfig.getInstance(this.currentAccount).clientUserId);
            byteBufferValue.reuse();
            if ((TLdeserialize instanceof TLRPC.TL_messageEmpty) || TLdeserialize.action != null) {
                return null;
            }
            TLdeserialize.id = sQLiteCursor.intValue(1);
            TLdeserialize.date = sQLiteCursor.intValue(2);
            TLdeserialize.dialog_id = sQLiteCursor.longValue(3);
            return TLdeserialize;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static void loadChannelDepths(MessagesStorage messagesStorage, ArrayList<ChannelSnapshot> arrayList) {
        LongSparseArray longSparseArray = new LongSparseArray(arrayList.size());
        for (int i = 0; i < arrayList.size(); i++) {
            ChannelSnapshot channelSnapshot = arrayList.get(i);
            channelSnapshot.depthMid = 0;
            channelSnapshot.depthDate = Integer.MAX_VALUE;
            channelSnapshot.hasCached = false;
            channelSnapshot.localStartReached = false;
            longSparseArray.put(channelSnapshot.dialogId, channelSnapshot);
        }
        int i2 = 0;
        while (i2 < arrayList.size()) {
            int i3 = i2 + 64;
            int min = Math.min(i3, arrayList.size());
            StringBuilder sb = new StringBuilder();
            while (i2 < min) {
                if (sb.length() > 0) {
                    sb.append(" UNION ALL ");
                }
                ChannelSnapshot channelSnapshot2 = arrayList.get(i2);
                int max = Math.max(channelSnapshot2.holeEnd, 1);
                sb.append("SELECT uid, mid, date FROM (SELECT uid, mid, date FROM messages_v2 WHERE uid = ");
                sb.append(channelSnapshot2.dialogId);
                sb.append(" AND mid >= ");
                sb.append(max);
                sb.append(" ORDER BY date ASC, mid ASC LIMIT 1)");
                i2++;
            }
            try {
                SQLiteCursor queryFinalized = messagesStorage.getDatabase().queryFinalized(sb.toString(), new Object[0]);
                try {
                    while (queryFinalized.next()) {
                        ChannelSnapshot channelSnapshot3 = (ChannelSnapshot) longSparseArray.get(queryFinalized.longValue(0));
                        if (channelSnapshot3 != null) {
                            channelSnapshot3.depthMid = queryFinalized.intValue(1);
                            channelSnapshot3.depthDate = queryFinalized.intValue(2);
                            channelSnapshot3.hasCached = true;
                        }
                    }
                } finally {
                    queryFinalized.dispose();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            i2 = i3;
        }
        for (int i4 = 0; i4 < arrayList.size(); i4++) {
            ChannelSnapshot channelSnapshot4 = arrayList.get(i4);
            channelSnapshot4.localStartReached = !channelSnapshot4.hasHole && channelSnapshot4.hasCached;
        }
    }

    private static void clusterGroupedMessages(ArrayList<TLRPC.Message> arrayList) {
        if (arrayList == null || arrayList.size() < 3) {
            return;
        }
        try {
            HashMap hashMap = new HashMap();
            boolean z = false;
            for (int i = 0; i < arrayList.size(); i++) {
                TLRPC.Message msg = arrayList.get(i);
                if (msg == null) continue;
                long j = msg.grouped_id;
                if (j != 0) {
                    ArrayList arrayList2 = (ArrayList) hashMap.get(Long.valueOf(j));
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                        hashMap.put(Long.valueOf(j), arrayList2);
                    } else {
                        z = true;
                    }
                    arrayList2.add(msg);
                }
            }
            if (z) {
                ArrayList arrayList3 = new ArrayList(arrayList.size());
                HashSet hashSet = new HashSet();
                for (int i2 = 0; i2 < arrayList.size(); i2++) {
                    TLRPC.Message message = arrayList.get(i2);
                    if (message == null) continue;
                    long j2 = message.grouped_id;
                    if (j2 == 0) {
                        arrayList3.add(message);
                    } else if (hashSet.add(Long.valueOf(j2))) {
                        Collection grouped = (Collection) hashMap.get(Long.valueOf(j2));
                        if (grouped != null) {
                            arrayList3.addAll(grouped);
                        }
                    }
                }
                arrayList.clear();
                arrayList.addAll(arrayList3);
            }
        } catch (Throwable th) {
            FileLog.e(th);
        }
    }
}