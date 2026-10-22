package com.exteragram.messenger.feed;

import com.google.android.gms.cast.framework.media.NotificationOptions;
import java.util.ArrayList;
import java.util.HashSet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

/* loaded from: D:\extragram\dex\classes.dex */
final class FeedBackfillCoordinator {
    private final int currentAccount;
    private int loadIndex;
    private final Runnable onRoundFinished;
    private int roundId;
    private boolean running;
    private final int guid = ConnectionsManager.generateClassGuid();
    private final HashSet<Long> pending = new HashSet<>();
    private final HashSet<Long> exhausted = new HashSet<>();

    public FeedBackfillCoordinator(int i, Runnable runnable) {
        this.currentAccount = i;
        this.onRoundFinished = runnable;
    }

    public HashSet<Long> getExhaustedSnapshot() {
        return new HashSet<>(this.exhausted);
    }

    public void clearExhausted() {
        this.exhausted.clear();
    }

    public void cancel() {
        this.running = false;
        this.roundId++;
        this.pending.clear();
        ConnectionsManager.getInstance(this.currentAccount).cancelRequestsForGuid(this.guid);
    }

    public void startRound(ArrayList<long[]> arrayList) {
        this.running = true;
        final int i = this.roundId + 1;
        this.roundId = i;
        this.pending.clear();
        int min = Math.min(4, arrayList.size());
        for (int i2 = 0; i2 < min; i2++) {
            this.pending.add(Long.valueOf(arrayList.get(i2)[0]));
        }
        MessagesController messagesController = MessagesController.getInstance(this.currentAccount);
        for (int i3 = 0; i3 < min; i3++) {
            long j = arrayList.get(i3)[0];
            int i4 = (int) arrayList.get(i3)[1];
            int i5 = this.guid;
            int i6 = this.loadIndex;
            this.loadIndex = i6 + 1;
            messagesController.loadMessages(j, 0L, false, 20, i4, 0, false, 0, i5, 0, 0, 0, 0L, 0, i6, false);
        }
        AndroidUtilities.runOnUIThread(new Runnable() { // from class: com.exteragram.messenger.feed.FeedBackfillCoordinator$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FeedBackfillCoordinator.this.lambda$startRound$0(i);
            }
        }, NotificationOptions.SKIP_STEP_TEN_SECONDS_IN_MS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startRound$0(int i) {
        if (i == this.roundId && this.running) {
            this.exhausted.addAll(this.pending);
            finishRound();
        }
    }

    public void onMessagesDidLoad(Object... objArr) {
        if (objArr == null || objArr.length <= 10) {
            return;
        }
        if (!(objArr[10] instanceof Integer) || ((Integer) objArr[10]).intValue() != this.guid) {
            return;
        }
        if (!(objArr[0] instanceof Long)) {
            return;
        }
        Long l = (Long) objArr[0];
        long longValue = l.longValue();
        if (objArr.length > 2 && (objArr[2] instanceof ArrayList) && ((ArrayList) objArr[2]).size() < 20) {
            this.exhausted.add(l);
        }
        onResult(longValue);
    }

    public void onLoadingMessagesFailed(Object... objArr) {
        long j;
        TLRPC.InputPeer inputPeer;
        if (objArr == null || objArr.length < 2) {
            return;
        }
        if (!(objArr[0] instanceof Integer) || ((Integer) objArr[0]).intValue() != this.guid) {
            return;
        }
        Object obj = objArr[1];
        if (!(obj instanceof TLRPC.TL_messages_getHistory) || (inputPeer = ((TLRPC.TL_messages_getHistory) obj).peer) == null) {
            j = 0;
        } else {
            long j2 = inputPeer.channel_id;
            if (j2 == 0) {
                j2 = inputPeer.chat_id;
            }
            j = -j2;
        }
        if (j != 0) {
            this.exhausted.add(Long.valueOf(j));
        }
        onResult(j);
    }

    private void onResult(long j) {
        if (this.running && this.pending.remove(Long.valueOf(j)) && this.pending.isEmpty()) {
            finishRound();
        }
    }

    private void finishRound() {
        this.running = false;
        this.roundId++;
        this.pending.clear();
        this.onRoundFinished.run();
    }
}