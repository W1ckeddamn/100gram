package org.telegram.messenger;

import org.telegram.ui.Storage.CacheModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HundredGramVaultRotator {

    public interface RotationCallback {
        void onDone(long freedBytes, int deletedCount);
    }

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void runRotation(int account, RotationCallback callback) {
        if (!isRunning.compareAndSet(false, true)) {
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onDone(0, 0));
            }
            return;
        }

        final int ttlDays = HundredGramConfig.vaultTtlDays;
        final int maxSizeGb = HundredGramConfig.vaultMaxSizeGb;

        if (ttlDays <= 0 && maxSizeGb <= 0 && callback == null) {
            isRunning.set(false);
            return;
        }

        FileLoader fileLoader = FileLoader.getInstance(account);
        FilePathDatabase fileDatabase = fileLoader.getFileDatabase();

        fileDatabase.getQueue().postRunnable(() -> {
            try {
                fileDatabase.ensureDatabaseCreated();

                ArrayList<CacheModel.FileInfo> allFiles = new ArrayList<>();
                collectFiles(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE), allFiles, fileDatabase);
                collectFiles(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE), allFiles, fileDatabase);
                collectFiles(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_AUDIO), allFiles, fileDatabase);
                collectFiles(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), allFiles, fileDatabase);
                collectFiles(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO), allFiles, fileDatabase);

                long now = System.currentTimeMillis();
                long maxAgeMs = ttlDays > 0 ? (long) ttlDays * 24L * 3600L * 1000L : Long.MAX_VALUE;
                long maxQuotaBytes = maxSizeGb > 0 ? (long) maxSizeGb * 1024L * 1024L * 1024L : Long.MAX_VALUE;

                ArrayList<CacheModel.FileInfo> filesToDelete = new ArrayList<>();
                ArrayList<CacheModel.FileInfo> survivingFiles = new ArrayList<>();
                long survivingTotalSize = 0;
                long freedBytes = 0;

                for (CacheModel.FileInfo info : allFiles) {
                    if (info.file == null || !info.file.exists()) continue;

                    boolean isProtected = (info.dialogId != 0 && HundredGramConfig.isVaultNeverExpire(info.dialogId));

                    if (!isProtected && ttlDays > 0 && (now - info.file.lastModified() > maxAgeMs)) {
                        filesToDelete.add(info);
                        freedBytes += info.size;
                    } else {
                        survivingFiles.add(info);
                        survivingTotalSize += info.size;
                    }
                }

                // If surviving files exceed quota, delete oldest non-protected files (FIFO)
                if (maxSizeGb > 0 && survivingTotalSize > maxQuotaBytes) {
                    // Sort by lastModified ascending (oldest first)
                    Collections.sort(survivingFiles, (a, b) -> {
                        long diff = a.file.lastModified() - b.file.lastModified();
                        if (diff != 0) return diff < 0 ? -1 : 1;
                        return 0;
                    });

                    for (int i = 0; i < survivingFiles.size(); i++) {
                        if (survivingTotalSize <= maxQuotaBytes) {
                            break;
                        }
                        CacheModel.FileInfo info = survivingFiles.get(i);
                        boolean isProtected = (info.dialogId != 0 && HundredGramConfig.isVaultNeverExpire(info.dialogId));
                        if (!isProtected) {
                            filesToDelete.add(info);
                            survivingTotalSize -= info.size;
                            freedBytes += info.size;
                        }
                    }
                }

                // Also rotate files in hundredgram_vault directory
                int vaultDeletedCount = 0;
                File vaultDir = HundredGramVaultManager.getInstance(account).getVaultDir();
                if (vaultDir != null && vaultDir.exists()) {
                    File[] vFiles = vaultDir.listFiles();
                    if (vFiles != null) {
                        ArrayList<File> survivingVaultFiles = new ArrayList<>();
                        long survivingVaultSize = 0;
                        for (File vf : vFiles) {
                            if (vf == null || !vf.isFile()) continue;
                            if (vf.length() == 0) {
                                vf.delete();
                                continue;
                            }
                            if (ttlDays > 0 && (now - vf.lastModified() > maxAgeMs)) {
                                long len = vf.length();
                                if (vf.delete()) {
                                    freedBytes += len;
                                    vaultDeletedCount++;
                                }
                            } else {
                                survivingVaultFiles.add(vf);
                                survivingVaultSize += vf.length();
                            }
                        }

                        if (maxSizeGb > 0 && survivingVaultSize > maxQuotaBytes) {
                            Collections.sort(survivingVaultFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                            for (File vf : survivingVaultFiles) {
                                if (survivingVaultSize <= maxQuotaBytes) break;
                                long len = vf.length();
                                if (vf.delete()) {
                                    survivingVaultSize -= len;
                                    freedBytes += len;
                                    vaultDeletedCount++;
                                }
                            }
                        }
                    }
                }

                final int deletedCount = filesToDelete.size() + vaultDeletedCount;
                final long totalFreed = freedBytes;

                if (!filesToDelete.isEmpty()) {
                    fileDatabase.removeFiles(filesToDelete);
                    fileLoader.getFileLoaderQueue().postRunnable(() -> {
                        for (CacheModel.FileInfo info : filesToDelete) {
                            try {
                                if (info.file != null && info.file.exists()) {
                                    info.file.delete();
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                    });
                }

                isRunning.set(false);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onDone(totalFreed, deletedCount));
                }

            } catch (Exception e) {
                FileLog.e(e);
                isRunning.set(false);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onDone(0, 0));
                }
            }
        });
    }

    private static void collectFiles(File directory, ArrayList<CacheModel.FileInfo> list, FilePathDatabase database) {
        if (directory == null || !directory.exists()) return;
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                collectFiles(f, list, database);
            } else if (f.isFile() && f.length() > 0) {
                CacheModel.FileInfo info = new CacheModel.FileInfo(f);
                info.size = f.length();
                try {
                    FilePathDatabase.FileMeta meta = database.getFileDialogId(f, null);
                    if (meta != null) {
                        info.dialogId = meta.dialogId;
                    }
                } catch (Exception ignored) {}
                list.add(info);
            }
        }
    }
}
