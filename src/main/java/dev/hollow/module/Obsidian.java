package dev.hollow.module;

import dev.hollow.action.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What counts as blast-proof, shared by the defensive modules. */
public final class Obsidian {
    private Obsidian() {}

    public static int slot() { return Inv.hotbar(Obsidian::is); }

    private static boolean is(ItemStack s) { return s.is(Items.OBSIDIAN) || s.is(Items.CRYING_OBSIDIAN) || s.is(Items.NETHERITE_BLOCK) || s.is(Items.ENDER_CHEST); }

    public static boolean blastProof(BlockPos p) { return Minecraft.getInstance().level.getBlockState(p).getBlock().getExplosionResistance() >= 600; }

    public static boolean open(BlockPos p) {
        var level = Minecraft.getInstance().level;
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }
}
