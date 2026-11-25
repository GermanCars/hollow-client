package dev.hollow.ui;

public final class Ease {
    private Ease() {}

    public static float clamp(float t) { return Math.clamp(t, 0f, 1f); }

    public static float outCubic(float t) { t = 1 - clamp(t); return 1 - t * t * t; }

    public static float outQuint(float t) { t = 1 - clamp(t); return 1 - t * t * t * t * t; }

    /** Overshoots past 1 and settles on it. */
    public static float outBack(float t) {
        t = clamp(t) - 1;
        return 1 + 2.4f * t * t * t + 1.4f * t * t;
    }

    public static float inOut(float t) { t = clamp(t); return t * t * (3 - 2 * t); }

    /** Frame-rate independent approach towards a target. */
    public static float approach(float from, float to, float speed, float seconds) { return to + (from - to) * (float) Math.exp(-speed * seconds); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
