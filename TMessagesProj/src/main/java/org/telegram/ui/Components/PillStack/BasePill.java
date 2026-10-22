package org.telegram.ui.Components.PillStack;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LoadingDrawable;

public abstract class BasePill extends FrameLayout {

    private static final SparseArray<Long> globalLastUpdateTimes = new SparseArray<>();
    private final Runnable autoRefreshRunnable;
    protected boolean loading;
    protected LoadingDrawable loadingDrawable;
    protected View loadingTargetView;
    private final RectF rectF = new RectF();
    protected Theme.ResourcesProvider resourcesProvider;
    private boolean stackVisible = true;

    public abstract int getPillId();
    public abstract long getRefreshInterval();
    public abstract void onPillClicked();
    public abstract boolean onPillLongClicked();
    public void onPillSelected() {}
    public void onPillUnselected() {}
    public abstract void onUpdateData(boolean force);
    public abstract void updateColors();

    public BasePill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.autoRefreshRunnable = () -> {
            onUpdateData(false);
            scheduleNextUpdate();
        };
        setLayoutParams(new FrameLayout.LayoutParams(-2, -2, (LocaleController.isRTL ? 3 : 5) | 16));
        setClipChildren(false);
        setClipToPadding(false);
    }

    private void scheduleNextUpdate() {
        removeCallbacks(autoRefreshRunnable);
        if (stackVisible) {
            long interval = getRefreshInterval();
            if (interval > 0) {
                postDelayed(autoRefreshRunnable, interval);
            }
        }
    }

    public boolean isRefreshDue() {
        long interval = getRefreshInterval();
        if (interval <= 0) {
            return true;
        }
        Long lastTime = globalLastUpdateTimes.get(getPillId());
        long last = lastTime != null ? lastTime : 0L;
        return last == 0 || SystemClock.elapsedRealtime() - last >= interval;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        long interval = getRefreshInterval();
        if (interval <= 0) {
            return;
        }
        if (PillStackConfig.checkAndClearPendingUpdate(getPillId())) {
            onUpdateData(true);
            scheduleNextUpdate();
        } else if (stackVisible) {
            long now = SystemClock.elapsedRealtime();
            Long lastTime = globalLastUpdateTimes.get(getPillId());
            long last = lastTime != null ? lastTime : 0L;
            if (last != 0) {
                long diff = now - last;
                if (diff < interval) {
                    postDelayed(autoRefreshRunnable, interval - diff);
                    return;
                }
            }
            autoRefreshRunnable.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(autoRefreshRunnable);
    }

    public void onStackVisibilityChanged(boolean visible) {
        if (stackVisible == visible) {
            return;
        }
        stackVisible = visible;
        if (visible) {
            if (getRefreshInterval() <= 0 || !isAttachedToWindow()) {
                return;
            }
            if (isRefreshDue()) {
                onUpdateData(false);
            }
            scheduleNextUpdate();
        } else {
            removeCallbacks(autoRefreshRunnable);
        }
    }

    public void markDataUpdated() {
        globalLastUpdateTimes.put(getPillId(), SystemClock.elapsedRealtime());
        scheduleNextUpdate();
    }

    public void setLoadingTargetView(View view) {
        this.loadingTargetView = view;
    }

    public void startLoading() {
        loading = true;
        if (loadingDrawable == null) {
            loadingDrawable = new LoadingDrawable(resourcesProvider);
            loadingDrawable.setCallback(this);
            loadingDrawable.setGradientScale(2.0f);
            loadingDrawable.setRadiiDp(14.0f);
            updateLoadingColors();
        }
        loadingDrawable.reset();
        loadingDrawable.resetDisappear();
        loadingDrawable.setAlpha(255);
        invalidate();
    }

    public void animateSizeChange() {
        if (isLaidOut() && getVisibility() == View.VISIBLE && getParent() != null && (getParent().getParent() instanceof ViewGroup)) {
            TransitionManager.beginDelayedTransition((ViewGroup) getParent().getParent(),
                    new TransitionSet().addTransition(new ChangeBounds()).setDuration(300L).setInterpolator((TimeInterpolator) CubicBezierInterpolator.EASE_OUT_QUINT));
        }
    }

    public void stopLoading() {
        loading = false;
        if (loadingDrawable != null) {
            loadingDrawable.disappear();
        }
    }

    public void updateLoadingColors() {
        if (loadingDrawable != null) {
            int color = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
            loadingDrawable.setColors(Theme.multAlpha(color, 0.05f), Theme.multAlpha(color, 0.15f), Theme.multAlpha(color, 0.1f), Theme.multAlpha(color, 0.25f));
        }
    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }

    public int getThemedColor(int key, float alpha) {
        return Theme.multAlpha(getThemedColor(key), alpha);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (loadingDrawable != null) {
            if (loadingDrawable.getAlpha() > 0 || !loadingDrawable.isDisappearing()) {
                View view = loadingTargetView != null ? loadingTargetView : this;
                rectF.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                if (loadingDrawable.stroke) {
                    float inset = loadingDrawable.strokePaint.getStrokeWidth() / 2.0f;
                    rectF.inset(inset, inset);
                }
                loadingDrawable.setBounds(rectF);
                loadingDrawable.draw(canvas);
                invalidate();
            }
        }
    }

    @Override
    public boolean verifyDrawable(Drawable drawable) {
        return drawable == loadingDrawable || super.verifyDrawable(drawable);
    }
}
