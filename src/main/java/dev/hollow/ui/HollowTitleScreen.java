package dev.hollow.ui;

import com.mojang.realmsclient.RealmsMainScreen;
import dev.hollow.Hollow;
import dev.hollow.core.Colors;
import dev.hollow.module.Interface;
import dev.hollow.render.Cavern;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class HollowTitleScreen extends Screen {
    private static final int SWITCH_AFTER = 620;

    private long shownAt = Util.getMillis(), leaveAt;
    private Runnable leaving;
    private boolean opened, draggingSkin;
    private SkinView skin;
    private float bannerX, bannerY, bannerScale, skinX, skinY, skinTall;

    public HollowTitleScreen() { super(Component.literal("Hollow Client")); }

    @Override public void added() {
        shownAt = Util.getMillis();
        leaving = null;
        opened = true;
    }

    @Override public void removed() { Scene.get().snow.banks.clear(); }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override protected void init() {
        Snow snow = Scene.get().snow;
        snow.banks.clear();
        // Minecraft's banner and ours side by side, shrunk together to fit.
        float natural = 256 + 18 + Brand.width("HOLLOW", 28);
        bannerScale = Math.min(1.1f, Math.min((width * .86f) / natural, height * .18f / 44));
        bannerX = (width - natural * bannerScale) / 2;
        bannerY = Math.max(8, height * .075f);

        boolean online = minecraft.allowsMultiplayer(), mods = FabricLoader.getInstance().isModLoaded("modmenu");
        List<List<Entry>> rows = new ArrayList<>();
        rows.add(List.of(new Entry("Singleplayer", true, () -> minecraft.gui.setScreen(new SelectWorldScreen(this)))));
        rows.add(List.of(new Entry("Multiplayer", online, () -> minecraft.gui.setScreen(minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)))));
        rows.add(List.of(new Entry("Hollow menu", true, () -> minecraft.gui.setScreen(new ClickGui(this)))));
        Entry realms = new Entry("Realms", online, () -> minecraft.gui.setScreen(new RealmsMainScreen(this)));
        rows.add(mods ? List.of(realms, new Entry("Mods", true, this::openMods)) : List.of(realms));
        rows.add(List.of(new Entry("Options", true, () -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options))), new Entry("Quit", true, minecraft::stop)));

        float top = bannerY + 44 * bannerScale + 16, room = height - 24 - top;
        int gap = 5, rowH = Math.clamp((int) ((room - gap * (rows.size() - 1)) / rows.size()), 13, 24);
        int colW = Math.min(190, width / 2 - 20), x = Math.max(10, Math.round(width * .075f)), y = Math.round(top + Math.max(0, (room - rows.size() * rowH - (rows.size() - 1) * gap) / 2));
        int order = 0;
        for (List<Entry> row : rows) {
            int w = (colW - gap * (row.size() - 1)) / row.size();
            for (int i = 0; i < row.size(); i++) {
                Entry e = row.get(i);
                HButton b = new HButton(x + i * (w + gap), y, w, rowH, e.label, Math.min(rowH * .46f, w / 8.2f), 140 + order * 70, () -> leave(e.open, e.label));
                b.active = e.active;
                snow.banks.add(b.bank);
                addRenderableWidget(b);
                order++;
            }
            y += rowH + gap;
        }
        skinX = width * .76f;
        skinY = Scene.get().groundY(skinX) + 2;
        skinTall = Math.min(height * .56f, skinY - top - 6);
        if (skin == null) skin = new SkinView();
    }

    private void openMods() {
        try {
            minecraft.gui.setScreen((Screen) Class.forName("com.terraformersmc.modmenu.api.ModMenuApi").getMethod("createModsScreen", Screen.class).invoke(null, this));
        } catch (ReflectiveOperationException e) {
            Hollow.LOG.warn("Mod Menu couldn't be opened", e);
        }
    }

    private void leave(Runnable to, String label) {
        if (leaving != null) return;
        leaving = to;
        leaveAt = Util.getMillis() + (Interface.calm() ? 0 : SWITCH_AFTER);
        for (var w : children()) if (w instanceof HButton b) b.gone = true;
        // The camera sets off while the button is still falling to pieces.
        Cavern.go(switch (label) {
            case "Singleplayer" -> 1;
            case "Multiplayer" -> 2;
            case "Options" -> 3;
            case "Realms" -> 4;
            case "Mods" -> 5;
            case "Hollow menu" -> 6;
            default -> 0;
        });
    }

    @Override public void tick() {
        if (leaving != null && Util.getMillis() >= leaveAt) {
            Runnable to = leaving;
            leaving = null;
            to.run();
        }
    }

    private float since(float delay, float length) { return Interface.calm() ? 1 : Ease.clamp((Util.getMillis() - shownAt - delay) / length * Interface.speed()); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Scene s = Scene.get();
        s.begin(g);
        s.drawBack(g);
        if (Interface.cavern()) {
            // Standing on the cavern's own floor, under the hole in its roof.
            float[] spot = Cavern.floorSpot(width, height, 1.12f, 2.3f);
            if (spot == null || Cavern.flying() && leaving != null) skinTall = 0;
            else {
                skinX = spot[0];
                skinY = spot[1];
                skinTall = Math.min(spot[2] * 1.12f, height * .6f);
            }
        } else skinY = s.groundY(skinX) + 2;
        if (Hollow.INTERFACE.skin.get() && skinTall > 40) {
            Draw d = new Draw(g);
            d.glow(skinX, skinY - 1, skinTall * .30f, skinTall * .05f, 0x90000814, 0, 32);
            d.flush();
            g.nextStratum();
            skin.draw(g, skinX, skinY, skinTall, mouseX, mouseY);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.nextStratum();
        // Everything that isn't a button drifts off with the camera.
        float out = leaving == null ? 0 : Ease.inOut(Ease.clamp(1 - (leaveAt - Util.getMillis()) / (float) SWITCH_AFTER));
        g.pose().pushMatrix();
        g.pose().translate(0, -out * height * .42f);
        banner(g);
        g.pose().popMatrix();
        super.extractRenderState(g, mouseX, mouseY, a);
        if (Hollow.INTERFACE.skin.get() && skinTall > 40 && leaving == null) nameplate(g);
        float foot = since(900, 500);
        Fonts.BOLD.draw(g, "Hollow Client " + Hollow.version(), Math.max(10, Math.round(width * .075f)), height - 8, 8.5f, Colors.with(Interface.cavern() ? 0xEAF4FA : 0x20303A, foot * (Interface.cavern() ? .7f : 1)));
        Fonts.BOLD.right(g, "Minecraft 26.3, " + Hollow.moduleCount() + " modules", width - 10, height - 8, 8.5f, Colors.with(Interface.cavern() ? 0xEAF4FA : 0x20303A, foot * (Interface.cavern() ? .7f : 1)));
        Scene.get().drawFront(g);
    }

    private void banner(GuiGraphicsExtractor g) {
        float in = since(0, 700), e = Ease.outBack(in), s = bannerScale;
        g.pose().pushMatrix();
        g.pose().translate(bannerX, bannerY + (e - 1) * 40);
        g.pose().scale(s);
        g.blit(RenderPipelines.GUI_TEXTURED, LogoRenderer.MINECRAFT_LOGO, 0, 0, 0, 0, 256, 44, 256, 64, Colors.with(0xFFFFFF, in * 2));
        g.pose().popMatrix();
        float x = bannerX + (256 + 18) * s, h = 28 * s, y = bannerY + 2 * s;
        // Fills with snow after opening, then breathes.
        float fill = Interface.calm() ? 1 : .12f + .5f * since(1300, 5200) + .06f * (float) Math.sin(Frame.time * .7) * since(1300, 5200);
        Draw d = new Draw(g);
        float bar = since(350, 500);
        d.rect(x - 10 * s, bannerY + 2 * s + 40 * s * (1 - Ease.outQuint(bar)) / 2, 1.2f, 40 * s * Ease.outQuint(bar), Colors.alpha(Theme.accent(), bar));
        d.flush();
        Draw.Shade snow = (px, py) -> Colors.mix(0xFFFFFFFF, Theme.at(Ease.clamp((px - x) / (Brand.width("HOLLOW", h)))), Ease.clamp((py - y) / h) * .75f + .1f);
        Brand.word(g, "HOLLOW", x, y, h, new Brand.Look(since(420, 1300), fill, 0xFFFFFFFF, Math.max(1.3f * s, .9f), snow, 1));
        // A rule runs out to the second word, which ends where the first does.
        float small = 10 * s, cw = Brand.width("HOLLOW", h), letters = Brand.width("CLIENT", small), track = 2.6f * s, pen = x + cw - letters - track * 5, rule = Ease.outQuint(since(800, 900));
        d = new Draw(g);
        d.gradientX(x, y + h + 8.4f * s, (pen - x - 7 * s) * rule, 1.2f, Theme.accent() & 0xFFFFFF, Theme.accent());
        d.poly(Draw.diamond(x + (pen - x - 7 * s) * rule, y + h + 9 * s, 2.2f * s), Colors.alpha(Theme.accent(), rule));
        d.flush();
        for (int i = 0; i < 6; i++) {
            String ch = "CLIENT".substring(i, i + 1);
            int c = Theme.flow(i * .5f);
            Brand.word(g, ch, pen, y + h + 4 * s, small, new Brand.Look(since(900 + i * 60, 500), 1, c, .01f, (px, py) -> c, 1));
            pen += Brand.width(ch, small) + track + .13f * small;
        }
    }

    private void nameplate(GuiGraphicsExtractor g) {
        String name = minecraft.getUser().getName();
        float in = Ease.outBack(since(700, 600)), w = Fonts.BOLD.width(name, 10) + 18, x = skinX - w / 2, y = skinY - skinTall - 16 + (1 - in) * 10 + (Interface.calm() ? 0 : (float) Math.sin(Frame.time * 1.3) * 1.2f);
        float alpha = since(700, 400);
        Style.plate(g, x, y, w, 15, alpha);
        Fonts.BOLD.middle(g, name, skinX, Fonts.BOLD.centre(y, 15, 10), 10, Colors.alpha(Theme.text(), alpha));
    }

    private boolean overSkin(double x, double y) { return Hollow.INTERFACE.skin.get() && Math.abs(x - skinX) < skinTall * .26f && y < skinY && y > skinY - skinTall; }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (super.mouseClicked(e, doubleClick)) return true;
        if (!overSkin(e.x(), e.y())) return false;
        draggingSkin = true;
        skin.wave();
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingSkin) skin.drag(dx);
        return draggingSkin || super.mouseDragged(e, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        draggingSkin = false;
        return super.mouseReleased(e);
    }

    private record Entry(String label, boolean active, Runnable open) {}
}
