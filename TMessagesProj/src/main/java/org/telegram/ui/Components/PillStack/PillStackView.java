package org.telegram.ui.Components.PillStack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.CubicBezierInterpolator;

import java.util.ArrayList;
import java.util.List;

public class PillStackView extends FrameLayout {

    private ValueAnimator currentAnimator;
    private int currentIndex = 0;
    private float currentSwipeProgress = 0.0f;
    private boolean isSwiping = false;
    private boolean isSwipingUp = false;
    private boolean longClickPerformed = false;
    private boolean maybeClick = false;
    private final List<BasePill> pills = new ArrayList<>();
    private boolean stackOnScreen = true;
    private float startX;
    private float startY;
    private final float touchSlop;
    private float visibilityFactor = -1.0f;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!maybeClick || isSwiping || pills.isEmpty()) {
                return;
            }
            BasePill current = pills.get(currentIndex);
            longClickPerformed = current.onPillLongClicked();
            if (longClickPerformed) {
                try {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                maybeClick = false;
                for (BasePill pill : pills) {
                    pill.setPressed(false);
                }
            }
        }
    };

    public PillStackView(Context context) {
        super(context);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
    }

    public void addPill(BasePill pill) {
        pills.add(pill);
        addView(pill);
        if (pills.size() - 1 != currentIndex) {
            pill.setAlpha(0.0f);
            pill.setScaleX(0.8f);
            pill.setScaleY(0.8f);
            pill.setVisibility(View.GONE);
        } else {
            pill.setVisibility(View.VISIBLE);
            pill.onPillSelected();
        }
        pill.onStackVisibilityChanged(stackOnScreen);
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (stackOnScreen == isVisible) {
            return;
        }
        stackOnScreen = isVisible;
        for (BasePill pill : pills) {
            pill.onStackVisibilityChanged(isVisible);
        }
    }

    public int getPillsCount() {
        return pills.size();
    }

    public void setCurrentIndex(int index) {
        if (index < 0 || index >= pills.size() || index == currentIndex) {
            return;
        }
        BasePill oldPill = pills.get(currentIndex);
        oldPill.setVisibility(View.GONE);
        oldPill.onPillUnselected();

        currentIndex = index;
        BasePill newPill = pills.get(index);
        newPill.setVisibility(View.VISIBLE);
        newPill.setAlpha(1.0f);
        newPill.setScaleX(1.0f);
        newPill.setScaleY(1.0f);
        newPill.setTranslationY(0.0f);
        newPill.onPillSelected();
        requestLayout();
    }

    public void clearPills() {
        if (!pills.isEmpty() && currentIndex < pills.size()) {
            pills.get(currentIndex).onPillUnselected();
        }
        pills.clear();
        removeAllViews();
        currentIndex = 0;
    }

    public void setVisibilityFactor(float factor) {
        if (this.visibilityFactor == factor) {
            return;
        }
        this.visibilityFactor = factor;
        if (factor > 0.01f) {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
            setAlpha(factor);
            setScaleX(AndroidUtilities.lerp(0.6f, 1.0f, factor));
            setScaleY(AndroidUtilities.lerp(0.6f, 1.0f, factor));
        } else {
            setVisibility(View.GONE);
        }
    }

    public void updateColors() {
        for (BasePill pill : pills) {
            pill.updateColors();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (pills.isEmpty()) {
            return super.onInterceptTouchEvent(ev);
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = ev.getRawX();
            startY = ev.getRawY();
            isSwiping = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = ev.getRawX() - startX;
            float dy = ev.getRawY() - startY;
            if (Math.abs(dy) > touchSlop && pills.size() > 1 && Math.abs(dy) > Math.abs(dx)) {
                isSwiping = true;
                if (currentAnimator != null) {
                    currentAnimator.cancel();
                }
                startY = ev.getRawY() - (isSwipingUp ? -(currentSwipeProgress * getHeight()) : getHeight() * currentSwipeProgress);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (pills.isEmpty()) {
            return super.onTouchEvent(ev);
        }
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = ev.getRawX();
            startY = ev.getRawY();
            maybeClick = true;
            longClickPerformed = false;
            postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
            if (currentIndex < pills.size()) {
                pills.get(currentIndex).setPressed(true);
            }
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = ev.getRawX() - startX;
            float dy = ev.getRawY() - startY;
            if (!isSwiping && (Math.abs(dy) > touchSlop || Math.abs(dx) > touchSlop)) {
                removeCallbacks(longPressRunnable);
                maybeClick = false;
                if (Math.abs(dy) > touchSlop && pills.size() > 1) {
                    isSwiping = true;
                    if (currentAnimator != null) {
                        currentAnimator.cancel();
                    }
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
            }
            if (isSwiping) {
                handleSwipeProgress(ev.getRawY() - startY);
            }
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            removeCallbacks(longPressRunnable);
            if (isSwiping) {
                finishSwipe(ev.getRawY() - startY);
                isSwiping = false;
            } else if (maybeClick && !longClickPerformed) {
                if (currentIndex < pills.size()) {
                    pills.get(currentIndex).onPillClicked();
                }
            }
            for (BasePill pill : pills) {
                pill.setPressed(false);
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(longPressRunnable);
            if (isSwiping) {
                cancelSwipe(isSwipingUp);
                isSwiping = false;
            }
            for (BasePill pill : pills) {
                pill.setPressed(false);
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }

    private void handleSwipeProgress(float dy) {
        int height = getHeight();
        if (pills.size() > 1 && height > 0) {
            isSwipingUp = dy < 0.0f;
            float progress = Math.abs(dy) / height;
            int nextIndex = isSwipingUp ? currentIndex + 1 : currentIndex - 1;
            if (!PillStackConfig.getInfiniteScrolling() && (nextIndex >= pills.size() || nextIndex < 0)) {
                currentSwipeProgress = progress;
            } else {
                currentSwipeProgress = Math.min(progress, 1.0f);
            }
            applyProgress(currentSwipeProgress, isSwipingUp);
        }
    }

    private void applyProgress(float progress, boolean swipingUp) {
        BasePill currentPill = pills.get(currentIndex);
        int nextIndex = swipingUp ? currentIndex + 1 : currentIndex - 1;
        if (PillStackConfig.getInfiniteScrolling()) {
            if (nextIndex >= pills.size()) {
                nextIndex = 0;
            }
            if (nextIndex < 0) {
                nextIndex = pills.size() - 1;
            }
        }

        for (int i = 0; i < pills.size(); i++) {
            if (i != currentIndex && i != nextIndex && pills.get(i).getVisibility() != View.GONE) {
                pills.get(i).setVisibility(View.GONE);
            }
        }

        if (!PillStackConfig.getInfiniteScrolling() && (nextIndex >= pills.size() || nextIndex < 0)) {
            float height = getHeight() * (float) (1.0d - (1.0d / ((progress * 0.18f) + 1.0d)));
            currentPill.setTranslationY(swipingUp ? -height : height);
            currentPill.setAlpha(1.0f);
            return;
        }

        float p = Math.min(progress, 1.0f);
        BasePill nextPill = pills.get(nextIndex);
        if (nextPill.getVisibility() != View.VISIBLE) {
            nextPill.setVisibility(View.VISIBLE);
        }

        float translation = getHeight() * p;
        currentPill.setTranslationY(swipingUp ? -translation : translation);
        currentPill.setAlpha(1.0f - p);
        float scaleCurrent = 1.0f - (0.2f * p);
        currentPill.setScaleX(scaleCurrent);
        currentPill.setScaleY(scaleCurrent);

        float scaleNext = 0.8f + (0.2f * p);
        nextPill.setScaleX(scaleNext);
        nextPill.setScaleY(scaleNext);
        nextPill.setAlpha(p);

        int height = getHeight();
        float startNextY = swipingUp ? height : -height;
        nextPill.setTranslationY(startNextY - (p * startNextY));
    }

    private void finishSwipe(float dy) {
        int height = getHeight();
        if (height <= 0) {
            cancelSwipe(isSwipingUp);
            return;
        }
        float threshold = height * 0.25f;
        boolean canSwitch = true;
        if (!PillStackConfig.getInfiniteScrolling()) {
            int next = isSwipingUp ? currentIndex + 1 : currentIndex - 1;
            if (next >= pills.size() || next < 0) {
                canSwitch = false;
            }
        }
        if (Math.abs(dy) > threshold && canSwitch) {
            animateToNextPill(isSwipingUp);
        } else {
            cancelSwipe(isSwipingUp);
        }
    }

    private void animateToNextPill(final boolean swipingUp) {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(currentSwipeProgress, 1.0f);
        currentAnimator = animator;
        animator.setDuration(250L);
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        animator.addUpdateListener(animation -> applyProgress((Float) animation.getAnimatedValue(), swipingUp));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                BasePill oldPill = pills.get(currentIndex);
                oldPill.setVisibility(View.GONE);
                oldPill.setPressed(false);
                oldPill.setScaleX(1.0f);
                oldPill.setScaleY(1.0f);
                oldPill.onPillUnselected();

                currentIndex = swipingUp ? currentIndex + 1 : currentIndex - 1;
                if (PillStackConfig.getInfiniteScrolling()) {
                    if (currentIndex >= pills.size()) {
                        currentIndex = 0;
                    }
                    if (currentIndex < 0) {
                        currentIndex = pills.size() - 1;
                    }
                }

                for (int i = 0; i < pills.size(); i++) {
                    if (i != currentIndex) {
                        pills.get(i).setVisibility(View.GONE);
                    }
                }
                BasePill activePill = pills.get(currentIndex);
                activePill.setVisibility(View.VISIBLE);
                activePill.setScaleX(1.0f);
                activePill.setScaleY(1.0f);
                activePill.setTranslationY(0.0f);
                activePill.setAlpha(1.0f);
                activePill.onPillSelected();
                currentSwipeProgress = 0.0f;
                PillStackConfig.saveLastActivePillId(activePill.getPillId());
            }
        });
        animator.start();
    }

    private void cancelSwipe(final boolean swipingUp) {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(currentSwipeProgress, 0.0f);
        currentAnimator = animator;
        animator.setDuration(200L);
        animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        animator.addUpdateListener(animation -> applyProgress((Float) animation.getAnimatedValue(), swipingUp));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (cancelled) return;
                for (int i = 0; i < pills.size(); i++) {
                    if (i != currentIndex) {
                        BasePill p = pills.get(i);
                        p.setVisibility(View.GONE);
                        p.setPressed(false);
                        p.setScaleX(1.0f);
                        p.setScaleY(1.0f);
                    }
                }
                BasePill current = pills.get(currentIndex);
                current.setTranslationY(0.0f);
                current.setAlpha(1.0f);
                current.setScaleX(1.0f);
                current.setScaleY(1.0f);
                currentSwipeProgress = 0.0f;
            }
        });
        animator.start();
    }
}
