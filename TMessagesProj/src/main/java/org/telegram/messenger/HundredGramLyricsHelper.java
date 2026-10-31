package org.telegram.messenger;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.audioinfo.AudioInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Помощник извлечения и парсинга текстов песен (Lyrics).
 * Источники:
 * 1. Локальные ID3-теги через AudioInfo.getLyrics() (если трек скачан)
 * 2. LRCLIB Public API (синхронизированные LRC-таймкоды и обычный текст)
 */
public class HundredGramLyricsHelper {

    private static final Pattern LRC_LINE_PATTERN = Pattern.compile("^\\[(\\d{1,2}):(\\d{2})(?:[\\.:](\\d{1,3}))?\\](.*)$");
    private static final Map<String, LyricsResult> memoryCache = Collections.synchronizedMap(new HashMap<>());

    public static class LyricsLine {
        public final long timeMs;
        public final String text;

        public LyricsLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text != null ? text.trim() : "";
        }
    }

    public static class LyricsResult {
        public final boolean isSynced;
        public final List<LyricsLine> lines;
        public final String plainText;
        public final String title;
        public final String artist;
        public final String source;

        public LyricsResult(boolean isSynced, List<LyricsLine> lines, String plainText, String title, String artist, String source) {
            this.isSynced = isSynced;
            this.lines = lines != null ? lines : Collections.emptyList();
            this.plainText = plainText != null ? plainText : "";
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.source = source != null ? source : "";
        }

        public boolean hasLyrics() {
            return (isSynced && !lines.isEmpty()) || !TextUtils.isEmpty(plainText.trim());
        }

        public int findCurrentLineIndex(long currentPositionMs) {
            if (!isSynced || lines.isEmpty()) {
                return -1;
            }
            int activeIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).timeMs <= currentPositionMs) {
                    activeIndex = i;
                } else {
                    break;
                }
            }
            return activeIndex;
        }

        public String getFullReadableText() {
            if (!TextUtils.isEmpty(plainText.trim())) {
                return plainText.trim();
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(lines.get(i).text);
            }
            return sb.toString();
        }
    }

    public interface Callback {
        void onLyricsLoaded(LyricsResult result);
    }

    public static void fetchLyrics(int currentAccount, MessageObject messageObject, Callback callback) {
        if (messageObject == null || callback == null) {
            if (callback != null) {
                callback.onLyricsLoaded(null);
            }
            return;
        }

        final String title = messageObject.getMusicTitle();
        final String artist = messageObject.getMusicAuthor();
        final int duration = (int) messageObject.getDuration();
        final String cacheKey = (artist != null ? artist.trim().toLowerCase() : "") + " - " + (title != null ? title.trim().toLowerCase() : "");

        LyricsResult cached = memoryCache.get(cacheKey);
        if (cached != null) {
            callback.onLyricsLoaded(cached);
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            LyricsResult result = null;

            // 1. Попытка извлечь из локального файла через ID3-теги
            try {
                File audioFile = null;
                if (!TextUtils.isEmpty(messageObject.messageOwner.attachPath)) {
                    File f = new File(messageObject.messageOwner.attachPath);
                    if (f.exists()) {
                        audioFile = f;
                    }
                }
                if (audioFile == null) {
                    File f = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner);
                    if (f != null && f.exists()) {
                        audioFile = f;
                    }
                }

                if (audioFile != null && audioFile.exists()) {
                    AudioInfo audioInfo = AudioInfo.getAudioInfo(audioFile);
                    if (audioInfo != null && !TextUtils.isEmpty(audioInfo.getLyrics())) {
                        String id3Lyrics = audioInfo.getLyrics().trim();
                        List<LyricsLine> parsedLines = parseSyncedLines(id3Lyrics);
                        if (!parsedLines.isEmpty()) {
                            result = new LyricsResult(true, parsedLines, null, title, artist, "ID3 (Synced)");
                        } else {
                            result = new LyricsResult(false, null, id3Lyrics, title, artist, "ID3");
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }

            // 2. Если в ID3 нет, запрашиваем LRCLIB API
            if (result == null && !TextUtils.isEmpty(title)) {
                result = fetchFromLrclib(title, artist, duration);
            }

            final LyricsResult finalResult = result;
            if (finalResult != null) {
                memoryCache.put(cacheKey, finalResult);
            }

            AndroidUtilities.runOnUIThread(() -> callback.onLyricsLoaded(finalResult));
        });
    }

    private static LyricsResult fetchFromLrclib(String title, String artist, int duration) {
        // Попытка 1: точный /api/get
        try {
            StringBuilder urlBuilder = new StringBuilder("https://lrclib.net/api/get?");
            urlBuilder.append("track_name=").append(URLEncoder.encode(title, "UTF-8"));
            if (!TextUtils.isEmpty(artist)) {
                urlBuilder.append("&artist_name=").append(URLEncoder.encode(artist, "UTF-8"));
            }
            if (duration > 0) {
                urlBuilder.append("&duration=").append(duration);
            }

            JSONObject json = requestJson(urlBuilder.toString());
            if (json != null) {
                LyricsResult res = parseLrclibJson(json, title, artist);
                if (res != null && res.hasLyrics()) {
                    return res;
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        // Попытка 2: поиск /api/search?q=...
        try {
            String query = (!TextUtils.isEmpty(artist) ? (artist + " ") : "") + title;
            String searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8");
            String raw = requestString(searchUrl);
            if (!TextUtils.isEmpty(raw)) {
                JSONArray arr = new JSONArray(raw);
                if (arr.length() > 0) {
                    JSONObject first = arr.getJSONObject(0);
                    LyricsResult res = parseLrclibJson(first, title, artist);
                    if (res != null && res.hasLyrics()) {
                        return res;
                    }
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        return null;
    }

    private static LyricsResult parseLrclibJson(JSONObject json, String fallbackTitle, String fallbackArtist) {
        if (json == null) return null;
        String syncedLyrics = json.optString("syncedLyrics", null);
        String plainLyrics = json.optString("plainLyrics", null);
        String trackName = json.optString("trackName", fallbackTitle);
        String artistName = json.optString("artistName", fallbackArtist);

        if (!TextUtils.isEmpty(syncedLyrics)) {
            List<LyricsLine> lines = parseSyncedLines(syncedLyrics);
            if (!lines.isEmpty()) {
                return new LyricsResult(true, lines, plainLyrics, trackName, artistName, "LRCLIB");
            }
        }

        if (!TextUtils.isEmpty(plainLyrics)) {
            return new LyricsResult(false, null, plainLyrics, trackName, artistName, "LRCLIB");
        }

        return null;
    }

    private static List<LyricsLine> parseSyncedLines(String lrcText) {
        List<LyricsLine> list = new ArrayList<>();
        if (TextUtils.isEmpty(lrcText)) return list;

        String[] lines = lrcText.split("\r?\n");
        for (String line : lines) {
            if (TextUtils.isEmpty(line)) continue;
            Matcher m = LRC_LINE_PATTERN.matcher(line.trim());
            if (m.find()) {
                try {
                    int min = Integer.parseInt(m.group(1));
                    int sec = Integer.parseInt(m.group(2));
                    int ms = 0;
                    String msGroup = m.group(3);
                    if (msGroup != null) {
                        if (msGroup.length() == 1) {
                            ms = Integer.parseInt(msGroup) * 100;
                        } else if (msGroup.length() == 2) {
                            ms = Integer.parseInt(msGroup) * 10;
                        } else {
                            ms = Integer.parseInt(msGroup);
                        }
                    }
                    long totalMs = (min * 60L + sec) * 1000L + ms;
                    String text = m.group(4);
                    list.add(new LyricsLine(totalMs, text));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private static JSONObject requestJson(String urlStr) {
        String resp = requestString(urlStr);
        if (resp != null) {
            try {
                return new JSONObject(resp);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return null;
    }

    private static String requestString(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "100gram-Android-Client (https://github.com/DrKLO/Telegram)");
            conn.connect();

            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                return sb.toString();
            }
        } catch (Throwable t) {
            FileLog.e(t, false);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
