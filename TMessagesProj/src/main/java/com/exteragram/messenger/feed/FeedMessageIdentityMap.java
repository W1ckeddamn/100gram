package com.exteragram.messenger.feed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

/* loaded from: D:\extragram\dex\classes.dex */
final class FeedMessageIdentityMap {
    private final HashMap<MessageCompositeID, Integer> generatedIds = new HashMap<>();
    private final ConcurrentHashMap<Integer, MessageCompositeID> realIdsByGeneratedId = new ConcurrentHashMap<>();
    private final HashMap<MessageCompositeID, MessageObject> messagesByRealId = new HashMap<>();
    private final HashMap<GroupKey, ArrayList<MessageObject>> groupMembers = new HashMap<>();
    private int lastGeneratedId = 2147483637;

    public boolean register(MessageObject messageObject) {
        boolean z;
        int realId = messageObject.getRealId();
        MessageCompositeID messageCompositeID = new MessageCompositeID(messageObject.getDialogId(), realId);
        Integer num = this.generatedIds.get(messageCompositeID);
        if (num == null) {
            int i = this.lastGeneratedId;
            this.lastGeneratedId = i - 1;
            num = Integer.valueOf(i);
            this.generatedIds.put(messageCompositeID, num);
        }
        this.realIdsByGeneratedId.put(num, messageCompositeID);
        if (this.messagesByRealId.containsKey(messageCompositeID)) {
            z = false;
        } else {
            this.messagesByRealId.put(messageCompositeID, messageObject);
            addToGroup(messageObject, messageCompositeID.dialog_id);
            z = true;
        }
        TLRPC.Message message = messageObject.messageOwner;
        message.realId = realId;
        message.id = num.intValue();
        return z;
    }

    public void replace(MessageObject messageObject) {
        MessageCompositeID messageCompositeID = new MessageCompositeID(messageObject.getDialogId(), messageObject.getRealId());
        this.generatedIds.put(messageCompositeID, Integer.valueOf(messageObject.getId()));
        this.realIdsByGeneratedId.put(Integer.valueOf(messageObject.getId()), messageCompositeID);
        MessageObject put = this.messagesByRealId.put(messageCompositeID, messageObject);
        if (put != null) {
            removeFromGroup(put, messageCompositeID.dialog_id);
        }
        addToGroup(messageObject, messageCompositeID.dialog_id);
    }

    public void purge(MessageObject messageObject) {
        MessageCompositeID messageCompositeID = new MessageCompositeID(messageObject.getDialogId(), messageObject.getRealId());
        this.generatedIds.remove(messageCompositeID);
        this.messagesByRealId.remove(messageCompositeID);
        this.realIdsByGeneratedId.remove(Integer.valueOf(messageObject.getId()));
        removeFromGroup(messageObject, messageCompositeID.dialog_id);
    }

    public MessageObject getByRealId(long j, int i) {
        return this.messagesByRealId.get(new MessageCompositeID(j, i));
    }

    public MessageObject getByAnyId(long j, int i) {
        MessageObject messageObject = this.messagesByRealId.get(new MessageCompositeID(j, i));
        if (messageObject != null) {
            return messageObject;
        }
        int resolveRealMessageId = resolveRealMessageId(j, i);
        if (resolveRealMessageId != i) {
            return this.messagesByRealId.get(new MessageCompositeID(j, resolveRealMessageId));
        }
        return null;
    }

    public int resolveRealMessageId(long j, int i) {
        MessageCompositeID messageCompositeID = this.realIdsByGeneratedId.get(Integer.valueOf(i));
        return (messageCompositeID == null || messageCompositeID.dialog_id != j) ? i : messageCompositeID.id;
    }

    public long resolveRealDialogId(int i) {
        MessageCompositeID messageCompositeID = this.realIdsByGeneratedId.get(Integer.valueOf(i));
        if (messageCompositeID != null) {
            return messageCompositeID.dialog_id;
        }
        return 0L;
    }

    public boolean isEmpty() {
        return this.realIdsByGeneratedId.isEmpty();
    }

    public void clear() {
        this.generatedIds.clear();
        this.realIdsByGeneratedId.clear();
        this.messagesByRealId.clear();
        this.groupMembers.clear();
        this.lastGeneratedId = 2147483637;
    }

    private void addToGroup(MessageObject messageObject, long j) {
        if (!messageObject.hasValidGroupId()) {
            messageObject.isPrimaryGroupMessage = false;
            return;
        }
        GroupKey groupKey = new GroupKey(j, messageObject.messageOwner.grouped_id);
        ArrayList<MessageObject> arrayList = this.groupMembers.get(groupKey);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.groupMembers.put(groupKey, arrayList);
        }
        if (!arrayList.contains(messageObject)) {
            arrayList.add(messageObject);
        }
        electPrimary(arrayList);
    }

    private void removeFromGroup(MessageObject messageObject, long j) {
        GroupKey groupKey;
        ArrayList<MessageObject> arrayList;
        if (messageObject.hasValidGroupId() && (arrayList = this.groupMembers.get((groupKey = new GroupKey(j, messageObject.messageOwner.grouped_id)))) != null) {
            arrayList.remove(messageObject);
            messageObject.isPrimaryGroupMessage = false;
            if (arrayList.isEmpty()) {
                this.groupMembers.remove(groupKey);
            } else {
                electPrimary(arrayList);
            }
        }
    }

    private static void electPrimary(ArrayList<MessageObject> arrayList) {
        MessageObject messageObject = null;
        for (int i = 0; i < arrayList.size(); i++) {
            MessageObject messageObject2 = arrayList.get(i);
            if (messageObject == null || messageObject2.getRealId() > messageObject.getRealId()) {
                messageObject = messageObject2;
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            arrayList.get(i2).isPrimaryGroupMessage = arrayList.get(i2) == messageObject;
        }
    }

    /* loaded from: D:\extragram\dex\classes4.dex */
    public static final class GroupKey {
        final long dialog_id;
        final long groupedId;

        public GroupKey(long j, long j2) {
            this.dialog_id = j;
            this.groupedId = j2;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && GroupKey.class == obj.getClass()) {
                GroupKey groupKey = (GroupKey) obj;
                if (this.dialog_id == groupKey.dialog_id && this.groupedId == groupKey.groupedId) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            return (Long.hashCode(this.dialog_id) * 31) + Long.hashCode(this.groupedId);
        }
    }

    public static final class MessageCompositeID {
        final long dialog_id;
        final int id;

        public MessageCompositeID(long j, int i) {
            this.dialog_id = j;
            this.id = i;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && MessageCompositeID.class == obj.getClass()) {
                MessageCompositeID messageCompositeID = (MessageCompositeID) obj;
                if (this.dialog_id == messageCompositeID.dialog_id && this.id == messageCompositeID.id) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            return (Long.hashCode(this.dialog_id) * 31) + this.id;
        }
    }
}