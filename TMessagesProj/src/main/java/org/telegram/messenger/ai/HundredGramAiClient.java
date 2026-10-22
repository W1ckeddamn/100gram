package org.telegram.messenger.ai;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class HundredGramAiClient {

    public interface GenerationCallback {
        void onChunk(String accumulatedText, String newDelta);
        void onResponse(String fullText);
        void onError(int code, String message);
    }

    public interface Cancellable {
        void cancel();
    }

    public static Cancellable request(final String userPrompt, final Role role, final boolean includeHistory, final GenerationCallback callback) {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final HttpURLConnection[] activeConnection = new HttpURLConnection[1];

        Utilities.globalQueue.postRunnable(() -> {
            if (cancelled.get()) return;

            Service service = HundredGramAiConfig.getSelectedService();
            if (service == null) {
                notifyError(callback, 400, "No AI service configured");
                return;
            }

            String apiKey = service.getKey();
            if (TextUtils.isEmpty(apiKey)) {
                notifyError(callback, 401, "API Key is missing. Please enter your API Key in 100gram AI settings.");
                return;
            }

            String endpointUrl = resolveEndpoint(service);
            boolean stream = HundredGramAiConfig.getResponseStreaming();
            float temperature = HundredGramAiConfig.getTemperature();

            try {
                JSONObject requestJson = new JSONObject();
                requestJson.put("model", service.getModel());
                requestJson.put("temperature", (double) temperature);
                requestJson.put("stream", stream);

                JSONArray messagesArray = new JSONArray();

                // System prompt / Role
                Role activeRole = role != null ? role : HundredGramAiController.getInstance().getSelectedRole();
                if (activeRole != null && !TextUtils.isEmpty(activeRole.getPrompt())) {
                    JSONObject systemMsg = new JSONObject();
                    systemMsg.put("role", "system");
                    systemMsg.put("content", activeRole.getPrompt());
                    messagesArray.put(systemMsg);
                }

                // History
                if (includeHistory && HundredGramAiConfig.getSaveHistory()) {
                    ArrayList<Message> history = HundredGramAiConfig.getConversationHistory();
                    int start = Math.max(0, history.size() - 10);
                    for (int i = start; i < history.size(); i++) {
                        Message m = history.get(i);
                        JSONObject histObj = new JSONObject();
                        histObj.put("role", m.role());
                        histObj.put("content", m.content());
                        messagesArray.put(histObj);
                    }
                }

                // User prompt
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userPrompt);
                messagesArray.put(userMsg);

                requestJson.put("messages", messagesArray);

                URL url = new URL(endpointUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                activeConnection[0] = conn;
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setRequestProperty("User-Agent", "100gram/1.0 (Android)");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                byte[] postBytes = requestJson.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postBytes);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (cancelled.get()) return;

                if (responseCode >= 200 && responseCode < 300) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder accumulated = new StringBuilder();

                    if (stream) {
                        String line;
                        while (!cancelled.get() && (line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;
                            if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                try {
                                    JSONObject chunkJson = new JSONObject(data);
                                    JSONArray choices = chunkJson.optJSONArray("choices");
                                    if (choices != null && choices.length() > 0) {
                                        JSONObject firstChoice = choices.getJSONObject(0);
                                        JSONObject delta = firstChoice.optJSONObject("delta");
                                        if (delta != null && delta.has("content")) {
                                            String contentPiece = delta.getString("content");
                                            if (!TextUtils.isEmpty(contentPiece)) {
                                                accumulated.append(contentPiece);
                                                final String currentAcc = accumulated.toString();
                                                final String currentDelta = contentPiece;
                                                AndroidUtilities.runOnUIThread(() -> {
                                                    if (!cancelled.get() && callback != null) {
                                                        callback.onChunk(currentAcc, currentDelta);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                } catch (Exception parseEx) {
                                    // Some chunks might be telemetry or empty ping
                                }
                            }
                        }
                    } else {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while (!cancelled.get() && (line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        try {
                            JSONObject respJson = new JSONObject(sb.toString());
                            JSONArray choices = respJson.optJSONArray("choices");
                            if (choices != null && choices.length() > 0) {
                                JSONObject choice = choices.getJSONObject(0);
                                JSONObject msg = choice.optJSONObject("message");
                                if (msg != null && msg.has("content")) {
                                    accumulated.append(msg.getString("content"));
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    reader.close();
                    final String finalText = accumulated.toString();

                    if (!cancelled.get()) {
                        // Save to history
                        HundredGramAiConfig.addMessageToHistory(new Message("user", userPrompt));
                        HundredGramAiConfig.addMessageToHistory(new Message("assistant", finalText));

                        AndroidUtilities.runOnUIThread(() -> {
                            if (!cancelled.get() && callback != null) {
                                callback.onResponse(finalText);
                            }
                        });
                    }
                } else {
                    InputStream es = conn.getErrorStream();
                    String errorBody = "";
                    if (es != null) {
                        BufferedReader errReader = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = errReader.readLine()) != null) {
                            sb.append(line);
                        }
                        errReader.close();
                        errorBody = sb.toString();
                    }
                    String errorMsg = parseErrorMessage(responseCode, errorBody);
                    notifyError(callback, responseCode, errorMsg);
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (!cancelled.get()) {
                    notifyError(callback, 500, e.getMessage() != null ? e.getMessage() : "Network error");
                }
            } finally {
                if (activeConnection[0] != null) {
                    try {
                        activeConnection[0].disconnect();
                    } catch (Exception ignored) {}
                }
            }
        });

        return () -> {
            cancelled.set(true);
            if (activeConnection[0] != null) {
                try {
                    activeConnection[0].disconnect();
                } catch (Exception ignored) {}
            }
        };
    }

    private static String resolveEndpoint(Service service) {
        String url = service.getUrl();
        if (TextUtils.isEmpty(url)) {
            return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        }
        String lower = url.toLowerCase();
        if (lower.contains("generativelanguage.googleapis.com")) {
            return "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.toLowerCase().endsWith("/chat/completions")) {
            url = url + "/chat/completions";
        }
        return url;
    }

    private static String parseErrorMessage(int code, String body) {
        if (!TextUtils.isEmpty(body)) {
            try {
                JSONObject json = new JSONObject(body);
                if (json.has("error")) {
                    Object errObj = json.get("error");
                    if (errObj instanceof JSONObject) {
                        return ((JSONObject) errObj).optString("message", body);
                    } else if (errObj instanceof String) {
                        return (String) errObj;
                    }
                }
                if (json.has("message")) {
                    return json.getString("message");
                }
            } catch (Exception ignored) {}
        }
        if (code == 401) {
            return "Invalid API Key (HTTP 401 Unauthorized)";
        } else if (code == 429) {
            return "Rate limit exceeded (HTTP 429 Too Many Requests)";
        } else if (code == 404) {
            return "Model or endpoint not found (HTTP 404)";
        }
        return "Server returned HTTP " + code;
    }

    private static void notifyError(GenerationCallback callback, int code, String message) {
        AndroidUtilities.runOnUIThread(() -> {
            if (callback != null) {
                callback.onError(code, message);
            }
        });
    }
}
