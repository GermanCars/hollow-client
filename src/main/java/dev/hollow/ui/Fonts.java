package dev.hollow.ui;

import dev.hollow.module.Interface;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Text drawn from a font baked at exactly the size it takes up on screen, so it is never resampled. */
public enum Fonts {
    TEXT(.5f), BOLD(.52f), DISPLAY(.36f * 2);

    /** Minecraft puts the baseline of every font this far below the y it is given. */
    private static final float BASELINE = 7;
    private static final Map<String, Style> STYLES = new HashMap<>();
    private static final Map<String, Float> WIDTHS = new HashMap<>();

    /** Height of a capital letter relative to the size. */
    private final float cap;

    Fonts(float cap) { this.cap = cap; }

    private Style style(int px) {
        Fonts face = this;
        if (this != DISPLAY) {
            if (Interface.face().equals("Minecraft")) return Style.EMPTY;
            if (Interface.face().equals("Unbounded")) face = DISPLAY;
        }
        return STYLES.computeIfAbsent(face.name().toLowerCase(Locale.ROOT) + px, n -> Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hollow", n))));
    }

    private float fit(float size) {
        if (this == DISPLAY) return size;
        return switch (Interface.face()) {
            case "Unbounded" -> size * .8f;
            case "Minecraft" -> size * .95f;
            default -> size;
        };
    }

    private boolean vanilla() { return this != DISPLAY && Interface.face().equals("Minecraft"); }

    private int pixels(float wanted) {
        int p = Math.round(wanted);
        if (p <= 40 || this != DISPLAY) return Math.clamp(p, 7, 40);
        return Math.clamp(Math.round(p / 4f) * 4, 44, 128);
    }

    public float width(String s, float size) {
        size = fit(size);
        int px = vanilla() ? 8 : pixels(size * Frame.scale);
        if (WIDTHS.size() > 4000) WIDTHS.clear();
        String k = name() + px + Interface.face() + s;
        Float w = WIDTHS.get(k);
        if (w == null) WIDTHS.put(k, w = Minecraft.getInstance().font.getSplitter().stringWidth(Component.literal(s).withStyle(style(px))));
        return w * size / px;
    }

    /** Baseline that centres capitals between y and y + height. */
    public float centre(float y, float height, float size) { return y + height / 2 + fit(size) * (vanilla() ? .44f : this == DISPLAY || Interface.face().equals("Unbounded") ? .36f : .35f); }

    public void draw(GuiGraphicsExtractor g, String s, float x, float baseline, float size, int color) {
        // The game treats nearly transparent text as opaque, so skip it.
        if (color >>> 24 < 4 || s.isEmpty()) return;
        size = fit(size);
        Matrix3x2f m = g.pose();
        float outer = (float) Math.hypot(m.m00, m.m01);
        int px = vanilla() ? 8 : pixels(size * outer * Frame.scale);
        float k = size / px;
        // Land on a whole screen pixel.
        float sx = Math.round((m.m00 * x + m.m10 * baseline + m.m20) * Frame.scale) / Frame.scale, sy = Math.round((m.m01 * x + m.m11 * baseline + m.m21) * Frame.scale) / Frame.scale;
        g.pose().pushMatrix();
        if (Math.abs(m.m01) < 1e-4f) g.pose().set(m.m00, m.m01, m.m10, m.m11, sx, sy);
        else g.pose().translate(x, baseline);
        g.pose().scale(k);
        g.pose().translate(0, -BASELINE);
        g.text(Minecraft.getInstance().font, Component.literal(s).withStyle(style(px)), 0, 0, color, false);
        g.pose().popMatrix();
    }

    public void right(GuiGraphicsExtractor g, String s, float x, float baseline, float size, int color) { draw(g, s, x - width(s, size), baseline, size, color); }

    public void middle(GuiGraphicsExtractor g, String s, float x, float baseline, float size, int color) { draw(g, s, x - width(s, size) / 2, baseline, size, color); }

    /** Shortens with an ellipsis to fit. */
    public String clip(String s, float size, float max) {
        if (width(s, size) <= max) return s;
        while (!s.isEmpty() && width(s + "…", size) > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Breaks at spaces. */
    public java.util.List<String> wrap(String s, float size, float max) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!line.isEmpty() && width(line + " " + w, size) > max) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(w);
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    /** For labels the game itself draws. */
    public static Component label(Component text) {
        int s = Math.clamp(Math.round(Frame.scale), 1, 8);
        return Component.empty().append(text).withStyle(STYLES.computeIfAbsent("label" + s, n -> Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("hollow", n)))));
    }
}
