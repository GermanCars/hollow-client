package dev.hollow.module;

import dev.hollow.action.Holes;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

public final class HoleEsp extends Module {
    private final Setting.Num range = num("Range", 8, 4, 16, 1);
    private final List<BlockPos> found = new ArrayList<>();
    private int wait;

    public HoleEsp() { super(Category.RENDER, "HoleESP", "Marks safe holes near you."); }

    @Override public void onTick() {
        if (wait-- > 0) return;
        wait = 4;
        found.clear();
        int r = range.getInt();
        BlockPos me = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -5, -r), me.offset(r, 4, r))) if (Holes.isHole(p, Obsidian::blastProof, Obsidian::open)) found.add(p.immutable());
    }

    @Override public void onWorldRender(float partial) {
        for (BlockPos p : found) Gizmos.cuboid(new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + .15, p.getZ() + 1), GizmoStyle.strokeAndFill(0xE07CE7FF, 2, 0x407CE7FF)).setAlwaysOnTop();
    }

    @Override protected void onDisable() { found.clear(); }

    @Override public void onWorldLeft() { found.clear(); }
}
