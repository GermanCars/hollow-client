package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.Notices;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Modules that change or use the world. */
public final class WorldModules {
    private static final Minecraft mc = Minecraft.getInstance();

    private WorldModules() {}

    static Direction faceTowardsMe(BlockPos p) { return Direction.getApproximateNearest(mc.player.getEyePosition().subtract(Vec3.atCenterOf(p))); }

    /** Looks at a block and mines it a tick's worth. */
    static void mine(BlockPos p) {
        Actions.submit(new Request(Priority.SCAFFOLD, Placement.look(Vec3.atCenterOf(p)), false, -1, () -> {
            mc.gameMode.continueDestroyBlock(p, faceTowardsMe(p));
            mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
        }));
    }

    static void useBlock(BlockPos p, int slot) {
        Direction f = faceTowardsMe(p);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(p).add(f.getStepX() * .5, f.getStepY() * .5, f.getStepZ() * .5), f, p, false);
        Actions.submit(new Request(Priority.PRINTER, Placement.look(hit.getLocation()), false, slot, () -> Placement.click(InteractionHand.MAIN_HAND, hit)));
    }

    static void useOn(Entity e, InteractionHand hand) { mc.gameMode.interact(mc.player, e, new EntityHitResult(e, e.getBoundingBox().getCenter()), hand); }

    static String id(BlockState s) { return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString(); }

    static boolean breakable(BlockPos p) {
        BlockState s = mc.level.getBlockState(p);
        return !s.isAir() && s.getFluidState().isEmpty() && s.getDestroySpeed(mc.level, p) >= 0;
    }

    static List<BlockPos> around(double r) {
        List<BlockPos> out = new ArrayList<>();
        int n = (int) Math.ceil(r);
        BlockPos me = BlockPos.containing(mc.player.getEyePosition());
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-n, -n, -n), me.offset(n, n, n))) if (Placement.inReach(p)) out.add(p.immutable());
        out.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(mc.player.getEyePosition())));
        return out;
    }

    public static final class Nuker extends Module {
        private final Setting.Choice mode = choice("Break", "Everything", "Only these", "All but these"), shape = choice("Shape", "All around", "Above my feet");
        private final Setting.Ids blocks = when(ids("Blocks", Setting.Ids.Kind.BLOCK, "stone", "netherrack", "short_grass"), () -> !mode.is("Everything"));
        private final Setting.Bool tool = bool("Pick the best tool", true);
        private BlockPos target;

        public Nuker() { super(Category.WORLD, "Nuker", "Breaks blocks around you."); }

        private boolean wants(BlockPos p) {
            if (!breakable(p) || shape.is("Above my feet") && p.getY() < mc.player.getBlockY()) return false;
            return mode.is("Everything") || mode.is("Only these") == blocks.has(id(mc.level.getBlockState(p)));
        }

        @Override public void onTick() {
            if (target == null || !wants(target) || !Placement.inReach(target)) {
                target = null;
                for (BlockPos p : around(5)) if (wants(p)) {
                    target = p;
                    break;
                }
            }
            if (target == null) return;
            if (tool.get()) PlayerModules.AutoTool.pick(target);
            mine(target);
        }

        @Override public void onWorldRender(float partial) { if (target != null) Gizmos.cuboid(target, GizmoStyle.stroke(0xFF000000 | dev.hollow.ui.Theme.accent(), 2)).setAlwaysOnTop(); }

        @Override protected void onDisable() { target = null; }

        @Override public void onWorldLeft() { target = null; }
    }

    public static final class VeinMiner extends Module {
        private final Setting.Num most = num("Most blocks", 48, 4, 200, 4);
        private final Setting.Ids blocks = ids("Blocks", Setting.Ids.Kind.BLOCK, "coal_ore", "iron_ore", "gold_ore", "diamond_ore", "emerald_ore", "redstone_ore", "lapis_ore", "copper_ore", "deepslate_coal_ore", "deepslate_iron_ore", "deepslate_gold_ore", "deepslate_diamond_ore", "deepslate_emerald_ore", "deepslate_redstone_ore", "deepslate_lapis_ore", "deepslate_copper_ore", "nether_quartz_ore", "nether_gold_ore", "ancient_debris", "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "cherry_log", "mangrove_log", "pale_oak_log");
        private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        public VeinMiner() { super(Category.WORLD, "VeinMiner", "Mines the rest of a vein or tree after you break one block."); }

        public void started(BlockPos p) {
            if (!on() || !queue.isEmpty()) return;
            BlockState s = mc.level.getBlockState(p);
            if (!blocks.has(id(s))) return;
            Set<BlockPos> seen = new HashSet<>(Set.of(p));
            ArrayDeque<BlockPos> open = new ArrayDeque<>(Set.of(p));
            while (!open.isEmpty() && seen.size() < most.getInt()) {
                BlockPos c = open.poll();
                for (BlockPos n : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) if (mc.level.getBlockState(n).is(s.getBlock()) && seen.add(n.immutable())) open.add(n.immutable());
            }
            queue.addAll(seen);
        }

        @Override public void onTick() {
            queue.removeIf(p -> !breakable(p) || p.distToCenterSqr(mc.player.position()) > 400);
            BlockPos next = queue.stream().filter(Placement::inReach).findFirst().orElse(null);
            if (next != null && !mc.options.keyAttack.isDown()) mine(next);
        }

        @Override public void onWorldRender(float partial) { for (BlockPos p : queue) Gizmos.cuboid(p, GizmoStyle.stroke(0xA0000000 | dev.hollow.ui.Theme.accent() & 0xFFFFFF, 1.5f)).setAlwaysOnTop(); }

        @Override protected void onDisable() { queue.clear(); }

        @Override public void onWorldLeft() { queue.clear(); }
    }

    public static final class AutoTunnel extends Module {
        private final Setting.Num wide = num("Width", 1, 1, 5, 1), tall = num("Height", 2, 2, 4, 1);
        private final Setting.Bool walk = bool("Walk forward", true), floor = bool("Fill the floor", true);
        private boolean walking;

        public AutoTunnel() { super(Category.WORLD, "AutoTunnel", "Digs a tunnel the way you are facing."); }

        @Override public void onMove() {
            Direction d = mc.player.getDirection(), side = d.getClockWise();
            BlockPos base = mc.player.blockPosition();
            boolean clear = true;
            for (int ahead = 1; ahead <= 2 && clear; ahead++) for (int y = 0; y < tall.getInt(); y++) for (int w = 0; w < wide.getInt(); w++) {
                BlockPos p = base.relative(d, ahead).above(y).relative(side, w - (wide.getInt() - 1) / 2);
                if (breakable(p) && !mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty()) clear = false;
            }
            walking = walk.get() && clear && mc.gui.screen() == null;
            if (walk.get()) mc.options.keyUp.setDown(walking);
        }

        @Override public void onTick() {
            Direction d = mc.player.getDirection(), side = d.getClockWise();
            BlockPos base = mc.player.blockPosition();
            for (int ahead = 1; ahead <= 2; ahead++) {
                for (int y = tall.getInt() - 1; y >= 0; y--) for (int w = 0; w < wide.getInt(); w++) {
                    BlockPos p = base.relative(d, ahead).above(y).relative(side, w - (wide.getInt() - 1) / 2);
                    if (!breakable(p) || !Placement.inReach(p)) continue;
                    PlayerModules.AutoTool.pick(p);
                    mine(p);
                    return;
                }
                if (floor.get()) {
                    BlockPos p = base.relative(d, ahead).below();
                    int slot = Inv.hotbar(s -> s.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().isSolid() && !(b.getBlock() instanceof FallingBlock));
                    if (mc.level.getBlockState(p).canBeReplaced() && Placement.place(Priority.SCAFFOLD, p, slot)) return;
                }
            }
        }

        @Override protected void onDisable() { mc.options.keyUp.setDown(false); }

        @Override public void onWorldLeft() { mc.options.keyUp.setDown(false); }
    }

    public static final class LiquidFill extends Module {
        private final Setting.Bool water = bool("Water", true), lava = bool("Lava", true), sources = bool("Sources only", true);

        public LiquidFill() { super(Category.WORLD, "LiquidFill", "Fills liquids around you with blocks."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(s -> s.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().isSolid());
            if (slot < 0) return;
            for (BlockPos p : around(5)) {
                var f = mc.level.getFluidState(p);
                if (f.isEmpty() || !mc.level.getBlockState(p).canBeReplaced() || sources.get() && !f.isSource()) continue;
                boolean isLava = f.is(net.minecraft.tags.FluidTags.LAVA);
                if ((isLava ? lava.get() : water.get()) && Placement.place(Priority.SCAFFOLD, p, slot)) return;
            }
        }
    }

    public static final class SpawnProof extends Module {
        private final Setting.Ids with = ids("Use", Setting.Ids.Kind.ITEM, "torch", "oak_button", "stone_button", "oak_slab", "white_carpet");
        private final Setting.Num light = num("Light below", 1, 1, 8, 1);

        public SpawnProof() { super(Category.WORLD, "SpawnProof", "Covers dark floors so nothing spawns there."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(s -> with.has(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
            if (slot < 0) return;
            for (BlockPos p : around(4.5)) {
                if (!mc.level.getBlockState(p).isAir() || mc.level.getBrightness(LightLayer.BLOCK, p) >= light.get() || !mc.level.getBlockState(p.below()).isFaceSturdy(mc.level, p.below(), Direction.UP)) continue;
                if (Placement.place(Priority.SCAFFOLD, p, slot)) return;
            }
        }
    }

    public static final class AutoTorch extends Module {
        private final Setting.Num light = num("Light below", 5, 1, 12, 1);
        private int wait;

        public AutoTorch() { super(Category.WORLD, "AutoTorch", "Puts a torch at your feet when it gets dark."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(s -> s.is(Items.TORCH) || s.is(Items.SOUL_TORCH));
            BlockPos p = mc.player.blockPosition();
            if (wait-- > 0 || slot < 0 || !mc.player.onGround() || mc.level.getBrightness(LightLayer.BLOCK, p) >= light.get()) return;
            if (Placement.place(Priority.SCAFFOLD, p, slot)) wait = 20;
        }
    }

    public static final class ChestStealer extends Module {
        private final Setting.Num delay = num("Delay", 1, 0, 10, 1);
        private final Setting.Choice which = choice("Take", "Everything", "Only these", "All but these"), way = choice("Direction", "Take", "Store");
        private final Setting.Ids items = when(ids("Items", Setting.Ids.Kind.ITEM), () -> !which.is("Everything"));
        private final Setting.Bool close = bool("Close when done", true);
        private int wait;

        public ChestStealer() { super(Category.WORLD, "ChestStealer", "Empties or fills chests you open."); }

        private boolean wants(ItemStack s) { return !s.isEmpty() && (which.is("Everything") || which.is("Only these") == items.has(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())); }

        @Override public void onTick() {
            if (!(mc.gui.screen() instanceof ContainerScreen) && !(mc.gui.screen() instanceof ShulkerBoxScreen) || wait-- > 0) return;
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.gui.screen();
            var menu = screen.getMenu();
            int own = menu.slots.size() - 36;
            for (int i = way.is("Take") ? 0 : own; i < (way.is("Take") ? own : menu.slots.size()); i++) {
                if (!wants(menu.slots.get(i).getItem())) continue;
                mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
                wait = delay.getInt();
                return;
            }
            if (close.get()) mc.player.closeContainer();
        }
    }

    public static final class AutoFarm extends Module {
        private final Setting.Bool replant = bool("Replant", true), meal = bool("Bone meal", false);
        private int wait;

        public AutoFarm() { super(Category.WORLD, "AutoFarm", "Harvests ripe crops and plants them again."); }

        @Override public void onTick() {
            if (wait-- > 0) return;
            for (BlockPos p : around(4.5)) {
                BlockState s = mc.level.getBlockState(p);
                if (s.getBlock() instanceof CropBlock c) {
                    if (c.isMaxAge(s)) {
                        mine(p);
                        return;
                    }
                    int slot = meal.get() ? Inv.hotbar(Items.BONE_MEAL) : -1;
                    if (slot >= 0) {
                        useBlock(p, slot);
                        wait = 2;
                        return;
                    }
                } else if (s.getBlock() instanceof NetherWartBlock && s.getValue(NetherWartBlock.AGE) == 3) {
                    mine(p);
                    return;
                } else if (replant.get() && s.isAir()) {
                    Block below = mc.level.getBlockState(p.below()).getBlock();
                    int slot = below instanceof FarmlandBlock ? Inv.hotbar(i -> i.is(Items.WHEAT_SEEDS) || i.is(Items.CARROT) || i.is(Items.POTATO) || i.is(Items.BEETROOT_SEEDS)) : below instanceof SoulSandBlock ? Inv.hotbar(Items.NETHER_WART) : -1;
                    if (slot < 0) continue;
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(p.below()).add(0, .5, 0), Direction.UP, p.below(), false);
                    Actions.submit(new Request(Priority.PRINTER, Placement.look(hit.getLocation()), false, slot, () -> Placement.click(InteractionHand.MAIN_HAND, hit)));
                    wait = 1;
                    return;
                }
            }
        }
    }

    /** Uses an item on nearby animals, each one only now and then. */
    private abstract static class Husbandry extends Module {
        final Setting.Num range = num("Range", 4, 1, 5, .5);
        private final Map<Integer, Long> done = new HashMap<>();
        private int wait;

        Husbandry(String name, String description) { super(Category.WORLD, name, description); }

        abstract boolean wants(LivingEntity e, ItemStack held);

        long again() { return 1200; }

        @Override public void onTick() {
            if (wait-- > 0) return;
            long now = mc.level.getGameTime();
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof LivingEntity l) || e == mc.player || e.distanceTo(mc.player) > range.get() || now - done.getOrDefault(e.getId(), -99999L) < again()) continue;
                int slot = Inv.hotbar(s -> wants(l, s));
                if (slot < 0) continue;
                done.put(e.getId(), now);
                Actions.submit(new Request(Priority.PRINTER, Placement.look(e.getBoundingBox().getCenter()), false, slot, () -> useOn(e, InteractionHand.MAIN_HAND)));
                wait = 4;
                return;
            }
        }

        @Override public void onWorldLeft() { done.clear(); }
    }

    public static final class AutoBreed extends Husbandry {
        public AutoBreed() { super("AutoBreed", "Feeds animals so they breed."); }

        @Override boolean wants(LivingEntity e, ItemStack s) { return e instanceof Animal a && !a.isBaby() && a.isFood(s); }

        @Override long again() { return 6000; }
    }

    public static final class AutoShear extends Husbandry {
        public AutoShear() { super("AutoShear", "Shears sheep."); }

        @Override boolean wants(LivingEntity e, ItemStack s) { return e instanceof Sheep sh && sh.readyForShearing() && s.is(Items.SHEARS); }

        @Override long again() { return 60; }
    }

    public static final class AutoNametag extends Husbandry {
        private final Setting.Bool monsters = bool("Monsters too", false);

        public AutoNametag() { super("AutoNametag", "Puts name tags on mobs."); }

        @Override boolean wants(LivingEntity e, ItemStack s) { return !(e instanceof Player) && !e.hasCustomName() && s.is(Items.NAME_TAG) && s.has(DataComponents.CUSTOM_NAME) && (monsters.get() || !(e instanceof Enemy)); }
    }

    public static final class Flamethrower extends Module {
        private int wait;

        public Flamethrower() { super(Category.WORLD, "Flamethrower", "Sets monsters near you on fire with flint and steel."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(Items.FLINT_AND_STEEL);
            if (wait-- > 0 || slot < 0) return;
            for (LivingEntity e : Targets.near(4, false, true)) {
                if (e.isOnFire() || e.isInWater() || !(e instanceof Enemy) && !(e instanceof Animal)) continue;
                BlockPos b = e.blockPosition().below();
                if (!mc.level.getBlockState(e.blockPosition()).canBeReplaced() || !Placement.inReach(b)) continue;
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(b).add(0, .5, 0), Direction.UP, b, false);
                Actions.submit(new Request(Priority.PRINTER, Placement.look(hit.getLocation()), false, slot, () -> Placement.click(InteractionHand.MAIN_HAND, hit)));
                wait = 10;
                return;
            }
        }
    }

    public static final class AutoSign extends Module {
        public final Setting.Text a = text("Line 1", "Hollow Client"), b = text("Line 2", "was here"), c = text("Line 3", ""), d = text("Line 4", "");

        public AutoSign() { super(Category.WORLD, "AutoSign", "Writes signs for you as you place them."); }

        /** True when the editor should stay shut. */
        public boolean write(BlockPos p, net.minecraft.world.level.block.entity.SignTextSlot slot) {
            if (!on()) return false;
            mc.player.connection.send(new ServerboundSignUpdatePacket(p, List.of(a.get(), b.get(), c.get(), d.get()), slot));
            return true;
        }
    }

    public static final class AirPlace extends Module {
        private boolean was;

        public AirPlace() { super(Category.WORLD, "AirPlace", "Places blocks in mid-air where the server allows it."); }

        @Override public void onTick() {
            boolean now = mc.options.keyUse.isDown();
            if (now && !was && mc.gui.screen() == null && mc.player.getMainHandItem().getItem() instanceof BlockItem && mc.hitResult instanceof BlockHitResult h && h.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                BlockPos p = BlockPos.containing(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(4)));
                Placement.click(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p), Direction.DOWN, p, false));
            }
            was = now;
        }
    }

    public static final class Timer extends Module {
        private final Setting.Num speed = num("Speed", 1.1, .1, 5, .05);

        public Timer() { super(Category.WORLD, "Timer", "Runs the game faster or slower."); }

        public float factor() { return on() ? speed.getF() : 1; }

        @Override public String info() { return speed.text() + "x"; }
    }

    public static final class StashFinder extends Module {
        private final Setting.Num least = num("Containers in a chunk", 8, 2, 60, 1);
        private final Set<Long> told = new HashSet<>();
        private int wait;

        public StashFinder() { super(Category.WORLD, "StashFinder", "Tells you about chunks full of chests and saves them to the log."); }

        @Override public void onTick() {
            if (wait-- > 0) return;
            wait = 40;
            Map<Long, Integer> n = new HashMap<>();
            for (BlockPos p : RenderModules.blockEntities().keySet()) n.merge(net.minecraft.world.level.ChunkPos.pack(p.getX() >> 4, p.getZ() >> 4), RenderModules.StorageEsp.storage(mc.level.getBlockEntity(p)) ? 1 : 0, Integer::sum);
            n.forEach((c, k) -> {
                if (k < least.get() || !told.add(c)) return;
                int x = net.minecraft.world.level.ChunkPos.getX(c) * 16 + 8, z = net.minecraft.world.level.ChunkPos.getZ(c) * 16 + 8;
                Hollow.tell(k + " containers near " + x + ", " + z);
                Hollow.LOG.info("StashFinder: {} containers near {}, {} in {}", k, x, z, mc.level.dimension().identifier());
                Notices.add("Stash found", k + " containers", true);
            });
        }

        @Override public void onWorldLeft() { told.clear(); }
    }

    public static final class BoneMealer extends Module {
        private int wait;

        public BoneMealer() { super(Category.WORLD, "BoneMealer", "Bone meals saplings and plants around you."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(Items.BONE_MEAL);
            if (wait-- > 0 || slot < 0) return;
            for (BlockPos p : around(4.5)) {
                BlockState s = mc.level.getBlockState(p);
                if (s.is(Blocks.GRASS_BLOCK) || !(s.getBlock() instanceof BonemealableBlock b) || !b.isValidBonemealTarget(mc.level, p, s, BonemealSource.INTERACTION)) continue;
                useBlock(p, slot);
                wait = 3;
                return;
            }
        }
    }

    public static final class AutoDoor extends Module {
        private final Map<BlockPos, Long> opened = new HashMap<>();

        public AutoDoor() { super(Category.WORLD, "AutoDoor", "Opens wooden doors in front of you and shuts them behind."); }

        @Override public void onTick() {
            long now = mc.level.getGameTime();
            for (BlockPos p : around(3.5)) {
                BlockState s = mc.level.getBlockState(p);
                if (!(s.getBlock() instanceof DoorBlock d) || s.is(Blocks.IRON_DOOR) || s.getValue(DoorBlock.HALF) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) continue;
                double dist = Math.sqrt(p.distToCenterSqr(mc.player.position()));
                boolean open = s.getValue(DoorBlock.OPEN), ahead = Vec3.atCenterOf(p).subtract(mc.player.position()).dot(mc.player.getLookAngle()) > 0;
                if (now - opened.getOrDefault(p, -100L) < 12) continue;
                if (!open && dist < 1.8 && ahead && MoveModules.heading(1) != null || open && dist > 1.9 && opened.containsKey(p)) {
                    useBlock(p, -1);
                    if (open) opened.remove(p);
                    else opened.put(p, now);
                    return;
                }
            }
        }

        @Override public void onWorldLeft() { opened.clear(); }
    }

    public static List<Module> all() {
        return List.of(new Nuker(), new VeinMiner(), new AutoTunnel(), new LiquidFill(), new SpawnProof(), new AutoTorch(), new ChestStealer(), new AutoFarm(), new AutoBreed(), new AutoShear(), new AutoNametag(), new Flamethrower(), new AutoSign(), new AirPlace(), new Timer(), new StashFinder(), new BoneMealer(), new AutoDoor());
    }
}
