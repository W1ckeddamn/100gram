package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class SwipeActionsPreviewCell extends FrameLayout {

    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubbleBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint actionCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bubbleRect = new RectF();

    private float swipeOffset = 0f;
    private float touchStartX = 0f;
    private boolean isSwiping = false;
    private boolean hapticTriggered = false;
    private ValueAnimator returnAnimator;

    private final LinearLayout chipsContainer;
    private final View previewArea;

    private static final int THRESHOLD_DP = 72;

    public interface OnSwipeActionChangedListener {
        void onActionChanged(int action);
    }

    private OnSwipeActionChangedListener listener;

    public SwipeActionsPreviewCell(Context context, OnSwipeActionChangedListener listener) {
        super(context);
        this.listener = listener;

        setWillNotDraw(false);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        textPaint.setTextSize(AndroidUtilities.dp(14.5f));
        textPaint.setTypeface(AndroidUtilities.bold());

        subTextPaint.setTextSize(AndroidUtilities.dp(12f));

        bubbleBorderPaint.setStyle(Paint.Style.STROKE);
        bubbleBorderPaint.setStrokeWidth(AndroidUtilities.dp(1f));

        previewArea = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                drawSwipePreview(canvas);
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return handleSwipeTouch(event);
            }
        };
        previewArea.setWillNotDraw(false);
        addView(previewArea, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 96, 48, 16, 12, 16, 8));

        chipsContainer = new LinearLayout(context);
        chipsContainer.setOrientation(LinearLayout.HORIZONTAL);
        chipsContainer.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.addView(chipsContainer, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 16));
        addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 80, 0, 116, 0, 4));

        rebuildChips();
    }

    public void setOnSwipeActionChangedListener(OnSwipeActionChangedListener listener) {
        this.listener = listener;
    }

    public void update() {
        rebuildChips();
        if (previewArea != null) {
            previewArea.invalidate();
        }
    }

    private boolean handleSwipeTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (returnAnimator != null && returnAnimator.isRunning()) {
                    returnAnimator.cancel();
                }
                touchStartX = event.getX();
                isSwiping = true;
                hapticTriggered = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isSwiping) return false;
                float deltaX = event.getX() - touchStartX;
                if (deltaX < 0) {
                    // Damped swipe to left
                    float maxSwipe = AndroidUtilities.dp(140);
                    float absDelta = Math.abs(deltaX);
                    swipeOffset = -Math.min(maxSwipe, absDelta * 0.85f);

                    if (Math.abs(swipeOffset) >= AndroidUtilities.dp(THRESHOLD_DP) && !hapticTriggered) {
                        hapticTriggered = true;
                        try {
                            previewArea.performHapticFeedback(
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                            ? HapticFeedbackConstants.CONFIRM
                                            : HapticFeedbackConstants.KEYBOARD_TAP
                            );
                        } catch (Exception ignored) {}
                    } else if (Math.abs(swipeOffset) < AndroidUtilities.dp(THRESHOLD_DP)) {
                        hapticTriggered = false;
                    }
                } else {
                    swipeOffset = 0;
                }
                previewArea.invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isSwiping = false;
                animateReturn();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private void animateReturn() {
        if (returnAnimator != null) {
            returnAnimator.cancel();
        }
        returnAnimator = ValueAnimator.ofFloat(swipeOffset, 0f);
        returnAnimator.setDuration(280);
        returnAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        returnAnimator.addUpdateListener(animation -> {
            swipeOffset = (float) animation.getAnimatedValue();
            previewArea.invalidate();
        });
        returnAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                swipeOffset = 0f;
                previewArea.invalidate();
            }
        });
        returnAnimator.start();
    }

    private void drawSwipePreview(Canvas canvas) {
        int width = previewArea.getWidth();
        int height = previewArea.getHeight();
        if (width <= 0 || height <= 0) return;

        int currentAction = HundredGramConfig.chatMessageSwipeAction;
        int actionColor = getActionColor(currentAction);
        int iconRes = getActionIcon(currentAction);

        float progress = Math.min(1f, Math.abs(swipeOffset) / (float) AndroidUtilities.dp(THRESHOLD_DP));

        // Draw action background reveal on the right
        if (swipeOffset < 0) {
            float circleRadius = AndroidUtilities.dp(22) * Math.max(0.2f, progress);
            float circleX = width - AndroidUtilities.dp(36);
            float circleY = height / 2f;

            actionCirclePaint.setColor(actionColor);
            actionCirclePaint.setAlpha((int) (255 * progress));
            canvas.drawCircle(circleX, circleY, circleRadius, actionCirclePaint);

            Drawable icon = ContextCompat.getDrawable(getContext(), iconRes);
            if (icon != null) {
                int iconSize = (int) (AndroidUtilities.dp(22) * progress);
                icon.setBounds(
                        (int) (circleX - iconSize / 2f),
                        (int) (circleY - iconSize / 2f),
                        (int) (circleX + iconSize / 2f),
                        (int) (circleY + iconSize / 2f)
                );
                icon.setTint(Color.WHITE);
                icon.setAlpha((int) (255 * progress));
                icon.draw(canvas);
            }
        }

        // Draw preview message bubble
        float bubbleLeft = AndroidUtilities.dp(12) + swipeOffset;
        float bubbleRight = width - AndroidUtilities.dp(12) + swipeOffset;
        float bubbleTop = AndroidUtilities.dp(8);
        float bubbleBottom = height - AndroidUtilities.dp(8);
        bubbleRect.set(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom);

        boolean isDark = Theme.isCurrentThemeDark();
        int bubbleBg = isDark ? 0xFF212D3B : 0xFFEEF2F7;
        bubblePaint.setColor(bubbleBg);

        float radius = AndroidUtilities.dp(14);
        canvas.drawRoundRect(bubbleRect, radius, radius, bubblePaint);

        bubbleBorderPaint.setColor(isDark ? 0x22FFFFFF : 0x1A000000);
        canvas.drawRoundRect(bubbleRect, radius, radius, bubbleBorderPaint);

        // Draw texts
        textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        subTextPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));

        String title = LocaleController.getString("SwipeActionPreviewTitle", R.string.SwipeActionPreviewTitle);
        String subtitle = String.format(LocaleController.getString("SwipeActionPreviewSub", R.string.SwipeActionPreviewSub), getActionName(currentAction));

        canvas.drawText(title, bubbleLeft + AndroidUtilities.dp(16), bubbleTop + AndroidUtilities.dp(28), textPaint);
        canvas.drawText(subtitle, bubbleLeft + AndroidUtilities.dp(16), bubbleTop + AndroidUtilities.dp(52), subTextPaint);
    }

    private void rebuildChips() {
        chipsContainer.removeAllViews();
        int current = HundredGramConfig.chatMessageSwipeAction;

        int[] actions = {
                HundredGramConfig.SWIPE_ACTION_REPLY,
                HundredGramConfig.SWIPE_ACTION_SAVE,
                HundredGramConfig.SWIPE_ACTION_COPY,
                HundredGramConfig.SWIPE_ACTION_FORWARD,
                HundredGramConfig.SWIPE_ACTION_EDIT,
                HundredGramConfig.SWIPE_ACTION_DELETE
        };

        for (int action : actions) {
            boolean selected = (action == current);
            TextView chip = new TextView(getContext());
            chip.setText(getActionName(action));
            chip.setTextSize(13);
            chip.setTypeface(selected ? AndroidUtilities.bold() : null);
            chip.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

            int color = getActionColor(action);
            if (selected) {
                chip.setTextColor(Color.WHITE);
                chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), color));
            } else {
                chip.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                chip.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.isCurrentThemeDark() ? 0x1EFFFFFF : 0x14000000));
            }

            chip.setOnClickListener(v -> {
                HundredGramConfig.setChatMessageSwipeAction(action);
                rebuildChips();
                previewArea.invalidate();
                if (listener != null) {
                    listener.onActionChanged(action);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = AndroidUtilities.dp(8);
            chipsContainer.addView(chip, lp);
        }
    }

    public static int getActionColor(int action) {
        switch (action) {
            case HundredGramConfig.SWIPE_ACTION_SAVE:
                return 0xFF00C853; // Green
            case HundredGramConfig.SWIPE_ACTION_COPY:
                return 0xFFFF9100; // Amber/Orange
            case HundredGramConfig.SWIPE_ACTION_FORWARD:
                return 0xFF00B0FF; // Cyan
            case HundredGramConfig.SWIPE_ACTION_EDIT:
                return 0xFFAA00FF; // Purple
            case HundredGramConfig.SWIPE_ACTION_DELETE:
                return 0xFFFF1744; // Red
            case HundredGramConfig.SWIPE_ACTION_REPLY:
            default:
                return 0xFF2979FF; // Blue
        }
    }

    public static int getActionIcon(int action) {
        switch (action) {
            case HundredGramConfig.SWIPE_ACTION_SAVE:
                return R.drawable.msg_fave;
            case HundredGramConfig.SWIPE_ACTION_COPY:
                return R.drawable.msg_copy;
            case HundredGramConfig.SWIPE_ACTION_FORWARD:
                return R.drawable.msg_share;
            case HundredGramConfig.SWIPE_ACTION_EDIT:
                return R.drawable.msg_edit;
            case HundredGramConfig.SWIPE_ACTION_DELETE:
                return R.drawable.msg_delete;
            case HundredGramConfig.SWIPE_ACTION_REPLY:
            default:
                return R.drawable.ic_ab_reply;
        }
    }

    public static String getActionName(int action) {
        switch (action) {
            case HundredGramConfig.SWIPE_ACTION_SAVE:
                return LocaleController.getString("DoubleTapSave", R.string.DoubleTapSave);
            case HundredGramConfig.SWIPE_ACTION_COPY:
                return LocaleController.getString("DoubleTapCopy", R.string.DoubleTapCopy);
            case HundredGramConfig.SWIPE_ACTION_FORWARD:
                return LocaleController.getString("DoubleTapForward", R.string.DoubleTapForward);
            case HundredGramConfig.SWIPE_ACTION_EDIT:
                return LocaleController.getString("DoubleTapEdit", R.string.DoubleTapEdit);
            case HundredGramConfig.SWIPE_ACTION_DELETE:
                return LocaleController.getString("DoubleTapDelete", R.string.DoubleTapDelete);
            case HundredGramConfig.SWIPE_ACTION_REPLY:
            default:
                return LocaleController.getString("DoubleTapReply", R.string.DoubleTapReply);
        }
    }
}
