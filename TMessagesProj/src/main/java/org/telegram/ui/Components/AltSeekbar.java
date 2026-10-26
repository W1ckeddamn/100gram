package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HundredGramConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;

public class AltSeekbar extends FrameLayout {

    public interface OnDrag {
        void run(float value);
    }

    protected final int min;
    protected final int max;
    protected float currentValue;
    protected int roundedValue;
    private final OnDrag onDrag;

    private final AnimatedTextView headerValue;
    protected final TextView leftTextView;
    protected final TextView rightTextView;
    public SeekBarView seekBarView;
    private MaterialSlider materialSlider;
    private int lastVibratedValue = -1;

    public AltSeekbar(Context context, OnDrag onDrag, int min, int max, String title, String leftLabel, String rightLabel) {
        super(context);
        this.onDrag = onDrag;
        this.min = min;
        this.max = max;

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(LocaleController.isRTL ? 5 : 3);

        TextView titleTextView = new TextView(context);
        titleTextView.setTextSize(1, 15.0f);
        titleTextView.setTypeface(AndroidUtilities.bold());
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleTextView.setText(title);
        headerLayout.addView(titleTextView, LayoutHelper.createLinear(-2, -2, 16));

        headerValue = new AnimatedTextView(context, false, true, true) {
            final Drawable backgroundDrawable = Theme.createRoundRectDrawable(
                    AndroidUtilities.dp(4.0f),
                    Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader), 0.15f)
            );

            @Override
            protected void onDraw(Canvas canvas) {
                if (getDrawable() != null) {
                    backgroundDrawable.setBounds(0, 0, (int) (getPaddingLeft() + getDrawable().getCurrentWidth() + getPaddingRight()), getMeasuredHeight());
                    backgroundDrawable.draw(canvas);
                }
                super.onDraw(canvas);
            }
        };
        headerValue.setAnimationProperties(0.45f, 0L, 240L, CubicBezierInterpolator.EASE_OUT_QUINT);
        headerValue.setAllowCancel(true);
        headerValue.setTypeface(AndroidUtilities.bold());
        headerValue.setPadding(AndroidUtilities.dp(5.33f), AndroidUtilities.dp(2.0f), AndroidUtilities.dp(5.33f), AndroidUtilities.dp(2.0f));
        headerValue.setTextSize(AndroidUtilities.dp(12.0f));
        headerValue.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        headerLayout.addView(headerValue, LayoutHelper.createLinear(-2, 17, 16, 6, 1, 0, 0));

        addView(headerLayout, LayoutHelper.createFrame(-1, -2.0f, 55, 21.0f, 12.0f, 21.0f, 0.0f));

        FrameLayout labelsLayout = new FrameLayout(context);
        leftTextView = new TextView(context);
        leftTextView.setTextSize(1, 13.0f);
        leftTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        leftTextView.setText(leftLabel);
        labelsLayout.addView(leftTextView, LayoutHelper.createFrame(-2, -2, 19));

        rightTextView = new TextView(context);
        rightTextView.setTextSize(1, 13.0f);
        rightTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        rightTextView.setText(rightLabel);
        labelsLayout.addView(rightTextView, LayoutHelper.createFrame(-2, -2, 21));

        addView(labelsLayout, LayoutHelper.createFrame(-1, -2.0f, 55, 21.0f, 40.0f, 21.0f, 0.0f));

        initSlider();
    }

    private void initSlider() {
        if (HundredGramConfig.newSliderStyle) {
            materialSlider = new MaterialSlider(getContext());
            materialSlider.setValueFrom(min);
            materialSlider.setValueTo(max);
            materialSlider.addOnChangeListener((value, fromUser) -> {
                if (fromUser) {
                    setProgress(value);
                    if (onDrag != null) {
                        onDrag.run(value);
                    }
                }
            });
            addView(materialSlider, LayoutHelper.createFrame(-1, 40.0f, 48, 7.0f, 62.0f, 7.0f, 4.0f));
        } else {
            seekBarView = new SeekBarView(getContext(), true, null);
            seekBarView.setReportChanges(true);
            seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    float val = AltSeekbar.this.min + progress * (AltSeekbar.this.max - AltSeekbar.this.min);
                    setProgress(val);
                    if (AltSeekbar.this.onDrag != null) {
                        AltSeekbar.this.onDrag.run(val);
                    }
                }
            });
            addView(seekBarView, LayoutHelper.createFrame(-1, 38.0f, 48, 6.0f, 62.0f, 6.0f, 6.0f));
        }
    }

    public void updateSliderStyle() {
        if (materialSlider != null) {
            removeView(materialSlider);
            materialSlider = null;
        }
        if (seekBarView != null) {
            removeView(seekBarView);
            seekBarView = null;
        }
        initSlider();
        setProgress(currentValue);
    }

    public void setProgress(float value) {
        float clamped = MathUtils.clamp(value, min, max);
        currentValue = clamped;
        roundedValue = Math.round(clamped);

        if (materialSlider != null) {
            materialSlider.setValue(currentValue);
        } else if (seekBarView != null && (max - min) > 0) {
            seekBarView.setProgress((currentValue - min) / (float) (max - min));
        }

        headerValue.cancelAnimation();
        headerValue.setText(getTextForHeader(), true);

        checkEndpointHaptic(currentValue);
        updateValues();
    }

    private void checkEndpointHaptic(float value) {
        int target = -1;
        if (roundedValue == min) {
            target = min;
        } else if (roundedValue == max) {
            target = max;
        }
        if (target != -1 && target != lastVibratedValue) {
            lastVibratedValue = target;
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        } else if (target == -1) {
            lastVibratedValue = -1;
        }
    }

    private void updateValues() {
        int gray = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        int blue = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);

        float mid = (min + max) / 2.0f;
        if (currentValue >= mid) {
            float frac = (currentValue - mid) / (max - mid);
            rightTextView.setTextColor(ColorUtils.blendARGB(gray, blue, frac));
            leftTextView.setTextColor(gray);
        } else {
            float frac = (mid - currentValue) / (mid - min);
            leftTextView.setTextColor(ColorUtils.blendARGB(gray, blue, frac));
            rightTextView.setTextColor(gray);
        }
    }

    public CharSequence getTextForHeader() {
        if (roundedValue == min) {
            return leftTextView.getText().toString().toUpperCase();
        } else if (roundedValue == max) {
            return rightTextView.getText().toString().toUpperCase();
        } else {
            return String.valueOf(roundedValue);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(widthMeasureSpec), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(106.0f), View.MeasureSpec.EXACTLY)
        );
    }
}
