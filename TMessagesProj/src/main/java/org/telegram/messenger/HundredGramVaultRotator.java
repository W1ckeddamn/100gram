package org.telegram.messenger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies the vault TTL / quota. Only files of the protected vault are touched: the vault settings
 * must never delete ordinary downloaded media of the user.
 */
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

        if (ttlDays <= 0 && maxSizeGb <= 0) {
            isRunning.set(false);
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onDone(0, 0));
            }
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            long freedBytes = 0;
            int deletedCount = 0;
            try {
                HundredGramVaultManager vault = HundredGramVaultManager.getInstance(account);
                // null means the vault table could not be read, so we cannot tell protected files apart
                Set<String> protectedNames = vault.getNeverExpireVaultFileNames();
                File vaultDir = vault.getVaultDir();

                if (protectedNames != null && vaultDir != null && vaultDir.exists()) {
                    long now = System.currentTimeMillis();
                    long maxAgeMs = ttlDays > 0 ? (long) ttlDays * 24L * 3600L * 1000L : Long.MAX_VALUE;
                    long maxQuotaBytes = maxSizeGb > 0 ? (long) maxSizeGb * 1024L * 1024L * 1024L : Long.MAX_VALUE;

                    ArrayList<File> surviving = new ArrayList<>();
                    File[] files = vaultDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f == null || !f.isFile()) continue;
                            if (f.length() == 0) {
                                f.delete();
                                continue;
                            }
                            if (!protectedNames.contains(f.getName()) && ttlDays > 0 && now - f.lastModified() > maxAgeMs) {
                                long len = f.length();
                                if (f.delete()) {
                                    freedBytes += len;
                                    deletedCount++;
                                    continue;
                                }
                            }
                            surviving.add(f);
                        }
                    }

                    long survivingSize = 0;
                    for (File f : surviving) {
                        survivingSize += f.length();
                    }
                    if (maxSizeGb > 0 && survivingSize > maxQuotaBytes) {
                        Collections.sort(surviving, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                        for (File f : surviving) {
                            if (survivingSize <= maxQuotaBytes) break;
                            if (protectedNames.contains(f.getName())) continue;
                            long len = f.length();
                            if (f.delete()) {
                                survivingSize -= len;
                                freedBytes += len;
                                deletedCount++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }

            final long totalFreed = freedBytes;
            final int totalDeleted = deletedCount;
            isRunning.set(false);
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onDone(totalFreed, totalDeleted));
            }
        });
    }
}
