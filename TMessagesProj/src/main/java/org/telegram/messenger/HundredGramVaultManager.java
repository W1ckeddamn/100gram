package org.telegram.messenger;

import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import androidx.core.util.Consumer;

public class HundredGramVaultManager implements NotificationCenter.NotificationCenterDelegate {

    private static volatile HundredGramVaultManager[] Instance = new HundredGramVaultManager[UserConfig.MAX_ACCOUNT_COUNT];

    public static HundredGramVaultManager getInstance(int account) {
        HundredGramVaultManager local = Instance[account];
        if (local == null) {
            synchronized (HundredGramVaultManager.class) {
                local = Instance[account];
                if (local == null) {
                    Instance[account] = local = new HundredGramVaultManager(account);
                }
            }
        }
        return local;
    }

    private final int currentAccount;
    private final File vaultDir;
    private final ConcurrentHashMap<String, String> vaultCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PendingVaultMedia> pendingDownloads = new ConcurrentHashMap<>();

    private static class PendingVaultMedia {
        final int messageId;
        final long dialogId;
        final String fileName;
        final int editDate;

        PendingVaultMedia(int messageId, long dialogId, String fileName) {
            this(messageId, dialogId, fileName, 0);
        }

        PendingVaultMedia(int messageId, long dialogId, String fileName, int editDate) {
            this.messageId = messageId;
            this.dialogId = dialogId;
            this.fileName = fileName;
            this.editDate = editDate;
        }
    }

    private HundredGramVaultManager(int account) {
        this.currentAccount = account;
        this.vaultDir = new File(ApplicationLoader.getFilesDirFixed(), "hundredgram_vault");
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }

        // Register observer for file loaded events
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.fileLoaded);

        // Load existing mappings from database
        MessagesStorage.getInstance(currentAccount).loadVaultMappings(map -> {
            if (map != null) {
                vaultCache.putAll(map);
            }
        });
    }

    public void isolateMediaForMessage(TLRPC.Message message, String filePath) {
        if (message == null || TextUtils.isEmpty(filePath)) {
            return;
        }
        if (!HundredGramConfig.vaultSaveForBots) {
            long fromId = MessageObject.getFromChatId(message);
            if (fromId > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
                if (user != null && user.bot) {
                    return;
                }
            }
        }
        long dialogId = message.dialog_id != 0 ? message.dialog_id : MessageObject.getDialogId(message);
        if (!HundredGramConfig.isChatAntiDelete(dialogId) && !HundredGramConfig.canVaultDownload(dialogId)) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            String fileName = file.getName();
            File dest = new File(vaultDir, fileName);
            if (linkOrCopyFile(file, dest)) {
                String vaultPath = dest.getAbsolutePath();
                vaultCache.put(fileName, vaultPath);
                vaultCache.put(dialogId + "_" + message.id, vaultPath);
                MessagesStorage.getInstance(currentAccount).putVaultFile(message.id, dialogId, fileName, vaultPath);
                FileLog.d("HundredGramArchiver: successfully isolated to Vault: " + vaultPath + " for msg " + message.id);
            }
        });
    }

    public void isolateMediaForMessage(int messageId, long dialogId, File file) {
        if (file == null || !file.exists() || file.length() == 0 || messageId == 0 || dialogId == 0) {
            return;
        }
        if (!HundredGramConfig.vaultSaveForBots && dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user != null && user.bot) {
                return;
            }
        }
        if (!HundredGramConfig.isChatAntiDelete(dialogId) && !HundredGramConfig.canVaultDownload(dialogId)) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            String fileName = file.getName();
            File dest = new File(vaultDir, fileName);
            if (linkOrCopyFile(file, dest)) {
                String vaultPath = dest.getAbsolutePath();
                vaultCache.put(fileName, vaultPath);
                vaultCache.put(dialogId + "_" + messageId, vaultPath);
                MessagesStorage.getInstance(currentAccount).putVaultFile(messageId, dialogId, fileName, vaultPath);
                FileLog.d("HundredGramArchiver: auto-isolated file to Vault: " + vaultPath + " for msg " + messageId);
            }
        });
    }

    public boolean shouldInterceptMessage(TLRPC.Message message) {
        if (message == null) {
            return false;
        }
        if (!HundredGramConfig.vaultSaveForBots) {
            long fromId = MessageObject.getFromChatId(message);
            if (fromId > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
                if (user != null && user.bot) {
                    return false;
                }
            }
        }
        long dialogId = MessageObject.getDialogId(message);
        return HundredGramConfig.isChatAntiDelete(dialogId) || HundredGramConfig.canVaultDownload(dialogId);
    }

    public void isolateMediaForMessage(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        if (!HundredGramConfig.vaultSaveForBots) {
            long fromId = messageObject.getFromChatId();
            if (fromId > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
                if (user != null && user.bot) {
                    return;
                }
            }
        }
        long dialogId = messageObject.getDialogId();
        if (!HundredGramConfig.isChatAntiDelete(dialogId) && !HundredGramConfig.canVaultDownload(dialogId)) {
            return;
        }
        TLRPC.Message message = messageObject.messageOwner;
        if (message.media == null || message.media instanceof TLRPC.TL_messageMediaEmpty) {
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            if (messageObject == null || messageObject.messageOwner == null) {
                return;
            }
            File localFile = null;
            String fileName = null;

            // 1. Check attachPath
            if (!TextUtils.isEmpty(message.attachPath)) {
                File f = new File(message.attachPath);
                if (f.exists()) {
                    localFile = f;
                    fileName = f.getName();
                }
            }

            // 2. Check getPathToMessage
            if (localFile == null) {
                File f = FileLoader.getInstance(currentAccount).getPathToMessage(message, true);
                if (f == null || !f.exists()) {
                    f = FileLoader.getInstance(currentAccount).getPathToMessage(message, false);
                }
                if (f != null && f.exists()) {
                    localFile = f;
                    fileName = f.getName();
                }
            }

            // 3. Check document attach path
            if (localFile == null && messageObject.getDocument() != null) {
                fileName = FileLoader.getAttachFileName(messageObject.getDocument());
                File f = FileLoader.getInstance(currentAccount).getPathToAttach(messageObject.getDocument(), true);
                if (f == null || !f.exists()) {
                    f = FileLoader.getInstance(currentAccount).getPathToAttach(messageObject.getDocument(), false);
                }
                if (f != null && f.exists()) {
                    localFile = f;
                }
            }

            // 4. Check photo attach path
            if (localFile == null && messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
                TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize(true));
                if (sizeFull != null) {
                    fileName = FileLoader.getAttachFileName(sizeFull, "jpg");
                    File f = FileLoader.getInstance(currentAccount).getPathToAttach(sizeFull, "jpg", true);
                    if (f == null || !f.exists()) {
                        f = FileLoader.getInstance(currentAccount).getPathToAttach(sizeFull, "jpg", false);
                    }
                    if (f != null && f.exists()) {
                        localFile = f;
                    }
                }
            }

            if (localFile != null && localFile.exists()) {
                if (fileName == null) {
                    fileName = localFile.getName();
                }
                File dest = new File(vaultDir, fileName);
                if (linkOrCopyFile(localFile, dest)) {
                    String vaultPath = dest.getAbsolutePath();
                    vaultCache.put(fileName, vaultPath);
                    vaultCache.put(dialogId + "_" + message.id, vaultPath);
                    MessagesStorage.getInstance(currentAccount).putVaultFile(message.id, dialogId, fileName, vaultPath);
                    FileLog.d("HundredGramArchiver: local file directly linked to Vault: " + vaultPath + " for msg " + message.id);
                }
            } else {
                // Media not downloaded yet. Check auto-download limits
                long size = messageObject.getSize();
                TLRPC.Photo photo = null;
                if (message.media instanceof TLRPC.TL_messageMediaPhoto && message.media.photo != null) {
                    photo = message.media.photo;
                    if (size <= 0 && photo.sizes != null && !photo.sizes.isEmpty()) {
                        TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true));
                        if (sizeFull != null) {
                            size = sizeFull.size;
                        }
                    }
                }
                int maxMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
                if (maxMb == 0) {
                    maxMb = HundredGramConfig.autoDownloadMaxSizeMb;
                }
                long maxLimit = maxMb > 0 ? (long) maxMb * 1024L * 1024L : Long.MAX_VALUE;
                if (size <= 0 || size <= maxLimit) {
                    acquireWakeLock(45000);
                    final String pendingFileName = fileName != null ? fileName :
                            (messageObject.getDocument() != null ? FileLoader.getAttachFileName(messageObject.getDocument()) : null);

                    if (pendingFileName != null) {
                        pendingDownloads.put(pendingFileName, new PendingVaultMedia(message.id, dialogId, pendingFileName));
                    }

                    if (messageObject.getDocument() != null) {
                        final TLRPC.Document doc = messageObject.getDocument();
                        FileLog.d("HundredGramArchiver: queueing document load for msg " + message.id + " (" + pendingFileName + ")");
                        AndroidUtilities.runOnUIThread(() -> FileLoader.getInstance(currentAccount).loadFile(doc, messageObject, FileLoader.PRIORITY_LOW, 0));
                    } else {
                        TLRPC.PhotoSize sizeFull = null;
                        if (photo != null && photo.sizes != null && !photo.sizes.isEmpty()) {
                            sizeFull = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true));
                        } else if (messageObject.photoThumbs != null && !messageObject.photoThumbs.isEmpty()) {
                            sizeFull = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize(true));
                        }
                        if (sizeFull != null) {
                            final TLRPC.PhotoSize fSize = sizeFull;
                            final String photoFileName = FileLoader.getAttachFileName(fSize, "jpg");
                            pendingDownloads.put(photoFileName, new PendingVaultMedia(message.id, dialogId, photoFileName));
                            final TLRPC.Photo finalPhoto = photo;
                            FileLog.d("HundredGramArchiver: queueing photo load for msg " + message.id + " (" + photoFileName + ")");
                            AndroidUtilities.runOnUIThread(() -> FileLoader.getInstance(currentAccount).loadFile(
                                    finalPhoto != null ? ImageLocation.getForPhoto(fSize, finalPhoto) : ImageLocation.getForObject(fSize, (org.telegram.tgnet.TLObject) messageObject.photoThumbsObject),
                                    messageObject,
                                    "jpg",
                                    FileLoader.PRIORITY_LOW,
                                    1
                            ));
                        }
                    }
                }
            }
        });
    }

    /**
     * Saves the media of an OLD message version (before a media-replacing edit) into the vault.
     * Key: (dialogId)_(mid)_edit_(editDate) in the vault cache.
     * Called from MessagesStorage when !sameMedia is detected during putMessages.
     */
    public void isolateMediaForEdit(TLRPC.Message oldMessage, int editDate) {
        if (oldMessage == null) return;
        if (!HundredGramConfig.vaultSaveForBots) {
            long fromId = MessageObject.getFromChatId(oldMessage);
            if (fromId > 0) {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
                if (user != null && user.bot) {
                    return;
                }
            }
        }
        long dialogId = oldMessage.dialog_id != 0 ? oldMessage.dialog_id : MessageObject.getDialogId(oldMessage);
        if (oldMessage.media == null || oldMessage.media instanceof TLRPC.TL_messageMediaEmpty) return;

        Utilities.globalQueue.postRunnable(() -> {
            File localFile = null;
            String baseFileName = null;

            // Resolve the local file for the old version's media
            if (!TextUtils.isEmpty(oldMessage.attachPath)) {
                File f = new File(oldMessage.attachPath);
                if (f.exists()) { localFile = f; baseFileName = f.getName(); }
            }
            if (localFile == null) {
                File f = FileLoader.getInstance(currentAccount).getPathToMessage(oldMessage, true);
                if (f == null || !f.exists()) {
                    f = FileLoader.getInstance(currentAccount).getPathToMessage(oldMessage, false);
                }
                if (f != null && f.exists()) { localFile = f; baseFileName = f.getName(); }
            }
            if (localFile == null && oldMessage.media instanceof TLRPC.TL_messageMediaDocument
                    && oldMessage.media.document != null) {
                baseFileName = FileLoader.getAttachFileName(oldMessage.media.document);
                File f = FileLoader.getInstance(currentAccount).getPathToAttach(oldMessage.media.document, true);
                if (f == null || !f.exists()) {
                    f = FileLoader.getInstance(currentAccount).getPathToAttach(oldMessage.media.document, false);
                }
                if (f != null && f.exists()) localFile = f;
            }
            if (localFile == null && oldMessage.media instanceof TLRPC.TL_messageMediaPhoto
                    && oldMessage.media.photo != null && !oldMessage.media.photo.sizes.isEmpty()) {
                TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(oldMessage.media.photo.sizes, 1280);
                if (sizeFull != null) {
                    baseFileName = FileLoader.getAttachFileName(sizeFull);
                    File f = FileLoader.getInstance(currentAccount).getPathToAttach(sizeFull, true);
                    if (f == null || !f.exists()) {
                        f = FileLoader.getInstance(currentAccount).getPathToAttach(sizeFull, false);
                    }
                    if (f != null && f.exists()) localFile = f;
                }
            }

            if (localFile == null || !localFile.exists()) {
                // If not locally downloaded, schedule download so it gets saved to vault when loaded
                int maxMb = HundredGramConfig.getChatVaultMaxSizeMb(dialogId);
                if (maxMb == 0) {
                    maxMb = HundredGramConfig.autoDownloadMaxSizeMb;
                }
                long maxLimit = maxMb > 0 ? (long) maxMb * 1024L * 1024L : Long.MAX_VALUE;
                MessageObject oldObj = new MessageObject(currentAccount, oldMessage, false, false);
                long size = oldObj.getSize();
                if (size <= 0 || size <= maxLimit) {
                    acquireWakeLock(45000);
                    if (oldMessage.media instanceof TLRPC.TL_messageMediaDocument && oldMessage.media.document != null) {
                        TLRPC.Document doc = oldMessage.media.document;
                        String docName = FileLoader.getAttachFileName(doc);
                        if (docName != null) {
                            pendingDownloads.put(docName, new PendingVaultMedia(oldMessage.id, dialogId, docName, editDate));
                            AndroidUtilities.runOnUIThread(() -> FileLoader.getInstance(currentAccount).loadFile(doc, oldObj, 1, 0));
                        }
                    } else if (oldMessage.media instanceof TLRPC.TL_messageMediaPhoto && oldMessage.media.photo != null) {
                        TLRPC.Photo photo = oldMessage.media.photo;
                        TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true));
                        if (sizeFull != null) {
                            String photoFileName = FileLoader.getAttachFileName(sizeFull);
                            pendingDownloads.put(photoFileName, new PendingVaultMedia(oldMessage.id, dialogId, photoFileName, editDate));
                            AndroidUtilities.runOnUIThread(() -> FileLoader.getInstance(currentAccount).loadFile(ImageLocation.getForObject(sizeFull, photo), oldObj, null, 1, 1));
                        }
                    }
                }
                return;
            }
            if (baseFileName == null) baseFileName = localFile.getName();

            // Build a unique vault name that encodes (mid, editDate) so we can look it up later
            String vaultFileName = "edit_" + oldMessage.id + "_" + editDate + "_" + baseFileName;
            File dest = new File(vaultDir, vaultFileName);
            if (linkOrCopyFile(localFile, dest)) {
                String vaultPath = dest.getAbsolutePath();
                // Index by edit-key for fast retrieval from edit history sheet
                String editKey = dialogId + "_" + oldMessage.id + "_edit_" + editDate;
                vaultCache.put(editKey, vaultPath);
                vaultCache.put(vaultFileName, vaultPath);
                MessagesStorage.getInstance(currentAccount).putVaultFile(oldMessage.id, dialogId, vaultFileName, vaultPath);
            }
        });
    }

    /**
     * Returns the vault file for a specific edit revision of a message.
     * @param mid message id
     * @param dialogId dialog id
     * @param editDate the edit_date / version_date of the old revision
     */
    public File getVaultFileForEdit(long mid, long dialogId, int editDate) {
        String editKey = dialogId + "_" + mid + "_edit_" + editDate;
        String path = vaultCache.get(editKey);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        // Fallback: scan vault dir for matching prefix
        if (vaultDir == null || !vaultDir.exists()) {
            return null;
        }
        File[] files = vaultDir.listFiles();
        if (files != null) {
            String prefix = "edit_" + mid + "_" + editDate + "_";
            for (File f : files) {
                if (f != null && f.exists() && f.getName().startsWith(prefix)) {
                    vaultCache.put(editKey, f.getAbsolutePath());
                    return f;
                }
            }
            // Fuzzy match: check +/- 1 second difference
            String prefixMinus = "edit_" + mid + "_" + (editDate - 1) + "_";
            String prefixPlus = "edit_" + mid + "_" + (editDate + 1) + "_";
            for (File f : files) {
                if (f != null && f.exists() && (f.getName().startsWith(prefixMinus) || f.getName().startsWith(prefixPlus))) {
                    vaultCache.put(editKey, f.getAbsolutePath());
                    return f;
                }
            }
            // General match for this mid if only one edit file exists
            String midPrefix = "edit_" + mid + "_";
            File singleMatch = null;
            int count = 0;
            for (File f : files) {
                if (f != null && f.exists() && f.getName().startsWith(midPrefix)) {
                    singleMatch = f;
                    count++;
                }
            }
            if (count == 1 && singleMatch != null) {
                vaultCache.put(editKey, singleMatch.getAbsolutePath());
                return singleMatch;
            }
        }
        return null;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoaded) {
            String fileName = (String) args[0];
            File file = (File) args[1];
            if (fileName != null && file != null && file.exists() && file.length() > 0) {
                PendingVaultMedia pending = pendingDownloads.remove(fileName);
                if (pending == null) {
                    pending = pendingDownloads.remove(file.getName());
                }
                if (pending != null) {
                    final PendingVaultMedia finalPending = pending;
                    Utilities.globalQueue.postRunnable(() -> {
                        String destFileName;
                        if (finalPending.editDate > 0) {
                            destFileName = "edit_" + finalPending.messageId + "_" + finalPending.editDate + "_" + fileName;
                        } else {
                            destFileName = fileName;
                        }
                        File dest = new File(vaultDir, destFileName);
                        if (linkOrCopyFile(file, dest)) {
                            String vaultPath = dest.getAbsolutePath();
                            vaultCache.put(destFileName, vaultPath);
                            if (finalPending.editDate > 0) {
                                String editKey = finalPending.dialogId + "_" + finalPending.messageId + "_edit_" + finalPending.editDate;
                                vaultCache.put(editKey, vaultPath);
                            } else {
                                vaultCache.put(finalPending.dialogId + "_" + finalPending.messageId, vaultPath);
                            }
                            MessagesStorage.getInstance(currentAccount).putVaultFile(finalPending.messageId, finalPending.dialogId, destFileName, vaultPath);
                        }
                    });
                } else {
                    FilePathDatabase.FileMeta fileMeta = FileLoader.getInstance(currentAccount).getFileDatabase().getFileDialogId(file, null);
                    if (fileMeta != null && fileMeta.dialogId != 0 && fileMeta.messageId != 0) {
                        if (HundredGramConfig.isChatAntiDelete(fileMeta.dialogId) || HundredGramConfig.canVaultDownload(fileMeta.dialogId)) {
                            isolateMediaForMessage(fileMeta.messageId, fileMeta.dialogId, file);
                        }
                    }
                }
            }
        }
    }

    public File getVaultFile(long mid, long uid, String originalFileName) {
        if (originalFileName != null) {
            String path = vaultCache.get(originalFileName);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) return f;
            }
        }
        String key = uid + "_" + mid;
        String path = vaultCache.get(key);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        if (originalFileName != null) {
            File f = new File(vaultDir, originalFileName);
            if (f.exists()) {
                vaultCache.put(originalFileName, f.getAbsolutePath());
                return f;
            }
        }
        return null;
    }

    public static String getAttachFileName(TLRPC.Message message) {
        if (message == null || message.media == null) return null;
        if (message.media instanceof TLRPC.TL_messageMediaDocument && message.media.document != null) {
            return FileLoader.getAttachFileName(message.media.document);
        } else if (message.media instanceof TLRPC.TL_messageMediaPhoto && message.media.photo != null) {
            TLRPC.PhotoSize sizeFull = FileLoader.getClosestPhotoSizeWithSize(message.media.photo.sizes, AndroidUtilities.getPhotoSize(true));
            if (sizeFull != null) {
                return FileLoader.getAttachFileName(sizeFull);
            }
        }
        return null;
    }

    public File getVaultFile(TLRPC.Message message) {
        if (message == null || message.media == null) return null;
        String currentFileName = getAttachFileName(message);
        if (currentFileName != null) {
            String path = vaultCache.get(currentFileName);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) return f;
            }
            File f = new File(vaultDir, currentFileName);
            if (f.exists()) {
                vaultCache.put(currentFileName, f.getAbsolutePath());
                return f;
            }
        }
        String key = message.dialog_id + "_" + message.id;
        String path = vaultCache.get(key);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                // Ensure this file actually matches the current message's media
                if (currentFileName == null || f.getName().equals(currentFileName)) {
                    return f;
                }
            }
        }
        return null;
    }

    public void onMediaReplaced(long dialogId, int messageId) {
        String key = dialogId + "_" + messageId;
        vaultCache.remove(key);
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            try {
                MessagesStorage.getInstance(currentAccount).getDatabase().executeFast(
                        String.format(java.util.Locale.US, "DELETE FROM hundredgram_vault WHERE mid = %d AND did = %d AND file_name NOT LIKE 'edit_%%'", messageId, dialogId)
                ).stepThis().dispose();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    public File getVaultFileByName(String fileName) {
        if (TextUtils.isEmpty(fileName)) return null;
        String path = vaultCache.get(fileName);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        File f = new File(vaultDir, fileName);
        if (f.exists()) {
            vaultCache.put(fileName, f.getAbsolutePath());
            return f;
        }
        return null;
    }

    public void deleteMessagePermanently(MessageObject messageObject) {
        if (messageObject == null) return;
        final int mid = messageObject.getId();
        final long did = messageObject.getDialogId();
        Utilities.globalQueue.postRunnable(() -> {
            String key = did + "_" + mid;
            String path = vaultCache.remove(key);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) f.delete();
            }
            if (messageObject.getDocument() != null) {
                String fn = FileLoader.getAttachFileName(messageObject.getDocument());
                if (fn != null) {
                    vaultCache.remove(fn);
                    File f = new File(vaultDir, fn);
                    if (f.exists()) f.delete();
                }
            }
            if (messageObject.photoThumbs != null) {
                for (TLRPC.PhotoSize ps : messageObject.photoThumbs) {
                    String fn = FileLoader.getAttachFileName(ps);
                    if (fn != null) {
                        vaultCache.remove(fn);
                        File f = new File(vaultDir, fn);
                        if (f.exists()) f.delete();
                    }
                }
            }
            MessagesStorage.getInstance(currentAccount).deleteMessagePermanently(mid, did);
        });
    }

    public long getVaultTotalSize() {
        long total = 0;
        if (vaultDir != null && vaultDir.exists()) {
            File[] files = vaultDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f != null && f.isFile()) {
                        total += f.length();
                    }
                }
            }
        }
        return total;
    }

    /**
     * Names of vault files that belong to a chat marked "never expire" in the vault settings.
     * Returns null when the vault table cannot be read, so rotation skips deletion instead of
     * destroying protected media.
     */
    public Set<String> getNeverExpireVaultFileNames() {
        HashSet<String> names = new HashSet<>();
        if (HundredGramConfig.vaultProtectedDialogs.isEmpty()) {
            return names;
        }
        org.telegram.SQLite.SQLiteCursor cursor = null;
        try {
            cursor = MessagesStorage.getInstance(currentAccount).getDatabase()
                    .queryFinalized("SELECT file_name, uid FROM hundredgram_vault");
            while (cursor.next()) {
                String fileName = cursor.stringValue(0);
                if (!TextUtils.isEmpty(fileName) && HundredGramConfig.isVaultNeverExpire(cursor.longValue(1))) {
                    names.add(fileName);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        } finally {
            if (cursor != null) cursor.dispose();
        }
        return names;
    }

    public void clearVault(long olderThanTimestamp) {
        clearVault(olderThanTimestamp, null);
    }

    public void clearVault(long olderThanTimestamp, Runnable onComplete) {
        Utilities.globalQueue.postRunnable(() -> {
            if (vaultDir != null && vaultDir.exists()) {
                File[] files = vaultDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f != null && f.isFile()) {
                            if (olderThanTimestamp == 0 || f.lastModified() < olderThanTimestamp) {
                                f.delete();
                            }
                        }
                    }
                }
            }
            if (olderThanTimestamp == 0) {
                vaultCache.clear();
                MessagesStorage.getInstance(currentAccount).clearAllVault();
            } else {
                java.util.Iterator<java.util.Map.Entry<String, String>> it = vaultCache.entrySet().iterator();
                while (it.hasNext()) {
                    java.util.Map.Entry<String, String> entry = it.next();
                    File f = new File(entry.getValue());
                    if (!f.exists()) {
                        it.remove();
                    }
                }
            }
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(onComplete);
            }
        });
    }

    public File getVaultDir() {
        return vaultDir;
    }

    public void deleteVaultFile(int mid, long uid, String fileName, String vaultPath) {
        if (!TextUtils.isEmpty(fileName)) {
            vaultCache.remove(fileName);
        }
        vaultCache.remove(uid + "_" + mid);
        if (!TextUtils.isEmpty(vaultPath)) {
            File f = new File(vaultPath);
            if (f.exists()) {
                f.delete();
            }
        }
        MessagesStorage.getInstance(currentAccount).deleteVaultEntry(mid, uid, fileName);
    }

    private void acquireWakeLock(long timeoutMs) {
        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "100gram:VaultAutoDownload");
                wl.acquire(timeoutMs);
            }
        } catch (Exception ignore) {}
    }

    public static boolean linkOrCopyFile(File source, File dest) {
        if (source == null || !source.exists() || dest == null) {
            return false;
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return true;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (dest.exists()) {
            if (dest.length() == source.length() && dest.length() > 0) {
                return true;
            }
            dest.delete();
        }
        // Try creating hard link first (0 bytes disk overhead, immune to cache wipe)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                android.system.Os.link(source.getAbsolutePath(), dest.getAbsolutePath());
                if (dest.exists() && dest.length() > 0) {
                    return true;
                }
            }
        } catch (Throwable e) {
            // Fallback to byte copy on cross-device link or filesystem without hardlink support
            FileLog.d("HundredGramVaultManager: Os.link fallback to copy: " + e.getMessage());
        }
        return copyFile(source, dest);
    }

    private static boolean copyFile(File source, File dest) {
        if (source == null || !source.exists() || dest == null) {
            return false;
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            return true;
        }
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(dest);
            inChannel = fis.getChannel();
            outChannel = fos.getChannel();
            long size = inChannel.size();
            long transferred = 0;
            while (transferred < size) {
                transferred += inChannel.transferTo(transferred, size - transferred, outChannel);
            }
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        } finally {
            try { if (inChannel != null) inChannel.close(); } catch (Exception ignore) {}
            try { if (outChannel != null) outChannel.close(); } catch (Exception ignore) {}
            try { if (fis != null) fis.close(); } catch (Exception ignore) {}
            try { if (fos != null) fos.close(); } catch (Exception ignore) {}
        }
    }

    public void exportVaultToZip(File destZip, List<File> files, Consumer<Float> progressCallback, Utilities.Callback<File> onComplete) {
        if (files == null || files.isEmpty()) {
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(() -> onComplete.run(null));
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            ZipOutputStream zos = null;
            FileOutputStream fos = null;
            boolean success = false;
            try {
                if (destZip.exists()) {
                    destZip.delete();
                }
                File parent = destZip.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                fos = new FileOutputStream(destZip);
                zos = new ZipOutputStream(new BufferedOutputStream(fos));

                byte[] buffer = new byte[65536];
                HashSet<String> addedNames = new HashSet<>();
                int totalFiles = files.size();

                for (int i = 0; i < totalFiles; i++) {
                    File file = files.get(i);
                    if (file == null || !file.exists() || !file.isFile() || file.length() == 0) {
                        continue;
                    }
                    String baseName = file.getName();
                    String entryName = baseName;
                    int count = 1;
                    while (addedNames.contains(entryName)) {
                        int dot = baseName.lastIndexOf('.');
                        if (dot > 0) {
                            entryName = baseName.substring(0, dot) + "_" + count + baseName.substring(dot);
                        } else {
                            entryName = baseName + "_" + count;
                        }
                        count++;
                    }
                    addedNames.add(entryName);

                    ZipEntry entry = new ZipEntry(entryName);
                    entry.setTime(file.lastModified());
                    zos.putNextEntry(entry);

                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(file);
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, read);
                        }
                    } finally {
                        if (fis != null) {
                            try { fis.close(); } catch (Exception ignore) {}
                        }
                    }
                    zos.closeEntry();

                    if (progressCallback != null) {
                        final float progress = (float) (i + 1) / totalFiles;
                        AndroidUtilities.runOnUIThread(() -> progressCallback.accept(progress));
                    }
                }
                zos.flush();
                success = true;
            } catch (Throwable e) {
                FileLog.e(e);
                if (destZip.exists()) {
                    destZip.delete();
                }
                success = false;
            } finally {
                if (zos != null) {
                    try { zos.close(); } catch (Exception ignore) {}
                }
                if (fos != null) {
                    try { fos.close(); } catch (Exception ignore) {}
                }
            }

            final boolean finalSuccess = success;
            AndroidUtilities.runOnUIThread(() -> {
                if (onComplete != null) {
                    onComplete.run(finalSuccess ? destZip : null);
                }
            });
        });
    }

    public void pruneOrphanDatabaseEntries(Consumer<Integer> onComplete) {
        MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(() -> {
            int cleanedCount = 0;
            org.telegram.SQLite.SQLiteCursor cursor = null;
            ArrayList<Object[]> toDelete = new ArrayList<>();
            try {
                cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT mid, uid, file_name, vault_path FROM hundredgram_vault");
                while (cursor.next()) {
                    int mid = cursor.intValue(0);
                    long uid = cursor.longValue(1);
                    String fileName = cursor.stringValue(2);
                    String vaultPath = cursor.stringValue(3);

                    File file = null;
                    if (!TextUtils.isEmpty(vaultPath)) {
                        file = new File(vaultPath);
                    }
                    if ((file == null || !file.exists()) && vaultDir != null && !TextUtils.isEmpty(fileName)) {
                        File fallback = new File(vaultDir, fileName);
                        if (fallback.exists()) {
                            file = fallback;
                        }
                    }

                    if (file == null || !file.exists() || file.length() == 0) {
                        if (file != null && file.exists() && file.length() == 0) {
                            file.delete();
                        }
                        toDelete.add(new Object[] { mid, uid, fileName, vaultPath });
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) cursor.dispose();
            }

            for (Object[] row : toDelete) {
                int mid = (Integer) row[0];
                long uid = (Long) row[1];
                String fileName = (String) row[2];
                if (!TextUtils.isEmpty(fileName)) {
                    vaultCache.remove(fileName);
                }
                vaultCache.remove(uid + "_" + mid);
                MessagesStorage.getInstance(currentAccount).deleteVaultEntry(mid, uid, fileName);
                cleanedCount++;
            }

            // Clean 0-byte orphan files from vaultDir
            if (vaultDir != null && vaultDir.exists()) {
                File[] files = vaultDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f != null && f.isFile() && f.length() == 0) {
                            f.delete();
                        }
                    }
                }
            }

            final int finalCleaned = cleanedCount;
            if (onComplete != null) {
                AndroidUtilities.runOnUIThread(() -> onComplete.accept(finalCleaned));
            }
        });
    }
}
