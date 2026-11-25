package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.module.Obsidian;
import dev.hollow.ui.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Every HUD element. */
public final class HudElements {
    private HudElements() {}

    public static List<HudElement> all() {
        return List.of(
            new Watermark(), new ModuleList(), new Armor(), new Hands(), new Coordinates(), new Target(), new Counters(), new Potions(), new Inventory(), new Radar(), new Nearby(), new Keys(), new Compass(), new Pops(), new Lag(), new CustomText(), new Binds(), new Speedometer(),
            info("FPS", "FPS", "Frames per second.", true, 0, 0, 6, 34, () -> Integer.toString(mc().getFps())),
            info("Ping", "Ping", "Delay to the server.", true, 0, 0, 6, 50, () -> Stats.ping() + " ms"),
            info("TPS", "TPS", "How fast the server is running. 20 is full speed.", true, 0, 0, 6, 66, () -> f(Stats.tps(), 1)),
            info("Clock", "Time", "The time where you are.", false, 0, 0, 112, 34, () -> java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))),
            info("Server", "Server", "The server you are on.", false, 0, 0, 112, 50, () -> mc().getCurrentServer() == null ? "Singleplayer" : mc().getCurrentServer().ip),
            info("Session", "Session", "Time since you joined.", false, 0, 0, 6, 130, Stats::session),
            info("Biome", "Biome", "The biome you are in.", false, 0, 0, 112, 82, () -> mc().level.getBiome(mc().player.blockPosition()).unwrapKey().map(k -> k.identifier().getPath().replace('_', ' ')).orElse("")),
            info("World day", "Day", "Days passed in the world.", false, 0, 0, 112, 98, () -> Long.toString(mc().level.getOverworldClockTime() / 24000)),
            info("Facing", "Facing", "Direction and angle.", false, 0, 0, 112, 114, () -> mc().player.getDirection().getName() + " " + f(net.minecraft.util.Mth.wrapDegrees(mc().player.getYRot()), 0) + " / " + f(mc().player.getXRot(), 0)),
            info("CPS", "CPS", "Your clicks per second.", false, 0, 0, 112, 130, () -> Integer.toString(Stats.cps())),
            info("Combo", "Combo", "Hits landed without being hit.", false, 0, 0, 112, 146, () -> Integer.toString(Stats.combo())),
            info("Memory", "Memory", "Memory the game is using.", false, 0, 0, 112, 162, () -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100 / Runtime.getRuntime().maxMemory() + "%"),
            info("Saturation", "Saturation", "Hidden hunger reserve.", false, 0, 0, 112, 178, () -> f(mc().player.getFoodData().getSaturationLevel(), 1)),
            info("Light", "Light", "Block light at your feet. Monsters need 0.", false, 0, 0, 112, 194, () -> Integer.toString(mc().level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, mc().player.blockPosition()))),
            info("Hole", "Hole", "Whether you are standing in a safe hole.", false, 0, 0, 112, 210, () -> Holes.isHole(mc().player.blockPosition(), Obsidian::blastProof, Obsidian::open) ? "safe" : "open"),
            info("Players online", "Online", "Players on the server.", false, 0, 0, 112, 226, () -> Integer.toString(mc().getConnection().getOnlinePlayers().size())),
            info("Elytra", "Elytra", "Durability left on your elytra.", false, 0, 0, 112, 242, () -> {
                ItemStack s = mc().player.getItemBySlot(EquipmentSlot.CHEST);
                return s.is(Items.ELYTRA) ? s.getMaxDamage() - s.getDamageValue() + "" : "none";
            }),
            info("Welcome", "Hello", "Your name.", false, 1, 0, 0, 6, () -> mc().getUser().getName()));
    }

    private static InfoLine info(String n, String l, String d, boolean on, int ax, int ay, double ox, double oy, java.util.function.Supplier<String> v) { return new InfoLine(n, l, d, on, ax, ay, ox, oy, v); }

    private static net.minecraft.client.Minecraft mc() { return net.minecraft.client.Minecraft.getInstance(); }

    static String f(double v, int places) { return String.format(Locale.ROOT, "%." + places + "f", v); }

    static void item(GuiGraphicsExtractor g, ItemStack s, float x, float y, float size) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 16);
        g.item(s, 0, 0);
        g.pose().popMatrix();
    }

    static float left(ItemStack s) { return s.getMaxDamage() <= 0 ? 1 : 1 - s.getDamageValue() / (float) s.getMaxDamage(); }

    static final class Watermark extends HudElement {
        private final Setting.Bool version = bool("Version", true);
        private final Setting.Num tall = num("Letter height", 14, 8, 30, 1);

        Watermark() {
            super("Watermark", "The Hollow Client name.", true, 0, 0, 6, 6);
            plate.set(false);
        }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float t = tall.getF();
            w = Brand.line(g, 0, 0, t, 1, .55f + .1f * (float) Math.sin(Frame.time * .6), 1, .5f);
            h = t + 1;
            if (version.get()) {
                Fonts.BOLD.draw(g, Hollow.version(), w + 5, t, 8, Theme.text());
                w += 5 + Fonts.BOLD.width(Hollow.version(), 8);
            }
        }
    }

    static final class ModuleList extends HudElement {
        private final Setting.Choice sort = choice("Order", "Longest first", "Alphabetical"), colours = choice("Colours", "Flowing", "Theme", "White");
        private final Setting.Bool bar = bool("Side bar", true), details = bool("Details", true);
        private final Map<Module, Anim> in = new HashMap<>();

        ModuleList() {
            super("Module list", "Modules that are on, sliding in and out.", true, 2, 0, -5, 5);
            plate.set(false);
        }

        private String label(Module m) {
            String i = details.get() && m.on() && mc.player != null ? m.info() : "";
            return i.isEmpty() ? m.name : m.name + "  " + i;
        }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            List<Module> list = new ArrayList<>(Hollow.modules().stream().filter(m -> !m.hidden && m.category != dev.hollow.core.Category.CLIENT && m.category != dev.hollow.core.Category.HUD).toList());
            if (sort.is("Alphabetical")) list.sort(Comparator.comparing(m -> m.name));
            else list.sort(Comparator.comparingDouble(m -> -Fonts.BOLD.width(label(m), 9)));
            boolean right = rightSide();
            float y = 0, widest = 40;
            int n = 0;
            for (Module m : list) {
                float v = in.computeIfAbsent(m, k -> new Anim()).to(m.on(), .32f);
                if (v <= 0) continue;
                String s = label(m);
                float e = Ease.outQuint(v), tw = Fonts.BOLD.width(s, 9), rw = tw + 11, rh = 12.5f * Ease.inOut(v);
                widest = Math.max(widest, rw);
                int c = switch (colours.get()) {
                    case "Flowing" -> Theme.flow(n * .42f);
                    case "Theme" -> Theme.accent();
                    default -> 0xFFFFFFFF;
                };
                float x = right ? w - rw * e : -rw * (1 - e);
                Ice.glass(g, new float[] {x, y, x + rw, y, x + rw, y + rh, x, y + rh}, e, .6f);
                Draw d = new Draw(g);
                d.rect(x, y, rw, rh, Colors.with(Theme.ink(), .62f * e));
                d.gradientX(x, y, rw, rh, Colors.with(c, right ? .02f * e : .16f * e), Colors.with(c, right ? .16f * e : .02f * e));
                if (bar.get()) d.poly(Draw.round(right ? x + rw - 2.2f : x + .4f, y + 1.6f, 1.8f, Math.max(0, rh - 3.2f), .9f), Colors.alpha(c, e));
                d.flush();
                float base = Fonts.BOLD.centre(y, 12.5f, 9), tx = right ? x + 4.5f : x + 6;
                Fonts.BOLD.draw(g, m.name, tx, base, 9, Colors.alpha(c, e));
                if (s.length() > m.name.length()) Fonts.BOLD.draw(g, s.substring(m.name.length() + 2), tx + Fonts.BOLD.width(m.name + "  ", 9), base, 9, Colors.alpha(0xFFB9C6C9, e));
                y += rh;
                n++;
            }
            w = widest;
            h = Math.max(12.5f, y);
            if (editing && n == 0) text(g, "Module list", 4, Fonts.BOLD.centre(0, h, 9), Theme.dim());
        }
    }

    static final class Armor extends HudElement {
        private final Setting.Choice layout = choice("Layout", "Across", "Down"), show = choice("Durability", "Percent", "Points left", "Bar only", "Nothing");
        private final Setting.Bool warn = bool("Flash when low", true);
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        Armor() { super("Armor", "Worn armor with durability.", true, 1, 2, 152, -3); }

        @Override protected boolean empty() {
            for (EquipmentSlot s : SLOTS) if (!mc.player.getItemBySlot(s).isEmpty()) return false;
            return true;
        }

        private static final ItemStack[] SAMPLE = {null, null, null, null};

        private ItemStack stack(int i, boolean editing) {
            ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getItemBySlot(SLOTS[i]);
            if (!s.isEmpty() || !editing) return s;
            if (SAMPLE[i] == null) SAMPLE[i] = new ItemStack(List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS).get(i));
            return SAMPLE[i];
        }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            boolean across = layout.is("Across"), words = show.is("Percent") || show.is("Points left");
            float cell = across ? 26 : 19, x = 4, y = 3;
            int n = 0;
            for (int i = 0; i < 4; i++) {
                ItemStack s = stack(i, editing);
                if (s.isEmpty()) continue;
                float t = left(s), alpha = warn.get() && t < .15f ? .45f + .55f * Math.abs((float) Math.sin(Frame.time * 5)) : 1;
                int c = Colors.alpha(Colors.health(t), alpha);
                String v = show.is("Percent") ? Math.round(t * 100) + "%" : Integer.toString(s.getMaxDamage() - s.getDamageValue());
                if (across) {
                    item(g, s, x + 3, y, 16);
                    if (words) Fonts.BOLD.middle(g, v, x + 11, y + 24, 8, c);
                    if (!show.is("Nothing")) bar(g, x + 2, y + (words ? 26.5f : 18), 18, t, c);
                    x += cell;
                } else {
                    item(g, s, x, y, 16);
                    if (words) Fonts.BOLD.draw(g, v, x + 20, Fonts.BOLD.centre(y, 14, 9), 9, c);
                    if (!show.is("Nothing")) bar(g, x + 20, y + 14, 26, t, c);
                    y += cell;
                }
                n++;
            }
            n = Math.max(1, n);
            w = across ? n * cell + 4 : show.is("Nothing") ? 24 : 56;
            h = across ? (words ? 33 : show.is("Nothing") ? 22 : 24) : n * cell + 3;
        }

        private static void bar(GuiGraphicsExtractor g, float x, float y, float bw, float t, int c) {
            Draw d = new Draw(g);
            d.rect(x, y, bw, 2, 0x60000000);
            d.rect(x, y, bw * t, 2, c);
            d.flush();
        }
    }

    static final class Hands extends HudElement {
        Hands() { super("Held items", "What you hold, with count and durability.", false, 1, 2, -150, -4); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float y = 3;
            w = 60;
            for (ItemStack s : mc.player == null ? List.of(ItemStack.EMPTY) : List.of(mc.player.getMainHandItem(), mc.player.getOffhandItem())) {
                if (s.isEmpty() && !editing) continue;
                item(g, s.isEmpty() ? new ItemStack(Items.DIAMOND_PICKAXE) : s, 4, y, 16);
                String v = s.getMaxDamage() > 0 ? Integer.toString(s.getMaxDamage() - s.getDamageValue()) : "x" + Math.max(1, Inv.count(s.getItem()));
                Fonts.BOLD.draw(g, v, 24, Fonts.BOLD.centre(y, 16, 9), 9, s.getMaxDamage() > 0 ? Colors.health(left(s)) : Theme.text());
                y += 18;
            }
            h = Math.max(20, y + 1);
        }

        @Override protected boolean empty() { return mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty(); }
    }

    static final class Coordinates extends HudElement {
        private final Setting.Bool other = bool("Other dimension", true), direction = bool("Direction", true), hide = bool("Hide numbers", false);
        private final Setting.Num places = num("Decimals", 1, 0, 2, 1);

        Coordinates() { super("Coordinates", "Where you are, and the matching place in the Nether or Overworld.", true, 0, 2, 4, -3); }

        private String num(double v) { return hide.get() ? "?" : f(v, places.getInt()); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            double x = mc.player == null ? 0 : mc.player.getX(), y = mc.player == null ? 64 : mc.player.getY(), z = mc.player == null ? 0 : mc.player.getZ();
            boolean nether = mc.level != null && mc.level.dimension() == Level.NETHER, end = mc.level != null && mc.level.dimension() == Level.END;
            float cx = 5, base = Fonts.BOLD.centre(0, 14, 9);
            String[] l = {"X", "Y", "Z"};
            double[] v = {x, y, z};
            for (int i = 0; i < 3; i++) {
                text(g, l[i], cx, base, Theme.accent());
                cx += Fonts.BOLD.width(l[i], 9) + 3;
                text(g, num(v[i]), cx, base, Theme.text());
                cx += Fonts.BOLD.width(num(v[i]), 9) + 7;
            }
            w = cx;
            h = 14;
            String second = "";
            if (other.get() && !end) {
                double k = nether ? 8 : .125;
                second = (nether ? "Overworld " : "Nether ") + num(x * k) + "  " + num(z * k);
            }
            if (direction.get() && mc.player != null) {
                String dir = switch (mc.player.getDirection()) {
                    case NORTH -> "North -Z";
                    case SOUTH -> "South +Z";
                    case EAST -> "East +X";
                    default -> "West -X";
                };
                second = second.isEmpty() ? dir : dir + "   " + second;
            }
            if (!second.isEmpty()) {
                text(g, second, 5, Fonts.BOLD.centre(12, 13, 9), Theme.dim());
                h = 26;
                w = Math.max(w, Fonts.BOLD.width(second, 9) + 10);
            }
        }
    }

    static final class Target extends HudElement {
        private final Setting.Num range = num("Range", 14, 4, 40, 1);
        private final Setting.Bool mobs = bool("Mobs", false);
        private LivingEntity last;
        private final Anim hp = new Anim(), lag = new Anim();

        Target() { super("Target", "Health and gear of whoever you are fighting.", true, 1, 1, 96, 46); }

        private LivingEntity find() {
            var near = Targets.near(range.get(), true, mobs.get());
            return near.isEmpty() ? null : near.getFirst();
        }

        @Override protected boolean empty() {
            LivingEntity e = find();
            if (e != null) last = e;
            return e == null;
        }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            LivingEntity e = editing && last == null ? mc.player : last;
            w = 132;
            h = 42;
            if (e == null) return;
            float max = Math.max(e.getMaxHealth(), 1), now = Damage.health(e), t = Math.clamp(now / max, 0, 1);
            hp.glide(t, 14);
            lag.glide(t, 3.2f);
            if (e instanceof AbstractClientPlayer p) PlayerFaceExtractor.extractRenderState(g, p.getSkin(), 6, 6, 24);
            else Fonts.DISPLAY.middle(g, e.getName().getString().substring(0, 1), 18, 24, 16, Theme.accent());
            Fonts.BOLD.draw(g, Fonts.BOLD.clip(e.getName().getString(), 10.5f, 62), 36, 15, 10.5f, Theme.text());
            Fonts.BOLD.right(g, f(now, 1), w - 7, 15, 10.5f, Colors.health(t));
            Fonts.TEXT.draw(g, f(e.distanceTo(mc.player), 1) + " blocks" + (Stats.pops(e) > 0 ? ", " + Stats.pops(e) + " pops" : ""), 36, 26, 8.5f, Theme.dim());
            float x = 97;
            for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = e.getItemBySlot(s);
                if (!st.isEmpty()) item(g, st, x, 18, 8);
                x += 8;
            }
            Draw d = new Draw(g);
            float[] shell = Draw.round(6, 33, w - 12, 5, 2.5f);
            d.poly(shell, 0x70000000);
            d.poly(Draw.clip(shell, -1, 0, -(6 + (w - 12) * lag.v)), Colors.with(0xFFFFFF, .35f));
            float[] full = Draw.clip(shell, -1, 0, -(6 + (w - 12) * hp.v));
            d.poly(full, Theme.fill(6, w - 6, 1));
            d.flush();
        }
    }

    static final class Counters extends HudElement {
        private final Setting.Ids items = ids("Items", Setting.Ids.Kind.ITEM, "totem_of_undying", "end_crystal", "enchanted_golden_apple", "experience_bottle", "obsidian");
        private final Setting.Choice layout = choice("Layout", "Across", "Down");
        private final Setting.Bool zero = bool("Show when none", true);

        Counters() { super("Item counts", "How many of the items you pick you are carrying.", true, 2, 2, -6, -42); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float x = 4, y = 3, across = 0;
            boolean row = layout.is("Across");
            for (String id : items.all()) {
                Identifier k = Identifier.tryParse(id);
                if (k == null) continue;
                var it = BuiltInRegistries.ITEM.getValue(k);
                int n = mc.player == null ? 0 : Inv.count(it);
                if (n == 0 && !zero.get()) continue;
                String s = Integer.toString(n);
                item(g, new ItemStack(it), x, y, 13);
                float tw = Fonts.BOLD.width(s, 9);
                Fonts.BOLD.draw(g, s, x + 15, Fonts.BOLD.centre(y, 13, 9), 9, n == 0 ? Theme.dim() : Theme.text());
                if (row) x += 15 + tw + 7;
                else y += 15;
                across = Math.max(across, 15 + tw + 8);
            }
            w = Math.max(20, row ? x : across + 4);
            h = row ? 19 : Math.max(19, y + 2);
        }
    }

    static final class Potions extends HudElement {
        Potions() { super("Effects", "Active effects and the time left on them.", true, 2, 2, -6, -66); }

        @Override protected boolean empty() { return mc.player.getActiveEffects().isEmpty(); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float y = 3, wide = 60;
            if (mc.player != null) for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String n = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
                int s = e.getDuration() / 20;
                String t = e.isInfiniteDuration() ? "∞" : s / 60 + ":" + String.format("%02d", s % 60);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(e.getEffect()), 4, (int) y, 14, 14);
                Fonts.BOLD.draw(g, n, 21, Fonts.BOLD.centre(y, 14, 9), 9, 0xFF000000 | e.getEffect().value().getColor());
                float nw = Fonts.BOLD.width(n, 9) + 6;
                Fonts.BOLD.draw(g, t, 21 + nw, Fonts.BOLD.centre(y, 14, 9), 9, s < 10 && !e.isInfiniteDuration() ? 0xFFFF9A8A : Theme.text());
                wide = Math.max(wide, 21 + nw + Fonts.BOLD.width(t, 9) + 6);
                y += 16;
            }
            if (y == 3) {
                if (editing) text(g, "Effects", 6, Fonts.BOLD.centre(0, 18, 9), Theme.dim());
                y = 15;
            }
            w = wide;
            h = y + 2;
        }
    }

    static final class Inventory extends HudElement {
        Inventory() { super("Inventory", "Your inventory at a glance.", false, 2, 2, -5, -6); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            w = 9 * 17 + 7;
            h = 3 * 17 + 7;
            if (mc.player == null) return;
            for (int i = 0; i < 27; i++) {
                ItemStack s = mc.player.getInventory().getItem(9 + i);
                int x = 4 + i % 9 * 17, y = 4 + i / 9 * 17;
                if (s.isEmpty()) continue;
                g.item(s, x, y);
                g.itemDecorations(mc.font, s, x, y);
            }
        }
    }

    static final class Radar extends HudElement {
        private final Setting.Num range = num("Range", 48, 16, 128, 8), side = num("Width", 76, 40, 160, 4);
        private final Setting.Bool players = bool("Players", true), hostile = bool("Monsters", true), passive = bool("Animals", false), turn = bool("Turn with me", true);

        Radar() { super("Radar", "A small map of who is around.", false, 0, 1, 6, 0); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            w = h = side.getF();
            Draw d = new Draw(g);
            float c = w / 2;
            d.line(c, 4, c, h - 4, .7f, Colors.with(0xFFFFFF, .12f));
            d.line(4, c, w - 4, c, .7f, Colors.with(0xFFFFFF, .12f));
            d.stroke(Draw.diamond(c, c, w * .25f), .7f, Colors.with(0xFFFFFF, .10f));
            if (mc.level != null && mc.player != null) {
                double yaw = turn.get() ? Math.toRadians(mc.player.getYRot() + 180) : 0, cos = Math.cos(-yaw), sin = Math.sin(-yaw);
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (e == mc.player || !(e instanceof LivingEntity)) continue;
                    int col;
                    if (e instanceof Player p) {
                        if (!players.get()) continue;
                        col = Hollow.FRIENDS.has(p.getScoreboardName()) ? 0xFF7CE7FF : Theme.accent();
                    } else if (e instanceof Enemy) {
                        if (!hostile.get()) continue;
                        col = 0xFFFF7A6B;
                    } else {
                        if (!passive.get()) continue;
                        col = 0xFFB9C6C9;
                    }
                    double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ(), k = (c - 5) / range.get();
                    float px = (float) (c + (dx * cos - dz * sin) * k), py = (float) (c + (dx * sin + dz * cos) * k);
                    if (px < 3 || py < 3 || px > w - 3 || py > h - 3) continue;
                    d.poly(Draw.diamond(px, py, e instanceof Player ? 2.8f : 2), col);
                }
            }
            d.poly(new float[] {c, c - 4, c + 3, c + 3, c, c + 1.6f, c - 3, c + 3}, 0xFFFFFFFF);
            d.flush();
        }
    }

    static final class Nearby extends HudElement {
        private final Setting.Num most = num("Most shown", 6, 1, 15, 1);

        Nearby() { super("Nearby players", "Players you can see, with distance and health.", false, 0, 1, 6, -70); }

        private List<AbstractClientPlayer> list() {
            return mc.level == null ? List.of() : mc.level.players().stream().filter(p -> p != mc.player).sorted(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player))).limit(most.getInt()).toList();
        }

        @Override protected boolean empty() { return list().isEmpty(); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float y = 2, wide = 70;
            for (AbstractClientPlayer p : list()) {
                String n = p.getScoreboardName(), dist = f(p.distanceTo(mc.player), 0) + "m", hp = f(Damage.health(p), 0);
                float x = 5, base = Fonts.BOLD.centre(y, 12, 9);
                text(g, hp, x, base, Colors.health(Damage.health(p) / 20));
                x += 18;
                text(g, n, x, base, Hollow.FRIENDS.has(n) ? 0xFF7CE7FF : Theme.text());
                x += Fonts.BOLD.width(n, 9) + 6;
                text(g, dist, x, base, Theme.dim());
                wide = Math.max(wide, x + Fonts.BOLD.width(dist, 9) + 5);
                y += 12;
            }
            if (y == 2) {
                if (editing) text(g, "Nearby players", 5, Fonts.BOLD.centre(2, 12, 9), Theme.dim());
                y = 14;
            }
            w = wide;
            h = y + 2;
        }
    }

    static final class Keys extends HudElement {
        private final Map<Object, Anim> down = new HashMap<>();

        Keys() {
            super("Keystrokes", "The movement keys and clicks you are pressing.", false, 0, 2, 6, -70);
            plate.set(false);
        }

        private void key(GuiGraphicsExtractor g, KeyMapping k, String label, float x, float y, float kw, float kh) {
            float v = down.computeIfAbsent(k, n -> new Anim()).to(k.isDown(), .12f);
            Style.plate(g, x, y, kw, kh, 1);
            Draw d = new Draw(g);
            d.sweep(Draw.shard(x, y, kw, kh, 4), Ease.outQuint(v), Theme.fill(0, 62, .75f));
            d.flush();
            Fonts.BOLD.middle(g, label, x + kw / 2, Fonts.BOLD.centre(y, kh, 9), 9, 0xFFFFFFFF);
        }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            var o = mc.options;
            key(g, o.keyUp, "W", 21, 0, 19, 19);
            key(g, o.keyLeft, "A", 0, 21, 19, 19);
            key(g, o.keyDown, "S", 21, 21, 19, 19);
            key(g, o.keyRight, "D", 42, 21, 19, 19);
            key(g, o.keyAttack, "Left", 0, 42, 29.5f, 15);
            key(g, o.keyUse, "Right", 31.5f, 42, 29.5f, 15);
            key(g, o.keyJump, "Jump", 0, 59, 61, 12);
            w = 61;
            h = 71;
        }
    }

    static final class Compass extends HudElement {
        Compass() { super("Compass", "A strip showing which way you face.", false, 1, 0, 0, 24); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            w = 170;
            h = 20;
            float yaw = mc.player == null ? 0 : net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()) + 180;
            String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
            Draw d = new Draw(g);
            for (int deg = 0; deg < 360; deg += 15) {
                float diff = net.minecraft.util.Mth.wrapDegrees(deg - yaw), x = w / 2 + diff * 1.15f, fade = Ease.clamp(1 - Math.abs(diff) / 72);
                if (fade <= 0) continue;
                if (deg % 45 != 0) d.rect(x - .4f, 8, .8f, 4, Colors.with(0xFFFFFF, .5f * fade));
            }
            d.poly(new float[] {w / 2 - 3, 0, w / 2 + 3, 0, w / 2, 4}, Theme.accent());
            d.flush();
            for (int i = 0; i < 8; i++) {
                float diff = net.minecraft.util.Mth.wrapDegrees(i * 45 - yaw), fade = Ease.clamp(1 - Math.abs(diff) / 72);
                if (fade > 0) Fonts.BOLD.middle(g, names[i], w / 2 + diff * 1.15f, 14.5f, i % 2 == 0 ? 10.5f : 8.5f, Colors.alpha(i == 0 ? Theme.accent() : Theme.text(), fade));
            }
        }
    }

    static final class Pops extends HudElement {
        Pops() { super("Totem pops", "Totems the players around you have used up.", false, 0, 1, 6, 60); }

        @Override protected boolean empty() { return Stats.popList().isEmpty(); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float y = 2, wide = 60;
            for (var e : Stats.popList()) {
                String n = e.getKey(), c = e.getValue() + "";
                text(g, n, 5, Fonts.BOLD.centre(y, 12, 9), Theme.text());
                float nw = Fonts.BOLD.width(n, 9);
                text(g, c, 11 + nw, Fonts.BOLD.centre(y, 12, 9), Theme.accent());
                wide = Math.max(wide, 16 + nw + Fonts.BOLD.width(c, 9));
                y += 12;
            }
            if (y == 2) {
                if (editing) text(g, "Totem pops", 5, Fonts.BOLD.centre(2, 12, 9), Theme.dim());
                y = 14;
            }
            w = wide;
            h = y + 2;
        }
    }

    static final class Lag extends HudElement {
        Lag() { super("Lag warning", "Shows when the server stops answering.", true, 1, 0, 0, 46); }

        @Override protected boolean empty() { return mc.hasSingleplayerServer() || Stats.silence() < 1500; }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            String s = "Server not responding for " + f((editing ? 2000 : Stats.silence()) / 1000.0, 1) + " s";
            w = Fonts.BOLD.width(s, 9.5f) + 14;
            h = 16;
            Fonts.BOLD.draw(g, s, 7, Fonts.BOLD.centre(0, h, 9.5f), 9.5f, 0xFFFF9A7A);
        }
    }

    static final class CustomText extends HudElement {
        private final Setting.Text line = text("Text", "Hollow Client");
        private final Setting.Choice font = choice("Font", "Display", "Bold");
        private final Setting.Color colour = color("Colour", 0xFFFFFFFF);

        CustomText() { super("Your text", "Any words you like.", false, 1, 0, 0, 8); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            Fonts f = font.is("Display") ? Fonts.DISPLAY : Fonts.BOLD;
            w = f.width(line.get(), 10) + 10;
            h = 16;
            f.draw(g, line.get(), 5, f.centre(0, h, 10), 10, Theme.of(colour));
        }
    }

    static final class Binds extends HudElement {
        Binds() { super("Key list", "Modules with a key, and whether they are on.", false, 2, 1, -5, -60); }

        @Override protected boolean empty() { return Hollow.modules().stream().noneMatch(m -> m.key != 0); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            float y = 3;
            w = 90;
            for (Module m : Hollow.modules()) {
                if (m.key == 0) continue;
                float base = Fonts.BOLD.centre(y, 12, 9);
                text(g, m.name, 5, base, m.on() ? Theme.accent() : Theme.dim());
                String k = com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(m.key).getDisplayName().getString();
                Fonts.BOLD.right(g, k, w - 5, base, 9, Theme.text());
                y += 12;
            }
            if (y == 3) {
                if (editing) text(g, "Key list", 5, Fonts.BOLD.centre(2, 12, 9), Theme.dim());
                y = 14;
            }
            h = y + 3;
        }
    }

    static final class Speedometer extends HudElement {
        private final Setting.Choice unit = choice("Unit", "km/h", "Blocks per second");
        private final Anim shown = new Anim();
        private double lx, lz;
        private int tick = -1;
        private float speed;

        Speedometer() { super("Speed", "How fast you are moving.", true, 0, 0, 6, 82); }

        @Override protected void paint(GuiGraphicsExtractor g, boolean editing) {
            if (mc.player != null && mc.player.tickCount != tick) {
                if (tick >= 0) speed = (float) Math.hypot(mc.player.getX() - lx, mc.player.getZ() - lz) * 20 / Math.max(1, mc.player.tickCount - tick);
                lx = mc.player.getX();
                lz = mc.player.getZ();
                tick = mc.player.tickCount;
            }
            shown.glide(speed, 9);
            boolean k = unit.is("km/h");
            String v = f(shown.v * (k ? 3.6 : 1), 1) + (k ? " km/h" : " b/s");
            text(g, "Speed", 5, Fonts.BOLD.centre(0, 14, 9), Theme.accent());
            float lw = Fonts.BOLD.width("Speed", 9) + 5;
            text(g, v, 5 + lw, Fonts.BOLD.centre(0, 14, 9), Theme.text());
            w = lw + Fonts.BOLD.width(v, 9) + 10;
            h = 14;
        }
    }
}
