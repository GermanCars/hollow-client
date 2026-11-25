package dev.hollow.hud;

import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Something on the HUD. Held to the nearest corner, edge or the middle, so it stays put when the window changes. */
public abstract class HudElement extends Module {
    private final Setting.Num anchorX, anchorY, offX, offY;
    public final Setting.Num scale;
    public final Setting.Bool plate;
    /** Unscaled size, set while painting. */
    protected float w = 40, h = 12;
    private final Anim in = new Anim();

    protected HudElement(String name, String description, boolean on, int ax, int ay, double ox, double oy) {
        super(Category.HUD, name, description);
        anchorX = hiddenNum("anchorX", ax, 0, 2, 1);
        anchorY = hiddenNum("anchorY", ay, 0, 2, 1);
        offX = hiddenNum("offX", ox, -4000, 4000, .05);
        offY = hiddenNum("offY", oy, -4000, 4000, .05);
        scale = num("Size", 1, .5, 3, .05);
        plate = bool("Background", true);
        restore(on);
    }

    private Setting.Num hiddenNum(String n, double v, double a, double b, double s) { return when(num(n, v, a, b, s), () -> false); }

    /** Draws at 0, 0 and sets w and h. */
    protected abstract void paint(GuiGraphicsExtractor g, boolean editing);

    /** Nothing to show right now. Ignored while editing. */
    protected boolean empty() { return false; }

    public float width() { return w * scale.getF(); }

    public float height() { return h * scale.getF(); }

    public float x(float sw) { return at(anchorX.getInt(), offX.getF(), sw, width()); }

    public float y(float sh) { return at(anchorY.getInt(), offY.getF(), sh, height()); }

    private static float at(int anchor, float off, float screen, float size) {
        float v = anchor == 0 ? off : anchor == 1 ? screen / 2 - size / 2 + off : screen - size + off;
        return Math.clamp(v, 0, Math.max(0, screen - size));
    }

    public void moveTo(float x, float y, float sw, float sh) {
        x = Math.clamp(x, 0, Math.max(0, sw - width()));
        y = Math.clamp(y, 0, Math.max(0, sh - height()));
        int ax = third(x + width() / 2, sw), ay = third(y + height() / 2, sh);
        anchorX.set(ax);
        anchorY.set(ay);
        offX.set(ax == 0 ? x : ax == 1 ? x - sw / 2 + width() / 2 : x - sw + width());
        offY.set(ay == 0 ? y : ay == 1 ? y - sh / 2 + height() / 2 : y - sh + height());
    }

    private static int third(float centre, float size) { return centre < size * .36f ? 0 : centre > size * .64f ? 2 : 1; }

    protected boolean rightSide() { return anchorX.getInt() == 2; }

    protected boolean bottomSide() { return anchorY.getInt() == 2; }

    public void draw(GuiGraphicsExtractor g, float sw, float sh, boolean editing) {
        float t = in.to(editing ? on() : on() && !empty(), .3f);
        if (t <= 0) return;
        float e = Ease.outQuint(t), x = x(sw), y = y(sh);
        // Comes in from the nearest side.
        float dx = anchorX.getInt() == 0 ? -1 : anchorX.getInt() == 2 ? 1 : 0, dy = anchorX.getInt() == 1 ? (anchorY.getInt() == 2 ? 1 : -1) : 0;
        g.pose().pushMatrix();
        g.pose().translate(x + dx * (1 - e) * 60, y + dy * (1 - e) * 30);
        g.pose().scale(scale.getF());
        if (plate.get()) plate(g);
        paint(g, editing);
        g.pose().popMatrix();
    }

    private void plate(GuiGraphicsExtractor g) {
        Style.plate(g, 0, 0, w, h, 1);
    }

    protected static void text(GuiGraphicsExtractor g, String s, float x, float base, int c) { Fonts.BOLD.draw(g, s, x, base, 9, c); }
}
