package org.telegram.messenger;

import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HundredGramPlaylistManager {

    public static class CustomPlaylist {
        private String id;
        private String title;
        private long createdAt;
        private final ArrayList<String> serializedTracks = new ArrayList<>();

        public CustomPlaylist(String id, String title, long createdAt) {
            this.id = id;
            this.title = title;
            this.createdAt = createdAt;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public int getTrackCount() {
            return serializedTracks.size();
        }

        public synchronized List<MessageObject> getMessageObjects(int account) {
            ArrayList<MessageObject> list = new ArrayList<>();
            for (String raw : serializedTracks) {
                try {
                    byte[] bytes = Base64.decode(raw, Base64.NO_WRAP);
                    SerializedData data = new SerializedData(bytes);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        MessageObject msgObj = new MessageObject(account, message, false, false);
                        list.add(msgObj);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return list;
        }

        public synchronized boolean addTrack(MessageObject messageObject) {
            if (messageObject == null || messageObject.messageOwner == null) {
                return false;
            }
            if (containsTrack(messageObject)) {
                return false;
            }
            try {
                SerializedData data = new SerializedData(messageObject.messageOwner.getObjectSize());
                messageObject.messageOwner.serializeToStream(data);
                byte[] bytes = data.toByteArray();
                String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
                serializedTracks.add(encoded);
                return true;
            } catch (Exception e) {
                FileLog.e(e);
                return false;
            }
        }

        public synchronized boolean removeTrack(int index) {
            if (index >= 0 && index < serializedTracks.size()) {
                serializedTracks.remove(index);
                return true;
            }
            return false;
        }

        public synchronized boolean containsTrack(MessageObject messageObject) {
            if (messageObject == null) return false;
            TLRPC.Document targetDoc = messageObject.getDocument();
            if (targetDoc == null) return false;

            for (String raw : serializedTracks) {
                try {
                    byte[] bytes = Base64.decode(raw, Base64.NO_WRAP);
                    SerializedData data = new SerializedData(bytes);
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null && message.media instanceof TLRPC.TL_messageMediaDocument) {
                        TLRPC.Document doc = message.media.document;
                        if (doc != null && doc.id == targetDoc.id) {
                            return true;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return false;
        }

        public JSONObject toJsonObject() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("title", title);
                obj.put("createdAt", createdAt);
                JSONArray arr = new JSONArray();
                for (String track : serializedTracks) {
                    arr.put(track);
                }
                obj.put("tracks", arr);
                return obj;
            } catch (Exception e) {
                return null;
            }
        }

        public static CustomPlaylist fromJsonObject(JSONObject obj) {
            if (obj == null) return null;
            try {
                String id = obj.getString("id");
                String title = obj.getString("title");
                long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
                CustomPlaylist playlist = new CustomPlaylist(id, title, createdAt);
                JSONArray arr = obj.optJSONArray("tracks");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        playlist.serializedTracks.add(arr.getString(i));
                    }
                }
                return playlist;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static final HundredGramPlaylistManager[] instances = new HundredGramPlaylistManager[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Object[] locks = new Object[UserConfig.MAX_ACCOUNT_COUNT];

    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            locks[i] = new Object();
        }
    }

    private final int currentAccount;
    private final SharedPreferences preferences;
    private final ArrayList<CustomPlaylist> playlists = new ArrayList<>();

    public static HundredGramPlaylistManager getInstance(int account) {
        if (instances[account] == null) {
            synchronized (locks[account]) {
                if (instances[account] == null) {
                    instances[account] = new HundredGramPlaylistManager(account);
                }
            }
        }
        return instances[account];
    }

    private HundredGramPlaylistManager(int account) {
        this.currentAccount = account;
        this.preferences = ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_playlists_" + account, 0);
        load();
    }

    private synchronized void load() {
        playlists.clear();
        String raw = preferences.getString("playlists_json", null);
        if (raw != null) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    CustomPlaylist playlist = CustomPlaylist.fromJsonObject(obj);
                    if (playlist != null) {
                        playlists.add(playlist);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public synchronized void save() {
        try {
            JSONArray arr = new JSONArray();
            for (CustomPlaylist playlist : playlists) {
                JSONObject obj = playlist.toJsonObject();
                if (obj != null) {
                    arr.put(obj);
                }
            }
            preferences.edit().putString("playlists_json", arr.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public synchronized List<CustomPlaylist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    public synchronized CustomPlaylist getPlaylist(String id) {
        if (id == null) return null;
        for (CustomPlaylist playlist : playlists) {
            if (id.equals(playlist.getId())) {
                return playlist;
            }
        }
        return null;
    }

    public synchronized CustomPlaylist createPlaylist(String title) {
        CustomPlaylist playlist = new CustomPlaylist(UUID.randomUUID().toString(), title, System.currentTimeMillis());
        playlists.add(playlist);
        save();
        return playlist;
    }

    public synchronized void deletePlaylist(String id) {
        if (id == null) return;
        playlists.removeIf(p -> id.equals(p.getId()));
        save();
    }

    public synchronized void renamePlaylist(String id, String newTitle) {
        if (id == null || newTitle == null) return;
        CustomPlaylist playlist = getPlaylist(id);
        if (playlist != null) {
            playlist.setTitle(newTitle);
            save();
        }
    }

    public synchronized boolean addTrackToPlaylist(String playlistId, MessageObject messageObject) {
        CustomPlaylist playlist = getPlaylist(playlistId);
        if (playlist != null) {
            boolean added = playlist.addTrack(messageObject);
            if (added) {
                save();
            }
            return added;
        }
        return false;
    }

    public synchronized void playPlaylist(CustomPlaylist playlist, int startIndex) {
        if (playlist == null) return;
        List<MessageObject> objects = playlist.getMessageObjects(currentAccount);
        if (objects.isEmpty()) return;
        int idx = Math.max(0, Math.min(startIndex, objects.size() - 1));
        MediaController.getInstance().setPlaylist(new ArrayList<>(objects), objects.get(idx), 0L);
    }
}
