package org.telegram.messenger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import androidx.collection.LongSparseArray;

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
        started = true;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoadFailed);

        // Delayed initial sync after startup (8 seconds)
        AndroidUtilities.runOnUIThread(() -> triggerSync(false), 8000);
    }

    public void stop() {
        started = false;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.didUpdateConnectionState);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
        AndroidUtilities.cancelRunOnUIThread(processQueueRunnable);
        AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
        queue.clear();
        queuedSet.clear();
        lastPolledTopMessageId.clear();
        pendingArchiverDownloads.clear();
        isSyncing = false;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            ensureArchiveFolderLoaded();
        } else if (id == NotificationCenter.didUpdateConnectionState) {
            int state = ConnectionsManager.getInstance(currentAccount).getConnectionState();
            if (state == ConnectionsManager.ConnectionStateConnected) {
                ensureArchiveFolderLoaded();
                AndroidUtilities.runOnUIThread(() -> triggerSync(false), 5000);
            }
        } else if (id == NotificationCenter.fileLoaded) {
            String location = (String) args[0];
            File file = (File) args[1];
            if (location != null && file != null && file.exists() && file.length() > 0) {
                PendingArchiverMedia pending = pendingArchiverDownloads.remove(location);
                if (pending != null) {
                    FileLog.d("HundredGramArchiver: fileLoaded intercepted for " + location + " (path=" + file.getAbsolutePath() + ")");
                    HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(pending.message, file.getAbsolutePath());
                }
            }
        } else if (id == NotificationCenter.fileLoadFailed) {
            String location = (String) args[0];
            if (location != null) {
                pendingArchiverDownloads.remove(location);
                FileLog.d("HundredGramArchiver: fileLoadFailed for " + location);
            }
        }
    }

    public void ensureArchiveFolderLoaded() {
        if (!HundredGramConfig.backgroundArchiverEnabled || !UserConfig.getInstance(currentAccount).isClientActivated()) {
            return;
        }
        MessagesController controller = MessagesController.getInstance(currentAccount);
        ArrayList<TLRPC.Dialog> archiveDialogs = controller.getDialogs(1);
        if (archiveDialogs == null || archiveDialogs.isEmpty()) {
            controller.loadDialogs(1, 0, 100, true);
        }
    }

    public void triggerSync(boolean force) {
        if (!HundredGramConfig.backgroundArchiverEnabled || !UserConfig.getInstance(currentAccount).isClientActivated()) {
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
        ensureArchiveFolderLoaded();

        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            ArrayList<Long> dids = new ArrayList<>();
            ArrayList<Integer> folderIds = new ArrayList<>();
            SQLiteCursor cursor = null;
            try {
                cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT did, folder_id FROM dialogs ORDER BY date DESC");
                while (cursor.next()) {
                    dids.add(cursor.longValue(0));
                    folderIds.add(cursor.intValue(1));
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

                MessagesController controller = MessagesController.getInstance(currentAccount);

                for (int i = 0; i < dids.size(); i++) {
                    long did = dids.get(i);
                    int folderId = folderIds.get(i);

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
                    if (mode == HundredGramConfig.VAULT_MODE_ALWAYS || archiveOverride == HundredGramConfig.OVERRIDE_ENABLED) {
                        queue.add(did);
                        queuedSet.add(did);
                        continue;
                    }

                    // VAULT_MODE_DEFAULT
                    if (!HundredGramConfig.isChatAutoArchive(did) || !HundredGramConfig.autoDownloadMediaVault) {
                        continue;
                    }

                    int scope = HundredGramConfig.autoDownloadChatsScope;
                    boolean eligible;
                    if (scope == 0) {
                        eligible = did > 0;
                    } else if (scope == 1) {
                        if (did > 0) {
                            eligible = true;
                        } else {
                            TLRPC.Chat chat = controller.getChat(-did);
                            eligible = chat == null || !ChatObject.isChannelAndNotMegaGroup(chat);
                        }
                    } else {
                        eligible = true; // scope == 2: All chats on device
                    }

                    if (eligible) {
                        queue.add(did);
                        queuedSet.add(did);
                    }
                }

                // Also check if any in-memory dialogs weren't in the database query
                ArrayList<TLRPC.Dialog> folder0 = controller.getDialogs(0);
                if (folder0 != null) {
                    addEligibleDialogs(folder0);
                }
                ArrayList<TLRPC.Dialog> folder1 = controller.getDialogs(1);
                if (folder1 != null) {
                    addEligibleDialogs(folder1);
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
        MessagesController controller = MessagesController.getInstance(currentAccount);
        for (int i = 0; i < list.size(); i++) {
            TLRPC.Dialog d = list.get(i);
            if (d == null || queuedSet.contains(d.id)) {
                continue;
            }
            if (d.folder_id == 1 && !HundredGramConfig.vaultIncludeArchivedChats) {
                continue;
            }
            int archiveOverride = HundredGramConfig.getChatOverride(d.id, "auto_archive");
            if (archiveOverride == HundredGramConfig.OVERRIDE_DISABLED) {
                continue;
            }
            int mode = HundredGramConfig.getChatVaultMode(d.id);
            if (mode == HundredGramConfig.VAULT_MODE_NEVER) {
                continue;
            }
            if (mode == HundredGramConfig.VAULT_MODE_ALWAYS || archiveOverride == HundredGramConfig.OVERRIDE_ENABLED) {
                queue.add(d.id);
                queuedSet.add(d.id);
                continue;
            }
            // VAULT_MODE_DEFAULT
            if (!HundredGramConfig.isChatAutoArchive(d.id) || !HundredGramConfig.autoDownloadMediaVault) {
                continue;
            }
            int scope = HundredGramConfig.autoDownloadChatsScope;
            boolean eligible;
            if (scope == 0) {
                eligible = d.id > 0;
            } else if (scope == 1) {
                if (d.id > 0) {
                    eligible = true;
                } else {
                    TLRPC.Chat chat = controller.getChat(-d.id);
                    eligible = chat == null || !ChatObject.isChannelAndNotMegaGroup(chat);
                }
            } else {
                eligible = true;
            }
            if (eligible) {
                queue.add(d.id);
                queuedSet.add(d.id);
            }
        }
    }

    private void processNext() {
        if (!started || !HundredGramConfig.backgroundArchiverEnabled || queue.isEmpty()) {
            isSyncing = false;
            scheduleNextPeriodicSync();
            return;
        }

        long dialogId = queue.remove(0);
        MessagesController controller = MessagesController.getInstance(currentAccount);

        TLRPC.Dialog dialog = controller.dialogs_dict != null ? controller.dialogs_dict.get(dialogId) : null;
        if (dialog != null && dialog.top_message > 0) {
            Integer lastTop = lastPolledTopMessageId.get(dialogId);
            if (lastTop != null && dialog.top_message == lastTop) {
                // No new messages in this chat, skip without querying MTProto
                AndroidUtilities.runOnUIThread(processQueueRunnable, 50);
                return;
            }
        }

        TLRPC.InputPeer peer = controller.getInputPeer(dialogId);
        if (peer == null || (peer instanceof TLRPC.TL_inputPeerChannel && ((TLRPC.TL_inputPeerChannel) peer).access_hash == 0)) {
            if (dialogId < 0) {
                TLRPC.Chat chat = controller.getChat(-dialogId);
                if (chat == null) {
                    chat = controller.getMessagesStorage().getChat(-dialogId);
                    if (chat != null) {
                        controller.putChat(chat, true);
                        peer = controller.getInputPeer(dialogId);
                    }
                }
            } else if (dialogId > 0) {
                TLRPC.User user = controller.getUser(dialogId);
                if (user == null) {
                    user = controller.getMessagesStorage().getUser(dialogId);
                    if (user != null) {
                        controller.putUser(user, true);
                        peer = controller.getInputPeer(dialogId);
                    }
                }
            }
        }
        if (peer == null) {
            AndroidUtilities.runOnUIThread(processQueueRunnable, 200);
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
                    if (HundredGramConfig.canVaultDownload(dialogId)) {
                        int mediaCount = 0;
                        for (int m = 0; m < res.messages.size(); m++) {
                            TLRPC.Message msg = res.messages.get(m);
                            if (msg == null || msg.media == null || msg.media instanceof TLRPC.TL_messageMediaEmpty) {
                                continue;
                            }
                            if (!HundredGramConfig.canVaultDownload(dialogId, msg)) {
                                continue;
                            }

                            // 1. Photos
                            if (msg.media instanceof TLRPC.TL_messageMediaPhoto && msg.media.photo != null && msg.media.photo.sizes != null && !msg.media.photo.sizes.isEmpty()) {
                                TLRPC.PhotoSize size = FileLoader.getClosestPhotoSizeWithSize(msg.media.photo.sizes, AndroidUtilities.getPhotoSize());
                                if (size != null) {
                                    String attachName = FileLoader.getAttachFileName(size, "jpg");
                                    File checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(size, "jpg", true);
                                    if (checkFile != null && checkFile.exists() && checkFile.length() > 0) {
                                        HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(msg, checkFile.getAbsolutePath());
                                    } else {
                                        pendingArchiverDownloads.put(attachName, new PendingArchiverMedia(msg, dialogId, attachName));
                                        final TLRPC.Photo finalPhoto = msg.media.photo;
                                        AndroidUtilities.runOnUIThread(() -> {
                                            FileLoader.getInstance(currentAccount).loadFile(
                                                    ImageLocation.getForPhoto(size, finalPhoto),
                                                    msg,
                                                    "jpg",
                                                    FileLoader.PRIORITY_LOW,
                                                    1
                                            );
                                        });
                                        mediaCount++;
                                        FileLog.d("HundredGramArchiver: queueing photo load for msg " + msg.id + " in dialog " + dialogId + " (" + attachName + ")");
                                    }
                                }
                            }
                            // 2. Documents, Voice, Round Video, Video
                            else if (msg.media.document != null) {
                                long maxLimit = (long) HundredGramConfig.vaultMaxAutodownloadSizeMb * 1024L * 1024L;
                                if (maxLimit <= 0) {
                                    maxLimit = 50L * 1024L * 1024L;
                                }
                                if (msg.media.document.size <= maxLimit) {
                                    String attachName = FileLoader.getAttachFileName(msg.media.document);
                                    File checkFile = FileLoader.getInstance(currentAccount).getPathToAttach(msg.media.document, true);
                                    if (checkFile != null && checkFile.exists() && checkFile.length() > 0) {
                                        HundredGramVaultManager.getInstance(currentAccount).isolateMediaForMessage(msg, checkFile.getAbsolutePath());
                                    } else {
                                        pendingArchiverDownloads.put(attachName, new PendingArchiverMedia(msg, dialogId, attachName));
                                        final TLRPC.Document doc = msg.media.document;
                                        AndroidUtilities.runOnUIThread(() -> {
                                            FileLoader.getInstance(currentAccount).loadFile(
                                                    doc,
                                                    msg,
                                                    FileLoader.PRIORITY_LOW,
                                                    0
                                            );
                                        });
                                        mediaCount++;
                                        FileLog.d("HundredGramArchiver: queueing doc load for msg " + msg.id + " in dialog " + dialogId + " (" + attachName + ", size=" + msg.media.document.size + ")");
                                    }
                                }
                            }
                        }
                        if (mediaCount > 0) {
                            FileLog.d("HundredGramArchiver: found " + mediaCount + " media items to download for dialog " + dialogId);
                        }
                    }
                }
                if (controller.dialogs_dict != null) {
                    TLRPC.Dialog d = controller.dialogs_dict.get(dialogId);
                    if (d != null && d.top_message > 0) {
                        lastPolledTopMessageId.put(dialogId, d.top_message);
                    }
                }
            }
            int jitterDelay = 300 + Utilities.random.nextInt(500);
            AndroidUtilities.runOnUIThread(processQueueRunnable, jitterDelay);
        });
    }

    public void onNewMessage(long dialogId) {
        lastPolledTopMessageId.remove(dialogId);
    }

    private void scheduleNextPeriodicSync() {
        AndroidUtilities.cancelRunOnUIThread(periodicSyncRunnable);
        AndroidUtilities.runOnUIThread(periodicSyncRunnable, 20 * 60 * 1000L);
    }
}
