package dev.hollow.ui;

import dev.hollow.core.Colors;
import dev.hollow.core.Setting;
import dev.hollow.module.Interface;

/** The colours everything is drawn with, worked out from the theme settings. */
public final class Theme {
    public record Preset(String name, int accent, int second, int ink) {}

    public static final Preset[] PRESETS = {
        new Preset("Glacier", 0xFF9FE6FF, 0xFF3F8CFF, 0xFF0B1826),
        new Preset("Aurora", 0xFF8CF0CB, 0xFF7FB2FF, 0xFF0F171C),
        new Preset("Ember", 0xFFFFB877, 0xFFF0603F, 0xFF191310),
        new Preset("Crystal", 0xFFFF8FDD, 0xFFB58CFF, 0xFF17121D),
        new Preset("Moss", 0xFFCBE58A, 0xFF62B07A, 0xFF11170F),
        new Preset("Gold", 0xFFFFE08A, 0xFFE0A03A, 0xFF18150D),
        new Preset("Blood", 0xFFFF8A80, 0xFFC4263B, 0xFF1A0F11),
        new Preset("Bone", 0xFFF1EBDF, 0xFFA9A49A, 0xFF141312)};

    private Theme() {}

    public static int accent() { return at(0); }

    public static int second() { return at(1); }

    /** A point along the theme's two colours, or the rainbow. */
    public static int at(float t) {
        if (Interface.rainbow()) return Colors.hsv(Frame.time * .08f + t * .22f, .5f, 1, 1);
        return Colors.mix(Interface.accent(), Interface.second(), t);
    }

    /** Slides back and forth, for lists and letters. */
    public static int flow(float offset) { return at(.5f + .5f * (float) Math.sin(Frame.time * 1.4f - offset)); }

    public static int ink() { return Interface.ink(); }

    public static int panel(float alpha) { return Colors.with(ink(), Interface.panelAlpha() * alpha); }

    /** A step lighter than the panel. */
    public static int raised(float alpha) { return Colors.with(Colors.mix(ink(), 0xFFFFFFFF, .07f), Math.min(1, Interface.panelAlpha() + .06f) * alpha); }

    public static int text() { return 0xFFEFF4F3; }

    public static int dim() { return Colors.mix(ink(), 0xFFEFF4F3, .62f); }

    public static int faint() { return Colors.mix(ink(), 0xFFEFF4F3, .28f); }

    public static float cut() { return Interface.cut(); }

    public static int of(Setting.Color c) { return c.theme ? Colors.alpha(accent(), c.alpha) : c.own(); }

    public static Draw.Shade fill(float x0, float x1, float alpha) { return (x, y) -> Colors.alpha(at(Math.clamp((x - x0) / Math.max(1, x1 - x0), 0, 1)), alpha); }
}
