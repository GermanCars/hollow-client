package dev.hollow.ui;

import dev.hollow.module.Interface;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/** Time and scale for the frame being drawn. Moved on once per frame. */
public final class Frame {
    /** Seconds since the last frame, scaled by the animation speed setting. */
    public static float dt;
    /** Seconds since the last frame, unscaled. */
    public static float realDt;
    /** Seconds of animation time, wrapping after an hour. */
    public static float time;
    /** Screen pixels per GUI unit. */
    public static float scale = 2;
    private static long last = Util.getMillis();

    private Frame() {}

    public static void next() {
        long now = Util.getMillis();
        realDt = Math.clamp((now - last) / 1000f, 0f, .1f);
        last = now;
        dt = Interface.calm() ? 10 : realDt * Interface.speed();
        time = (time + (Interface.calm() ? 0 : realDt)) % 3600;
        scale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
        dev.hollow.render.Glass.prepare();
    }
}
