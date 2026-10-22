package com.exteragram.messenger.feed;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class FeedConfig {

    public static final Companion INSTANCE = new Companion();

    private static final FeedConfig[] instances = new FeedConfig[16];
    private static final Object[] lockObjects = new Object[16];

    static {
        for (int i = 0; i < 16; i++) {
            lockObjects[i] = new Object();
        }
    }

    private final SharedPreferences preferences;
    private volatile Set<Long> excludedChannels;
    private volatile boolean includeArchived;
    private volatile int generation;

    public static FeedConfig getInstance(int account) {
        return INSTANCE.getInstance(account);
    }

    private FeedConfig(int account) {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("feedconfig" + account, 0);
        includeArchived = preferences.getBoolean("includeArchived", false);
        Set<String> stringSet = preferences.getStringSet("excludedChannels", null);
        if (stringSet != null) {
            Set<Long> set = new HashSet<>();
            for (String s : stringSet) {
                try {
                    set.add(Long.parseLong(s));
                } catch (Exception ignored) {
                }
            }
            excludedChannels = set;
        } else {
            excludedChannels = Collections.emptySet();
        }
    }

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public void setIncludeArchived(boolean value) {
        if (includeArchived != value) {
            includeArchived = value;
            preferences.edit().putBoolean("includeArchived", value).apply();
            generation++;
        }
    }

    public boolean isExcluded(long dialogId) {
        return excludedChannels.contains(dialogId);
    }

    public synchronized void setExcluded(long dialogId, boolean excluded) {
        Set<Long> updated = new HashSet<>(excludedChannels);
        if (excluded) {
            updated.add(dialogId);
        } else {
            updated.remove(dialogId);
        }
        applyExcluded(updated);
    }

    public synchronized void excludeAll(Collection<Long> ids) {
        Set<Long> updated = new HashSet<>(excludedChannels);
        updated.addAll(ids);
        applyExcluded(updated);
    }

    public synchronized void removeExcluded(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        Set<Long> updated = new HashSet<>(excludedChannels);
        updated.removeAll(ids);
        applyExcluded(updated);
    }

    public synchronized void clearExcluded() {
        applyExcluded(Collections.emptySet());
    }

    private void applyExcluded(Set<Long> updated) {
        excludedChannels = updated;
        Set<String> stringSet = new HashSet<>();
        for (Long id : updated) {
            stringSet.add(String.valueOf(id));
        }
        preferences.edit().putStringSet("excludedChannels", stringSet).apply();
        generation++;
    }

    public Set<Long> getExcludedSnapshot() {
        return new HashSet<>(excludedChannels);
    }

    public int getGeneration() {
        return generation;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(includeArchived, new HashSet<>(excludedChannels), generation);
    }

    public static final class Snapshot {
        private final boolean includeArchived;
        private final Set<Long> excludedChannels;
        private final int generation;

        public Snapshot(boolean includeArchived, Set<Long> excludedChannels, int generation) {
            this.includeArchived = includeArchived;
            this.excludedChannels = excludedChannels != null ? Collections.unmodifiableSet(excludedChannels) : Collections.emptySet();
            this.generation = generation;
        }

        public boolean getIncludeArchived() {
            return includeArchived;
        }

        public Set<Long> getExcludedChannels() {
            return excludedChannels;
        }

        public int getGeneration() {
            return generation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Snapshot snapshot = (Snapshot) o;
            return includeArchived == snapshot.includeArchived &&
                    generation == snapshot.generation &&
                    Objects.equals(excludedChannels, snapshot.excludedChannels);
        }

        @Override
        public int hashCode() {
            return Objects.hash(includeArchived, excludedChannels, generation);
        }
    }

    public static final class Companion {
        private Companion() {
        }

        public FeedConfig getInstance(int account) {
            FeedConfig config = instances[account];
            if (config == null) {
                synchronized (lockObjects[account]) {
                    config = instances[account];
                    if (config == null) {
                        config = new FeedConfig(account);
                        instances[account] = config;
                    }
                }
            }
            return config;
        }
    }
}