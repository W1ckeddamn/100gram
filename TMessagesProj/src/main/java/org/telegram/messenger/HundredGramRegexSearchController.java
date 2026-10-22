package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HundredGramRegexSearchController {

    private static final String PREF_NAME = "hundredgram_regex_search";
    private static final String KEY_CUSTOM_TEMPLATES = "custom_templates";

    public static class Template {
        public String id;
        public String title;
        public String pattern;
        public String description;
        public boolean isCustom;

        public Template(String id, String title, String pattern, String description, boolean isCustom) {
            this.id = id;
            this.title = title;
            this.pattern = pattern;
            this.description = description;
            this.isCustom = isCustom;
        }
    }

    public static ArrayList<Template> getBuiltInTemplates() {
        ArrayList<Template> list = new ArrayList<>();
        list.add(new Template(
                "phone",
                LocaleController.getString("RegexPresetPhone", R.string.RegexPresetPhone),
                "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{2,4}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{2,4}",
                LocaleController.getString("RegexPresetPhoneDesc", R.string.RegexPresetPhoneDesc),
                false
        ));
        list.add(new Template(
                "cards",
                LocaleController.getString("RegexPresetCards", R.string.RegexPresetCards),
                "\\b(?:\\d{4}[ -]?){3}\\d{4}\\b",
                LocaleController.getString("RegexPresetCardsDesc", R.string.RegexPresetCardsDesc),
                false
        ));
        list.add(new Template(
                "btc",
                LocaleController.getString("RegexPresetBtc", R.string.RegexPresetBtc),
                "\\b(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\\b",
                LocaleController.getString("RegexPresetBtcDesc", R.string.RegexPresetBtcDesc),
                false
        ));
        list.add(new Template(
                "eth",
                LocaleController.getString("RegexPresetEth", R.string.RegexPresetEth),
                "\\b0x[a-fA-F0-9]{40}\\b",
                LocaleController.getString("RegexPresetEthDesc", R.string.RegexPresetEthDesc),
                false
        ));
        list.add(new Template(
                "ton",
                LocaleController.getString("RegexPresetTon", R.string.RegexPresetTon),
                "\\b(?:EQ|UQ)[a-zA-Z0-9_-]{46}\\b",
                LocaleController.getString("RegexPresetTonDesc", R.string.RegexPresetTonDesc),
                false
        ));
        list.add(new Template(
                "usdt",
                LocaleController.getString("RegexPresetUsdt", R.string.RegexPresetUsdt),
                "\\bT[A-Za-z1-9]{33}\\b",
                LocaleController.getString("RegexPresetUsdtDesc", R.string.RegexPresetUsdtDesc),
                false
        ));
        list.add(new Template(
                "urls",
                LocaleController.getString("RegexPresetUrls", R.string.RegexPresetUrls),
                "https?://[^\\s/$.?#].[^\\s]*",
                LocaleController.getString("RegexPresetUrlsDesc", R.string.RegexPresetUrlsDesc),
                false
        ));
        list.add(new Template(
                "email",
                LocaleController.getString("RegexPresetEmail", R.string.RegexPresetEmail),
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                LocaleController.getString("RegexPresetEmailDesc", R.string.RegexPresetEmailDesc),
                false
        ));
        list.add(new Template(
                "ipv4",
                LocaleController.getString("RegexPresetIpv4", R.string.RegexPresetIpv4),
                "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b",
                LocaleController.getString("RegexPresetIpv4Desc", R.string.RegexPresetIpv4Desc),
                false
        ));
        list.add(new Template(
                "dates",
                LocaleController.getString("RegexPresetDates", R.string.RegexPresetDates),
                "\\b\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}\\b",
                LocaleController.getString("RegexPresetDatesDesc", R.string.RegexPresetDatesDesc),
                false
        ));
        return list;
    }

    public static ArrayList<Template> getCustomTemplates() {
        ArrayList<Template> list = new ArrayList<>();
        try {
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_CUSTOM_TEMPLATES, null);
            if (!TextUtils.isEmpty(json)) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.optString("id", UUID.randomUUID().toString());
                    String title = obj.optString("title", "");
                    String pattern = obj.optString("pattern", "");
                    String desc = obj.optString("desc", "");
                    if (!TextUtils.isEmpty(pattern)) {
                        list.add(new Template(id, title, pattern, desc, true));
                    }
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
        return list;
    }

    public static void saveCustomTemplate(String title, String pattern) {
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(pattern)) {
            return;
        }
        try {
            ArrayList<Template> current = getCustomTemplates();
            String id = UUID.randomUUID().toString();
            current.add(new Template(id, title, pattern, pattern, true));

            JSONArray arr = new JSONArray();
            for (Template t : current) {
                JSONObject obj = new JSONObject();
                obj.put("id", t.id);
                obj.put("title", t.title);
                obj.put("pattern", t.pattern);
                obj.put("desc", t.description);
                arr.put(obj);
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CUSTOM_TEMPLATES, arr.toString()).apply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static void deleteCustomTemplate(String id) {
        if (TextUtils.isEmpty(id)) {
            return;
        }
        try {
            ArrayList<Template> current = getCustomTemplates();
            JSONArray arr = new JSONArray();
            for (Template t : current) {
                if (!id.equals(t.id)) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", t.id);
                    obj.put("title", t.title);
                    obj.put("pattern", t.pattern);
                    obj.put("desc", t.description);
                    arr.put(obj);
                }
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CUSTOM_TEMPLATES, arr.toString()).apply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static String validateRegex(String pattern) {
        if (TextUtils.isEmpty(pattern)) {
            return null;
        }
        try {
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
            return null;
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        } catch (Throwable t) {
            return t.getMessage();
        }
    }

    public static Pattern compileSafe(String pattern) {
        if (TextUtils.isEmpty(pattern)) {
            return null;
        }
        try {
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
        } catch (Throwable ignore) {
            return null;
        }
    }
}
