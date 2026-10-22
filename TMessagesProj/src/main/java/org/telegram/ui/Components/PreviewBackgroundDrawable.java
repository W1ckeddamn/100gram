package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class PreviewBackgroundDrawable extends Drawable {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private final float radius;
    private float selectionProgress;

    public PreviewBackgroundDrawable() {
        this(12.0f);
    }

    public PreviewBackgroundDrawable(float radiusDp) {
        this.radius = AndroidUtilities.dp(radiusDp);
        strokePaint.setStyle(Paint.Style.STROKE);
    }

    public void setSelectionProgress(float progress) {
        if (selectionProgress == progress) {
            return;
        }
        selectionProgress = progress;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float strokeWidth = AndroidUtilities.dp(AndroidUtilities.lerp(0.5f, 2.0f, selectionProgress));
        float halfStroke = strokeWidth / 2.0f;
        rectF.set(bounds.left + halfStroke, bounds.top + halfStroke, bounds.right - halfStroke, bounds.bottom - halfStroke);

        backgroundPaint.setColor(PreviewColors.getBackgroundColor());
        canvas.drawRoundRect(rectF, radius, radius, backgroundPaint);

        strokePaint.setColor(ColorUtils.blendARGB(PreviewColors.getOutlineColor(), Theme.getColor(Theme.key_windowBackgroundWhiteValueText), selectionProgress));
        strokePaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(rectF, radius, radius, strokePaint);
    }

    @Override
    public void setAlpha(int alpha) {
        backgroundPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        strokePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
