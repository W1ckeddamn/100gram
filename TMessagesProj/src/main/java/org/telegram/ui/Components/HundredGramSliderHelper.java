package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import org.telegram.messenger.AndroidUtilities;

/**
 * Helper class for rendering Material Design 3 and Android 13/14 Expressive
 * wave sliders (ported and adapted from Opexgram).
 */
public class HundredGramSliderHelper {

    private static final Path tmpPath = new Path();
    private static final float[] tmpRadii = new float[8];

    public static final float[] EXPRESSIVE_TRACK_HEIGHTS = {8.0f, 4.0f, 2.0f, 2.0f};
    public static final float[] EXPRESSIVE_GAPS = {6.0f, 4.0f, 0.0f, 3.0f};
    public static final float[] EXPRESSIVE_HANDLE_WIDTH_IDLE = {4.0f, 12.0f, 0.0f, 3.0f};
    public static final float[] EXPRESSIVE_HANDLE_WIDTH_PRESSED = {2.0f, 16.0f, 0.0f, 2.0f};
    public static final float[] EXPRESSIVE_HANDLE_HEIGHTS = {20.0f, 0.0f, 0.0f, 16.0f};
    public static final float[] EXPRESSIVE_WAVE_AMPLITUDES = {0.0f, 3.0f, 0.0f, 2.5f};
    public static final float[] EXPRESSIVE_WAVE_LENGTHS = {0.0f, 32.0f, 0.0f, 24.0f};
    public static final float[] EXPRESSIVE_END_DOTS = {4.0f, 4.0f, 0.0f, 0.0f};

    /**
     * Draws an MD3 slider track segment with optional asymmetric corner rounding.
     */
    public static void drawTrack(Canvas canvas, RectF rectF, float left, float right, boolean roundedEnds, Paint paint) {
        float minDim = Math.min(rectF.height(), rectF.width()) / 2.0f;
        if (minDim <= 0.0f) {
            return;
        }
        float halfHeight = rectF.height() / 2.0f;
        if (!roundedEnds) {
            canvas.drawRoundRect(rectF, halfHeight, halfHeight, paint);
            return;
        }
        float smallRad = Math.min(halfHeight, AndroidUtilities.dpf2(2.0f));
        float radLeft = Math.min(minDim, rectF.left <= left + 0.5f ? halfHeight : smallRad);
        float radRight = rectF.right < right - 0.5f ? smallRad : halfHeight;
        float maxRad = Math.min(minDim, radRight);
        if (radLeft == maxRad) {
            canvas.drawRoundRect(rectF, radLeft, radLeft, paint);
            return;
        }
        float[] radii = tmpRadii;
        radii[0] = radii[1] = radii[6] = radii[7] = radLeft;
        radii[2] = radii[3] = radii[4] = radii[5] = maxRad;
        Path path = tmpPath;
        path.rewind();
        path.addRoundRect(rectF, radii, Path.Direction.CW);
        canvas.drawPath(path, paint);
    }

    /**
     * Generates a sinusoidal wavy track path for the Android 13/14 Media Player slider.
     */
    public static void generateWavePath(Path path, float startX, float endX, float centerY, float amplitude, float wavelength, float phase) {
        float currentX = startX;
        path.rewind();
        if (endX <= currentX || wavelength <= 0.0f) {
            path.moveTo(currentX, centerY);
            path.lineTo(Math.max(startX, endX), centerY);
            return;
        }
        float step = Math.max(1.0f, wavelength / 16.0f);
        float shiftedX = currentX + phase;
        float nextSampleX = (((float) Math.ceil(shiftedX / step)) * step) - phase;
        float currentY = (((float) Math.sin(((double) (shiftedX / wavelength)) * Math.PI * 2.0d)) * amplitude) + centerY;
        path.moveTo(currentX, currentY);
        if (nextSampleX - currentX < 0.05f * step) {
            nextSampleX += step;
        }
        while (true) {
            float segmentEndX = Math.min(nextSampleX, endX);
            float nextY = (((float) Math.sin(((double) ((segmentEndX + phase) / wavelength)) * Math.PI * 2.0d)) * amplitude) + centerY;
            path.quadTo(currentX, currentY, (currentX + segmentEndX) / 2.0f, (currentY + nextY) / 2.0f);
            if (segmentEndX >= endX) {
                path.lineTo(segmentEndX, nextY);
                return;
            } else {
                nextSampleX += step;
                currentX = segmentEndX;
                currentY = nextY;
            }
        }
    }

    /**
     * Calculates the animated wave phase offset based on elapsed time and wave speed.
     */
    public static float calculateWavePhase(long timeMs, float speed, float wavelength) {
        if (wavelength <= 0.0f) {
            return 0.0f;
        }
        return (float) (((timeMs * ((double) speed)) / 1000.0d) % ((double) wavelength));
    }
}
