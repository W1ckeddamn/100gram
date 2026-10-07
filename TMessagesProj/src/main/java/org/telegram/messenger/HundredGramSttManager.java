package org.telegram.messenger;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HundredGramSttManager {

    // Ссылка на проверенную компактную русскую модель Vosk (~43 MB)
    public static final String VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip";
    public static final String MODEL_DIR_NAME = "vosk-model-ru";

    private static volatile Model sharedModel = null;
    private static final Object modelLock = new Object();
    private static boolean isDownloading = false;

    public interface DownloadCallback {
        void onProgress(int percent);
        void onSuccess();
        void onError(String error);
    }

    public interface TranscribeCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static File getModelDir() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return null;
        return new File(context.getFilesDir(), MODEL_DIR_NAME);
    }

    public static boolean isModelDownloaded() {
        File dir = getModelDir();
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        // В правильной модели Vosk должны быть папки am и conf (или graph)
        File amDir = new File(dir, "am");
        File confDir = new File(dir, "conf");
        if (amDir.exists() && confDir.exists()) {
            return true;
        }
        // Если распаковано во вложенную директорию (vosk-model-small-ru-0.22)
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File sub : subFiles) {
                if (sub.isDirectory() && new File(sub, "am").exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static File getEffectiveModelDir() {
        File dir = getModelDir();
        if (dir == null) return null;
        if (new File(dir, "am").exists()) {
            return dir;
        }
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File sub : subFiles) {
                if (sub.isDirectory() && new File(sub, "am").exists()) {
                    return sub;
                }
            }
        }
        return dir;
    }

    public static long getModelSizeBytes() {
        File dir = getModelDir();
        if (dir == null || !dir.exists()) return 0;
        return getFolderSize(dir);
    }

    private static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) {
                    size += getFolderSize(child);
                }
            }
        } else {
            size += f.length();
        }
        return size;
    }

    public static boolean deleteModel() {
        synchronized (modelLock) {
            if (sharedModel != null) {
                try {
                    sharedModel.close();
                } catch (Throwable ignore) {}
                sharedModel = null;
            }
        }
        File dir = getModelDir();
        if (dir != null && dir.exists()) {
            return deleteRecursive(dir);
        }
        return false;
    }

    private static boolean deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        return f.delete();
    }

    public static void showDownloadDialog(BaseFragment fragment, Runnable onSuccess) {
        Context context = fragment != null && fragment.getParentActivity() != null ? fragment.getParentActivity() : (org.telegram.ui.LaunchActivity.instance != null ? org.telegram.ui.LaunchActivity.instance : (fragment != null ? fragment.getContext() : null));
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("LocalSttDownloadTitle", R.string.LocalSttDownloadTitle));
        builder.setMessage(LocaleController.getString("LocalSttDownloadMessage", R.string.LocalSttDownloadMessage));

        AlertDialog[] dialogHolder = new AlertDialog[1];
        builder.setPositiveButton(LocaleController.getString("Download", R.string.LocalSttDownloadTitle), (dialog, which) -> {
            startDownloadWithProgress(fragment, onSuccess);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    public static void startDownloadWithProgress(BaseFragment fragment, Runnable onSuccess) {
        if (isDownloading) return;
        isDownloading = true;

        Context context = fragment != null && fragment.getParentActivity() != null ? fragment.getParentActivity() : (org.telegram.ui.LaunchActivity.instance != null ? org.telegram.ui.LaunchActivity.instance : (fragment != null ? fragment.getContext() : null));
        AlertDialog progressDialog = null;
        if (context != null) {
            progressDialog = new AlertDialog(context, 3);
            progressDialog.setTitle(LocaleController.getString("LocalSttDownloading", R.string.LocalSttDownloading));
            progressDialog.setMessage("0%");
            progressDialog.setProgress(0);
            progressDialog.setCanCancel(false);
            progressDialog.show();
        }
        final AlertDialog finalDialog = progressDialog;

        Utilities.globalQueue.postRunnable(() -> {
            downloadAndUnzipModel(new DownloadCallback() {
                @Override
                public void onProgress(int percent) {
                    AndroidUtilities.runOnUIThread(() -> {
                        if (finalDialog != null && finalDialog.isShowing()) {
                            finalDialog.setProgress(percent);
                            finalDialog.setMessage(percent + "%");
                        }
                    });
                }

                @Override
                public void onSuccess() {
                    isDownloading = false;
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (finalDialog != null) finalDialog.dismiss();
                        } catch (Exception ignore) {}
                        BaseFragment targetFrag = fragment != null ? fragment : (org.telegram.ui.LaunchActivity.instance != null && org.telegram.ui.LaunchActivity.instance.getActionBarLayout() != null ? org.telegram.ui.LaunchActivity.instance.getActionBarLayout().getLastFragment() : null);
                        if (targetFrag != null) {
                            BulletinFactory.of(targetFrag).createSimpleBulletin(
                                    R.raw.done,
                                    LocaleController.getString("LocalSttModelReady", R.string.LocalSttModelReady)
                            ).show();
                        }
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    isDownloading = false;
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (finalDialog != null) finalDialog.dismiss();
                        } catch (Exception ignore) {}
                        if (fragment != null) {
                            BulletinFactory.of(fragment).createSimpleBulletin(
                                    R.raw.error,
                                    LocaleController.getString("LocalSttDownloadFailed", R.string.LocalSttDownloadFailed)
                            ).show();
                        }
                    });
                }
            });
        });
    }

    private static void downloadAndUnzipModel(DownloadCallback callback) {
        File destDir = getModelDir();
        if (destDir == null) {
            if (callback != null) callback.onError("Target directory unavailable");
            return;
        }
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File tempZip = new File(destDir, "model_temp.zip");
        try {
            URL url = new URL(VOSK_MODEL_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();

            int fileLength = connection.getContentLength();
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(tempZip);

            byte[] data = new byte[8192];
            long total = 0;
            int count;
            int lastProgress = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                if (fileLength > 0) {
                    int percent = (int) (total * 85 / fileLength);
                    if (percent != lastProgress) {
                        lastProgress = percent;
                        if (callback != null) callback.onProgress(percent);
                    }
                }
            }

            output.flush();
            output.close();
            input.close();
            connection.disconnect();

            // Распаковка ZIP
            if (callback != null) callback.onProgress(90);
            unzip(tempZip, destDir);
            tempZip.delete();

            if (callback != null) {
                callback.onProgress(100);
                callback.onSuccess();
            }
        } catch (Throwable e) {
            FileLog.e(e);
            if (tempZip.exists()) tempZip.delete();
            if (callback != null) callback.onError(e.getMessage());
        }
    }

    private static void unzip(File zipFile, File targetDirectory) throws Exception {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry ze;
        byte[] buffer = new byte[8192];

        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(targetDirectory, ze.getName());
            File dir = ze.isDirectory() ? file : file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                throw new java.io.FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
            }
            if (ze.isDirectory()) {
                continue;
            }
            FileOutputStream fout = new FileOutputStream(file);
            int count;
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }
            fout.close();
        }
        zis.close();
    }

    public static void transcribeVoiceAsync(File voiceFile, TranscribeCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (voiceFile == null || !voiceFile.exists()) {
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("Audio file not found"));
                    return;
                }
                if (!isModelDownloaded()) {
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("Model not downloaded"));
                    return;
                }

                // 1. Декодируем Opus/OGG в PCM 16kHz Mono
                byte[] pcmData = decodeAudioToPcm16(voiceFile);
                if (pcmData == null || pcmData.length == 0) {
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("Failed to decode audio"));
                    return;
                }

                // 2. Инициализируем или берем кэшированную модель Vosk
                Model model;
                synchronized (modelLock) {
                    if (sharedModel == null) {
                        File effectiveDir = getEffectiveModelDir();
                        sharedModel = new Model(effectiveDir.getAbsolutePath());
                    }
                    model = sharedModel;
                }

                // 3. Распознаем через Recognizer
                Recognizer recognizer = new Recognizer(model, 16000.0f);
                recognizer.acceptWaveForm(pcmData, pcmData.length);
                String finalResultJson = recognizer.getFinalResult();
                recognizer.close();

                // 4. Извлекаем поле "text" из JSON ответа Vosk
                String recognizedText = "";
                if (!TextUtils.isEmpty(finalResultJson)) {
                    JSONObject obj = new JSONObject(finalResultJson);
                    recognizedText = obj.optString("text", "").trim();
                }

                final String resultString = recognizedText;
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onSuccess(resultString));
                }
            } catch (Throwable e) {
                FileLog.e(e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Декодирует аудиофайл Telegram (OGG Opus) в сырой 16-bit PCM 16000 Hz Mono через Android MediaExtractor + MediaCodec.
     */
    public static byte[] decodeAudioToPcm16(File audioFile) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        ByteArrayOutputStream pcmOutput = new ByteArrayOutputStream();

        try {
            extractor.setDataSource(audioFile.getAbsolutePath());
            int audioTrackIndex = -1;
            MediaFormat format = null;

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i;
                    format = trackFormat;
                    break;
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                extractor.release();
                return null;
            }

            extractor.selectTrack(audioTrackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isInputEOS = false;
            boolean isOutputEOS = false;

            int originalSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    int inIndex = codec.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer inBuffer = codec.getInputBuffer(inIndex);
                        if (inBuffer != null) {
                            int sampleSize = extractor.readSampleData(inBuffer, 0);
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isInputEOS = true;
                            } else {
                                codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                                extractor.advance();
                            }
                        }
                    }
                }

                int outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000);
                if (outIndex >= 0) {
                    ByteBuffer outBuffer = codec.getOutputBuffer(outIndex);
                    if (outBuffer != null && bufferInfo.size > 0) {
                        byte[] chunk = new byte[bufferInfo.size];
                        outBuffer.get(chunk);
                        pcmOutput.write(chunk);
                    }
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true;
                    }
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = codec.getOutputFormat();
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        originalSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                }
            }

            byte[] decodedPcm = pcmOutput.toByteArray();
            // Преобразуем к 16000 Hz Mono если исходный поток отличается
            return convertPcmTo16kMono(decodedPcm, originalSampleRate, channelCount);

        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                    codec.release();
                } catch (Throwable ignore) {}
            }
            try {
                extractor.release();
            } catch (Throwable ignore) {}
        }
    }

    /**
     * Конвертирует PCM (стерео в моно, ресемплинг до 16000 Гц при необходимости).
     */
    private static byte[] convertPcmTo16kMono(byte[] pcmData, int sampleRate, int channels) {
        if (pcmData == null || pcmData.length == 0) return pcmData;

        // 1. Конвертируем в моно, если было 2 канала (Stereo -> Mono)
        byte[] monoData;
        if (channels == 2) {
            monoData = new byte[pcmData.length / 2];
            for (int i = 0; i < monoData.length / 2; i++) {
                int left = (pcmData[i * 4 + 1] << 8) | (pcmData[i * 4] & 0xFF);
                int right = (pcmData[i * 4 + 3] << 8) | (pcmData[i * 4 + 2] & 0xFF);
                int avg = (left + right) / 2;
                monoData[i * 2] = (byte) (avg & 0xFF);
                monoData[i * 2 + 1] = (byte) ((avg >> 8) & 0xFF);
            }
        } else {
            monoData = pcmData;
        }

        // 2. Ресемплинг к 16000 Hz если исходный sampleRate другой (например 48000 Hz у Opus)
        if (sampleRate == 16000) {
            return monoData;
        }

        int totalSamples = monoData.length / 2;
        int targetSamples = (int) ((long) totalSamples * 16000 / sampleRate);
        byte[] resampled = new byte[targetSamples * 2];

        double ratio = (double) sampleRate / 16000.0;
        for (int i = 0; i < targetSamples; i++) {
            double srcIdx = i * ratio;
            int srcIndexInt = (int) srcIdx;
            if (srcIndexInt >= totalSamples - 1) {
                srcIndexInt = totalSamples - 1;
            }
            resampled[i * 2] = monoData[srcIndexInt * 2];
            resampled[i * 2 + 1] = monoData[srcIndexInt * 2 + 1];
        }

        return resampled;
    }
}
