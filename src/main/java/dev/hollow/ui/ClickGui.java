package dev.hollow.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.module.Interface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

/** Hollow's main menu: one slab of ice with categories, modules and settings side by side. */
public final class ClickGui extends Screen {
    private enum Page { MODULES, THEME, FRIENDS, PROFILES }

    private static final float HEAD = 42, RAIL = 112, SIDE = 190, ROW = 27, PAD = 8;

    /** Where the window was left, for this session. */
    private static float winX = Float.NaN, winY;
    private static Page page = Page.MODULES;
    private static Category category = Category.COMBAT;
    private static Module selected;
    private static final Map<Module, Anim[]> ROWS = new HashMap<>();

    private record Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable middle) {
        boolean has(float px, float py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    private final Screen parent;
    private final long openedAt = Util.getMillis();
    private final Snow snow = new Snow();
    private final SnowBank ledge = new SnowBank(3);
    private final Anim railY = new Anim(Float.NaN), swap = new Anim(1), sideIn = new Anim(1);
    private final Map<String, Anim> anims = new HashMap<>();
    private final TextField search = new TextField("Search", 30, s -> {}), friendName = new TextField("Player name", 16, s -> {}), profileName = new TextField("Profile name", 32, s -> {});
    private final SettingsView theme = new SettingsView(Hollow.INTERFACE.settings);
    private final List<Hit> hits = new ArrayList<>();
    private final PlayerSkinHolder skin = new PlayerSkinHolder();
    private SettingsView settings;
    private Module settingsFor, binding;
    private float listScroll, listTo, sideScroll, sideTo, pageScroll, pageTo, w, h, s = 1, mx, my, x, y, ww, wh, grabX, grabY;
    private boolean moving;
    private String confirm = "";

    /** Looked up once; the lookup itself is cheap afterwards. */
    private static final class PlayerSkinHolder {
        private java.util.function.Supplier<PlayerSkin> s;

        PlayerSkin get() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (s == null) s = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
            return s.get();
        }
    }

    public ClickGui() { this(null); }

    public ClickGui(Screen parent) {
        super(Component.literal("Hollow Client"));
        this.parent = parent;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    @Override public void removed() { Hollow.save(); }

    @Override protected void init() { minecraft.textInputManager().startTextInput(this); }

    private float since(float delay, float length) { return Interface.calm() ? 1 : Ease.clamp((Util.getMillis() - openedAt - delay) / length * Interface.speed()); }

    private Anim anim(String k) { return anims.computeIfAbsent(k, n -> new Anim()); }

    private boolean hot(float ax, float ay, float aw, float ah) { return mx >= ax && mx < ax + aw && my >= ay && my < ay + ah && !moving; }

    private void hit(float ax, float ay, float aw, float ah, Runnable left) { hits.add(new Hit(ax, ay, aw, ah, left, null, null)); }

    private static void sound(float pitch) { if (Hollow.INTERFACE.sounds.get()) net.minecraft.client.Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_HIT, pitch, .55f)); }

    static String keyName(int key) { return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString(); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (minecraft.level == null) {
            Scene.get().begin(g);
            Scene.get().drawBack(g);
        }
        float t = Ease.outCubic(since(0, 450));
        Draw d = new Draw(g);
        // Dark at the edges, clear in the middle, so the glass has something to show.
        d.rect(0, 0, width, height, Colors.with(0x02080F, (minecraft.level == null ? .12f : .30f) * t));
        d.glow(width / 2f, height / 2f, width * .8f, height * .9f, Colors.with(0x02080F, 0), Colors.with(0x02080F, .5f * t), 48);
        d.flush();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        s = Interface.menuScale() * Math.clamp(Math.min(width / 640f, height / 360f), .5f, 1.15f);
        w = width / s;
        h = height / s;
        mx = (float) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()) / s;
        my = (float) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()) / s;
        ww = Math.min(606, w - 20);
        wh = Math.min(336, h - 16);
        x = Float.isNaN(winX) ? (w - ww) / 2 : Math.clamp(winX, 0, Math.max(0, w - ww));
        y = Float.isNaN(winX) ? (h - wh) / 2 : Math.clamp(winY, 0, Math.max(0, h - wh));
        hits.clear();
        g.pose().pushMatrix();
        g.pose().scale(s);
        boolean snows = Hollow.INTERFACE.menuSnow.get();
        snow.banks.clear();
        if (snows) {
            ledge.place(x, y, ww, 10);
            snow.banks.add(ledge);
            snow.update(w, h, .5f);
            Draw d = new Draw(g);
            snow.draw(d, 0, .5f, 0, .7f * since(0, 900));
            d.flush();
        }
        window(g);
        if (snows) {
            Draw d = new Draw(g);
            snow.drawSettled(d, since(700, 600));
            snow.draw(d, .5f, 2, 0, .75f * since(0, 900));
            d.flush();
        }
        g.pose().popMatrix();
    }

    // ----------------------------------------------------------------- window

    private void window(GuiGraphicsExtractor g) {
        // The slab comes forward out of the dark, then its parts slide in from different sides.
        float in = Ease.outQuint(since(0, 620)), alpha = Ease.clamp(since(0, 300) * 1.2f);
        float cx = x + ww / 2, cy = y + wh / 2;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy + (1 - in) * 24);
        g.pose().scale(.86f + .14f * in);
        g.pose().translate(-cx, -cy);
        Style.panel(g, x, y, ww, wh, alpha, .25f + .75f * since(150, 1500));
        float[] shape = Draw.shard(x, y, ww, wh, Theme.cut() * 1.3f);
        Draw d = new Draw(g);
        Ice.glint(d, shape, x, ww, y, wh, since(260, 1100), 1);
        d.flush();
        hits.add(new Hit(x, y, ww, HEAD, () -> {
            moving = true;
            grabX = mx - x;
            grabY = my - y;
            snow.shake(ledge);
        }, null, () -> winX = Float.NaN));
        header(g);
        part(g, -34, 0, since(140, 560), () -> rail(g));
        float bx = x + RAIL, by = y + HEAD, bh = wh - HEAD - PAD, bw = ww - RAIL - PAD;
        float sw = Ease.outQuint(swap.to(1, .32f));
        switch (page) {
            case MODULES -> {
                part(g, 0, 26, since(220, 560), () -> list(g, bx, by, bw - SIDE - PAD, bh, sw));
                part(g, 40, 0, since(300, 560), () -> side(g, x + ww - SIDE - PAD, by, SIDE, bh));
            }
            case THEME -> part(g, 0, 18, sw, () -> theme(g, bx, by, bw, bh));
            case FRIENDS -> part(g, 0, 18, sw, () -> friends(g, bx, by, bw, bh));
            case PROFILES -> part(g, 0, 18, sw, () -> profiles(g, bx, by, bw, bh));
        }
        g.pose().popMatrix();
    }

    private void part(GuiGraphicsExtractor g, float dx, float dy, float t, Runnable draw) {
        if (t <= 0) return;
        float e = 1 - Ease.outQuint(t);
        g.pose().pushMatrix();
        g.pose().translate(dx * e, dy * e);
        draw.run();
        g.pose().popMatrix();
    }

    /** A hollow cut into the slab. */
    private void well(GuiGraphicsExtractor g, float ax, float ay, float aw, float ah) {
        Draw d = new Draw(g);
        float[] p = Draw.shard(ax, ay, aw, ah, Math.min(Theme.cut(), 7));
        d.poly(p, Colors.with(0x01070E, .30f));
        d.poly(Draw.clip(p, 0, -1, -(ay + 10)), (px, py) -> Colors.with(0, .16f * (1 - (py - ay) / 10)));
        d.stroke(Draw.offset(p, -.4f), .8f, (px, py) -> Colors.with(0xFFFFFF, .03f + .09f * Ease.clamp((py - ay) / ah)), 0, 1);
        d.flush();
    }

    private void header(GuiGraphicsExtractor g) {
        float tall = 18, bw = Brand.lineWidth(tall), in = since(120, 1100);
        long on = Hollow.modules().stream().filter(m -> m.on() && Category.COLUMNS.contains(m.category)).count();
        Draw d = new Draw(g);
        d.glow(x + ww / 2, y + 20, bw * .7f, 34, Colors.with(Theme.accent(), .16f * in), Theme.accent() & 0xFFFFFF, 32);
        d.gradientX(x + 14, y + HEAD - 4, ww / 2 - 14, 1, 0x00FFFFFF, 0x30FFFFFF);
        d.gradientX(x + ww / 2, y + HEAD - 4, ww / 2 - 14, 1, 0x30FFFFFF, 0x00FFFFFF);
        d.flush();
        Brand.line(g, x + (ww - bw) / 2, y + 10, tall, in, Math.clamp(.2f + on / 24f, .2f, .88f) * since(400, 1800), 1, .25f);
        float fw = Math.min(124, (ww - bw) / 2 - 26);
        if (fw > 40) {
            search.draw(g, x + ww - fw - 12, y + 12, fw, 17, 9, since(350, 400));
            hit(x + ww - fw - 12, y + 12, fw, 17, () -> {
                search.focused = true;
                open(Page.MODULES);
            });
        }
        Fonts.TEXT.draw(g, "Hollow Client " + Hollow.version(), x + 14, y + 24, 8.5f, Colors.with(Theme.text(), .55f * in));
    }

    private void open(Page p) {
        if (p == page) return;
        page = p;
        swap.snap(0);
        pageTo = pageScroll = 0;
        confirm = "";
    }

    private void open(Category c) {
        sound(1.15f);
        search.set("");
        open(Page.MODULES);
        if (c == category) return;
        category = c;
        swap.snap(0);
        listTo = listScroll = 0;
    }

    // ------------------------------------------------------------------- rail

    private void rail(GuiGraphicsExtractor g) {
        float rx = x + PAD, ry = y + HEAD, rw = RAIL - PAD * 2, rowH = 22, cy = ry + 2;
        boolean searching = !search.text.isBlank();
        List<Object[]> rows = new ArrayList<>();
        for (Category c : Category.COLUMNS) rows.add(new Object[] {c.title, page == Page.MODULES && category == c && !searching, (Runnable) () -> open(c), Hollow.modules().stream().filter(m -> m.category == c && m.on()).count()});
        rows.add(null);
        rows.add(new Object[] {"HUD editor", false, (Runnable) () -> minecraft.gui.setScreen(new HudEditor(this)), 0L});
        rows.add(new Object[] {"Theme", page == Page.THEME, (Runnable) () -> openPage(Page.THEME), 0L});
        rows.add(new Object[] {"Friends", page == Page.FRIENDS, (Runnable) () -> openPage(Page.FRIENDS), (long) Hollow.FRIENDS.all().size()});
        rows.add(new Object[] {"Profiles", page == Page.PROFILES, (Runnable) () -> openPage(Page.PROFILES), 0L});
        float target = Float.NaN, yy = cy;
        for (Object[] r : rows) {
            if (r == null) {
                yy += 9;
                continue;
            }
            if ((Boolean) r[1]) target = yy;
            yy += rowH;
        }
        if (!Float.isNaN(target)) {
            if (Float.isNaN(railY.v)) railY.snap(target);
            float ty = railY.spring(target, 340, 27);
            float[] mark = Draw.shard(rx, ty, rw, rowH - 2, 6);
            Draw d = new Draw(g);
            d.shadow(mark, 8, Colors.with(Theme.accent(), .22f));
            d.poly(mark, (px, py) -> Colors.alpha(Theme.at(Ease.clamp((px - rx) / rw)), .62f));
            Ice.rim(d, mark, 1, .9f);
            d.flush();
        }
        int n = 0;
        for (Object[] r : rows) {
            if (r == null) {
                Draw d = new Draw(g);
                d.gradientX(rx + 4, cy + 4, rw - 8, 1, 0x26FFFFFF, 0x00FFFFFF);
                d.flush();
                cy += 9;
                continue;
            }
            boolean on = (Boolean) r[1], over = hot(rx, cy, rw, rowH - 2);
            float v = anim("rail" + r[0]).to(over, .16f), o = anim("railon" + r[0]).to(on, .22f), t = Ease.outQuint(since(180 + n++ * 34, 460));
            Fonts.BOLD.draw(g, (String) r[0], rx + 9 + v * 3 - (1 - t) * 14, Fonts.BOLD.centre(cy, rowH - 2, 10), 10, Colors.with(Colors.mix(0xFFB9CCDA, 0xFFFFFFFF, Math.max(v, o)), t));
            if ((Long) r[3] > 0) Fonts.BOLD.right(g, Long.toString((Long) r[3]), rx + rw - 8, Fonts.BOLD.centre(cy, rowH - 2, 8.5f), 8.5f, Colors.with(Colors.mix(Theme.accent(), 0xFFFFFFFF, o), t));
            hit(rx, cy, rw, rowH - 2, (Runnable) r[2]);
            cy += rowH;
        }
        float fy = y + wh - PAD - 22;
        if (fy > cy) {
            PlayerFaceExtractor.extractRenderState(g, skin.get(), Math.round(rx + 4), Math.round(fy + 3), 16);
            Fonts.BOLD.draw(g, Fonts.BOLD.clip(minecraft.getUser().getName(), 9, rw - 30), rx + 25, fy + 10, 9, Theme.text());
            Fonts.TEXT.draw(g, Hollow.moduleCount() + " modules", rx + 25, fy + 19, 8, Colors.with(Theme.text(), .55f));
        }
    }

    private void openPage(Page p) {
        sound(1.2f);
        open(p);
    }

    // ------------------------------------------------------------------- list

    private static Anim[] row(Module m) { return ROWS.computeIfAbsent(m, k -> new Anim[] {new Anim(k.on() ? 1 : 0), new Anim(), new Anim()}); }

    private List<Module> shown() {
        String q = search.text.strip().toLowerCase(Locale.ROOT).replace(" ", "");
        if (q.isEmpty()) return Hollow.modules().stream().filter(m -> m.category == category).toList();
        List<Module> out = new ArrayList<>(Hollow.modules().stream().filter(m -> Category.COLUMNS.contains(m.category) && m.name.toLowerCase(Locale.ROOT).contains(q)).toList());
        Hollow.modules().stream().filter(m -> Category.COLUMNS.contains(m.category) && !out.contains(m) && m.description.toLowerCase(Locale.ROOT).replace(" ", "").contains(q)).forEach(out::add);
        return out;
    }

    private void list(GuiGraphicsExtractor g, float lx, float ly, float lw, float lh, float sw) {
        List<Module> list = shown();
        boolean searching = !search.text.isBlank();
        well(g, lx, ly, lw, lh);
        if (list.isEmpty()) {
            Fonts.TEXT.middle(g, "Nothing matches \"" + Fonts.TEXT.clip(search.text, 9.5f, lw - 110) + "\"", lx + lw / 2, ly + lh / 2, 9.5f, Theme.dim());
            return;
        }
        if (selected == null || !list.contains(selected) && !searching && selected.category != category) select(list.getFirst());
        float max = Math.max(0, list.size() * ROW + 6 - lh);
        listTo = Math.clamp(listTo, 0, max);
        listScroll = Ease.approach(listScroll, listTo, 15, Frame.dt);
        float top = ly + 1, bottom = ly + lh - 1;
        g.enableScissor((int) Math.floor(lx), (int) Math.ceil(top), (int) Math.ceil(lx + lw), (int) Math.floor(bottom));
        for (int i = 0; i < list.size(); i++) {
            float ry = ly + 3 + i * ROW - listScroll;
            if (ry + ROW < top || ry > bottom) continue;
            // Rows drop in one after another when the category changes.
            float t = Ease.clamp(swap.v * 2.2f - (ry - ly) / Math.max(1, lh) * 1.1f), e = Ease.outQuint(t);
            if (t > 0) row(g, list.get(i), lx + 3 + (1 - e) * 18, ry, lw - 9, top, bottom, e, searching);
        }
        g.disableScissor();
        Draw d = new Draw(g);
        if (listScroll > 1) d.gradient(lx + 1, ly + 1, lw - 2, 12, Colors.with(Theme.ink(), .55f), Theme.ink() & 0xFFFFFF);
        if (listScroll < max - 1) d.gradient(lx + 1, ly + lh - 15, lw - 2, 14, Theme.ink() & 0xFFFFFF, Colors.with(Theme.ink(), .6f));
        if (max > 0) {
            float bh = Math.max(18, lh * lh / (list.size() * ROW + 6));
            d.poly(Draw.round(lx + lw - 4.2f, ly + 3 + (lh - 6 - bh) * listScroll / max, 2.2f, bh, 1.1f), Colors.with(Theme.accent(), .7f));
        }
        d.flush();
    }

    private void select(Module m) {
        if (selected == m && settingsFor == m) return;
        selected = m;
        binding = null;
        sideIn.snap(0);
        sideTo = sideScroll = 0;
    }

    private void row(GuiGraphicsExtractor g, Module m, float rx, float ry, float rw, float top, float bottom, float alpha, boolean searching) {
        float a = Math.max(ry, top), b = Math.min(ry + ROW - 2, bottom);
        boolean over = b > a && hot(rx, a, rw, b - a);
        Anim[] an = row(m);
        float on = an[0].to(m.on(), .34f), v = an[1].to(over, .16f), sel = an[2].to(selected == m, .2f), e = Ease.outQuint(on), rh = ROW - 2;
        float[] p = Draw.shard(rx, ry, rw, rh, 6);
        Draw d = new Draw(g);
        d.poly(p, Colors.with(0xFFFFFF, (.018f + .05f * v + .035f * sel) * alpha));
        switch (Hollow.INTERFACE.moduleStyle.get()) {
            case "Fill" -> {
                if (on > 0) d.sweep(p, e, (px, py) -> Colors.alpha(Theme.at(Ease.clamp((px - rx) / rw)), (.46f - .20f * Ease.clamp((px - rx) / rw)) * alpha));
            }
            case "Edge" -> d.gradientX(rx + 1, ry + 1, rw * .6f * e, rh - 2, Colors.with(Theme.accent(), .30f * on * alpha), Theme.accent() & 0xFFFFFF);
            default -> {}
        }
        if (on > 0) {
            float[] bar = Draw.round(rx + 2, ry + rh / 2 - rh * .34f * e, 2.2f, rh * .68f * e, 1.1f);
            d.shadow(bar, 5, Colors.with(Theme.accent(), .4f * on * alpha));
            d.poly(bar, Colors.with(Theme.accent(), on * alpha));
        }
        if (sel > 0 || v > 0 || on > 0) Ice.rim(d, p, alpha * Math.max(Math.max(sel, v * .7f), on * .55f), .8f);
        Style.toggle(d, rx + rw - 31, ry + rh / 2 - 6, 24, 12, on, v, alpha);
        d.flush();
        float tx = rx + 10 + v * 2, room = rw - 52;
        Fonts.BOLD.draw(g, m.name, tx, ry + 11, 10, Colors.with(Colors.mix(0xFFD5E4EE, 0xFFFFFFFF, Math.max(on, v)), alpha));
        float nw = Fonts.BOLD.width(m.name, 10);
        String tag = searching ? m.category.title : m.key != 0 ? keyName(m.key) : "";
        if (!tag.isEmpty() && nw + 40 < room) {
            float tw = Fonts.BOLD.width(tag, 7.5f) + 8;
            Draw c = new Draw(g);
            c.poly(Draw.round(tx + nw + 6, ry + 3.6f, tw, 9.5f, 3), Colors.with(0xFFFFFF, .10f * alpha));
            c.flush();
            Fonts.BOLD.middle(g, tag, tx + nw + 6 + tw / 2, Fonts.BOLD.centre(ry + 3.6f, 9.5f, 7.5f), 7.5f, Colors.with(0xFFD5E4EE, .85f * alpha));
        }
        Fonts.TEXT.draw(g, Fonts.TEXT.clip(m.description, 8, room), tx, ry + 20.5f, 8, Colors.with(0xFFC3D6E3, (.55f + .25f * v) * alpha));
        Ice.drawCracks(g, m, rx, a, rw, Math.max(0, b - a), alpha);
        if (b > a) hits.add(new Hit(rx, a, rw, b - a, () -> {
            m.toggle();
            select(m);
            Ice.crack(m, mx, my, 34);
            sound(m.on() ? 1.35f : .85f);
        }, () -> {
            select(m);
            sound(1.1f);
        }, () -> {
            select(m);
            binding = m;
        }));
    }

    // --------------------------------------------------------------- settings

    private void side(GuiGraphicsExtractor g, float sx, float sy, float sw, float sh) {
        well(g, sx, sy, sw, sh);
        Module m = selected;
        if (m == null) return;
        if (settingsFor != m) {
            settingsFor = m;
            settings = new SettingsView(m.settings);
        }
        float e = Ease.outQuint(sideIn.to(1, .3f)), ix = sx + 10 + (1 - e) * 14, iw = sw - 20, cy = sy + 9;
        float title = Math.min(12, 12 * (iw - 32) / Fonts.DISPLAY.width(m.name, 12));
        Fonts.DISPLAY.draw(g, m.name, ix, cy + 10, title, Colors.with(0xFFFFFFFF, e));
        Draw d = new Draw(g);
        Style.toggle(d, ix + iw - 27, cy + .5f, 27, 13.5f, row(m)[0].v, hot(ix + iw - 28, cy, 28, 14) ? 1 : 0, e);
        d.flush();
        hit(ix + iw - 28, cy, 28, 14, () -> {
            m.toggle();
            sound(m.on() ? 1.35f : .85f);
        });
        cy += 17;
        for (String line : Fonts.TEXT.wrap(m.description, 8.5f, iw)) {
            Fonts.TEXT.draw(g, line, ix, cy + 7, 8.5f, Colors.with(0xFFC3D6E3, .8f * e));
            cy += 10.5f;
        }
        cy += 4;
        String bind = binding == m ? "Press a key" : m.key == 0 ? "No key" : keyName(m.key);
        float bw = iw * .44f, cw = (iw - bw - 8) / 2;
        chip(g, ix, cy, bw, bind, binding == m, () -> binding = binding == m ? null : m);
        chip(g, ix + bw + 4, cy, cw, "Hold", m.hold, () -> m.hold = !m.hold);
        chip(g, ix + bw + cw + 8, cy, cw, "Listed", !m.hidden, () -> m.hidden = !m.hidden);
        cy += 22;
        float top = cy, end = sy + sh - (m.settings.isEmpty() ? 8 : 25), max = Math.max(0, settings.height - (end - top));
        sideTo = Math.clamp(sideTo, 0, max);
        sideScroll = Ease.approach(sideScroll, sideTo, 15, Frame.dt);
        g.enableScissor((int) Math.floor(sx), (int) Math.floor(top), (int) Math.ceil(sx + sw), (int) Math.ceil(end));
        settings.draw(g, ix, top - sideScroll, iw, top, end, mx, my, e);
        g.disableScissor();
        if (m.settings.isEmpty()) Fonts.TEXT.draw(g, "Nothing to adjust here.", ix, top + 10, 8.5f, Colors.with(Theme.dim(), e));
        else chip(g, ix, sy + sh - 20, iw, "Reset settings", false, () -> m.settings.forEach(Setting::reset));
        if (max > 0) {
            d = new Draw(g);
            float bh = Math.max(12, (end - top) * (end - top) / settings.height);
            d.poly(Draw.round(sx + sw - 4.2f, top + (end - top - bh) * sideScroll / max, 2.2f, bh, 1.1f), Colors.with(Theme.accent(), .7f));
            d.flush();
        }
    }

    private void chip(GuiGraphicsExtractor g, float ax, float ay, float aw, String label, boolean on, Runnable click) {
        boolean over = hot(ax, ay, aw, 16);
        String k = label + (int) ax + "," + (int) (ay / 3);
        float v = anim(k).to(over, .18f), f = anim("on" + k).to(on, .25f);
        float[] p = Draw.shard(ax, ay, aw, 16, 5);
        Draw d = new Draw(g);
        d.poly(p, Colors.with(0xFFFFFF, .055f + .07f * v));
        if (f > 0) d.sweep(p, Ease.outQuint(f), (px, py) -> Colors.alpha(Theme.at(Ease.clamp((px - ax) / aw)), .72f));
        Ice.rim(d, p, 1, .5f + .5f * Math.max(v, f));
        d.flush();
        Fonts.BOLD.middle(g, Fonts.BOLD.clip(label, 8.5f, aw - 8), ax + aw / 2, Fonts.BOLD.centre(ay, 16, 8.5f), 8.5f, Colors.mix(0xFFD5E4EE, 0xFFFFFFFF, Math.max(v, f)));
        hit(ax, ay, aw, 16, () -> {
            sound(1.15f);
            click.run();
        });
    }

    // ------------------------------------------------------------------ pages

    private void theme(GuiGraphicsExtractor g, float ax, float ay, float aw, float ah) {
        float left = aw * .42f;
        well(g, ax, ay, left - 4, ah);
        well(g, ax + left + 4, ay, aw - left - 4, ah);
        float cx = ax + 10, cw = left - 24, cy = ay + 9, cellW = (cw - 6) / 2, cellH = Math.min(31, (ah - 108) / 4 - 5);
        Fonts.DISPLAY.draw(g, "Themes", cx, cy + 8, 10, 0xFFFFFFFF);
        cy += 17;
        for (int i = 0; i < Theme.PRESETS.length; i++) {
            Theme.Preset p = Theme.PRESETS[i];
            float px0 = cx + i % 2 * (cellW + 6), py0 = cy + i / 2 * (cellH + 5);
            boolean current = Hollow.INTERFACE.preset.index() == i, over = hot(px0, py0, cellW, cellH);
            float v = anim("t" + i).to(over || current, .26f), e = Ease.outBack(v);
            float[] shape = Draw.shard(px0, py0, cellW, cellH, 7);
            Draw d = new Draw(g);
            if (current) d.shadow(shape, 9, Colors.with(p.accent(), .3f));
            d.poly(shape, Colors.with(p.ink(), .85f));
            // A ball of the theme's own light.
            float bx = px0 + cellH * .55f, r = cellH * (.24f + .07f * e);
            d.glow(bx, py0 + cellH / 2, cellH * (1 + e), cellH * .9f, Colors.with(p.accent(), .35f), p.accent() & 0xFFFFFF, 24);
            d.circle(bx, py0 + cellH / 2, r, p.second());
            d.circle(bx - r * .18f, py0 + cellH / 2 - r * .2f, r * .72f, p.accent());
            Ice.rim(d, shape, 1, current ? 1.4f : .5f + .5f * v);
            d.flush();
            Fonts.BOLD.draw(g, p.name(), px0 + cellH * 1.08f, Fonts.BOLD.centre(py0, cellH, 9.5f), 9.5f, Colors.mix(0xFFCFDFEA, 0xFFFFFFFF, v));
            int n = i;
            hit(px0, py0, cellW, cellH, () -> {
                Hollow.INTERFACE.preset.pick(n);
                sound(1 + n * .06f);
            });
        }
        cy += 4 * (cellH + 5) + 3;
        chip(g, cx, cy, cw, "Make it my own", Hollow.INTERFACE.preset.index() == Theme.PRESETS.length, Hollow.INTERFACE::customise);
        chip(g, cx, cy + 20, cw, "Put the window back in the middle", false, () -> winX = Float.NaN);
        float by = cy + 46, tall = Math.min(17, cw / Brand.lineWidth(1));
        if (by + tall < ay + ah) Brand.line(g, cx, by, tall, 1, .55f, 1);
        float sx = ax + left + 14, sw = aw - left - 28, top = ay + 8, end = ay + ah - 8, max = Math.max(0, theme.height - (end - top));
        pageTo = Math.clamp(pageTo, 0, max);
        pageScroll = Ease.approach(pageScroll, pageTo, 15, Frame.dt);
        g.enableScissor((int) sx - 6, (int) top, (int) (sx + sw + 6), (int) end);
        theme.draw(g, sx, top - pageScroll, sw, top, end, mx, my, 1);
        g.disableScissor();
        if (max > 0) {
            Draw d = new Draw(g);
            float bh = (end - top) * (end - top) / theme.height;
            d.poly(Draw.round(ax + aw - 5, top + (end - top - bh) * pageScroll / max, 2.2f, bh, 1.1f), Colors.with(Theme.accent(), .7f));
            d.flush();
        }
    }

    private void lines(GuiGraphicsExtractor g, float ax, float ay, float aw, float end, List<String> names, Function<String, Object[]> buttons) {
        float max = Math.max(0, names.size() * 25 - (end - ay));
        pageTo = Math.clamp(pageTo, 0, max);
        pageScroll = Ease.approach(pageScroll, pageTo, 15, Frame.dt);
        g.enableScissor((int) ax, (int) ay, (int) (ax + aw), (int) end);
        for (int i = 0; i < names.size(); i++) {
            float ry = ay + i * 25 - pageScroll;
            if (ry < ay - 25 || ry > end) continue;
            String n = names.get(i);
            float v = anim("line" + n).to(hot(ax, ry, aw, 22), .16f);
            float[] p = Draw.shard(ax, ry, aw, 22, 6);
            Draw d = new Draw(g);
            d.poly(p, Colors.with(0xFFFFFF, .035f + .05f * v));
            Ice.rim(d, p, .4f + .6f * v, .6f);
            d.flush();
            Fonts.BOLD.draw(g, n, ax + 11, Fonts.BOLD.centre(ry, 22, 10), 10, Theme.text());
            Object[] b = buttons.apply(n);
            float bx = ax + aw - 4;
            for (int j = b.length - 2; j >= 0; j -= 2) {
                float bw = Fonts.BOLD.width((String) b[j], 8.5f) + 18;
                bx -= bw;
                chip(g, bx, ry + 3, bw, (String) b[j], false, (Runnable) b[j + 1]);
                bx -= 4;
            }
        }
        g.disableScissor();
    }

    private float page(GuiGraphicsExtractor g, float ax, float ay, float aw, float ah, String title, String words, TextField field, String button, Runnable press) {
        well(g, ax, ay, aw, ah);
        float cx = ax + 12, cy = ay + 10, cw = aw - 24;
        Fonts.DISPLAY.draw(g, title, cx, cy + 8, 11, 0xFFFFFFFF);
        Fonts.TEXT.draw(g, words, cx, cy + 21, 8.5f, Colors.with(0xFFC3D6E3, .8f));
        cy += 29;
        field.draw(g, cx, cy, cw - 72, 18, 9.5f, 1);
        hit(cx, cy, cw - 72, 18, () -> field.focused = true);
        chip(g, cx + cw - 66, cy + 1, 66, button, false, press);
        return cy + 27;
    }

    private void friends(GuiGraphicsExtractor g, float ax, float ay, float aw, float ah) {
        float cy = page(g, ax, ay, aw, ah, "Friends", "Friends are never attacked, trapped or aimed at.", friendName, "Add", this::addFriend);
        if (Hollow.FRIENDS.all().isEmpty()) Fonts.TEXT.draw(g, "Nobody yet. Add a name above, or middle click a player with MiddleClick on.", ax + 12, cy + 10, 8.5f, Theme.dim());
        lines(g, ax + 12, cy, aw - 24, ay + ah - 8, new ArrayList<>(Hollow.FRIENDS.all()), n -> new Object[] {"Remove", (Runnable) () -> Hollow.FRIENDS.remove(n)});
    }

    private void addFriend() {
        String n = friendName.text.strip();
        if (!n.isEmpty()) Hollow.FRIENDS.add(n);
        friendName.set("");
    }

    private void profiles(GuiGraphicsExtractor g, float ax, float ay, float aw, float ah) {
        float cy = page(g, ax, ay, aw, ah, "Profiles", "A profile keeps every module, setting, key, HUD position and colour.", profileName, "Save", () -> {
            if (Hollow.saveProfile(profileName.text)) profileName.set("");
        });
        List<String> names = Hollow.profiles();
        if (names.isEmpty()) Fonts.TEXT.draw(g, "No profiles saved yet. Name one above to keep the current setup.", ax + 12, cy + 10, 8.5f, Theme.dim());
        lines(g, ax + 12, cy, aw - 24, ay + ah - 8, names, n -> new Object[] {
            "Load", (Runnable) () -> Hollow.loadProfile(n),
            "Update", (Runnable) () -> Hollow.saveProfile(n),
            confirm.equals(n) ? "Really delete" : "Delete", (Runnable) () -> {
                if (confirm.equals(n)) Hollow.deleteProfile(n);
                confirm = confirm.equals(n) ? "" : n;
            }});
    }

    // ------------------------------------------------------------------ input

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        mx = (float) e.x() / s;
        my = (float) e.y() / s;
        search.focused = friendName.focused = profileName.focused = false;
        theme.unfocus();
        if (settings != null) settings.unfocus();
        if (page == Page.MODULES && settings != null && settings.click(mx, my, e.button()) || page == Page.THEME && theme.click(mx, my, e.button())) {
            sound(1.1f);
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit t = hits.get(i);
            if (!t.has(mx, my)) continue;
            Runnable r = e.button() == InputConstants.MOUSE_BUTTON_LEFT ? t.left : e.button() == InputConstants.MOUSE_BUTTON_RIGHT ? t.right : t.middle;
            if (r != null) r.run();
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        float px = (float) e.x() / s, py = (float) e.y() / s;
        if (moving) {
            winX = px - grabX;
            winY = py - grabY;
            return true;
        }
        return settings != null && settings.drag(px, py) || theme.drag(px, py);
    }

    @Override public boolean mouseReleased(MouseButtonEvent e) {
        moving = false;
        ledge.shaken = false;
        if (settings != null) settings.release();
        theme.release();
        return true;
    }

    @Override public boolean mouseScrolled(double sx0, double sy0, double dx, double dy) {
        mx = (float) sx0 / s;
        my = (float) sy0 / s;
        float by = (float) dy;
        if (page != Page.MODULES) pageTo -= by * 28;
        else if (mx >= x + ww - SIDE - PAD) {
            if (settings == null || !settings.scroll(mx, my, by)) sideTo -= by * 26;
        } else listTo -= by * ROW * 1.8f;
        return true;
    }

    @Override public boolean charTyped(CharacterEvent e) {
        String t = e.codepointAsString();
        if (binding != null) return true;
        if (settings != null && settings.type(t) || theme.type(t) || friendName.type(t) || profileName.type(t)) return true;
        if (page == Page.MODULES) {
            if (search.text.isEmpty()) swap.snap(.3f);
            search.focused = true;
            listTo = 0;
            return search.type(t);
        }
        return false;
    }

    @Override public boolean keyPressed(KeyEvent e) {
        if (binding != null) {
            if (e.key() == InputConstants.KEY_DELETE || e.key() == InputConstants.KEY_BACKSPACE) binding.key = 0;
            else if (e.key() != InputConstants.KEY_ESCAPE) binding.key = e.key();
            binding = null;
            sound(1.4f);
            return true;
        }
        if (settings != null && settings.key(e) || theme.key(e) || search.key(e) || friendName.key(e) || profileName.key(e)) return true;
        if (e.key() == InputConstants.KEY_RETURN && friendName.focused) {
            addFriend();
            return true;
        }
        if (e.key() == InputConstants.KEY_ESCAPE && !search.text.isEmpty()) {
            search.set("");
            return true;
        }
        if (e.key() == InputConstants.KEY_RSHIFT && !(settings != null && settings.typing())) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }
}
