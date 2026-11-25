package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class AutoTrap extends Module {
    private final Setting.Num range = num("Range", 5, 2, 7, .5), perTick = num("Blocks per tick", 2, 1, 6, 1);

    public AutoTrap() { super(Category.COMBAT, "AutoTrap", "Boxes the nearest enemy in with obsidian."); }

    @Override public void onTick() {
        int slot = Obsidian.slot(), left = perTick.getInt();
        var enemies = Targets.near(range.get(), true, false);
        if (slot < 0 || enemies.isEmpty()) return;
        BlockPos feet = enemies.getFirst().blockPosition();
        List<BlockPos> cage = new ArrayList<>();
        for (int y = 0; y < 2; y++) for (Direction d : Direction.Plane.HORIZONTAL) cage.add(feet.above(y).relative(d));
        // The roof needs something beside it to hang from.
        cage.add(feet.above(2).relative(Direction.NORTH));
        cage.add(feet.above(2));
        for (BlockPos p : cage) {
            if (left <= 0) return;
            if (mc.level.getBlockState(p).canBeReplaced() && Placement.inReach(p) && Placement.place(Priority.TRAP, p, slot)) left--;
        }
    }
}
