package com.exteragram.messenger.adblock.backend;

import android.content.SharedPreferences;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;

public class SubscriptionsManager {
    private static SubscriptionsManager instance;
    private static final Pattern redirectPattern = Pattern.compile("!\\s*Redirect:\\s*(\\S+)");
    private final Object lock = new Object();
    private final DispatchQueue queue = new DispatchQueue("SubscriptionsManager");
    private final SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("ublock_subscriptions", 0);

    public static SubscriptionsManager getInstance() {
        if (instance == null) {
            instance = new SubscriptionsManager();
        }
        return instance;
    }

    private File getFileForUrl(String str) {
        File file = new File(ApplicationLoader.applicationContext.getFilesDir(), "adblock");
        if (!file.exists()) {
            file.mkdirs();
        }
        return new File(file, Base64.encodeToString(str.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.URL_SAFE) + ".txt");
    }

    public void update(final String[] strArr, final Runnable runnable) {
        this.queue.postRunnable(new Runnable() {
            @Override
            public final void run() {
                SubscriptionsManager.this.lambda$update$0(strArr, runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void lambda$update$0(String[] strArr, Runnable runnable) {
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
        long currentTimeMillis = System.currentTimeMillis();
        for (FilterMetadata filterMetadata : getSubscriptions()) {
            if (currentTimeMillis >= filterMetadata.expires || !getFileForUrl(filterMetadata.url).exists()) {
                linkedHashSet.add(filterMetadata.url);
            }
        }
        synchronized (this.lock) {
            try {
                for (String str : strArr) {
                    if (!this.prefs.contains("metadata_" + str)) {
                        if (!this.prefs.contains("redirect_" + str)) {
                            linkedHashSet.add(str);
                        }
                    }
                }
            } catch (Throwable th) {
                FileLog.e(th);
            }
        }
        for (String url : linkedHashSet) {
            fetchSubscription(url, 0);
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    public boolean hasFilters() {
        return !getSubscriptionFilePaths().isEmpty();
    }

    private void unsubscribe(String str) {
        synchronized (this.lock) {
            SharedPreferences.Editor edit = this.prefs.edit();
            edit.remove("metadata_" + str);
            edit.apply();
        }
        File fileForUrl = getFileForUrl(str);
        if (fileForUrl.exists()) {
            fileForUrl.delete();
        }
    }

    public List<FilterMetadata> getSubscriptions() {
        ArrayList<FilterMetadata> arrayList = new ArrayList<>();
        synchronized (this.lock) {
            for (String str : this.prefs.getAll().keySet()) {
                if (str.startsWith("metadata_")) {
                    try {
                        String string = this.prefs.getString(str, null);
                        if (string != null) {
                            arrayList.add(FilterMetadata.fromJson(new JSONObject(string)));
                        }
                    } catch (JSONException unused) {
                    }
                }
            }
        }
        return Collections.unmodifiableList(arrayList);
    }

    public List<String> getSubscriptionFilePaths() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (FilterMetadata meta : getSubscriptions()) {
            File fileForUrl = getFileForUrl(meta.url);
            if (fileForUrl.exists()) {
                arrayList.add(fileForUrl.getAbsolutePath());
            }
        }
        return arrayList;
    }

    private boolean fetchSubscription(String str, int i) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(str);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                String string = sb.toString();
                String extractRedirect = extractRedirect(string);
                if (extractRedirect != null) {
                    if (i < 3 && fetchSubscription(extractRedirect, i + 1)) {
                        unsubscribe(str);
                        synchronized (this.lock) {
                            this.prefs.edit().putString("redirect_" + str, extractRedirect).apply();
                        }
                        return true;
                    }
                } else {
                    FilterMetadata parseMetadata = parseMetadata(str, string);
                    File fileForUrl = getFileForUrl(str);
                    File file = new File(fileForUrl.getPath() + ".tmp");
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        fileOutputStream.write(string.getBytes(StandardCharsets.UTF_8));
                        fileOutputStream.flush();
                        if (!file.renameTo(fileForUrl)) {
                            file.delete();
                        } else {
                            synchronized (this.lock) {
                                SharedPreferences.Editor edit = this.prefs.edit();
                                edit.putString("metadata_" + str, parseMetadata.toJson().toString());
                                edit.apply();
                            }
                            return true;
                        }
                    } catch (IOException e) {
                        file.delete();
                        return false;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractRedirect(String str) {
        Matcher matcher = redirectPattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private FilterMetadata parseMetadata(String str, String str2) {
        String extractMetadataValue = extractMetadataValue(str2, "Title");
        String extractMetadataValue2 = extractMetadataValue(str2, "Homepage");
        int countRules = countRules(str2);
        long calculateExpiration = calculateExpiration(str2);
        if (extractMetadataValue == null) {
            extractMetadataValue = "Unnamed list";
        }
        if (extractMetadataValue2 == null) {
            extractMetadataValue2 = "";
        }
        return new FilterMetadata(str, extractMetadataValue, extractMetadataValue2, countRules, calculateExpiration);
    }

    private String extractMetadataValue(String str, String str2) {
        Matcher matcher = Pattern.compile("!\\s*" + str2 + ":\\s*(.+)").matcher(str);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private int countRules(String str) {
        int i = 0;
        for (String str2 : str.split("\n")) {
            String trim = str2.trim();
            if (!trim.isEmpty() && !trim.startsWith("!")) {
                i++;
            }
        }
        return i;
    }

    private long calculateExpiration(String str) {
        String extractMetadataValue = extractMetadataValue(str, "Expires");
        if (extractMetadataValue == null) {
            return System.currentTimeMillis() + 432000000L;
        }
        try {
            int parseInt = Integer.parseInt(extractMetadataValue.replaceAll("[^0-9]", ""));
            if (extractMetadataValue.contains("hour")) {
                return System.currentTimeMillis() + TimeUnit.HOURS.toMillis(parseInt);
            }
            if (extractMetadataValue.contains("day")) {
                return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(parseInt);
            }
        } catch (NumberFormatException unused) {
        }
        return System.currentTimeMillis() + 432000000L;
    }

    public static class FilterMetadata {
        public final long expires;
        public final String homepage;
        public final int rulesCount;
        public final String title;
        public final String url;

        private FilterMetadata(String str, String str2, String str3, int i, long j) {
            this.url = str;
            this.title = str2;
            this.homepage = str3;
            this.rulesCount = i;
            this.expires = j;
        }

        public static FilterMetadata fromJson(JSONObject jSONObject) throws JSONException {
            return new FilterMetadata(jSONObject.getString("url"), jSONObject.getString("title"), jSONObject.getString("homepage"), jSONObject.getInt("rulesCount"), jSONObject.getLong("expires"));
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("url", this.url);
            jSONObject.put("title", this.title);
            jSONObject.put("homepage", this.homepage);
            jSONObject.put("rulesCount", this.rulesCount);
            jSONObject.put("expires", this.expires);
            return jSONObject;
        }
    }
}