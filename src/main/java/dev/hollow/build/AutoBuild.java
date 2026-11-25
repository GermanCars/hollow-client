package dev.hollow.build;

import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/** Walks to unfinished parts of the schematic while Printer places them. Stops when stuck or out of blocks. */
public final class AutoBuild extends Module {
    private static final int RESCAN = 20, STUCK = 120;
    private final Setting.Num range = num("Search range", 24, 8, 48, 4);
    private BlockPos goal;
    private int rescan, still;
    private Vec3 lastPos = Vec3.ZERO;

    public AutoBuild() { super(Category.WORLD, "AutoBuild", "Walks around the schematic so Printer can finish it."); }

    @Override protected void onEnable() {
        reset();
        Hollow.PRINTER.set(true);
        if (!Hollow.PRINTER.on()) set(false);
    }

    @Override protected void onDisable() { release(); }

    @Override public void onWorldLeft() {
        reset();
        release();
    }

    private void reset() {
        goal = null;
        rescan = 0;
        still = 0;
    }

    private static void release() {
        mc.options.keyUp.setDown(false);
        mc.options.keyJump.setDown(false);
    }

    private void stop(String why) {
        set(false);
        release();
        Hollow.tell(why);
    }

    @Override public void onMove() {
        if (!Hollow.PRINTER.on()) {
            stop("AutoBuild stopped with Printer");
            return;
        }
        if (mc.gui.screen() != null) {
            release();
            return;
        }
        if (rescan-- <= 0 || goal != null && !Printer.unfinished(goal)) {
            rescan = RESCAN;
            goal = find();
            if (goal == null) {
                stop("AutoBuild finished: nothing left within " + range.text() + " blocks");
                return;
            }
        }
        boolean placing = mc.level.getGameTime() - Hollow.PRINTER.lastPlaced() < 30;
        still = placing || lastPos.distanceToSqr(mc.player.position()) > .01 ? 0 : still + 1;
        lastPos = mc.player.position();
        if (still > STUCK) {
            Item need = Hollow.PRINTER.missing();
            stop(need == null ? "AutoBuild is stuck and stopped" : "AutoBuild is out of " + need.getName(need.getDefaultInstance()).getString());
            return;
        }
        walk();
    }

    /** The lowest unfinished block, nearest first. */
    private BlockPos find() {
        int r = range.getInt();
        BlockPos me = mc.player.blockPosition(), best = null;
        double bestScore = Double.MAX_VALUE;
        int low = Math.max(mc.level.getMinY(), me.getY() - r), high = Math.min(mc.level.getMaxY(), me.getY() + r);
        for (int y = low; y <= high && best == null; y++) {
            for (BlockPos p : BlockPos.betweenClosed(me.getX() - r, y, me.getZ() - r, me.getX() + r, y, me.getZ() + r)) {
                double d = p.distSqr(me);
                if (d < bestScore && Printer.unfinished(p)) {
                    bestScore = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private void walk() {
        double dx = goal.getX() + .5 - mc.player.getX(), dz = goal.getZ() + .5 - mc.player.getZ(), flat = Math.hypot(dx, dz);
        boolean on = flat < .9 && Math.abs(goal.getY() - mc.player.getY()) < 2;
        if (flat < 2.6 && !on) {
            release();
            return;
        }
        // Standing in the way of the next block: step off it.
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)) + (on ? 180 : 0);
        mc.player.setYRot(mc.player.getYRot() + net.minecraft.util.Mth.wrapDegrees(yaw - mc.player.getYRot()));
        double rad = Math.toRadians(yaw);
        if (mc.player.onGround() && drop(mc.player.getX() - Math.sin(rad) * .8, mc.player.getZ() + Math.cos(rad) * .8)) {
            release();
            return;
        }
        mc.options.keyUp.setDown(true);
        mc.options.keyJump.setDown(mc.player.horizontalCollision && mc.player.onGround());
    }

    /** More than three blocks of nothing ahead. */
    private static boolean drop(double x, double z) {
        for (int i = 1; i <= 4; i++) {
            BlockPos p = BlockPos.containing(x, mc.player.getY() - i + .1, z);
            if (!mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty()) return false;
        }
        return true;
    }
}
