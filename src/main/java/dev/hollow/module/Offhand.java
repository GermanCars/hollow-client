package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.action.Inv;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Fills the offhand when AutoTotem doesn't need it. */
public final class Offhand extends Module {
    private final Setting.Choice item = choice("Item", "Crystal", "Golden apple");

    public Offhand() { super(Category.PLAYER, "Offhand", "Keeps crystals or golden apples in the offhand when a totem is not needed."); }

    @Override public void onTick() {
        if (Hollow.AUTO_TOTEM.wanted()) return;
        Item want = item.is("Crystal") ? Items.END_CRYSTAL : Items.ENCHANTED_GOLDEN_APPLE;
        if (!item.is("Crystal") && Inv.count(want) == 0) want = Items.GOLDEN_APPLE;
        if (mc.player.getOffhandItem().is(want) || mc.player.isUsingItem()) return;
        Item w = want;
        int slot = Inv.anywhere(s -> s.is(w));
        if (slot >= 0) Inv.swap(slot, Inv.OFFHAND);
    }

    @Override public String info() { return item.get(); }
}
