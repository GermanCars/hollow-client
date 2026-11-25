package dev.hollow.ui;

import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/** The northern night behind every menu: hollow moon, aurora, mountains, spruces, snow. */
public final class Scene {
    private static final Scene INSTANCE = new Scene();

    public final Snow snow = new Snow();
    private final Anim mx = new Anim(), my = new Anim();
    private float w, h;
    private long gustStart = -10_000;
    private boolean gustBack;

    private Scene() {}

    public static Scene get() { return INSTANCE; }

    /** A cheap, steady noise in 0..1. */
    private static float rnd(int i, int salt) {
        int n = i * 374761393 + salt * 668265263;
        n = (n ^ n >>> 13) * 1274126177;
        return ((n ^ n >>> 16) & 0xFFFF) / 65535f;
    }

    /** Smooth 1D noise in -1..1. */
    private static float wave(float x, int salt) {
        int i = (int) Math.floor(x);
        float f = x - i;
        return Ease.lerp(rnd(i, salt), rnd(i + 1, salt), f * f * (3 - 2 * f)) * 2 - 1;
    }

    public void gust(boolean back) {
        if (Interface.calm()) return;
        gustStart = Util.getMillis();
        gustBack = back;
        snow.wind = back ? -9 : 12;
    }

    /** A wall of blown snow drawn over everything, hiding the switch between two screens. */
    public void drawGust(GuiGraphicsExtractor g) {
        float t = (Util.getMillis() - gustStart) / 820f;
        if (t < 0 || t >= 1) return;
        g.nextStratum();
        float w = g.guiWidth(), h = g.guiHeight(), e = Ease.inOut(t), dir = gustBack ? -1 : 1, mid = w / 2 + dir * (e * 3.3f - 1.65f) * w;
        Draw d = new Draw(g);
        int bands = 26;
        for (int i = 0; i < bands; i++) {
            float y0 = h * i / bands, y1 = h * (i + 1) / bands + .5f, off = (wave(i * .5f, 3) * .22f + wave(i * 1.7f, 4) * .1f) * w + (h / 2 - y0) * .35f, half = w * (.82f + wave(i * .9f, 5) * .1f), soft = w * (.22f + rnd(i, 6) * .2f);
            float l = mid + off - half, r = mid + off + half;
            int s = 0xFFF3F8FA, z = 0x00F3F8FA;
            d.gradientX(l - soft, y0, soft, y1 - y0, z, s);
            d.rect(l, y0, r - l, y1 - y0, s);
            d.gradientX(r, y0, soft, y1 - y0, s, z);
        }
        d.flush();
    }

    public float shiftX(float strength) { return mx.v * w * strength; }

    public float shiftY(float strength) { return my.v * h * strength; }

    public void begin(GuiGraphicsExtractor g) {
        w = g.guiWidth();
        h = g.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        boolean calm = Interface.calm();
        mx.glide(calm ? 0 : Math.clamp((float) mc.mouseHandler.getScaledXPos(mc.getWindow()) / w - .5f, -.5f, .5f), 5);
        my.glide(calm ? 0 : Math.clamp((float) mc.mouseHandler.getScaledYPos(mc.getWindow()) / h - .5f, -.5f, .5f), 5);
        snow.update(w, h, Hollow.INTERFACE.snowfall.getF());
    }

    /** Everything behind the menu's own content. */
    public void drawBack(GuiGraphicsExtractor g) {
        if (Interface.cavern()) {
            dev.hollow.render.Cavern.update();
            Draw c = new Draw(g);
            snow.draw(c, 0, .62f, 0, .55f);
            c.flush();
            return;
        }
        Draw d = new Draw(g);
        int accent = Theme.accent(), second = Theme.second();
        int top = Colors.mix(0xFF04070C, second, .05f), mid = Colors.mix(0xFF0A131E, second, .2f), low = Colors.mix(0xFF1D2C38, accent, .3f);
        d.gradient(0, 0, w, h * .45f, top, mid);
        d.gradient(0, h * .45f, w, h * .33f, mid, low);
        d.rect(0, h * .78f, w, h * .22f, low);
        stars(d);
        moon(d, accent);
        if (Hollow.INTERFACE.aurora.get()) aurora(d);
        peaks(d, .62f, .30f, 5, 11, Colors.mix(0xFF2A3A47, second, .25f), Colors.mix(0xFF1A2833, second, .2f), .006f, accent);
        mist(d, h * .60f, h * .22f, Colors.with(low, .55f));
        peaks(d, .74f, .24f, 7, 23, Colors.mix(0xFF1B2832, second, .16f), Colors.mix(0xFF111B23, second, .1f), .014f, accent);
        snow.draw(d, 0, .25f, shiftX(.02f), 1);
        hills(d, .83f, .05f, .026f, 31, Colors.mix(0xFF0E171E, second, .1f), Colors.mix(0xFFB8C9D2, accent, .16f), 9);
        mist(d, h * .78f, h * .2f, Colors.with(Colors.mix(low, 0xFFFFFFFF, .1f), .22f));
        ground(d);
        snow.draw(d, .25f, .62f, shiftX(.03f), 1);
        d.flush();
    }

    /** Snow in front of the menu. */
    public void drawFront(GuiGraphicsExtractor g) {
        Draw d = new Draw(g);
        snow.drawSettled(d, 1);
        snow.draw(d, .62f, 2, shiftX(.04f), .9f);
        // Dark edges keep the eye on the middle.
        d.gradient(0, 0, w, h * .16f, 0x50000000, 0);
        d.gradient(0, h * .86f, w, h * .14f, 0, 0x78000000);
        d.flush();
    }

    private void stars(Draw d) {
        for (int i = 0; i < 150; i++) {
            float x = rnd(i, 1) * w * 1.04f - shiftX(.004f) - w * .02f, y = rnd(i, 2) * h * .62f - shiftY(.004f), r = .25f + rnd(i, 3) * rnd(i, 4) * .75f;
            float twinkle = .45f + .55f * (float) Math.sin(Frame.time * (.5f + rnd(i, 5) * 1.6f) + i);
            int c = Colors.with(0xFFEAF6FF, Math.max(.06f, (.25f + rnd(i, 6) * .65f) * twinkle * (1 - y / (h * .7f))));
            d.poly(Draw.diamond(x, y, r * 1.5f), c);
        }
        // Now and then one falls.
        float cycle = Frame.time / 9, k = cycle - (float) Math.floor(cycle);
        if (k < .07f) {
            int n = (int) Math.floor(cycle);
            float p = k / .07f, sx = w * (.2f + rnd(n, 8) * .7f), sy = h * (.05f + rnd(n, 9) * .2f), dx = -w * .2f, dy = h * .13f, tail = .25f;
            float fade = (float) Math.sin(p * Math.PI);
            for (int i = 0; i < 6; i++) {
                float a = p - tail * (i + 1) / 6, b = p - tail * i / 6;
                d.line(sx + dx * a, sy + dy * a, sx + dx * b, sy + dy * b, .9f, Colors.with(0xFFFFFF, fade * (1 - i / 6f) * .9f));
            }
        }
    }

    /** A square moon with its middle missing. */
    private void moon(Draw d, int accent) {
        float s = h * .052f, cx = w * .93f - shiftX(.008f), cy = h * .36f - shiftY(.008f);
        int c = Colors.mix(0xFFF6FBFA, accent, .14f);
        d.glow(cx, cy, s * 5.2f, s * 5.2f, Colors.with(accent, .20f), Colors.with(accent, 0), 48);
        d.glow(cx, cy, s * 2.2f, s * 2.2f, Colors.with(c, .22f), Colors.with(c, 0), 48);
        float[] ring = Draw.shard(cx - s, cy - s, s * 2, s * 2, s * .42f);
        d.stroke(Draw.offset(ring, -s * .27f), s * .54f, (x, y) -> Colors.mix(c, accent, Math.clamp((y - cy + s) / (s * 2) * .5f, 0, 1)), 0, 1);
    }

    private void aurora(Draw d) {
        int cols = 96;
        for (int r = 0; r < 3; r++) {
            float base = h * (.33f + r * .09f), amp = h * (.075f - r * .012f), drift = Frame.time * (.045f + r * .018f);
            float[] ys = new float[cols + 1], hs = new float[cols + 1];
            int[] cs = new int[cols + 1];
            for (int i = 0; i <= cols; i++) {
                float u = i / (float) cols;
                ys[i] = base + wave(u * 3.2f + drift, 40 + r) * amp + wave(u * 8 - drift * 1.6f, 50 + r) * amp * .28f - shiftY(.012f) - u * h * .05f;
                float streak = .55f + .45f * wave(u * 46 + drift * 7, 60 + r), patch = Ease.clamp(wave(u * 2.1f - drift * .9f, 70 + r) * .9f + .5f), ends = Ease.clamp(Math.min(u, 1 - u) * 7);
                hs[i] = h * (.16f + .22f * patch) * (.7f + streak * .4f);
                cs[i] = Colors.with(Theme.at(Ease.clamp(u * .8f + r * .22f + wave(u * 2 + drift, 80) * .25f)), (.10f + .46f * streak) * patch * ends * (1 - r * .16f));
            }
            float sx = shiftX(.014f + r * .006f);
            for (int i = 0; i < cols; i++) {
                float x0 = w * 1.1f * i / cols - w * .05f - sx, x1 = w * 1.1f * (i + 1) / cols - w * .05f - sx;
                float lean = h * .06f;
                int z0 = cs[i] & 0xFFFFFF, z1 = cs[i + 1] & 0xFFFFFF;
                // Bright foot, long fading curtain, short glow underneath.
                d.quad(x0 + lean, ys[i] - hs[i], z0, x0, ys[i], cs[i], x1, ys[i + 1], cs[i + 1], x1 + lean, ys[i + 1] - hs[i + 1], z1);
                d.quad(x0, ys[i], cs[i], x0, ys[i] + h * .035f, z0, x1, ys[i + 1] + h * .035f, z1, x1, ys[i + 1], cs[i + 1]);
            }
        }
    }

    /** Faceted mountains, lit from the moon's side, with snowy tops. */
    private void peaks(Draw d, float line, float tall, int count, int salt, int lit, int dark, float move, int accent) {
        float base = h * line + h * .2f - shiftY(move * .5f), span = w * 1.3f / count, ox = -w * .15f - shiftX(move);
        int cap = Colors.mix(Colors.mix(lit, 0xFFE9F3F6, .62f), accent, .08f), capDark = Colors.mix(dark, 0xFFB9CBD6, .42f);
        for (int i = -1; i <= count; i++) {
            float cx = ox + span * (i + .5f + (rnd(i + 2, salt) - .5f) * .5f), ph = h * tall * (.5f + rnd(i + 2, salt + 1) * .65f), half = span * (.72f + rnd(i + 2, salt + 2) * .4f), top = h * line - ph * .75f - shiftY(move * .5f), ridge = cx + span * (rnd(i + 2, salt + 3) - .5f) * .16f;
            float k = (base - top);
            float foot = cx + half * .12f;
            d.poly(new float[] {ridge, top, cx - half, base, foot, base}, lit);
            d.poly(new float[] {ridge, top, foot, base, cx + half, base}, dark);
            // Snow line with a ragged lower edge.
            float s = .34f + rnd(i + 2, salt + 4) * .1f;
            float lx = Ease.lerp(ridge, cx - half, s), ly = top + k * s, mx2 = Ease.lerp(ridge, foot, s * .82f), my2 = top + k * s * .82f, rx = Ease.lerp(ridge, cx + half, s * .9f), ry = top + k * s * .9f;
            float jx = Ease.lerp(lx, mx2, .45f), jy = Ease.lerp(ly, my2, .45f) - k * .07f, qx = Ease.lerp(mx2, rx, .5f), qy = Ease.lerp(my2, ry, .5f) - k * .06f;
            d.poly(new float[] {ridge, top, lx, ly, jx, jy}, cap);
            d.poly(new float[] {ridge, top, jx, jy, mx2, my2}, cap);
            d.poly(new float[] {ridge, top, mx2, my2, qx, qy}, capDark);
            d.poly(new float[] {ridge, top, qx, qy, rx, ry}, capDark);
        }
    }

    private void mist(Draw d, float y, float tall, int c) {
        d.gradient(0, y, w, tall * .6f, c & 0xFFFFFF, c);
        d.gradient(0, y + tall * .6f, w, tall * .4f, c, c & 0xFFFFFF);
    }

    /** Blocky hills with spruces on them. */
    private void hills(Draw d, float line, float block, float move, int salt, int body, int cap, int trees) {
        float b = Math.max(5, h * block * .8f), ox = -shiftX(move) - b * 3, base = h * line - shiftY(move * .4f);
        int count = (int) Math.ceil((w + b * 6) / b);
        for (int i = 0; i < count; i++) {
            float y = base + Math.round(wave(i * .13f, salt) * 2.2f + wave(i * .4f, salt + 1)) * b * .7f;
            float x = ox + i * b;
            if (rnd(i, salt + 2) < trees / 24f) spruce(d, x + b / 2, y, b * (1.5f + rnd(i, salt + 3) * 1.7f), body, cap);
            d.rect(x, y, b + .4f, h - y + 10, body);
            d.gradient(x, y, b + .4f, Math.max(1, b * .3f), cap, Colors.mix(body, cap, .35f));
        }
    }

    private void spruce(Draw d, float x, float y, float tall, int body, int cap) {
        int tiers = 4;
        d.rect(x - tall * .035f, y - tall * .2f, tall * .07f, tall * .22f, body);
        for (int i = 0; i < tiers; i++) {
            float t = i / (float) tiers, half = tall * (.30f - t * .24f), ty = y - tall * (.18f + t * .78f), th = tall * .2f;
            d.poly(new float[] {x - half * .45f, ty - th, x + half * .45f, ty - th, x + half, ty, x - half, ty}, body);
            d.poly(new float[] {x - half * .45f, ty - th, x + half * .45f, ty - th, x + half * .62f, ty - th * .62f, x - half * .62f, ty - th * .62f}, Colors.with(cap, .75f));
        }
    }

    /** The near snowfield. Its height is where the player stands. */
    public float groundY(float x) {
        float u = x / w;
        return h * (.905f - .035f * Ease.inOut(Ease.clamp((u - .5f) * 3.2f)) + .012f * wave(u * 3.1f, 90));
    }

    private void ground(Draw d) {
        int top = Colors.mix(0xFFDCE8ED, Theme.accent(), .10f), low = Colors.mix(0xFF6F8796, Theme.second(), .3f);
        int n = 48;
        for (int i = 0; i < n; i++) {
            float x0 = w * i / n, x1 = w * (i + 1) / n + .3f, y0 = groundY(x0), y1 = groundY(x1);
            int c0 = Colors.mix(top, low, .25f + .25f * wave(i * .33f, 91)), c1 = Colors.mix(top, low, .25f + .25f * wave((i + 1) * .33f, 91));
            d.quad(x0, y0, c0, x0, h + 4, low, x1, h + 4, low, x1, y1, c1);
            d.quad(x0, y0 - .6f, top & 0xFFFFFF, x0, y0 + .6f, top, x1, y1 + .6f, top, x1, y1 - .6f, top & 0xFFFFFF);
        }
    }
}
