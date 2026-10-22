package com.exteragram.messenger.feed;

import com.exteragram.messenger.feed.FeedTimelineLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import org.telegram.messenger.MessageObject;

/* loaded from: D:\extragram\dex\classes.dex */
public final class FeedStore {
    private int count;
    private boolean endReached;
    private final ArrayList<MessageObject> messages = new ArrayList<>();
    private final FeedMessageIdentityMap identityMap = new FeedMessageIdentityMap();
    private final HashSet<Long> hiddenDialogIds = new HashSet<>();
    private final FeedTimelineLoader.Cursor oldestCursor = new FeedTimelineLoader.Cursor();
    private final FeedTimelineLoader.Cursor newestCursor = new FeedTimelineLoader.Cursor();

    public synchronized ArrayList<MessageObject> getMessages() {
        return this.messages;
    }

    public synchronized ArrayList<MessageObject> getVisibleMessages() {
        if (this.hiddenDialogIds.isEmpty()) {
            return new ArrayList<>(this.messages);
        }
        ArrayList<MessageObject> arrayList = new ArrayList<>(this.messages.size());
        for (int i = 0; i < this.messages.size(); i++) {
            MessageObject messageObject = this.messages.get(i);
            if (messageObject != null && !this.hiddenDialogIds.contains(Long.valueOf(messageObject.getDialogId()))) {
                arrayList.add(messageObject);
            }
        }
        return arrayList;
    }

    public synchronized boolean isEmpty() {
        return this.messages.isEmpty();
    }

    public synchronized int getVisibleCount() {
        if (this.hiddenDialogIds.isEmpty()) {
            return this.messages.size();
        }
        int i = 0;
        for (int i2 = 0; i2 < this.messages.size(); i2++) {
            MessageObject messageObject = this.messages.get(i2);
            if (messageObject != null && !this.hiddenDialogIds.contains(Long.valueOf(messageObject.getDialogId()))) {
                i++;
            }
        }
        return i;
    }

    public synchronized boolean hasMessagesForDialog(long j) {
        for (int i = 0; i < this.messages.size(); i++) {
            MessageObject messageObject = this.messages.get(i);
            if (messageObject != null && messageObject.getDialogId() == j) {
                return true;
            }
        }
        return false;
    }

    public synchronized HashSet<Long> getLoadedDialogIds() {
        HashSet<Long> hashSet = new HashSet<>();
        for (int i = 0; i < this.messages.size(); i++) {
            MessageObject messageObject = this.messages.get(i);
            if (messageObject != null) {
                hashSet.add(Long.valueOf(messageObject.getDialogId()));
            }
        }
        return hashSet;
    }

    public synchronized HashSet<Long> getHiddenSnapshot() {
        return new HashSet<>(this.hiddenDialogIds);
    }

    public synchronized boolean setHidden(long j, boolean z) {
        HashSet<Long> hashSet = this.hiddenDialogIds;
        Long valueOf = Long.valueOf(j);
        boolean add = z ? hashSet.add(valueOf) : hashSet.remove(valueOf);
        if (add) {
            updateCount();
        }
        return add;
    }

    public synchronized boolean applyIncludedDialogs(HashSet<Long> hashSet) {
        HashSet<Long> loadedDialogIds = getLoadedDialogIds();
        Iterator<Long> it = loadedDialogIds.iterator();
        boolean z = false;
        while (it.hasNext()) {
            Long next = it.next();
            if (!hashSet.contains(next)) {
                z |= this.hiddenDialogIds.add(next);
            }
        }
        Iterator<Long> it2 = this.hiddenDialogIds.iterator();
        while (it2.hasNext()) {
            Long next2 = it2.next();
            if (hashSet.contains(next2) || !loadedDialogIds.contains(next2)) {
                it2.remove();
                z = true;
            }
        }
        if (z) {
            updateCount();
        }
        return z;
    }

    public synchronized FeedTimelineLoader.Cursor getOldestCursor() {
        return this.oldestCursor;
    }

    public synchronized FeedTimelineLoader.Cursor getNewestCursor() {
        return this.newestCursor;
    }

    public synchronized boolean isEndReached() {
        return this.endReached;
    }

    public synchronized void setEndReached(boolean z) {
        this.endReached = z;
        updateCount();
    }

    public synchronized int getCount() {
        return this.count;
    }

    private void updateCount() {
        int visibleCount = getVisibleCount();
        int newCount = 0;
        if (visibleCount != 0) {
            newCount = (this.endReached ? 0 : 3) + visibleCount;
        }
        this.count = newCount;
    }

    public synchronized void clear() {
        this.messages.clear();
        this.identityMap.clear();
        this.hiddenDialogIds.clear();
        this.endReached = false;
        this.count = 0;
        this.oldestCursor.set(0, 0L, 0);
        this.newestCursor.set(0, 0L, 0);
    }

    public synchronized ArrayList<MessageObject> appendMessages(ArrayList<MessageObject> arrayList, boolean z) {
        ArrayList<MessageObject> arrayList2 = new ArrayList<>(arrayList.size());
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            MessageObject messageObject = arrayList.get(i);
            i++;
            MessageObject messageObject2 = messageObject;
            if (this.identityMap.register(messageObject2)) {
                arrayList2.add(messageObject2);
            }
        }
        if (z) {
            ArrayList arrayList3 = new ArrayList(arrayList2);
            Collections.reverse(arrayList3);
            this.messages.addAll(0, arrayList3);
        } else {
            this.messages.addAll(arrayList2);
        }
        updateCount();
        return arrayList2;
    }

    public synchronized ArrayList<MessageObject> mergeRows(ArrayList<MessageObject> arrayList) {
        ArrayList<MessageObject> arrayList2 = new ArrayList<>(arrayList.size());
        int size = arrayList.size();
        int i = 0;
        int i2 = 0;
        while (i2 < size) {
            MessageObject messageObject = arrayList.get(i2);
            i2++;
            MessageObject messageObject2 = messageObject;
            if (this.identityMap.register(messageObject2)) {
                arrayList2.add(messageObject2);
            }
        }
        int i3 = 0;
        while (i < arrayList2.size()) {
            MessageObject messageObject3 = arrayList2.get(i);
            int i4 = i + 1;
            long groupId = messageObject3.getGroupId();
            while (groupId != 0 && i4 < arrayList2.size() && arrayList2.get(i4).getGroupId() == groupId && arrayList2.get(i4).getDialogId() == messageObject3.getDialogId()) {
                i4++;
            }
            i3 = findMergeIndex(messageObject3, i3);
            while (i < i4) {
                this.messages.add(i3, arrayList2.get(i));
                i++;
                i3++;
            }
            i = i4;
        }
        updateCount();
        return arrayList2;
    }

    private int findMergeIndex(MessageObject messageObject, int i) {
        MessageObject messageObject2;
        while (i < this.messages.size() && ((messageObject2 = this.messages.get(i)) == null || compareTimeline(messageObject2.messageOwner.date, messageObject2.getDialogId(), messageObject2.getRealId(), messageObject.messageOwner.date, messageObject.getDialogId(), messageObject.getRealId()) >= 0)) {
            i++;
        }
        while (i > 0 && i < this.messages.size()) {
            MessageObject messageObject3 = this.messages.get(i - 1);
            MessageObject messageObject4 = this.messages.get(i);
            if (messageObject3 == null || messageObject4 == null || messageObject3.getGroupId() == 0 || messageObject3.getGroupId() != messageObject4.getGroupId() || messageObject3.getDialogId() != messageObject4.getDialogId()) {
                break;
            }
            i++;
        }
        return i;
    }

    public synchronized void replaceMessage(MessageObject messageObject, MessageObject messageObject2) {
        if (messageObject == null || messageObject2 == null) {
            return;
        }
        int indexOf = this.messages.indexOf(messageObject);
        if (indexOf >= 0) {
            this.messages.set(indexOf, messageObject2);
        }
        this.identityMap.replace(messageObject2);
    }

    public synchronized ArrayList<Integer> deleteMessages(long j, ArrayList<Integer> arrayList, boolean[] zArr) {
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        if (arrayList == null) {
            return arrayList2;
        }
        HashSet hashSet = new HashSet(arrayList);
        HashSet<Integer> hashSet2 = new HashSet<>();
        boolean z = false;
        for (int i = 0; i < arrayList.size(); i++) {
            MessageObject byRealId = this.identityMap.getByRealId(j, arrayList.get(i).intValue());
            if (byRealId != null) {
                this.messages.remove(byRealId);
                purgeRow(byRealId, arrayList2, hashSet2);
                z = true;
            }
        }
        for (int size = this.messages.size() - 1; size >= 0; size--) {
            MessageObject messageObject = this.messages.get(size);
            if (messageObject != null && messageObject.getDialogId() == j && hashSet.contains(Integer.valueOf(messageObject.getRealId()))) {
                this.messages.remove(size);
                purgeRow(messageObject, arrayList2, hashSet2);
                z = true;
            }
        }
        if (z) {
            if (!hasMessagesForDialog(j)) {
                this.hiddenDialogIds.remove(Long.valueOf(j));
            }
            onRowsRemoved();
        }
        zArr[0] = z;
        return arrayList2;
    }

    public synchronized ArrayList<Integer> deleteHistory(long j, int i, boolean[] zArr) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        HashSet<Integer> hashSet = new HashSet<>();
        boolean z = false;
        for (int size = this.messages.size() - 1; size >= 0; size--) {
            MessageObject messageObject = this.messages.get(size);
            if (messageObject != null && messageObject.getDialogId() == j && messageObject.getRealId() > 0 && messageObject.getRealId() <= i) {
                this.messages.remove(size);
                purgeRow(messageObject, arrayList, hashSet);
                z = true;
            }
        }
        if (z) {
            if (!hasMessagesForDialog(j)) {
                this.hiddenDialogIds.remove(Long.valueOf(j));
            }
            onRowsRemoved();
        }
        zArr[0] = z;
        return arrayList;
    }

    public synchronized boolean trim(int i) {
        if (i <= 0 || this.messages.size() <= i) {
            return false;
        }
        MessageObject messageObject = this.messages.get(i - 1);
        int i2 = messageObject.messageOwner.date;
        long dialogId = messageObject.getDialogId();
        int realId = messageObject.getRealId();
        boolean z = false;
        for (int size = this.messages.size() - 1; size >= 0; size--) {
            MessageObject messageObject2 = this.messages.get(size);
            if (messageObject2 != null && compareTimeline(messageObject2.messageOwner.date, messageObject2.getDialogId(), messageObject2.getRealId(), i2, dialogId, realId) < 0) {
                this.messages.remove(size);
                this.identityMap.purge(messageObject2);
                z = true;
            }
        }
        if (!z) {
            return false;
        }
        if (this.messages.isEmpty()) {
            this.oldestCursor.set(0, 0L, 0);
        } else {
            int i3 = 0;
            int i4 = 0;
            long j = 0;
            for (int i5 = 0; i5 < this.messages.size(); i5++) {
                MessageObject messageObject3 = this.messages.get(i5);
                if (messageObject3 != null && (i3 == 0 || compareTimeline(messageObject3.messageOwner.date, messageObject3.getDialogId(), messageObject3.getRealId(), i3, j, i4) < 0)) {
                    i3 = messageObject3.messageOwner.date;
                    long dialogId2 = messageObject3.getDialogId();
                    i4 = messageObject3.getRealId();
                    j = dialogId2;
                }
            }
            this.oldestCursor.set(i3, j, i4);
        }
        this.endReached = false;
        updateCount();
        return true;
    }

    private void onRowsRemoved() {
        if (!rebuildPagingCursorsFromLoadedRows()) {
            this.endReached = false;
        }
        updateCount();
    }

    /* JADX WARN: Code restructure failed: missing block: B:9:0x004f, code lost:
    
        if (r14 > 0) goto L14;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean rebuildPagingCursorsFromLoadedRows() {
        boolean oldestEmpty = this.oldestCursor.isEmpty();
        int newestDate = 0;
        long newestUid = 0;
        int newestMid = 0;
        int oldestDate = 0;
        long oldestUid = 0;
        int oldestMid = 0;

        for (int i = 0; i < this.messages.size(); i++) {
            MessageObject msg = this.messages.get(i);
            if (!isPagingRow(msg)) {
                continue;
            }
            int date = msg.messageOwner.date;
            long uid = msg.getDialogId();
            int mid = msg.getRealId();

            if (newestDate == 0 || compareTimeline(date, uid, mid, newestDate, newestUid, newestMid) > 0) {
                newestDate = date;
                newestUid = uid;
                newestMid = mid;
            }
            if (oldestDate == 0 || compareTimeline(date, uid, mid, oldestDate, oldestUid, oldestMid) < 0) {
                oldestDate = date;
                oldestUid = uid;
                oldestMid = mid;
            }
        }

        if (newestDate == 0) {
            this.oldestCursor.set(0, 0, 0);
            this.newestCursor.set(0, 0, 0);
            return false;
        }

        if (!oldestEmpty && compareTimeline(oldestDate, oldestUid, oldestMid, this.oldestCursor.date, this.oldestCursor.uid, this.oldestCursor.mid) > 0) {
            this.endReached = false;
        }

        this.newestCursor.set(newestDate, newestUid, newestMid);
        this.oldestCursor.set(oldestDate, oldestUid, oldestMid);
        return true;
    }

    private static boolean isPagingRow(MessageObject messageObject) {
        return (messageObject == null || messageObject.isDateObject || messageObject.messageOwner == null || messageObject.getRealId() <= 0) ? false : true;
    }

    public static int compareTimeline(int i, long j, int i2, int i3, long j2, int i4) {
        if (i != i3) {
            return Integer.compare(i, i3);
        }
        if (j != j2) {
            return Long.compare(j, j2);
        }
        return Integer.compare(i2, i4);
    }

    private void purgeRow(MessageObject messageObject, ArrayList<Integer> arrayList, HashSet<Integer> hashSet) {
        this.identityMap.purge(messageObject);
        if (hashSet.add(Integer.valueOf(messageObject.getId()))) {
            arrayList.add(Integer.valueOf(messageObject.getId()));
        }
    }

    public synchronized boolean hasNoSyntheticIds() {
        return this.identityMap.isEmpty();
    }

    public synchronized MessageObject getMessage(long j, int i) {
        return this.identityMap.getByAnyId(j, i);
    }

    public synchronized int resolveRealMessageId(long j, int i) {
        return this.identityMap.resolveRealMessageId(j, i);
    }

    public synchronized long resolveRealDialogId(int i) {
        return this.identityMap.resolveRealDialogId(i);
    }
}