package org.telegram.messenger.math.inline;

import android.graphics.Canvas;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.widget.TextView;
import org.telegram.messenger.FileLog;

public final class GhostTextLayout {

    public static final class GhostAlphaSpan extends CharacterStyle implements UpdateAppearance {
        private float alpha = 1.0f;

        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        public float getAlpha() {
            return alpha;
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.setAlpha((int) (tp.getAlpha() * alpha));
        }
    }

    private final GhostAlphaSpan ghostAlpha = new GhostAlphaSpan();
    private StaticLayout layout;
    private int insertOffset;
    private float drawTop;
    private boolean movedText;
    private boolean detached;
    private int paragraphStart;
    private int paragraphEnd;
    private int extraHeight;
    private float cursorShiftX;
    private float cursorShiftY;

    public int getParagraphStart() {
        return paragraphStart;
    }

    public int getParagraphEnd() {
        return paragraphEnd;
    }

    public boolean getDetached() {
        return detached;
    }

    public int getExtraHeight() {
        return extraHeight;
    }

    public float getCursorShiftX() {
        return cursorShiftX;
    }

    public float getCursorShiftY() {
        return cursorShiftY;
    }

    public boolean getMovedText() {
        return movedText;
    }

    public void clear() {
        layout = null;
        insertOffset = 0;
        drawTop = 0.0f;
        movedText = false;
        detached = false;
        paragraphStart = 0;
        paragraphEnd = 0;
        extraHeight = 0;
        cursorShiftX = 0.0f;
        cursorShiftY = 0.0f;
    }

    public boolean isEmpty() {
        return layout == null;
    }

    public boolean build(TextView view, Layout real, int start, int end, int caret, CharSequence insert) {
        clear();
        CharSequence text = view.getText();
        if (text == null || start < 0 || end > text.length() || caret < start || caret > end || real.getWidth() <= 0) {
            return false;
        }
        int width = real.getWidth();
        int offset = caret - start;
        SpannableStringBuilder builder = new SpannableStringBuilder(text, start, end);
        builder.insert(offset, insert);
        builder.setSpan(ghostAlpha, offset, offset + insert.length(), 33);
        StaticLayout newLayout = newLayout(view, real, builder, width);
        if (newLayout == null) {
            return false;
        }
        int lineStart = real.getLineForOffset(start);
        int lineEnd = real.getLineForOffset(end);
        this.layout = newLayout;
        this.insertOffset = offset;
        this.paragraphStart = start;
        this.paragraphEnd = end;
        this.drawTop = real.getLineTop(lineStart);
        this.extraHeight = Math.max(0, newLayout.getHeight() - (real.getLineTop(lineEnd + 1) - real.getLineTop(lineStart)));
        this.movedText = offset > 0 && moved(real, newLayout, (start + offset) - 1, offset - 1);
        this.cursorShiftX = newLayout.getPrimaryHorizontal(offset) - real.getPrimaryHorizontal(caret);
        this.cursorShiftY = (newLayout.getLineTop(newLayout.getLineForOffset(offset)) + this.drawTop) - real.getLineTop(real.getLineForOffset(caret));
        return true;
    }

    public boolean buildDetached(TextView view, Layout real, int end, CharSequence insert) {
        clear();
        int width = real.getWidth();
        if (width <= 0) {
            return false;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(insert);
        builder.setSpan(ghostAlpha, 0, builder.length(), 33);
        StaticLayout newLayout = newLayout(view, real, builder, width);
        if (newLayout == null) {
            return false;
        }
        this.layout = newLayout;
        this.detached = true;
        this.drawTop = real.getLineBottom(real.getLineForOffset(end));
        this.extraHeight = newLayout.getHeight();
        return true;
    }

    public void setAlpha(float value) {
        ghostAlpha.setAlpha(value);
    }

    public void draw(Canvas canvas) {
        if (layout == null) {
            return;
        }
        canvas.save();
        canvas.translate(0.0f, drawTop);
        layout.draw(canvas);
        canvas.restore();
    }

    public void readInsertedPositions(int from, int count, float[] x, float[] y) {
        if (layout == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            int offset = insertOffset + from + i;
            x[i] = layout.getPrimaryHorizontal(offset);
            int line = layout.getLineForOffset(offset);
            y[i] = drawTop + layout.getLineBaseline(line);
        }
    }

    private boolean moved(Layout real, StaticLayout shadow, int realOffset, int shadowOffset) {
        return Math.abs(shadow.getPrimaryHorizontal(shadowOffset) - real.getPrimaryHorizontal(realOffset)) >= 0.5f
                || Math.abs((float) shadow.getLineTop(shadow.getLineForOffset(shadowOffset)) - ((float) real.getLineTop(real.getLineForOffset(realOffset)) - drawTop)) >= 0.5f;
    }

    private StaticLayout newLayout(TextView view, Layout real, CharSequence text, int width) {
        try {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), view.getPaint(), width)
                    .setAlignment(real.getAlignment())
                    .setLineSpacing(view.getLineSpacingExtra(), view.getLineSpacingMultiplier())
                    .setIncludePad(view.getIncludeFontPadding())
                    .setBreakStrategy(view.getBreakStrategy())
                    .setHyphenationFrequency(view.getHyphenationFrequency())
                    .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR);
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setJustificationMode(view.getJustificationMode());
            }
            if (Build.VERSION.SDK_INT >= 28) {
                builder.setUseLineSpacingFromFallbacks(view.isFallbackLineSpacing());
            }
            return builder.build();
        } catch (Exception e) {
            FileLog.e(e, false);
            return null;
        }
    }
}
