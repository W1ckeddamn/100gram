package org.telegram.messenger;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class HundredGramChatExporter {

    public static final int FORMAT_HTML = 0;
    public static final int FORMAT_JSON = 1;
    public static final int FORMAT_MARKDOWN = 2;
    public static final int FORMAT_TXT = 3;

    public interface ExportCallback {
        void onProgress(int current, int total);
        void onSuccess(File exportedFile);
        void onError(String error);
    }

    public static class ExportOptions {
        public long dialogId;
        public int account;
        public int format;
        public int limit; // -1 for all, or 100, 500, 1000
        public boolean includeDeleted;
        public boolean includeEdits;
        public boolean includeMedia;

        public ExportOptions(long dialogId, int account, int format, int limit, boolean includeDeleted, boolean includeEdits, boolean includeMedia) {
            this.dialogId = dialogId;
            this.account = account;
            this.format = format;
            this.limit = limit;
            this.includeDeleted = includeDeleted;
            this.includeEdits = includeEdits;
            this.includeMedia = includeMedia;
        }
    }

    public static void exportChat(ExportOptions options, ExportCallback callback) {
        if (options == null) {
            if (callback != null) callback.onError("Invalid export options");
            return;
        }

        MessagesStorage storage = MessagesStorage.getInstance(options.account);
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteDatabase database = storage.getDatabase();
            if (database == null) {
                notifyError(callback, "Database not available");
                return;
            }

            try {
                // 1. Resolve chat name
                String chatName = resolveChatName(database, options.account, options.dialogId);

                // 2. Load edits if requested
                HashMap<Integer, ArrayList<TLRPC.Message>> editsMap = new HashMap<>();
                if (options.includeEdits) {
                    SQLiteCursor editsCursor = null;
                    try {
                        editsCursor = database.queryFinalized(String.format(Locale.US,
                                "SELECT mid, edit_date, data FROM hundredgram_message_edits WHERE uid = %d ORDER BY edit_date ASC",
                                options.dialogId));
                        while (editsCursor.next()) {
                            int mid = editsCursor.intValue(0);
                            NativeByteBuffer editData = editsCursor.byteBufferValue(2);
                            if (editData != null) {
                                try {
                                    TLRPC.Message editMsg = TLRPC.Message.TLdeserialize(editData, editData.readInt32(false), false);
                                    if (editMsg != null) {
                                        editMsg.id = mid;
                                        ArrayList<TLRPC.Message> list = editsMap.get(mid);
                                        if (list == null) {
                                            list = new ArrayList<>();
                                            editsMap.put(mid, list);
                                        }
                                        list.add(editMsg);
                                    }
                                } finally {
                                    editData.reuse();
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    } finally {
                        if (editsCursor != null) editsCursor.dispose();
                    }
                }

                // 3. Query messages
                StringBuilder query = new StringBuilder("SELECT m.mid, m.data, tmd.isdel FROM messages_v2 as m LEFT JOIN telegraher_message_deletions as tmd ON tmd.mid = m.mid AND tmd.uid = m.uid WHERE m.uid = ?");
                if (!options.includeDeleted) {
                    query.append(" AND (tmd.isdel IS NULL OR tmd.isdel = 0)");
                }
                query.append(" ORDER BY m.date DESC");
                if (options.limit > 0) {
                    query.append(" LIMIT ").append(options.limit);
                }

                SQLiteCursor cursor = null;
                ArrayList<TLRPC.Message> messages = new ArrayList<>();
                try {
                    cursor = database.queryFinalized(query.toString(), options.dialogId);
                    while (cursor.next()) {
                        NativeByteBuffer data = cursor.byteBufferValue(1);
                        if (data != null) {
                            try {
                                TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                                if (message != null) {
                                    int isDel = cursor.isNull(2) ? 0 : cursor.intValue(2);
                                    if (isDel == 1) {
                                        message.isDeleted = true;
                                    }
                                    messages.add(message);
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            } finally {
                                data.reuse();
                            }
                        }
                    }
                } finally {
                    if (cursor != null) cursor.dispose();
                }

                if (messages.isEmpty()) {
                    notifyError(callback, LocaleController.getString("ChatExportEmpty", R.string.ChatExportEmpty));
                    return;
                }

                // Reverse to chronological order (oldest to newest)
                Collections.reverse(messages);

                // 4. Cache authors
                HashMap<Long, String> authorCache = new HashMap<>();
                int total = messages.size();

                for (int i = 0; i < total; i++) {
                    TLRPC.Message msg = messages.get(i);
                    long senderId = getSenderId(msg);
                    if (!authorCache.containsKey(senderId)) {
                        authorCache.put(senderId, resolveAuthorName(database, options.account, msg));
                    }
                    if (i % 250 == 0) {
                        final int cur = i;
                        notifyProgress(callback, cur, total);
                    }
                }

                // 5. Build export file content
                File exportDir = getExportDirectory();
                String ext;
                switch (options.format) {
                    case FORMAT_HTML: ext = "html"; break;
                    case FORMAT_JSON: ext = "json"; break;
                    case FORMAT_MARKDOWN: ext = "md"; break;
                    case FORMAT_TXT: default: ext = "txt"; break;
                }

                String cleanTitle = chatName.replaceAll("[^a-zA-Z0-9а-яА-Я._-]", "_");
                if (cleanTitle.length() > 30) cleanTitle = cleanTitle.substring(0, 30);
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String fileName = "export_" + cleanTitle + "_" + timestamp + "." + ext;
                File targetFile = new File(exportDir, fileName);

                switch (options.format) {
                    case FORMAT_HTML:
                        writeHtmlExport(targetFile, chatName, options.dialogId, messages, authorCache, editsMap, options.includeMedia);
                        break;
                    case FORMAT_JSON:
                        writeJsonExport(targetFile, chatName, options.dialogId, messages, authorCache, editsMap, options.includeMedia);
                        break;
                    case FORMAT_MARKDOWN:
                        writeMarkdownExport(targetFile, chatName, options.dialogId, messages, authorCache, editsMap, options.includeMedia);
                        break;
                    case FORMAT_TXT:
                    default:
                        writeTxtExport(targetFile, chatName, options.dialogId, messages, authorCache, editsMap, options.includeMedia);
                        break;
                }

                notifyProgress(callback, total, total);
                notifySuccess(callback, targetFile);

            } catch (Exception e) {
                FileLog.e(e);
                notifyError(callback, e.getMessage());
            }
        });
    }

    private static File getExportDirectory() {
        File dir = null;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                dir = new File(downloads, "100gram_exports");
            } else {
                File external = Environment.getExternalStorageDirectory();
                dir = new File(external, "100gram_exports");
            }
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception ignored) {}

        if (dir == null || !dir.exists() || !dir.canWrite()) {
            dir = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "100gram_exports");
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

    private static long getSenderId(TLRPC.Message message) {
        if (message == null) return 0;
        if (message.from_id != null) {
            if (message.from_id.user_id != 0) return message.from_id.user_id;
            if (message.from_id.chat_id != 0) return -message.from_id.chat_id;
            if (message.from_id.channel_id != 0) return -message.from_id.channel_id;
        }
        if (message.peer_id != null) {
            if (message.peer_id.user_id != 0) return message.peer_id.user_id;
            if (message.peer_id.chat_id != 0) return -message.peer_id.chat_id;
            if (message.peer_id.channel_id != 0) return -message.peer_id.channel_id;
        }
        return 0;
    }

    private static String resolveChatName(SQLiteDatabase database, int account, long dialogId) {
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
            if (user != null) {
                return ContactsController.formatName(user.first_name, user.last_name);
            }
            try {
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM users WHERE uid = %d", dialogId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        try {
                            TLRPC.User u = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                            if (u != null) {
                                return ContactsController.formatName(u.first_name, u.last_name);
                            }
                        } finally {
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();
            } catch (Exception ignored) {}
            return "User " + dialogId;
        } else {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            if (chat != null && !TextUtils.isEmpty(chat.title)) {
                return chat.title;
            }
            try {
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM chats WHERE uid = %d", chatId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        try {
                            TLRPC.Chat c = TLRPC.Chat.TLdeserialize(data, data.readInt32(false), false);
                            if (c != null && !TextUtils.isEmpty(c.title)) {
                                return c.title;
                            }
                        } finally {
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();
            } catch (Exception ignored) {}
            return "Chat " + dialogId;
        }
    }

    private static String resolveAuthorName(SQLiteDatabase database, int account, TLRPC.Message message) {
        if (message == null) return "Unknown";
        if (!TextUtils.isEmpty(message.post_author)) {
            return message.post_author;
        }
        long senderId = getSenderId(message);
        if (senderId > 0) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(senderId);
            if (user != null) {
                return ContactsController.formatName(user.first_name, user.last_name);
            }
            try {
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM users WHERE uid = %d", senderId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        try {
                            TLRPC.User u = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                            if (u != null) {
                                return ContactsController.formatName(u.first_name, u.last_name);
                            }
                        } finally {
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();
            } catch (Exception ignored) {}
            return "User " + senderId;
        } else if (senderId < 0) {
            long chatId = -senderId;
            TLRPC.Chat chat = MessagesController.getInstance(account).getChat(chatId);
            if (chat != null && !TextUtils.isEmpty(chat.title)) {
                return chat.title;
            }
            try {
                SQLiteCursor cursor = database.queryFinalized(String.format(Locale.US, "SELECT data FROM chats WHERE uid = %d", chatId));
                if (cursor.next()) {
                    NativeByteBuffer data = cursor.byteBufferValue(0);
                    if (data != null) {
                        try {
                            TLRPC.Chat c = TLRPC.Chat.TLdeserialize(data, data.readInt32(false), false);
                            if (c != null && !TextUtils.isEmpty(c.title)) {
                                return c.title;
                            }
                        } finally {
                            data.reuse();
                        }
                    }
                }
                cursor.dispose();
            } catch (Exception ignored) {}
            return "Group " + chatId;
        }
        return "Unknown";
    }

    private static String getMediaDescription(TLRPC.Message message) {
        if (message == null || message.media == null) return "";
        TLRPC.MessageMedia media = message.media;
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            return "[Photo]";
        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.TL_messageMediaDocument docMedia = (TLRPC.TL_messageMediaDocument) media;
            if (docMedia.document != null) {
                String fileName = FileLoader.getDocumentFileName(docMedia.document);
                for (int a = 0; a < docMedia.document.attributes.size(); a++) {
                    TLRPC.DocumentAttribute attr = docMedia.document.attributes.get(a);
                    if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                        return "[Video" + (TextUtils.isEmpty(fileName) ? "" : ": " + fileName) + "]";
                    } else if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                        return "[Audio/Voice" + (TextUtils.isEmpty(fileName) ? "" : ": " + fileName) + "]";
                    } else if (attr instanceof TLRPC.TL_documentAttributeSticker) {
                        return "[Sticker" + (attr.alt != null ? " " + attr.alt : "") + "]";
                    } else if (attr instanceof TLRPC.TL_documentAttributeAnimated) {
                        return "[GIF]";
                    }
                }
                return "[Document: " + (!TextUtils.isEmpty(fileName) ? fileName : "file") + "]";
            }
            return "[Document]";
        } else if (media instanceof TLRPC.TL_messageMediaContact) {
            TLRPC.TL_messageMediaContact contact = (TLRPC.TL_messageMediaContact) media;
            return "[Contact: " + contact.first_name + " " + contact.last_name + " (" + contact.phone_number + ")]";
        } else if (media instanceof TLRPC.TL_messageMediaGeo || media instanceof TLRPC.TL_messageMediaVenue) {
            return "[Location]";
        } else if (media instanceof TLRPC.TL_messageMediaPoll) {
            TLRPC.TL_messageMediaPoll poll = (TLRPC.TL_messageMediaPoll) media;
            return "[Poll: " + (poll.poll != null && poll.poll.question != null ? poll.poll.question.text : "") + "]";
        } else if (media instanceof TLRPC.TL_messageMediaWebPage) {
            return "[Web Page Preview]";
        }
        return "[Media]";
    }

    // ==========================================
    // EXPORT WRITERS
    // ==========================================

    private static void writeHtmlExport(File file, String chatName, long dialogId, ArrayList<TLRPC.Message> messages,
                                       HashMap<Long, String> authorCache, HashMap<Integer, ArrayList<TLRPC.Message>> editsMap,
                                       boolean includeMedia) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n");
            writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("<title>100gram Export - " + escapeHtml(chatName) + "</title>\n");
            writer.write("<style>\n");
            writer.write("body { background-color: #0e1621; color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 0; padding: 16px; }\n");
            writer.write(".container { max-width: 800px; margin: 0 auto; }\n");
            writer.write(".header { background: #17212b; padding: 20px; border-radius: 12px; margin-bottom: 20px; border: 1px solid #2b394a; }\n");
            writer.write(".header h1 { margin: 0 0 8px 0; font-size: 22px; color: #5288c1; }\n");
            writer.write(".header p { margin: 4px 0; font-size: 13px; color: #7f91a4; }\n");
            writer.write(".msg { margin-bottom: 12px; display: flex; flex-direction: column; }\n");
            writer.write(".msg-bubble { background: #182533; border-radius: 10px; padding: 10px 14px; max-width: 85%; align-self: flex-start; border: 1px solid #242f3d; word-wrap: break-word; }\n");
            writer.write(".msg-out { align-self: flex-end; background: #2b5278; border-color: #3b6b98; }\n");
            writer.write(".msg-author { font-size: 13px; font-weight: bold; color: #64b5f6; margin-bottom: 4px; }\n");
            writer.write(".msg-text { font-size: 14px; line-height: 1.45; white-space: pre-wrap; }\n");
            writer.write(".msg-media { font-size: 13px; color: #40c4ff; margin: 4px 0; font-style: italic; }\n");
            writer.write(".msg-meta { font-size: 11px; color: #7f91a4; margin-top: 6px; display: flex; align-items: center; justify-content: flex-end; gap: 8px; }\n");
            writer.write(".badge-del { background: #e53935; color: #fff; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; }\n");
            writer.write(".badge-edit { background: #ffa726; color: #000; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; }\n");
            writer.write(".edits-details { margin-top: 6px; font-size: 12px; color: #8ab4f8; background: rgba(0,0,0,0.2); padding: 6px 10px; border-radius: 6px; }\n");
            writer.write(".edits-details summary { cursor: pointer; font-weight: bold; outline: none; }\n");
            writer.write(".edits-list { list-style: none; padding-left: 0; margin: 6px 0 0 0; }\n");
            writer.write(".edits-list li { border-top: 1px dashed rgba(255,255,255,0.15); padding: 4px 0; font-size: 12px; }\n");
            writer.write("</style>\n</head>\n<body>\n<div class=\"container\">\n");

            writer.write("<div class=\"header\">\n");
            writer.write("<h1>" + escapeHtml(chatName) + "</h1>\n");
            writer.write("<p>Dialog ID: " + dialogId + "</p>\n");
            writer.write("<p>Export Date: " + sdf.format(new Date()) + "</p>\n");
            writer.write("<p>Total Messages: " + messages.size() + "</p>\n");
            writer.write("</div>\n");

            for (TLRPC.Message msg : messages) {
                boolean isOut = msg.out;
                String author = authorCache.get(getSenderId(msg));
                if (author == null) author = "Unknown";
                String dateStr = sdf.format(new Date(msg.date * 1000L));

                writer.write("<div class=\"msg\">\n");
                writer.write("  <div class=\"msg-bubble" + (isOut ? " msg-out" : "") + "\">\n");
                writer.write("    <div class=\"msg-author\">" + escapeHtml(author) + "</div>\n");

                if (includeMedia) {
                    String mediaDesc = getMediaDescription(msg);
                    if (!TextUtils.isEmpty(mediaDesc)) {
                        writer.write("    <div class=\"msg-media\">" + escapeHtml(mediaDesc) + "</div>\n");
                    }
                }

                if (!TextUtils.isEmpty(msg.message)) {
                    writer.write("    <div class=\"msg-text\">" + escapeHtml(msg.message) + "</div>\n");
                }

                // Edits
                ArrayList<TLRPC.Message> edits = editsMap.get(msg.id);
                if (edits != null && !edits.isEmpty()) {
                    writer.write("    <details class=\"edits-details\">\n");
                    writer.write("      <summary>Edit History (" + edits.size() + " revisions)</summary>\n");
                    writer.write("      <ul class=\"edits-list\">\n");
                    for (TLRPC.Message edit : edits) {
                        String editDate = sdf.format(new Date(edit.edit_date > 0 ? edit.edit_date * 1000L : edit.date * 1000L));
                        writer.write("        <li><strong>" + editDate + ":</strong> " + escapeHtml(edit.message != null ? edit.message : "") + "</li>\n");
                    }
                    writer.write("      </ul>\n");
                    writer.write("    </details>\n");
                }

                writer.write("    <div class=\"msg-meta\">\n");
                if (msg.isDeleted) {
                    writer.write("      <span class=\"badge-del\">Deleted</span>\n");
                }
                if (msg.edit_date > 0 || (edits != null && !edits.isEmpty())) {
                    writer.write("      <span class=\"badge-edit\">Edited</span>\n");
                }
                writer.write("      <span>" + dateStr + " (ID: " + msg.id + ")</span>\n");
                writer.write("    </div>\n");

                writer.write("  </div>\n");
                writer.write("</div>\n");
            }

            writer.write("</div>\n</body>\n</html>\n");
        }
    }

    private static void writeJsonExport(File file, String chatName, long dialogId, ArrayList<TLRPC.Message> messages,
                                       HashMap<Long, String> authorCache, HashMap<Integer, ArrayList<TLRPC.Message>> editsMap,
                                       boolean includeMedia) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        JSONObject root = new JSONObject();
        root.put("chat_name", chatName);
        root.put("dialog_id", dialogId);
        root.put("export_date", sdf.format(new Date()));
        root.put("total_messages", messages.size());

        JSONArray msgArray = new JSONArray();
        for (TLRPC.Message msg : messages) {
            JSONObject m = new JSONObject();
            m.put("id", msg.id);
            m.put("date", msg.date);
            m.put("date_formatted", sdf.format(new Date(msg.date * 1000L)));
            m.put("sender_id", getSenderId(msg));
            m.put("sender_name", authorCache.get(getSenderId(msg)));
            m.put("out", msg.out);
            m.put("text", msg.message != null ? msg.message : "");
            m.put("is_deleted", msg.isDeleted);
            m.put("is_edited", msg.edit_date > 0);

            if (includeMedia) {
                String media = getMediaDescription(msg);
                if (!TextUtils.isEmpty(media)) {
                    m.put("media", media);
                }
            }

            ArrayList<TLRPC.Message> edits = editsMap.get(msg.id);
            if (edits != null && !edits.isEmpty()) {
                JSONArray editsArr = new JSONArray();
                for (TLRPC.Message edit : edits) {
                    JSONObject e = new JSONObject();
                    e.put("edit_date", edit.edit_date > 0 ? edit.edit_date : edit.date);
                    e.put("date_formatted", sdf.format(new Date(edit.edit_date > 0 ? edit.edit_date * 1000L : edit.date * 1000L)));
                    e.put("text", edit.message != null ? edit.message : "");
                    editsArr.put(e);
                }
                m.put("edit_history", editsArr);
            }

            msgArray.put(m);
        }
        root.put("messages", msgArray);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(root.toString(2));
        }
    }

    private static void writeMarkdownExport(File file, String chatName, long dialogId, ArrayList<TLRPC.Message> messages,
                                           HashMap<Long, String> authorCache, HashMap<Integer, ArrayList<TLRPC.Message>> editsMap,
                                           boolean includeMedia) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("# Chat Export: " + chatName + "\n\n");
            writer.write("- **Dialog ID:** `" + dialogId + "`\n");
            writer.write("- **Export Date:** `" + sdf.format(new Date()) + "`\n");
            writer.write("- **Total Messages:** `" + messages.size() + "`\n\n");
            writer.write("---\n\n");

            for (TLRPC.Message msg : messages) {
                String author = authorCache.get(getSenderId(msg));
                if (author == null) author = "Unknown";
                String dateStr = sdf.format(new Date(msg.date * 1000L));

                writer.write("### **" + author + "** &middot; `" + dateStr + "` `(ID: " + msg.id + ")`");
                if (msg.isDeleted) {
                    writer.write(" **[DELETED]**");
                }
                if (msg.edit_date > 0) {
                    writer.write(" *[Edited]*");
                }
                writer.write("\n\n");

                if (includeMedia) {
                    String media = getMediaDescription(msg);
                    if (!TextUtils.isEmpty(media)) {
                        writer.write("*" + media + "*\n\n");
                    }
                }

                if (!TextUtils.isEmpty(msg.message)) {
                    // Indent as blockquote
                    String[] lines = msg.message.split("\n");
                    for (String line : lines) {
                        writer.write("> " + line + "\n");
                    }
                    writer.write("\n");
                }

                ArrayList<TLRPC.Message> edits = editsMap.get(msg.id);
                if (edits != null && !edits.isEmpty()) {
                    writer.write("<details>\n<summary>Edit History (" + edits.size() + " revisions)</summary>\n\n");
                    for (TLRPC.Message edit : edits) {
                        String editDate = sdf.format(new Date(edit.edit_date > 0 ? edit.edit_date * 1000L : edit.date * 1000L));
                        writer.write("- **" + editDate + ":** " + (edit.message != null ? edit.message : "") + "\n");
                    }
                    writer.write("</details>\n\n");
                }

                writer.write("---\n\n");
            }
        }
    }

    private static void writeTxtExport(File file, String chatName, long dialogId, ArrayList<TLRPC.Message> messages,
                                      HashMap<Long, String> authorCache, HashMap<Integer, ArrayList<TLRPC.Message>> editsMap,
                                      boolean includeMedia) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("====================================================\n");
            writer.write("100gram Chat Export: " + chatName + " (ID: " + dialogId + ")\n");
            writer.write("Date: " + sdf.format(new Date()) + "\n");
            writer.write("Total Messages: " + messages.size() + "\n");
            writer.write("====================================================\n\n");

            for (TLRPC.Message msg : messages) {
                String author = authorCache.get(getSenderId(msg));
                if (author == null) author = "Unknown";
                String dateStr = sdf.format(new Date(msg.date * 1000L));

                StringBuilder line = new StringBuilder();
                line.append("[").append(dateStr).append("] ").append(author).append(": ");

                if (includeMedia) {
                    String media = getMediaDescription(msg);
                    if (!TextUtils.isEmpty(media)) {
                        line.append(media).append(" ");
                    }
                }

                if (!TextUtils.isEmpty(msg.message)) {
                    line.append(msg.message);
                }

                if (msg.isDeleted) {
                    line.append(" [DELETED]");
                }
                if (msg.edit_date > 0) {
                    line.append(" [EDITED]");
                }

                writer.write(line.toString() + "\n");

                ArrayList<TLRPC.Message> edits = editsMap.get(msg.id);
                if (edits != null && !edits.isEmpty()) {
                    for (TLRPC.Message edit : edits) {
                        String editDate = sdf.format(new Date(edit.edit_date > 0 ? edit.edit_date * 1000L : edit.date * 1000L));
                        writer.write("    -> (Edit " + editDate + "): " + (edit.message != null ? edit.message : "") + "\n");
                    }
                }
            }
        }
    }

    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void notifyProgress(ExportCallback callback, int current, int total) {
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onProgress(current, total));
        }
    }

    private static void notifySuccess(ExportCallback callback, File file) {
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(file));
        }
    }

    private static void notifyError(ExportCallback callback, String error) {
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onError(error));
        }
    }
}
