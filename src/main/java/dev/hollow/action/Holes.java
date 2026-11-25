package dev.hollow.action;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class Holes {
    private Holes() {}

    /** A one-block hole: blast-proof floor and walls, standing room, and a free block to drop in through. */
    public static boolean isHole(BlockPos feet, Predicate<BlockPos> blastProof, Predicate<BlockPos> free) {
        if (!free.test(feet) || !free.test(feet.above()) || !free.test(feet.above(2)) || !blastProof.test(feet.below())) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) if (!blastProof.test(feet.relative(d))) return false;
        return true;
    }
}
