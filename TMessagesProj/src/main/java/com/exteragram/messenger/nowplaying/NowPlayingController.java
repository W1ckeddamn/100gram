package com.exteragram.messenger.nowplaying;

import android.text.TextUtils;

import com.exteragram.messenger.api.dto.NowPlayingDTO;
import com.exteragram.messenger.nowplaying.ui.components.NowPlayingCardData;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NowPlayingController {

    public static final NowPlayingController INSTANCE = new NowPlayingController();

    private NowPlayingController() {
    }

    public static boolean shouldShowCard(NowPlayingCardData nowPlayingCardData) {
        if (nowPlayingCardData == null || !HundredGramConfig.enableNowPlaying) {
            return false;
        }
        return true;
    }

    public static Runnable getCurrentPlayingTrack(long userId, TLRPC.Document savedMusic, boolean checkApi, BiConsumer<NowPlayingDTO, Long> callback) {
        if (!HundredGramConfig.enableNowPlaying) {
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.accept(null, 0L));
            }
            return null;
        }

        Runnable task = new Runnable() {
            private volatile boolean cancelled;

            @Override
            public void run() {
                if (cancelled) return;

                NowPlayingDTO resultDto = null;

                if (savedMusic != null) {
                    String title = null;
                    String performer = null;
                    long duration = 0;

                    if (savedMusic.attributes != null) {
                        for (int i = 0; i < savedMusic.attributes.size(); i++) {
                            TLRPC.DocumentAttribute attr = savedMusic.attributes.get(i);
                            if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                                TLRPC.TL_documentAttributeAudio audio = (TLRPC.TL_documentAttributeAudio) attr;
                                title = audio.title;
                                performer = audio.performer;
                                duration = (long) audio.duration;
                            } else if (attr instanceof TLRPC.TL_documentAttributeFilename && TextUtils.isEmpty(title)) {
                                title = ((TLRPC.TL_documentAttributeFilename) attr).file_name;
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(performer)) {
                        resultDto = fetchItunesTrack(performer != null ? performer : "", title != null ? title : "", duration);
                        if (resultDto == null) {
                            resultDto = new NowPlayingDTO(
                                    title != null ? title : "Audio",
                                    Collections.singletonList(performer != null ? performer : ""),
                                    null,
                                    null,
                                    null,
                                    null,
                                    true,
                                    null,
                                    "TELEGRAM",
                                    duration
                            );
                        }
                    }
                } else if (UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId() == userId) {
                    MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
                    if (playing != null && playing.isMusic()) {
                        String title = playing.getMusicTitle();
                        String performer = playing.getMusicAuthor();
                        long duration = (long) playing.getDuration();

                        resultDto = fetchItunesTrack(performer, title, duration);
                        if (resultDto == null) {
                            resultDto = new NowPlayingDTO(
                                    title != null ? title : "Music",
                                    Collections.singletonList(performer != null ? performer : ""),
                                    null,
                                    null,
                                    null,
                                    null,
                                    true,
                                    null,
                                    "TELEGRAM",
                                    duration
                            );
                        }
                    }
                }

                if (cancelled) return;

                final NowPlayingDTO finalDto = resultDto;
                AndroidUtilities.runOnUIThread(() -> {
                    if (!cancelled && callback != null) {
                        callback.accept(finalDto, 0L);
                    }
                });
            }
        };

        Utilities.globalQueue.postRunnable(task);
        return task;
    }

    private static NowPlayingDTO fetchItunesTrack(String performer, String title, long fallbackDuration) {
        HttpURLConnection connection = null;
        try {
            String query = (performer + " " + title).trim();
            if (TextUtils.isEmpty(query)) {
                return null;
            }

            String urlStr = "https://itunes.apple.com/search?term=" + URLEncoder.encode(query, "UTF-8") + "&entity=song&limit=1";
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "100gram/Telegram-Android");

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                int resultCount = root.optInt("resultCount", 0);
                if (resultCount > 0) {
                    JSONArray results = root.getJSONArray("results");
                    JSONObject track = results.getJSONObject(0);

                    String trackName = track.optString("trackName", title);
                    String artistName = track.optString("artistName", performer);
                    String albumName = track.optString("collectionName", null);
                    String coverUrl = track.optString("artworkUrl100", null);
                    if (coverUrl != null) {
                        coverUrl = coverUrl.replace("100x100bb.jpg", "600x600bb.jpg")
                                           .replace("100x100", "600x600");
                    }
                    String previewUrl = track.optString("previewUrl", null);
                    String trackViewUrl = track.optString("trackViewUrl", null);
                    long trackMillis = track.optLong("trackTimeMillis", fallbackDuration * 1000);

                    return new NowPlayingDTO(
                            trackName,
                            Collections.singletonList(artistName),
                            albumName,
                            coverUrl,
                            previewUrl,
                            trackViewUrl,
                            true,
                            null,
                            "TELEGRAM",
                            trackMillis / 1000
                    );
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }
}
