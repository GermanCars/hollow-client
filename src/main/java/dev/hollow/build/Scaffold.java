package dev.hollow.build;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;

public final class Scaffold extends Module {
    private final Setting.Bool safe = bool("Don't fall off", true), tower = bool("Climb while jumping", true);

    /** Stay on the edge while there's something to place. */
    public boolean safe() { return on() && safe.get(); }

    public Scaffold() { super(Category.WORLD, "Scaffold", "Places blocks under your feet."); }

    private static boolean fullBlock(ItemStack s) {
        return s.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !(b.getBlock() instanceof net.minecraft.world.level.block.FallingBlock);
    }

    @Override public void onTick() {
        BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - .5, mc.player.getZ());
        if (!mc.level.getBlockState(below).canBeReplaced()) return;
        int slot = Inv.hotbar(Scaffold::fullBlock);
        if (Placement.place(Priority.SCAFFOLD, below, slot)) {
            if (tower.get() && mc.options.keyJump.isDown() && mc.player.input.getMoveVector().lengthSquared() == 0) mc.player.setDeltaMovement(0, .42, 0);
            return;
        }
        // Moving diagonally leaves nothing to build against, so bridge through a side first.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = below.relative(d);
            if (Placement.support(side) != null && Placement.place(Priority.SCAFFOLD, side, slot)) return;
        }
    }
}
