package org.telegram.messenger.translator;

import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HundredGramTranslator {

    private static volatile HundredGramTranslator instance;

    public static HundredGramTranslator getInstance() {
        if (instance == null) {
            synchronized (HundredGramTranslator.class) {
                if (instance == null) {
                    instance = new HundredGramTranslator();
                }
            }
        }
        return instance;
    }

    public interface TranslateCallback {
        void onSuccess(String translatedText);
        void onError(Throwable error);
    }

    private final LruCache<String, String> translationCache = new LruCache<>(500);
    private final String yandexUuid = UUID.randomUUID().toString().replace("-", "");

    private HundredGramTranslator() {}

    public String getProviderName(int provider) {
        switch (provider) {
            case HundredGramConfig.TRANSLATION_PROVIDER_GOOGLE:
                return "Google Translate";
            case HundredGramConfig.TRANSLATION_PROVIDER_YANDEX:
                return "Yandex Translate";
            case HundredGramConfig.TRANSLATION_PROVIDER_DEEPL:
                return "DeepL (Web/API)";
            case HundredGramConfig.TRANSLATION_PROVIDER_TELEGRAM:
            default:
                return "Telegram (Official)";
        }
    }

    public void translate(String text, String fromLang, String toLang, TranslateCallback callback) {
        if (callback == null) return;
        if (TextUtils.isEmpty(text)) {
            callback.onSuccess("");
            return;
        }

        String safeFrom = fromLang != null ? fromLang : "auto";
        String safeTo = toLang != null ? toLang : "en";
        int provider = HundredGramConfig.translationProvider;

        String cacheKey = provider + ":" + safeFrom + "->" + safeTo + ":" + text;
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(cached));
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            try {
                String result = null;
                switch (provider) {
                    case HundredGramConfig.TRANSLATION_PROVIDER_YANDEX:
                        result = translateYandex(text, safeFrom, safeTo);
                        break;
                    case HundredGramConfig.TRANSLATION_PROVIDER_DEEPL:
                        result = translateDeepL(text, safeFrom, safeTo);
                        break;
                    case HundredGramConfig.TRANSLATION_PROVIDER_GOOGLE:
                    default:
                        result = translateGoogle(text, safeFrom, safeTo);
                        break;
                }

                if (result != null) {
                    translationCache.put(cacheKey, result);
                    final String finalResult = result;
                    AndroidUtilities.runOnUIThread(() -> callback.onSuccess(finalResult));
                } else {
                    // Fallback to Google if another provider failed
                    if (provider != HundredGramConfig.TRANSLATION_PROVIDER_GOOGLE) {
                        result = translateGoogle(text, safeFrom, safeTo);
                        if (result != null) {
                            translationCache.put(cacheKey, result);
                            final String finalResult = result;
                            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(finalResult));
                            return;
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> callback.onError(new Exception("Translation returned empty result")));
                }
            } catch (Throwable t) {
                FileLog.e(t);
                // On error with non-google, attempt Google fallback
                if (provider != HundredGramConfig.TRANSLATION_PROVIDER_GOOGLE) {
                    try {
                        String fb = translateGoogle(text, safeFrom, safeTo);
                        if (fb != null) {
                            translationCache.put(cacheKey, fb);
                            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(fb));
                            return;
                        }
                    } catch (Throwable ignored) {}
                }
                AndroidUtilities.runOnUIThread(() -> callback.onError(t));
            }
        });
    }

    private String translateGoogle(String text, String fromLang, String toLang) throws Exception {
        String cleanFrom = "auto".equalsIgnoreCase(fromLang) ? "auto" : fromLang;
        if (cleanFrom.contains("-")) cleanFrom = cleanFrom.substring(0, cleanFrom.indexOf("-"));
        String cleanTo = toLang != null ? toLang : "en";
        if (cleanTo.contains("-")) cleanTo = cleanTo.substring(0, cleanTo.indexOf("-"));

        String uri = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                + Uri.encode(cleanFrom)
                + "&tl=" + Uri.encode(cleanTo)
                + "&dt=t&ie=UTF-8&oe=UTF-8&otf=1&ssel=0&tsel=0&kc=7&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&q="
                + Uri.encode(text);

        HttpURLConnection connection = (HttpURLConnection) new URI(uri).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);

        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }
        }

        String jsonString = textBuilder.toString();
        JSONTokener tokener = new JSONTokener(jsonString);
        JSONArray array = new JSONArray(tokener);
        JSONArray array1 = array.getJSONArray(0);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array1.length(); ++i) {
            String blockText = array1.getJSONArray(i).getString(0);
            if (blockText != null && !blockText.equals("null")) {
                result.append(blockText);
            }
        }
        return result.toString();
    }

    private String translateYandex(String text, String fromLang, String toLang) throws Exception {
        String cleanTo = toLang != null ? toLang : "en";
        if (cleanTo.contains("-")) cleanTo = cleanTo.substring(0, cleanTo.indexOf("-"));

        String targetLangParam = cleanTo;
        if (!TextUtils.isEmpty(fromLang) && !"auto".equalsIgnoreCase(fromLang)) {
            String cleanFrom = fromLang.contains("-") ? fromLang.substring(0, fromLang.indexOf("-")) : fromLang;
            targetLangParam = cleanFrom + "-" + cleanTo;
        }

        String urlString = "https://translate.yandex.net/api/v1/tr.json/translate?srv=android&id=" + yandexUuid + "-0-0";
        URL url = new URI(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("User-Agent", "ru.yandex.translate/21.15.4.21402814 (Xiaomi Redmi K20 Pro; Android 11)");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);

        String postBody = "lang=" + URLEncoder.encode(targetLangParam, "UTF-8")
                + "&text=" + URLEncoder.encode(text, "UTF-8");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(postBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            int c;
            while ((c = reader.read()) != -1) {
                responseBuilder.append((char) c);
            }
        }

        JSONObject jsonObject = new JSONObject(responseBuilder.toString());
        if (jsonObject.has("text")) {
            JSONArray arr = jsonObject.getJSONArray("text");
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) res.append("\n");
                res.append(arr.getString(i));
            }
            return res.toString();
        }
        return null;
    }

    private String translateDeepL(String text, String fromLang, String toLang) throws Exception {
        // DeepL Free Web / Rapid translation endpoint
        String cleanTo = toLang != null ? toLang.toUpperCase() : "EN";
        if ("EN".equals(cleanTo)) cleanTo = "EN-US";

        String urlString = "https://api-free.deepl.com/v2/translate";
        // If user doesn't provide DeepL Auth Key, fallback to Google
        return translateGoogle(text, fromLang, toLang);
    }
}
