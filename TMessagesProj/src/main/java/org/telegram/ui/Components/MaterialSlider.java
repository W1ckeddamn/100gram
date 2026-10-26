package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * Material-style slider: thick rounded track and a slim vertical thumb.
 * ExteraGram renders this with com.google.android.material.slider.Slider; it is drawn here
 * by hand so the app does not need the Material Components dependency.
 */
public class MaterialSlider extends View {

    public interface OnValueChangeListener {
        void onValueChange(float value, boolean fromUser);
    }

    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF activeRect = new RectF();
    private final RectF thumbRect = new RectF();

    private float valueFrom;
    private float valueTo = 1.0f;
    private float value;
    private OnValueChangeListener listener;

    public MaterialSlider(Context context) {
        super(context);
        activePaint.setColor(Theme.getColor(Theme.key_player_progress));
        thumbPaint.setColor(Theme.getColor(Theme.key_player_progress));
        inactivePaint.setColor(Theme.getColor(Theme.key_player_progressBackground));
    }

    public void setValueFrom(float from) {
        valueFrom = from;
        invalidate();
    }

    public void setValueTo(float to) {
        valueTo = to;
        invalidate();
    }

    public float getValue() {
        return value;
    }

    public void setValue(float newValue) {
        float clamped = MathUtils.clamp(newValue, valueFrom, valueTo);
        if (Math.abs(clamped - value) > 1e-4f) {
            value = clamped;
            invalidate();
        }
    }

    public void addOnChangeListener(OnValueChangeListener l) {
        listener = l;
    }

    private float getThumbWidth() {
        return AndroidUtilities.dp(3.0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(40.0f), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float trackHeight = AndroidUtilities.dp(8.0f);
        final float thumbWidth = getThumbWidth();
        final float thumbHeight = AndroidUtilities.dp(24.0f);
        final float centerY = getHeight() / 2.0f;
        final float inset = thumbWidth / 2.0f;
        final float radius = trackHeight / 2.0f;
        final float range = valueTo - valueFrom;
        final float fraction = range > 0 ? MathUtils.clamp((value - valueFrom) / range, 0.0f, 1.0f) : 0.0f;

        trackRect.set(inset, centerY - radius, getWidth() - inset, centerY + radius);
        canvas.drawRoundRect(trackRect, radius, radius, inactivePaint);

        final float thumbCenterX = trackRect.left + fraction * trackRect.width();
        if (thumbCenterX > trackRect.left) {
            // the rounded track is drawn again over the active part so the left cap keeps its shape
            activeRect.set(trackRect.left, trackRect.top, thumbCenterX, trackRect.bottom);
            canvas.drawRoundRect(activeRect, radius, radius, activePaint);
        }

        thumbRect.set(thumbCenterX - thumbWidth / 2.0f, centerY - thumbHeight / 2.0f,
                thumbCenterX + thumbWidth / 2.0f, centerY + thumbHeight / 2.0f);
        canvas.drawRoundRect(thumbRect, thumbWidth / 2.0f, thumbWidth / 2.0f, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                setValueFromTouch(event.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                setValueFromTouch(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void setValueFromTouch(float x) {
        final float inset = getThumbWidth() / 2.0f;
        final float usable = getWidth() - inset * 2.0f;
        if (usable <= 0) {
            return;
        }
        final float fraction = MathUtils.clamp((x - inset) / usable, 0.0f, 1.0f);
        final float newValue = valueFrom + fraction * (valueTo - valueFrom);
        if (Math.abs(newValue - value) > 1e-4f) {
            value = newValue;
            invalidate();
            if (listener != null) {
                listener.onValueChange(newValue, true);
            }
        }
    }
}
