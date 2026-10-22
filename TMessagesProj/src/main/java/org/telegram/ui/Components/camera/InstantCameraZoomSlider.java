package org.telegram.ui.Components.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.SystemClock;
import android.view.MotionEvent;

import java.util.Arrays;
import java.util.List;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.camera.Camera2Session;
import org.telegram.messenger.camera.CameraInfo;
import org.telegram.messenger.camera.CameraSession;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

@SuppressLint({"ViewConstructor"})
public class InstantCameraZoomSlider extends CameraZoomSliderView {
    public static final AnimationProperties.FloatProperty<InstantCameraZoomSlider> OPEN_ALPHA = new AnimationProperties.FloatProperty<InstantCameraZoomSlider>("openAlpha") {
        @Override
        public void setValue(InstantCameraZoomSlider instantCameraZoomSlider, float f) {
            instantCameraZoomSlider.setOpenAlpha(f);
        }

        @Override
        public Float get(InstantCameraZoomSlider instantCameraZoomSlider) {
            return Float.valueOf(instantCameraZoomSlider.getOpenAlpha());
        }
    };
    private boolean animateNextConfiguration;
    private ValueAnimator appearAnimator;
    private float appearProgress;
    private Backend backend;
    private float baseTranslationY;
    private int bindRetries;
    private final Runnable bindRunnable;
    private BlurredBackgroundDrawable blurBackground;
    private float blurCornerRadius;
    private float camera1LinearZoom;
    private CameraSession camera1Session;
    private int camera1ZoomIndex;
    private float[] camera1ZoomRatios;
    private Camera2Session camera2Session;
    private OnCameraZoomChangeListener cameraZoomChangeListener;
    private float defaultZoom;
    private float displayOneZoom;
    private float lastAppliedZoom;
    private long lastZoomAppliedAt;
    private float openAlpha;
    private float[] opticalZoomRatios;
    private float pendingZoom;
    private final Theme.ResourcesProvider resourcesProvider;
    private boolean switchingCamera;
    private int textureViewSize;
    private float wideZoom;
    private final Runnable zoomFlushRunnable;
    private boolean zoomFlushScheduled;

    public enum Backend {
        NONE,
        CAMERA_1,
        CAMERA_2
    }

    public interface OnCameraZoomChangeListener {
        void onCameraZoomChanged(float f, boolean z);
    }

    public InstantCameraZoomSlider(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.backend = Backend.NONE;
        this.camera1ZoomRatios = new float[0];
        this.opticalZoomRatios = new float[0];
        this.camera1ZoomIndex = -1;
        this.defaultZoom = 1.0f;
        this.wideZoom = 1.0f;
        this.displayOneZoom = 1.0f;
        this.pendingZoom = Float.NaN;
        this.lastAppliedZoom = Float.NaN;
        this.bindRunnable = new Runnable() {
            @Override
            public final void run() {
                InstantCameraZoomSlider.this.tryBind();
            }
        };
        this.zoomFlushRunnable = new Runnable() {
            @Override
            public final void run() {
                InstantCameraZoomSlider.this.flushPendingZoom();
            }
        };
        this.blurCornerRadius = -1.0f;
        this.resourcesProvider = resourcesProvider;
        setVisibility(8);
        applyAppearProgress();
        setOnZoomChangeListener(new CameraZoomSliderView.OnZoomChangeListener() {
            @Override
            public final void onZoomChanged(float f) {
                InstantCameraZoomSlider.this.applyZoom(f);
            }
        });
        applyTelegramColors();
    }

    public float getOpenAlpha() {
        return this.openAlpha;
    }

    public void setOpenAlpha(float f) {
        if (this.openAlpha != f) {
            this.openAlpha = f;
            setAlpha(f * this.appearProgress);
        }
    }

    private void setAppearProgress(float f) {
        if (this.appearProgress != f) {
            this.appearProgress = f;
            applyAppearProgress();
        }
    }

    private void applyAppearProgress() {
        setAlpha(this.openAlpha * this.appearProgress);
        float f = (this.appearProgress * 0.100000024f) + 0.9f;
        setScaleX(f);
        setScaleY(f);
    }

    private void showAnimated() {
        setEnabled(true);
        if (!HundredGramConfig.cameraRoundZoomSlider) {
            hideImmediately();
            return;
        }
        if (getVisibility() != 0) {
            setVisibility(0);
        }
        if (this.appearAnimator == null) {
            float f = this.appearProgress;
            if (f >= 1.0f) {
                return;
            }
            ValueAnimator ofFloat = ValueAnimator.ofFloat(f, 1.0f);
            this.appearAnimator = ofFloat;
            ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    InstantCameraZoomSlider.this.setAppearProgress(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            ofFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    if (InstantCameraZoomSlider.this.appearAnimator == animator) {
                        InstantCameraZoomSlider.this.appearAnimator = null;
                        InstantCameraZoomSlider.this.setAppearProgress(1.0f);
                    }
                }
            });
            ofFloat.setDuration(180L);
            ofFloat.setInterpolator(CubicBezierInterpolator.DEFAULT);
            ofFloat.start();
        }
    }

    private void hideImmediately() {
        cancelAppearAnimation();
        setAppearProgress(0.0f);
        setVisibility(8);
    }

    private void cancelAppearAnimation() {
        ValueAnimator valueAnimator = this.appearAnimator;
        if (valueAnimator != null) {
            this.appearAnimator = null;
            valueAnimator.cancel();
        }
    }

    private void applyTelegramColors() {
        int color = Theme.getColor(Theme.key_chat_messagePanelBackground, this.resourcesProvider);
        int color2 = Theme.getColor(Theme.key_featuredStickers_addButton, this.resourcesProvider);
        int color3 = Theme.getColor(Theme.key_chats_actionIcon, this.resourcesProvider);
        int color4 = Theme.getColor(Theme.key_chat_messagePanelText, this.resourcesProvider);
        setColors(color, color4, color2, color2, color3);
        setToggleTextColor(color4);
    }

    public void setBlurBackground(BlurredBackgroundDrawable blurredBackgroundDrawable) {
        if (this.blurBackground != blurredBackgroundDrawable) {
            this.blurBackground = blurredBackgroundDrawable;
            this.blurCornerRadius = -1.0f;
            invalidate();
        }
    }

    @Override
    public boolean drawPillBackground(Canvas canvas, RectF rectF, float f) {
        BlurredBackgroundDrawable blurredBackgroundDrawable = this.blurBackground;
        if (blurredBackgroundDrawable == null) {
            return false;
        }
        if (this.blurCornerRadius != f) {
            this.blurCornerRadius = f;
            blurredBackgroundDrawable.setRadius(f);
        }
        this.blurBackground.setBounds(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
        this.blurBackground.draw(canvas);
        return true;
    }

    public void setOnCameraZoomChangeListener(OnCameraZoomChangeListener onCameraZoomChangeListener) {
        this.cameraZoomChangeListener = onCameraZoomChangeListener;
    }

    public void bindSession(Camera2Session camera2Session) {
        boolean z = camera2Session != null && this.switchingCamera;
        this.switchingCamera = false;
        resetBinding(!z);
        if (camera2Session == null) {
            return;
        }
        this.backend = Backend.CAMERA_2;
        this.camera2Session = camera2Session;
        this.animateNextConfiguration = z;
        tryBind();
    }

    public void bindSession(CameraSession cameraSession, float f) {
        boolean z = cameraSession != null && this.switchingCamera;
        this.switchingCamera = false;
        resetBinding(!z);
        if (cameraSession == null) {
            return;
        }
        this.backend = Backend.CAMERA_1;
        this.camera1Session = cameraSession;
        this.camera1LinearZoom = Utilities.clamp01(f);
        this.animateNextConfiguration = z;
        tryBind();
    }

    public void beginCameraSwitch() {
        if (this.backend != Backend.NONE && getVisibility() == 0 && isLaidOut()) {
            this.switchingCamera = true;
            prepareZoomConfigurationTransition();
            setEnabled(false);
            setExpanded(false, true);
        }
    }

    public void unbindSession() {
        this.switchingCamera = false;
        resetBinding(true);
    }

    private void resetBinding(boolean z) {
        setExternalZoomGestureActive(false);
        resetZoomThrottle();
        this.backend = Backend.NONE;
        setZoom(getZoom());
        this.camera2Session = null;
        this.camera1Session = null;
        this.camera1ZoomRatios = new float[0];
        this.opticalZoomRatios = new float[0];
        this.camera1ZoomIndex = -1;
        this.defaultZoom = 1.0f;
        this.wideZoom = 1.0f;
        this.displayOneZoom = 1.0f;
        this.bindRetries = 0;
        removeCallbacks(this.bindRunnable);
        this.animateNextConfiguration = false;
        if (z) {
            cancelZoomConfigurationTransition();
            setEnabled(true);
            setExpanded(false, false);
            hideImmediately();
        }
    }

    public void syncZoom() {
        if (this.backend == Backend.CAMERA_2) {
            Camera2Session c2Session = this.camera2Session;
            if (c2Session == null || !c2Session.isInitiated()) {
                return;
            }
            setZoom(this.camera2Session.getZoom());
            return;
        }
        if (this.backend == Backend.CAMERA_1 && this.camera1ZoomRatios.length > 1) {
            setZoom(camera1RatioForLinearZoom(this.camera1LinearZoom));
        }
    }

    public void syncZoom(float f) {
        if (this.backend == Backend.CAMERA_1) {
            float clamp01 = Utilities.clamp01(f);
            this.camera1LinearZoom = clamp01;
            this.camera1ZoomIndex = -1;
            if (this.camera1ZoomRatios.length > 1) {
                setZoom(camera1RatioForLinearZoom(clamp01));
                return;
            }
            return;
        }
        if (this.backend == Backend.CAMERA_2) {
            setZoom(f);
        } else {
            syncZoom();
        }
    }

    public void beginExternalZoomGesture() {
        discardPendingZoom();
        setZoom(getZoom());
    }

    public void beginPinchZoomGesture() {
        beginExternalZoomGesture();
        setExternalZoomGestureActive(true);
    }

    public void endPinchZoomGesture() {
        setExternalZoomGestureActive(false);
    }

    public void beginSteppedZoomGesture() {
        beginExternalZoomGesture();
        setExpanded(true, true);
    }

    public float[] getOpticalZoomRatios() {
        float[] fArr = this.opticalZoomRatios;
        return Arrays.copyOf(fArr, fArr.length);
    }

    public float getDisplayOneZoom() {
        return this.displayOneZoom;
    }

    private void tryBind() {
        float minZoomRatio;
        boolean isActiveCameraFrontface;
        float f;
        float[] fArr;
        float cameraRatio;

        if (this.backend == Backend.CAMERA_2) {
            Camera2Session c2Session = this.camera2Session;
            if (c2Session == null || !c2Session.isInitiated()) {
                retryBinding();
                return;
            }
            minZoomRatio = c2Session.getMinZoom();
            f = c2Session.getMaxZoom();
            isActiveCameraFrontface = c2Session.isFront();
            cameraRatio = c2Session.getZoom();
            fArr = CameraLensStops.opticalZoomRatios(getContext(), c2Session.cameraId, minZoomRatio);
        } else if (this.backend == Backend.CAMERA_1) {
            CameraSession cameraSession = this.camera1Session;
            if (cameraSession == null || !cameraSession.isInitied()) {
                retryBinding();
                return;
            }
            float[] readCamera1ZoomRatios = readCamera1ZoomRatios(this.camera1Session);
            this.camera1ZoomRatios = readCamera1ZoomRatios;
            if (readCamera1ZoomRatios == null) {
                this.camera1ZoomRatios = new float[0];
                retryBinding();
                return;
            }
            minZoomRatio = readCamera1ZoomRatios.length == 0 ? 1.0f : readCamera1ZoomRatios[0];
            f = readCamera1ZoomRatios.length == 0 ? 1.0f : readCamera1ZoomRatios[readCamera1ZoomRatios.length - 1];
            isActiveCameraFrontface = false;
            cameraRatio = camera1RatioForLinearZoom(this.camera1LinearZoom);
            fArr = new float[0];
        } else {
            return;
        }

        float f2 = minZoomRatio;
        if (!Float.isFinite(f2) || !Float.isFinite(f) || f2 <= 0.0f || f <= f2) {
            this.animateNextConfiguration = false;
            cancelZoomConfigurationTransition();
            setEnabled(true);
            hideImmediately();
            return;
        }
        this.defaultZoom = Utilities.clamp(1.0f, f, f2);
        this.wideZoom = f2;
        this.displayOneZoom = 1.0f;
        setDisplayNormalizationFactor(1.0f);
        this.opticalZoomRatios = fArr != null ? fArr : new float[0];
        float[] buildToggleStops = CameraLensStops.buildToggleStops(isActiveCameraFrontface, f2, f, this.opticalZoomRatios);
        float[] buildRulerStops = CameraLensStops.buildRulerStops(f2, f, buildToggleStops);

        float clamp = Utilities.clamp(cameraRatio, f, f2);
        boolean z = this.animateNextConfiguration;
        this.animateNextConfiguration = false;
        setZoomConfiguration(f2, f, buildToggleStops, buildRulerStops, clamp, z);
        if (!z) {
            setExpanded(false, false);
        }
        showAnimated();
    }

    private void retryBinding() {
        if (this.backend != Backend.NONE) {
            int i = this.bindRetries;
            this.bindRetries = i + 1;
            if (i < 25) {
                postDelayed(this.bindRunnable, 100L);
                return;
            }
        }
        this.animateNextConfiguration = false;
        cancelZoomConfigurationTransition();
        setEnabled(true);
        hideImmediately();
    }

    private void applyZoom(float f) {
        this.pendingZoom = f;
        scheduleZoomFlush();
    }

    private void scheduleZoomFlush() {
        if (this.zoomFlushScheduled) {
            return;
        }
        this.zoomFlushScheduled = true;
        postOnAnimation(this.zoomFlushRunnable);
    }

    private void flushPendingZoom() {
        this.zoomFlushScheduled = false;
        float f = this.pendingZoom;
        if (Float.isNaN(f) || this.backend == Backend.NONE) {
            return;
        }
        long uptimeMillis = SystemClock.uptimeMillis();
        if (!Float.isNaN(this.lastAppliedZoom)) {
            if (f == this.lastAppliedZoom) {
                return;
            }
            if (uptimeMillis - this.lastZoomAppliedAt < getZoomUpdateIntervalMs()) {
                scheduleZoomFlush();
                return;
            }
        }
        this.lastAppliedZoom = f;
        this.lastZoomAppliedAt = uptimeMillis;
        sendZoomToCamera(f);
    }

    private long getZoomUpdateIntervalMs() {
        int recordingFrameRate = 30;
        if (this.backend == Backend.CAMERA_2 && this.camera2Session != null) {
            recordingFrameRate = this.camera2Session.getRecordingFrameRate();
        }
        if (recordingFrameRate <= 0) {
            recordingFrameRate = 30;
        }
        return Math.max(1L, 1000 / recordingFrameRate);
    }

    private void discardPendingZoom() {
        removeCallbacks(this.zoomFlushRunnable);
        this.zoomFlushScheduled = false;
        this.pendingZoom = Float.NaN;
    }

    private void resetZoomThrottle() {
        discardPendingZoom();
        this.lastAppliedZoom = Float.NaN;
        this.lastZoomAppliedAt = 0L;
    }

    private void sendZoomToCamera(float f) {
        if (this.backend == Backend.CAMERA_2) {
            Camera2Session session = this.camera2Session;
            if (session != null) {
                session.setZoom(f);
            }
        } else if (this.backend == Backend.CAMERA_1) {
            if (this.camera1Session == null || this.camera1ZoomRatios.length < 2) {
                return;
            }
            int camera1ZoomIndexForRatio = camera1ZoomIndexForRatio(f);
            float camera1LinearZoomForIndex = camera1LinearZoomForIndex(camera1ZoomIndexForRatio);
            this.camera1LinearZoom = camera1LinearZoomForIndex;
            if (camera1ZoomIndexForRatio != this.camera1ZoomIndex) {
                this.camera1ZoomIndex = camera1ZoomIndexForRatio;
                this.camera1Session.setZoom(camera1LinearZoomForIndex);
            }
            f = this.camera1LinearZoom;
        }
        OnCameraZoomChangeListener onCameraZoomChangeListener = this.cameraZoomChangeListener;
        if (onCameraZoomChangeListener != null) {
            onCameraZoomChangeListener.onCameraZoomChanged(f, true);
        }
    }

    private static float[] readCamera1ZoomRatios(CameraSession cameraSession) {
        try {
            CameraInfo cameraInfo = cameraSession.cameraInfo;
            android.hardware.Camera camera = cameraInfo != null ? cameraInfo.getCamera() : null;
            if (camera == null) {
                return null;
            }
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null && parameters.isZoomSupported()) {
                List<Integer> zoomRatios = parameters.getZoomRatios();
                if (zoomRatios != null && zoomRatios.size() >= 2) {
                    int size = zoomRatios.size();
                    float[] fArr = new float[size];
                    for (int i = 0; i < size; i++) {
                        float f = 1.0f;
                        Integer r4 = zoomRatios.get(i);
                        if (r4 != null) {
                            f = Math.max(1.0f, r4.intValue() / 100.0f);
                        }
                        fArr[i] = f;
                    }
                    return fArr;
                }
                return new float[0];
            }
            return new float[0];
        } catch (Exception unused) {
            return null;
        }
    }

    private float camera1RatioForLinearZoom(float f) {
        int length = this.camera1ZoomRatios.length - 1;
        return this.camera1ZoomRatios[Math.min((int) (Utilities.clamp01(f) * length), length)];
    }

    private int camera1ZoomIndexForRatio(float f) {
        int i = 0;
        float f2 = Float.MAX_VALUE;
        int i2 = 0;
        while (true) {
            float[] fArr = this.camera1ZoomRatios;
            if (i >= fArr.length) {
                return i2;
            }
            float abs = Math.abs(fArr[i] - f);
            if (abs < f2) {
                i2 = i;
                f2 = abs;
            }
            i++;
        }
    }

    private float camera1LinearZoomForIndex(int i) {
        int length = this.camera1ZoomRatios.length - 1;
        if (i <= 0) {
            return 0.0f;
        }
        if (i >= length) {
            return 1.0f;
        }
        return (i + 0.001f) / length;
    }

    public void setTextureViewSize(int i) {
        if (this.textureViewSize != i) {
            this.textureViewSize = i;
            applyPosition();
        }
    }

    public void setBaseTranslationY(float f) {
        if (this.baseTranslationY != f) {
            this.baseTranslationY = f;
            applyPosition();
        }
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled() || getAlpha() < 0.5f) {
            return false;
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        applyPosition();
    }

    private void applyPosition() {
        setTranslationY(((this.baseTranslationY + (this.textureViewSize / 2.0f)) + AndroidUtilities.dp(80.0f)) - (getMeasuredHeight() / 2.0f));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.backend != Backend.NONE && (getVisibility() != 0 || !isEnabled() || this.animateNextConfiguration)) {
            this.bindRetries = 0;
            tryBind();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.bindRunnable);
        discardPendingZoom();
        if (this.appearAnimator != null) {
            cancelAppearAnimation();
            setAppearProgress(getVisibility() == 0 ? 1.0f : 0.0f);
        }
    }
}