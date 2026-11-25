package dev.hollow.ui;

import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** How interactive things look, shared by Hollow's menus and the game's own widgets. */
public final class Style {
    /** Animation state for one widget. */
    public static final class State {
        public final Anim hover = new Anim(), press = new Anim(), value = new Anim(Float.NaN), glint = new Anim();
        boolean was;
    }

    private static final Map<Object, State> STATES = new WeakHashMap<>();

    private Style() {}

    public static State state(Object widget) { return STATES.computeIfAbsent(widget, k -> new State()); }

    public static float cut(float h) { return Math.min(Theme.cut(), h * .42f); }

    /** Frosted at rest. Pointed at, the frost melts back, light crosses it and it fills with colour. */
    public static void button(GuiGraphicsExtractor g, Object owner, float x, float y, float w, float h, float hover, boolean active, float alpha) {
        State st = state(owner);
        boolean lit = hover > .02f && active;
        if (lit && !st.was) st.glint.snap(0);
        st.was = lit;
        float gl = st.glint.to(1, .7f), e = Ease.outQuint(hover);
        float[] s = Draw.shard(x, y, w, h, cut(h));
        if (e > 0) {
            Draw d = new Draw(g);
            d.shadow(Draw.offset(s, 1), 9, Colors.with(Theme.accent(), .26f * e * alpha));
            d.flush();
        }
        Ice.panel(g, s, x, y, w, h, new Ice.Look(alpha, active ? .5f : .72f, active ? 1 - e * .85f : .5f, active ? 1 + e * .5f : .5f, 0));
        Draw d = new Draw(g);
        if (e > 0) d.sweep(s, e, (px, py) -> Colors.alpha(Theme.at(Ease.clamp((px - x) / w)), alpha * (.40f + .22f * Ease.clamp((py - y) / h))));
        if (lit) Ice.glint(d, s, x, w, y, h, gl, alpha);
        d.flush();
        Ice.drawCracks(g, owner, x, y, w, h, alpha);
    }

    public static void button(GuiGraphicsExtractor g, float x, float y, float w, float h, float hover, boolean active, float alpha) { button(g, "plain", x, y, w, h, hover, active, alpha); }

    /** Text colour on a button at this point of its hover. */
    public static int buttonText(float hover, boolean active) { return active ? Colors.mix(0xFFEAF4FA, 0xFFFFFFFF, hover) : Colors.with(0xC4D6E4, .55f); }

    public static void field(GuiGraphicsExtractor g, float x, float y, float w, float h, float focus, float alpha) {
        float[] s = Draw.shard(x, y, w, h, cut(h) * .8f);
        Ice.glass(g, s, alpha, 2);
        Draw d = new Draw(g);
        d.poly(s, Colors.with(Colors.mix(Theme.ink(), 0xFF000000, .4f), .62f * alpha));
        d.stroke(Draw.offset(s, -.5f), 1, Colors.with(0xFFFFFF, .13f * alpha));
        if (focus > 0) {
            d.stroke(Draw.offset(s, -.5f), 1.1f, Theme.fill(x, x + w, alpha), 0, Ease.inOut(focus));
            d.poly(s, Colors.with(Theme.accent(), .06f * focus * alpha));
        }
        d.flush();
    }

    /** A bead of ice riding a line of light. */
    public static void track(Draw d, float x, float y, float w, float t, float hover, float alpha) {
        d.poly(Draw.round(x - 1, y - 1.3f, w + 2, 2.6f, 1.3f), Colors.with(0x000000, .38f * alpha));
        if (t > 0) {
            float[] lit = Draw.round(x - 1, y - 1.3f, w * t + 2, 2.6f, 1.3f);
            d.shadow(lit, 5, Colors.with(Theme.accent(), .22f * alpha));
            d.poly(lit, Theme.fill(x, x + w, alpha));
        }
        float cx = x + w * t, r = 3.7f + hover * 1.2f;
        d.circle(cx, y, r + 3 * hover, Colors.with(Theme.accent(), .2f * hover * alpha));
        d.circle(cx, y, r, Colors.alpha(Colors.mix(0xFFFFFFFF, Theme.at(t), .12f), alpha));
        d.circle(cx, y, r * .48f, Colors.alpha(Theme.at(t), alpha));
    }

    public static void slider(GuiGraphicsExtractor g, float x, float y, float w, float h, float value, float hover, boolean active, float alpha) {
        float[] s = Draw.shard(x, y, w, h, cut(h));
        Ice.panel(g, s, x, y, w, h, new Ice.Look(alpha, .5f, 1 - hover * .7f, 1 + hover * .4f, 0));
        Draw d = new Draw(g);
        d.sweep(s, value, Theme.fill(x, x + w, alpha * (.14f + .12f * hover)));
        track(d, x + 7, y + h - 3.6f, w - 14, value, hover, active ? alpha : alpha * .5f);
        d.flush();
    }

    /** A switch: a bead that slides across and lights up. */
    public static void toggle(Draw d, float x, float y, float w, float h, float on, float hover, float alpha) {
        float e = Ease.outBack(on);
        float[] s = Draw.round(x, y, w, h, h / 2);
        d.poly(s, Colors.with(0x000000, .34f * alpha));
        if (on > 0) {
            d.shadow(s, 6, Colors.with(Theme.accent(), .25f * on * alpha));
            d.poly(s, (px, py) -> Colors.alpha(Theme.at(Ease.clamp((px - x) / w)), on * alpha * .92f));
        }
        d.stroke(Draw.offset(s, -.4f), .8f, Colors.with(0xFFFFFF, (.16f + .2f * hover) * alpha));
        float r = h / 2 - 1.5f, cx = x + h / 2 + (w - h) * e;
        d.circle(cx, y + h / 2, r + .6f, Colors.with(0, .22f * alpha));
        d.circle(cx, y + h / 2, r, Colors.alpha(Colors.mix(0xFFBCCBD6, 0xFFFFFFFF, Math.max(on, hover)), alpha));
    }

    /** Small tick box: an empty bead that fills. */
    public static void check(Draw d, float cx, float cy, float r, float on, float hover, float alpha) {
        d.circle(cx, cy, r, Colors.with(0, .3f * alpha));
        float[] ring = Draw.round(cx - r, cy - r, r * 2, r * 2, r);
        d.stroke(Draw.offset(ring, -.5f), 1, Colors.alpha(Colors.mix(Colors.with(0xFFFFFF, .28f), Theme.accent(), Math.max(on, hover)), alpha));
        if (on > 0) {
            d.circle(cx, cy, r * 1.7f * on, Colors.with(Theme.accent(), .14f * alpha));
            d.circle(cx, cy, (r - 1.5f) * Ease.outBack(on), Colors.alpha(Theme.accent(), alpha));
        }
    }

    public static void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float alpha) { panel(g, x, y, w, h, alpha, 1); }

    public static void panel(GuiGraphicsExtractor g, float x, float y, float w, float h, float alpha, float frost) {
        float[] s = Draw.shard(x, y, w, h, Math.min(Theme.cut() * 1.3f, Math.min(w, h) * .4f));
        Ice.panel(g, s, x, y, w, h, new Ice.Look(alpha, .8f, frost, 1, 14));
    }

    /** A HUD plate: thin and quiet. */
    public static void plate(GuiGraphicsExtractor g, float x, float y, float w, float h, float alpha) {
        float[] s = Draw.shard(x, y, w, h, Math.min(Theme.cut() * .7f, h * .36f));
        Ice.glass(g, s, alpha, 2.5f);
        Draw d = new Draw(g);
        Ice.cast(d, s, alpha);
        d.poly(s, Colors.with(Theme.ink(), .48f * Interface.panelAlpha() * alpha));
        Ice.rim(d, s, alpha, .55f);
        d.flush();
    }
}
