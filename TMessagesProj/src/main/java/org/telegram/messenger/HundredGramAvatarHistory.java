package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class HundredGramAvatarHistory implements NotificationCenter.NotificationCenterDelegate {

    private static volatile HundredGramAvatarHistory Instance;

    public static HundredGramAvatarHistory getInstance() {
        HundredGramAvatarHistory localInstance = Instance;
        if (localInstance == null) {
            synchronized (HundredGramAvatarHistory.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new HundredGramAvatarHistory();
                }
            }
        }
        return localInstance;
    }

    public static class AvatarEntry {
        public final long dialogId;
        public final long photoId;
        public final long timestamp;
        public final String localPath;

        public AvatarEntry(long dialogId, long photoId, long timestamp, String localPath) {
            this.dialogId = dialogId;
            this.photoId = photoId;
            this.timestamp = timestamp;
            this.localPath = localPath;
        }

        public JSONObject toJson() {
            try {
                JSONObject json = new JSONObject();
                json.put("dialogId", dialogId);
                json.put("photoId", photoId);
                json.put("timestamp", timestamp);
                json.put("localPath", localPath);
                return json;
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }

        public static AvatarEntry fromJson(JSONObject json) {
            if (json == null) return null;
            try {
                long dialogId = json.optLong("dialogId");
                long photoId = json.optLong("photoId");
                long timestamp = json.optLong("timestamp");
                String localPath = json.optString("localPath");
                return new AvatarEntry(dialogId, photoId, timestamp, localPath);
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }
    }

    private final HashSet<String> pendingDownloads = new HashSet<>();

    private HundredGramAvatarHistory() {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.fileLoaded);
        }
    }

    private File getAvatarsDir() {
        File dir = new File(ApplicationLoader.getFilesDirFixed(), "avatar_history");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getHistoryIndexFile(long dialogId) {
        return new File(getAvatarsDir(), "history_" + dialogId + ".json");
    }

    public synchronized ArrayList<AvatarEntry> getAvatarHistory(long dialogId) {
        ArrayList<AvatarEntry> result = new ArrayList<>();
        File indexFile = getHistoryIndexFile(dialogId);
        if (!indexFile.exists()) {
            return result;
        }
        try {
            FileInputStream fis = new FileInputStream(indexFile);
            byte[] bytes = new byte[(int) indexFile.length()];
            fis.read(bytes);
            fis.close();
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                AvatarEntry entry = AvatarEntry.fromJson(array.getJSONObject(i));
                if (entry != null) {
                    File f = new File(entry.localPath);
                    if (f.exists()) {
                        result.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        Collections.sort(result, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        return result;
    }

    public int getAvatarHistoryCount(long dialogId) {
        return getAvatarHistory(dialogId).size();
    }

    private synchronized void saveAvatarHistory(long dialogId, ArrayList<AvatarEntry> list) {
        try {
            JSONArray array = new JSONArray();
            for (AvatarEntry entry : list) {
                JSONObject json = entry.toJson();
                if (json != null) {
                    array.put(json);
                }
            }
            File indexFile = getHistoryIndexFile(dialogId);
            FileOutputStream fos = new FileOutputStream(indexFile);
            fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void checkAndRecordAvatar(int account, TLRPC.User user) {
        if (!HundredGramConfig.avatarHistoryEnabled || user == null || user.photo == null) {
            return;
        }
        long dialogId = user.id;
        long photoId = user.photo.photo_id;
        if (photoId == 0 && user.photo.photo_big != null) {
            photoId = user.photo.photo_big.volume_id ^ user.photo.photo_big.local_id;
        }
        if (photoId == 0) return;

        recordAvatarInternal(account, dialogId, photoId, user.photo.photo_big, user);
    }

    public void checkAndRecordAvatar(int account, TLRPC.Chat chat) {
        if (!HundredGramConfig.avatarHistoryEnabled || chat == null || chat.photo == null) {
            return;
        }
        long dialogId = -chat.id;
        long photoId = chat.photo.photo_id;
        if (photoId == 0 && chat.photo.photo_big != null) {
            photoId = chat.photo.photo_big.volume_id ^ chat.photo.photo_big.local_id;
        }
        if (photoId == 0) return;

        recordAvatarInternal(account, dialogId, photoId, chat.photo.photo_big, chat);
    }

    private void recordAvatarInternal(int account, long dialogId, long photoId, TLRPC.FileLocation location, Object parentObject) {
        if (location == null) return;

        // Check if already in history
        ArrayList<AvatarEntry> history = getAvatarHistory(dialogId);
        for (AvatarEntry entry : history) {
            if (entry.photoId == photoId) {
                return;
            }
        }

        File srcFile = FileLoader.getInstance(account).getPathToAttach(location, true);
        if (srcFile != null && srcFile.exists()) {
            saveAvatarFile(dialogId, photoId, srcFile);
        } else {
            // Not yet downloaded, queue download and listen
            ImageLocation imageLocation;
            if (parentObject instanceof TLRPC.User) {
                imageLocation = ImageLocation.getForUserOrChat((TLRPC.User) parentObject, ImageLocation.TYPE_BIG);
            } else if (parentObject instanceof TLRPC.Chat) {
                imageLocation = ImageLocation.getForUserOrChat((TLRPC.Chat) parentObject, ImageLocation.TYPE_BIG);
            } else {
                imageLocation = ImageLocation.getForLocal(location);
            }
            if (imageLocation != null) {
                String attachFileName = FileLoader.getAttachFileName(location);
                String key = dialogId + "_" + photoId + "_" + attachFileName;
                pendingDownloads.add(key);
                FileLoader.getInstance(account).loadFile(imageLocation, parentObject, null, FileLoader.PRIORITY_LOW, 1);
            }
        }
    }

    private synchronized void saveAvatarFile(long dialogId, long photoId, File srcFile) {
        try {
            File destFile = new File(getAvatarsDir(), "avatar_" + dialogId + "_" + photoId + ".jpg");
            if (!destFile.exists()) {
                copyFile(srcFile, destFile);
            }
            ArrayList<AvatarEntry> history = getAvatarHistory(dialogId);
            boolean exists = false;
            for (AvatarEntry entry : history) {
                if (entry.photoId == photoId) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                history.add(new AvatarEntry(dialogId, photoId, System.currentTimeMillis(), destFile.getAbsolutePath()));
                saveAvatarHistory(dialogId, history);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoaded) {
            String fileName = (String) args[0];
            File file = (File) args[1];
            if (fileName == null || file == null || !file.exists()) {
                return;
            }
            ArrayList<String> toRemove = new ArrayList<>();
            for (String key : pendingDownloads) {
                String[] parts = key.split("_", 3);
                if (parts.length == 3 && parts[2].equals(fileName)) {
                    long dialogId = Utilities.parseLong(parts[0]);
                    long photoId = Utilities.parseLong(parts[1]);
                    saveAvatarFile(dialogId, photoId, file);
                    toRemove.add(key);
                }
            }
            pendingDownloads.removeAll(toRemove);
        }
    }

    public static boolean saveToGallery(Context context, File sourceFile) {
        if (context == null || sourceFile == null || !sourceFile.exists()) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, sourceFile.getName());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/100gram");
                Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream os = context.getContentResolver().openOutputStream(uri);
                    InputStream is = new FileInputStream(sourceFile);
                    copyStream(is, os);
                    is.close();
                    if (os != null) os.close();
                    return true;
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "100gram");
                if (!dir.exists()) dir.mkdirs();
                File dest = new File(dir, sourceFile.getName());
                copyFile(sourceFile, dest);
                MediaController.saveFile(dest.getAbsolutePath(), context, 0, null, null);
                return true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    private static void copyFile(File src, File dst) throws Exception {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        copyStream(in, out);
        in.close();
        out.close();
    }

    private static void copyStream(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }
}
