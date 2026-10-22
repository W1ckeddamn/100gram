package com.exteragram.messenger.feed;

import androidx.collection.LongSparseArray;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;

/* loaded from: D:\extragram\dex\classes.dex */
public class FeedChannelRegistry implements NotificationCenter.NotificationCenterDelegate {
    private static final FeedChannelRegistry[] instances = new FeedChannelRegistry[16];
    private static final Object[] locks = new Object[16];
    private boolean built;
    public final int currentAccount;
    private boolean rebuildScheduled;
    private final HashSet<Long> channelIds = new HashSet<>();
    private final ArrayList<Listener> listeners = new ArrayList<>();
    private final Runnable rebuildRunnable = new Runnable() { // from class: com.exteragram.messenger.feed.FeedChannelRegistry$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            FeedChannelRegistry.this.lambda$new$0();
        }
    };

    public interface Listener {
        void onFeedChannelsChanged(HashSet<Long> hashSet, HashSet<Long> hashSet2);
    }

    static {
        for (int i = 0; i < 16; i++) {
            locks[i] = new Object();
        }
    }

    public static FeedChannelRegistry getInstance(int i) {
        FeedChannelRegistry feedChannelRegistry;
        FeedChannelRegistry[] feedChannelRegistryArr = instances;
        FeedChannelRegistry feedChannelRegistry2 = feedChannelRegistryArr[i];
        if (feedChannelRegistry2 != null) {
            return feedChannelRegistry2;
        }
        synchronized (locks[i]) {
            try {
                feedChannelRegistry = feedChannelRegistryArr[i];
                if (feedChannelRegistry == null) {
                    feedChannelRegistry = new FeedChannelRegistry(i);
                    feedChannelRegistryArr[i] = feedChannelRegistry;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return feedChannelRegistry;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0() {
        this.rebuildScheduled = false;
        rebuild(true);
    }

    private FeedChannelRegistry(final int i) {
        this.currentAccount = i;
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: com.exteragram.messenger.feed.FeedChannelRegistry$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                FeedChannelRegistry.this.lambda$new$1(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1(int i) {
        NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.dialogsNeedReload);
    }

    public void addListener(Listener listener) {
        ensureBuilt();
        if (this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
    }

    private void ensureBuilt() {
        if (this.built) {
            return;
        }
        this.built = true;
        rebuild(false);
    }

    @Override // org.telegram.messenger.NotificationCenter.NotificationCenterDelegate
    public void didReceivedNotification(int i, int i2, Object... objArr) {
        if (i == NotificationCenter.dialogsNeedReload) {
            ensureBuilt();
            if (this.rebuildScheduled) {
                return;
            }
            this.rebuildScheduled = true;
            AndroidUtilities.runOnUIThread(this.rebuildRunnable, 500L);
        }
    }

    private void rebuild(boolean z) {
        MessagesController messagesController = MessagesController.getInstance(this.currentAccount);
        LongSparseArray<TLRPC.Dialog> longSparseArray = messagesController.dialogs_dict;
        HashSet hashSet = new HashSet();
        for (int i = 0; i < longSparseArray.size(); i++) {
            TLRPC.Dialog valueAt = longSparseArray.valueAt(i);
            if (valueAt != null && DialogObject.isChatDialog(valueAt.id) && FeedController.isEligibleChannel(messagesController.getChat(Long.valueOf(-valueAt.id)))) {
                hashSet.add(Long.valueOf(valueAt.id));
            }
        }
        Iterator it = hashSet.iterator();
        HashSet<Long> hashSet2 = null;
        HashSet<Long> hashSet3 = null;
        while (it.hasNext()) {
            Long l = (Long) it.next();
            if (!this.channelIds.contains(l)) {
                if (hashSet3 == null) {
                    hashSet3 = new HashSet<>();
                }
                hashSet3.add(l);
            }
        }
        Iterator<Long> it2 = this.channelIds.iterator();
        while (it2.hasNext()) {
            Long next = it2.next();
            if (!hashSet.contains(next)) {
                if (hashSet2 == null) {
                    hashSet2 = new HashSet<>();
                }
                hashSet2.add(next);
            }
        }
        if (hashSet3 == null && hashSet2 == null) {
            return;
        }
        this.channelIds.clear();
        this.channelIds.addAll(hashSet);
        if (z) {
            if (hashSet3 == null) {
                hashSet3 = new HashSet<>();
            }
            if (hashSet2 == null) {
                hashSet2 = new HashSet<>();
            }
            for (int size = this.listeners.size() - 1; size >= 0; size--) {
                this.listeners.get(size).onFeedChannelsChanged(hashSet3, hashSet2);
            }
        }
    }
}