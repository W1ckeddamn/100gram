package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.telegram.ui.ChatActivity;

/**
 * 100gram: user-defined order and visibility of the message context menu.
 * Ported from OpexGram (ac.h2). Default state leaves the menu untouched.
 */
public class HundredGramMessageMenu {

    public static final int GROUP_COUNT = 6;

    public static final class Entry {
        public final String key;
        public final int group;
        public final int labelRes;
        public final int iconRes;
        public final int[] options;

        Entry(String key, int group, int labelRes, int iconRes, int... options) {
            this.key = key;
            this.group = group;
            this.labelRes = labelRes;
            this.iconRes = iconRes;
            this.options = options;
        }
    }

    private static final LinkedHashMap<String, Entry> ITEMS = new LinkedHashMap<>();
    private static final HashMap<Integer, String> BY_OPTION = new HashMap<>();
    private static final ArrayList<String> DEFAULT_ORDER = new ArrayList<>();

    private static final ArrayList<String> order = new ArrayList<>();
    private static final HashSet<String> hidden = new HashSet<>();
    private static boolean loaded;

    static {
        add(0, "reply", R.string.Reply, R.drawable.menu_reply, ChatActivity.OPTION_REPLY);
        add(1, "copy", R.string.Copy, R.drawable.msg_copy, ChatActivity.OPTION_COPY, ChatActivity.OPTION_COPY_PHONE_NUMBER);
        add(1, "copylink", R.string.CopyLink, R.drawable.msg_link, ChatActivity.OPTION_COPY_LINK);
        add(0, "forward", R.string.Forward, R.drawable.msg_forward, ChatActivity.OPTION_FORWARD);
        add(2, "pin", R.string.PinMessage, R.drawable.msg_pin, ChatActivity.OPTION_PIN, ChatActivity.OPTION_UNPIN);
        add(3, "translate", R.string.TranslateMessage, R.drawable.msg_translate, ChatActivity.OPTION_TRANSLATE);
        add(2, "edit", R.string.Edit, R.drawable.msg_edit, ChatActivity.OPTION_EDIT);
        add(2, "delete", R.string.Delete, R.drawable.msg_delete, ChatActivity.OPTION_DELETE);
        add(4, "gallery", R.string.SaveToGallery, R.drawable.msg_gallery, ChatActivity.OPTION_SAVE_TO_GALLERY, ChatActivity.OPTION_SAVE_TO_GALLERY2);
        add(1, "copypic", R.string.CopyPhoto, R.drawable.msg_copy, ChatActivity.OPTION_COPY_PHOTO);
        add(4, "downloads", R.string.SaveToDownloads, R.drawable.msg_download, ChatActivity.OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC);
        add(0, "share", R.string.ShareFile, R.drawable.msg_shareout, ChatActivity.OPTION_SHARE);
        add(4, "gif", R.string.SaveToGIFs, R.drawable.msg_gif, ChatActivity.OPTION_ADD_TO_GIFS);
        add(4, "stickers", R.string.AddToStickers, R.drawable.msg_sticker, ChatActivity.OPTION_ADD_TO_STICKERS_OR_MASKS);
        add(4, "favorites", R.string.AddToFavorites, R.drawable.msg_fave, ChatActivity.OPTION_ADD_STICKER_TO_FAVORITES, ChatActivity.OPTION_DELETE_STICKER_FROM_FAVORITES);
        add(5, "thread", R.string.ViewThread, R.drawable.msg_viewreplies, ChatActivity.OPTION_VIEW_REPLIES_OR_THREAD);
        add(5, "topic", R.string.ViewInTopic, R.drawable.msg_viewintopic, ChatActivity.OPTION_VIEW_IN_TOPIC);
        add(2, "factcheck", R.string.AddFactCheck, R.drawable.menu_factcheck, ChatActivity.OPTION_FACT_CHECK);
        add(5, "contact", R.string.AddContactTitle, R.drawable.msg_addcontact, ChatActivity.OPTION_ADD_CONTACT);
        add(5, "call", R.string.Call, R.drawable.msg_callback, ChatActivity.OPTION_CALL, ChatActivity.OPTION_CALL_AGAIN);
        add(5, "sendnow", R.string.MessageScheduleSend, R.drawable.msg_send, ChatActivity.OPTION_SEND_NOW);
        add(5, "editschedule", R.string.MessageScheduleEditTime, R.drawable.msg_calendar2, ChatActivity.OPTION_EDIT_SCHEDULE_TIME);
        add(2, "report", R.string.ReportChat, R.drawable.msg_report, ChatActivity.OPTION_REPORT_CHAT);
        DEFAULT_ORDER.addAll(ITEMS.keySet());
        order.addAll(DEFAULT_ORDER);
    }

    private static void add(int group, String key, int labelRes, int iconRes, int... options) {
        Entry entry = new Entry(key, group, labelRes, iconRes, options);
        ITEMS.put(key, entry);
        for (int option : options) {
            BY_OPTION.put(option, key);
        }
    }

    public static int idOf(String key) {
        int i = 0;
        for (String k : ITEMS.keySet()) {
            if (k.equals(key)) {
                return 10000 + i;
            }
            i++;
        }
        return -1;
    }

    public static String keyOfId(int id) {
        int index = id - 10000;
        if (index < 0 || index >= ITEMS.size()) {
            return null;
        }
        int i = 0;
        for (String k : ITEMS.keySet()) {
            if (i++ == index) {
                return k;
            }
        }
        return null;
    }

    public static ArrayList<Entry> entries() {
        ensureLoaded();
        ArrayList<Entry> result = new ArrayList<>(ITEMS.size());
        for (String key : order) {
            Entry entry = ITEMS.get(key);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }

    public static boolean isHidden(String key) {
        ensureLoaded();
        return hidden.contains(key);
    }

    public static void setHidden(String key, boolean hide) {
        ensureLoaded();
        if (!ITEMS.containsKey(key)) {
            return;
        }
        boolean changed = hide ? hidden.add(key) : hidden.remove(key);
        if (changed) {
            save();
        }
    }

    /** Moves the keys of one section among their old positions, keeping the rest of the order. */
    public static void applySectionOrder(ArrayList<String> sectionKeys) {
        ensureLoaded();
        ArrayList<String> reordered = new ArrayList<>(sectionKeys.size());
        for (String key : sectionKeys) {
            if (ITEMS.containsKey(key) && !reordered.contains(key)) {
                reordered.add(key);
            }
        }
        if (reordered.isEmpty()) {
            return;
        }
        int next = 0;
        ArrayList<String> result = new ArrayList<>(order.size());
        for (String key : order) {
            result.add(reordered.contains(key) ? reordered.get(next++) : key);
        }
        if (!result.equals(order)) {
            order.clear();
            order.addAll(result);
            save();
        }
    }

    public static void reset() {
        ensureLoaded();
        hidden.clear();
        order.clear();
        order.addAll(DEFAULT_ORDER);
        save();
    }

    public static boolean isDefault() {
        ensureLoaded();
        return hidden.isEmpty() && order.equals(DEFAULT_ORDER);
    }

    /** Reorders and hides menu entries in place. No-op while nothing was customized. */
    public static void apply(ArrayList<CharSequence> items, ArrayList<Integer> options, ArrayList<Integer> icons) {
        ensureLoaded();
        if (isDefault()) {
            return;
        }
        int size = options.size();
        if (size == 0 || items.size() != size || icons.size() != size) {
            return;
        }
        ArrayList<Integer> slots = new ArrayList<>();
        ArrayList<Pending> pending = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String key = BY_OPTION.get(options.get(i));
            if (key == null) {
                continue;
            }
            slots.add(i);
            if (!hidden.contains(key)) {
                pending.add(new Pending(options.get(i), icons.get(i), order.indexOf(key), items.get(i)));
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        pending.sort((a, b) -> Integer.compare(a.order, b.order));
        for (int k = 0; k < pending.size(); k++) {
            int pos = slots.get(k);
            Pending p = pending.get(k);
            options.set(pos, p.option);
            icons.set(pos, p.icon);
            items.set(pos, p.text);
        }
        for (int k = slots.size() - 1; k >= pending.size(); k--) {
            int pos = slots.get(k);
            options.remove(pos);
            icons.remove(pos);
            items.remove(pos);
        }
    }

    private static final class Pending {
        final int option;
        final int icon;
        final int order;
        final CharSequence text;

        Pending(int option, int icon, int order, CharSequence text) {
            this.option = option;
            this.icon = icon;
            this.order = order;
            this.text = text;
        }
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            SharedPreferences prefs = prefs();
            order.clear();
            for (String key : split(prefs.getString("order", ""))) {
                if (ITEMS.containsKey(key) && !order.contains(key)) {
                    order.add(key);
                }
            }
            for (String key : DEFAULT_ORDER) {
                if (!order.contains(key)) {
                    order.add(key);
                }
            }
            for (String key : split(prefs.getString("hidden", ""))) {
                if (ITEMS.containsKey(key)) {
                    hidden.add(key);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    private static void save() {
        try {
            prefs().edit()
                    .putString("order", TextUtils.join(",", order))
                    .putString("hidden", TextUtils.join(",", hidden))
                    .apply();
        } catch (Throwable ignore) {
        }
    }

    private static ArrayList<String> split(String value) {
        ArrayList<String> result = new ArrayList<>();
        if (!TextUtils.isEmpty(value)) {
            for (String part : value.split(",")) {
                String key = part.trim();
                if (!key.isEmpty()) {
                    result.add(key);
                }
            }
        }
        return result;
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("hundredgram_msgmenu", Activity.MODE_PRIVATE);
    }
}
