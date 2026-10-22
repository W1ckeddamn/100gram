package org.telegram.ui.Components.PillStack;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class ColoredBackground extends Drawable {

    private final Paint paint;
    private final Paint strokePaint;

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public ColoredBackground() {
        this(-14965523, -15431455);
    }

    public ColoredBackground(int topColor, int bottomColor) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, AndroidUtilities.dp(28.0f),
                new int[]{topColor, bottomColor}, new float[]{0.0f, 1.0f}, tileMode));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(1.0f));
        strokePaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, AndroidUtilities.dp(28.0f),
                new int[]{1308622847, 0, 452984831}, new float[]{0.0f, 0.5f, 1.0f}, tileMode));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float radius = AndroidUtilities.dp(14.0f);
        RectF rectF = AndroidUtilities.rectTmp;
        rectF.set(bounds);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        Theme.ThemeInfo activeTheme = Theme.getActiveTheme();
        boolean isMonet = activeTheme != null && activeTheme.isMonet();
        if (!Theme.isCurrentThemeDark() || isMonet) {
            return;
        }
        float strokeWidth = AndroidUtilities.dp(1.0f);
        strokePaint.setStrokeWidth(strokeWidth);
        float inset = strokeWidth / 2.0f;
        rectF.inset(inset, inset);
        canvas.drawRoundRect(rectF, radius, radius, strokePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        strokePaint.setColorFilter(colorFilter);
    }
}
