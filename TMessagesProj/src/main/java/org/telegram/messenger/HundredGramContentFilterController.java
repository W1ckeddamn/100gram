package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HundredGramContentFilterController {

    private static final String PREF_NAME = "hundredgram_content_filter";
    private static final String KEY_ENABLED = "filter_enabled";
    private static final String KEY_CHANNELS = "filter_channels";
    private static final String KEY_GROUPS = "filter_groups";
    private static final String KEY_DMS = "filter_dms";
    private static final String KEY_PRESET_CRYPTO = "preset_crypto";
    private static final String KEY_PRESET_LINKS = "preset_links";
    private static final String KEY_KEYWORDS = "filter_keywords";
    private static final String KEY_REGEX = "filter_regex";
    private static final String KEY_FILTERED_COUNT = "filtered_count";

    private static final String PRESET_CRYPTO_REGEX = "(?i)(\\b(airdrop|giveaway|whitelist|presale|cryptocurrency|binance|bybit|1win|casino|freespin|казино|крипт(а|ы|у|ой|овалют)|раздач[а-я]|вулкан|ставк[а-я]|сигнал[а-я]|заработок|пассивный\\s+доход|инсайд(ер)?|слив\\s+схем|договорн(ой|ые)\\s+матч)\\b)";
    private static final String PRESET_LINKS_REGEX = "(?i)(t\\.me/\\+[a-zA-Z0-9_-]+|t\\.me/joinchat/[a-zA-Z0-9_-]+|bit\\.ly/[a-zA-Z0-9_-]+|tinyurl\\.com/[a-zA-Z0-9_-]+|cutt\\.ly/[a-zA-Z0-9_-]+|is\\.gd/[a-zA-Z0-9_-]+)";

    private static final Object LOCK = new Object();

    private static volatile boolean filterEnabled;
    private static volatile boolean filterChannels = true;
    private static volatile boolean filterGroups = true;
    private static volatile boolean filterDMs = false;
    private static volatile boolean presetCrypto = false;
    private static volatile boolean presetLinks = false;

    private static volatile List<String> keywordsList = Collections.emptyList();
    private static volatile List<String> keywordsLowerList = Collections.emptyList();
    private static volatile List<String> regexList = Collections.emptyList();
    private static volatile List<Pattern> compiledPatterns = Collections.emptyList();

    private static Pattern presetCryptoCompiled;
    private static Pattern presetLinksCompiled;

    private static final AtomicInteger filteredCount = new AtomicInteger(0);

    static {
        loadSettings();
    }

    public static void loadSettings() {
        synchronized (LOCK) {
            Context context = ApplicationLoader.applicationContext;
            if (context == null) {
                return;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
            filterEnabled = prefs.getBoolean(KEY_ENABLED, false);
            filterChannels = prefs.getBoolean(KEY_CHANNELS, true);
            filterGroups = prefs.getBoolean(KEY_GROUPS, true);
            filterDMs = prefs.getBoolean(KEY_DMS, false);
            presetCrypto = prefs.getBoolean(KEY_PRESET_CRYPTO, false);
            presetLinks = prefs.getBoolean(KEY_PRESET_LINKS, false);
            filteredCount.set(prefs.getInt(KEY_FILTERED_COUNT, 0));

            Set<String> kwSet = prefs.getStringSet(KEY_KEYWORDS, null);
            ArrayList<String> kws = new ArrayList<>();
            ArrayList<String> kwsLower = new ArrayList<>();
            if (kwSet != null) {
                for (String kw : kwSet) {
                    if (!TextUtils.isEmpty(kw)) {
                        kws.add(kw);
                        kwsLower.add(kw.toLowerCase());
                    }
                }
            }
            keywordsList = Collections.unmodifiableList(kws);
            keywordsLowerList = Collections.unmodifiableList(kwsLower);

            Set<String> rgxSet = prefs.getStringSet(KEY_REGEX, null);
            ArrayList<String> rgxs = new ArrayList<>();
            ArrayList<Pattern> patterns = new ArrayList<>();
            if (rgxSet != null) {
                for (String rgx : rgxSet) {
                    if (!TextUtils.isEmpty(rgx)) {
                        try {
                            Pattern p = Pattern.compile(rgx, Pattern.CASE_INSENSITIVE);
                            patterns.add(p);
                            rgxs.add(rgx);
                        } catch (PatternSyntaxException e) {
                            FileLog.e(e);
                        }
                    }
                }
            }
            regexList = Collections.unmodifiableList(rgxs);
            compiledPatterns = Collections.unmodifiableList(patterns);

            try {
                presetCryptoCompiled = Pattern.compile(PRESET_CRYPTO_REGEX);
            } catch (Exception ignored) {
            }
            try {
                presetLinksCompiled = Pattern.compile(PRESET_LINKS_REGEX);
            } catch (Exception ignored) {
            }
        }
    }

    private static SharedPreferences.Editor getEditor() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit();
    }

    public static boolean isFilterEnabled() {
        return filterEnabled;
    }

    public static void setFilterEnabled(boolean enabled) {
        filterEnabled = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_ENABLED, enabled).apply();
        }
        notifyInterfaces();
    }

    public static boolean isFilterChannels() {
        return filterChannels;
    }

    public static void setFilterChannels(boolean enabled) {
        filterChannels = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_CHANNELS, enabled).apply();
        }
        notifyInterfaces();
    }

    public static boolean isFilterGroups() {
        return filterGroups;
    }

    public static void setFilterGroups(boolean enabled) {
        filterGroups = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_GROUPS, enabled).apply();
        }
        notifyInterfaces();
    }

    public static boolean isFilterDMs() {
        return filterDMs;
    }

    public static void setFilterDMs(boolean enabled) {
        filterDMs = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_DMS, enabled).apply();
        }
        notifyInterfaces();
    }

    public static boolean isPresetCrypto() {
        return presetCrypto;
    }

    public static void setPresetCrypto(boolean enabled) {
        presetCrypto = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_PRESET_CRYPTO, enabled).apply();
        }
        notifyInterfaces();
    }

    public static boolean isPresetLinks() {
        return presetLinks;
    }

    public static void setPresetLinks(boolean enabled) {
        presetLinks = enabled;
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putBoolean(KEY_PRESET_LINKS, enabled).apply();
        }
        notifyInterfaces();
    }

    public static List<String> getKeywords() {
        return keywordsList;
    }

    public static void addKeyword(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            HashSet<String> set = new HashSet<>(keywordsList);
            if (set.add(trimmed)) {
                ArrayList<String> kws = new ArrayList<>(set);
                ArrayList<String> kwsLower = new ArrayList<>();
                for (String s : kws) {
                    kwsLower.add(s.toLowerCase());
                }
                keywordsList = Collections.unmodifiableList(kws);
                keywordsLowerList = Collections.unmodifiableList(kwsLower);

                SharedPreferences.Editor editor = getEditor();
                if (editor != null) {
                    editor.putStringSet(KEY_KEYWORDS, set).apply();
                }
            }
        }
        notifyInterfaces();
    }

    public static void removeKeyword(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        synchronized (LOCK) {
            HashSet<String> set = new HashSet<>(keywordsList);
            if (set.remove(keyword)) {
                ArrayList<String> kws = new ArrayList<>(set);
                ArrayList<String> kwsLower = new ArrayList<>();
                for (String s : kws) {
                    kwsLower.add(s.toLowerCase());
                }
                keywordsList = Collections.unmodifiableList(kws);
                keywordsLowerList = Collections.unmodifiableList(kwsLower);

                SharedPreferences.Editor editor = getEditor();
                if (editor != null) {
                    editor.putStringSet(KEY_KEYWORDS, set).apply();
                }
            }
        }
        notifyInterfaces();
    }

    public static List<String> getRegexPatterns() {
        return regexList;
    }

    public static boolean addRegexPattern(String regex) throws PatternSyntaxException {
        if (TextUtils.isEmpty(regex)) {
            return false;
        }
        String trimmed = regex.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        // Test compile - throws PatternSyntaxException if invalid
        Pattern testPattern = Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE);

        synchronized (LOCK) {
            HashSet<String> set = new HashSet<>(regexList);
            if (set.add(trimmed)) {
                ArrayList<String> rgxs = new ArrayList<>(set);
                ArrayList<Pattern> patterns = new ArrayList<>();
                for (String s : rgxs) {
                    try {
                        patterns.add(Pattern.compile(s, Pattern.CASE_INSENSITIVE));
                    } catch (Exception ignored) {
                    }
                }
                regexList = Collections.unmodifiableList(rgxs);
                compiledPatterns = Collections.unmodifiableList(patterns);

                SharedPreferences.Editor editor = getEditor();
                if (editor != null) {
                    editor.putStringSet(KEY_REGEX, set).apply();
                }
            }
        }
        notifyInterfaces();
        return true;
    }

    public static void removeRegexPattern(String regex) {
        if (TextUtils.isEmpty(regex)) {
            return;
        }
        synchronized (LOCK) {
            HashSet<String> set = new HashSet<>(regexList);
            if (set.remove(regex)) {
                ArrayList<String> rgxs = new ArrayList<>(set);
                ArrayList<Pattern> patterns = new ArrayList<>();
                for (String s : rgxs) {
                    try {
                        patterns.add(Pattern.compile(s, Pattern.CASE_INSENSITIVE));
                    } catch (Exception ignored) {
                    }
                }
                regexList = Collections.unmodifiableList(rgxs);
                compiledPatterns = Collections.unmodifiableList(patterns);

                SharedPreferences.Editor editor = getEditor();
                if (editor != null) {
                    editor.putStringSet(KEY_REGEX, set).apply();
                }
            }
        }
        notifyInterfaces();
    }

    public static int getRulesCount() {
        int count = keywordsList.size() + regexList.size();
        if (presetCrypto) count++;
        if (presetLinks) count++;
        return count;
    }

    public static int getFilteredCount() {
        return filteredCount.get();
    }

    public static void resetFilteredCount() {
        filteredCount.set(0);
        SharedPreferences.Editor editor = getEditor();
        if (editor != null) {
            editor.putInt(KEY_FILTERED_COUNT, 0).apply();
        }
    }

    private static void incrementFilteredCount() {
        int count = filteredCount.incrementAndGet();
        if (count % 5 == 0) {
            SharedPreferences.Editor editor = getEditor();
            if (editor != null) {
                editor.putInt(KEY_FILTERED_COUNT, count).apply();
            }
        }
    }

    private static void notifyInterfaces() {
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
        });
    }

    /**
     * Checks if the message should be filtered (suppressed) based on active rules.
     */
    public static boolean isMessageFiltered(MessageObject messageObject) {
        if (!filterEnabled || messageObject == null) {
            return false;
        }
        if (messageObject.isOut() || messageObject.isOutOwner()) {
            return false;
        }

        long dialogId = messageObject.getDialogId();
        if (DialogObject.isUserDialog(dialogId)) {
            if (!filterDMs) {
                return false;
            }
        } else if (DialogObject.isChatDialog(dialogId)) {
            TLRPC.Chat chat = MessagesController.getInstance(messageObject.currentAccount).getChat(-dialogId);
            if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
                if (!filterChannels) {
                    return false;
                }
            } else {
                if (!filterGroups) {
                    return false;
                }
            }
        }

        // Extract message text content
        CharSequence msgTextSeq = messageObject.messageText;
        String text = msgTextSeq != null ? msgTextSeq.toString() : null;
        if (TextUtils.isEmpty(text) && messageObject.messageOwner != null && messageObject.messageOwner.message != null) {
            text = messageObject.messageOwner.message;
        }

        boolean hasText = !TextUtils.isEmpty(text);
        String textLower = hasText ? text.toLowerCase() : "";

        // 1. Check custom keywords
        List<String> kwLowers = keywordsLowerList;
        if (hasText && !kwLowers.isEmpty()) {
            for (int i = 0; i < kwLowers.size(); i++) {
                String kw = kwLowers.get(i);
                if (textLower.contains(kw)) {
                    incrementFilteredCount();
                    return true;
                }
            }
        }

        // 2. Check custom regex patterns
        List<Pattern> patterns = compiledPatterns;
        if (hasText && !patterns.isEmpty()) {
            for (int i = 0; i < patterns.size(); i++) {
                Pattern p = patterns.get(i);
                if (p.matcher(text).find()) {
                    incrementFilteredCount();
                    return true;
                }
            }
        }

        // 3. Check Crypto Scam preset
        if (presetCrypto && hasText && presetCryptoCompiled != null) {
            if (presetCryptoCompiled.matcher(text).find()) {
                incrementFilteredCount();
                return true;
            }
        }

        // 4. Check Spam Links preset
        if (presetLinks) {
            if (hasText && presetLinksCompiled != null && presetLinksCompiled.matcher(text).find()) {
                incrementFilteredCount();
                return true;
            }
            if (messageObject.messageOwner != null && messageObject.messageOwner.entities != null) {
                for (int i = 0; i < messageObject.messageOwner.entities.size(); i++) {
                    TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(i);
                    if (entity instanceof TLRPC.TL_messageEntityUrl) {
                        if (hasText && entity.offset >= 0 && entity.offset + entity.length <= text.length()) {
                            String url = text.substring(entity.offset, entity.offset + entity.length);
                            if (presetLinksCompiled != null && presetLinksCompiled.matcher(url).find()) {
                                incrementFilteredCount();
                                return true;
                            }
                        }
                    } else if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        String url = entity.url;
                        if (url != null && presetLinksCompiled != null && presetLinksCompiled.matcher(url).find()) {
                            incrementFilteredCount();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
