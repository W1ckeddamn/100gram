package org.telegram.messenger.camera.camerax;

import static android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.SharedConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CameraXUtils {

    private static Map<Quality, Size> qualityToSize;
    private static Exception qualityException;
    private static int cameraResolution = -1;

    public static boolean isCameraXSupported() {
        try {
            return SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isWideAngleAvailable(ProcessCameraProvider provider) {
        if (provider == null) {
            return false;
        }
        return getWideCameraId(provider) != null;
    }

    @SuppressLint("UnsafeOptInUsageError")
    public static CameraSelector getDefaultWideAngleCamera(ProcessCameraProvider provider) {
        String wideCamera = getWideCameraId(provider);
        if (wideCamera != null) {
            return new CameraSelector.Builder().addCameraFilter(cameraInfo -> {
                List<CameraInfo> cameraFiltered = new ArrayList<>();
                for (int i = 0; i < cameraInfo.size(); i++) {
                    CameraInfo c = cameraInfo.get(i);
                    String id = Camera2CameraInfo.from(c).getCameraId();
                    if (id.equals(wideCamera)) {
                        cameraFiltered.add(c);
                    }
                }
                return cameraFiltered;
            }).build();
        }
        throw new IllegalArgumentException("This device doesn't support wide camera! "
                + "isWideAngleAvailable should be checked first before calling "
                + "getDefaultWideAngleCamera.");
    }

    public static Map<Quality, Size> getAvailableVideoSizes() throws IllegalStateException {
        if (qualityException != null) {
            throw new IllegalStateException("CameraX sizes failed to load!", qualityException);
        }
        return qualityToSize == null ? new HashMap<>() : qualityToSize;
    }

    public static void loadCameraXSizes() {
        if (qualityToSize != null || qualityException != null) {
            return;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return;
        ListenableFuture<ProcessCameraProvider> providerFtr = ProcessCameraProvider.getInstance(context);
        providerFtr.addListener(() -> {
            ProcessCameraProvider provider = null;
            try {
                CameraSelector.Builder cameraBuilder = new CameraSelector.Builder();
                provider = providerFtr.get();
                CameraSelector camera = cameraBuilder.build();
                qualityToSize = getAvailableVideoSizes(camera, provider);
                loadSuggestedResolution();
            } catch (Exception e) {
                qualityException = e;
                FileLog.e(e);
            } finally {
                if (provider != null) {
                    try {
                        provider.unbindAll();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private static Map<Quality, Size> getAvailableVideoSizes(CameraSelector cameraSelector, ProcessCameraProvider provider) {
        return cameraSelector.filter(provider.getAvailableCameraInfos()).parallelStream()
                .findFirst()
                .map(camInfo ->
                        QualitySelector.getSupportedQualities(camInfo).parallelStream().collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        quality -> Optional.ofNullable(QualitySelector.getResolution(camInfo, quality))
                                                .orElse(new Size(0, 0)),
                                        (v1, v2) -> v1,
                                        HashMap::new
                                )
                        )
                ).orElse(new HashMap<>());
    }

    public static void loadSuggestedResolution() {
        int suggestedRes = getSuggestedResolution(false);
        Map<Quality, Size> sizes = getAvailableVideoSizes();

        sizes.values().parallelStream()
                .sorted(Comparator.comparingInt(Size::getHeight).reversed())
                .mapToInt(Size::getHeight)
                .filter(height -> height <= suggestedRes)
                .findFirst()
                .ifPresent(height -> cameraResolution = height);
    }

    public static int getCameraResolution() {
        return cameraResolution;
    }

    public static Size getPreviewBestSize() {
        int suggestedRes = getSuggestedResolution(true);
        int targetRes = cameraResolution > 0 ? cameraResolution : suggestedRes;
        return getAvailableVideoSizes().values().parallelStream()
                .filter(size -> size.getHeight() <= targetRes && size.getHeight() <= suggestedRes)
                .max(Comparator.comparingInt(Size::getHeight))
                .orElse(new Size(1080, 1080));
    }

    public static Quality getVideoQuality() {
        if (HundredGramConfig.cameraPerformanceMode) {
            return getAvailableVideoSizes().entrySet().parallelStream()
                    .filter(entry -> entry.getValue().getHeight() <= 1080)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(Quality.FHD);
        }
        int userRes = HundredGramConfig.cameraQuality;
        int targetRes = userRes > 0 ? userRes : (cameraResolution > 0 ? cameraResolution : 1080);
        return getAvailableVideoSizes().entrySet().parallelStream()
                .filter(entry -> entry.getValue().getHeight() == targetRes)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(Quality.HIGHEST);
    }

    public static class CameraReport {
        public boolean cameraXSupported;
        public boolean can4K;
        public boolean can60Fps;
        public boolean wideAngleAvailable;
        public boolean stabilizationAvailable;
        public boolean hdrAvailable;
    }

    public static CameraReport diagnoseCamera(Context context) {
        CameraReport report = new CameraReport();
        report.cameraXSupported = isCameraXSupported();
        Map<Quality, Size> sizes = getAvailableVideoSizes();
        for (Size s : sizes.values()) {
            if (s.getHeight() >= 2160) {
                report.can4K = true;
                break;
            }
        }
        report.can60Fps = SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE;
        try {
            android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager != null) {
                for (String id : manager.getCameraIdList()) {
                    CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                    int[] stabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
                    if (stabModes != null && stabModes.length > 1) {
                        report.stabilizationAvailable = true;
                    }
                    float[] focal = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focal != null && focal.length > 0 && focal[0] < 3.0f) {
                        report.wideAngleAvailable = true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        report.hdrAvailable = report.can60Fps;
        return report;
    }

    private static int getSuggestedResolution(boolean isPreview) {
        int suggestedRes;
        switch (SharedConfig.getDevicePerformanceClass()) {
            case SharedConfig.PERFORMANCE_CLASS_LOW:
                suggestedRes = 720;
                break;
            case SharedConfig.PERFORMANCE_CLASS_AVERAGE:
                suggestedRes = 1080;
                break;
            case SharedConfig.PERFORMANCE_CLASS_HIGH:
            default:
                suggestedRes = isPreview ? 1080 : 2160;
                break;
        }
        return suggestedRes;
    }

    @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
    public static String getWideCameraId(ProcessCameraProvider provider) {
        if (provider == null) return null;
        float lowestAngledCamera = Integer.MAX_VALUE;
        List<CameraInfo> cameraInfoList = provider.getAvailableCameraInfos();
        String cameraId = null;
        int availableBackCamera = 0;
        boolean foundWideAngleOnPrimaryCamera = false;
        for (int i = 0; i < cameraInfoList.size(); i++) {
            CameraInfo cameraInfo = cameraInfoList.get(i);
            try {
                String id = Camera2CameraInfo.from(cameraInfo).getCameraId();
                CameraCharacteristics cameraCharacteristics = Camera2CameraInfo.from(cameraInfo).getCameraCharacteristicsMap().get(id);
                if (cameraCharacteristics != null) {
                    Integer lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (lensFacing != null && lensFacing == LENS_FACING_BACK) {
                        availableBackCamera++;
                        ZoomState zoomState = cameraInfo.getZoomState().getValue();
                        if (zoomState != null && zoomState.getMinZoomRatio() < 1.0F && zoomState.getMinZoomRatio() > 0) {
                            foundWideAngleOnPrimaryCamera = true;
                        }
                        float[] listLensAngle = cameraCharacteristics.get(LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        if (listLensAngle != null && listLensAngle.length > 0) {
                            if (listLensAngle[0] < 3.0f && listLensAngle[0] < lowestAngledCamera) {
                                lowestAngledCamera = listLensAngle[0];
                                cameraId = id;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
        return availableBackCamera >= 2 && !foundWideAngleOnPrimaryCamera ? cameraId : null;
    }
}
