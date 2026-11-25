package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HoleFill extends Module {
    private final Setting.Num near = num("Enemy distance", 3, 1, 6, .5), perTick = num("Blocks per tick", 1, 1, 4, 1);

    public HoleFill() { super(Category.COMBAT, "HoleFill", "Fills safe holes near enemies before they reach them."); }

    @Override public void onTick() {
        int slot = Obsidian.slot(), left = perTick.getInt();
        var enemies = Targets.near(12, true, false);
        if (slot < 0 || enemies.isEmpty()) return;
        BlockPos me = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-5, -4, -5), me.offset(5, 3, 5))) {
            if (p.equals(me) || !Placement.inReach(p) || !Holes.isHole(p, Obsidian::blastProof, Obsidian::open)) continue;
            Vec3 c = Vec3.atCenterOf(p);
            boolean wanted = false;
            for (LivingEntity e : enemies) wanted |= e.distanceToSqr(c) <= near.get() * near.get();
            if (wanted && Placement.place(Priority.HOLE_FILL, p.immutable(), slot) && --left <= 0) return;
        }
    }
}
