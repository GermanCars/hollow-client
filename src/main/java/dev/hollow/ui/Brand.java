package dev.hollow.ui;

import dev.hollow.core.Colors;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The HOLLOW CLIENT lettering. Drawn as outlines that fill up with snow. */
public final class Brand {
    private static final float T = .26f, K = .13f, GAP = .13f;

    /** Outline loops, and convex pieces that make up the inside. Height 1, y down. */
    private record Letter(float width, List<float[]> loops, List<float[]> pieces) {}

    private static final Map<Character, Letter> LETTERS = Map.of(
        'H', h(), 'O', o(), 'L', l(), 'W', w(), 'C', c(), 'I', i(), 'E', e(), 'N', n(), 'T', t());

    private Brand() {}

    private static float[] p(float... v) { return v; }

    private static Letter h() {
        float w = .92f, a = .5f - T * .46f, b = .5f + T * .46f;
        return new Letter(w, List.of(p(K, 0, T, 0, T, a, w - T, a, w - T, 0, w, 0, w, 1 - K, w - K, 1, w - T, 1, w - T, b, T, b, T, 1, 0, 1, 0, K)),
            List.of(p(K, 0, T, 0, T, 1, 0, 1, 0, K), p(T, a, w - T, a, w - T, b, T, b), p(w - T, 0, w, 0, w, 1 - K, w - K, 1, w - T, 1)));
    }

    private static Letter o() {
        float w = .98f, j = .07f;
        float[] out = p(K, 0, w - K, 0, w, K, w, 1 - K, w - K, 1, K, 1, 0, 1 - K, 0, K), in = p(T + j, T, w - T - j, T, w - T, T + j, w - T, 1 - T - j, w - T - j, 1 - T, T + j, 1 - T, T, 1 - T - j, T, T + j);
        List<float[]> pieces = new java.util.ArrayList<>();
        for (int a = 0; a < 8; a++) {
            int b = (a + 1) % 8;
            pieces.add(p(out[a * 2], out[a * 2 + 1], out[b * 2], out[b * 2 + 1], in[b * 2], in[b * 2 + 1], in[a * 2], in[a * 2 + 1]));
        }
        return new Letter(w, List.of(out, in), pieces);
    }

    private static Letter l() {
        float w = .74f;
        return new Letter(w, List.of(p(K, 0, T, 0, T, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, 0, 1, 0, K)), List.of(p(K, 0, T, 0, T, 1 - T, 0, 1 - T, 0, K), p(0, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, 0, 1)));
    }

    private static Letter w() {
        float w = 1.3f, m = w / 2 - T / 2, q = w / 2 + T / 2, top = .28f;
        return new Letter(w, List.of(p(K, 0, T, 0, T, 1 - T, m, 1 - T, m, top, q, top, q, 1 - T, w - T, 1 - T, w - T, 0, w, 0, w, 1 - K, w - K, 1, K, 1, 0, 1 - K, 0, K)),
            List.of(p(K, 0, T, 0, T, 1 - T, 0, 1 - T, 0, K), p(m, top, q, top, q, 1 - T, m, 1 - T), p(w - T, 0, w, 0, w, 1 - T, w - T, 1 - T), p(0, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, K, 1, 0, 1 - K)));
    }

    private static Letter c() {
        float w = .86f;
        return new Letter(w, List.of(p(K, 0, w, 0, w, T, T, T, T, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, K, 1, 0, 1 - K, 0, K)),
            List.of(p(K, 0, w, 0, w, T, 0, T, 0, K), p(0, T, T, T, T, 1 - T, 0, 1 - T), p(0, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, K, 1, 0, 1 - K)));
    }

    private static Letter i() {
        float[] a = p(K, 0, T, 0, T, 1 - K, T - K, 1, 0, 1, 0, K);
        return new Letter(T, List.of(a), List.of(a));
    }

    private static Letter e() {
        float w = .8f, a = .5f - T * .44f, b = .5f + T * .44f, m = w - .1f;
        return new Letter(w, List.of(p(K, 0, w, 0, w, T, T, T, T, a, m, a, m, b, T, b, T, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, 0, 1, 0, K)),
            List.of(p(K, 0, w, 0, w, T, 0, T, 0, K), p(0, T, T, T, T, 1 - T, 0, 1 - T), p(T, a, m, a, m, b, T, b), p(0, 1 - T, w, 1 - T, w, 1 - K, w - K, 1, 0, 1)));
    }

    private static Letter n() {
        float w = .94f, d = .58f;
        return new Letter(w, List.of(p(K, 0, T, 0, w - T, d, w - T, 0, w, 0, w, 1 - K, w - K, 1, w - T, 1, T, 1 - d, T, 1, 0, 1, 0, K)),
            List.of(p(K, 0, T, 0, T, 1, 0, 1, 0, K), p(T, 0, w - T, d, w - T, 1, T, 1 - d), p(w - T, 0, w, 0, w, 1 - K, w - K, 1, w - T, 1)));
    }

    private static Letter t() {
        float w = .88f, m = w / 2 - T / 2, q = w / 2 + T / 2;
        return new Letter(w, List.of(p(K, 0, w, 0, w, T, q, T, q, 1 - K, q - K, 1, m, 1, m, T, 0, T, 0, K)), List.of(p(K, 0, w, 0, w, T, 0, T, 0, K), p(m, T, q, T, q, 1 - K, q - K, 1, m, 1)));
    }

    public static float width(String s, float height) {
        float w = 0;
        for (char ch : s.toCharArray()) w += LETTERS.get(ch).width + GAP;
        return (w - GAP) * height;
    }

    /** How each letter comes in: reveal runs 0 to 1 over the whole word. */
    public record Look(float reveal, float fill, int line, float lineWidth, Draw.Shade snow, float alpha, float backing) {
        public Look(float reveal, float fill, int line, float lineWidth, Draw.Shade snow, float alpha) { this(reveal, fill, line, lineWidth, snow, alpha, 0); }
    }

    /** Draws a word and returns its width. Fill 0 is hollow, 1 is full; the snow line drifts. */
    public static float word(GuiGraphicsExtractor g, String s, float x, float y, float height, Look look) {
        Draw d = new Draw(g);
        float pen = x;
        int count = s.length();
        for (int i = 0; i < count; i++) {
            Letter l = LETTERS.get(s.charAt(i));
            float t = Ease.clamp(look.reveal * (1 + (count - 1) * .35f) - i * .35f);
            if (t > 0) {
                float drop = (1 - Ease.outBack(t)) * height * .55f, oy = y - drop;
                // Keeps the outline readable over a bright sky.
                if (look.backing > 0) {
                    for (float[] piece : l.pieces) d.poly(Draw.offset(place(piece, pen, oy, height), height * .07f), (px, py) -> Colors.with(Theme.ink(), look.backing * look.alpha), height * .12f);
                }
                if (look.fill > 0) for (float[] piece : l.pieces) snow(d, piece, pen, oy, height, look, t);
                for (float[] loop : l.loops) {
                    float[] at = place(loop, pen, oy, height);
                    d.stroke(Draw.offset(at, -look.lineWidth / 2), look.lineWidth, (px, py) -> Colors.alpha(look.line, look.alpha), 0, Ease.inOut(t));
                }
            }
            pen += (l.width + GAP) * height;
        }
        d.flush();
        return pen - GAP * height - x;
    }

    private static float[] place(float[] p, float x, float y, float h) {
        float[] o = new float[p.length];
        for (int i = 0; i < p.length; i += 2) {
            o[i] = x + p[i] * h;
            o[i + 1] = y + p[i + 1] * h;
        }
        return o;
    }

    /** One convex piece, cut off above the uneven snow line. */
    private static void snow(Draw d, float[] piece, float x, float y, float h, Look look, float t) {
        float[] at = place(piece, x, y, h);
        Draw.Shade shade = (px, py) -> Colors.alpha(look.snow.at(px, py), look.alpha * Ease.clamp(t * 2 - 1));
        if (look.fill >= 1) {
            d.poly(at, shade);
            return;
        }
        float x0 = Float.MAX_VALUE, x1 = -x0;
        for (int i = 0; i < at.length; i += 2) {
            x0 = Math.min(x0, at[i]);
            x1 = Math.max(x1, at[i]);
        }
        // Slices follow the drifting surface; inside seams are hidden by overlapping a hair.
        int slices = Math.max(1, Math.round((x1 - x0) / (h * .12f)));
        for (int s = 0; s < slices; s++) {
            float a = x0 + (x1 - x0) * s / slices, b = Math.min(x1, x0 + (x1 - x0) * (s + 1) / slices + h * .02f);
            float[] cell = slices == 1 ? at : Draw.clip(Draw.clip(at, 1, 0, a), -1, 0, -b);
            if (cell.length < 6) continue;
            float ya = surface(a, y, h, look.fill), yb = surface(b, y, h, look.fill), slope = (yb - ya) / Math.max(1e-4f, b - a);
            // y >= ya + (x - a) * slope
            float[] cut = Draw.clip(cell, -slope, 1, ya - a * slope);
            if (cut.length >= 6) d.poly(Draw.offset(cut, h * .006f), shade, .02f);
        }
    }

    private static float surface(float x, float y, float h, float fill) {
        float wave = (float) (Math.sin(x / h * 2.3 + Frame.time * .9) * .045 + Math.sin(x / h * 5.1 - Frame.time * .6) * .022);
        return y + h * (1.06f - fill * 1.12f + wave * Math.min(1, (1 - fill) * 6));
    }

    /** Both words on one line, the second smaller and solid. Returns the width. */
    public static float line(GuiGraphicsExtractor g, float x, float y, float height, float reveal, float fill, float alpha) { return line(g, x, y, height, reveal, fill, alpha, 0); }

    public static float line(GuiGraphicsExtractor g, float x, float y, float height, float reveal, float fill, float alpha, float backing) {
        Draw.Shade snow = (px, py) -> Theme.flow((px - x) / height * .5f);
        float w = word(g, "HOLLOW", x, y, height, new Look(reveal, fill, Theme.text(), Math.max(height * .045f, 1.2f / Frame.scale), snow, alpha, backing));
        float small = height * .42f, gap = height * .26f;
        int c = Theme.text();
        w += gap + word(g, "CLIENT", x + w + gap, y + height - small, small, new Look(Ease.clamp(reveal * 1.6f - .6f), 1, c, 0.01f, (px, py) -> c, alpha, backing));
        return w;
    }

    public static float lineWidth(float height) { return width("HOLLOW", height) + height * .26f + width("CLIENT", height * .42f); }
}
