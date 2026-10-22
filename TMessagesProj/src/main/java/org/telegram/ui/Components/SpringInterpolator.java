package org.telegram.ui.Components;

import android.view.animation.Interpolator;

/**
 * SpringInterpolator provides physics-based damped harmonic oscillation for fragment and screen transitions.
 * Inspired by exteraGram's SPRING transition physics.
 */
public class SpringInterpolator implements Interpolator {

    public static final SpringInterpolator INSTANCE = new SpringInterpolator(0.72f, 12.0f);

    private final float damping;
    private final float frequency;

    public SpringInterpolator() {
        this(0.72f, 12.0f);
    }

    public SpringInterpolator(float damping, float frequency) {
        this.damping = damping;
        this.frequency = frequency;
    }

    @Override
    public float getInterpolation(float input) {
        if (input <= 0f) return 0f;
        if (input >= 1f) return 1f;

        // Damped harmonic oscillation: 1 - e^(-beta*t) * cos(omega*t)
        return (float) (1.0 - Math.exp(-damping * input * 6.0) * Math.cos(frequency * input * (1.0 - input * 0.3)));
    }
}
