package dev.hollow.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.hud.HudElement;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag HUD elements about, switch them on and off, and change how they look. */
public final class HudEditor extends Screen {
    private static final float PANEL = 128, ROW = 14, SNAP = 4;

    private final Screen parent;
    private final Anim panelIn = new Anim(), guideX = new Anim(), guideY = new Anim();
    private final Snow snow = new Snow();
    private HudElement selected, dragging;
    private SettingsView settings;
    private float grabX, grabY, listScroll, setScroll, mx, my, lineX = -1, lineY = -1;
    private static float px = Float.NaN, py = Float.NaN;
    private boolean collapsed, movingPanel;

    public HudEditor(Screen parent) {
        super(Component.literal("HUD editor"));
        this.parent = parent;
    }

    @Override protected void init() { minecraft.textInputManager().startTextInput(this); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public void removed() { Hollow.save(); }

    private float panelX() { return Math.clamp(Float.isNaN(px) ? width * .24f : px, 0, Math.max(0, width - PANEL)); }

    private float panelY() { return Math.clamp(Float.isNaN(py) ? 30 : py, 0, Math.max(0, height - 24)); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (minecraft.level == null) {
            Scene.get().begin(g);
            Scene.get().drawBack(g);
        }
        g.fill(0, 0, width, height, 0x66060A0E);
        Draw d = new Draw(g);
        // Thirds: which side an element will hold on to.
        for (float t : new float[] {.36f, .64f}) {
            d.rect(width * t, 0, .5f, height, Colors.with(0xFFFFFF, .07f));
            d.rect(0, height * t, width, .5f, Colors.with(0xFFFFFF, .07f));
        }
        d.flush();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        mx = (float) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        my = (float) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        boolean onPanel = overPanel(mx, my);
        HudElement over = onPanel || dragging != null ? null : at(mx, my);
        for (HudElement e : Hollow.HUD_ELEMENTS) e.draw(g, width, height, true);
        Draw d = new Draw(g);
        for (HudElement e : Hollow.HUD_ELEMENTS) {
            if (!e.on()) continue;
            float v = Style.state(e).hover.to(e == over || e == dragging || e == selected, .18f), x = e.x(width) - 2, y = e.y(height) - 2, w = e.width() + 4, h = e.height() + 4;
            float[] s = Draw.shard(x, y, w, h, 4);
            d.stroke(s, .8f, Colors.with(0xFFFFFF, .16f));
            if (v > 0) d.stroke(s, 1.2f, Theme.fill(x, x + w, 1), 0, Ease.inOut(v));
        }
        float gx = guideX.to(dragging != null && lineX >= 0, .12f), gy = guideY.to(dragging != null && lineY >= 0, .12f);
        if (gx > 0 && lineX >= 0) d.rect(lineX - .4f, 0, .8f, height, Colors.alpha(Theme.accent(), gx));
        if (gy > 0 && lineY >= 0) d.rect(0, lineY - .4f, width, .8f, Colors.alpha(Theme.accent(), gy));
        d.flush();
        if (over != null && over != selected) {
            float y = over.y(height) > 16 ? over.y(height) - 6 : over.y(height) + over.height() + 11;
            Fonts.BOLD.draw(g, over.name, Math.min(over.x(width), width - Fonts.BOLD.width(over.name, 9) - 4), y, 9, 0xFFFFFFFF);
        }
        g.nextStratum();
        panel(g);
        Fonts.TEXT.middle(g, "Drag to move. Scroll to resize. Right click for settings. Hold Shift to stop snapping.", width / 2f, height - 6, 8.5f, Colors.alpha(Theme.text(), dragging == null ? .75f : .2f));
    }

    private float panelH() { return Math.min(height - 40, 300); }

    private boolean overPanel(float x, float y) { return x >= panelX() && x < panelX() + PANEL && y >= panelY() && y < panelY() + (collapsed ? 24 : panelH()); }

    private void panel(GuiGraphicsExtractor g) {
        float in = Ease.outQuint(panelIn.to(1, .4f)), x = panelX(), y = panelY(), h = collapsed ? 24 : panelH();
        if (dragging != null) in *= .25f;
        Style.panel(g, x, y, PANEL, h, in);
        Brand.line(g, x + 9, y + 7, Math.min(11, (PANEL - 30) / Brand.lineWidth(1)), 1, .6f, in);
        if (collapsed) return;
        float top = y + 26, end = y + h - 24;
        Draw d = new Draw(g);
        d.rect(x + 8, top - 2, PANEL - 16, 1, Colors.with(0xFFFFFF, .1f * in));
        d.flush();
        if (selected != null) {
            Fonts.BOLD.draw(g, selected.name, x + 9, top + 10, 10, Colors.alpha(Theme.text(), in));
            float st = top + 16;
            setScroll = Math.clamp(setScroll, 0, Math.max(0, settings.height - (end - st)));
            g.enableScissor((int) x, (int) st, (int) (x + PANEL), (int) end);
            settings.draw(g, x + 9, st - setScroll, PANEL - 18, st, end, mx, my, in);
            g.disableScissor();
            button(g, x + 8, y + h - 20, PANEL - 16, "All elements", in);
        } else {
            List<HudElement> all = Hollow.HUD_ELEMENTS;
            listScroll = Math.clamp(listScroll, 0, Math.max(0, all.size() * ROW - (end - top)));
            g.enableScissor((int) x, (int) top, (int) (x + PANEL), (int) end);
            for (int i = 0; i < all.size(); i++) {
                HudElement e = all.get(i);
                float ry = top + i * ROW - listScroll;
                if (ry < top - ROW || ry > end) continue;
                boolean hot = mx >= x && mx < x + PANEL && my >= ry && my < ry + ROW && my >= top && my < end;
                Style.State st = Style.state(e.settings);
                Draw r = new Draw(g);
                if (hot) r.rect(x, ry, PANEL, ROW, Colors.with(0xFFFFFF, .06f * in));
                Style.check(r, x + 14, ry + ROW / 2, 4, st.press.to(e.on(), .2f), st.hover.to(hot, .15f), in);
                r.flush();
                Fonts.BOLD.draw(g, e.name, x + 23, Fonts.BOLD.centre(ry, ROW, 9), 9, Colors.alpha(e.on() ? Theme.text() : Theme.dim(), in));
            }
            g.disableScissor();
            button(g, x + 8, y + h - 20, PANEL - 16, parent == null ? "Done" : "Back to modules", in);
        }
    }

    private void button(GuiGraphicsExtractor g, float x, float y, float w, String label, float alpha) {
        boolean hot = mx >= x && mx < x + w && my >= y && my < y + 15;
        float v = Style.state(label.intern()).hover.to(hot, .2f);
        Style.button(g, x, y, w, 15, v, true, alpha);
        Fonts.BOLD.middle(g, label, x + w / 2, Fonts.BOLD.centre(y, 15, 9), 9, Colors.alpha(Style.buttonText(v, true), alpha));
    }

    private HudElement at(float x, float y) {
        HudElement best = null;
        for (HudElement e : Hollow.HUD_ELEMENTS) {
            if (!e.on()) continue;
            float ex = e.x(width), ey = e.y(height);
            if (x >= ex - 2 && x < ex + e.width() + 2 && y >= ey - 2 && y < ey + e.height() + 2 && (best == null || e.width() * e.height() < best.width() * best.height())) best = e;
        }
        return best;
    }

    private void select(HudElement e) {
        selected = e;
        settings = e == null ? null : new SettingsView(e.settings);
        setScroll = 0;
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        mx = (float) e.x();
        my = (float) e.y();
        if (settings != null) settings.unfocus();
        if (overPanel(mx, my)) {
            float x = panelX(), y = panelY(), h = panelH();
            if (my < y + 24) {
                if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) collapsed = !collapsed;
                else {
                    movingPanel = true;
                    grabX = mx - x;
                    grabY = my - y;
                }
            }
            else if (my >= y + h - 22) {
                if (selected != null) select(null);
                else onClose();
            } else if (selected != null) settings.click(mx, my, e.button());
            else {
                int i = (int) ((my - y - 26 + listScroll) / ROW);
                if (i >= 0 && i < Hollow.HUD_ELEMENTS.size() && my < y + h - 24) {
                    HudElement el = Hollow.HUD_ELEMENTS.get(i);
                    if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) el.toggle();
                    else {
                        el.set(true);
                        select(el);
                    }
                }
            }
            return true;
        }
        HudElement hit = at(mx, my);
        if (hit == null) {
            select(null);
            return true;
        }
        if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) select(hit);
        else if (e.button() == InputConstants.MOUSE_BUTTON_MIDDLE) hit.set(false);
        else {
            dragging = hit;
            grabX = mx - hit.x(width);
            grabY = my - hit.y(height);
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        mx = (float) e.x();
        my = (float) e.y();
        if (movingPanel) {
            px = mx - grabX;
            py = my - grabY;
            return true;
        }
        if (dragging == null) return settings != null && settings.drag(mx, my);
        float x = mx - grabX, y = my - grabY, w = dragging.width(), h = dragging.height();
        lineX = lineY = -1;
        if (!minecraft.hasShiftDown()) {
            // Screen middle and margins, then the edges of other elements.
            float bestX = SNAP, bestY = SNAP, sx = x, sy = y;
            for (float[] t : targets()) {
                for (float k : new float[] {0, .5f, 1}) {
                    float d = t[0] - (x + w * k);
                    if (t[1] == 0 && Math.abs(d) < bestX) {
                        bestX = Math.abs(d);
                        sx = x + d;
                        lineX = t[0];
                    }
                    d = t[0] - (y + h * k);
                    if (t[1] == 1 && Math.abs(d) < bestY) {
                        bestY = Math.abs(d);
                        sy = y + d;
                        lineY = t[0];
                    }
                }
            }
            x = sx;
            y = sy;
        }
        dragging.moveTo(x, y, width, height);
        return true;
    }

    private List<float[]> targets() {
        List<float[]> out = new java.util.ArrayList<>(List.of(new float[] {6, 0}, new float[] {width / 2f, 0}, new float[] {width - 6, 0}, new float[] {6, 1}, new float[] {height / 2f, 1}, new float[] {height - 6, 1}));
        for (HudElement e : Hollow.HUD_ELEMENTS) {
            if (e == dragging || !e.on()) continue;
            float ex = e.x(width), ey = e.y(height);
            out.add(new float[] {ex, 0});
            out.add(new float[] {ex + e.width(), 0});
            out.add(new float[] {ey, 1});
            out.add(new float[] {ey + e.height(), 1});
            out.add(new float[] {ey + e.height() + 2, 1});
        }
        return out;
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        dragging = null;
        movingPanel = false;
        lineX = lineY = -1;
        if (settings != null) settings.release();
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        mx = (float) x;
        my = (float) y;
        if (overPanel(mx, my)) {
            if (selected == null) listScroll -= (float) sy * ROW * 2;
            else if (!settings.scroll(mx, my, (float) sy)) setScroll -= (float) sy * 20;
        } else {
            HudElement e = dragging != null ? dragging : at(mx, my);
            if (e != null) e.scale.set(e.scale.get() + Math.signum(sy) * .05);
        }
        return true;
    }

    @Override public boolean charTyped(CharacterEvent e) { return settings != null && settings.type(e.codepointAsString()); }

    @Override public boolean keyPressed(KeyEvent e) {
        if (settings != null && settings.key(e)) return true;
        if (e.key() == InputConstants.KEY_ESCAPE && selected != null) {
            select(null);
            return true;
        }
        if (selected != null && !settings.typing()) {
            int dx = e.key() == InputConstants.KEY_RIGHT ? 1 : e.key() == InputConstants.KEY_LEFT ? -1 : 0, dy = e.key() == InputConstants.KEY_DOWN ? 1 : e.key() == InputConstants.KEY_UP ? -1 : 0;
            if (dx != 0 || dy != 0) {
                selected.moveTo(selected.x(width) + dx, selected.y(height) + dy, width, height);
                return true;
            }
        }
        return super.keyPressed(e);
    }
}
