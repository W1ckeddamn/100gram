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
import java.nio.ByteOrder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HundredGramSttManager {

    // Ссылка на проверенную компактную русскую модель Vosk (~43 MB)
    public static final String VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip";
    public static final String MODEL_DIR_NAME = "vosk-model-ru";

    private static volatile Model sharedModel = null;
    private static final Object modelLock = new Object();
    private static volatile boolean isDownloading = false;

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
            MediaExtractor extractor = null;
            MediaCodec codec = null;
            Recognizer recognizer = null;
            try {
                if (voiceFile == null || !voiceFile.exists()) {
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("Audio file not found"));
                    return;
                }
                if (!isModelDownloaded()) {
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("Model not downloaded"));
                    return;
                }

                extractor = new MediaExtractor();
                extractor.setDataSource(voiceFile.getAbsolutePath());
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
                    if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onError("No audio track found in file"));
                    return;
                }

                extractor.selectTrack(audioTrackIndex);
                String mime = format.getString(MediaFormat.KEY_MIME);
                codec = MediaCodec.createDecoderByType(mime);
                codec.configure(format, null, null, 0);
                codec.start();

                Model model;
                synchronized (modelLock) {
                    if (sharedModel == null) {
                        File effectiveDir = getEffectiveModelDir();
                        sharedModel = new Model(effectiveDir.getAbsolutePath());
                    }
                    model = sharedModel;
                }

                int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                        ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 48000;
                int channelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                        ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

                recognizer = new Recognizer(model, 16000.0f);
                StringBuilder fullText = new StringBuilder();

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                boolean isInputEOS = false;
                boolean isOutputEOS = false;

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
                            outBuffer.position(bufferInfo.offset);
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            byte[] chunk = new byte[bufferInfo.size];
                            outBuffer.get(chunk);

                            byte[] resampledChunk = convertPcmTo16kMono(chunk, sampleRate, channelCount);
                            if (resampledChunk != null && resampledChunk.length > 0) {
                                if (recognizer.acceptWaveForm(resampledChunk, resampledChunk.length)) {
                                    String phrase = extractTextFromJson(recognizer.getResult());
                                    if (!TextUtils.isEmpty(phrase)) {
                                        if (fullText.length() > 0) fullText.append(" ");
                                        fullText.append(phrase);
                                    }
                                }
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false);
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            isOutputEOS = true;
                        }
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = codec.getOutputFormat();
                        if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        }
                        if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        }
                    }
                }

                String finalPhrase = extractTextFromJson(recognizer.getFinalResult());
                if (!TextUtils.isEmpty(finalPhrase)) {
                    if (fullText.length() > 0) fullText.append(" ");
                    fullText.append(finalPhrase);
                }

                String recognizedText = fullText.toString().trim();
                if (recognizedText.length() > 0) {
                    recognizedText = Character.toUpperCase(recognizedText.charAt(0)) + recognizedText.substring(1);
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
            } finally {
                if (recognizer != null) {
                    try { recognizer.close(); } catch (Throwable ignore) {}
                }
                if (codec != null) {
                    try {
                        codec.stop();
                        codec.release();
                    } catch (Throwable ignore) {}
                }
                if (extractor != null) {
                    try { extractor.release(); } catch (Throwable ignore) {}
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
                        outBuffer.position(bufferInfo.offset);
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size);
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
     * Конвертирует PCM: стерео в моно, высококачественный ресемплинг до 16000 Гц с анти-алиасингом и нормализацию громкости (AGC).
     */
    private static byte[] convertPcmTo16kMono(byte[] pcmData, int sampleRate, int channels) {
        if (pcmData == null || pcmData.length == 0) return pcmData;

        // 1. Конвертируем в моно, если было 2 канала (Stereo -> Mono)
        short[] monoSamples;
        if (channels == 2) {
            int totalStereoSamples = pcmData.length / 4;
            monoSamples = new short[totalStereoSamples];
            ByteBuffer bb = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < totalStereoSamples; i++) {
                short left = bb.getShort();
                short right = bb.getShort();
                monoSamples[i] = (short) (((int) left + right) / 2);
            }
        } else {
            int totalMonoSamples = pcmData.length / 2;
            monoSamples = new short[totalMonoSamples];
            ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(monoSamples);
        }

        // 2. Ресемплинг до 16000 Hz
        short[] resampledSamples;
        if (sampleRate == 16000) {
            resampledSamples = monoSamples;
        } else {
            int numInputSamples = monoSamples.length;
            int numOutputSamples = (int) ((long) numInputSamples * 16000 / sampleRate);
            resampledSamples = new short[numOutputSamples];

            if (sampleRate == 48000) {
                // Высококачественный анти-алиасинг: 3:1 усреднение (стандарт для Telegram Opus)
                for (int i = 0; i < numOutputSamples; i++) {
                    int src = i * 3;
                    if (src + 2 < numInputSamples) {
                        resampledSamples[i] = (short) (((int) monoSamples[src] + monoSamples[src + 1] + monoSamples[src + 2]) / 3);
                    } else {
                        resampledSamples[i] = monoSamples[Math.min(src, numInputSamples - 1)];
                    }
                }
            } else {
                // Линейная интерполяция для произвольных частот дискретизации
                double ratio = (double) sampleRate / 16000.0;
                for (int i = 0; i < numOutputSamples; i++) {
                    double srcPos = i * ratio;
                    int index1 = (int) srcPos;
                    int index2 = Math.min(index1 + 1, numInputSamples - 1);
                    double frac = srcPos - index1;
                    int s1 = monoSamples[Math.min(index1, numInputSamples - 1)];
                    int s2 = monoSamples[index2];
                    resampledSamples[i] = (short) (s1 + frac * (s2 - s1));
                }
            }
        }

        // 3. Автоматическая регулировка усиления (AGC) для тихих записей
        int maxAmp = 0;
        for (short s : resampledSamples) {
            int abs = Math.abs(s);
            if (abs > maxAmp) maxAmp = abs;
        }
        if (maxAmp > 0 && maxAmp < 16000) {
            float gain = Math.min(24000f / maxAmp, 3.5f);
            for (int i = 0; i < resampledSamples.length; i++) {
                int val = Math.round(resampledSamples[i] * gain);
                if (val > 32767) val = 32767;
                else if (val < -32768) val = -32768;
                resampledSamples[i] = (short) val;
            }
        }

        byte[] outputPcm = new byte[resampledSamples.length * 2];
        ByteBuffer.wrap(outputPcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(resampledSamples);
        return outputPcm;
    }

    private static String extractTextFromJson(String json) {
        if (TextUtils.isEmpty(json)) return "";
        try {
            JSONObject obj = new JSONObject(json);
            return obj.optString("text", "").trim();
        } catch (Exception ignore) {
            return "";
        }
    }
}
