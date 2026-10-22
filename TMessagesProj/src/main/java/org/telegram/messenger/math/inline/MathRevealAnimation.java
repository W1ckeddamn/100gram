package org.telegram.messenger.math.inline;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.CubicBezierInterpolator;

public final class MathRevealAnimation {

    public static final class MaskSpan extends CharacterStyle implements UpdateAppearance {
        private final MathRevealAnimation owner;

        public MaskSpan(MathRevealAnimation owner) {
            this.owner = owner;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            if (this.owner.maskSpan == this) {
                tp.setAlpha(0);
            }
        }
    }

    private final TextView view;
    private final Runnable onFinished;
    private final TextPaint paint = new TextPaint(1);

    private MaskSpan maskSpan;
    private int rangeStart = -1;
    private int rangeEnd = -1;
    private long startedAt;
    private float duration = 200.0f;
    private float[] fromX = new float[0];
    private float[] fromY = new float[0];
    private float[] toX = new float[0];
    private float[] toY = new float[0];
    private Layout targetLayout;

    public MathRevealAnimation(TextView view, Runnable onFinished) {
        this.view = view;
        this.onFinished = onFinished;
    }

    public boolean isRunning() {
        return rangeStart >= 0;
    }

    public boolean isCaretOutside(int caret) {
        return caret < rangeStart || caret > rangeEnd;
    }

    public void begin(Editable text, int from, int count, float[] originX, float[] originY) {
        this.rangeStart = from;
        this.rangeEnd = from + count;
        this.fromX = originX;
        this.fromY = originY;
        this.toX = new float[count];
        this.toY = new float[count];
        this.targetLayout = null;
        this.startedAt = SystemClock.elapsedRealtime();
        this.duration = Math.min(420.0f, (count * 28.0f) + 200.0f);
        this.maskSpan = new MaskSpan(this);
        text.setSpan(this.maskSpan, rangeStart, rangeEnd, 33);
    }

    public void cancel() {
        stop(false);
    }

    public void draw(Canvas canvas, int accent) {
        Layout layout = view.getLayout();
        CharSequence text = view.getText();
        if (layout == null || text == null || rangeEnd > text.length()) {
            stop(true);
            return;
        }
        int count = rangeEnd - rangeStart;
        if (targetLayout != layout) {
            targetLayout = layout;
            for (int i = 0; i < count; i++) {
                int offset = rangeStart + i;
                toX[i] = layout.getPrimaryHorizontal(offset);
                int line = layout.getLineForOffset(offset);
                toY[i] = layout.getLineBaseline(line);
            }
        }
        float elapsed = (float) (SystemClock.elapsedRealtime() - startedAt);
        float progress = Math.max(0.0f, Math.min(1.0f, elapsed / duration));
        float colorProgress = Math.max(0.0f, Math.min(1.0f, elapsed / 800.0f));
        int blendedColor = ColorUtils.blendARGB(accent, view.getCurrentTextColor(), CubicBezierInterpolator.EASE_BOTH.getInterpolation(colorProgress));
        int alpha = Color.alpha(blendedColor);
        float textSizeOffset = view.getPaint().getTextSize() * 0.32f;
        paint.set(view.getPaint());
        paint.setColor(blendedColor);

        for (int i = 0; i < count; i++) {
            int charIndex = rangeStart + i;
            float cascade = AndroidUtilities.cascade(progress, i, count, 3.5f);
            float interp = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(cascade);
            float scaleInterp = CubicBezierInterpolator.EASE_OUT_BACK.getInterpolation(cascade);
            float curX = AndroidUtilities.lerp(fromX[i], toX[i], interp);
            float curY = AndroidUtilities.lerp(fromY[i], toY[i], interp);
            float curScale = AndroidUtilities.lerp(1.12f, 1.0f, scaleInterp);
            paint.setAlpha((int) (alpha * AndroidUtilities.lerp(0.4f, 1.0f, interp)));

            int save = canvas.save();
            float textWidth = paint.measureText(text, charIndex, charIndex + 1);
            canvas.scale(curScale, curScale, curX + (textWidth / 2.0f), curY - textSizeOffset);
            try {
                canvas.drawText(text, charIndex, charIndex + 1, curX, curY, paint);
            } finally {
                canvas.restoreToCount(save);
            }
        }

        if (progress < 1.0f || colorProgress < 1.0f) {
            view.invalidate();
        } else {
            stop(true);
        }
    }

    private void stop(boolean deferred) {
        MaskSpan span = this.maskSpan;
        if (rangeStart >= 0 || span != null) {
            rangeStart = -1;
            rangeEnd = -1;
            maskSpan = null;
            targetLayout = null;
            if (span != null) {
                if (deferred) {
                    AndroidUtilities.runOnUIThread(this::removeMasks);
                } else {
                    removeMasks();
                }
            }
            onFinished.run();
            view.invalidate();
        }
    }

    private void removeMasks() {
        CharSequence text = view.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            MaskSpan[] spans = spannable.getSpans(0, spannable.length(), MaskSpan.class);
            if (spans != null) {
                for (MaskSpan s : spans) {
                    spannable.removeSpan(s);
                }
            }
        }
    }
}
