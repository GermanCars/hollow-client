package dev.hollow.build;

import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Places the Litematica schematic's blocks in reach. Wrong blocks are skipped, never broken. */
public final class Printer extends Module {
    private static final float[] PITCHES = {0, 89, -89, 35, -35};
    /** Where on the clicked face to aim: middle, then the halves that decide slabs, stairs and hinges. */
    private static final double[][] AIM = {{0, 0, 0}, {0, .3, 0}, {0, -.3, 0}, {.3, 0, 0}, {-.3, 0, 0}, {0, 0, .3}, {0, 0, -.3}};
    private final Setting.Num perTick = num("Blocks per tick", 1, 1, 4, 1), delay = num("Delay", 2, 0, 20, 1);
    private final Setting.Bool refill = bool("Refill hotbar", true);
    private final Map<BlockPos, Long> tried = new HashMap<>();
    /**
     * Blocks that face the way you look (pistons, observers) go by your head on the server, which follows your view a tick late.
     * So the view is held for a moment before they are placed.
     */
    private record Aim(BlockPos pos, Plan plan, BlockState want, int ticks) {}

    private static final int HOLD = 3;
    private Aim aim;
    private int wait;
    private Item missing;
    private long lastPlaced;

    public Printer() { super(Category.WORLD, "Printer", "Places the blocks of your Litematica schematic as you walk near them."); }

    @Override protected void onEnable() {
        aim = null;
        tried.clear();
        missing = null;
        if (!LitematicaBridge.available()) stop("Printer needs Litematica installed");
        else if (!LitematicaBridge.hasPlacement()) stop("Load a schematic in Litematica first");
    }

    @Override public void onWorldLeft() {
        aim = null;
        tried.clear();
        missing = null;
    }

    private void stop(String why) {
        set(false);
        Hollow.tell(why);
    }

    public Item missing() { return missing; }

    public long lastPlaced() { return lastPlaced; }

    /** Still to be placed: the schematic wants a block and the world has room for it. */
    public static boolean unfinished(BlockPos p) {
        BlockState want = LitematicaBridge.wanted(p);
        if (want == null || want.isAir() || BuildPlan.placedByOtherHalf(want) || want.getBlock().asItem() == Items.AIR) return false;
        BlockState have = mc.level.getBlockState(p);
        if (have.getBlock() == want.getBlock()) return BuildPlan.doubleSlab(want) && !BuildPlan.doubleSlab(have);
        return have.canBeReplaced();
    }

    @Override public void onTick() {
        if (!LitematicaBridge.available() || !LitematicaBridge.hasPlacement()) {
            stop("Printer stopped: no schematic is loaded");
            return;
        }
        if (Actions.busy() || mc.player.isUsingItem()) return;
        long now = mc.level.getGameTime();
        if (aim != null) {
            hold(now);
            return;
        }
        if (wait-- > 0) return;
        List<BlockPos> todo = new ArrayList<>();
        BlockPos me = BlockPos.containing(mc.player.getEyePosition());
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-6, -6, -6), me.offset(6, 6, 6))) {
            if (Placement.inReach(p) && unfinished(p) && now - tried.getOrDefault(p, -100L) > 20) todo.add(p.immutable());
        }
        missing = null;
        int left = perTick.getInt();
        List<BlockPos> order = BuildPlan.order(todo, mc.player.position());
        for (BlockPos p : order) {
            if (left <= 0) break;
            if (print(p, now)) left--;
        }
        if (left < perTick.getInt()) wait = delay.getInt();
        else fetch(order);
    }

    private boolean print(BlockPos pos, long now) {
        BlockState want = LitematicaBridge.wanted(pos);
        Item item = want.getBlock().asItem();
        int slot = Inv.hotbar(item);
        if (slot < 0) return false;
        BlockState aim = want;
        if (BuildPlan.doubleSlab(want)) {
            BlockState have = mc.level.getBlockState(pos);
            if (have.getBlock() == want.getBlock()) return addSlab(pos, have, slot, now);
            aim = want.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        }
        if (!Placement.free(pos, aim)) return false;
        Plan plan = plan(pos, aim, mc.player.getInventory().getItem(slot));
        if (plan == null) return false;
        if (aim.hasProperty(BlockStateProperties.FACING)) {
            this.aim = new Aim(pos, plan, aim, 0);
            hold(now);
        } else submit(pos, plan.hit, plan.rot, plan.exact, slot, now);
        return true;
    }

    private void hold(long now) {
        Aim a = aim;
        int slot = Inv.hotbar(a.want.getBlock().asItem());
        if (slot < 0 || !Placement.inReach(a.pos) || !unfinished(a.pos)) {
            aim = null;
            return;
        }
        if (a.ticks < HOLD) {
            aim = new Aim(a.pos, a.plan, a.want, a.ticks + 1);
            Actions.submit(new Request(Priority.PRINTER, a.plan.rot, true, -1, () -> {}));
        } else {
            aim = null;
            submit(a.pos, a.plan.hit, a.plan.rot, true, slot, now);
            wait = delay.getInt();
        }
    }

    /** Nothing in the hotbar could be placed, so bring one more kind of block down. One a tick, or they would push each other out. */
    private void fetch(List<BlockPos> order) {
        for (BlockPos p : order) {
            Item item = LitematicaBridge.wanted(p).getBlock().asItem();
            if (Inv.hotbar(item) >= 0) continue;
            int from = refill.get() && Inv.canClick() ? Inv.anywhere(s -> s.is(item)) : -1;
            if (from < 0) {
                missing = item;
                continue;
            }
            Inv.swap(from, spareSlot());
            missing = null;
            wait = 1;
            return;
        }
    }

    private int nextSpare;

    private boolean addSlab(BlockPos pos, BlockState have, int slot, long now) {
        boolean bottom = have.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), bottom ? Direction.UP : Direction.DOWN, pos, false);
        submit(pos, hit, Placement.look(hit.getLocation()), false, slot, now);
        return true;
    }

    private void submit(BlockPos pos, BlockHitResult hit, Rot rot, boolean exact, int slot, long now) {
        tried.put(pos, now);
        if (tried.size() > 4000) tried.values().removeIf(t -> now - t > 40);
        Actions.submit(new Request(Priority.PRINTER, rot, exact, slot, () -> {
            if (Placement.use(InteractionHand.MAIN_HAND, hit)) lastPlaced = now;
        }));
    }

    /** The hotbar slot refills go into: an empty one, else the last block slot, so tools and food stay put. */
    private int spareSlot() {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        for (int n = 0; n < 9; n++) {
            int i = nextSpare = (nextSpare + 8) % 9;
            if (mc.player.getInventory().getItem(i).getItem() instanceof net.minecraft.world.item.BlockItem) return i;
        }
        return 8;
    }

    private record Plan(BlockHitResult hit, Rot rot, boolean exact) {}

    /** Asks the block itself what it would become for each way of placing it, and takes the first that matches. */
    private static Plan plan(BlockPos pos, BlockState want, ItemStack stack) {
        LocalPlayer p = mc.player;
        float yaw = p.getYRot(), pitch = p.getXRot();
        try {
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d);
                BlockState s = mc.level.getBlockState(n);
                if (s.canBeReplaced() || s.getCollisionShape(mc.level, n).isEmpty() || !Placement.inReach(n)) continue;
                for (double[] a : AIM) {
                    if (a[0] * d.getStepX() + a[1] * d.getStepY() + a[2] * d.getStepZ() != 0) continue;
                    Vec3 at = Vec3.atCenterOf(pos).add(d.getStepX() * .5 + a[0], d.getStepY() * .5 + a[1], d.getStepZ() * .5 + a[2]);
                    BlockHitResult hit = new BlockHitResult(at, d.getOpposite(), n, false);
                    Rot look = Placement.look(at);
                    if (steady(want, pos, stack, hit, look)) return new Plan(hit, look, true);
                    for (int q = 0; q < 16; q++) for (float pt : PITCHES) {
                        // Sixteenths only matter for signs, banners and heads; quarters come first.
                        Rot r = new Rot(q < 4 ? q * 90 : q * 22.5f, pt);
                        if (steady(want, pos, stack, hit, r)) return new Plan(hit, r, true);
                        if (q >= 4 && !want.hasProperty(BlockStateProperties.ROTATION_16)) break;
                    }
                }
            }
            return null;
        } finally {
            p.setYRot(yaw);
            p.setXRot(pitch);
        }
    }

    /** Still right if the server sees a slightly different angle, so never balanced between two directions. */
    private static boolean steady(BlockState want, BlockPos pos, ItemStack stack, BlockHitResult hit, Rot r) {
        return gives(want, pos, stack, hit, r) && gives(want, pos, stack, hit, new Rot(r.yaw() + 2, r.pitch())) && gives(want, pos, stack, hit, new Rot(r.yaw() - 2, r.pitch()))
            && gives(want, pos, stack, hit, new Rot(r.yaw(), Math.min(90, r.pitch() + 2))) && gives(want, pos, stack, hit, new Rot(r.yaw(), Math.max(-90, r.pitch() - 2)));
    }

    private static boolean gives(BlockState want, BlockPos pos, ItemStack stack, BlockHitResult hit, Rot look) {
        mc.player.setYRot(look.yaw());
        mc.player.setXRot(look.pitch());
        BlockPlaceContext ctx = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, stack, hit);
        if (!ctx.canPlace() || !ctx.getClickedPos().equals(pos)) return false;
        // Items such as torches choose between a floor and a wall block, so ask the item.
        BlockState got = stack.getItem() instanceof net.minecraft.world.item.BlockItem item ? ((dev.hollow.mixin.BlockItemAccess) item).hollow$placementState(ctx) : null;
        return got != null && BuildPlan.sameOrientation(got, want);
    }

    @Override public String info() { return missing == null ? "" : "needs " + missing.getName(missing.getDefaultInstance()).getString(); }
}
