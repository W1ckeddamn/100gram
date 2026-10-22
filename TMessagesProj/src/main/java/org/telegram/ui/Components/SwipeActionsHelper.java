package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.chats.SwipeAction;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

public class SwipeActionsHelper {
    private float anchorY;
    private AnimatedEmojiDrawable animatedReaction;
    private long animatedReactionId;
    private boolean armed;
    private boolean beyondMax;
    private int direction;
    private float driftX;
    private float driftY;
    private final FloatValueHolder fill;
    private final SpringAnimation fillSpring;
    private boolean frameDark;
    private boolean frameGradient;
    private Paint framePaint;
    private ColorFilter iconFilter;
    private int iconFilterColor;
    private float lastX;
    private float lastY;
    private final AnimatedFloat liveAlpha;
    private boolean looped;
    private final Paint outlineDarkenPaint;
    private final Paint outlinePaint;
    private final View parent;
    private final Path path;
    private final FloatValueHolder position;
    private final SpringAnimation positionSpring;
    private Drawable reactionDrawable;
    private String reactionEmojicon;
    private ImageReceiver reactionImage;
    private final RectF rect;
    private final Theme.ResourcesProvider resourcesProvider;
    private boolean reversed;
    private final FloatValueHolder ring;
    private final SpringAnimation ringSpring;
    private boolean scrubbing;
    private int selected;
    private int slot;
    private boolean stepped;
    private final FloatValueHolder visibility;
    private final SpringAnimation visibilitySpring;
    private final List<SwipeAction> actions = new ArrayList<>();
    private final SparseArray<Drawable> icons = new SparseArray<>();
    private final SparseArray<Drawable> spareIcons = new SparseArray<>();
    private final SparseArray<Float> iconFits = new SparseArray<>();

    public SwipeActionsHelper(View view, Theme.ResourcesProvider resourcesProvider) {
        Paint paint = new Paint(1);
        this.outlinePaint = paint;
        Paint paint2 = new Paint(1);
        this.outlineDarkenPaint = paint2;
        this.path = new Path();
        this.rect = new RectF();
        this.liveAlpha = new AnimatedFloat(this::invalidate, 0L, 280L, CubicBezierInterpolator.EASE_OUT);
        FloatValueHolder floatValueHolder = new FloatValueHolder(0.0f);
        this.position = floatValueHolder;
        this.positionSpring = new SpringAnimation(floatValueHolder).setSpring(new SpringForce(0.0f).setStiffness(430.0f).setDampingRatio(0.55f)).addUpdateListener((dynamicAnimation, f, f2) -> invalidate());
        FloatValueHolder floatValueHolder2 = new FloatValueHolder(0.0f);
        this.visibility = floatValueHolder2;
        this.visibilitySpring = new SpringAnimation(floatValueHolder2).setMinValue(0.0f).setMaxValue(2000.0f).setSpring(new SpringForce(0.0f).setStiffness(1500.0f).setDampingRatio(1.0f)).addUpdateListener((dynamicAnimation, f, f2) -> invalidate());
        FloatValueHolder floatValueHolder3 = new FloatValueHolder(0.0f);
        this.fill = floatValueHolder3;
        this.fillSpring = new SpringAnimation(floatValueHolder3).setMinValue(0.0f).setSpring(new SpringForce(0.0f).setStiffness(400.0f).setDampingRatio(0.5f)).addUpdateListener((dynamicAnimation, f, f2) -> invalidate());
        FloatValueHolder floatValueHolder4 = new FloatValueHolder(0.0f);
        this.ring = floatValueHolder4;
        this.ringSpring = new SpringAnimation(floatValueHolder4).setMinValue(0.0f).setSpring(new SpringForce(0.0f).setStiffness(200.0f).setDampingRatio(1.0f)).addUpdateListener((dynamicAnimation, f, f2) -> invalidate());
        this.parent = view;
        this.resourcesProvider = resourcesProvider;
        Paint.Style style = Paint.Style.STROKE;
        paint.setStyle(style);
        Paint.Cap cap = Paint.Cap.ROUND;
        paint.setStrokeCap(cap);
        paint.setStrokeWidth(AndroidUtilities.dp(2.0f));
        paint2.setStyle(style);
        paint2.setStrokeCap(cap);
        paint2.setStrokeWidth(AndroidUtilities.dp(2.0f));
    }

    private void invalidate() {
        this.parent.invalidate();
    }

    public void setReaction(int account, String str, long documentId) {
        if (documentId != 0) {
            if (this.animatedReactionId != documentId) {
                detach();
                AnimatedEmojiDrawable make = AnimatedEmojiDrawable.make(account, 0, documentId);
                this.animatedReaction = make;
                make.addView(this.parent);
                this.animatedReactionId = documentId;
            }
            this.reactionDrawable = this.animatedReaction;
            return;
        }
        TLRPC.TL_availableReaction tL_availableReaction = str != null ? MediaDataController.getInstance(account).getReactionsMap().get(str) : null;
        if (tL_availableReaction != null && tL_availableReaction.center_icon != null) {
            if (this.reactionImage == null || !TextUtils.equals(this.reactionEmojicon, str)) {
                detach();
                ImageReceiver imageReceiver = new ImageReceiver(this.parent);
                this.reactionImage = imageReceiver;
                imageReceiver.ignoreNotifications = true;
                imageReceiver.onAttachedToWindow();
                this.reactionImage.setImage(ImageLocation.getForDocument(tL_availableReaction.center_icon), "40_40_lastreactframe", DocumentObject.getSvgThumb(tL_availableReaction.static_icon, Theme.key_windowBackgroundGray, 1.0f), "webp", tL_availableReaction, 1);
                this.reactionEmojicon = str;
                return;
            }
            return;
        }
        detach();
        this.reactionDrawable = str != null ? Emoji.getEmojiDrawable(str) : null;
    }

    public void detach() {
        AnimatedEmojiDrawable animatedEmojiDrawable = this.animatedReaction;
        if (animatedEmojiDrawable != null) {
            animatedEmojiDrawable.removeView(this.parent);
            this.animatedReaction = null;
            this.animatedReactionId = 0L;
        }
        ImageReceiver imageReceiver = this.reactionImage;
        if (imageReceiver != null) {
            imageReceiver.onDetachedFromWindow();
            this.reactionImage = null;
            this.reactionEmojicon = null;
        }
        this.reactionDrawable = null;
    }

    public void start(List<SwipeAction> list) {
        this.actions.clear();
        this.actions.addAll(list);
        this.looped = HundredGramConfig.getSwipeActionsLoop();
        this.reversed = HundredGramConfig.getSwipeActionsReversed();
        this.selected = 0;
        this.slot = 0;
        this.armed = false;
        this.stepped = false;
        this.scrubbing = false;
        this.liveAlpha.set(0.0f, true);
        this.anchorY = 0.0f;
        this.driftX = 0.0f;
        this.driftY = 0.0f;
        this.direction = 0;
        this.positionSpring.cancel();
        this.positionSpring.getSpring().setFinalPosition(0.0f);
        this.position.setValue(0.0f);
        this.visibilitySpring.cancel();
        this.visibilitySpring.getSpring().setFinalPosition(0.0f);
        this.visibility.setValue(0.0f);
        this.fillSpring.cancel();
        this.fillSpring.getSpring().setFinalPosition(0.0f);
        this.fill.setValue(0.0f);
        this.ringSpring.cancel();
        this.ringSpring.getSpring().setFinalPosition(0.0f);
        this.ring.setValue(0.0f);
        this.beyondMax = false;
    }

    public int size() {
        return this.actions.size();
    }

    public SwipeAction getSelected() {
        int i = this.selected;
        if (i < 0 || i >= this.actions.size()) {
            return null;
        }
        return this.actions.get(this.selected);
    }

    public void update(float dx, float touchX, float touchY) {
        if (this.actions.size() < 2) {
            return;
        }
        if (Math.abs(dx) < AndroidUtilities.dp(50.0f)) {
            this.armed = false;
            this.scrubbing = false;
            return;
        }
        if (!this.armed) {
            this.armed = true;
            this.lastX = touchX;
            this.lastY = touchY;
            this.driftX = 0.0f;
            this.driftY = 0.0f;
            return;
        }
        float absX = Math.abs(touchX - this.lastX);
        float deltaY = touchY - this.lastY;
        this.lastX = touchX;
        this.lastY = touchY;
        if (!this.scrubbing) {
            this.driftX = (this.driftX * 0.7f) + (absX * 0.3f);
            float absY = (this.driftY * 0.7f) + (Math.abs(deltaY) * 0.3f);
            this.driftY = absY;
            if (absY < AndroidUtilities.dp(0.5f) || this.driftY <= this.driftX * 1.5f) {
                return;
            }
            this.scrubbing = true;
            this.anchorY = touchY;
            return;
        }
        float f5 = (touchY - this.anchorY) * (this.direction < 0 ? -1 : 1) * (this.reversed ? -1 : 1);
        float dp = AndroidUtilities.dp(this.stepped ? 36.0f : 44.0f);
        if (f5 >= dp) {
            this.anchorY = touchY;
            this.stepped = true;
            if (this.looped || this.selected < this.actions.size() - 1) {
                move(1);
                return;
            }
            return;
        }
        if (f5 <= (-dp)) {
            this.anchorY = touchY;
            this.stepped = true;
            if (this.looped || this.selected > 0) {
                move(-1);
            }
        }
    }

    private int chooseDirection(float cy) {
        float dp = AndroidUtilities.dp(44.0f) + ((this.actions.size() - 2) * AndroidUtilities.dp(36.0f));
        float height = this.parent.getHeight() - cy;
        return (height >= dp || height >= cy) ? 1 : -1;
    }

    public void select(int i) {
        this.scrubbing = true;
        if (i == this.selected || i < 0 || i >= this.actions.size()) {
            return;
        }
        int i2 = i - this.selected;
        if (this.looped && (i2 = Math.floorMod(i2, this.actions.size())) > this.actions.size() / 2) {
            i2 -= this.actions.size();
        }
        this.selected = i;
        this.slot += i2;
        this.positionSpring.getSpring().setFinalPosition(this.slot * 2000.0f);
        this.positionSpring.start();
        invalidate();
    }

    public boolean remove(SwipeAction swipeAction) {
        int indexOf;
        boolean z = false;
        if (!this.scrubbing && this.actions.size() >= 2 && (indexOf = this.actions.indexOf(swipeAction)) >= 0 && (indexOf != this.selected || this.visibility.getValue() == 0.0f)) {
            this.actions.remove(indexOf);
            z = true;
            if (this.selected >= this.actions.size()) {
                int size = this.actions.size() - 1;
                this.selected = size;
                this.slot = size;
                this.positionSpring.cancel();
                this.positionSpring.getSpring().setFinalPosition(this.selected * 2000.0f);
                this.position.setValue(this.selected * 2000.0f);
            }
            invalidate();
        }
        return z;
    }

    private void move(int i) {
        int i2 = this.slot + i;
        this.slot = i2;
        this.selected = Math.floorMod(i2, this.actions.size());
        try {
            this.parent.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } catch (Exception unused) {
        }
        this.positionSpring.getSpring().setFinalPosition(this.slot * 2000.0f);
        this.positionSpring.start();
        invalidate();
    }

    public void draw(Canvas canvas, float translationX, boolean isOut, float cy, float maxRight) {
        if (this.actions.isEmpty() || Thread.currentThread() != Looper.getMainLooper().getThread()) {
            return;
        }
        if (this.direction == 0) {
            this.direction = chooseDirection(cy);
        }
        Paint themePaint = Theme.getThemePaint("paintChatActionBackground", this.resourcesProvider);
        if (themePaint == null) {
            themePaint = Theme.chat_actionBackgroundPaint;
        }
        Paint paint = Theme.chat_actionBackgroundGradientDarkenPaint;
        if (this.outlinePaint.getColor() != themePaint.getColor()) {
            this.outlinePaint.setColor(themePaint.getColor());
        }
        if (this.outlineDarkenPaint.getColor() != paint.getColor()) {
            this.outlineDarkenPaint.setColor(paint.getColor());
        }
        if (this.outlinePaint.getShader() != themePaint.getShader()) {
            this.outlinePaint.setShader(themePaint.getShader());
        }
        if (this.outlineDarkenPaint.getShader() != paint.getShader()) {
            this.outlineDarkenPaint.setShader(paint.getShader());
        }
        this.framePaint = themePaint;
        Theme.ResourcesProvider resourcesProvider = this.resourcesProvider;
        this.frameGradient = resourcesProvider != null && resourcesProvider.hasGradientService();
        this.frameDark = ColorUtils.calculateLuminance(Theme.getColor(Theme.key_windowBackgroundWhite, this.resourcesProvider)) <= 0.5d;
        int color = this.outlineDarkenPaint.getColor();
        float value = this.fill.getValue() / 2000.0f;
        if (value > 1.0f) {
            this.beyondMax = true;
        }
        if (this.visibility.getValue() == 0.0f) {
            this.fillSpring.cancel();
            this.fillSpring.getSpring().setFinalPosition(0.0f);
            this.fill.setValue(0.0f);
            this.ringSpring.cancel();
            this.ringSpring.getSpring().setFinalPosition(0.0f);
            this.ring.setValue(0.0f);
            this.beyondMax = false;
        }
        boolean z2 = this.fillSpring.getSpring().getFinalPosition() == 2000.0f;
        float clamp = z2 ? 1.0f : MathUtils.clamp(((-translationX) - AndroidUtilities.dp(20.0f)) / AndroidUtilities.dp(30.0f), 0.0f, 1.0f);
        if (clamp == 1.0f && !z2) {
            this.fillSpring.getSpring().setFinalPosition(2000.0f);
            this.fillSpring.start();
            this.ringSpring.getSpring().setFinalPosition(2000.0f);
            this.ringSpring.start();
        }
        float f9 = translationX <= ((float) (-AndroidUtilities.dp(20.0f))) ? 2000.0f : 0.0f;
        if (f9 != this.visibilitySpring.getSpring().getFinalPosition()) {
            this.visibilitySpring.getSpring().setFinalPosition(f9);
            if (!this.visibilitySpring.isRunning()) {
                this.visibilitySpring.start();
            }
        }
        float value2 = this.visibility.getValue() / 2000.0f;
        float measuredWidth = this.parent.getMeasuredWidth();
        float cx = Math.max((translationX * (isOut ? 0.5f : 1.0f)) + measuredWidth, Math.min((maxRight + measuredWidth) / 2.0f, measuredWidth));
        boolean z3 = this.beyondMax;
        float f10 = z3 ? value : value2;
        float f11 = z3 ? 0.0f : 1.0f - value;
        float value3 = this.position.getValue() / 2000.0f;
        float dp = AndroidUtilities.dp(36.0f) * this.direction;
        float scrubbingAlpha = this.liveAlpha.set(this.scrubbing);
        int size = this.actions.size();
        int ceil = (int) Math.ceil(value3 - 2.0f);
        int floor = (int) Math.floor(value3 + 2.0f);
        if (!this.looped || size < 2) {
            ceil = Math.max(ceil, 0);
            floor = Math.min(floor, size - 1);
        }
        int i2 = floor;
        int i3 = this.slot;
        for (int i = ceil; i <= i2; i++) {
            if (i != i3) {
                float f13 = i - value3;
                drawBubble(canvas, cx, cy + (f13 * dp), f10, f11, value2, clamp, Math.abs(f13), scrubbingAlpha, i, false);
            }
        }
        drawBubble(canvas, cx, cy + ((i3 - value3) * dp), f10, f11, value2, clamp, Math.abs(i3 - value3), 1.0f, this.slot, true);
        this.outlineDarkenPaint.setColor(color);
        paint.setColor(color);
    }

    private void drawBubble(Canvas canvas, float cx, float cy, float f3, float f4, float f5, float progress, float distance, float scrubbingAlpha, int slot, boolean isCurrentSlot) {
        if (distance >= 2.0f) {
            return;
        }
        float max = Math.max(0.0f, 1.0f - distance);
        float max2 = Math.max(max, (2.0f - distance) * 0.45f);
        float interpolation = ((max * 0.3f) + 0.7f) * ((CubicBezierInterpolator.EASE_OUT_BACK.getInterpolation(scrubbingAlpha) * 0.15f) + 0.85f);
        float f12 = f3 * interpolation;
        float f13 = f4 * interpolation;
        float f18 = f5 * max2 * scrubbingAlpha;
        if (f18 <= 0.0f) {
            return;
        }
        Paint paint = this.framePaint;
        Paint paint2 = Theme.chat_actionBackgroundGradientDarkenPaint;
        boolean z2 = this.frameGradient;
        boolean z3 = this.frameDark;
        float dp = AndroidUtilities.dp(16.0f);

        if (isCurrentSlot && progress > 0.0f && this.fill.getValue() == 0.0f) {
            setRect(cx, cy, (dp * f12) - (this.outlinePaint.getStrokeWidth() / 2.0f));
            applyServiceShaderMatrix();
            int alpha = this.outlinePaint.getAlpha();
            this.outlinePaint.setAlpha((int) (alpha * f18));
            float sweep = 360.0f * progress;
            canvas.drawArc(this.rect, -90.0f, sweep, false, this.outlinePaint);
            this.outlinePaint.setAlpha(alpha);
            if (z2) {
                int alpha2 = this.outlineDarkenPaint.getAlpha();
                if (z3) {
                    this.outlineDarkenPaint.setColor(-1);
                }
                this.outlineDarkenPaint.setAlpha((int) (alpha2 * f18));
                canvas.drawArc(this.rect, -90.0f, sweep, false, this.outlineDarkenPaint);
            }
        }

        float r = dp * f12;
        setRect(cx, cy, r);
        applyServiceShaderMatrix();
        this.path.rewind();
        this.path.addRoundRect(this.rect, r, r, Path.Direction.CW);
        int alpha3 = paint.getAlpha();
        float f21 = 0.6f * f18 * progress;
        paint.setAlpha((int) (alpha3 * f21));
        canvas.drawPath(this.path, paint);
        paint.setAlpha(alpha3);
        if (z2) {
            int alpha4 = paint2.getAlpha();
            if (z3) {
                paint2.setColor(-1);
            }
            paint2.setAlpha((int) (f21 * alpha4));
            canvas.drawPath(this.path, paint2);
            paint2.setAlpha(alpha4);
        }

        if (f13 != 0.0f) {
            setRect(cx, cy, dp * f13);
            this.path.rewind();
            this.path.addRoundRect(this.rect, dp, dp, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(this.path, Region.Op.DIFFERENCE);
        }

        setRect(cx, cy, r);
        applyServiceShaderMatrix();
        this.path.rewind();
        this.path.addRoundRect(this.rect, r, r, Path.Direction.CW);
        int alpha5 = paint.getAlpha();
        float f22 = 0.4f * f18;
        paint.setAlpha((int) (alpha5 * f22));
        canvas.drawPath(this.path, paint);
        paint.setAlpha(alpha5);
        if (z2) {
            int alpha6 = paint2.getAlpha();
            if (z3) {
                paint2.setColor(-1);
            }
            paint2.setAlpha((int) (f22 * alpha6));
            canvas.drawPath(this.path, paint2);
            paint2.setAlpha(alpha6);
        }

        if (f13 != 0.0f) {
            canvas.restore();
        }

        float ringVal = this.ring.getValue() / 2000.0f;
        if (isCurrentSlot && ringVal != 0.0f && ringVal != 1.0f) {
            float f23 = ringVal + 1.0f;
            float strokeWidth = this.outlinePaint.getStrokeWidth();
            float f15 = (1.0f - ringVal) * strokeWidth;
            if (f15 != 0.0f) {
                float f24 = dp * f23;
                setRect(cx, cy, f24 - f15);
                applyServiceShaderMatrix();
                int alpha7 = this.outlinePaint.getAlpha();
                this.outlinePaint.setAlpha((int) (alpha7 * f18));
                this.outlinePaint.setStrokeWidth(f15);
                canvas.drawRoundRect(this.rect, f24, f24, this.outlinePaint);
                this.outlinePaint.setStrokeWidth(strokeWidth);
                this.outlinePaint.setAlpha(alpha7);
                if (z2) {
                    int alpha8 = this.outlineDarkenPaint.getAlpha();
                    if (z3) {
                        this.outlineDarkenPaint.setColor(-1);
                    }
                    this.outlineDarkenPaint.setAlpha((int) (alpha8 * f18));
                    this.outlineDarkenPaint.setStrokeWidth(f15);
                    canvas.drawRoundRect(this.rect, f24, f24, this.outlineDarkenPaint);
                    this.outlineDarkenPaint.setStrokeWidth(strokeWidth);
                }
            }
        }

        List<SwipeAction> list = this.actions;
        SwipeAction swipeAction = list.get(Math.floorMod(slot, list.size()));
        if (swipeAction == SwipeAction.REACTION && this.reactionImage != null) {
            float dp2 = (AndroidUtilities.dp(20.0f) / 2.0f) * f12;
            float f25 = cx - dp2;
            float f26 = cy - dp2;
            float f27 = dp2 * 2.0f;
            this.reactionImage.setImageCoords(f25, f26, f27, f27);
            this.reactionImage.setAlpha(f18);
            this.reactionImage.draw(canvas);
            return;
        }
        if (swipeAction == SwipeAction.REACTION && this.reactionDrawable != null) {
            float dp3 = (AndroidUtilities.dp(20.0f) / 2.0f) * f12;
            setRect(cx, cy, dp3);
            this.path.rewind();
            float f28 = 0.35f * dp3;
            this.path.addRoundRect(this.rect, f28, f28, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(this.path);
            this.reactionDrawable.setAlpha((int) (f18 * 255.0f));
            this.reactionDrawable.setBounds((int) (cx - dp3), (int) (cy - dp3), (int) (cx + dp3), (int) (dp3 + cy));
            this.reactionDrawable.draw(canvas);
            this.reactionDrawable.setAlpha(255);
            canvas.restore();
            return;
        }

        Drawable icon;
        boolean isSingleReply = this.actions.size() == 1 && swipeAction == SwipeAction.REPLY;
        if (isSingleReply) {
            icon = Theme.getThemeDrawable("drawableReplyIcon", this.resourcesProvider);
            if (icon == null) {
                icon = Theme.chat_replyIconDrawable;
            }
        } else {
            boolean spare = (Math.floorDiv(slot, this.actions.size()) & 1) != 0;
            icon = getIcon(swipeAction, spare);
        }
        if (icon == null) {
            return;
        }

        Float f29 = this.iconFits.get(swipeAction.iconRes);
        float scale;
        if (isSingleReply) {
            scale = f12;
        } else {
            scale = f12 * swipeAction.iconTrim * (f29 != null ? f29 : 1.0f);
        }
        float intrinsicWidth = (icon.getIntrinsicWidth() / 2.0f) * scale;
        float intrinsicHeight = (icon.getIntrinsicHeight() / 2.0f) * scale;
        icon.setColorFilter(getIconFilter());
        icon.setAlpha((int) (f18 * 255.0f));
        icon.setBounds((int) (cx - intrinsicWidth), (int) (cy - intrinsicHeight), (int) (intrinsicWidth + cx), (int) (intrinsicHeight + cy));
        icon.draw(canvas);
    }

    private void setRect(float cx, float cy, float radius) {
        this.rect.set(cx - radius, cy - radius, cx + radius, cy + radius);
    }

    private void applyServiceShaderMatrix() {
        int measuredWidth = this.parent.getMeasuredWidth();
        float y = this.parent.getY() + this.rect.top;
        Theme.ResourcesProvider resourcesProvider = this.resourcesProvider;
        if (resourcesProvider != null) {
            resourcesProvider.applyServiceShaderMatrix(measuredWidth, AndroidUtilities.displaySize.y, 0.0f, y);
        } else {
            Theme.applyServiceShaderMatrix(measuredWidth, AndroidUtilities.displaySize.y, 0.0f, y);
        }
    }

    private ColorFilter getIconFilter() {
        int color = Theme.getColor(Theme.key_chat_serviceIcon, this.resourcesProvider);
        if (this.iconFilter == null || this.iconFilterColor != color) {
            this.iconFilterColor = color;
            this.iconFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
        return this.iconFilter;
    }

    private float measureIconFit(Drawable drawable) {
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            return 1.0f;
        }
        Bitmap createBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
        drawable.draw(new Canvas(createBitmap));
        int[] iArr = new int[intrinsicWidth * intrinsicHeight];
        createBitmap.getPixels(iArr, 0, intrinsicWidth, 0, 0, intrinsicWidth, intrinsicHeight);
        createBitmap.recycle();
        int minX = intrinsicWidth;
        int maxX = -1;
        int minY = intrinsicHeight;
        int maxY = -1;
        for (int y = 0; y < intrinsicHeight; y++) {
            for (int x = 0; x < intrinsicWidth; x++) {
                if ((iArr[(y * intrinsicWidth) + x] >>> 24) >= 8) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return 1.0f;
        }
        return Math.min(AndroidUtilities.dpf2(15.0f) / ((maxX - minX) + 1), AndroidUtilities.dpf2(16.5f) / ((maxY - minY) + 1));
    }

    private Drawable getIcon(SwipeAction swipeAction, boolean spare) {
        SparseArray<Drawable> sparseArray = spare ? this.spareIcons : this.icons;
        Drawable drawable = sparseArray.get(swipeAction.iconRes);
        if (drawable != null) {
            return drawable;
        }
        Drawable drawable2 = this.parent.getContext().getDrawable(swipeAction.iconRes);
        if (drawable2 == null) {
            return null;
        }
        if (this.iconFits.get(swipeAction.iconRes) == null) {
            this.iconFits.put(swipeAction.iconRes, measureIconFit(drawable2));
        }
        Drawable mutate = drawable2.mutate();
        sparseArray.put(swipeAction.iconRes, mutate);
        return mutate;
    }
}
