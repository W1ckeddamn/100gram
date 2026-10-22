package com.exteragram.messenger.feed;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CustomFeed {
    public String id;
    public String title;
    public Set<Long> channelIds;
    public boolean isDefault;

    public CustomFeed() {
        this.id = UUID.randomUUID().toString();
        this.title = "";
        this.channelIds = new HashSet<>();
        this.isDefault = false;
    }

    public CustomFeed(String id, String title, Set<Long> channelIds, boolean isDefault) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title != null ? title : "";
        this.channelIds = channelIds != null ? new HashSet<>(channelIds) : new HashSet<>();
        this.isDefault = isDefault;
    }

    public CustomFeed(String id, String title, java.util.Collection<Long> channels) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title != null ? title : "";
        this.channelIds = channels != null ? new HashSet<>(channels) : new HashSet<>();
        this.isDefault = false;
    }

    public String getName() {
        return title != null ? title : "";
    }

    public int getChannelCount() {
        return channelIds != null ? channelIds.size() : 0;
    }

    public void addChannel(long dialogId) {
        if (channelIds == null) {
            channelIds = new HashSet<>();
        }
        channelIds.add(dialogId);
    }

    public void removeChannel(long dialogId) {
        if (channelIds != null) {
            channelIds.remove(dialogId);
        }
    }

    public boolean containsChannel(long dialogId) {
        return channelIds != null && channelIds.contains(dialogId);
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("title", title);
            obj.put("isDefault", isDefault);
            JSONArray arr = new JSONArray();
            if (channelIds != null) {
                for (Long cid : channelIds) {
                    arr.put(cid);
                }
            }
            obj.put("channelIds", arr);
        } catch (Exception ignored) {
        }
        return obj;
    }

    public static CustomFeed fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        try {
            String id = obj.optString("id", UUID.randomUUID().toString());
            String title = obj.optString("title", "");
            boolean isDefault = obj.optBoolean("isDefault", false);
            Set<Long> set = new HashSet<>();
            JSONArray arr = obj.optJSONArray("channelIds");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    set.add(arr.getLong(i));
                }
            }
            return new CustomFeed(id, title, set, isDefault);
        } catch (Exception e) {
            return null;
        }
    }
}
