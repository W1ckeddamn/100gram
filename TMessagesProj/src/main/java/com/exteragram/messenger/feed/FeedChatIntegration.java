package com.exteragram.messenger.feed;

import androidx.collection.LongSparseArray;
import com.exteragram.messenger.feed.FeedController;
import java.util.ArrayList;
import java.util.HashSet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_update;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

/* loaded from: D:\extragram\dex\classes4.dex */
public class FeedChatIntegration {
    private Runnable channelsChangedCallback;
    private final int currentAccount;
    private boolean destroyed;
    private final Host host;
    private boolean initialScrollApplied;
    private boolean pagedownShownByScroll;
    private boolean pendingDividerScroll;
    private long pendingHideDialogId;
    private ScrollAnchor pendingInitialScrollRestore;
    private boolean reactionsRefreshScheduled;
    private boolean readyToMarkAsRead;
    private final boolean restoreDrawerScrollPosition;
    private boolean scrollPreservedNewerToUnread;
    private boolean settleAtNewestScheduled;
    private int totalScrollDy;
    private MessageObject unreadDivider;
    private boolean viewportActive;
    private static final int PAGEDOWN_SCROLL_THRESHOLD = AndroidUtilities.dp(100.0f);
    private static final int NEAR_NEWEST_THRESHOLD = AndroidUtilities.dp(160.0f);
    private int preserveScrollLoadIndex = -1;
    private int lastPagedownCount = -1;
    private final Runnable settleAtNewestRunnable = new Runnable() { // from class: com.exteragram.messenger.feed.FeedChatIntegration$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            FeedChatIntegration.this.settleAtNewestNow();
        }
    };
    private final int reactionsRequestGuid = ConnectionsManager.generateClassGuid();
    private final LongSparseArray<ArrayList<Integer>> pendingReactionIds = new LongSparseArray<>();
    private final Runnable reactionsRefreshRunnable = new Runnable() { // from class: com.exteragram.messenger.feed.FeedChatIntegration$$ExternalSyntheticLambda1
        @Override // java.lang.Runnable
        public final void run() {
            FeedChatIntegration.this.flushReactionsRefresh();
        }
    };

    public interface Host {
        boolean canScrollToNewer();

        ScrollAnchor captureScrollAnchor();

        void deleteRows(ArrayList<Integer> arrayList);

        void dematerializeRow(MessageObject messageObject);

        int getDistanceToNewerPx();

        BaseFragment getFragment();

        int getLastVisibleMessageIndex();

        ArrayList<MessageObject> getMessages();

        int getNewestVisibleMessageIndex();

        void invalidateVisiblePart();

        boolean isFirstLoadComplete();

        boolean isListReady();

        boolean isListScrollIdle();

        boolean isPagedownButtonVisible();

        boolean isScrollAnimationRunning();

        void materializeRow(MessageObject messageObject);

        int nextStableId();

        void notifyAllMessagesChanged();

        void notifyMessageInserted(int i);

        void notifyMessageRemoved(int i);

        void onFeedListChanged();

        void reloadFeed();

        void requestOlderFeedPage();

        void restoreScrollAnchor(ScrollAnchor scrollAnchor);

        void scrollToMessage(int i, int i2);

        void scrollToMessageAnimated(int i, int i2);

        void setPagedownButtonVisible(boolean z);

        void setPagedownCount(int i);

        void showEmptyFeedProgress();

        void showEmptyFeedState();

        int stableIdForDateHeader(int i);
    }

    public static final class ScrollAnchor {
        public final int offsetTop;
        public final MessageObject row;

        public ScrollAnchor(MessageObject messageObject, int i) {
            this.row = messageObject;
            this.offsetTop = i;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settleAtNewestNow() {
        this.settleAtNewestScheduled = false;
        if (this.destroyed || !this.viewportActive || !this.host.isListReady() || this.host.isScrollAnimationRunning() || this.host.canScrollToNewer()) {
            return;
        }
        settleUnreadDivider();
    }

    public FeedChatIntegration(int i, Host host, boolean z) {
        this.currentAccount = i;
        this.host = host;
        this.restoreDrawerScrollPosition = z;
    }

    public void refreshAds() {
        requestPendingInitialPosition();
    }

    public void setChannelsChangedCallback(Runnable runnable) {
        this.channelsChangedCallback = runnable;
    }

    public void notifyChannelsChanged() {
        Runnable runnable = this.channelsChangedCallback;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void resetUiState() {
        resetMetadataRefresh();
        this.initialScrollApplied = false;
        this.readyToMarkAsRead = false;
        this.pendingDividerScroll = false;
        this.pendingInitialScrollRestore = null;
        this.scrollPreservedNewerToUnread = false;
        this.preserveScrollLoadIndex = -1;
        this.unreadDivider = null;
        this.lastPagedownCount = -1;
        this.pagedownShownByScroll = false;
        this.totalScrollDy = 0;
        this.pendingHideDialogId = 0L;
        if (this.settleAtNewestScheduled) {
            AndroidUtilities.cancelRunOnUIThread(this.settleAtNewestRunnable);
            this.settleAtNewestScheduled = false;
        }
    }

    private boolean hasMaterializedPostRows() {
        ArrayList<MessageObject> messages = this.host.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            if (FeedMessageUtils.isPostRow(messages.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void onMessagesLoaded() {
        if (!this.host.getFragment().isPaused() && this.host.isListReady() && hasMaterializedPostRows()) {
            if (!this.initialScrollApplied) {
                this.initialScrollApplied = true;
                FeedController feedController = FeedController.getInstance(this.currentAccount);
                boolean consumeInitialUnreadScroll = feedController.consumeInitialUnreadScroll();
                FeedController.SavedScrollPosition drawerScrollPosition = this.restoreDrawerScrollPosition ? feedController.getDrawerScrollPosition() : null;
                MessageObject message = drawerScrollPosition != null ? feedController.getMessage(drawerScrollPosition.dialogId, drawerScrollPosition.messageId) : null;
                if (message != null && this.host.getMessages().contains(message)) {
                    applyUnreadDivider(false);
                    refreshAds();
                    this.pendingInitialScrollRestore = new ScrollAnchor(message, drawerScrollPosition.offsetTop);
                    requestPendingInitialPosition();
                } else {
                    applyUnreadDivider((consumeInitialUnreadScroll || this.host.getDistanceToNewerPx() > NEAR_NEWEST_THRESHOLD) ? consumeInitialUnreadScroll : true);
                }
            }
        }
    }

    public void onHostResumed() {
        if (!this.host.getMessages().isEmpty()) {
            onMessagesLoaded();
        }
        requestPendingInitialPosition();
    }

    private boolean hasPendingInitialPosition() {
        return this.pendingInitialScrollRestore != null || this.pendingDividerScroll;
    }

    private void requestPendingInitialPosition() {
        if (this.host.getFragment().isPaused() || !this.host.isListReady()) {
            return;
        }
        ScrollAnchor scrollAnchor = this.pendingInitialScrollRestore;
        if (scrollAnchor != null) {
            this.host.restoreScrollAnchor(scrollAnchor);
            return;
        }
        if (this.pendingDividerScroll) {
            int indexOf = this.unreadDivider == null ? -1 : this.host.getMessages().indexOf(this.unreadDivider);
            if (indexOf < 0) {
                this.pendingDividerScroll = false;
                this.readyToMarkAsRead = true;
            } else {
                this.host.scrollToMessage(indexOf, AndroidUtilities.dp(48.0f));
            }
        }
    }

    public void setViewportActive(boolean z) {
        if (this.viewportActive == z) {
            return;
        }
        this.viewportActive = z;
        if (!z) {
            if (this.settleAtNewestScheduled) {
                AndroidUtilities.cancelRunOnUIThread(this.settleAtNewestRunnable);
                this.settleAtNewestScheduled = false;
            }
            cancelPendingReactionsRefresh();
            return;
        }
        onHostResumed();
        if (this.host.getMessages().isEmpty()) {
            return;
        }
        onVisiblePartInvalidated();
    }

    public boolean canMarkVisibleAsRead() {
        return this.viewportActive && !this.host.getFragment().isPaused() && this.initialScrollApplied && this.readyToMarkAsRead && !this.pendingDividerScroll && this.pendingInitialScrollRestore == null && !BaseFragment.hasSheets(this.host.getFragment());
    }

    public void onPostCellVisible(MessageObject messageObject, boolean z, boolean z2) {
        if (messageObject == null || messageObject.isSponsored()) {
            return;
        }
        requestReactionsRefresh(messageObject);
        if (canMarkVisibleAsRead()) {
            if (z || z2) {
                FeedController.getInstance(this.currentAccount).onPostSeen(messageObject.getDialogId(), messageObject.getRealId());
            }
        }
    }

    private void requestReactionsRefresh(MessageObject messageObject) {
        if (this.destroyed || !this.viewportActive || messageObject.messageOwner == null) {
            return;
        }
        int realId = messageObject.getRealId();
        long dialogId = messageObject.getDialogId();
        if (realId <= 0 || dialogId == 0) {
            return;
        }
        if (messageObject.messageOwner.action == null || messageObject.canSetReaction()) {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - messageObject.reactionsLastCheckTime <= 15000) {
                return;
            }
            messageObject.reactionsLastCheckTime = currentTimeMillis;
            ArrayList<Integer> arrayList = this.pendingReactionIds.get(dialogId);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.pendingReactionIds.put(dialogId, arrayList);
            }
            arrayList.add(Integer.valueOf(realId));
            if (this.reactionsRefreshScheduled) {
                return;
            }
            this.reactionsRefreshScheduled = true;
            AndroidUtilities.runOnUIThread(this.reactionsRefreshRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flushReactionsRefresh() {
        this.reactionsRefreshScheduled = false;
        if (this.destroyed || !this.viewportActive) {
            this.pendingReactionIds.clear();
            return;
        }
        for (int i = 0; i < this.pendingReactionIds.size(); i++) {
            TLRPC.TL_messages_getMessagesReactions tL_messages_getMessagesReactions = new TLRPC.TL_messages_getMessagesReactions();
            tL_messages_getMessagesReactions.peer = MessagesController.getInstance(this.currentAccount).getInputPeer(this.pendingReactionIds.keyAt(i));
            tL_messages_getMessagesReactions.id.addAll(this.pendingReactionIds.valueAt(i));
            ConnectionsManager.getInstance(this.currentAccount).bindRequestToGuid(ConnectionsManager.getInstance(this.currentAccount).sendRequest(tL_messages_getMessagesReactions, new RequestDelegate() { // from class: com.exteragram.messenger.feed.FeedChatIntegration$$ExternalSyntheticLambda3
                @Override // org.telegram.tgnet.RequestDelegate
                public final void run(TLObject tLObject, TLRPC.TL_error tL_error) {
                    FeedChatIntegration.this.lambda$flushReactionsRefresh$0(tLObject, tL_error);
                }
            }), this.reactionsRequestGuid);
        }
        this.pendingReactionIds.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$flushReactionsRefresh$0(TLObject tLObject, TLRPC.TL_error tL_error) {
        if (tLObject instanceof TLRPC.Updates) {
            TLRPC.Updates updates = (TLRPC.Updates) tLObject;
            for (int i = 0; i < updates.updates.size(); i++) {
                TLRPC.Update u = updates.updates.get(i);
                if (u instanceof TL_update.TL_updateMessageReactions) {
                    ((TL_update.TL_updateMessageReactions) u).updateUnreadState = false;
                }
            }
            MessagesController.getInstance(this.currentAccount).processUpdates(updates, false);
        }
    }

    private void cancelPendingReactionsRefresh() {
        AndroidUtilities.cancelRunOnUIThread(this.reactionsRefreshRunnable);
        this.reactionsRefreshScheduled = false;
        this.pendingReactionIds.clear();
    }

    private void resetMetadataRefresh() {
        cancelPendingReactionsRefresh();
        ConnectionsManager.getInstance(this.currentAccount).cancelRequestsForGuid(this.reactionsRequestGuid);
    }

    public void markAllRead() {
        FeedController.getInstance(this.currentAccount).markAllRead();
        applyUnreadDivider(false);
        refreshAds();
        this.host.invalidateVisiblePart();
    }

    public void settleUnreadDivider() {
        int lastVisibleMessageIndex;
        if (canMarkVisibleAsRead() && this.host.isListReady() && (lastVisibleMessageIndex = this.host.getLastVisibleMessageIndex()) != Integer.MIN_VALUE) {
            ArrayList<MessageObject> messages = this.host.getMessages();
            int min = this.host.canScrollToNewer() ? Math.min(lastVisibleMessageIndex, messages.size() - 1) : messages.size() - 1;
            if (min >= 0) {
                FeedController feedController = FeedController.getInstance(this.currentAccount);
                for (int i = 0; i <= min; i++) {
                    MessageObject messageObject = messages.get(i);
                    if (messageObject != null && !messageObject.isDateObject && messageObject.type != 6 && !messageObject.isSponsored()) {
                        feedController.onPostSeen(messageObject.getDialogId(), messageObject.getRealId());
                    }
                }
            }
            applyUnreadDivider(false);
            refreshAds();
            updatePagedownCounter();
        }
    }

    public void onScrollAnimationFinished() {
        if (this.destroyed || !this.viewportActive || this.settleAtNewestScheduled) {
            return;
        }
        this.settleAtNewestScheduled = true;
        AndroidUtilities.runOnUIThread(this.settleAtNewestRunnable);
    }

    public void destroy() {
        this.destroyed = true;
        resetMetadataRefresh();
        if (this.settleAtNewestScheduled) {
            AndroidUtilities.cancelRunOnUIThread(this.settleAtNewestRunnable);
            this.settleAtNewestScheduled = false;
        }
        this.pendingInitialScrollRestore = null;
    }

    public void saveDrawerScrollPosition() {
        ScrollAnchor captureScrollAnchor = this.host.captureScrollAnchor();
        if (captureScrollAnchor == null || captureScrollAnchor.row == null) {
            return;
        }
        FeedController.getInstance(this.currentAccount).saveDrawerScrollPosition(captureScrollAnchor.row.getDialogId(), captureScrollAnchor.row.getRealId(), captureScrollAnchor.offsetTop);
    }

    public void onReadStateRefreshed() {
        ScrollAnchor captureScrollAnchor = this.host.captureScrollAnchor();
        boolean hasPendingInitialPosition = hasPendingInitialPosition();
        applyUnreadDivider(false);
        refreshAds();
        if (!hasPendingInitialPosition || !hasPendingInitialPosition()) {
            this.host.restoreScrollAnchor(captureScrollAnchor);
        }
        this.lastPagedownCount = -1;
        updatePagedownCounter();
        this.host.invalidateVisiblePart();
    }

    public void onPreserveScrollLoadStarted(int i) {
        this.preserveScrollLoadIndex = i;
    }

    public boolean consumePreserveScrollLoad(int i) {
        if (this.preserveScrollLoadIndex != i) {
            return false;
        }
        this.preserveScrollLoadIndex = -1;
        return true;
    }

    public void beforePreservedNewerMessagesInserted() {
        this.scrollPreservedNewerToUnread = this.host.isListScrollIdle() && !this.host.isScrollAnimationRunning() && this.host.getDistanceToNewerPx() <= NEAR_NEWEST_THRESHOLD;
    }

    public boolean afterPreservedNewerMessagesInserted() {
        boolean z = this.scrollPreservedNewerToUnread;
        applyUnreadDivider(z, true);
        this.scrollPreservedNewerToUnread = false;
        return z;
    }

    public void applyUnreadDivider(boolean z) {
        applyUnreadDivider(z, false);
    }

    private void applyUnreadDivider(boolean z, boolean z2) {
        if (this.host.isListReady()) {
            ArrayList<MessageObject> messages = this.host.getMessages();
            if (messages.isEmpty()) {
                this.unreadDivider = null;
                this.readyToMarkAsRead = false;
                return;
            }
            MessageObject messageObject = this.unreadDivider;
            int indexOf = messageObject == null ? -1 : messages.indexOf(messageObject);
            MessageObject messageObject2 = indexOf >= 0 ? this.unreadDivider : null;
            if (indexOf >= 0) {
                messages.remove(indexOf);
            }
            this.unreadDivider = null;
            int findFirstUnreadIndex = FeedController.getInstance(this.currentAccount).findFirstUnreadIndex(messages);
            if (findFirstUnreadIndex < 0) {
                this.pendingDividerScroll = false;
                if (indexOf >= 0) {
                    if (z && !z2) {
                        this.host.notifyAllMessagesChanged();
                    } else {
                        this.host.notifyMessageRemoved(indexOf);
                        this.host.invalidateVisiblePart();
                    }
                }
                this.readyToMarkAsRead = true;
                return;
            }
            int findDividerInsertIndex = findDividerInsertIndex(messages, findFirstUnreadIndex);
            if (messageObject2 == null) {
                messageObject2 = FeedMessageUtils.createUnreadDivider(this.currentAccount, this.host.nextStableId());
            }
            messages.add(findDividerInsertIndex, messageObject2);
            this.unreadDivider = messageObject2;
            if (z) {
                this.readyToMarkAsRead = false;
                if (!z2) {
                    this.host.notifyAllMessagesChanged();
                } else if (indexOf < 0) {
                    this.host.notifyMessageInserted(findDividerInsertIndex);
                } else if (indexOf != findDividerInsertIndex) {
                    this.host.notifyMessageRemoved(indexOf);
                    this.host.notifyMessageInserted(findDividerInsertIndex);
                }
                this.pendingDividerScroll = true;
                requestPendingInitialPosition();
                this.host.invalidateVisiblePart();
                return;
            }
            this.readyToMarkAsRead = true;
            if (indexOf < 0) {
                this.host.notifyMessageInserted(findDividerInsertIndex);
                this.host.invalidateVisiblePart();
            } else if (indexOf != findDividerInsertIndex) {
                this.host.notifyMessageRemoved(indexOf);
                this.host.notifyMessageInserted(findDividerInsertIndex);
                this.host.invalidateVisiblePart();
            }
        }
    }

    public boolean scrollToUnreadDividerIfAbove() {
        int newestVisibleMessageIndex;
        if (!this.host.isListReady()) {
            return false;
        }
        ArrayList<MessageObject> messages = this.host.getMessages();
        MessageObject messageObject = this.unreadDivider;
        if (messageObject == null || !messages.contains(messageObject)) {
            applyUnreadDivider(false);
            refreshAds();
            messages = this.host.getMessages();
        }
        int indexOf = messages.indexOf(this.unreadDivider);
        if (indexOf < 0 || (newestVisibleMessageIndex = this.host.getNewestVisibleMessageIndex()) == Integer.MIN_VALUE || indexOf >= newestVisibleMessageIndex) {
            return false;
        }
        this.host.scrollToMessageAnimated(indexOf, AndroidUtilities.dp(48.0f));
        this.host.invalidateVisiblePart();
        return true;
    }

    private static int findDividerInsertIndex(ArrayList<MessageObject> arrayList, int i) {
        MessageObject messageObject;
        MessageObject messageObject2 = arrayList.get(i);
        long groupId = messageObject2.getGroupId();
        if (groupId == 0) {
            return i + 1;
        }
        long dialogId = messageObject2.getDialogId();
        do {
            i++;
            if (i >= arrayList.size() || (messageObject = arrayList.get(i)) == null || messageObject.getGroupId() != groupId) {
                break;
            }
        } while (messageObject.getDialogId() == dialogId);
        return i;
    }

    public void onVisiblePartInvalidated() {
        if (!this.viewportActive || this.host.getFragment().isPaused()) {
            return;
        }
        ScrollAnchor scrollAnchor = this.pendingInitialScrollRestore;
        if (scrollAnchor != null) {
            this.host.restoreScrollAnchor(scrollAnchor);
            this.pendingInitialScrollRestore = null;
        }
        maybeScrollToDivider();
        updatePagedownCounter();
    }

    private void maybeScrollToDivider() {
        if (this.pendingDividerScroll) {
            if (!this.host.isListReady() || this.unreadDivider == null) {
                this.pendingDividerScroll = false;
                this.readyToMarkAsRead = true;
                return;
            }
            int lastVisibleMessageIndex = this.host.getLastVisibleMessageIndex();
            if (lastVisibleMessageIndex == Integer.MIN_VALUE) {
                return;
            }
            int indexOf = this.host.getMessages().indexOf(this.unreadDivider);
            if (indexOf >= 0 && indexOf > lastVisibleMessageIndex) {
                this.host.scrollToMessage(indexOf, AndroidUtilities.dp(48.0f));
            }
            this.pendingDividerScroll = false;
            this.readyToMarkAsRead = true;
        }
    }

    private void updatePagedownCounter() {
        if (this.host.isListReady() && !this.host.isScrollAnimationRunning()) {
            int newestVisibleMessageIndex = this.host.getNewestVisibleMessageIndex();
            int countUnreadBelow = newestVisibleMessageIndex == Integer.MIN_VALUE ? 0 : FeedController.getInstance(this.currentAccount).countUnreadBelow(this.host.getMessages(), newestVisibleMessageIndex);
            if (countUnreadBelow != this.lastPagedownCount) {
                this.lastPagedownCount = countUnreadBelow;
                this.host.setPagedownCount(countUnreadBelow);
            }
            if (countUnreadBelow > 0) {
                this.pagedownShownByScroll = false;
                this.host.setPagedownButtonVisible(true);
            } else {
                if (this.host.canScrollToNewer()) {
                    return;
                }
                this.pagedownShownByScroll = false;
                this.host.setPagedownButtonVisible(false);
            }
        }
    }

    public void onScrolled(int i) {
        if (this.viewportActive && this.host.isListReady() && !this.host.isScrollAnimationRunning()) {
            if (!this.host.canScrollToNewer()) {
                this.totalScrollDy = 0;
                this.pagedownShownByScroll = false;
                this.host.setPagedownButtonVisible(false);
                if (i <= 0 || this.settleAtNewestScheduled) {
                    return;
                }
                this.settleAtNewestScheduled = true;
                AndroidUtilities.runOnUIThread(this.settleAtNewestRunnable);
                return;
            }
            if (this.lastPagedownCount > 0) {
                return;
            }
            boolean isPagedownButtonVisible = this.host.isPagedownButtonVisible();
            if (i > 0) {
                if (isPagedownButtonVisible) {
                    return;
                }
                int i2 = this.totalScrollDy + i;
                this.totalScrollDy = i2;
                if (i2 > PAGEDOWN_SCROLL_THRESHOLD) {
                    this.totalScrollDy = 0;
                    this.pagedownShownByScroll = true;
                    this.host.setPagedownButtonVisible(true);
                    return;
                }
                return;
            }
            if (i < 0 && this.pagedownShownByScroll && isPagedownButtonVisible) {
                int i3 = this.totalScrollDy + i;
                this.totalScrollDy = i3;
                if (i3 < (-PAGEDOWN_SCROLL_THRESHOLD)) {
                    this.totalScrollDy = 0;
                    this.host.setPagedownButtonVisible(false);
                }
            }
        }
    }

    public void onMessagesDeleted() {
        if (this.unreadDivider == null) {
            return;
        }
        ArrayList<MessageObject> messages = this.host.getMessages();
        int indexOf = messages.indexOf(this.unreadDivider);
        for (int i = 0; i < indexOf; i++) {
            if (FeedMessageUtils.isPostRow(messages.get(i))) {
                return;
            }
        }
        this.unreadDivider = null;
        if (indexOf < 0 || !this.host.isListReady()) {
            return;
        }
        messages.remove(indexOf);
        this.host.notifyMessageRemoved(indexOf);
    }

    public void loadReplyMessages(ArrayList<MessageObject> arrayList, int i, int i2) {
        if (arrayList == null || arrayList.isEmpty()) {
            return;
        }
        LongSparseArray longSparseArray = new LongSparseArray();
        for (int i3 = 0; i3 < arrayList.size(); i3++) {
            MessageObject messageObject = arrayList.get(i3);
            if (messageObject != null && !messageObject.isDateObject) {
                long dialogId = messageObject.getDialogId();
                if (dialogId != 0) {
                    ArrayList arrayList2 = (ArrayList) longSparseArray.get(dialogId);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                        longSparseArray.put(dialogId, arrayList2);
                    }
                    arrayList2.add(messageObject);
                }
            }
        }
        for (int i4 = 0; i4 < longSparseArray.size(); i4++) {
            MediaDataController.getInstance(this.currentAccount).loadReplyMessagesForMessages((ArrayList) longSparseArray.valueAt(i4), longSparseArray.keyAt(i4), i, 0L, null, i2, null);
        }
    }

    public void reconcileWithStore() {
        boolean z;
        if (this.host.isListReady()) {
            FeedStore store = FeedController.getInstance(this.currentAccount).getStore();
            ArrayList<MessageObject> messages = this.host.getMessages();
            int i = 0;
            for (int i2 = 0; i2 < messages.size(); i2++) {
                if (FeedMessageUtils.isPostRow(messages.get(i2))) {
                    i++;
                }
            }
            ArrayList<MessageObject> visibleMessages = store.getVisibleMessages();
            if (i == 0) {
                if (!visibleMessages.isEmpty() && this.host.isFirstLoadComplete()) {
                    this.host.reloadFeed();
                    return;
                } else {
                    if (store.isEmpty() || store.isEndReached()) {
                        return;
                    }
                    this.host.requestOlderFeedPage();
                    return;
                }
            }
            if (store.isEmpty()) {
                this.host.reloadFeed();
                return;
            }
            HashSet hashSet = new HashSet(visibleMessages);
            ArrayList<Integer> arrayList = null;
            ArrayList arrayList2 = null;
            for (int i3 = 0; i3 < messages.size(); i3++) {
                MessageObject messageObject = messages.get(i3);
                if (FeedMessageUtils.isPostRow(messageObject) && !hashSet.contains(messageObject)) {
                    if (store.getMessage(messageObject.getDialogId(), messageObject.getId()) == messageObject) {
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList();
                        }
                        arrayList2.add(messageObject);
                    } else {
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                        }
                        arrayList.add(Integer.valueOf(messageObject.getId()));
                    }
                }
            }
            int size = (arrayList == null ? 0 : arrayList.size()) + (arrayList2 == null ? 0 : arrayList2.size());
            if (size == 0 && i == visibleMessages.size()) {
                return;
            }
            ScrollAnchor captureScrollAnchor = this.host.captureScrollAnchor();
            boolean hasPendingInitialPosition = hasPendingInitialPosition();
            if (arrayList != null) {
                this.host.deleteRows(arrayList);
            }
            if (arrayList2 != null) {
                for (int i4 = 0; i4 < arrayList2.size(); i4++) {
                    int indexOf = messages.indexOf(arrayList2.get(i4));
                    if (indexOf >= 0) {
                        messages.remove(indexOf);
                        this.host.dematerializeRow((MessageObject) arrayList2.get(i4));
                        this.host.notifyMessageRemoved(indexOf);
                    }
                }
            }
            HashSet hashSet2 = new HashSet();
            for (int i5 = 0; i5 < messages.size(); i5++) {
                if (FeedMessageUtils.isPostRow(messages.get(i5))) {
                    hashSet2.add(messages.get(i5));
                }
            }
            int i6 = 0;
            boolean z2 = false;
            int i7 = 0;
            while (true) {
                z = true;
                if (i6 >= visibleMessages.size()) {
                    break;
                }
                MessageObject messageObject2 = visibleMessages.get(i6);
                if (hashSet2.contains(messageObject2)) {
                    while (i7 < messages.size() && messages.get(i7) != messageObject2) {
                        i7++;
                    }
                    if (i7 < messages.size()) {
                        i7++;
                    }
                } else {
                    this.host.materializeRow(messageObject2);
                    int insertIndex = getInsertIndex(messages, messageObject2, visibleMessages, i6, i7);
                    messages.add(insertIndex, messageObject2);
                    this.host.notifyMessageInserted(insertIndex);
                    i7 = insertIndex + 1;
                    z2 = true;
                }
                i6++;
            }
            if (size <= 0 && !z2) {
                z = false;
            }
            if (!normalizeDateHeaders(messages) && !z) {
                return;
            }
            this.host.onFeedListChanged();
            applyUnreadDivider(false);
            refreshAds();
            onFeedExclusionsChanged();
            if (!hasPendingInitialPosition || !hasPendingInitialPosition()) {
                this.host.restoreScrollAnchor(captureScrollAnchor);
            }
            this.host.invalidateVisiblePart();
            if (store.getVisibleCount() == 0) {
                boolean isEndReached = store.isEndReached();
                Host host = this.host;
                if (isEndReached) {
                    host.showEmptyFeedState();
                } else {
                    host.showEmptyFeedProgress();
                    this.host.requestOlderFeedPage();
                }
            }
        }
    }

    private static int getInsertIndex(ArrayList<MessageObject> arrayList, MessageObject messageObject, ArrayList<MessageObject> arrayList2, int i, int i2) {
        int i3;
        int min = Math.min(i2, arrayList.size());
        if (i <= 0 || min >= arrayList.size()) {
            return min;
        }
        MessageObject messageObject2 = arrayList2.get(i - 1);
        MessageObject messageObject3 = arrayList.get(min);
        return (messageObject2 == null || messageObject3 == null || !messageObject3.isDateObject || (i3 = messageObject2.dateKeyInt) == messageObject.dateKeyInt || messageObject3.dateKeyInt != i3) ? min : min + 1;
    }

    private boolean normalizeDateHeaders(ArrayList<MessageObject> arrayList) {
        MessageObject messageObject = null;
        int i = 0;
        boolean z = false;
        boolean z2 = false;
        while (i < arrayList.size()) {
            MessageObject messageObject2 = arrayList.get(i);
            if (messageObject2 == null || messageObject2.type == 6 || messageObject2.isSponsored()) {
                i++;
            } else {
                if (messageObject2.isDateObject) {
                    if (!z) {
                        arrayList.remove(i);
                        this.host.notifyMessageRemoved(i);
                        z2 = true;
                    } else if (messageObject != null && messageObject.dateKeyInt != messageObject2.dateKeyInt) {
                        arrayList.add(i, createDateHeader(messageObject));
                        this.host.notifyMessageInserted(i);
                        i++;
                        z = false;
                        messageObject = null;
                        z2 = true;
                    }
                } else if (messageObject == null || messageObject.dateKeyInt == messageObject2.dateKeyInt) {
                    i++;
                    messageObject = messageObject2;
                    z = true;
                } else {
                    arrayList.add(i, createDateHeader(messageObject));
                    this.host.notifyMessageInserted(i);
                    i++;
                    z = false;
                    messageObject = null;
                    z2 = true;
                }
            }
        }
        if (!z || messageObject == null) {
            return z2;
        }
        arrayList.add(createDateHeader(messageObject));
        this.host.notifyMessageInserted(arrayList.size() - 1);
        return true;
    }

    private MessageObject createDateHeader(MessageObject messageObject) {
        return FeedMessageUtils.createDateHeader(this.currentAccount, messageObject, this.host.stableIdForDateHeader(messageObject.dateKeyInt));
    }

    public ArrayList<Integer> collectLocalRowIds(long j, ArrayList<Integer> arrayList, int i) {
        boolean z;
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        HashSet hashSet = arrayList != null ? new HashSet(arrayList) : null;
        ArrayList<MessageObject> messages = this.host.getMessages();
        for (int i2 = 0; i2 < messages.size(); i2++) {
            MessageObject messageObject = messages.get(i2);
            if (FeedMessageUtils.isPostRow(messageObject) && messageObject.getDialogId() == j) {
                int realId = messageObject.getRealId();
                if (hashSet != null) {
                    z = hashSet.contains(Integer.valueOf(realId));
                } else {
                    z = realId > 0 && realId <= i;
                }
                if (z) {
                    arrayList2.add(Integer.valueOf(messageObject.getId()));
                }
            }
        }
        return arrayList2;
    }

    public static void mergeDeletedIds(ArrayList<Integer> arrayList, ArrayList<Integer> arrayList2) {
        for (int i = 0; i < arrayList2.size(); i++) {
            if (!arrayList.contains(arrayList2.get(i))) {
                arrayList.add(arrayList2.get(i));
            }
        }
    }

    public void hideChannelWithUndo(final long j, CharSequence charSequence) {
        FeedConfig feedConfig = FeedConfig.getInstance(this.currentAccount);
        FeedController feedController = FeedController.getInstance(this.currentAccount);
        feedConfig.setExcluded(j, true);
        feedController.markConfigApplied();
        feedController.getStore().setHidden(j, true);
        this.pendingHideDialogId = j;
        reconcileWithStore();
        onFeedExclusionsChanged();
        notifyChannelsChanged();
        BulletinFactory.of(this.host.getFragment()).createUndoBulletin(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.FeedChannelHidden, charSequence)), new Runnable() { // from class: com.exteragram.messenger.feed.FeedChatIntegration$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                FeedChatIntegration.this.undoHideChannel();
            }
        }, new Runnable() { // from class: com.exteragram.messenger.feed.FeedChatIntegration$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                FeedChatIntegration.this.lambda$hideChannelWithUndo$1(j);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$hideChannelWithUndo$1(long j) {
        if (this.pendingHideDialogId == j) {
            this.pendingHideDialogId = 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void undoHideChannel() {
        long j = this.pendingHideDialogId;
        if (j == 0) {
            return;
        }
        this.pendingHideDialogId = 0L;
        FeedController feedController = FeedController.getInstance(this.currentAccount);
        FeedConfig.getInstance(this.currentAccount).setExcluded(j, false);
        feedController.markConfigApplied();
        feedController.getStore().setHidden(j, false);
        reconcileWithStore();
        onFeedExclusionsChanged();
        notifyChannelsChanged();
    }

    public void onFeedExclusionsChanged() {
        this.lastPagedownCount = -1;
        updatePagedownCounter();
        NotificationCenter.getInstance(this.currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE);
    }
}