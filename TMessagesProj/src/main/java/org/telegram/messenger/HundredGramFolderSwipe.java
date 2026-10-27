package org.telegram.messenger;

import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

import java.util.WeakHashMap;

/**
 * Shrinks a dialogs page while it is being swiped between folders.
 * Ported from OpexGram (lc.p2 / lc.o2 / gc.n).
 */
public class HundredGramFolderSwipe {

    private static final WeakHashMap<View, PageOutline> outlines = new WeakHashMap<>();
    private static float lastTabsBottom;
    private static Spring cachedSpring;
    private static int cachedSpringMode = -1;

    private static class PageOutline extends ViewOutlineProvider {
        final ColorDrawable background = new ColorDrawable();
        boolean active;
        float top;
        float radius;
        int color;

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, Math.round(top), view.getWidth(), view.getHeight(), radius);
        }
    }

    public static void apply(View page, float translationX, View tabsView, int color) {
        PageOutline outline = outlines.get(page);
        int width = page.getMeasuredWidth();
        float shrink = HundredGramConfig.folderSwipe ? HundredGramConfig.folderSwipeShrink / 100f : 0f;
        float sin = (float) Math.sin((width > 0 ? Math.min(1f, Math.abs(translationX) / width) : 0f) * Math.PI);
        float scale = 1f - shrink * sin;
        if (shrink <= 0f || scale >= 0.9995f) {
            if (outline == null || !outline.active) {
                return;
            }
            outline.active = false;
            outline.background.setAlpha(0);
            page.setClipToOutline(false);
            page.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            page.setScaleX(1f);
            page.setScaleY(1f);
            if (Build.VERSION.SDK_INT >= 28) {
                page.resetPivot();
            } else {
                page.setPivotX(page.getWidth() / 2f);
                page.setPivotY(page.getHeight() / 2f);
            }
            invalidateParent(page);
            return;
        }
        if (outline == null) {
            outline = new PageOutline();
            outlines.put(page, outline);
        }
        float tabsBottom = (tabsView == null || tabsView.getVisibility() != View.VISIBLE) ? 0f : tabsView.getY() + tabsView.getHeight();
        outline.top = Math.max(0f, (tabsBottom - page.getTop()) - page.getTranslationY());
        outline.radius = AndroidUtilities.dp(28f) * sin;
        boolean invalidate = Math.abs(lastTabsBottom - tabsBottom) > 0.5f;
        lastTabsBottom = tabsBottom;
        if (outline.color != color) {
            outline.color = color;
            outline.background.setColor(color);
            invalidate = true;
        }
        if (!outline.active) {
            outline.active = true;
            if (page.getBackground() == null) {
                page.setBackground(outline.background);
            }
            outline.background.setAlpha(255);
            page.setOutlineProvider(outline);
            page.setClipToOutline(true);
        }
        if (invalidate) {
            invalidateParent(page);
        }
        page.setPivotX(width / 2f);
        page.setPivotY((outline.top + page.getMeasuredHeight()) / 2f);
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.invalidateOutline();
    }

    private static void invalidateParent(View view) {
        if (view.getParent() instanceof View) {
            ((View) view.getParent()).invalidate();
        }
    }

    public static Spring spring() {
        int mode = HundredGramConfig.folderSwipeSpring;
        if (cachedSpring == null || cachedSpringMode != mode) {
            cachedSpring = mode == 1 ? new Spring(300f, 0.9f, 0f) : new Spring(200f, 0.8f, 0f);
            cachedSpringMode = mode;
        }
        return cachedSpring;
    }

    /** Replaces the default 320ms/cubic-bezier snap with the selected spring. */
    public static void applySpring(AnimatorSet animator, float velocity) {
        float clamped = Math.max(-40f, Math.min(40f, velocity));
        Spring s = HundredGramConfig.folderSwipeSpring == 1
                ? new Spring(300f, 0.9f, clamped)
                : new Spring(200f, 0.8f, clamped);
        animator.setDuration(Math.round(s.durationSeconds * 1000f));
        animator.setInterpolator(s);
    }

    public static class Spring implements TimeInterpolator {
        public final float durationSeconds;
        private final float damping;
        private final float frequency;
        private final float offset;

        Spring(float stiffness, float dampingRatio, float velocity) {
            float sqrt = (float) Math.sqrt(Math.max(1.0E-4f, stiffness));
            float ratio = Math.max(0.0f, Math.min(0.999f, dampingRatio));
            this.damping = ratio * sqrt;
            this.frequency = sqrt * (float) Math.sqrt(1.0f - ratio * ratio);
            this.offset = (velocity - this.damping) / this.frequency;
            this.durationSeconds = Math.max(0.12f, Math.min(1.2f, this.damping > 0f
                    ? (float) Math.log((float) Math.sqrt(offset * offset + 1f) / 0.001f) / this.damping
                    : 1.2f));
        }

        @Override
        public float getInterpolation(float t) {
            if (t >= 1f) {
                return 1f;
            }
            double x = t * durationSeconds;
            double decay = Math.exp(-damping * x);
            float value = (float) (((Math.sin(frequency * x) * offset) - Math.cos(frequency * x)) * decay) + 1f;
            return value == 1f ? 0.99999f : value;
        }
    }
}
