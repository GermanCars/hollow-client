package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class Surround extends Module {
    private final Setting.Num perTick = num("Blocks per tick", 2, 1, 8, 1);
    private final Setting.Bool centre = bool("Centre", false), offOnJump = bool("Off when jumping", true);
    private double startY;

    public Surround() { super(Category.COMBAT, "Surround", "Walls your feet in with obsidian."); }

    @Override protected void onEnable() {
        startY = mc.player.getY();
        if (centre.get()) {
            BlockPos p = mc.player.blockPosition();
            mc.player.setPos(new Vec3(p.getX() + .5, mc.player.getY(), p.getZ() + .5));
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        }
    }

    @Override public void onTick() {
        if (offOnJump.get() && mc.player.getY() > startY + .6) {
            set(false);
            return;
        }
        if (!mc.player.onGround()) return;
        startY = mc.player.getY();
        int slot = Obsidian.slot(), left = perTick.getInt();
        // Standing on farmland, soul sand or an ender chest puts the feet slightly inside the block below.
        BlockPos feet = BlockPos.containing(mc.player.getX(), mc.player.getY() + .2, mc.player.getZ());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = feet.relative(d);
            if (left <= 0) return;
            if (!mc.level.getBlockState(p).canBeReplaced()) continue;
            if (Placement.place(Priority.SURROUND, p, slot) || Placement.support(p) == null && Placement.place(Priority.SURROUND, p.below(), slot)) left--;
        }
    }
}
