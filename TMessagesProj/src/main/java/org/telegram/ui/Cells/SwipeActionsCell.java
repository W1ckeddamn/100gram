package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.chats.SwipeAction;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackgroundGradientDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.SwipeActionsHelper;

public class SwipeActionsCell extends FrameLayout {
    private final List<SwipeAction> actions = new ArrayList<>();
    private Drawable backgroundDrawable;
    private BackgroundGradientDrawable.Disposable backgroundGradientDisposable;
    private ValueAnimator cycle;
    private final SwipeActionsHelper helper;
    private boolean looped;
    private final ChatMessageCell messageCell;
    private final Drawable monetBackgroundDrawable;
    private List<SwipeAction> pending;
    private int selected;
    private final Drawable shadowDrawable;
    private float slide;

    @Override
    public void dispatchSetPressed(boolean pressed) {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public SwipeActionsCell(Context context) {
        super(context);
        setWillNotDraw(false);
        this.monetBackgroundDrawable = new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray));
        this.shadowDrawable = Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow);
        this.helper = new SwipeActionsHelper(this, null);

        TLRPC.TL_message tL_message = new TLRPC.TL_message();
        tL_message.message = LocaleController.getString("SwipeActionsPreviewMessage", R.string.SwipeActionsPreviewMessage);
        tL_message.date = ((int) (System.currentTimeMillis() / 1000)) - 3600;
        tL_message.dialog_id = 1L;
        tL_message.flags = 256;
        TLRPC.TL_peerUser tL_peerUser = new TLRPC.TL_peerUser();
        tL_message.from_id = tL_peerUser;
        tL_peerUser.user_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        tL_message.id = 1;
        tL_message.out = false;
        TLRPC.TL_peerUser tL_peerUser2 = new TLRPC.TL_peerUser();
        tL_message.peer_id = tL_peerUser2;
        tL_peerUser2.user_id = 0L;

        TLRPC.TL_message tL_message2 = new TLRPC.TL_message();
        tL_message2.message = LocaleController.getString("SwipeActionsPreviewReply", R.string.SwipeActionsPreviewReply);
        tL_message2.date = tL_message.date;
        tL_message2.dialog_id = 1L;
        tL_message2.flags = 256;
        TLRPC.TL_peerUser tL_peerUser3 = new TLRPC.TL_peerUser();
        tL_message2.from_id = tL_peerUser3;
        tL_peerUser3.user_id = tL_message.from_id.user_id;
        tL_message2.id = 2;
        tL_message2.media = new TLRPC.TL_messageMediaEmpty();
        tL_message2.out = true;
        TLRPC.TL_peerUser tL_peerUser4 = new TLRPC.TL_peerUser();
        tL_message2.peer_id = tL_peerUser4;
        tL_peerUser4.user_id = 1L;

        TLRPC.TL_messageReplyHeader tL_messageReplyHeader = new TLRPC.TL_messageReplyHeader();
        tL_message.reply_to = tL_messageReplyHeader;
        tL_messageReplyHeader.flags |= 16;
        tL_messageReplyHeader.reply_to_msg_id = tL_message2.id;
        tL_message.flags |= 8;

        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, tL_message, true, false);
        messageObject.replyMessageObject = new MessageObject(UserConfig.selectedAccount, tL_message2, true, false);
        messageObject.customReplyName = LocaleController.getString("FromYou", R.string.FromYou);
        messageObject.resetLayout();
        messageObject.eventId = 1L;

        ChatMessageCell cell = new ChatMessageCell(context, UserConfig.selectedAccount);
        this.messageCell = cell;
        cell.isChat = false;
        cell.setFullyDraw(true);
        cell.setMessageObject(messageObject, null, false, false, false);
        addView(cell, LayoutHelper.createFrame(-1, -2, 16));
        updateActions();
    }

    public void updateActions() {
        List<SwipeAction> enabled = SwipeAction.enabled();
        if (enabled.equals(this.actions) && this.looped == HundredGramConfig.getSwipeActionsLoop()) {
            return;
        }
        if (this.cycle == null || this.actions.isEmpty() || enabled.isEmpty()) {
            applyActions(enabled);
        } else {
            this.pending = enabled;
        }
    }

    private void applyActions(List<SwipeAction> list) {
        this.actions.clear();
        this.actions.addAll(list);
        this.looped = HundredGramConfig.getSwipeActionsLoop();
        this.selected = 0;
        this.helper.start(this.actions);
        this.helper.select(0);
        if (this.actions.contains(SwipeAction.REACTION)) {
            String quickReactionEmoticon = SwipeAction.quickReactionEmoticon(UserConfig.selectedAccount);
            if (quickReactionEmoticon != null) {
                ReactionsLayoutInBubble.VisibleReaction fromEmojicon = ReactionsLayoutInBubble.VisibleReaction.fromEmojicon(quickReactionEmoticon);
                this.helper.setReaction(UserConfig.selectedAccount, fromEmojicon.emojicon, fromEmojicon.documentId);
            }
        }
        restartCycle();
    }

    private void restartCycle() {
        if (this.cycle != null) {
            this.cycle.cancel();
            this.cycle = null;
        }
        this.slide = 0.0f;
        this.messageCell.setSlidingOffset(0.0f);
        if (this.actions.isEmpty() || !isAttachedToWindow()) {
            invalidate();
            return;
        }
        int count = this.actions.size() + (this.looped && this.actions.size() > 1 ? 1 : 0);
        final int steps = count;
        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.cycle = animator;
        animator.setDuration((steps + 2) * 1100L);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(valueAnimator -> {
            float progress = (Float) valueAnimator.getAnimatedValue();
            float f = 1.0f / (steps + 2);
            if (progress < f) {
                this.slide = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(progress / f);
            } else {
                float f2 = 1.0f - f;
                if (progress > f2) {
                    this.slide = 1.0f - CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation((progress - f2) / f);
                } else {
                    this.slide = 1.0f;
                }
            }
            if (progress >= f && progress <= 1.0f - f && !this.actions.isEmpty()) {
                int index = Math.max(0, Math.min(steps - 1, (int) ((progress - f) / f))) % this.actions.size();
                if (index != this.selected) {
                    this.selected = index;
                    this.helper.select(index);
                }
            }
            this.messageCell.setSlidingOffset((-this.slide) * AndroidUtilities.dp(72.0f));
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                if (SwipeActionsCell.this.pending != null) {
                    final List<SwipeAction> next = SwipeActionsCell.this.pending;
                    SwipeActionsCell.this.pending = null;
                    AndroidUtilities.runOnUIThread(() -> SwipeActionsCell.this.applyActions(next));
                } else {
                    SwipeActionsCell.this.selected = 0;
                    SwipeActionsCell.this.helper.start(SwipeActionsCell.this.actions);
                    SwipeActionsCell.this.helper.select(0);
                }
            }
        });
        animator.start();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.actions.isEmpty()) {
            return;
        }
        float dp = (-this.slide) * AndroidUtilities.dp(72.0f);
        this.helper.draw(canvas, dp, false, this.messageCell.getTop() + (this.messageCell.getMeasuredHeight() / 2.0f), this.messageCell.getBackgroundDrawableRight() + dp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable cachedWallpaperNonBlocking = Theme.getCachedWallpaperNonBlocking();
        if (cachedWallpaperNonBlocking != this.backgroundDrawable && cachedWallpaperNonBlocking != null) {
            if (this.backgroundGradientDisposable != null) {
                this.backgroundGradientDisposable.dispose();
                this.backgroundGradientDisposable = null;
            }
            this.backgroundDrawable = cachedWallpaperNonBlocking;
        }
        Drawable drawable = this.backgroundDrawable;
        if (drawable != null) {
            drawable.setAlpha(drawable == this.monetBackgroundDrawable ? 150 : 255);
            drawWallpaper(canvas, this.backgroundDrawable);
        }
        if (this.shadowDrawable != null) {
            this.shadowDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            this.shadowDrawable.draw(canvas);
        }
    }

    private void drawWallpaper(Canvas canvas, Drawable drawable) {
        if ((drawable instanceof ColorDrawable) || (drawable instanceof GradientDrawable) || (drawable instanceof MotionBackgroundDrawable)) {
            drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            if (drawable instanceof BackgroundGradientDrawable) {
                this.backgroundGradientDisposable = ((BackgroundGradientDrawable) drawable).drawExactBoundsSize(canvas, this);
            } else {
                drawable.draw(canvas);
            }
            return;
        }
        if (drawable instanceof BitmapDrawable) {
            canvas.save();
            if (((BitmapDrawable) drawable).getTileModeX() == Shader.TileMode.REPEAT) {
                float f = 2.0f / AndroidUtilities.density;
                canvas.scale(f, f);
                drawable.setBounds(0, 0, (int) Math.ceil(getMeasuredWidth() / f), (int) Math.ceil(getMeasuredHeight() / f));
            } else {
                float max = Math.max((float) getMeasuredWidth() / drawable.getIntrinsicWidth(), (float) getMeasuredHeight() / drawable.getIntrinsicHeight());
                int ceil = (int) Math.ceil(drawable.getIntrinsicWidth() * max);
                int ceil2 = (int) Math.ceil(drawable.getIntrinsicHeight() * max);
                canvas.clipRect(0, 0, ceil, getMeasuredHeight());
                drawable.setBounds((getMeasuredWidth() - ceil) / 2, (getMeasuredHeight() - ceil2) / 2, ((getMeasuredWidth() - ceil) / 2) + ceil, ((getMeasuredHeight() - ceil2) / 2) + ceil2);
            }
            drawable.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(150.0f), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.cycle == null) {
            restartCycle();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.cycle != null) {
            this.cycle.cancel();
            this.cycle = null;
        }
        this.helper.detach();
        if (this.backgroundGradientDisposable != null) {
            this.backgroundGradientDisposable.dispose();
            this.backgroundGradientDisposable = null;
        }
    }
}
