package org.telegram.messenger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import androidx.collection.LongSparseArray;
import androidx.core.util.Consumer;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

public class HundredGramBackgroundArchiver implements NotificationCenter.NotificationCenterDelegate {

    private static volatile HundredGramBackgroundArchiver[] Instance = new HundredGramBackgroundArchiver[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramBackgroundArchiver getInstance(int num) {
        HundredGramBackgroundArchiver localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (HundredGramBackgroundArchiver.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new HundredGramBackgroundArchiver(num);
                }
            }
        }
        return localInstance;
    }

    private final int currentAccount;
    private boolean started;
    private boolean isSyncing;
    private long lastSyncTime;
    public static class ScanProgress {
        public int currentChat;
        public int totalChats;
        public int mediaEnqueued;
        public boolean completed;
        public long currentDialogId;
    }

    private boolean isDeepScanningAll;
    private final ArrayList<Long> queue = new ArrayList<>();
    private final HashSet<Long> queuedSet = new HashSet<>();
    private final LongSparseArray<Integer> lastPolledTopMessageId = new LongSparseArray<>();
    private static class PendingArchiverMedia {
        final TLRPC.Message message;
        final long dialogId;
        final String attachName;

        PendingArchiverMedia(TLRPC.Message message, long dialogId, String attachName) {
            this.message = message;
            this.dialogId = dialogId;
            this.attachName = attachName;
        }
    }

    private final ConcurrentHashMap<String, PendingArchiverMedia> pendingArchiverDownloads = new ConcurrentHashMap<>();
    private final Runnable processQueueRunnable = this::processNext;
    private final Runnable periodicSyncRunnable = () -> triggerSync(false);

    private HundredGramBackgroundArchiver(int account) {
        this.currentAccount = account;
    }

    public void start() {
        if (started) {
            return;
        }
        if (!HundredGramConfig.backgroundArchiverEnabled) {
            return;
        }
        started = true;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didReceiveNewMessages);

        if (HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_FULL_SYNC) {
            // Delayed initial sync after startup (10 seconds) only in full sync mode
            AndroidUtilities.runOnUIThread(() -> triggerSync(false), 10000);
        }
    }

    public void stop() {
        started = false;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didReceiveNewMessages);
        AndroidUtilities.cancelRunOnUIThread(processQueueRunnable);
        AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
        queue.clear();
        queuedSet.clear();
        lastPolledTopMessageId.clear();
        pendingArchiverDownloads.clear();
        isSyncing = false;
        isDeepScanningAll = false;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateConnectionState) {
            int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
            if (state == ConnectionsManager.ConnectionStateConnected && HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_FULL_SYNC) {
                AndroidUtilities.runOnUIThread(() -> triggerSync(false), 8000);
            }
        } else if (id == NotificationCenter.fileLoaded) {
            String location = (String) args[0];
            File file = (File) args[1];
            if (location != null && file != null && file.exists() && file.length() > 0) {
                PendingArchiverMedia pending = pendingArchiverDownloads.remove(location);
                if (pending == null) {
                    pending = pendingArchiverDownloads.remove(file.getName());
                }
                if (pending != null) {
                    FileLog.d("HundredGramArchiver: fileLoaded intercepted for " + location + " (path=" + file.getAbsolutePath() + ")");
                    HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(pending.message, file.getAbsolutePath());
                }
            }
        } else if (id == NotificationCenter.fileLoadFailed) {
            String location = (String) args[0];
            if (location != null) {
                PendingArchiverMedia pending = pendingArchiverDownloads.remove(location);
                if (pending != null) {
                    FileLog.d("HundredGramArchiver: archiver pending fileLoadFailed for " + location);
                }
            }
        } else if (id == NotificationCenter.didReceiveNewMessages) {
            if (!HundredGramConfig.backgroundArchiverEnabled) {
                return;
            }
            if (args != null && args.length > 2 && args[2] instanceof Boolean) {
                Boolean scheduled = (Boolean) args[2];
                if (scheduled != null && scheduled) {
                    return;
                }
            }
            if (args != null && args.length > 1 && args[1] instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<MessageObject> messages = (ArrayList<MessageObject>) args[1];
                if (messages != null && !messages.isEmpty()) {
                    for (int i = 0; i < messages.size(); i++) {
                        MessageObject obj = messages.get(i);
                        if (obj != null && obj.messageOwner != null) {
                            processIncomingMessage(obj.messageOwner);
                        }
                    }
                }
            }
        }
    }

    public void triggerSync(boolean force) {
        if (!HundredGramConfig.backgroundArchiverEnabled || !UserConfig.getInstance(currentAccount).isClientActivated()) {
            scheduleNextPeriodicSync();
            return;
        }

        // В режиме "Только новые сообщения" фоновое сканирование истории чатов отключено
        if (!force && HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_NEW_ONLY) {
            scheduleNextPeriodicSync();
            return;
        }

        if (force) {
            lastPolledTopMessageId.clear();
        }

        long now = System.currentTimeMillis();
        if (!force && (now - lastSyncTime < 15 * 60 * 1000L)) {
            // Already synced recently
            scheduleNextPeriodicSync();
            return;
        }

        if (isSyncing) {
            return;
        }

        lastSyncTime = now;

        MessagesController mc = MessagesController.getInstance(currentAccount);
        if (mc.dialogs_dict == null || mc.dialogs_dict.isEmpty()) {
            MessagesStorage.getInstance(currentAccount).getDialogs(0, 0, 100, true);
        }

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            ArrayList<Long> dids = new ArrayList<>();
            ArrayList<Integer> folderIds = new ArrayList<>();
            ArrayList<Integer> topMessages = new ArrayList<>();
            SQLiteCursor cursor = null;
            try {
                cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT did, folder_id, last_mid FROM dialogs ORDER BY date DESC");
                while (cursor.next()) {
                    long did = cursor.longValue(0);
                    int fid = cursor.intValue(1);
                    int lastMid = cursor.intValue(2);
                    dids.add(did);
                    folderIds.add(fid);
                    topMessages.add(lastMid);
                    HundredGramConfig.setDialogFolder(did, fid);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) cursor.dispose();
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!started || !HundredGramConfig.backgroundArchiverEnabled) {
                    return;
                }
                queue.clear();
                queuedSet.clear();

                for (int i = 0; i < dids.size(); i++) {
                    long did = dids.get(i);
                    int folderId = folderIds.get(i);
                    int topMsg = topMessages.get(i);

                    if (DialogObject.isFolderDialogId(did)) {
                        continue;
                    }

                    if (queuedSet.contains(did)) {
                        continue;
                    }

                    if (folderId == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
                        continue;
                    }

                    int archiveOverride = HundredGramConfig.getChatOverride(did, "auto_archive");
                    if (archiveOverride == HundredGramConfig.OVERRIDE_DISABLED) {
                        continue;
                    }
                    int mode = HundredGramConfig.getChatVaultMode(did);
                    if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
                        continue;
                    }
                    if (mode == HundredGramConfig.VAULT_MODE_ALWAYS || archiveOverride == HundredGramConfig.OVERRIDE_ENABLED || HundredGramConfig.canVaultDownload(did)) {
                        Integer lastTop = lastPolledTopMessageId.get(did);
                        if (!force && lastTop != null && topMsg > 0 && topMsg <= lastTop) {
                            // Dialog already scanned and has no new messages
                            continue;
                        }
                        queue.add(did);
                        queuedSet.add(did);
                    }
                }

                MessagesController controller = MessagesController.getInstance(currentAccount);

                // Also check if any in-memory dialogs weren't in the database query
                if (controller.dialogs_dict != null) {
                    for (int i = 0; i < controller.dialogs_dict.size(); i++) {
                        TLRPC.Dialog d = controller.dialogs_dict.valueAt(i);
                        if (d != null) {
                            addEligibleDialog(d);
                        }
                    }
                }

                if (queue.isEmpty()) {
                    scheduleNextPeriodicSync();
                    return;
                }

                isSyncing = true;
                AndroidUtilities.runOnUIThread(processQueueRunnable, 1000);
            });
        });
    }

    private void addEligibleDialogs(ArrayList<TLRPC.Dialog> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            addEligibleDialog(list.get(i));
        }
    }

    private void addEligibleDialog(TLRPC.Dialog d) {
        if (d == null || queuedSet.contains(d.id) || DialogObject.isFolderDialogId(d.id)) {
            return;
        }
        HundredGramConfig.setDialogFolder(d.id, d.folder_id);
        if (d.folder_id == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
            return;
        }
        int archiveOverride = HundredGramConfig.getChatOverride(d.id, "auto_archive");
        if (archiveOverride == HundredGramConfig.OVERRIDE_DISABLED) {
            return;
        }
        int mode = HundredGramConfig.getChatVaultMode(d.id);
        if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
            return;
        }
        if (mode == HundredGramConfig.VAULT_MODE_ALWAYS || archiveOverride == HundredGramConfig.OVERRIDE_ENABLED || HundredGramConfig.canVaultDownload(d.id)) {
            Integer lastTop = lastPolledTopMessageId.get(d.id);
            if (lastTop != null && d.top_message > 0 && d.top_message <= lastTop) {
                return;
            }
            queue.add(d.id);
            queuedSet.add(d.id);
        }
    }

    public TLRPC.InputPeer getInputPeerForDialog(long dialogId) {
        MessagesController controller = MessagesController.getInstance(currentAccount);
        if (dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return new TLRPC.TL_inputPeerSelf();
        }
        if (dialogId < 0) {
            long chatId = -dialogId;
            TLRPC.Chat chat = controller.getChat(chatId);
            if (chat == null) {
                chat = controller.getMessagesStorage().getChat(chatId);
                if (chat != null) {
                    controller.putChat(chat, true);
                }
            }
            if (chat != null) {
                return MessagesController.getInputPeer(chat);
            }
            return controller.getInputPeer(dialogId);
        } else {
            TLRPC.User user = controller.getUser(dialogId);
            if (user == null) {
                user = controller.getMessagesStorage().getUser(dialogId);
                if (user != null) {
                    controller.putUser(user, true);
                }
            }
            if (user != null) {
                return MessagesController.getInputPeer(user);
            }
            return controller.getInputPeer(dialogId);
        }
    }

    private void processNext() {
        if (!started || !HundredGramConfig.backgroundArchiverEnabled || queue.isEmpty()) {
            isSyncing = false;
            scheduleNextPeriodicSync();
            return;
        }

        // If Telegram is currently syncing (ConnectionStateUpdating), yield to let it finish!
        if (ConnectionsManager.getInstance(currentAccount).getConnectionState() == ConnectionsManager.ConnectionStateUpdating) {
            AndroidUtilities.runOnUIThread(processQueueRunnable, 2000);
            return;
        }

        // Backpressure: If 50+ media items are currently downloading, pause to avoid flooding FileLoader
        if (pendingArchiverDownloads.size() >= 50) {
            AndroidUtilities.runOnUIThread(processQueueRunnable, 3000);
            return;
        }

        long dialogId = queue.remove(0);
        MessagesController controller = MessagesController.getInstance(currentAccount);

        TLRPC.Dialog dialog = controller.dialogs_dict != null ? controller.dialogs_dict.get(dialogId) : null;
        if (dialog != null && dialog.top_message > 0) {
            Integer lastTop = lastPolledTopMessageId.get(dialogId);
            if (lastTop != null && dialog.top_message <= lastTop) {
                // No new messages in this chat, skip without querying MTProto
                AndroidUtilities.runOnUIThread(processQueueRunnable, 20);
                return;
            }
        }

        TLRPC.InputPeer peer = getInputPeerForDialog(dialogId);
        if (peer == null) {
            AndroidUtilities.runOnUIThread(processQueueRunnable, 100);
            return;
        }

        TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
        req.peer = peer;
        req.limit = 25;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                FileLog.e("HundredGramBackgroundArchiver sendRequest error: " + error.code + " " + error.text);
                if (error.text != null && error.text.startsWith("FLOOD_WAIT_")) {
                    int waitSeconds = 60;
                    try {
                        waitSeconds = Integer.parseInt(error.text.replaceAll("[^0-9]", ""));
                    } catch (Throwable ignore) {}
                    waitSeconds = Math.max(10, Math.min(waitSeconds, 3600));
                    isSyncing = false;
                    queue.clear();
                    queuedSet.clear();
                    AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
                    AndroidUtilities.runOnUIThread(periodicSyncRunnable, (waitSeconds + 5) * 1000L);
                    return;
                }
                int jitterDelay = 500 + Utilities.random.nextInt(1000);
                AndroidUtilities.runOnUIThread(processQueueRunnable, jitterDelay);
                return;
            }
            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                if (res.messages != null) {
                    if (res.users != null) {
                        controller.putUsers(res.users, false);
                    }
                    if (res.chats != null) {
                        controller.putChats(res.chats, false);
                    }
                    // NOTE: Do not call controller.getMessagesStorage().putMessages(...) here.
                    // Storing history in DB triggers dialogsNeedReload and floods SQLite transactions.
                    if (HundredGramConfig.canVaultDownload(dialogId)) {
                        int mediaCount = 0;
                        ArrayList<TLRPC.Message> postsWithComments = new ArrayList<>();
                        for (int m = 0; m < res.messages.size(); m++) {
                            TLRPC.Message msg = res.messages.get(m);
                            if (msg != null && msg.dialog_id == 0) {
                                msg.dialog_id = dialogId;
                            }
                            // Cap max new downloads per dialog pass to 15 to ensure fair scheduling across groups
                            if (mediaCount < 15 && checkMediaAndDownload(msg, dialogId, false)) {
                                mediaCount++;
                            }
                            if (HundredGramConfig.vaultArchiveChannelComments && msg != null && msg.replies != null && msg.replies.replies > 0 && postsWithComments.size() < 2) {
                                postsWithComments.add(msg);
                            }
                        }
                        if (mediaCount > 0) {
                            FileLog.d("HundredGramArchiver: found " + mediaCount + " media items to download for dialog " + dialogId);
                        }
                        if (!postsWithComments.isEmpty()) {
                            scanCommentsForPosts(dialogId, postsWithComments, 0, false, null, () -> {
                                int jitterDelay = 300 + Utilities.random.nextInt(500);
                                AndroidUtilities.runOnUIThread(processQueueRunnable, jitterDelay);
                            });
                            return;
                        }
                    }
                }
                if (dialog != null && dialog.top_message > 0) {
                    lastPolledTopMessageId.put(dialogId, dialog.top_message);
                } else if (res.messages != null && !res.messages.isEmpty()) {
                    int maxId = 0;
                    for (int i = 0; i < res.messages.size(); i++) {
                        maxId = Math.max(maxId, res.messages.get(i).id);
                    }
                    if (maxId > 0) {
                        lastPolledTopMessageId.put(dialogId, maxId);
                    }
                }
            }
            int jitterDelay = 300 + Utilities.random.nextInt(500);
            AndroidUtilities.runOnUIThread(processQueueRunnable, jitterDelay);
        });
    }

    public void processIncomingMessage(TLRPC.Message msg) {
        if (msg == null || !HundredGramConfig.backgroundArchiverEnabled) {
            return;
        }
        long dialogId = msg.dialog_id != 0 ? msg.dialog_id : MessageObject.getDialogId(msg);
        if (DialogObject.isFolderDialogId(dialogId)) {
            return;
        }
        if (msg.dialog_id == 0) {
            msg.dialog_id = dialogId;
        }
        int folderId = HundredGramConfig.getDialogFolderId(dialogId);
        if (folderId == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
            return;
        }
        int archiveOverride = HundredGramConfig.getChatOverride(dialogId, "auto_archive");
        if (archiveOverride == HundredGramConfig.OVERRIDE_DISABLED) {
            return;
        }
        int mode = HundredGramConfig.getChatVaultMode(dialogId);
        if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
            return;
        }
        // Mode 0: Only messages received after archiver activation
        if (HundredGramConfig.backgroundArchiverMode == HundredGramConfig.ARCHIVER_MODE_NEW_ONLY) {
            if (HundredGramConfig.backgroundArchiverActivationTime > 0 && msg.date < HundredGramConfig.backgroundArchiverActivationTime) {
                return;
            }
        }
        if (mode == HundredGramConfig.VAULT_MODE_ALWAYS || archiveOverride == HundredGramConfig.OVERRIDE_ENABLED || HundredGramConfig.canVaultDownload(dialogId, msg, folderId)) {
            checkMediaAndDownload(msg, dialogId, false);
        }
    }

    public void onNewMessage(long dialogId) {
        lastPolledTopMessageId.remove(dialogId);
    }

    private void scheduleNextPeriodicSync() {
        AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
        AndroidUtilities.runOnUIThread(periodicSyncRunnable, 20 * 60 * 1000L);
    }

    private boolean checkMediaAndDownload(TLRPC.Message msg, long dialogId, boolean force) {
        if (msg == null || msg.media == null || msg.media instanceof TLRPC.TL_messageMediaEmpty) {
            return false;
        }
        if (msg.dialog_id == 0) {
            msg.dialog_id = dialogId;
        }
        // Skip stickers and animated stickers entirely
        if (MessageObject.isStickerMessage(msg) || MessageObject.isAnimatedStickerMessage(msg)) {
            return false;
        }
        if (!force && !HundredGramConfig.canVaultDownload(dialogId, msg)) {
            return false;
        }
        if (force) {
            int mode = HundredGramConfig.getChatVaultMode(dialogId);
            if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
                return false;
            }
            if (MessageObject.isPhoto(msg)) {
                if (!HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_PHOTO)) {
                    return false;
                }
            } else if (MessageObject.isVideoMessage(msg)) {
                if (!HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VIDEO)) {
                    return false;
                }
            } else if (MessageObject.isVoiceMessage(msg) || MessageObject.isRoundVideoMessage(msg)) {
                if (!HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_VOICE_ROUND)) {
                    return false;
                }
            } else if (msg.media != null && msg.media.document != null) {
                if (!HundredGramConfig.isChatVaultTypeAllowed(dialogId, HundredGramConfig.VAULT_TYPE_DOCUMENT)) {
                    return false;
                }
            }
        }

        File vaultFile = HundredGramVaultManager.getInstance(currentAccount).getVaultFile(msg);
        if (vaultFile != null && vaultFile.exists() && vaultFile.length() > 0) {
            return false;
        }

        // 1. Photos
        if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null && msg.media.photo.sizes != null && !msg.media.photo.sizes.isEmpty()) {
            TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize(true), false, null, true);
            if (size == null) {
                size = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize());
            }
            if (size != null) {
                String attachName = FileLoader.getAttachFileName(size, "jpg");
                File vf = HundredGramVaultManager.getInstance(currentAccount).getVaultFileByName(attachName);
                if (vf != null && vf.exists() && vf.length() > 0) {
                    return false;
                }
                File checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(size, "jpg", true);
                if (checkFile == null || !checkFile.exists() || checkFile.length() <= 0) {
                    checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(size, "jpg", false);
                }
                if (checkFile == null || !checkFile.exists() || checkFile.length() <= 0) {
                    File msgFile = FileLoader.getInstance(currentAccount).getPathToMessage(msg);
                    if (msgFile != null && msgFile.exists() && msgFile.length() > 0) {
                        checkFile = msgFile;
                    }
                }
                if (checkFile != null && checkFile.exists() && checkFile.length() > 0) {
                    HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(msg, checkFile.getAbsolutePath());
                    return false;
                } else {
                    if (pendingArchiverDownloads.putIfAbsent(attachName, new PendingArchiverMedia(msg, dialogId, attachName)) == null) {
                        final TLRPC.Photo finalPhoto = msg.media.photo;
                        final TLRPC.PhotoSize finalSize = size;
                        AndroidUtilities.runOnUIThread(() -> {
                            FileLoader.getInstance(currentAccount).loadFile(
                                    ImageLocation.getForPhoto(finalSize, finalPhoto),
                                    new MessageObject(currentAccount, msg, false, false),
                                    "jpg",
                                    FileLoader.PRIORITY_LOW,
                                    1
                            );
                        });
                        FileLog.d("HundredGramArchiver: queueing photo load for msg " + msg.id + " in dialog " + dialogId + " (" + attachName + ")");
                        return true;
                    }
                    return false;
                }
            }
        }
        // 2. Documents, Voice, Round Video, Video
        else if (msg.media.document != null) {
            int maxMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
            if (maxMb == 0) {
                maxMb = HundredGramConfig.autoDownloadMaxSizeMb;
            }
            long maxLimit = (maxMb > 0 && !force)
                    ? (long) maxMb * 1024L * 1024L
                    : Long.MAX_VALUE;
            if (msg.media.document.size <= maxLimit) {
                String attachName = FileLoader.getAttachFileName(msg.media.document);
                File vf = HundredGramVaultManager.getInstance(currentAccount).getVaultFileByName(attachName);
                if (vf != null && vf.exists() && vf.length() > 0) {
                    return false;
                }
                File checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(msg.media.document, true);
                if (checkFile == null || !checkFile.exists() || checkFile.length() <= 0) {
                    checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(msg.media.document, false);
                }
                if (checkFile == null || !checkFile.exists() || checkFile.length() <= 0) {
                    File msgFile = FileLoader.getInstance(currentAccount).getPathToMessage(msg);
                    if (msgFile != null && msgFile.exists() && msgFile.length() > 0) {
                        checkFile = msgFile;
                    }
                }
                if (checkFile != null && checkFile.exists() && checkFile.length() > 0) {
                    HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(msg, checkFile.getAbsolutePath());
                    return false;
                } else {
                    if (pendingArchiverDownloads.putIfAbsent(attachName, new PendingArchiverMedia(msg, dialogId, attachName)) == null) {
                        final TLRPC.Document doc = msg.media.document;
                        AndroidUtilities.runOnUIThread(() -> {
                            FileLoader.getInstance(currentAccount).loadFile(
                                    doc,
                                    new MessageObject(currentAccount, msg, false, false),
                                    FileLoader.PRIORITY_LOW,
                                    0
                            );
                        });
                        FileLog.d("HundredGramArchiver: queueing doc load for msg " + msg.id + " in dialog " + dialogId + " (" + attachName + ", size=" + msg.media.document.size + ")");
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public void forceScanDialog(long dialogId, int messageLimit, Utilities.Callback<Integer> onComplete) {
        final int effectiveLimit = messageLimit <= 0 ? Integer.MAX_VALUE : messageLimit;

        final int[] totalScanned = new int[]{0};
        final int[] totalEnqueued = new int[]{0};
        final int[] currentOffsetId = new int[]{0};
        final int batchSize = 100;

        final Runnable[] requestNextBatch = new Runnable[1];
        requestNextBatch[0] = () -> {
            if (totalScanned[0] >= effectiveLimit) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (onComplete != null) {
                        onComplete.run(totalEnqueued[0]);
                    }
                });
                return;
            }

            MessagesController controller = MessagesController.getInstance(currentAccount);
            TLRPC.InputPeer peer = getInputPeerForDialog(dialogId);

            if (peer == null) {
                FileLog.e("HundredGramArchiver forceScanDialog: could not get input peer for dialog " + dialogId);
                AndroidUtilities.runOnUIThread(() -> {
                    if (onComplete != null) {
                        onComplete.run(totalEnqueued[0]);
                    }
                });
                return;
            }

            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = peer;
            req.offset_id = currentOffsetId[0];
            int remaining = effectiveLimit == Integer.MAX_VALUE ? batchSize : (effectiveLimit - totalScanned[0]);
            req.limit = Math.min(batchSize, Math.max(1, remaining));

            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (error != null) {
                    FileLog.e("HundredGramArchiver forceScanDialog error: " + error.code + " " + error.text);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onComplete != null) {
                            onComplete.run(totalEnqueued[0]);
                        }
                    });
                    return;
                }

                if (!(response instanceof TLRPC.messages_Messages)) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onComplete != null) {
                            onComplete.run(totalEnqueued[0]);
                        }
                    });
                    return;
                }

                TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                if (res.users != null) {
                    controller.putUsers(res.users, false);
                }
                if (res.chats != null) {
                    controller.putChats(res.chats, false);
                }
                if (res.messages == null || res.messages.isEmpty()) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (onComplete != null) {
                            onComplete.run(totalEnqueued[0]);
                        }
                    });
                    return;
                }

                if (controller.getMessagesStorage() != null && controller.getDownloadController() != null) {
                    controller.getMessagesStorage().putMessages(
                            res.messages,
                            true,
                            true,
                            false,
                            controller.getDownloadController().getAutodownloadMask(),
                            0,
                            0
                    );
                }

                int minId = Integer.MAX_VALUE;
                ArrayList<TLRPC.Message> postsWithComments = new ArrayList<>();
                for (int i = 0; i < res.messages.size(); i++) {
                    TLRPC.Message msg = res.messages.get(i);
                    if (msg == null) continue;
                    totalScanned[0]++;
                    if (msg.id > 0 && msg.id < minId) {
                        minId = msg.id;
                    }
                    if (checkMediaAndDownload(msg, dialogId, true)) {
                        totalEnqueued[0]++;
                    }
                    if (HundredGramConfig.vaultArchiveChannelComments && msg.replies != null && msg.replies.replies > 0) {
                        postsWithComments.add(msg);
                    }
                }

                final int finalMinId = minId;
                Runnable continueToNextBatch = () -> {
                    if (finalMinId == Integer.MAX_VALUE || finalMinId == currentOffsetId[0] || res.messages.size() < req.limit || totalScanned[0] >= effectiveLimit) {
                        AndroidUtilities.runOnUIThread(() -> {
                            if (onComplete != null) {
                                onComplete.run(totalEnqueued[0]);
                            }
                        });
                        return;
                    }

                    currentOffsetId[0] = finalMinId;
                    Utilities.stageQueue.postRunnable(requestNextBatch[0], 500);
                };

                if (postsWithComments.isEmpty()) {
                    continueToNextBatch.run();
                } else {
                    scanCommentsForPosts(dialogId, postsWithComments, 0, true, totalEnqueued, continueToNextBatch);
                }
            });
        };

        Utilities.stageQueue.postRunnable(requestNextBatch[0]);
    }

    public void forceScanDialog(long dialogId, int messageLimit, Runnable onComplete) {
        forceScanDialog(dialogId, messageLimit, (Utilities.Callback<Integer>) (queuedCount) -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void forceDeepScanAll(int messagesPerChat, Consumer<ScanProgress> progressCallback) {
        if (isDeepScanningAll) {
            FileLog.d("HundredGramArchiver: forceDeepScanAll already in progress");
            return;
        }
        isDeepScanningAll = true;

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            ArrayList<Long> dids = new ArrayList<>();
            ArrayList<Integer> folderIds = new ArrayList<>();
            SQLiteCursor cursor = null;
            try {
                cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT did, folder_id FROM dialogs");
                while (cursor.next()) {
                    long did = cursor.longValue(0);
                    int fid = cursor.intValue(1);
                    dids.add(did);
                    folderIds.add(fid);
                    HundredGramConfig.setDialogFolder(did, fid);
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                MessagesController controller = MessagesController.getInstance(currentAccount);
                HashSet<Long> addedSet = new HashSet<>();
                ArrayList<Long> targetDialogs = new ArrayList<>();

                for (int i = 0; i < dids.size(); i++) {
                    long did = dids.get(i);
                    int fid = folderIds.get(i);
                    if (DialogObject.isFolderDialogId(did)) {
                        continue;
                    }
                    if (addedSet.contains(did)) {
                        continue;
                    }
                    if (fid == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
                        continue;
                    }
                    if (HundredGramConfig.getChatVaultMode(did) == HundredGramConfig.VAULT_MODE_NEVER) {
                        continue;
                    }
                    if (HundredGramConfig.getChatVaultMode(did) == HundredGramConfig.VAULT_MODE_ALWAYS || HundredGramConfig.canVaultDownload(did)) {
                        addedSet.add(did);
                        targetDialogs.add(did);
                    }
                }

                if (controller.dialogs_dict != null) {
                    for (int i = 0; i < controller.dialogs_dict.size(); i++) {
                        TLRPC.Dialog d = controller.dialogs_dict.valueAt(i);
                        if (d != null && !addedSet.contains(d.id) && !DialogObject.isFolderDialogId(d.id)) {
                            HundredGramConfig.setDialogFolder(d.id, d.folder_id);
                            if (d.folder_id == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
                                continue;
                            }
                            if (HundredGramConfig.getChatVaultMode(d.id) == HundredGramConfig.VAULT_MODE_NEVER) {
                                continue;
                            }
                            if (HundredGramConfig.getChatVaultMode(d.id) == HundredGramConfig.VAULT_MODE_ALWAYS || HundredGramConfig.canVaultDownload(d.id)) {
                                addedSet.add(d.id);
                                targetDialogs.add(d.id);
                            }
                        }
                    }
                }

                if (targetDialogs.isEmpty()) {
                    isDeepScanningAll = false;
                    ScanProgress p = new ScanProgress();
                    p.completed = true;
                    if (progressCallback != null) {
                        progressCallback.accept(p);
                    }
                    return;
                }

                ScanProgress progress = new ScanProgress();
                progress.totalChats = targetDialogs.size();
                progress.currentChat = 0;
                progress.mediaEnqueued = 0;

                scanNextInDeepScanAll(0, targetDialogs, messagesPerChat, progress, progressCallback);
            });
        });
    }

    private void scanNextInDeepScanAll(int index, ArrayList<Long> targetDialogs, int messagesPerChat, ScanProgress progress, Consumer<ScanProgress> progressCallback) {
        if (index >= targetDialogs.size()) {
            isDeepScanningAll = false;
            progress.completed = true;
            AndroidUtilities.runOnUIThread(() -> {
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            });
            return;
        }

        long dialogId = targetDialogs.get(index);
        progress.currentChat = index + 1;
        progress.currentDialogId = dialogId;
        AndroidUtilities.runOnUIThread(() -> {
            if (progressCallback != null) {
                progressCallback.accept(progress);
            }
        });

        forceScanDialog(dialogId, messagesPerChat, (Utilities.Callback<Integer>) (queuedCount) -> {
            progress.mediaEnqueued += queuedCount;
            AndroidUtilities.runOnUIThread(() -> {
                if (progressCallback != null) {
                    progressCallback.accept(progress);
                }
            });
            Utilities.stageQueue.postRunnable(() -> {
                scanNextInDeepScanAll(index + 1, targetDialogs, messagesPerChat, progress, progressCallback);
            }, 500);
        });
    }

    private void scanCommentsForPosts(long channelDialogId, ArrayList<TLRPC.Message> posts, int index, boolean force, int[] totalEnqueued, Runnable onDone) {
        if (index >= posts.size()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        TLRPC.Message post = posts.get(index);
        if (post == null || post.replies == null || post.replies.replies <= 0) {
            scanCommentsForPosts(channelDialogId, posts, index + 1, force, totalEnqueued, onDone);
            return;
        }

        MessagesController controller = MessagesController.getInstance(currentAccount);
        TLRPC.InputPeer peer = getInputPeerForDialog(channelDialogId);
        if (peer == null) {
            scanCommentsForPosts(channelDialogId, posts, index + 1, force, totalEnqueued, onDone);
            return;
        }

        TLRPC.TL_messages_getReplies req = new TLRPC.TL_messages_getReplies();
        req.peer = peer;
        req.msg_id = post.id;
        req.limit = force ? 100 : 20;
        req.offset_id = 0;

        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
            if (error != null) {
                FileLog.d("HundredGramArchiver: getReplies error for msg " + post.id + ": " + error.code + " " + error.text);
                if (error.text != null && error.text.startsWith("FLOOD_WAIT_")) {
                    if (onDone != null) {
                        onDone.run();
                    }
                    return;
                }
                Utilities.stageQueue.postRunnable(() -> {
                    scanCommentsForPosts(channelDialogId, posts, index + 1, force, totalEnqueued, onDone);
                }, 300);
                return;
            }

            if (response instanceof TLRPC.messages_Messages) {
                TLRPC.messages_Messages resReplies = (TLRPC.messages_Messages) response;
                if (resReplies.users != null) {
                    controller.putUsers(resReplies.users, false);
                }
                if (resReplies.chats != null) {
                    controller.putChats(resReplies.chats, false);
                }
                if (resReplies.messages != null && !resReplies.messages.isEmpty()) {
                    for (int j = 0; j < resReplies.messages.size(); j++) {
                        TLRPC.Message commentMsg = resReplies.messages.get(j);
                        if (commentMsg == null) continue;
                        long commentDid = MessageObject.getDialogId(commentMsg);
                        if (commentDid == 0) {
                            if (post.replies != null && post.replies.channel_id != 0) {
                                commentDid = -post.replies.channel_id;
                            } else {
                                commentDid = channelDialogId;
                            }
                            commentMsg.dialog_id = commentDid;
                        }
                        if (checkMediaAndDownload(commentMsg, commentDid, force)) {
                            if (totalEnqueued != null && totalEnqueued.length > 0) {
                                totalEnqueued[0]++;
                            }
                        }
                    }
                }
            }

            Utilities.stageQueue.postRunnable(() -> {
                scanCommentsForPosts(channelDialogId, posts, index + 1, force, totalEnqueued, onDone);
            }, 300);
        });
    }
}
