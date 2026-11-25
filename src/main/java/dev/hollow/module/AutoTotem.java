package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;

public final class AutoTotem extends Module {
    private final Setting.Choice mode = choice("Mode", "Always", "Smart");
    private final Setting.Num health = num("Smart health", 14, 1, 36, 1);

    public AutoTotem() { super(Category.COMBAT, "AutoTotem", "Keeps a totem in your offhand."); }

    /** Whether the offhand belongs to a totem right now. */
    public boolean wanted() {
        if (!on() || Inv.count(Items.TOTEM_OF_UNDYING) == 0) return false;
        if (mode.is("Always") || mc.player.isFallFlying() || mc.player.fallDistance > 10) return true;
        float worst = 0;
        for (EndCrystal c : mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(12))) worst = Math.max(worst, Damage.explosion(c.position(), Damage.CRYSTAL, mc.player, null));
        return Damage.health(mc.player) - worst <= health.get();
    }

    @Override public void onTick() {
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) || !wanted()) return;
        int slot = Inv.anywhere(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (slot >= 0) Inv.swap(slot, Inv.OFFHAND);
    }

    @Override public String info() { return Integer.toString(mc.player == null ? 0 : Inv.count(Items.TOTEM_OF_UNDYING)); }
}
