package dev.hollow.ui;

import java.util.Arrays;

/** Snow resting on the top edge of something. */
public final class SnowBank {
    public static final float STEP = .75f;
    public float x, y, width;
    public float[] depth = new float[0];
    /** While shaken it holds no snow. */
    public boolean shaken;
    private final float max;
    private float inset;

    public SnowBank(float max) { this.max = max; }

    /** The left end is chipped off, so snow starts after it. */
    public void place(float x, float y, float width, float inset) {
        this.x = x;
        this.y = y;
        this.inset = inset;
        if (Math.abs(this.width - width) > .5f) {
            this.width = width;
            depth = new float[Math.max(2, (int) (width / STEP))];
        }
    }

    private boolean holds(int i) { return i > inset / STEP && i < depth.length - 1; }

    public boolean land(float fx, float fy, float reach, float amount) {
        if (shaken) return false;
        int i = (int) ((fx - x) / STEP);
        if (i < 0 || !holds(i) || fy < y - depth[i] || fy > y - depth[i] + reach) return false;
        for (int k = -4; k <= 4; k++) if (i + k >= 0 && holds(i + k)) depth[i + k] = Math.min(max, depth[i + k] + amount * (float) Math.exp(-k * k / 6f));
        return true;
    }

    public void settle(float seconds) {
        float f = Math.min(.5f, seconds * 2);
        for (int i = 1; i < depth.length - 1; i++) if (holds(i - 1) && holds(i) && holds(i + 1)) depth[i] += ((depth[i - 1] + depth[i + 1]) / 2 - depth[i]) * f;
    }

    public void clear() { Arrays.fill(depth, 0); }

    public void draw(Draw d, float alpha) {
        int c = dev.hollow.core.Colors.with(0xFFFFFF, alpha);
        for (int i = 0; i < depth.length - 1; i++) {
            float a = depth[i], b = depth[i + 1];
            if (a < .06f && b < .06f) continue;
            float xa = x + i * STEP;
            d.quad(xa, y - a, c, xa, y + .35f, c, xa + STEP, y + .35f, c, xa + STEP, y - b, c);
        }
    }
}
