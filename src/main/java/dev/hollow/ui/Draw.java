package dev.hollow.ui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.hollow.core.Colors;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** Smooth-edged shapes at sub-pixel positions, collected into one GUI element. */
public final class Draw {
    public interface Shade { int at(float x, float y); }

    private final GuiGraphicsExtractor g;
    private final float px;
    private float[] xy = new float[256];
    private int[] col = new int[128];
    private int n;

    public Draw(GuiGraphicsExtractor g) {
        this.g = g;
        Matrix3x2f m = new Matrix3x2f(g.pose());
        float s = (float) Math.hypot(m.m00, m.m01);
        px = 1 / (Frame.scale * Math.max(.05f, s));
    }

    /** One screen pixel in drawing units. */
    public float px() { return px; }

    public void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        if (n + 4 > col.length) {
            col = Arrays.copyOf(col, col.length * 2);
            xy = Arrays.copyOf(xy, xy.length * 2);
        }
        // The pipeline hides back faces, so wind every quad the same way.
        float area = (x2 - x0) * (y3 - y1) - (x3 - x1) * (y2 - y0);
        if (area <= 0) {
            put(x0, y0, c0);
            put(x1, y1, c1);
            put(x2, y2, c2);
            put(x3, y3, c3);
        } else {
            put(x3, y3, c3);
            put(x2, y2, c2);
            put(x1, y1, c1);
            put(x0, y0, c0);
        }
    }

    private void put(float x, float y, int c) {
        xy[n * 2] = x;
        xy[n * 2 + 1] = y;
        col[n++] = c;
    }

    public void rect(float x, float y, float w, float h, int c) { gradient(x, y, w, h, c, c); }

    public void gradient(float x, float y, float w, float h, int top, int bottom) { quad(x, y, top, x, y + h, bottom, x + w, y + h, bottom, x + w, y, top); }

    public void gradientX(float x, float y, float w, float h, int left, int right) { quad(x, y, left, x, y + h, left, x + w, y + h, right, x + w, y, right); }

    /** Soft line with rounded-off edges. */
    public void line(float x0, float y0, float x1, float y1, float width, int c) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.hypot(dx, dy);
        if (len < 1e-4f) return;
        float nx = -dy / len, ny = dx / len, in = Math.max(0, width / 2 - px / 2), out = width / 2 + px / 2;
        int z = c & 0xFFFFFF;
        quad(x0 + nx * in, y0 + ny * in, c, x0 - nx * in, y0 - ny * in, c, x1 - nx * in, y1 - ny * in, c, x1 + nx * in, y1 + ny * in, c);
        quad(x0 + nx * out, y0 + ny * out, z, x0 + nx * in, y0 + ny * in, c, x1 + nx * in, y1 + ny * in, c, x1 + nx * out, y1 + ny * out, z);
        quad(x0 - nx * out, y0 - ny * out, z, x0 - nx * in, y0 - ny * in, c, x1 - nx * in, y1 - ny * in, c, x1 - nx * out, y1 - ny * out, z);
    }

    public void circle(float cx, float cy, float r, int c) {
        int sides = r * Frame.scale < 3 ? 8 : r * Frame.scale < 14 ? 16 : 40;
        float[] p = new float[sides * 2];
        for (int i = 0; i < sides; i++) {
            p[i * 2] = cx + (float) Math.cos(Math.PI * 2 * i / sides) * r;
            p[i * 2 + 1] = cy + (float) Math.sin(Math.PI * 2 * i / sides) * r;
        }
        poly(p, c);
    }

    /** A disc fading from the centre colour to the rim colour. */
    public void glow(float cx, float cy, float rx, float ry, int centre, int rim, int sides) {
        for (int i = 0; i < sides; i += 2) {
            double a = Math.PI * 2 * i / sides, b = Math.PI * 2 * (i + 1) / sides, d = Math.PI * 2 * (i + 2) / sides;
            quad(cx, cy, centre, cx + (float) Math.cos(d) * rx, cy + (float) Math.sin(d) * ry, rim, cx + (float) Math.cos(b) * rx, cy + (float) Math.sin(b) * ry, rim, cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry, rim);
        }
    }

    /** Convex, any winding. */
    public void poly(float[] p, int c) { poly(p, (x, y) -> c, px); }

    public void poly(float[] p, Shade c) { poly(p, c, px); }

    /** Feather is the width of the soft edge. */
    public void poly(float[] p, Shade c, float feather) {
        if (p.length < 6) return;
        float[] in = offset(p, -feather / 2), out = offset(p, feather / 2);
        int k = p.length / 2;
        for (int i = 1; i + 1 < k; i += 2) {
            int a = i, b = i + 1, d = Math.min(i + 2, k - 1);
            quad(in[0], in[1], c.at(in[0], in[1]), in[a * 2], in[a * 2 + 1], c.at(in[a * 2], in[a * 2 + 1]), in[b * 2], in[b * 2 + 1], c.at(in[b * 2], in[b * 2 + 1]), in[d * 2], in[d * 2 + 1], c.at(in[d * 2], in[d * 2 + 1]));
        }
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            int ci = c.at(in[i * 2], in[i * 2 + 1]), cj = c.at(in[j * 2], in[j * 2 + 1]);
            quad(in[i * 2], in[i * 2 + 1], ci, in[j * 2], in[j * 2 + 1], cj, out[j * 2], out[j * 2 + 1], cj & 0xFFFFFF, out[i * 2], out[i * 2 + 1], ci & 0xFFFFFF);
        }
    }

    /** Moves every edge of a closed shape outwards (or inwards when negative). */
    public static float[] offset(float[] p, float by) {
        int k = p.length / 2;
        float area = 0;
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
        }
        float sign = area > 0 ? 1 : -1;
        float[] out = new float[p.length];
        for (int i = 0; i < k; i++) {
            int a = (i + k - 1) % k, b = (i + 1) % k;
            float ax = p[i * 2] - p[a * 2], ay = p[i * 2 + 1] - p[a * 2 + 1], bx = p[b * 2] - p[i * 2], by2 = p[b * 2 + 1] - p[i * 2 + 1];
            float al = (float) Math.hypot(ax, ay), bl = (float) Math.hypot(bx, by2);
            if (al < 1e-5f) { ax = bx; ay = by2; al = bl; }
            if (bl < 1e-5f) { bx = ax; by2 = ay; bl = al; }
            if (al < 1e-5f) { out[i * 2] = p[i * 2]; out[i * 2 + 1] = p[i * 2 + 1]; continue; }
            float n1x = ay / al, n1y = -ax / al, n2x = by2 / bl, n2y = -bx / bl, mx = n1x + n2x, my = n1y + n2y, d = 1 + n1x * n2x + n1y * n2y;
            float f = by * sign / Math.max(.35f, d);
            out[i * 2] = p[i * 2] + mx * f;
            out[i * 2 + 1] = p[i * 2 + 1] + my * f;
        }
        return out;
    }

    public void stroke(float[] p, float width, int c) { stroke(p, width, (x, y) -> c, 0, 1); }

    /** Part of a closed outline, from one fraction of its length to another. Fractions may run past 1. */
    public void stroke(float[] p, float width, Shade c, float from, float to) {
        if (to - from <= 0) return;
        int k = p.length / 2;
        float[] len = new float[k + 1];
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            len[i + 1] = len[i] + (float) Math.hypot(p[j * 2] - p[i * 2], p[j * 2 + 1] - p[i * 2 + 1]);
        }
        float total = len[k], in = Math.max(0, width / 2 - px / 2), out = width / 2 + px / 2;
        if (total <= 0) return;
        float[] o2 = offset(p, out), o1 = offset(p, in), i1 = offset(p, -in), i2 = offset(p, -out);
        float a = from * total, b = Math.min(to * total, a + total);
        for (int turn = 0; turn <= 1; turn++) for (int i = 0; i < k; i++) {
            float s = len[i] + turn * total, e = len[i + 1] + turn * total;
            if (e <= a || s >= b || e - s < 1e-5f) continue;
            float t0 = Math.max(0, (a - s) / (e - s)), t1 = Math.min(1, (b - s) / (e - s));
            int j = (i + 1) % k;
            band(o2, o1, i, j, t0, t1, c, true, false);
            band(o1, i1, i, j, t0, t1, c, false, false);
            band(i1, i2, i, j, t0, t1, c, false, true);
        }
    }

    private void band(float[] u, float[] v, int i, int j, float t0, float t1, Shade c, boolean fadeU, boolean fadeV) {
        float ux0 = Ease.lerp(u[i * 2], u[j * 2], t0), uy0 = Ease.lerp(u[i * 2 + 1], u[j * 2 + 1], t0), ux1 = Ease.lerp(u[i * 2], u[j * 2], t1), uy1 = Ease.lerp(u[i * 2 + 1], u[j * 2 + 1], t1);
        float vx0 = Ease.lerp(v[i * 2], v[j * 2], t0), vy0 = Ease.lerp(v[i * 2 + 1], v[j * 2 + 1], t0), vx1 = Ease.lerp(v[i * 2], v[j * 2], t1), vy1 = Ease.lerp(v[i * 2 + 1], v[j * 2 + 1], t1);
        int a = c.at(ux0, uy0), b = c.at(ux1, uy1), d = c.at(vx1, vy1), e = c.at(vx0, vy0);
        quad(ux0, uy0, fadeU ? a & 0xFFFFFF : a, ux1, uy1, fadeU ? b & 0xFFFFFF : b, vx1, vy1, fadeV ? d & 0xFFFFFF : d, vx0, vy0, fadeV ? e & 0xFFFFFF : e);
    }

    /** The house shape: a block with its top-left and bottom-right corners chipped off. */
    public static float[] shard(float x, float y, float w, float h, float cut) {
        float c = Math.min(cut, Math.min(w, h) / 2);
        if (c <= .01f) return new float[] {x, y, x + w, y, x + w, y + h, x, y + h};
        if (dev.hollow.module.Interface.rounded()) return round(x, y, w, h, c);
        return new float[] {x + c, y, x + w, y, x + w, y + h - c, x + w - c, y + h, x, y + h, x, y + c};
    }

    /** Rounded corners, a little squarer than a circle. */
    public static float[] round(float x, float y, float w, float h, float r) {
        r = Math.min(r, Math.min(w, h) / 2);
        int seg = r < 3 ? 4 : 7;
        float[] p = new float[(seg + 1) * 8];
        int n = 0;
        for (int c = 0; c < 4; c++) {
            float cx = c == 0 || c == 3 ? x + r : x + w - r, cy = c < 2 ? y + r : y + h - r;
            for (int i = 0; i <= seg; i++) {
                double a = Math.PI + c * Math.PI / 2 + Math.PI / 2 * i / seg, cos = Math.cos(a), sin = Math.sin(a);
                p[n++] = cx + (float) (Math.signum(cos) * Math.pow(Math.abs(cos), .86)) * r;
                p[n++] = cy + (float) (Math.signum(sin) * Math.pow(Math.abs(sin), .86)) * r;
            }
        }
        return p;
    }

    public static float[] diamond(float cx, float cy, float r) { return new float[] {cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy}; }

    /** A diamond turned by an angle in radians. */
    public static float[] diamond(float cx, float cy, float r, double turn) {
        float s = (float) Math.sin(turn) * r, c = (float) Math.cos(turn) * r;
        return new float[] {cx + s, cy - c, cx + c, cy + s, cx - s, cy + c, cx - c, cy - s};
    }

    /** Keeps the part of a convex shape where x * nx + y * ny >= d. */
    public static float[] clip(float[] p, float nx, float ny, float d) {
        int k = p.length / 2, m = 0;
        float[] out = new float[p.length * 2 + 4];
        for (int i = 0; i < k; i++) {
            int j = (i + 1) % k;
            float ax = p[i * 2], ay = p[i * 2 + 1], bx = p[j * 2], by = p[j * 2 + 1], da = ax * nx + ay * ny - d, db = bx * nx + by * ny - d;
            if (da >= 0) {
                out[m++] = ax;
                out[m++] = ay;
            }
            if (da >= 0 != db >= 0) {
                float t = da / (da - db);
                out[m++] = ax + (bx - ax) * t;
                out[m++] = ay + (by - ay) * t;
            }
        }
        return Arrays.copyOf(out, m);
    }

    /** Fills a shape from the left up to a fraction, with a leaning edge like the chipped corners. */
    public void sweep(float[] p, float fraction, Shade c) {
        if (fraction <= 0) return;
        if (fraction >= 1) {
            poly(p, c);
            return;
        }
        float x0 = Float.MAX_VALUE, x1 = -x0, y0 = x0, y1 = -x0;
        for (int i = 0; i < p.length; i += 2) {
            x0 = Math.min(x0, p[i]);
            x1 = Math.max(x1, p[i]);
            y0 = Math.min(y0, p[i + 1]);
            y1 = Math.max(y1, p[i + 1]);
        }
        float lean = (y1 - y0) * .7f, edge = x0 + (x1 - x0 + lean) * fraction;
        // The edge leans right at the top: x <= edge - (y - y0) * .7
        poly(clip(p, -1, -.7f, -(edge + y0 * .7f)), c);
    }

    /** Darkness spilling out from under a shape. */
    public void shadow(float[] p, float spread, int c) { poly(p, (x, y) -> c, spread); }

    public void flush() {
        if (n == 0) return;
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float x = xy[i * 2], y = xy[i * 2 + 1], tx = pose.m00 * x + pose.m10 * y + pose.m20, ty = pose.m01 * x + pose.m11 * y + pose.m21;
            x0 = Math.min(x0, tx);
            y0 = Math.min(y0, ty);
            x1 = Math.max(x1, tx);
            y1 = Math.max(y1, ty);
        }
        int left = (int) Math.floor(x0), top = (int) Math.floor(y0);
        ScreenRectangle bounds = new ScreenRectangle(left, top, (int) Math.ceil(x1) - left, (int) Math.ceil(y1) - top), scissor = g.scissorStack.peek();
        if (scissor != null) bounds = scissor.intersection(bounds);
        if (bounds != null) g.guiRenderState.addGuiElement(new Batch(Arrays.copyOf(xy, n * 2), Arrays.copyOf(col, n), pose, scissor, bounds));
        n = 0;
    }

    private record Batch(float[] xy, int[] col, Matrix3x2f pose, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public void buildVertices(VertexConsumer out) {
            for (int i = 0; i < col.length; i++) out.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(col[i]);
        }

        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }

        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
    }

    public static Shade across(float x0, float x1, int a, int b) { return (x, y) -> Colors.mix(a, b, (x - x0) / (x1 - x0)); }

    public static Shade down(float y0, float y1, int a, int b) { return (x, y) -> Colors.mix(a, b, (y - y0) / (y1 - y0)); }
}
