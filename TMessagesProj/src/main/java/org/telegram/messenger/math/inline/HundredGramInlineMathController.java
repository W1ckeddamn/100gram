package org.telegram.messenger.math.inline;

import android.graphics.Canvas;
import android.graphics.Region;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.MetricAffectingSpan;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.TextView;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.math.MathExpression;
import org.telegram.messenger.math.MathOptions;
import org.telegram.messenger.math.MathSuggestion;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.TextStyleSpan;

public final class HundredGramInlineMathController {

    public interface Delegate {
        int accentColor();
        void runProgrammatic(Runnable action);
    }

    private final TextView view;
    private final Delegate delegate;
    private final GhostTextLayout ghost = new GhostTextLayout();
    private final AnimatedFloat appear;
    private final MathRevealAnimation reveal;

    private MathSuggestion suggestion;
    private boolean dirty = true;
    private int layoutWidth = -1;
    private String announcedValue;
    private final Runnable announce = this::performAnnouncement;
    private MathOptions options = new MathOptions('.');
    private Locale optionsLocale;
    private int undoStart = -1;
    private int undoEnd = -1;
    private int suppressedAt = -1;
    private boolean insertingSelf;
    private boolean caretMovedByTouch;
    private int swallowKeyCode;

    public HundredGramInlineMathController(TextView textView, Delegate delegate) {
        this.view = textView;
        this.delegate = delegate;
        this.appear = new AnimatedFloat(textView, 0L, 120L, CubicBezierInterpolator.EASE_OUT_QUINT);
        this.reveal = new MathRevealAnimation(textView, () -> this.dirty = true);
    }

    private void performAnnouncement() {
        if (suggestion != null && suggestion.getValue() != null) {
            announcedValue = suggestion.getValue();
            AndroidUtilities.makeAccessibilityAnnouncement(LocaleController.formatString(R.string.InlineMathResultAnnouncement, announcedValue));
        }
    }

    public int getExtraBottom() {
        return ghost.getExtraHeight();
    }

    public boolean hasCursorShift() {
        return ghost.getMovedText();
    }

    public float getCursorShiftX() {
        return hasCursorShift() ? ghost.getCursorShiftX() : 0.0f;
    }

    public float getCursorShiftY() {
        return hasCursorShift() ? ghost.getCursorShiftY() : 0.0f;
    }

    public void clipReplacedParagraph(Canvas canvas, int top) {
        if (ghost.isEmpty() || ghost.getDetached() || view.getLayout() == null) {
            return;
        }
        Layout layout = view.getLayout();
        float f = top;
        int lineStart = layout.getLineForOffset(ghost.getParagraphStart());
        int lineEnd = layout.getLineForOffset(ghost.getParagraphEnd());
        canvas.clipRect(0.0f, f + layout.getLineTop(lineStart), view.getWidth(), f + layout.getLineBottom(lineEnd), Region.Op.DIFFERENCE);
    }

    public void draw(Canvas canvas, int left, int top, float clipTop, float clipBottom) {
        boolean isRunning = reveal.isRunning();
        if (!isRunning && ghost.isEmpty()) {
            appear.set(0.0f, true);
            return;
        }
        canvas.save();
        canvas.clipRect(0.0f, clipTop, view.getWidth(), clipBottom);
        canvas.translate(left, top);
        if (isRunning) {
            int accent = delegate != null ? delegate.accentColor() : view.getCurrentTextColor();
            reveal.draw(canvas, accent);
        } else {
            ghost.setAlpha(appear.set(1.0f) * 0.4f);
            ghost.draw(canvas);
        }
        canvas.restore();
    }

    public void invalidateState() {
        if (!insertingSelf) {
            int sel = view.getSelectionStart();
            if (sel != suppressedAt) {
                suppressedAt = -1;
            }
            if (sel != undoEnd || view.getSelectionEnd() != undoEnd) {
                clearUndo();
            }
            if (reveal.isRunning() && reveal.isCaretOutside(sel)) {
                reveal.cancel();
            }
        }
        schedule();
    }

    public void onTextChanged() {
        if (!insertingSelf) {
            reveal.cancel();
            clearUndo();
            suppressedAt = -1;
            caretMovedByTouch = false;
        }
        schedule();
    }

    public void onTouchDown() {
        if (caretMovedByTouch) {
            return;
        }
        caretMovedByTouch = true;
        schedule();
    }

    public void onFocusChanged(boolean focused) {
        if (!focused) {
            reveal.cancel();
            clearUndo();
        }
        schedule();
    }

    public void cancel() {
        AndroidUtilities.cancelRunOnUIThread(announce);
        announcedValue = null;
        reveal.cancel();
        clearUndo();
        suppressedAt = -1;
        caretMovedByTouch = false;
        suggestion = null;
        ghost.clear();
        schedule();
    }

    public boolean updateOnMeasure() {
        int extra = ghost.getExtraHeight();
        Layout layout = view.getLayout();
        int width = layout != null ? layout.getWidth() : 0;
        if (width != layoutWidth) {
            layoutWidth = width;
            dirty = true;
        }
        update();
        if (AndroidUtilities.isAccessibilityScreenReaderEnabled()) {
            updateAnnouncement();
        }
        return ghost.getExtraHeight() != extra;
    }

    private void schedule() {
        dirty = true;
        view.invalidate();
        if (ghost.getExtraHeight() == 0 && suggestion == null && !canTrigger()) {
            return;
        }
        view.requestLayout();
    }

    private boolean canTrigger() {
        CharSequence text = view.getText();
        if (!HundredGramConfig.inlineMath || text == null) {
            return false;
        }
        int sel = view.getSelectionStart();
        return sel >= 1 && sel <= text.length() && text.charAt(sel - 1) == '=';
    }

    private void updateAnnouncement() {
        String val = suggestion != null ? suggestion.getValue() : null;
        if (val == null) {
            announcedValue = null;
            AndroidUtilities.cancelRunOnUIThread(announce);
        } else {
            if (val.equals(announcedValue)) {
                return;
            }
            AndroidUtilities.cancelRunOnUIThread(announce);
            AndroidUtilities.runOnUIThread(announce, 600L);
        }
    }

    private MathOptions getOptions() {
        Locale current = LocaleController.getInstance().getCurrentLocale();
        if (current == null) {
            current = Locale.US;
        }
        if (!current.equals(optionsLocale)) {
            optionsLocale = current;
            options = new MathOptions(DecimalFormatSymbols.getInstance(current).getDecimalSeparator());
        }
        return options;
    }

    private void update() {
        if (!dirty) {
            return;
        }
        dirty = false;
        suggestion = null;
        ghost.clear();

        if (!HundredGramConfig.inlineMath || reveal.isRunning() || suppressedAt >= 0 || !view.isFocused() || !view.isEnabled()) {
            return;
        }
        Layout layout = view.getLayout();
        CharSequence text = view.getText();
        if (layout == null || text == null) {
            return;
        }
        int sel = view.getSelectionStart();
        if (sel <= 0 || sel != view.getSelectionEnd() || sel > text.length()) {
            return;
        }
        MathSuggestion found = MathExpression.INSTANCE.suggestionAt(text, sel, getOptions());
        if (found == null) {
            return;
        }
        if (text instanceof Spannable && BaseInputConnection.getComposingSpanStart((Spannable) text) != -1) {
            return;
        }
        if (text instanceof Spanned) {
            if (((Spanned) text).getSpans(sel - 1, sel, MetricAffectingSpan.class).length > 0) {
                return;
            }
        }
        if (layout.getParagraphDirection(layout.getLineForOffset(sel)) != 1) {
            return;
        }
        int lineStart = 0;
        for (int i = sel - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                lineStart = i + 1;
                break;
            }
        }
        int lineEnd = text.length();
        for (int i = sel; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineEnd = i;
                break;
            }
        }
        boolean atEnd = lineEnd >= text.length();
        if (hasUnsupportedSpans(text, lineStart, lineEnd)) {
            if (atEnd && ghost.buildDetached(view, layout, lineEnd, found.getInsertText())) {
                suggestion = found;
            }
            return;
        }
        if (ghost.build(view, layout, lineStart, lineEnd, sel, found.getInsertText())) {
            if (((ghost.getExtraHeight() > 0 && !atEnd) || (ghost.getMovedText() && caretMovedByTouch)) && (!atEnd || !ghost.buildDetached(view, layout, lineEnd, found.getInsertText()))) {
                ghost.clear();
            } else {
                suggestion = found;
            }
        }
    }

    public boolean onKeyEvent(KeyEvent event) {
        int action = event.getAction();
        if (action == KeyEvent.ACTION_UP) {
            if (swallowKeyCode != 0 && event.getKeyCode() == swallowKeyCode) {
                swallowKeyCode = 0;
                return true;
            }
            return false;
        }
        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0 && !event.isCtrlPressed() && !event.isAltPressed()) {
            if (event.isShiftPressed()) {
                return false;
            }
            int code = event.getKeyCode();
            boolean handled = false;
            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_DEL) {
                handled = undo();
            } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_TAB || code == KeyEvent.KEYCODE_SPACE) {
                handled = commit();
            }
            if (handled) {
                swallowKeyCode = event.getKeyCode();
                return true;
            }
        }
        return false;
    }

    public InputConnection wrap(final InputConnection connection) {
        if (connection == null) {
            return null;
        }
        return new InputConnectionWrapper(connection, true) {
            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (text != null && text.length() == 1 && text.charAt(0) == ' ') {
                    if (suggestion != null) {
                        finishComposingText();
                        if (commit()) {
                            return true;
                        }
                    }
                }
                return super.commitText(text, newCursorPosition);
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                if (beforeLength == 1 && afterLength == 0) {
                    if (undo()) {
                        return true;
                    }
                }
                return super.deleteSurroundingText(beforeLength, afterLength);
            }

            @Override
            public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
                if (beforeLength == 1 && afterLength == 0) {
                    if (undo()) {
                        return true;
                    }
                }
                return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
            }
        };
    }

    public boolean commit() {
        final MathSuggestion s = this.suggestion;
        if (s == null) {
            return false;
        }
        CharSequence text = view.getText();
        if (!(text instanceof Editable)) {
            return false;
        }
        final Editable editable = (Editable) text;
        if (s.getInsertAt() != view.getSelectionStart() || view.getSelectionStart() != view.getSelectionEnd() || s.getInsertAt() > editable.length()) {
            return false;
        }
        int prefixLen = s.getInsertText().length() - s.getValue().length();
        int valLen = s.getValue().length();
        float[] x = new float[valLen];
        float[] y = new float[valLen];
        ghost.readInsertedPositions(prefixLen, valLen, x, y);
        BaseInputConnection.removeComposingSpans(editable);

        boolean[] success = new boolean[]{true};
        edit(() -> {
            try {
                editable.insert(s.getInsertAt(), s.getInsertText());
            } catch (Exception e) {
                FileLog.e(e, false);
                success[0] = false;
            }
        });
        if (!success[0]) {
            return false;
        }

        Selection.setSelection(editable, s.getInsertAt() + s.getInsertText().length());
        this.suggestion = null;
        this.ghost.clear();
        this.dirty = true;
        this.reveal.begin(editable, s.getInsertAt() + prefixLen, valLen, x, y);
        this.undoStart = s.getInsertAt();
        this.undoEnd = s.getInsertAt() + s.getInsertText().length();
        this.appear.set(0.0f, true);
        this.view.requestLayout();
        this.view.invalidate();
        return true;
    }

    public boolean undo() {
        if (undoStart < 0) {
            return false;
        }
        CharSequence text = view.getText();
        if (!(text instanceof Editable)) {
            return false;
        }
        final Editable editable = (Editable) text;
        if (undoEnd > editable.length() || view.getSelectionStart() != undoEnd || view.getSelectionEnd() != undoEnd) {
            clearUndo();
            return false;
        }
        reveal.cancel();
        final int start = undoStart;
        boolean[] success = new boolean[]{true};
        edit(() -> {
            try {
                editable.delete(undoStart, undoEnd);
            } catch (Exception e) {
                FileLog.e(e, false);
                success[0] = false;
            }
        });
        if (!success[0]) {
            return false;
        }
        Selection.setSelection(editable, start);
        clearUndo();
        this.suppressedAt = start;
        this.dirty = true;
        this.view.requestLayout();
        this.view.invalidate();
        return true;
    }

    private void edit(Runnable block) {
        insertingSelf = true;
        try {
            if (delegate != null) {
                delegate.runProgrammatic(block);
            } else {
                block.run();
            }
        } finally {
            insertingSelf = false;
        }
    }

    private boolean hasUnsupportedSpans(CharSequence text, int start, int end) {
        if (!(text instanceof Spanned)) {
            return false;
        }
        Spanned spanned = (Spanned) text;
        if (spanned.getSpans(start, end, AnimatedEmojiSpan.class).length > 0) {
            return true;
        }
        if (spanned.getSpans(start, end, QuoteSpan.class).length > 0) {
            return true;
        }
        TextStyleSpan[] spans = spanned.getSpans(start, end, TextStyleSpan.class);
        for (TextStyleSpan span : spans) {
            if (span.isSpoiler()) {
                return true;
            }
        }
        return false;
    }

    private void clearUndo() {
        undoStart = -1;
        undoEnd = -1;
    }
}
