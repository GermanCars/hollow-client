package dev.hollow.module;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.Stats;
import dev.hollow.ui.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Things drawn in or over the world. */
public final class RenderModules {
    private static final Minecraft mc = Minecraft.getInstance();

    private RenderModules() {}

    static int opaque(int c) { return 0xFF000000 | c & 0xFFFFFF; }

    static void box(AABB b, int c, float line) { Gizmos.cuboid(b, GizmoStyle.strokeAndFill(opaque(c), line, Colors.with(c, .16f))).setAlwaysOnTop(); }

    /** Every block entity the client knows of. */
    static Map<BlockPos, BlockEntity> blockEntities() {
        Map<BlockPos, BlockEntity> out = new HashMap<>();
        int r = mc.options.getEffectiveRenderDistance(), cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
            LevelChunk c = mc.level.getChunkSource().getChunkNow(x, z);
            if (c != null) out.putAll(c.getBlockEntities());
        }
        return out;
    }

    static int entityColor(Entity e) {
        if (e instanceof Player p) return Hollow.FRIENDS.has(p.getScoreboardName()) ? 0xFF7CE7FF : Theme.accent();
        return e instanceof Enemy ? 0xFFFF7A6B : e instanceof LivingEntity ? 0xFFB9E08A : 0xFFFFE08A;
    }

    public static final class Tracers extends Module {
        private final Setting.Bool players = bool("Players", true), mobs = bool("Monsters", false), items = bool("Items", false);
        private final Setting.Choice from = choice("From", "Crosshair", "Bottom");
        private final Setting.Num width = num("Width", 1, .5, 4, .25), alpha = num("Opacity", .75, .1, 1, .05);

        public Tracers() { super(Category.RENDER, "Tracers", "Draws lines to players and more."); }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            Draw d = new Draw(g);
            float x = g.guiWidth() / 2f, y = from.is("Crosshair") ? g.guiHeight() / 2f : g.guiHeight();
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof Player ? players.get() : e instanceof Enemy ? mobs.get() : e instanceof ItemEntity && items.get())) continue;
                float[] s = Project.at(e.getPosition(partial).add(0, e.getBbHeight() / 2, 0));
                if (s != null) d.line(x, y, s[0], s[1], width.getF(), Colors.with(entityColor(e), alpha.getF()));
            }
            d.flush();
        }
    }

    public static final class Nametags extends Module {
        private final Setting.Num size = num("Size", 1, .5, 2, .1);
        private final Setting.Bool gear = bool("Gear", true), ping = bool("Ping", true), distance = bool("Distance", true), pops = bool("Totem pops", true), self = bool("Myself", false);

        public Nametags() { super(Category.RENDER, "Nametags", "Bigger name tags with health, gear and ping."); }

        public boolean replaces(Entity e) { return on() && e instanceof Player; }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            List<AbstractClientPlayer> list = new ArrayList<>(mc.level.players());
            list.sort(java.util.Comparator.comparingDouble(p -> -p.distanceToSqr(mc.player)));
            for (AbstractClientPlayer p : list) {
                if (p == mc.player && (!self.get() || mc.options.getCameraType().isFirstPerson())) continue;
                float[] s = Project.at(p.getPosition(partial).add(0, p.getBbHeight() + .35, 0));
                if (s == null) continue;
                double dist = mc.gameRenderer.mainCamera().position().distanceTo(p.position());
                float k = size.getF() * (float) Math.clamp(1.15 - dist / 160, .7, 1.1);
                g.nextStratum();
                g.pose().pushMatrix();
                g.pose().translate(s[0], s[1]);
                g.pose().scale(k);
                String name = p.getScoreboardName(), hp = String.format(java.util.Locale.ROOT, "%.0f", Damage.health(p));
                PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
                String extra = (ping.get() && info != null ? "  " + info.getLatency() + "ms" : "") + (distance.get() ? "  " + Math.round(dist) + "m" : "") + (pops.get() && Stats.pops(p) > 0 ? "  -" + Stats.pops(p) : "");
                float nw = Fonts.BOLD.width(name, 10), hw = Fonts.BOLD.width(hp, 10), ew = Fonts.TEXT.width(extra, 8.5f), w = nw + hw + ew + 18, x = -w / 2;
                Draw d = new Draw(g);
                float[] shape = Draw.shard(x, -16, w, 16, 5);
                d.poly(shape, Colors.with(Theme.ink(), .82f));
                float t = Ease.clamp(Damage.health(p) / Math.max(1, p.getMaxHealth()));
                d.rect(x + 5, -1.6f, (w - 10) * t, 1.6f, Colors.health(t));
                d.flush();
                float base = Fonts.BOLD.centre(-16, 15, 10);
                Fonts.BOLD.draw(g, name, x + 6, base, 10, Hollow.FRIENDS.has(name) ? 0xFF7CE7FF : Theme.text());
                Fonts.BOLD.draw(g, hp, x + 11 + nw, base, 10, Colors.health(t));
                Fonts.TEXT.draw(g, extra, x + 12 + nw + hw, base, 8.5f, Theme.dim());
                if (gear.get()) {
                    List<ItemStack> st = new ArrayList<>(List.of(p.getMainHandItem()));
                    for (EquipmentSlot e : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) st.add(p.getItemBySlot(e));
                    st.add(p.getOffhandItem());
                    st.removeIf(ItemStack::isEmpty);
                    for (int i = 0; i < st.size(); i++) {
                        int ix = i * 17 - st.size() * 17 / 2;
                        g.item(st.get(i), ix, -34);
                        g.itemDecorations(mc.font, st.get(i), ix, -34);
                    }
                }
                g.pose().popMatrix();
            }
        }
    }

    public static final class StorageEsp extends Module {
        private final Setting.Bool chests = bool("Chests", true), ender = bool("Ender chests", true), shulkers = bool("Shulker boxes", true), other = bool("Barrels, hoppers and furnaces", true), spawners = bool("Spawners", true), tracers = bool("Tracers", false);
        private Map<BlockPos, BlockEntity> found = Map.of();
        private int wait;

        public StorageEsp() { super(Category.RENDER, "StorageESP", "Shows chests, shulkers and other storage through walls."); }

        static boolean storage(BlockEntity e) { return e instanceof ChestBlockEntity || e instanceof ShulkerBoxBlockEntity || e instanceof BarrelBlockEntity || e instanceof EnderChestBlockEntity; }

        private int color(BlockEntity e) {
            if (e instanceof ChestBlockEntity) return chests.get() ? 0xFFFFC266 : 0;
            if (e instanceof EnderChestBlockEntity) return ender.get() ? 0xFFB58CFF : 0;
            if (e instanceof ShulkerBoxBlockEntity) return shulkers.get() ? 0xFFFF8FDD : 0;
            if (e instanceof SpawnerBlockEntity || e instanceof TrialSpawnerBlockEntity) return spawners.get() ? 0xFF7FD0FF : 0;
            return other.get() && (e instanceof BarrelBlockEntity || e instanceof HopperBlockEntity || e instanceof AbstractFurnaceBlockEntity || e instanceof DispenserBlockEntity) ? 0xFFB9C6C9 : 0;
        }

        @Override public void onTick() {
            if (wait-- > 0) return;
            wait = 10;
            found = blockEntities();
            found.values().removeIf(e -> color(e) == 0);
        }

        @Override public void onWorldRender(float partial) {
            found.forEach((p, e) -> {
                VoxelShape s = mc.level.getBlockState(p).getShape(mc.level, p);
                box(s.isEmpty() ? new AABB(p) : s.bounds().move(p), color(e), 1.5f);
            });
        }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            if (!tracers.get()) return;
            Draw d = new Draw(g);
            found.forEach((p, e) -> {
                float[] s = Project.at(Vec3.atCenterOf(p));
                if (s != null) d.line(g.guiWidth() / 2f, g.guiHeight() / 2f, s[0], s[1], .8f, Colors.with(color(e), .6f));
            });
            d.flush();
        }

        @Override public void onWorldLeft() { found = Map.of(); }

        @Override public String info() { return Integer.toString(found.size()); }
    }

    public static final class BlockEsp extends Module {
        private final Setting.Ids blocks = ids("Blocks", Setting.Ids.Kind.BLOCK, "ancient_debris", "diamond_ore", "deepslate_diamond_ore", "nether_portal", "end_portal_frame");
        private final Setting.Num range = num("Chunks", 5, 1, 12, 1), most = num("Most shown", 400, 50, 2000, 50);
        private final Setting.Color colour = color("Colour", 0xFF8CF0CB);
        private final Setting.Bool ownColour = bool("Use each block's map colour", true), tracers = bool("Tracers", false);
        private final Map<Long, List<BlockPos>> found = new ConcurrentHashMap<>();
        private int cursor, version;

        public BlockEsp() { super(Category.RENDER, "BlockESP", "Finds the blocks you pick and shows them through walls."); }

        @Override public void onTick() {
            if (version != blocks.version) {
                version = blocks.version;
                found.clear();
                cursor = 0;
            }
            int r = range.getInt(), side = r * 2 + 1, cx = mc.player.getBlockX() >> 4, cz = mc.player.getBlockZ() >> 4;
            // A few columns a tick, round and round.
            for (int n = 0; n < 6; n++) {
                int i = cursor++ % (side * side);
                LevelChunk c = mc.level.getChunkSource().getChunkNow(cx - r + i % side, cz - r + i / side);
                if (c != null) scan(c);
            }
            if (cursor % 60 == 0) found.keySet().removeIf(k -> Math.abs(SectionPos.x(k) - cx) > r || Math.abs(SectionPos.z(k) - cz) > r);
        }

        private void scan(LevelChunk c) {
            LevelChunkSection[] sections = c.getSections();
            for (int i = 0; i < sections.length; i++) {
                long key = SectionPos.asLong(c.getPos().x(), c.getSectionYFromSectionIndex(i), c.getPos().z());
                LevelChunkSection s = sections[i];
                if (s.hasOnlyAir() || !s.getStates().maybeHas(b -> blocks.has(WorldModules.id(b)))) {
                    found.remove(key);
                    continue;
                }
                List<BlockPos> list = new ArrayList<>();
                int bx = c.getPos().getMinBlockX(), by = SectionPos.sectionToBlockCoord(c.getSectionYFromSectionIndex(i)), bz = c.getPos().getMinBlockZ();
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) if (blocks.has(WorldModules.id(s.getBlockState(x, y, z)))) list.add(new BlockPos(bx + x, by + y, bz + z));
                if (list.isEmpty()) found.remove(key);
                else found.put(key, list);
            }
        }

        private int color(BlockPos p) {
            if (!ownColour.get()) return Theme.of(colour);
            int c = mc.level.getBlockState(p).getMapColor(mc.level, p).col;
            if (c == 0) return Theme.of(colour);
            // Dark map colours would vanish against the block.
            float[] h = Colors.toHsv(opaque(c));
            return Colors.hsv(h[0], Math.min(h[1], .75f), Math.max(h[2], .85f), 1);
        }

        @Override public void onWorldRender(float partial) {
            int n = 0;
            for (List<BlockPos> l : found.values()) for (BlockPos p : l) {
                if (n++ >= most.getInt()) return;
                box(new AABB(p), color(p), 1.5f);
            }
        }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            if (!tracers.get()) return;
            Draw d = new Draw(g);
            int n = 0;
            for (List<BlockPos> l : found.values()) for (BlockPos p : l) {
                float[] s = n++ < 200 ? Project.at(Vec3.atCenterOf(p)) : null;
                if (s != null) d.line(g.guiWidth() / 2f, g.guiHeight() / 2f, s[0], s[1], .8f, Colors.with(color(p), .55f));
            }
            d.flush();
        }

        @Override protected void onDisable() { found.clear(); }

        @Override public void onWorldLeft() { found.clear(); }

        @Override public String info() { return Integer.toString(found.values().stream().mapToInt(List::size).sum()); }
    }

    public static final class ItemEsp extends Module {
        private final Setting.Bool names = bool("Names", true), boxes = bool("Boxes", true);

        public ItemEsp() { super(Category.RENDER, "ItemESP", "Shows dropped items and what they are."); }

        @Override public void onWorldRender(float partial) {
            if (boxes.get()) for (Entity e : mc.level.entitiesForRendering()) if (e instanceof ItemEntity) box(e.getInterpolatedBoundingBox(partial), 0xFFFFE08A, 1.5f);
        }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            if (!names.get()) return;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof ItemEntity i)) continue;
                float[] s = Project.at(e.getPosition(partial).add(0, .6, 0));
                if (s == null) continue;
                String n = i.getItem().getHoverName().getString() + (i.getItem().getCount() > 1 ? " x" + i.getItem().getCount() : "");
                Fonts.BOLD.middle(g, n, s[0], s[1], 8.5f, 0xFFFFE9B0);
            }
        }
    }

    public static final class Glow extends Module {
        private final Setting.Bool players = bool("Players", true), mobs = bool("Mobs", false), items = bool("Items", false);

        public Glow() { super(Category.RENDER, "Glow", "Outlines players and more through walls."); }

        public boolean glows(Entity e) { return on() && e != mc.player && (e instanceof Player ? players.get() : e instanceof LivingEntity ? mobs.get() : e instanceof ItemEntity && items.get()); }

        public int color(Entity e) { return entityColor(e) & 0xFFFFFF; }
    }

    public static final class Freecam extends Module {
        private final Setting.Num speed = num("Speed", 1, .1, 5, .1);
        public Vec3 pos = Vec3.ZERO;
        public float yaw, pitch;
        private final Anim vx = new Anim(), vy = new Anim(), vz = new Anim();

        public Freecam() { super(Category.RENDER, "Freecam", "Flies the camera away while you stand still."); }

        @Override protected void onEnable() {
            pos = mc.gameRenderer.mainCamera().position();
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
            vx.snap(0);
            vy.snap(0);
            vz.snap(0);
        }

        public void turn(double dx, double dy) {
            yaw += (float) dx * .15f;
            pitch = Mth.clamp(pitch + (float) dy * .15f, -90, 90);
        }

        @Override public void onWorldRender(float partial) {
            var o = mc.options;
            double f = 0, s = 0, u = 0;
            if (mc.gui.screen() == null) {
                f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
                s = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
                u = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
            }
            double k = speed.get() * (o.keySprint.isDown() ? 22 : 11), r = Math.toRadians(yaw), sin = Math.sin(r), cos = Math.cos(r), dt = Frame.realDt;
            vx.v = Ease.approach(vx.v, (float) ((s * cos - f * sin) * k), 9, (float) dt);
            vy.v = Ease.approach(vy.v, (float) (u * k), 9, (float) dt);
            vz.v = Ease.approach(vz.v, (float) ((f * cos + s * sin) * k), 9, (float) dt);
            pos = pos.add(vx.v * dt, vy.v * dt, vz.v * dt);
        }

        @Override public void onWorldLeft() { restore(false); }
    }

    public static final class Freelook extends Module {
        public float yaw, pitch;

        public Freelook() {
            super(Category.RENDER, "Freelook", "Look around without turning.");
            hold = true;
        }

        @Override protected void onEnable() {
            yaw = mc.player.getYRot();
            pitch = mc.player.getXRot();
        }

        public void turn(double dx, double dy) {
            yaw += (float) dx * .15f;
            pitch = Mth.clamp(pitch + (float) dy * .15f, -90, 90);
        }

        @Override public void onWorldLeft() { restore(false); }
    }

    public static final class Zoom extends Module {
        private final Setting.Num amount = num("Zoom", 4, 1.5, 20, .5);
        private final Setting.Bool scroll = bool("Scroll to change", true), smooth = bool("Smooth camera", true);
        private final Anim in = new Anim(), level = new Anim(1);
        private float extra = 1;
        private boolean wasSmooth;

        public Zoom() {
            super(Category.RENDER, "Zoom", "Zooms in while you hold the key.");
            key = InputConstants.KEY_C;
            hold = hidden = true;
        }

        @Override protected void onEnable() {
            extra = 1;
            wasSmooth = mc.options.smoothCamera;
            if (smooth.get()) mc.options.smoothCamera = true;
        }

        @Override protected void onDisable() { mc.options.smoothCamera = wasSmooth; }

        @Override public void onWorldLeft() { restore(false); }

        public float fov(float fov) {
            float t = Ease.outCubic(in.to(on(), .22f)), z = level.glide(extra, 14);
            return t <= 0 ? fov : fov / Ease.lerp(1, amount.getF() * z, t);
        }

        public boolean scrolled(double by) {
            if (!on() || !scroll.get()) return false;
            extra = Math.clamp(extra * (by > 0 ? 1.18f : .85f), .3f, 8);
            return true;
        }
    }

    public static final class NoRender extends Module {
        public final Setting.Bool hurt = bool("Hurt tilt", true), fire = bool("Fire on screen", true), water = bool("Water on screen", true), block = bool("Suffocation", true), totem = bool("Totem animation", false), pumpkin = bool("Pumpkin and frost", true), nausea = bool("Nausea and portal", true), vignette = bool("Dark edges", false), fog = bool("Fog", false), explosions = bool("Explosion particles", false), weather = bool("Weather", false), bosses = bool("Boss bars", false), scoreboard = bool("Scoreboard", false);

        public NoRender() { super(Category.RENDER, "NoRender", "Hides things that get in the way."); }
    }

    public static final class Trajectories extends Module {
        private final Setting.Color colour = color("Colour", 0xFF8CF0CB);

        public Trajectories() { super(Category.RENDER, "Trajectories", "Shows where arrows, pearls and potions will land."); }

        @Override public void onWorldRender(float partial) {
            for (ItemStack s : new ItemStack[] {mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
                double speed, gravity = .03, up = 0;
                if (s.getItem() instanceof BowItem) {
                    speed = BowItem.getPowerForTime(mc.player.isUsingItem() ? mc.player.getTicksUsingItem() : 20) * 3;
                    gravity = .05;
                } else if (s.getItem() instanceof CrossbowItem) {
                    speed = 3.15;
                    gravity = .05;
                } else if (s.is(Items.ENDER_PEARL) || s.is(Items.SNOWBALL) || s.is(Items.EGG)) speed = 1.5;
                else if (s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION)) {
                    speed = .5;
                    gravity = .05;
                    up = -20;
                } else if (s.is(Items.EXPERIENCE_BOTTLE)) {
                    speed = .7;
                    gravity = .07;
                    up = -20;
                } else if (s.is(Items.TRIDENT)) {
                    speed = 2.5;
                    gravity = .05;
                } else continue;
                draw(speed, gravity, up, partial);
                return;
            }
        }

        private void draw(double speed, double gravity, double up, float partial) {
            Vec3 pos = mc.player.getEyePosition(partial).subtract(0, .1, 0), v = Vec3.directionFromRotation(mc.player.getViewXRot(partial) + (float) up, mc.player.getViewYRot(partial)).scale(speed);
            int c = Theme.of(colour);
            for (int i = 0; i < 160; i++) {
                Vec3 next = pos.add(v);
                BlockHitResult hit = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
                if (hit.getType() != HitResult.Type.MISS) next = hit.getLocation();
                if (i > 0) Gizmos.line(pos, next, c, 2);
                if (hit.getType() != HitResult.Type.MISS) {
                    Gizmos.circle(next, .35f, GizmoStyle.stroke(c, 2)).setAlwaysOnTop();
                    Gizmos.point(next, c, 6).setAlwaysOnTop();
                    return;
                }
                pos = next;
                v = v.scale(.99).subtract(0, gravity, 0);
            }
        }
    }

    public static final class BreakEsp extends Module {
        private record Break(BlockPos pos, int stage, long time) {}

        private final Map<Integer, Break> seen = new ConcurrentHashMap<>();

        public BreakEsp() { super(Category.RENDER, "BreakESP", "Shows the blocks others are mining."); }

        public void progress(int id, BlockPos p, int stage) {
            if (stage < 0 || stage > 9) seen.remove(id);
            else seen.put(id, new Break(p.immutable(), stage, mc.level.getGameTime()));
        }

        @Override public void onWorldRender(float partial) {
            seen.values().removeIf(b -> mc.level.getGameTime() - b.time > 200 || mc.level.getBlockState(b.pos).isAir());
            for (var e : seen.entrySet()) {
                if (e.getKey() == mc.player.getId()) continue;
                Break b = e.getValue();
                float t = (b.stage + 1) / 10f;
                box(new AABB(b.pos).inflate(-.5 * (1 - t)), Colors.health(1 - t), 2);
            }
        }

        @Override public void onWorldLeft() { seen.clear(); }
    }

    public static final class LogoutSpots extends Module {
        private record Spot(String name, AABB box, float health) {}

        private final Map<UUID, Spot> last = new HashMap<>(), spots = new HashMap<>();

        public LogoutSpots() { super(Category.RENDER, "LogoutSpots", "Marks where players were when they left the server."); }

        @Override public void onTick() {
            Map<UUID, Spot> now = new HashMap<>();
            for (AbstractClientPlayer p : mc.level.players()) if (p != mc.player && mc.getConnection().getPlayerInfo(p.getUUID()) != null) now.put(p.getUUID(), new Spot(p.getScoreboardName(), p.getBoundingBox(), Damage.health(p)));
            last.forEach((id, s) -> { if (!now.containsKey(id) && mc.getConnection().getPlayerInfo(id) == null) spots.put(id, s); });
            spots.keySet().removeIf(id -> mc.getConnection().getPlayerInfo(id) != null);
            last.clear();
            last.putAll(now);
        }

        @Override public void onWorldRender(float partial) { for (Spot s : spots.values()) box(s.box, 0xFFFF9A7A, 2); }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            for (Spot s : spots.values()) {
                float[] at = Project.at(s.box.getCenter().add(0, 1.2, 0));
                if (at != null) Fonts.BOLD.middle(g, s.name + " left with " + Math.round(s.health), at[0], at[1], 9, 0xFFFFC2AD);
            }
        }

        @Override public void onWorldLeft() {
            last.clear();
            spots.clear();
        }
    }

    public static final class Trail extends Module {
        private final Setting.Num length = num("Points", 400, 20, 2000, 20);
        private final ArrayDeque<Vec3> points = new ArrayDeque<>();

        public Trail() { super(Category.RENDER, "Trail", "Leaves a line where you have been."); }

        @Override public void onTick() {
            Vec3 p = mc.player.position().add(0, .05, 0);
            if (points.isEmpty() || points.peekLast().distanceToSqr(p) > .04) points.add(p);
            while (points.size() > length.get()) points.poll();
        }

        @Override public void onWorldRender(float partial) {
            Vec3 prev = null;
            int i = 0;
            for (Vec3 p : points) {
                float t = i++ / (float) points.size();
                if (prev != null && prev.distanceToSqr(p) < 100) Gizmos.line(prev, p, Colors.with(Theme.at(1 - t), t), 3);
                prev = p;
            }
        }

        @Override protected void onDisable() { points.clear(); }

        @Override public void onWorldLeft() { points.clear(); }
    }

    public static final class CameraTweaks extends Module {
        public final Setting.Num distance = num("Third person distance", 4, 1, 20, .5);
        public final Setting.Bool clip = bool("Through walls", true);

        public CameraTweaks() { super(Category.RENDER, "CameraTweaks", "Sets how far the third person camera sits and lets it pass walls."); }
    }

    public static final class Ambience extends Module {
        public final Setting.Bool time = bool("Set the time", true);
        public final Setting.Num hour = when(num("Time of day", 18000, 0, 24000, 250), time::get);

        public Ambience() { super(Category.RENDER, "Ambience", "Changes the time of day for you only."); }
    }

    public static final class BlockOutline extends Module {
        private final Setting.Color colour = color("Colour", 0xFF8CF0CB);
        private final Setting.Num width = num("Line width", 2, 1, 6, .5), fill = num("Fill", .15, 0, .8, .05);
        private final Setting.Bool glide = bool("Glide between blocks", true);
        private final Anim[] a = {new Anim(), new Anim(), new Anim(), new Anim(), new Anim(), new Anim()};
        private final Anim in = new Anim();

        public BlockOutline() { super(Category.RENDER, "BlockOutline", "Outlines the block you point at, gliding from one to the next."); }

        @Override public void onWorldRender(float partial) {
            AABB b = null;
            if (mc.hitResult instanceof BlockHitResult h && h.getType() == HitResult.Type.BLOCK) {
                VoxelShape s = mc.level.getBlockState(h.getBlockPos()).getShape(mc.level, h.getBlockPos());
                if (!s.isEmpty()) b = s.bounds().move(h.getBlockPos());
            }
            float was = in.v, v = in.to(b != null, .15f);
            if (b != null) {
                double[] t = {b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ};
                for (int i = 0; i < 6; i++) {
                    // Kept relative to the player so far-out coordinates stay precise.
                    if (!glide.get() || was == 0 || Math.abs(a[i].v - t[i]) > 8) a[i].snap((float) t[i]);
                    else a[i].glide((float) t[i], 22);
                }
            }
            if (v <= 0) return;
            int c = Theme.of(colour);
            AABB box = glide.get() && Math.abs(a[0].v) < 60000 ? new AABB(a[0].v, a[1].v, a[2].v, a[3].v, a[4].v, a[5].v) : b;
            if (box != null) Gizmos.cuboid(box.inflate(.002), GizmoStyle.strokeAndFill(Colors.alpha(c, v), width.getF(), Colors.with(c, fill.getF() * v)));
        }
    }

    public static final class VoidEsp extends Module {
        private final List<BlockPos> holes = new ArrayList<>();
        private int wait;

        public VoidEsp() { super(Category.RENDER, "VoidESP", "Marks gaps in the bedrock floor."); }

        @Override public void onTick() {
            if (wait-- > 0) return;
            wait = 10;
            holes.clear();
            int y = mc.level.getMinY();
            if (mc.player.getY() > y + 40) return;
            BlockPos me = mc.player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.getX() - 10, y, me.getZ() - 10, me.getX() + 10, y, me.getZ() + 10)) if (!mc.level.getBlockState(p).is(Blocks.BEDROCK)) holes.add(p.immutable());
        }

        @Override public void onWorldRender(float partial) { for (BlockPos p : holes) box(new AABB(p), 0xFFFF6B5A, 2); }

        @Override public void onWorldLeft() { holes.clear(); }
    }

    public static final class LightOverlay extends Module {
        private final Setting.Num range = num("Range", 12, 4, 24, 1);
        private final List<BlockPos> dark = new ArrayList<>();
        private int wait;

        public LightOverlay() { super(Category.RENDER, "LightOverlay", "Marks floors dark enough for monsters."); }

        @Override public void onTick() {
            if (wait-- > 0) return;
            wait = 10;
            dark.clear();
            int r = range.getInt();
            BlockPos me = mc.player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -5, -r), me.offset(r, 4, r))) {
                if (mc.level.getBrightness(LightLayer.BLOCK, p) > 0 || !mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty() || !mc.level.getFluidState(p).isEmpty() || !mc.level.getBlockState(p.below()).isFaceSturdy(mc.level, p.below(), Direction.UP)) continue;
                dark.add(p.immutable());
                if (dark.size() > 1500) return;
            }
        }

        @Override public void onWorldRender(float partial) {
            for (BlockPos p : dark) {
                int c = mc.level.getBrightness(LightLayer.SKY, p) > 7 ? 0xC0FFE08A : 0xC0FF7A6B;
                double x = p.getX(), y = p.getY() + .02, z = p.getZ();
                Gizmos.line(new Vec3(x + .2, y, z + .2), new Vec3(x + .8, y, z + .8), c, 2);
                Gizmos.line(new Vec3(x + .8, y, z + .2), new Vec3(x + .2, y, z + .8), c, 2);
            }
        }

        @Override public void onWorldLeft() { dark.clear(); }
    }

    public static final class CityEsp extends Module {
        public CityEsp() { super(Category.RENDER, "CityESP", "Shows the surround blocks of enemies that can be mined."); }

        @Override public void onWorldRender(float partial) {
            for (LivingEntity e : Targets.near(8, true, false)) for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = e.blockPosition().relative(d);
                if (!mc.level.getBlockState(p).isAir() && mc.level.getBlockState(p).getDestroySpeed(mc.level, p) >= 0) box(new AABB(p), 0xFFFF9A7A, 1.5f);
            }
        }
    }

    public static final class Crosshair extends Module {
        private final Setting.Choice shape = choice("Shape", "Cross", "Diamond", "Dot", "Shard");
        private final Setting.Num size = num("Size", 5, 1, 14, .5), gap = num("Gap", 2, 0, 10, .5), thick = num("Thickness", 1.4, .5, 5, .1);
        private final Setting.Color colour = color("Colour", 0xFFFFFFFF);
        private final Setting.Bool outline = bool("Dark outline", true), cooldown = bool("Open up after attacking", true);
        private final Anim spread = new Anim();

        public Crosshair() { super(Category.RENDER, "Crosshair", "Your own crosshair."); }

        public void draw(GuiGraphicsExtractor g) {
            float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f, s = size.getF(), t = thick.getF(), k = gap.getF() + (cooldown.get() ? spread.glide((1 - mc.player.getAttackStrengthScale(0)) * 4, 18) : 0);
            int c = Theme.of(colour);
            Draw d = new Draw(g);
            for (int pass = outline.get() ? 0 : 1; pass < 2; pass++) {
                float o = pass == 0 ? .7f : 0;
                int col = pass == 0 ? 0xB0000000 : c;
                switch (shape.get()) {
                    case "Cross" -> {
                        d.rect(cx - k - s - o, cy - t / 2 - o, s + o * 2, t + o * 2, col);
                        d.rect(cx + k - o, cy - t / 2 - o, s + o * 2, t + o * 2, col);
                        d.rect(cx - t / 2 - o, cy - k - s - o, t + o * 2, s + o * 2, col);
                        d.rect(cx - t / 2 - o, cy + k - o, t + o * 2, s + o * 2, col);
                    }
                    case "Diamond" -> d.stroke(Draw.diamond(cx, cy, s + k), t + o * 2, col);
                    case "Dot" -> d.circle(cx, cy, t + o, col);
                    default -> d.stroke(Draw.shard(cx - s - k, cy - s - k, (s + k) * 2, (s + k) * 2, (s + k) * .7f), t + o * 2, col);
                }
            }
            d.flush();
        }
    }

    public static final class Snowfall extends Module {
        private final Setting.Num amount = num("Amount", 4, 1, 20, 1);

        public Snowfall() { super(Category.RENDER, "Snowfall", "Gentle snow around you, wherever you are."); }

        @Override public void onTick() {
            var r = mc.player.getRandom();
            for (int i = 0; i < amount.get(); i++) mc.level.addParticle(ParticleTypes.SNOWFLAKE, mc.player.getX() + (r.nextDouble() - .5) * 24, mc.player.getY() + 3 + r.nextDouble() * 7, mc.player.getZ() + (r.nextDouble() - .5) * 24, (r.nextDouble() - .5) * .02, -.06, (r.nextDouble() - .5) * .02);
        }
    }

    /** A ring that grows and fades. */
    private record Ring(Vec3 pos, long start, int life, float from, float to, int color) {}

    private static final class Rings {
        final List<Ring> list = new ArrayList<>();

        void draw() {
            long now = net.minecraft.util.Util.getMillis();
            list.removeIf(r -> now - r.start > r.life);
            for (Ring r : list) {
                float t = (now - r.start) / (float) r.life, e = Ease.outCubic(t), radius = Ease.lerp(r.from, r.to, e);
                ring(r.pos, radius, Colors.with(r.color, 1 - t), 3);
            }
        }
    }

    static void ring(Vec3 c, float radius, int color, float width) {
        int n = 36;
        Vec3 prev = null;
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            Vec3 p = c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            if (prev != null) Gizmos.line(prev, p, color, width);
            prev = p;
        }
    }

    public static final class JumpRings extends Module {
        private final Setting.Num radius = num("Size", 1.1, .4, 3, .1);
        private final Rings rings = new Rings();
        private boolean was = true;

        public JumpRings() { super(Category.RENDER, "JumpRings", "A ring spreads from under you when you jump."); }

        @Override public void onTick() {
            if (was && !mc.player.onGround() && mc.player.getDeltaMovement().y > 0) rings.list.add(new Ring(new Vec3(mc.player.getX(), mc.player.yo + .03, mc.player.getZ()), net.minecraft.util.Util.getMillis(), 700, .2f, radius.getF(), Theme.accent()));
            was = mc.player.onGround();
        }

        @Override public void onWorldRender(float partial) { rings.draw(); }
    }

    public static final class PopEffects extends Module {
        private final Rings rings = new Rings();

        public PopEffects() { super(Category.RENDER, "PopEffects", "Rings burst out when someone uses a totem."); }

        public void popped(Entity e) { if (on()) for (int i = 0; i < 3; i++) rings.list.add(new Ring(e.position().add(0, .2 + i * .7, 0), net.minecraft.util.Util.getMillis() + i * 90L, 900, .3f, 2.2f, Theme.at(i / 2f))); }

        @Override public void onWorldRender(float partial) {
            rings.list.removeIf(r -> r.start > net.minecraft.util.Util.getMillis() + 5000);
            rings.draw();
        }
    }

    public static final class TargetRing extends Module {
        private final Setting.Num range = num("Range", 8, 3, 30, 1);
        private final Setting.Bool mobs = bool("Mobs", false);

        public TargetRing() { super(Category.RENDER, "TargetRing", "A ring travels up and down the nearest enemy."); }

        @Override public void onWorldRender(float partial) {
            var near = Targets.near(range.get(), true, mobs.get());
            if (near.isEmpty()) return;
            LivingEntity e = near.getFirst();
            float t = .5f + .5f * (float) Math.sin(Frame.time * 2.6f);
            Vec3 p = e.getPosition(partial);
            for (int i = 0; i < 5; i++) {
                float h = Ease.clamp(t - i * .035f * Math.signum((float) Math.cos(Frame.time * 2.6f)));
                ring(p.add(0, h * e.getBbHeight(), 0), e.getBbWidth() * .85f, Colors.with(Theme.at(h), 1 - i * .2f), 3 - i * .4f);
            }
        }
    }

    public static final class HitMarker extends Module {
        private final Setting.Bool sound = bool("Sound", true);
        private final Anim in = new Anim();

        public HitMarker() { super(Category.RENDER, "HitMarker", "Marks your hits at the crosshair."); }

        public void hit() {
            if (!on()) return;
            in.snap(1);
            if (sound.get()) mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT, 1.6f, .7f));
        }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            float t = in.to(0, .3f);
            if (t <= 0) return;
            float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f, a = 4 + (1 - t) * 2, b = a + 5;
            Draw d = new Draw(g);
            for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) d.line(cx + a * sx, cy + a * sy, cx + b * sx, cy + b * sy, 1.4f, Colors.with(0xFFFFFF, t));
            d.flush();
        }
    }

    public static final class DamageNumbers extends Module {
        private record Number(Vec3 pos, float amount, long start, float drift) {}

        private final Map<Integer, Float> health = new HashMap<>();
        private final List<Number> numbers = new ArrayList<>();

        public DamageNumbers() { super(Category.RENDER, "DamageNumbers", "Damage and healing float up from whoever it happened to."); }

        @Override public void onTick() {
            Map<Integer, Float> now = new HashMap<>();
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity l) || e == mc.player || e.distanceTo(mc.player) > 48) continue;
                float h = Damage.health(l);
                Float was = health.get(e.getId());
                if (was != null && Math.abs(h - was) > .05f) numbers.add(new Number(e.position().add(0, e.getBbHeight() * .8, 0), h - was, net.minecraft.util.Util.getMillis(), mc.player.getRandom().nextFloat() - .5f));
                now.put(e.getId(), h);
            }
            health.clear();
            health.putAll(now);
        }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            long now = net.minecraft.util.Util.getMillis();
            numbers.removeIf(n -> now - n.start > 1100);
            for (Number n : numbers) {
                float t = (now - n.start) / 1100f, e = Ease.outCubic(t);
                float[] s = Project.at(n.pos.add(0, e * .9, 0));
                if (s == null) continue;
                float size = 8 + Math.min(7, Math.abs(n.amount) * .5f);
                Fonts.DISPLAY.middle(g, String.format(java.util.Locale.ROOT, "%.1f", Math.abs(n.amount)), s[0] + n.drift * 26 * e, s[1], size * Ease.outBack(Ease.clamp(t * 4)), Colors.with(n.amount < 0 ? 0xFFFF8A7A : 0xFF9BE88A, 1 - Ease.clamp(t * 2 - 1)));
            }
        }

        @Override public void onWorldLeft() {
            health.clear();
            numbers.clear();
        }
    }

    public static final class PlayerArrows extends Module {
        private final Setting.Num radius = num("Radius", 46, 20, 120, 2), size = num("Size", 6, 3, 14, 1);

        public PlayerArrows() { super(Category.RENDER, "PlayerArrows", "Arrows point to players who are off screen."); }

        @Override public void onOverlay(GuiGraphicsExtractor g, float partial) {
            float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f, r = radius.getF(), s = size.getF();
            Draw d = new Draw(g);
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p == mc.player) continue;
                float[] at = Project.at(p.getPosition(partial).add(0, 1, 0));
                if (at != null && at[0] > 0 && at[0] < g.guiWidth() && at[1] > 0 && at[1] < g.guiHeight()) continue;
                double a = Math.toRadians(Math.toDegrees(Math.atan2(p.getZ() - mc.player.getZ(), p.getX() - mc.player.getX())) - mc.player.getViewYRot(partial) - 180);
                float ux = (float) Math.cos(a), uy = (float) Math.sin(a), x = cx + ux * r, y = cy + uy * r;
                int c = Colors.with(entityColor(p), (float) Math.clamp(1.2 - p.distanceTo(mc.player) / 120, .35, 1));
                d.poly(new float[] {x + ux * s, y + uy * s, x - uy * s * .7f - ux * s * .5f, y + ux * s * .7f - uy * s * .5f, x - ux * s * .1f, y - uy * s * .1f}, c);
                d.poly(new float[] {x + ux * s, y + uy * s, x + uy * s * .7f - ux * s * .5f, y - ux * s * .7f - uy * s * .5f, x - ux * s * .1f, y - uy * s * .1f}, c);
            }
            d.flush();
        }
    }

    public static final class HandView extends Module {
        public final Setting.Num x = num("Sideways", 0, -1, 1, .02), y = num("Up", 0, -1, 1, .02), z = num("Forward", 0, -1, 1, .02), scale = num("Scale", 1, .3, 2, .05), tilt = num("Tilt", 0, -90, 90, 1), yaw = num("Turn", 0, -90, 90, 1);

        public HandView() { super(Category.RENDER, "HandView", "Moves and resizes the items in your hands."); }
    }

    public static final class ChunkBorders extends Module {
        public ChunkBorders() { super(Category.RENDER, "ChunkBorders", "Outlines the chunk you are in."); }

        @Override public void onWorldRender(float partial) {
            int x = mc.player.getBlockX() >> 4 << 4, z = mc.player.getBlockZ() >> 4 << 4;
            double y = mc.player.getY();
            int c = Theme.accent();
            for (int[] k : new int[][] {{0, 0}, {16, 0}, {0, 16}, {16, 16}}) Gizmos.line(new Vec3(x + k[0], y - 24, z + k[1]), new Vec3(x + k[0], y + 32, z + k[1]), c, 2);
            for (int h = (int) Math.floor(y / 8) * 8 - 16; h <= y + 24; h += 8) {
                int col = Colors.with(c, .35f);
                Gizmos.line(new Vec3(x, h, z), new Vec3(x + 16, h, z), col, 1.5f);
                Gizmos.line(new Vec3(x + 16, h, z), new Vec3(x + 16, h, z + 16), col, 1.5f);
                Gizmos.line(new Vec3(x + 16, h, z + 16), new Vec3(x, h, z + 16), col, 1.5f);
                Gizmos.line(new Vec3(x, h, z + 16), new Vec3(x, h, z), col, 1.5f);
            }
        }
    }

    public static final class LookLines extends Module {
        private final Setting.Num length = num("Length", 4, 1, 30, 1);

        public LookLines() { super(Category.RENDER, "LookLines", "Shows where other players are looking."); }

        @Override public void onWorldRender(float partial) {
            for (AbstractClientPlayer p : mc.level.players()) {
                if (p == mc.player) continue;
                Vec3 e = p.getEyePosition(partial);
                Gizmos.line(e, e.add(p.getViewVector(partial).scale(length.get())), Colors.with(entityColor(p), .9f), 2).setAlwaysOnTop();
            }
        }
    }

    public static List<Module> all() {
        return List.of(new Tracers(), new Nametags(), new StorageEsp(), new BlockEsp(), new ItemEsp(), new Glow(), new Freecam(), new Freelook(), new Zoom(), new NoRender(), new Trajectories(), new BreakEsp(), new LogoutSpots(), new Trail(), new CameraTweaks(), new Ambience(), new BlockOutline(), new VoidEsp(), new LightOverlay(), new CityEsp(), new Crosshair(), new Snowfall(), new JumpRings(), new PopEffects(), new TargetRing(), new HitMarker(), new DamageNumbers(), new PlayerArrows(), new HandView(), new ChunkBorders(), new LookLines());
    }
}
