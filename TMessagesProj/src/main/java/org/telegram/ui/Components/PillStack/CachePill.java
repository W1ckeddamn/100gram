package org.telegram.ui.Components.PillStack;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.LaunchActivity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressLint({"ViewConstructor"})
public class CachePill extends BasePill implements NotificationCenter.NotificationCenterDelegate {

    private static final AtomicLong lastKnownCacheSize = new AtomicLong(-1);
    private static float lastKnownProgress = -1.0f;
    private final AtomicBoolean calculating = new AtomicBoolean(false);
    private final ImageView iconView;
    private final LinearLayout layout;
    private final StorageProgressDrawable progressDrawable;
    private final AnimatedTextView textView;

    @Override
    public long getRefreshInterval() {
        return 180000L; // 3 minutes
    }

    public CachePill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
        LinearLayout linearLayout = new LinearLayout(context);
        this.layout = linearLayout;
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setMinimumWidth(AndroidUtilities.dp(48.0f));
        linearLayout.setPadding(AndroidUtilities.dp(6.0f), 0, AndroidUtilities.dp(8.0f), 0);
        addView(linearLayout, LayoutHelper.createFrame(-2, 28, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL));

        ImageView imageView = new ImageView(context);
        this.iconView = imageView;
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        linearLayout.addView(imageView, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));

        StorageProgressDrawable storageProgressDrawable = new StorageProgressDrawable(imageView);
        this.progressDrawable = storageProgressDrawable;
        imageView.setImageDrawable(storageProgressDrawable);

        AnimatedTextView animatedTextView = new AnimatedTextView(context, true, true, true);
        this.textView = animatedTextView;
        animatedTextView.setTextSize(AndroidUtilities.dp(13.0f));
        animatedTextView.setTypeface(AndroidUtilities.bold());
        animatedTextView.setIncludeFontPadding(false);
        animatedTextView.adaptWidth = true;
        linearLayout.addView(animatedTextView, LayoutHelper.createLinear(-2, -2, Gravity.CENTER_VERTICAL));

        setLoadingTargetView(linearLayout);
        updateColors();
        ScaleStateListAnimator.apply(linearLayout);

        long cached = lastKnownCacheSize.get();
        if (cached != -1 && !isRefreshDue()) {
            setData(cached, lastKnownProgress, false);
        } else {
            imageView.setVisibility(View.GONE);
            animatedTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getPillId() {
        return PillType.TYPE_CACHE;
    }

    @Override
    public void onUpdateData(boolean force) {
        boolean firstTime = lastKnownCacheSize.get() == -1;
        if ((force || firstTime || isRefreshDue()) && calculating.compareAndSet(false, true)) {
            if (force || firstTime) {
                CacheControlActivity.resetCalculatedTotalSIze();
            }
            startLoading();
            Utilities.globalQueue.postRunnable(() -> {
                ImageLoader.getInstance().checkMediaPaths(() -> {
                    CacheControlActivity.calculateTotalSize(size -> {
                        lastKnownCacheSize.set(size != null ? size : 0L);
                        CacheControlActivity.getDeviceTotalSize((total, free) -> {
                            float progress = (total != null && total > 0 && free != null) ? (float) (total - free) / total : 0.0f;
                            lastKnownProgress = progress;
                            calculating.set(false);
                            AndroidUtilities.runOnUIThread(() -> setData(lastKnownCacheSize.get(), progress, true));
                        });
                    });
                });
            });
        }
    }

    private void setData(long cacheSize, float progress, boolean animated) {
        stopLoading();
        String text = AndroidUtilities.formatFileSize(cacheSize);
        if (animated && (textView.getText() == null || !TextUtils.equals(textView.getText(), text) || textView.getVisibility() == View.GONE)) {
            animateSizeChange();
        }
        textView.setText(text, animated);
        progressDrawable.setProgress(progress, animated);
        iconView.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        markDataUpdated();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.pillStackSettingsChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.pillStackSettingsChanged);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.pillStackSettingsChanged && PillStackConfig.shouldUpdatePill(args, getPillId())) {
            PillStackConfig.checkAndClearPendingUpdate(getPillId());
            onUpdateData(true);
        }
    }

    @Override
    public void onPillClicked() {
        openCacheSettings();
    }

    @Override
    public boolean onPillLongClicked() {
        final BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment == null) {
            return false;
        }
        ItemOptions.makeOptions(fragment, this)
                .add(R.drawable.msg_settings, LocaleController.getString("StorageUsage", R.string.StorageUsage), this::openCacheSettings)
                .addGap()
                .add(R.drawable.msg_retry, LocaleController.getString("Refresh", R.string.Refresh), () -> onUpdateData(true))
                .setDrawScrim(false)
                .setDimAlpha(0)
                .show();
        return true;
    }

    private void openCacheSettings() {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment != null) {
            fragment.presentFragment(new CacheControlActivity());
        }
    }

    @Override
    public void updateColors() {
        int themedColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText, 0.75f);
        this.layout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(14.0f),
                Theme.isCurrentThemeDark() ? getThemedColor(Theme.key_windowBackgroundWhite) : Theme.multAlpha(themedColor, 0.09f),
                Theme.multAlpha(themedColor, 0.15f)
        ));
        this.textView.setTextColor(themedColor);
        this.progressDrawable.setColor(themedColor);
        updateLoadingColors();
    }

    public static class StorageProgressDrawable extends Drawable {
        private final AnimatedFloat animatedProgress;
        private int color;
        private final Paint paint;
        private float progress;
        private final RectF rectF;

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        public StorageProgressDrawable(View view) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rectF = new RectF();
            progress = 0.0f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            animatedProgress = new AnimatedFloat(view, 650L, CubicBezierInterpolator.EASE_OUT_QUINT);
        }

        public void setProgress(float val, boolean animated) {
            float clamped = Math.max(0.05f, Math.min(val, 1.0f));
            progress = clamped;
            if (!animated) {
                animatedProgress.set(clamped, true);
            }
            invalidateSelf();
        }

        public void setColor(int color) {
            this.color = color;
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();
            float min = Math.min(width, height) - AndroidUtilities.dp(2.0f);
            float cx = width / 2.0f;
            float cy = height / 2.0f;
            float halfMin = min / 2.0f;
            rectF.set(cx - halfMin, cy - halfMin, cx + halfMin, cy + halfMin);

            float animated = animatedProgress.set(progress);
            paint.setStrokeWidth(AndroidUtilities.dp(2.0f));
            paint.setColor(color);
            paint.setAlpha(50);
            canvas.drawCircle(cx, cy, halfMin, paint);
            paint.setAlpha(255);
            canvas.drawArc(rectF, -90.0f, animated * 360.0f, false, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }
    }
}
