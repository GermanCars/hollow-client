package dev.hollow.ui;

import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/** A slab of ice on Hollow's own screens. It leans towards the cursor, and shatters when it's chosen. */
public final class HButton extends AbstractButton {
    private final Runnable action;
    private final float size;
    public final SnowBank bank = new SnowBank(2.2f);
    private final long shownAt;
    private final Anim leaving = new Anim(), tiltX = new Anim(), tiltY = new Anim();
    private float pressX = Float.NaN, pressY;
    private boolean broken;
    /** Set when some other button was chosen. */
    public boolean gone;

    public HButton(int x, int y, int w, int h, String label, float size, int delay, Runnable action) {
        super(x, y, w, h, Component.literal(label));
        this.action = action;
        this.size = size;
        shownAt = Util.getMillis() + delay;
        bank.place(x, y, w, Style.cut(h) * .6f);
    }

    @Override public void onClick(MouseButtonEvent e, boolean doubleClick) {
        pressX = (float) e.x();
        pressY = (float) e.y();
        super.onClick(e, doubleClick);
    }

    @Override public void onPress(InputWithModifiers input) {
        float px = Float.isNaN(pressX) ? getX() + width / 2f : pressX, py = Float.isNaN(pressX) ? getY() + height / 2f : pressY;
        Scene.get().snow.burst(px, getY(), 22);
        if (!Interface.calm()) {
            broken = true;
            bank.clear();
            Ice.shatter(getX(), getY(), width, height, px, py);
        }
        action.run();
    }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Style.State s = Style.state(this);
        boolean lit = active && isHoveredOrFocused() && !gone;
        float hover = s.hover.to(lit, .3f), in = Interface.calm() ? 1 : Ease.clamp((Util.getMillis() - shownAt) / 700f * Interface.speed()), e = Ease.outQuint(in), out = leaving.to(gone || broken, .32f);
        if (lit || gone || broken) Scene.get().snow.shake(bank);
        else if (hover == 0) bank.shaken = in < 1;
        if (in <= 0 || broken || out >= 1) return;
        float o = Ease.inOut(out), alpha = e * (1 - o);
        tiltX.glide(lit ? Math.clamp((mouseX - getX() - width / 2f) / (width / 2f), -1, 1) : 0, 10);
        tiltY.glide(lit ? Math.clamp((mouseY - getY() - height / 2f) / (height / 2f), -1, 1) : 0, 10);
        float grow = 1 + .035f * Ease.outBack(hover) - .12f * o, cx = getX() + width / 2f, cy = getY() + height / 2f;
        g.pose().pushMatrix();
        // Slides in from the dark at the side, out of focus and large.
        g.pose().translate(cx + (e - 1) * 70 - o * 46 + tiltX.v * 1.6f, cy + tiltY.v * 1.2f);
        g.pose().scale(grow + (1 - e) * .22f);
        g.pose().translate(-cx, -cy);
        Style.button(g, this, getX(), getY(), width, height, hover, active, alpha);
        float base = Fonts.DISPLAY.centre(getY(), height, size), tx = getX() + 12 + Ease.outQuint(hover) * 7;
        Draw d = new Draw(g);
        float bar = Ease.outBack(hover);
        d.poly(Draw.round(getX() + 5.5f, cy - height * .24f * bar, 2, height * .48f * bar, 1), Colors.with(0xFFFFFF, .95f * hover * alpha));
        d.flush();
        Fonts.DISPLAY.draw(g, getMessage().getString(), tx + .6f, base + .8f, size, Colors.with(0x02101C, .45f * alpha));
        Fonts.DISPLAY.draw(g, getMessage().getString(), tx, base, size, Colors.alpha(Style.buttonText(hover, active), alpha));
        g.pose().popMatrix();
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
}
