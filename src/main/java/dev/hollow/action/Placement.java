package dev.hollow.action;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Placing blocks against what is already there. */
public final class Placement {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final Set<Class<?>> USABLE = Set.of(DoorBlock.class, TrapDoorBlock.class, FenceGateBlock.class, LeverBlock.class, ButtonBlock.class, RepeaterBlock.class, ComparatorBlock.class,
        NoteBlock.class, CakeBlock.class, RespawnAnchorBlock.class, DaylightDetectorBlock.class, BedBlock.class, JukeboxBlock.class, ComposterBlock.class);

    private Placement() {}

    public static boolean inReach(BlockPos pos) { return mc.player.isWithinBlockInteractionRange(pos, 0); }

    /** Empty enough for this block: replaceable and no entity in the way. */
    public static boolean free(BlockPos pos, BlockState placing) {
        return mc.level.isInWorldBounds(pos) && mc.level.getBlockState(pos).canBeReplaced() && mc.level.isUnobstructed(placing, pos, CollisionContext.empty());
    }

    /** Right-clicking it does something, so it has to be sneaked on. */
    public static boolean usable(BlockPos pos) {
        BlockState s = mc.level.getBlockState(pos);
        if (s.getMenuProvider(mc.level, pos) != null) return true;
        return USABLE.stream().anyMatch(c -> c.isInstance(s.getBlock()));
    }

    /** The side of a neighbouring block to click so that a block lands at pos, or null. Visible sides win. */
    public static BlockHitResult support(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        BlockHitResult best = null;
        double bestScore = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState s = mc.level.getBlockState(n);
            if (s.canBeReplaced() || s.getCollisionShape(mc.level, n).isEmpty() || !inReach(n)) continue;
            Direction face = d.getOpposite();
            Vec3 hit = Vec3.atCenterOf(pos).add(d.getStepX() * .5, d.getStepY() * .5, d.getStepZ() * .5), to = eye.subtract(hit);
            boolean visible = to.x * face.getStepX() + to.y * face.getStepY() + to.z * face.getStepZ() > 0;
            double score = to.lengthSqr() + (visible ? 0 : 1000) + (usable(n) ? 500 : 0);
            if (score < bestScore) {
                bestScore = score;
                best = new BlockHitResult(hit, face, n, false);
            }
        }
        return best;
    }

    public static Rot look(Vec3 at) {
        Vec3 e = mc.player.getEyePosition();
        return Rot.between(e.x, e.y, e.z, at.x, at.y, at.z);
    }

    /** Queues a block; false when it can't go there. */
    public static boolean place(Priority priority, BlockPos pos, int slot) {
        if (slot < 0) return false;
        Block block = Block.byItem(mc.player.getInventory().getItem(slot).getItem());
        if (block == Blocks.AIR || !free(pos, block.defaultBlockState())) return false;
        BlockHitResult hit = support(pos);
        if (hit == null) return false;
        Actions.submit(new Request(priority, look(hit.getLocation()), false, slot, () -> use(InteractionHand.MAIN_HAND, hit)));
        return true;
    }

    /** Right-clicks a block face, sneaking if the block would otherwise react. */
    public static boolean use(InteractionHand hand, BlockHitResult hit) {
        LocalPlayer p = mc.player;
        boolean sneak = usable(hit.getBlockPos()) && !p.isShiftKeyDown();
        Input before = p.input.keyPresses;
        if (sneak) sneak(p, new Input(before.forward(), before.backward(), before.left(), before.right(), before.jump(), true, before.sprint()));
        try {
            return click(hand, hit);
        } finally {
            if (sneak) sneak(p, before);
        }
    }

    private static void sneak(LocalPlayer p, Input input) {
        p.input.keyPresses = input;
        p.connection.send(new ServerboundPlayerInputPacket(input));
    }

    /** Right-clicks a block face as it is. */
    public static boolean click(InteractionHand hand, BlockHitResult hit) {
        ItemStack held = mc.player.getItemInHand(hand);
        var swing = held.getInteractAnimation();
        if (mc.gameMode.useItemOn(mc.player, hand, hit) instanceof InteractionResult.Success s) {
            if (s.swingSource() == InteractionResult.SwingSource.PREDICTED) mc.player.swing(hand, swing, false);
            return true;
        }
        return false;
    }

    public static double distanceTo(AABB box) { return Math.sqrt(box.distanceToSqr(mc.player.getEyePosition())); }
}
