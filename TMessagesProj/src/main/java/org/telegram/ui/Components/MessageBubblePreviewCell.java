package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class MessageBubblePreviewCell extends FrameLayout {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outBubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF inBubbleRect = new RectF();
    private final RectF outBubbleRect = new RectF();
    private final Path inTailPath = new Path();
    private final Path outTailPath = new Path();

    private final View previewArea;

    public MessageBubblePreviewCell(Context context) {
        super(context);
        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        textPaint.setTextSize(AndroidUtilities.dp(14.5f));
        timePaint.setTextSize(AndroidUtilities.dp(11f));

        previewArea = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                drawBubbles(canvas);
            }
        };
        previewArea.setWillNotDraw(false);
        addView(previewArea, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 150, 48, 16, 10, 16, 10));
    }

    public void update() {
        if (previewArea != null) {
            previewArea.invalidate();
        }
        invalidate();
    }

    private void drawBubbles(Canvas canvas) {
        int width = previewArea.getWidth();
        int height = previewArea.getHeight();
        if (width <= 0 || height <= 0) return;

        boolean isDark = Theme.isCurrentThemeDark();

        // Outer container background
        int containerBg = isDark ? 0xFF17212B : 0xFFE4ECF4;
        bgPaint.setColor(containerBg);
        RectF containerRect = AndroidUtilities.rectTmp;
        containerRect.set(0, 0, width, height);
        canvas.drawRoundRect(containerRect, AndroidUtilities.dp(14), AndroidUtilities.dp(14), bgPaint);

        boolean hideTail = HundredGramConfig.removeMessageTail;
        boolean replaceEdited = HundredGramConfig.replaceEditedWithIcon;
        boolean showSeconds = HundredGramConfig.showSecondsInTime;
        boolean showForwards = HundredGramConfig.showForwardsCount;

        // In bubble colors & rect
        int inBg = isDark ? 0xFF242F3D : 0xFFFFFFFF;
        inBubblePaint.setColor(inBg);

        float inLeft = AndroidUtilities.dp(hideTail ? 14 : 20);
        float inTop = AndroidUtilities.dp(14);
        float inWidth = Math.min(width * 0.72f, AndroidUtilities.dp(240));
        float inHeight = AndroidUtilities.dp(52);
        inBubbleRect.set(inLeft, inTop, inLeft + inWidth, inTop + inHeight);

        float radius = AndroidUtilities.dp(14);
        canvas.drawRoundRect(inBubbleRect, radius, radius, inBubblePaint);

        // Draw In Tail if not hidden
        if (!hideTail) {
            tailPaint.setColor(inBg);
            inTailPath.reset();
            inTailPath.moveTo(inLeft, inTop + inHeight - AndroidUtilities.dp(14));
            inTailPath.lineTo(inLeft - AndroidUtilities.dp(8), inTop + inHeight);
            inTailPath.lineTo(inLeft + AndroidUtilities.dp(10), inTop + inHeight);
            inTailPath.close();
            canvas.drawPath(inTailPath, tailPaint);
        }

        // Draw In Text
        textPaint.setColor(isDark ? 0xFFFFFFFF : 0xFF000000);
        timePaint.setColor(isDark ? 0x99FFFFFF : 0x88000000);

        String inText = LocaleController.getString("BubblePreviewInText", R.string.BubblePreviewInText);
        canvas.drawText(inText, inLeft + AndroidUtilities.dp(12), inTop + AndroidUtilities.dp(24), textPaint);

        String inTime = showSeconds ? "12:45:15" : "12:45";
        float inTimeW = timePaint.measureText(inTime);
        float inRightEdge = inLeft + inWidth - AndroidUtilities.dp(12);

        if (showForwards) {
            String fwdCount = "14";
            float fwdW = timePaint.measureText(fwdCount);
            String bullet = " • ";
            float bulletW = timePaint.measureText(bullet);
            Drawable fwdIcon = ContextCompat.getDrawable(getContext(), R.drawable.msg_reply_small);
            int iconW = AndroidUtilities.dp(10);
            int iconH = AndroidUtilities.dp(9);
            float totalW = inTimeW + bulletW + iconW + AndroidUtilities.dp(3) + fwdW;
            float startX = inRightEdge - totalW;

            // Draw time
            canvas.drawText(inTime, startX, inTop + inHeight - AndroidUtilities.dp(8), timePaint);
            startX += inTimeW;

            // Draw bullet
            canvas.drawText(bullet, startX, inTop + inHeight - AndroidUtilities.dp(8), timePaint);
            startX += bulletW;

            // Draw forward icon (mirrored horizontally)
            if (fwdIcon != null) {
                fwdIcon = fwdIcon.mutate();
                fwdIcon.setColorFilter(new PorterDuffColorFilter(timePaint.getColor(), PorterDuff.Mode.SRC_IN));
                int iconLeft = (int) startX;
                int iconTop = (int) (inTop + inHeight - AndroidUtilities.dp(17));
                fwdIcon.setBounds(iconLeft, iconTop, iconLeft + iconW, iconTop + iconH);
                canvas.save();
                canvas.scale(-1.0f, 1.0f, iconLeft + iconW / 2.0f, 0.0f);
                fwdIcon.draw(canvas);
                canvas.restore();
            }
            startX += iconW + AndroidUtilities.dp(3);

            // Draw count
            canvas.drawText(fwdCount, startX, inTop + inHeight - AndroidUtilities.dp(8), timePaint);
        } else {
            canvas.drawText(inTime, inRightEdge - inTimeW, inTop + inHeight - AndroidUtilities.dp(8), timePaint);
        }

        // Out bubble colors & rect
        int outBg = isDark ? 0xFF2B5278 : 0xFFEEFFDE;
        outBubblePaint.setColor(outBg);

        float outWidth = Math.min(width * 0.76f, AndroidUtilities.dp(250));
        float outRight = width - AndroidUtilities.dp(hideTail ? 14 : 20);
        float outLeft = outRight - outWidth;
        float outTop = inTop + inHeight + AndroidUtilities.dp(10);
        float outHeight = AndroidUtilities.dp(52);
        outBubbleRect.set(outLeft, outTop, outRight, outTop + outHeight);

        canvas.drawRoundRect(outBubbleRect, radius, radius, outBubblePaint);

        // Draw Out Tail if not hidden
        if (!hideTail) {
            tailPaint.setColor(outBg);
            outTailPath.reset();
            outTailPath.moveTo(outRight, outTop + outHeight - AndroidUtilities.dp(14));
            outTailPath.lineTo(outRight + AndroidUtilities.dp(8), outTop + outHeight);
            outTailPath.lineTo(outRight - AndroidUtilities.dp(10), outTop + outHeight);
            outTailPath.close();
            canvas.drawPath(outTailPath, tailPaint);
        }

        // Draw Out Text
        int outTextColor = isDark ? 0xFFFFFFFF : 0xFF000000;
        textPaint.setColor(outTextColor);
        timePaint.setColor(isDark ? 0xAAFFFFFF : 0x88000000);

        String outText = hideTail ? LocaleController.getString("BubblePreviewOutFlat", R.string.BubblePreviewOutFlat) : LocaleController.getString("BubblePreviewOutText", R.string.BubblePreviewOutText);
        canvas.drawText(outText, outLeft + AndroidUtilities.dp(12), outTop + AndroidUtilities.dp(24), textPaint);

        String outTime = showSeconds ? "12:45:32" : "12:45";
        float timeW = timePaint.measureText(outTime);
        float metaRight = outRight - AndroidUtilities.dp(12);

        // Checkmark status (read)
        Drawable checkmark = ContextCompat.getDrawable(getContext(), R.drawable.msg_check_s);
        if (checkmark != null) {
            int checkW = AndroidUtilities.dp(12);
            int checkH = AndroidUtilities.dp(10);
            checkmark.setBounds((int) (metaRight - checkW), (int) (outTop + outHeight - AndroidUtilities.dp(18)), (int) metaRight, (int) (outTop + outHeight - AndroidUtilities.dp(8)));
            checkmark.setTint(isDark ? 0xFF64B5F6 : 0xFF4CAF50);
            checkmark.draw(canvas);
            metaRight -= checkW + AndroidUtilities.dp(4);
        }

        // Draw time
        canvas.drawText(outTime, metaRight - timeW, outTop + outHeight - AndroidUtilities.dp(8), timePaint);
        metaRight -= timeW + AndroidUtilities.dp(6);

        // Draw edited mark (icon or text)
        if (replaceEdited) {
            Drawable pencil = ContextCompat.getDrawable(getContext(), R.drawable.msg_edit);
            if (pencil != null) {
                int pencilSize = AndroidUtilities.dp(11);
                pencil.setBounds((int) (metaRight - pencilSize), (int) (outTop + outHeight - AndroidUtilities.dp(18)), (int) metaRight, (int) (outTop + outHeight - AndroidUtilities.dp(7)));
                pencil.setTint(isDark ? 0xAAFFFFFF : 0x88000000);
                pencil.draw(canvas);
            }
        } else {
            String editedText = LocaleController.getString("BubblePreviewEdited", R.string.BubblePreviewEdited);
            canvas.drawText(editedText, metaRight - timePaint.measureText(editedText), outTop + outHeight - AndroidUtilities.dp(8), timePaint);
        }
    }
}
