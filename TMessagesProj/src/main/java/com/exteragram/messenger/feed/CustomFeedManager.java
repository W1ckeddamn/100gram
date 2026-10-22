package com.exteragram.messenger.feed;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;

import java.util.ArrayList;
import java.util.List;

public class CustomFeedManager {

    public static final String ALL_FEED_ID = "all";

    private static final CustomFeedManager[] instances = new CustomFeedManager[16];
    private static final Object[] locks = new Object[16];

    static {
        for (int i = 0; i < 16; i++) {
            locks[i] = new Object();
        }
    }

    private final int currentAccount;
    private final SharedPreferences preferences;
    private final ArrayList<CustomFeed> feeds = new ArrayList<>();
    private String activeFeedId = ALL_FEED_ID;

    public static CustomFeedManager getInstance(int account) {
        if (instances[account] == null) {
            synchronized (locks[account]) {
                if (instances[account] == null) {
                    instances[account] = new CustomFeedManager(account);
                }
            }
        }
        return instances[account];
    }

    private CustomFeedManager(int account) {
        this.currentAccount = account;
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("custom_feeds_" + account, 0);
        load();
    }

    private synchronized void load() {
        feeds.clear();
        activeFeedId = preferences.getString("active_feed_id", ALL_FEED_ID);
        String raw = preferences.getString("feeds_json", null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    CustomFeed feed = CustomFeed.fromJsonObject(obj);
                    if (feed != null) {
                        feeds.add(feed);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private synchronized void save() {
        try {
            JSONArray arr = new JSONArray();
            for (CustomFeed feed : feeds) {
                arr.put(feed.toJsonObject());
            }
            preferences.edit()
                    .putString("feeds_json", arr.toString())
                    .putString("active_feed_id", activeFeedId)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized List<CustomFeed> getFeeds() {
        return new ArrayList<>(feeds);
    }

    public synchronized CustomFeed getFeed(String id) {
        if (id == null) return null;
        for (CustomFeed feed : feeds) {
            if (id.equals(feed.id)) {
                return feed;
            }
        }
        return null;
    }

    public synchronized void saveFeed(CustomFeed feed) {
        if (feed == null) return;
        boolean found = false;
        for (int i = 0; i < feeds.size(); i++) {
            if (feeds.get(i).id.equals(feed.id)) {
                feeds.set(i, feed);
                found = true;
                break;
            }
        }
        if (!found) {
            feeds.add(feed);
        }
        save();
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.feedNeedReload);
    }

    public synchronized void deleteFeed(String id) {
        if (id == null) return;
        feeds.removeIf(f -> id.equals(f.id));
        if (id.equals(activeFeedId)) {
            activeFeedId = ALL_FEED_ID;
        }
        save();
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.feedNeedReload);
    }

    public synchronized String getActiveFeedId() {
        return activeFeedId;
    }

    public synchronized void setActiveFeedId(String activeFeedId) {
        this.activeFeedId = activeFeedId != null ? activeFeedId : ALL_FEED_ID;
        preferences.edit().putString("active_feed_id", this.activeFeedId).apply();
        FeedController.getInstance(currentAccount).invalidateConfig();
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.feedNeedReload);
    }

    public synchronized CustomFeed getActiveFeed() {
        if (ALL_FEED_ID.equals(activeFeedId)) {
            return null;
        }
        return getFeed(activeFeedId);
    }

    public synchronized boolean isChannelIncludedInFeed(long dialogId) {
        if (ALL_FEED_ID.equals(activeFeedId)) {
            return !FeedConfig.getInstance(currentAccount).isExcluded(dialogId);
        }
        CustomFeed customFeed = getFeed(activeFeedId);
        if (customFeed != null) {
            return customFeed.containsChannel(dialogId);
        }
        return !FeedConfig.getInstance(currentAccount).isExcluded(dialogId);
    }
}
