package dev.hollow.ui;

/** A value that glides or springs towards a target. With motion reduced it jumps there. */
public final class Anim {
    public float v;
    private float velocity;

    public Anim() {}

    public Anim(float start) { v = start; }

    /** Linear over the given seconds; pass the result through an easing. */
    public float to(float target, float seconds) {
        float s = Frame.dt / seconds;
        v = v < target ? Math.min(target, v + s) : Math.max(target, v - s);
        return v;
    }

    public float to(boolean on, float seconds) { return to(on ? 1 : 0, seconds); }

    public float glide(float target, float speed) { return v = Ease.approach(v, target, speed, Frame.dt); }

    /** Overshoots a little, then settles. */
    public float spring(float target, float stiffness, float damping) {
        float left = Math.min(Frame.dt, .1f);
        if (Frame.dt > 1) {
            v = target;
            velocity = 0;
            return v;
        }
        while (left > 0) {
            float h = Math.min(left, 1 / 240f);
            velocity += ((target - v) * stiffness - velocity * damping) * h;
            v += velocity * h;
            left -= h;
        }
        return v;
    }

    public void snap(float value) {
        v = value;
        velocity = 0;
    }
}
