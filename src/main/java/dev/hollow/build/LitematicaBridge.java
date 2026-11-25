package dev.hollow.build;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The only class that touches Litematica. Call available() before anything else here. */
public final class LitematicaBridge {
    private LitematicaBridge() {}

    public static boolean available() { return FabricLoader.getInstance().isModLoaded("litematica"); }

    public static boolean hasPlacement() {
        return SchematicWorldHandler.getSchematicWorld() != null && DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().stream().anyMatch(p -> p.isEnabled());
    }

    /** The block the schematic wants here, or null outside the shown layers. */
    public static BlockState wanted(BlockPos pos) {
        var world = SchematicWorldHandler.getSchematicWorld();
        return world == null || !DataManager.getRenderLayerRange().isPositionWithinRange(pos) ? null : world.getBlockState(pos);
    }
}
