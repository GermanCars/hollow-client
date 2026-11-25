package dev.hollow.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.render.Glass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;

/** Frosted glass: what's behind it, blurred, bent at the rim, with light along the edge, frost in the corners and cracks where it's pressed. */
public final class Ice {
    private static final Identifier FROST = Identifier.fromNamespaceAndPath("hollow", "textures/frost.png");

    private Ice() {}

    // ------------------------------------------------------------------ glass

    /** Triangles and quads textured with the blurred picture. */
    private static final class Mesh {
        float[] v = new float[512];
        int[] c = new int[128];
        int n;

        void put(float x, float y, float u, float w, int color) {
            if (n + 1 > c.length) {
                c = Arrays.copyOf(c, c.length * 2);
                v = Arrays.copyOf(v, v.length * 2);
            }
            v[n * 4] = x;
            v[n * 4 + 1] = y;
            v[n * 4 + 2] = u;
            v[n * 4 + 3] = w;
            c[n++] = color;
        }

        /** Four corners as x, y, u, v. Wound the way the pipeline shows. */
        void quad(float[] a, float[] b, float[] d, float[] e, int ca, int cb, int cd, int ce) {
            float area = (d[0] - a[0]) * (e[1] - b[1]) - (e[0] - b[0]) * (d[1] - a[1]);
            if (area <= 0) {
                put(a[0], a[1], a[2], a[3], ca);
                put(b[0], b[1], b[2], b[3], cb);
                put(d[0], d[1], d[2], d[3], cd);
                put(e[0], e[1], e[2], e[3], ce);
            } else {
                put(e[0], e[1], e[2], e[3], ce);
                put(d[0], d[1], d[2], d[3], cd);
                put(b[0], b[1], b[2], b[3], cb);
                put(a[0], a[1], a[2], a[3], ca);
            }
        }

        void submit(GuiGraphicsExtractor g, TextureSetup t) {
            if (n == 0) return;
            Matrix3x2f pose = new Matrix3x2f(g.pose());
            float x0 = Float.MAX_VALUE, y0 = x0, x1 = -x0, y1 = -x0;
            for (int i = 0; i < n; i++) {
                float x = v[i * 4], y = v[i * 4 + 1], tx = pose.m00 * x + pose.m10 * y + pose.m20, ty = pose.m01 * x + pose.m11 * y + pose.m21;
                x0 = Math.min(x0, tx);
                y0 = Math.min(y0, ty);
                x1 = Math.max(x1, tx);
                y1 = Math.max(y1, ty);
            }
            ScreenRectangle b = new ScreenRectangle((int) Math.floor(x0), (int) Math.floor(y0), (int) Math.ceil(x1 - Math.floor(x0)), (int) Math.ceil(y1 - Math.floor(y0))), s = g.scissorStack.peek();
            if (s != null) b = s.intersection(b);
            if (b != null) g.guiRenderState.addGuiElement(new Element(Arrays.copyOf(v, n * 4), Arrays.copyOf(c, n), pose, t, s, b));
        }
    }

    private record Element(float[] v, int[] c, Matrix3x2f pose, TextureSetup textureSetup, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer out) {
            for (int i = 0; i < c.length; i++) out.addVertexWith2DPose(pose, v[i * 4], v[i * 4 + 1]).setUv(v[i * 4 + 2], v[i * 4 + 3]).setColor(c[i]);
        }

        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }
    }

    /** Looks up where a point lies on screen, in texture coordinates. */
    private static final class Lookup {
        final Matrix3x2f m;
        final float w, h;

        Lookup(GuiGraphicsExtractor g) {
            m = new Matrix3x2f(g.pose());
            w = g.guiWidth();
            h = g.guiHeight();
        }

        float[] at(float x, float y, float lookX, float lookY) {
            float sx = m.m00 * lookX + m.m10 * lookY + m.m20, sy = m.m01 * lookX + m.m11 * lookY + m.m21;
            return new float[] {x, y, Math.clamp(sx / w, .002f, .998f), Math.clamp(1 - sy / h, .002f, .998f)};
        }
    }

    /**
     * The glass alone.
     *
     * @param bend how wide the curved rim is
     */
    public static void glass(GuiGraphicsExtractor g, float[] p, float alpha, float bend) {
        if (alpha <= .004f) return;
        TextureSetup t = Glass.texture();
        if (t == null) {
            Draw d = new Draw(g);
            d.poly(p, Colors.with(Theme.ink(), .82f * alpha));
            d.flush();
            return;
        }
        Lookup look = new Lookup(g);
        Mesh mesh = new Mesh();
        int k = p.length / 2, full = Colors.with(0xFFFFFF, alpha), rim = Colors.with(0xFFFFFF, alpha);
        float px = 1 / Frame.scale;
        float[] in = Draw.offset(p, -bend), mid = Draw.offset(p, -bend * .4f), out = Draw.offset(p, px);
        float[] centre = new float[2];
        for (int i = 0; i < k; i++) {
            centre[0] += in[i * 2] / k;
            centre[1] += in[i * 2 + 1] / k;
        }
        float[] c = look.at(centre[0], centre[1], centre[0], centre[1]);
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            float[] a0 = look.at(in[i * 2], in[i * 2 + 1], in[i * 2], in[i * 2 + 1]), a1 = look.at(in[j * 2], in[j * 2 + 1], in[j * 2], in[j * 2 + 1]);
            mesh.quad(c, a0, a1, c, full, full, full, full);
            // Towards the rim the light comes from further and further inside.
            float[] b0 = bent(look, mid, in, i, 1.1f), b1 = bent(look, mid, in, j, 1.1f), e0 = bent(look, p, in, i, 1.9f), e1 = bent(look, p, in, j, 1.9f);
            mesh.quad(a0, b0, b1, a1, full, full, full, full);
            mesh.quad(b0, e0, e1, b1, full, rim, rim, full);
            float[] f0 = look.at(out[i * 2], out[i * 2 + 1], p[i * 2], p[i * 2 + 1]), f1 = look.at(out[j * 2], out[j * 2 + 1], p[j * 2], p[j * 2 + 1]);
            mesh.quad(e0, f0, f1, e1, rim, rim & 0xFFFFFF, rim & 0xFFFFFF, rim);
        }
        mesh.submit(g, t);
    }

    private static float[] bent(Lookup look, float[] at, float[] in, int i, float by) {
        float x = at[i * 2], y = at[i * 2 + 1], ix = in[i * 2], iy = in[i * 2 + 1];
        return look.at(x, y, x + (ix - x) * by * 1.6f, y + (iy - y) * by * 1.6f);
    }

    /** Light from the top left running round the rim. */
    public static void rim(Draw d, float[] p, float alpha, float strength) {
        float x0 = Float.MAX_VALUE, y0 = x0, x1 = -x0, y1 = -x0;
        for (int i = 0; i < p.length; i += 2) {
            x0 = Math.min(x0, p[i]);
            x1 = Math.max(x1, p[i]);
            y0 = Math.min(y0, p[i + 1]);
            y1 = Math.max(y1, p[i + 1]);
        }
        float fx = x0, fy = y0, w = Math.max(1, x1 - x0), h = Math.max(1, y1 - y0);
        int lit = Colors.mix(0xFFFFFFFF, Theme.accent(), .25f), low = Theme.accent();
        Draw.Shade shade = (x, y) -> {
            float u = (x - fx) / w, v = (y - fy) / h;
            float tl = Ease.clamp(1 - (u * .55f + v * 1.25f)), br = Ease.clamp((u * .6f + v * .9f) - 1.05f);
            return Colors.with(Colors.mix(low, lit, tl), (.14f + .72f * tl * tl + .42f * br) * strength * alpha);
        };
        d.stroke(Draw.offset(p, -.5f), 1, shade, 0, 1);
        d.stroke(Draw.offset(p, -1.5f), .7f, (x, y) -> Colors.with(0xFFFFFF, .07f * strength * alpha * Ease.clamp(1 - (y - fy) / h)), 0, 1);
    }

    /** The colour of the ice itself. */
    static void cast(Draw d, float[] p, float strength) { d.poly(p, Colors.with(Colors.mix(Theme.ink(), Theme.second(), .34f), .34f * strength)); }

    /** How a panel is made. Tint is how dark the glass is, frost how far it has grown. */
    public record Look(float alpha, float tint, float frost, float rim, float shadow) {}

    public static void panel(GuiGraphicsExtractor g, float[] p, float x, float y, float w, float h, Look k) {
        if (k.alpha <= .004f) return;
        if (k.shadow > 0) {
            Draw s = new Draw(g);
            s.shadow(Draw.offset(p, -1), k.shadow, Colors.with(0x01060C, .34f * k.alpha));
            s.flush();
        }
        glass(g, p, k.alpha, Math.min(7, Math.min(w, h) * .2f));
        Draw d = new Draw(g);
        float tint = k.tint * Interface.panelAlpha() * k.alpha;
        int ink = Theme.ink();
        // Ice turns whatever is behind it blue.
        cast(d, p, k.alpha * Math.min(1, .35f + k.tint));
        d.poly(p, (px, py) -> Colors.with(Colors.mix(ink, Theme.second(), .12f * Ease.clamp(1 - (py - y) / h)), tint * (.86f + .14f * Ease.clamp((py - y) / h))));
        // A soft sheen across the top.
        d.poly(Draw.clip(p, 0, -1, -(y + Math.min(h, 26))), (px, py) -> Colors.with(0xFFFFFF, .075f * k.alpha * Ease.clamp(1 - (py - y) / 26)));
        d.flush();
        if (k.frost > .01f && Interface.frost() > 0) frost(g, p, x, y, w, h, k.frost, k.alpha * Interface.frost());
        Draw r = new Draw(g);
        rim(r, p, k.alpha, k.rim);
        r.flush();
    }

    /** Feathers of ice in two opposite corners, clipped to the shape. */
    private static void frost(GuiGraphicsExtractor g, float[] p, float x, float y, float w, float h, float grown, float alpha) {
        var tex = Minecraft.getInstance().getTextureManager().getTexture(FROST);
        if (tex == null) return;
        TextureSetup t = TextureSetup.singleTexture(tex.getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        Mesh m = new Mesh();
        float size = Math.clamp(Math.min(w, h) * 1.15f, 14, 120) * (.45f + .55f * Ease.outCubic(grown));
        corner(m, p, x, y, size, 1, 1, .5f * alpha * grown);
        corner(m, p, x + w, y + h, size * .92f, -1, -1, .42f * alpha * grown);
        corner(m, p, x + w, y, size * .6f, -1, 1, .26f * alpha * grown);
        m.submit(g, t);
    }

    private static void corner(Mesh m, float[] p, float cx, float cy, float size, int sx, int sy, float alpha) {
        float[] c = Draw.clip(Draw.clip(p, -sx, 0, -(cx * sx + size)), 0, -sy, -(cy * sy + size));
        int k = c.length / 2, col = Colors.with(Colors.mix(0xFFFFFFFF, Theme.accent(), .18f), alpha);
        for (int i = 1; i + 1 < k; i++) m.quad(uv(c, 0, cx, cy, size, sx, sy), uv(c, i, cx, cy, size, sx, sy), uv(c, i + 1, cx, cy, size, sx, sy), uv(c, 0, cx, cy, size, sx, sy), col, col, col, col);
    }

    private static float[] uv(float[] c, int i, float cx, float cy, float size, int sx, int sy) { return new float[] {c[i * 2], c[i * 2 + 1], Ease.clamp((c[i * 2] - cx) * sx / size), Ease.clamp((c[i * 2 + 1] - cy) * sy / size)}; }

    /** A streak of light crossing a shape, t from 0 to 1. */
    public static void glint(Draw d, float[] p, float x, float w, float y, float h, float t, float alpha) {
        if (t <= 0 || t >= 1) return;
        float lean = h * .6f, at = x - 30 + (w + lean + 60) * Ease.inOut(t);
        for (int i = 0; i < 2; i++) {
            float band = i == 0 ? 16 : 5;
            float[] c = Draw.clip(Draw.clip(p, 1, .6f, at - band + y * .6f), -1, -.6f, -(at + band + y * .6f));
            int strip = i;
            if (c.length >= 6) d.poly(c, (px, py) -> Colors.with(0xFFFFFF, (strip == 0 ? .07f : .16f) * alpha * (1 - Math.abs(px + (py - y) * .6f - at) / band)), .01f);
        }
    }

    // ----------------------------------------------------------------- cracks

    private record Crack(float[] lines, long start, float reach) {}

    private static final Map<Object, List<Crack>> CRACKS = new WeakHashMap<>();
    private static final Random RANDOM = new Random();

    /** Cracks run out from a point. Owner is whatever was pressed. */
    public static void crack(Object owner, float x, float y, float reach) {
        if (Interface.calm()) return;
        List<Float> l = new ArrayList<>();
        int arms = 6 + RANDOM.nextInt(3);
        for (int i = 0; i < arms; i++) branch(l, x, y, (float) (Math.PI * 2 * (i + RANDOM.nextFloat() * .7f) / arms), reach * (.55f + RANDOM.nextFloat() * .6f), 0, 2);
        // A couple of rings joining the arms.
        for (int r = 1; r <= 2; r++) {
            float rad = reach * .16f * r;
            for (int i = 0; i < arms; i++) if (RANDOM.nextFloat() < .6f) {
                double a = Math.PI * 2 * i / arms, b = Math.PI * 2 * (i + .8) / arms;
                add(l, x + (float) Math.cos(a) * rad, y + (float) Math.sin(a) * rad, x + (float) Math.cos(b) * rad, y + (float) Math.sin(b) * rad, rad / reach);
            }
        }
        float[] out = new float[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = l.get(i);
        List<Crack> list = CRACKS.computeIfAbsent(owner, k -> new ArrayList<>());
        if (list.size() > 3) list.removeFirst();
        list.add(new Crack(out, Util.getMillis(), reach));
    }

    private static void add(List<Float> l, float a, float b, float c, float d, float when) {
        l.add(a);
        l.add(b);
        l.add(c);
        l.add(d);
        l.add(when);
    }

    private static void branch(List<Float> l, float x, float y, float a, float len, float done, int depth) {
        int steps = 3 + RANDOM.nextInt(3);
        for (int i = 0; i < steps; i++) {
            a += (RANDOM.nextFloat() - .5f) * .75f;
            float nx = x + (float) Math.cos(a) * len / steps, ny = y + (float) Math.sin(a) * len / steps;
            add(l, x, y, nx, ny, done + (1 - done) * i / steps);
            x = nx;
            y = ny;
            if (depth > 0 && RANDOM.nextFloat() < .4f) branch(l, x, y, a + (RANDOM.nextBoolean() ? .75f : -.75f), len * .42f, done + (1 - done) * (i + 1) / steps, depth - 1);
        }
    }

    public static void drawCracks(GuiGraphicsExtractor g, Object owner, float x, float y, float w, float h, float alpha) {
        List<Crack> list = CRACKS.get(owner);
        if (list == null || list.isEmpty()) return;
        long now = Util.getMillis();
        list.removeIf(c -> now - c.start > 1500);
        g.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + w), (int) Math.ceil(y + h));
        Draw d = new Draw(g);
        for (Crack c : list) {
            float t = (now - c.start) / 1500f, spread = Ease.outQuint(t * 5), fade = (1 - Ease.inOut(Ease.clamp(t * 1.5f - .5f))) * alpha;
            for (int i = 0; i + 4 < c.lines.length; i += 5) {
                float when = c.lines[i + 4];
                if (when > spread) continue;
                float part = Ease.clamp((spread - when) * 6), ex = Ease.lerp(c.lines[i], c.lines[i + 2], part), ey = Ease.lerp(c.lines[i + 1], c.lines[i + 3], part);
                d.line(c.lines[i] + .5f, c.lines[i + 1] + .6f, ex + .5f, ey + .6f, .8f, Colors.with(0, .35f * fade));
                d.line(c.lines[i], c.lines[i + 1], ex, ey, .75f * (1.2f - when * .7f), Colors.with(0xFFFFFF, .9f * fade));
            }
        }
        d.flush();
        g.disableScissor();
    }

    // ---------------------------------------------------------------- shatter

    private static final class Shard {
        float[] pts;
        float cx, cy, vx, vy, spin, zoom;
    }

    private static final List<Shard> SHARDS = new ArrayList<>();
    private static long shatteredAt = -10_000;

    /** Breaks a rectangle of glass from a point; the pieces fly past the viewer. */
    public static void shatter(float x, float y, float w, float h, float px, float py) {
        if (Interface.calm()) return;
        SHARDS.clear();
        shatteredAt = Util.getMillis();
        int nx = Math.clamp(Math.round(w / 15), 3, 14), ny = Math.clamp(Math.round(h / 9), 2, 8);
        float[][][] v = new float[nx + 1][ny + 1][];
        for (int i = 0; i <= nx; i++) for (int j = 0; j <= ny; j++) {
            boolean ex = i == 0 || i == nx, ey = j == 0 || j == ny;
            v[i][j] = new float[] {x + w * (i + (ex ? 0 : (RANDOM.nextFloat() - .5f) * .8f)) / nx, y + h * (j + (ey ? 0 : (RANDOM.nextFloat() - .5f) * .8f)) / ny};
        }
        for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) {
            float[] a = v[i][j], b = v[i + 1][j], c = v[i + 1][j + 1], e = v[i][j + 1];
            if (RANDOM.nextBoolean()) {
                piece(px, py, a, b, c);
                piece(px, py, a, c, e);
            } else {
                piece(px, py, a, b, e);
                piece(px, py, b, c, e);
            }
        }
    }

    private static void piece(float px, float py, float[]... p) {
        Shard s = new Shard();
        s.cx = (p[0][0] + p[1][0] + p[2][0]) / 3;
        s.cy = (p[0][1] + p[1][1] + p[2][1]) / 3;
        s.pts = new float[] {p[0][0] - s.cx, p[0][1] - s.cy, p[1][0] - s.cx, p[1][1] - s.cy, p[2][0] - s.cx, p[2][1] - s.cy};
        float dx = s.cx - px, dy = s.cy - py, dist = (float) Math.hypot(dx, dy) + 8, push = 150 + 2600 / dist;
        s.vx = dx / dist * push * (.5f + RANDOM.nextFloat()) + (RANDOM.nextFloat() - .5f) * 40;
        s.vy = dy / dist * push * (.5f + RANDOM.nextFloat()) - 30 - RANDOM.nextFloat() * 60;
        s.spin = (RANDOM.nextFloat() - .5f) * 11;
        s.zoom = .6f + RANDOM.nextFloat() * 2.4f;
        SHARDS.add(s);
    }

    public static void drawShards(GuiGraphicsExtractor g) {
        float t = (Util.getMillis() - shatteredAt) / 1000f;
        if (t >= 1 || SHARDS.isEmpty()) {
            SHARDS.clear();
            return;
        }
        g.nextStratum();
        float fade = 1 - Ease.inOut(Ease.clamp(t * 1.7f - .7f));
        Draw d = new Draw(g);
        TextureSetup tex = Glass.texture();
        Lookup look = new Lookup(g);
        Mesh mesh = new Mesh();
        for (Shard s : SHARDS) {
            double a = s.spin * t;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a), z = 1 + s.zoom * t * t * 1.6f, flip = (float) Math.cos(a * 1.4), ox = s.cx + s.vx * t, oy = s.cy + s.vy * t + 210 * t * t;
            float[] p = new float[6];
            for (int i = 0; i < 3; i++) {
                float lx = s.pts[i * 2] * z * flip, ly = s.pts[i * 2 + 1] * z;
                p[i * 2] = ox + lx * cos - ly * sin;
                p[i * 2 + 1] = oy + lx * sin + ly * cos;
            }
            int c = Colors.with(0xFFFFFF, fade);
            if (tex != null) {
                float[] u = look.at(p[0], p[1], p[0] + 14, p[1] - 8), v = look.at(p[2], p[3], p[2] + 14, p[3] - 8), w = look.at(p[4], p[5], p[4] + 14, p[5] - 8);
                mesh.quad(u, v, w, u, c, c, c, c);
            }
            float face = .10f + .5f * (float) Math.pow(Math.abs(Math.sin(a * 1.4 + s.cx)), 6);
            d.poly(p, Colors.with(Colors.mix(0xFFFFFFFF, Theme.accent(), .35f), face * fade));
            d.stroke(p, .7f, Colors.with(0xFFFFFF, .75f * fade));
        }
        if (tex != null) mesh.submit(g, tex);
        d.flush();
    }
}
