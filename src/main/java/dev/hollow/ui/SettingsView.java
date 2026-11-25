package dev.hollow.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.core.Colors;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Draws a list of settings with a control for each, and handles clicks on them. */
public final class SettingsView {
    private static final float SIZE = 8.5f, PICK_ROW = 13, PICK_ROWS = 7;

    private final List<Setting> settings;
    private final Map<Setting, Anim> shown = new HashMap<>(), open = new HashMap<>(), over = new HashMap<>(), on = new HashMap<>();
    private final Map<Setting, TextField> fields = new HashMap<>();
    private final Map<Setting, Float> pickScroll = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private Setting expanded, dragging;
    private int dragPart;
    private Hit dragHit;
    private float x, y, w, mx, my;
    /** Height everything takes up, known after drawing. */
    public float height;

    private record Hit(float x, float y, float w, float h, Setting s, int part, String id) {
        boolean has(float px, float py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    public SettingsView(List<Setting> settings) { this.settings = settings; }

    private static Anim of(Map<Setting, Anim> m, Setting s) { return m.computeIfAbsent(s, k -> new Anim()); }

    /** Only what lies between top and bottom takes clicks. */
    public void draw(GuiGraphicsExtractor g, float x, float y, float w, float top, float bottom, float mx, float my, float alpha) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.mx = mx;
        this.my = my;
        hits.clear();
        float cy = y;
        String section = "";
        for (Setting s : settings) {
            Anim vis = of(shown, s);
            if (!shown.containsKey(s) || height == 0) vis.snap(s.visible() ? 1 : 0);
            float v = Ease.inOut(vis.to(s.visible(), .2f));
            if (v <= 0) continue;
            if (!s.section.equals(section)) {
                section = s.section;
                if (!section.isEmpty()) {
                    cy += cy == y ? 0 : 5;
                    Fonts.DISPLAY.draw(g, section, x, cy + 10, 8, Colors.alpha(Theme.accent(), alpha));
                    float tw = Fonts.DISPLAY.width(section, 8);
                    Draw d = new Draw(g);
                    d.gradientX(x + tw + 6, cy + 6.6f, w - tw - 6, 1, Colors.alpha(Theme.faint(), alpha), Theme.faint() & 0xFFFFFF);
                    d.flush();
                    cy += 16;
                }
            }
            float full = rowHeight(s), h = full * v;
            if (cy + h > top - 4 && cy < bottom + 4) {
                boolean clipped = v < 1 || of(open, s).v > 0 && of(open, s).v < 1;
                if (clipped) g.enableScissor((int) Math.floor(x - 4), (int) Math.max(top, cy), (int) Math.ceil(x + w + 4), (int) Math.min(bottom, cy + h + 1));
                row(g, s, x, cy, w, alpha * v, top, bottom);
                if (clipped) g.disableScissor();
            }
            cy += h;
        }
        height = cy - y;
    }

    private float extra(Setting s) {
        return switch (s) {
            case Setting.Choice c -> c.options.size() * 12 + 3;
            case Setting.Color c -> 76;
            case Setting.Ids i -> 18 + PICK_ROW * PICK_ROWS + 6;
            default -> 0;
        };
    }

    private float rowHeight(Setting s) {
        float o = Ease.inOut(of(open, s).to(expanded == s, .22f));
        return switch (s) {
            case Setting.Num n -> 24;
            case Setting.Text t -> 30;
            default -> 16 + extra(s) * o;
        };
    }

    private void hit(float hx, float hy, float hw, float hh, Setting s, int part, String id, float top, float bottom) {
        float a = Math.max(hy, top), b = Math.min(hy + hh, bottom);
        if (b > a) hits.add(new Hit(hx, a, hw, b - a, s, part, id));
    }

    private void row(GuiGraphicsExtractor g, Setting s, float x, float y, float w, float alpha, float top, float bottom) {
        boolean inside = my >= top && my < bottom && mx >= x - 4 && mx < x + w + 4;
        float hover = of(over, s).to(inside && my >= y && my < y + (s instanceof Setting.Num ? 24 : 16), .15f);
        int label = Colors.alpha(Colors.mix(Colors.mix(Theme.dim(), Theme.text(), .5f), 0xFFFFFFFF, hover), alpha);
        float base = Fonts.TEXT.centre(y, 16, SIZE);
        switch (s) {
            case Setting.Bool b -> {
                Fonts.TEXT.draw(g, s.name, x, base, SIZE, label);
                Draw d = new Draw(g);
                Style.toggle(d, x + w - 21, y + 3, 20, 10, of(on, s).to(b.get(), .28f), hover, alpha);
                d.flush();
                hit(x, y, w, 16, s, 0, null, top, bottom);
            }
            case Setting.Num n -> {
                Fonts.TEXT.draw(g, s.name, x, y + 10, SIZE, label);
                Fonts.BOLD.right(g, n.text(), x + w, y + 10, SIZE, Colors.alpha(Theme.text(), alpha));
                float t = (float) ((n.get() - n.min) / (n.max - n.min));
                Draw d = new Draw(g);
                Style.track(d, x + 4, y + 17, w - 8, of(on, s).glide(t, 24), dragging == s ? 1 : hover, alpha);
                d.flush();
                hit(x, y, w, 24, s, 0, null, top, bottom);
            }
            case Setting.Text t -> {
                Fonts.TEXT.draw(g, s.name, x, y + 9, SIZE, label);
                TextField f = fields.computeIfAbsent(s, k -> new TextField("", Setting.Text.MAX, t::set));
                if (!f.focused) f.text = t.get();
                f.draw(g, x, y + 12, w, 15, SIZE, alpha);
                hit(x, y + 12, w, 15, s, 0, null, top, bottom);
            }
            case Setting.Choice c -> {
                Fonts.TEXT.draw(g, s.name, x, base, SIZE, label);
                float o = of(open, s).v;
                chevron(g, x + w - 4, y + 8, o, alpha);
                Fonts.BOLD.right(g, c.get(), x + w - 12, base, SIZE, Colors.alpha(Theme.accent(), alpha));
                hit(x, y, w, 16, s, 0, null, top, bottom);
                if (o <= 0) return;
                for (int i = 0; i < c.options.size(); i++) {
                    float oy = y + 16 + i * 12;
                    boolean hot = inside && my >= oy && my < oy + 12;
                    Draw d = new Draw(g);
                    if (hot) d.poly(Draw.shard(x + 4, oy, w - 4, 12, 4), Colors.with(0xFFFFFF, .06f * alpha));
                    Style.check(d, x + 11, oy + 6, 3.4f, c.index() == i ? 1 : 0, hot ? 1 : 0, alpha * o);
                    d.flush();
                    Fonts.TEXT.draw(g, c.options.get(i), x + 19, Fonts.TEXT.centre(oy, 12, SIZE), SIZE, Colors.alpha(c.index() == i ? Theme.text() : Theme.dim(), alpha * o));
                    if (o > .9f) hit(x, oy, w, 12, s, 1 + i, null, top, bottom);
                }
            }
            case Setting.Color c -> color(g, c, x, y, w, alpha, hover, label, base, top, bottom);
            case Setting.Ids i -> ids(g, i, x, y, w, alpha, inside, label, base, top, bottom);
        }
    }

    private static void chevron(GuiGraphicsExtractor g, float cx, float cy, float open, float alpha) {
        Draw d = new Draw(g);
        float k = 2.6f * (1 - open * 2);
        d.line(cx - 3, cy - k / 2, cx, cy + k / 2, 1.2f, Colors.alpha(Theme.dim(), alpha));
        d.line(cx, cy + k / 2, cx + 3, cy - k / 2, 1.2f, Colors.alpha(Theme.dim(), alpha));
        d.flush();
    }

    private void color(GuiGraphicsExtractor g, Setting.Color c, float x, float y, float w, float alpha, float hover, int label, float base, float top, float bottom) {
        Fonts.TEXT.draw(g, c.name, x, base, SIZE, label);
        Draw d = new Draw(g);
        float[] chip = Draw.shard(x + w - 22, y + 3, 22, 10, 4);
        d.poly(chip, Colors.alpha(Theme.of(c), alpha));
        d.stroke(Draw.offset(chip, -.4f), .8f, Colors.with(0xFFFFFF, (.2f + hover * .5f) * alpha));
        d.flush();
        hit(x, y, w, 16, c, 0, null, top, bottom);
        float o = of(open, c).v;
        if (o <= 0) return;
        float sy = y + 18, sw = w - 14, sh = 36;
        d = new Draw(g);
        // Saturation across, brightness down.
        int hue = Colors.hsv(c.hue, 1, 1, alpha), white = Colors.with(0xFFFFFF, alpha), black = Colors.with(0, alpha);
        d.quad(x, sy, white, x, sy + sh, white, x + sw, sy + sh, hue, x + sw, sy, hue);
        d.gradient(x, sy, sw, sh, 0, black);
        int own = Colors.with(c.own(), alpha);
        float kx = x + c.sat * sw, ky = sy + (1 - c.val) * sh;
        d.poly(Draw.diamond(kx, ky, 4.4f), white);
        d.poly(Draw.diamond(kx, ky, 3.2f), own);
        for (int i = 0; i < 12; i++) d.gradient(x + sw + 5, sy + sh * i / 12, 9, sh / 12 + .1f, Colors.hsv(i / 12f, .85f, 1, alpha), Colors.hsv((i + 1) / 12f, .85f, 1, alpha));
        d.poly(Draw.diamond(x + sw + 9.5f, sy + c.hue * sh, 4.4f), white);
        d.poly(Draw.diamond(x + sw + 9.5f, sy + c.hue * sh, 3.2f), Colors.hsv(c.hue, .85f, 1, alpha));
        float ay = sy + sh + 7;
        d.gradientX(x, ay - 2, w, 4, Colors.with(0xFFFFFF, .08f * alpha), own);
        d.poly(Draw.diamond(x + c.alpha * w, ay, 4.4f), white);
        d.poly(Draw.diamond(x + c.alpha * w, ay, 3.2f), own);
        Style.check(d, x + 5, ay + 13, 4.4f, of(on, c).to(c.theme, .2f), 0, alpha);
        d.flush();
        Fonts.TEXT.draw(g, "Follow the theme", x + 14, Fonts.TEXT.centre(ay + 6, 14, SIZE), SIZE, Colors.alpha(Theme.dim(), alpha));
        Fonts.BOLD.right(g, String.format(Locale.ROOT, "#%06X", c.own() & 0xFFFFFF), x + w, Fonts.TEXT.centre(ay + 6, 14, SIZE), SIZE, Colors.alpha(Theme.dim(), alpha));
        if (o < .95f) return;
        hit(x, sy, sw, sh, c, 1, null, top, bottom);
        hit(x + sw + 3, sy, 13, sh, c, 2, null, top, bottom);
        hit(x, ay - 5, w, 10, c, 3, null, top, bottom);
        hit(x, ay + 6, w, 14, c, 4, null, top, bottom);
    }

    private static List<String> allIds(Setting.Ids.Kind kind) {
        var keys = switch (kind) {
            case BLOCK -> BuiltInRegistries.BLOCK.keySet();
            case ITEM -> BuiltInRegistries.ITEM.keySet();
            case ENTITY -> BuiltInRegistries.ENTITY_TYPE.keySet();
        };
        return keys.stream().map(Identifier::toString).filter(i -> !i.equals("minecraft:air")).sorted().toList();
    }

    private static ItemStack icon(Setting.Ids.Kind kind, String id) {
        Identifier k = Identifier.tryParse(id);
        if (k == null) return ItemStack.EMPTY;
        return switch (kind) {
            case BLOCK -> new ItemStack(BuiltInRegistries.BLOCK.getValue(k));
            case ITEM -> new ItemStack(BuiltInRegistries.ITEM.getValue(k));
            case ENTITY -> new ItemStack(BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(k.getNamespace(), k.getPath() + "_spawn_egg")).orElse(Items.AIR));
        };
    }

    private static String pretty(String id) { return id.substring(id.indexOf(':') + 1).replace('_', ' '); }

    private List<String> matching(Setting.Ids i) {
        String q = fields.containsKey(i) ? fields.get(i).text.toLowerCase(Locale.ROOT).strip() : "";
        List<String> out = new ArrayList<>(i.all().stream().filter(s -> pretty(s).contains(q)).toList());
        for (String s : allIds(i.kind)) if (!i.has(s) && pretty(s).contains(q)) out.add(s);
        return out;
    }

    private void ids(GuiGraphicsExtractor g, Setting.Ids i, float x, float y, float w, float alpha, boolean inside, int label, float base, float top, float bottom) {
        Fonts.TEXT.draw(g, i.name, x, base, SIZE, label);
        float o = of(open, i).v;
        chevron(g, x + w - 4, y + 8, o, alpha);
        Fonts.BOLD.right(g, i.all().size() + " picked", x + w - 12, base, SIZE, Colors.alpha(Theme.accent(), alpha));
        hit(x, y, w, 16, i, 0, null, top, bottom);
        if (o <= 0) return;
        TextField f = fields.computeIfAbsent(i, k -> new TextField("Search", 40, t -> pickScroll.put(i, 0f)));
        f.draw(g, x, y + 17, w, 14, SIZE, alpha * o);
        hit(x, y + 17, w, 14, i, 1, null, top, bottom);
        List<String> list = matching(i);
        float ly = y + 34, lh = PICK_ROW * PICK_ROWS, max = Math.max(0, list.size() * PICK_ROW - lh), sc = Math.clamp(pickScroll.getOrDefault(i, 0f), 0, max);
        pickScroll.put(i, sc);
        float ct = Math.max(top, ly), cb = Math.min(bottom, ly + lh);
        if (cb <= ct) return;
        g.enableScissor((int) x - 1, (int) ct, (int) Math.ceil(x + w) + 1, (int) cb);
        for (int n = (int) (sc / PICK_ROW); n < list.size() && n * PICK_ROW - sc < lh; n++) {
            float ry = ly + n * PICK_ROW - sc;
            String id = list.get(n);
            boolean picked = i.has(id), hot = inside && my >= ry && my < ry + PICK_ROW && my >= ct && my < cb;
            Draw d = new Draw(g);
            if (hot) d.rect(x, ry, w, PICK_ROW, Colors.with(0xFFFFFF, .06f * alpha));
            Style.check(d, x + w - 8, ry + PICK_ROW / 2, 3.6f, picked ? 1 : 0, hot ? 1 : 0, alpha);
            d.flush();
            ItemStack st = icon(i.kind, id);
            if (!st.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(x, ry + .5f);
                g.pose().scale(.75f);
                g.item(st, 0, 0);
                g.pose().popMatrix();
            }
            Fonts.TEXT.draw(g, Fonts.TEXT.clip(pretty(id), SIZE, w - 32), x + 16, Fonts.TEXT.centre(ry, PICK_ROW, SIZE), SIZE, Colors.alpha(picked ? Theme.text() : Theme.dim(), alpha));
            if (o > .95f) hit(x, ry, w, PICK_ROW, i, 2, id, ct, cb);
        }
        g.disableScissor();
        if (max > 0) {
            Draw d = new Draw(g);
            float bh = Math.max(8, lh * lh / (list.size() * PICK_ROW));
            d.rect(x + w + 1, ly + (lh - bh) * sc / max, 1.5f, bh, Colors.alpha(Theme.accent(), alpha * .8f));
            d.flush();
        }
    }

    public void unfocus() { fields.values().forEach(f -> f.focused = false); }

    public boolean typing() { return fields.values().stream().anyMatch(f -> f.focused); }

    public boolean click(float px, float py, int button) {
        unfocus();
        for (int n = hits.size() - 1; n >= 0; n--) {
            Hit h = hits.get(n);
            if (!h.has(px, py)) continue;
            if (button == InputConstants.MOUSE_BUTTON_MIDDLE) {
                h.s.reset();
                return true;
            }
            boolean left = button == InputConstants.MOUSE_BUTTON_LEFT;
            switch (h.s) {
                case Setting.Bool b -> b.set(!b.get());
                case Setting.Num v -> startDrag(h, px, py);
                case Setting.Text t -> fields.get(t).focused = true;
                case Setting.Choice c -> {
                    if (h.part > 0) c.pick(h.part - 1);
                    else if (left) expanded = expanded == c ? null : c;
                    else c.cycle(1);
                }
                case Setting.Color c -> {
                    if (h.part == 0) expanded = expanded == c ? null : c;
                    else if (h.part == 4) c.theme = !c.theme;
                    else startDrag(h, px, py);
                }
                case Setting.Ids i -> {
                    if (h.part == 0) expanded = expanded == i ? null : i;
                    else if (h.part == 1) fields.get(i).focused = true;
                    else i.toggle(h.id);
                }
            }
            return true;
        }
        return false;
    }

    private void startDrag(Hit h, float px, float py) {
        dragging = h.s;
        dragPart = h.part;
        dragHit = h;
        drag(px, py);
    }

    public boolean drag(float px, float py) {
        if (dragging == null) return false;
        float tx = Math.clamp((px - dragHit.x) / dragHit.w, 0, 1), ty = Math.clamp((py - dragHit.y) / dragHit.h, 0, 1);
        switch (dragging) {
            case Setting.Num n -> n.set(n.min + Math.clamp((px - dragHit.x - 4) / (dragHit.w - 8), 0, 1) * (n.max - n.min));
            case Setting.Color c -> {
                c.theme = false;
                if (dragPart == 1) {
                    c.sat = tx;
                    c.val = 1 - ty;
                } else if (dragPart == 2) c.hue = Math.min(ty, .999f);
                else c.alpha = tx;
            }
            default -> {}
        }
        return true;
    }

    public void release() { dragging = null; }

    /** True when a picker list took the scroll. */
    public boolean scroll(float px, float py, float by) {
        if (!(expanded instanceof Setting.Ids i)) return false;
        for (Hit h : hits) if (h.s == i && h.part == 2 && h.has(px, py)) {
            pickScroll.merge(i, -by * PICK_ROW * 2, Float::sum);
            return true;
        }
        return false;
    }

    public boolean type(String s) {
        for (TextField f : fields.values()) if (f.type(s)) return true;
        return false;
    }

    public boolean key(KeyEvent e) {
        for (TextField f : fields.values()) {
            if (f.focused && (e.key() == InputConstants.KEY_ESCAPE || e.key() == InputConstants.KEY_RETURN)) {
                f.focused = false;
                return true;
            }
            if (f.key(e)) return true;
        }
        return false;
    }
}
