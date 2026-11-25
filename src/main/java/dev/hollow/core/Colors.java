package dev.hollow.core;

public final class Colors {
    private Colors() {}

    public static int hsv(float h, float s, float v, float a) {
        h = (h % 1 + 1) % 1 * 6;
        int i = (int) h;
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return Math.round(a * 255) << 24 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    public static float[] toHsv(int argb) {
        float r = (argb >> 16 & 255) / 255f, g = (argb >> 8 & 255) / 255f, b = (argb & 255) / 255f, max = Math.max(r, Math.max(g, b)), d = max - Math.min(r, Math.min(g, b)), h = 0;
        if (d > 0) {
            if (max == r) h = (g - b) / d / 6;
            else if (max == g) h = ((b - r) / d + 2) / 6;
            else h = ((r - g) / d + 4) / 6;
        }
        return new float[] {(h % 1 + 1) % 1, max == 0 ? 0 : d / max, max};
    }

    public static int alpha(int argb, float a) { return Math.round((argb >>> 24) * Math.clamp(a, 0f, 1f)) << 24 | argb & 0xFFFFFF; }

    /** Replaces the alpha instead of scaling it. */
    public static int with(int argb, float a) { return Math.round(255 * Math.clamp(a, 0f, 1f)) << 24 | argb & 0xFFFFFF; }

    public static int mix(int a, int b, float t) {
        t = Math.clamp(t, 0f, 1f);
        int out = 0;
        for (int s = 0; s <= 24; s += 8) out |= Math.round((a >>> s & 255) + ((b >>> s & 255) - (a >>> s & 255)) * t) << s;
        return out;
    }

    /** Dark text on light fills and the other way round. */
    public static int on(int fill) {
        double l = .299 * (fill >> 16 & 255) + .587 * (fill >> 8 & 255) + .114 * (fill & 255);
        return l > 140 ? 0xFF0B1116 : 0xFFF4F7F8;
    }

    /** Red through amber to green for fractions such as health or durability. */
    public static int health(float t) { return hsv(Math.clamp(t, 0f, 1f) * .36f, .62f, .98f, 1); }
}
