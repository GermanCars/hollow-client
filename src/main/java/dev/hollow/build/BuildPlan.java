package dev.hollow.build;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/** What to build next, and whether a block that would be placed is the one wanted. */
public final class BuildPlan {
    /** Set by how a block is placed. Everything else (fence arms, power, stair corners) follows from its neighbours. */
    private static final Set<Property<?>> PLACED = Set.of(
        BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.FACING_HOPPER, BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_AXIS,
        BlockStateProperties.HALF, BlockStateProperties.SLAB_TYPE, BlockStateProperties.ATTACH_FACE, BlockStateProperties.ROTATION_16, BlockStateProperties.ORIENTATION,
        BlockStateProperties.HANGING, BlockStateProperties.DOOR_HINGE, BlockStateProperties.BELL_ATTACHMENT, BlockStateProperties.VERTICAL_DIRECTION);

    private BuildPlan() {}

    public static boolean sameOrientation(BlockState placed, BlockState wanted) {
        if (placed.getBlock() != wanted.getBlock()) return false;
        for (Property<?> p : PLACED) if (wanted.hasProperty(p) && !wanted.getValue(p).equals(placed.getValue(p))) return false;
        return true;
    }

    /** The second half of doors and beds appears by itself. */
    public static boolean placedByOtherHalf(BlockState s) {
        return s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            || s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
    }

    public static boolean doubleSlab(BlockState s) { return s.hasProperty(BlockStateProperties.SLAB_TYPE) && s.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE; }

    /** Lowest first, then nearest. */
    public static List<BlockPos> order(List<BlockPos> todo, Vec3 from) {
        return todo.stream().sorted(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(p -> p.distToCenterSqr(from))).toList();
    }
}
