package org.telegram.ui.Components.camera;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Size;
import android.util.SizeF;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

public abstract class CameraLensStops {
    private static final float[] NO_RATIOS = new float[0];
    private static final float[] SNAP_RATIOS = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 10.0f, 12.0f, 15.0f, 20.0f};
    private static final float[] ROUND_RATIOS = {1.5f, 2.0f, 3.0f, 5.0f, 10.0f, 15.0f, 20.0f, 30.0f};
    private static final float[] RULER_LADDER = {1.0f, 2.0f, 5.0f, 10.0f, 30.0f};
    private static final Map<String, float[]> RATIO_CACHE = new HashMap<>();

    public static float[] opticalZoomRatios(Context context, String str, float f) {
        float[] fArr;
        if (context == null || str == null || Build.VERSION.SDK_INT < 28) {
            return NO_RATIOS;
        }
        Map<String, float[]> map = RATIO_CACHE;
        synchronized (map) {
            fArr = map.get(str);
        }
        if (fArr == null) {
            fArr = readOpticalZoomRatios(context, str);
            synchronized (map) {
                map.put(str, fArr);
            }
        }
        return normalizeRatios(fArr, f);
    }

    private static float[] normalizeRatios(float[] r11, float r12) {
        if (r11 == null || r11.length < 2) {
            return NO_RATIOS;
        }
        float r0 = 0.0f;
        float r3 = 1.0f;
        if (r12 > 0.0f) {
            float r1 = r11[0];
            if (r1 > 1.25f * r12) {
                r12 = r12 / r1;
            } else {
                r12 = r3;
            }
        } else {
            r12 = r3;
        }

        double r4 = Double.MAX_VALUE;
        for (int r6 = 0; r6 < r11.length; r6++) {
            float r7 = r11[r6] * r12;
            double r8 = Math.abs(octaves(r3, r7));
            if (r8 < r4) {
                r0 = r7;
                r4 = r8;
            }
        }

        if (Math.abs(r0 - r3) > 0.12f || r0 <= 0.0f) {
            return NO_RATIOS;
        }

        ArrayList<Float> r1 = new ArrayList<>(r11.length);
        for (int r2 = 0; r2 < r11.length; r2++) {
            float r4_val = snapToNiceRatio((r11[r2] * r12) / r0);
            addDistinctStop(r1, r4_val);
        }
        return toArray(r1);
    }

    private static float[] readOpticalZoomRatios(Context context, String str) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService("camera");
            if (cameraManager == null) {
                return NO_RATIOS;
            }
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(str);
            Set<String> physicalCameraIds = cameraCharacteristics.getPhysicalCameraIds();
            if (physicalCameraIds != null && physicalCameraIds.size() >= 2) {
                float halfFieldOfViewTangent = halfFieldOfViewTangent(cameraCharacteristics);
                if (halfFieldOfViewTangent <= 0.0f) {
                    return NO_RATIOS;
                }
                ArrayList<Float> arrayList = new ArrayList<>(physicalCameraIds.size());
                Iterator<String> it = physicalCameraIds.iterator();
                while (it.hasNext()) {
                    try {
                        float halfFieldOfViewTangent2 = halfFieldOfViewTangent(cameraManager.getCameraCharacteristics(it.next()));
                        if (halfFieldOfViewTangent2 > 0.0f) {
                            addDistinctStop(arrayList, halfFieldOfViewTangent / halfFieldOfViewTangent2);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                return toArray(arrayList);
            }
            return NO_RATIOS;
        } catch (Exception e2) {
            FileLog.e(e2);
            return NO_RATIOS;
        }
    }

    private static float halfFieldOfViewTangent(CameraCharacteristics cameraCharacteristics) {
        float width;
        float width2;
        int width3;
        float[] fArr = (float[]) cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF sizeF = (SizeF) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        Rect rect = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Size size = (Size) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        Integer num = (Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (fArr != null && fArr.length != 0 && sizeF != null && rect != null && size != null && num != null) {
            if (num.intValue() % 180 == 90) {
                width = sizeF.getHeight();
                width2 = rect.height();
                width3 = size.getHeight();
            } else {
                width = sizeF.getWidth();
                width2 = rect.width();
                width3 = size.getWidth();
            }
            float f = width3;
            float f2 = fArr[0];
            if (f2 > 0.0f && width > 0.0f && width2 > 0.0f && f > 0.0f) {
                return ((width * width2) / f) / (f2 * 2.0f);
            }
        }
        return 0.0f;
    }

    public static float[] buildToggleStops(boolean isFrontface, float minZoom, float maxZoom, float[] opticalRatios) {
        float[] telephotoRatios = telephotoRatios(opticalRatios, maxZoom);
        if (telephotoRatios.length == 0) {
            return boundStops(isFrontface ? new float[]{minZoom, 1.0f, 2.0f} : new float[]{minZoom, 1.0f, 2.0f, 5.0f}, minZoom, maxZoom, false);
        }
        ArrayList<Float> arrayList = new ArrayList<>(6);
        if (minZoom < 0.9999f) {
            addDistinctStop(arrayList, minZoom);
        }
        addDistinctStop(arrayList, Utilities.clamp(1.0f, maxZoom, minZoom));
        for (float f3 : telephotoRatios) {
            addDistinctStop(arrayList, f3);
        }
        fillWideGaps(arrayList);
        addReachStop(arrayList, telephotoRatios[telephotoRatios.length - 1], maxZoom);
        dropCrowdedStops(arrayList);
        return toArray(arrayList);
    }

    public static float[] buildRulerStops(float minZoom, float maxZoom, float[] toggleStops) {
        ArrayList<Float> arrayList = new ArrayList<>(toggleStops.length + RULER_LADDER.length + 2);
        addDistinctStop(arrayList, minZoom);
        for (float f3 : toggleStops) {
            addDistinctStop(arrayList, Utilities.clamp(f3, maxZoom, minZoom));
        }
        addDistinctStop(arrayList, maxZoom);
        for (float f4 : RULER_LADDER) {
            if (f4 >= minZoom - 1.0E-4f && f4 <= 1.0E-4f + maxZoom && nearestOctaveDistance(arrayList, f4) >= 0.55d) {
                addDistinctStop(arrayList, f4);
            }
        }
        return toArray(arrayList);
    }

    private static float[] telephotoRatios(float[] fArr, float f) {
        if (fArr == null || fArr.length == 0) {
            return NO_RATIOS;
        }
        ArrayList<Float> arrayList = new ArrayList<>(fArr.length);
        for (float f2 : fArr) {
            if (f2 >= 1.15f && f2 <= 1.0E-4f + f) {
                addDistinctStop(arrayList, f2);
            }
        }
        return toArray(arrayList);
    }

    private static void fillWideGaps(ArrayList<Float> arrayList) {
        while (arrayList.size() < 4) {
            int i = -1;
            double d = 2.0d;
            for (int i2 = 1; i2 < arrayList.size(); i2++) {
                double octaves = octaves(arrayList.get(i2 - 1).floatValue(), arrayList.get(i2).floatValue());
                if (octaves > d) {
                    i = i2;
                    d = octaves;
                }
            }
            if (i < 0) {
                return;
            }
            float chooseRoundRatio = chooseRoundRatio(arrayList.get(i - 1).floatValue(), arrayList.get(i).floatValue());
            if (chooseRoundRatio <= 0.0f) {
                return;
            }
            int size = arrayList.size();
            addDistinctStop(arrayList, chooseRoundRatio);
            if (arrayList.size() == size) {
                return;
            }
        }
    }

    private static float chooseRoundRatio(float f, float f2) {
        double sqrt = Math.sqrt(f * f2);
        float f3 = 0.0f;
        double d = Double.MAX_VALUE;
        for (float f4 : ROUND_RATIOS) {
            if (f4 > f + 1.0E-4f && f4 < f2 - 1.0E-4f) {
                double abs = Math.abs(octaves(f4, (float) sqrt));
                if (abs < d) {
                    f3 = f4;
                    d = abs;
                }
            }
        }
        return f3;
    }

    private static void addReachStop(ArrayList<Float> arrayList, float f, float f2) {
        if (arrayList.size() >= 4 || arrayList.isEmpty()) {
            return;
        }
        float snapToNiceRatio = snapToNiceRatio(f * 2.0f);
        if (snapToNiceRatio > f2 + 1.0E-4f || snapToNiceRatio <= arrayList.get(arrayList.size() - 1).floatValue() + 1.0E-4f) {
            return;
        }
        addDistinctStop(arrayList, snapToNiceRatio);
    }

    private static void dropCrowdedStops(ArrayList<Float> arrayList) {
        while (arrayList.size() > 5) {
            int i = -1;
            double d = Double.MAX_VALUE;
            for (int i2 = 1; i2 < arrayList.size() - 1; i2++) {
                double octaves = octaves(arrayList.get(i2 - 1).floatValue(), arrayList.get(i2).floatValue());
                if (octaves < d) {
                    i = i2;
                    d = octaves;
                }
            }
            if (i < 0) {
                return;
            } else {
                arrayList.remove(i);
            }
        }
    }

    private static float snapToNiceRatio(float f) {
        if (!Float.isFinite(f) || f <= 0.0f) {
            return 0.0f;
        }
        float f2 = Float.MAX_VALUE;
        float f3 = 0.0f;
        for (float f4 : SNAP_RATIOS) {
            float f5 = (f4 - f) / f;
            if (f5 >= -0.03f && f5 <= 0.1f) {
                float abs = Math.abs(f5);
                if (abs < f2) {
                    f3 = f4;
                    f2 = abs;
                }
            }
        }
        return f3 > 0.0f ? f3 : Math.round(f * 10.0f) / 10.0f;
    }

    private static double nearestOctaveDistance(ArrayList<Float> arrayList, float f) {
        double d = Double.MAX_VALUE;
        for (int i = 0; i < arrayList.size(); i++) {
            d = Math.min(d, Math.abs(octaves(arrayList.get(i).floatValue(), f)));
        }
        return d;
    }

    private static double octaves(float f, float f2) {
        if (f <= 0.0f || f2 <= 0.0f) {
            return 0.0d;
        }
        return Math.log(f2 / f) / Math.log(2.0d);
    }

    private static float[] boundStops(float[] fArr, float f, float f2, boolean z) {
        ArrayList<Float> arrayList = new ArrayList<>(fArr.length + 2);
        if (z) {
            addDistinctStop(arrayList, f);
        }
        for (float f3 : fArr) {
            if (f3 > 0.0f && Float.isFinite(f3)) {
                if (f3 < f - 1.0E-4f) {
                    if (!z) {
                        addDistinctStop(arrayList, f);
                    }
                } else if (f3 <= 1.0E-4f + f2) {
                    addDistinctStop(arrayList, Utilities.clamp(f3, f2, f));
                }
            }
        }
        if (z) {
            addDistinctStop(arrayList, f2);
        }
        return toArray(arrayList);
    }

    private static void addDistinctStop(ArrayList<Float> arrayList, float f) {
        if (f <= 0.0f || !Float.isFinite(f)) {
            return;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            float floatValue = arrayList.get(i).floatValue();
            if (Math.abs(floatValue - f) <= 1.0E-4f) {
                return;
            }
            if (floatValue > f) {
                arrayList.add(i, Float.valueOf(f));
                return;
            }
        }
        arrayList.add(Float.valueOf(f));
    }

    private static float[] toArray(ArrayList<Float> arrayList) {
        int size = arrayList.size();
        float[] fArr = new float[size];
        for (int i = 0; i < size; i++) {
            fArr[i] = arrayList.get(i).floatValue();
        }
        return fArr;
    }
}
