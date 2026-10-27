package org.telegram.messenger;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

/**
 * OpexGram-style haptics: named actions with a tunable strength and per-action effect,
 * using vibration primitives on Android 11+ and waveforms below that.
 */
public class HundredGramHaptics {

    public static final String[] ACTION_NAMES = {
            "send_message", "send_media", "attach_pick", "reaction_set", "select_message",
            "tab_switch", "pull_archive", "account_switch", "photo_dismiss"
    };

    private static final int[] ACTION_DEFAULTS = {
            4, 4, 1, 5, 1, 2, 5, 6, 9
    };

    private static final class Effect {
        final int[] primitives;
        final float[] scales;
        final int[] delays;
        final long[] timings;
        final int[] amplitudes;

        Effect(int[] primitives, float[] scales, int[] delays, long[] timings, int[] amplitudes) {
            this.primitives = primitives;
            this.scales = scales;
            this.delays = delays;
            this.timings = timings;
            this.amplitudes = amplitudes;
        }

        boolean isEmpty() {
            return primitives.length == 0;
        }
    }

    private static final Effect[] EFFECTS = {
            new Effect(new int[0], new float[0], new int[0], new long[0], new int[0]),
            new Effect(new int[]{7}, new float[]{0.3f}, new int[]{0}, new long[]{2}, new int[]{60}),
            new Effect(new int[]{7}, new float[]{0.55f}, new int[]{0}, new long[]{7}, new int[]{95}),
            new Effect(new int[]{8}, new float[]{0.65f}, new int[]{0}, new long[]{12}, new int[]{110}),
            new Effect(new int[]{1}, new float[]{0.6f}, new int[]{0}, new long[]{7}, new int[]{170}),
            new Effect(new int[]{1}, new float[]{0.95f}, new int[]{0}, new long[]{3}, new int[]{225}),
            new Effect(new int[]{2}, new float[]{1f}, new int[]{0}, new long[]{9}, new int[]{255}),
            new Effect(new int[]{1, 1}, new float[]{0.6f, 0.6f}, new int[]{0, 70}, new long[]{7, 60, 7}, new int[]{170, 0, 170}),
            new Effect(new int[]{4}, new float[]{0.6f}, new int[]{0}, new long[]{10, 10, 10, 12}, new int[]{60, 110, 175, 235}),
            new Effect(new int[]{6}, new float[]{0.6f}, new int[]{0}, new long[]{12, 10, 10, 10}, new int[]{235, 175, 110, 60}),
            new Effect(new int[]{4, 1}, new float[]{0.4f, 0.75f}, new int[]{0, 0}, new long[]{14, 65, 14}, new int[]{175, 0, 255}),
            new Effect(new int[]{1, 7}, new float[]{0.8f, 0.5f}, new int[]{0, 80}, new long[]{14, 64, 14}, new int[]{225, 0, 175}),
            new Effect(new int[]{1, 1, 1}, new float[]{0.75f, 0.75f, 1f}, new int[]{0, 60, 60}, new long[]{14, 48, 14, 48, 20}, new int[]{200, 0, 200, 0, 255}),
    };

    public static int getEffectCount() {
        return EFFECTS.length;
    }

    public static int defaultEffect(String action) {
        for (int i = 0; i < ACTION_NAMES.length; i++) {
            if (ACTION_NAMES[i].equals(action)) {
                return ACTION_DEFAULTS[i];
            }
        }
        return 0;
    }

    public static boolean hasVibrator() {
        try {
            Vibrator vibrator = AndroidUtilities.getVibrator();
            return vibrator != null && vibrator.hasVibrator();
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static void play(View view, String action) {
        if (!HundredGramConfig.hapticsEnabled || !HundredGramConfig.isHapticActionEnabled(action)) {
            return;
        }
        playEffect(view, HundredGramConfig.getHapticEffect(action));
    }

    public static void playEffect(View view, int effectIndex) {
        if (effectIndex <= 0 || effectIndex >= EFFECTS.length) {
            return;
        }
        Effect effect = EFFECTS[effectIndex];
        if (effect.isEmpty()) {
            return;
        }
        Vibrator vibrator = null;
        try {
            if (view != null) {
                vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator == null) {
                vibrator = AndroidUtilities.getVibrator();
            }
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            float strength = HundredGramConfig.hapticStrength / 100f;
            if (Build.VERSION.SDK_INT >= 30) {
                VibrationEffect.Composition composition = VibrationEffect.startComposition();
                for (int i = 0; i < effect.primitives.length; i++) {
                    composition.addPrimitive(effect.primitives[i],
                            Math.max(0.05f, Math.min(1f, effect.scales[i] * strength)), effect.delays[i]);
                }
                vibrator.vibrate(composition.compose());
            } else if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(effect.timings, effect.amplitudes, -1));
            } else {
                vibrator.vibrate(effect.timings, -1);
            }
        } catch (Throwable ignore) {
        }
    }
}
