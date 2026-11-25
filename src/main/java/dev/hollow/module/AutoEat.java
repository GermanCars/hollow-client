package dev.hollow.module;

import dev.hollow.action.Actions;
import dev.hollow.action.Damage;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoEat extends Module {
    private final Setting.Num hunger = num("Hunger", 14, 1, 19, 1), health = num("Golden apple health", 10, 0, 20, 1);
    private boolean eating;
    private int before;

    public AutoEat() { super(Category.PLAYER, "AutoEat", "Eats when you are hungry or hurt."); }

    @Override public void onTick() {
        if (eating) {
            if (mc.player.isUsingItem() && mc.gui.screen() == null) return;
            stop();
            return;
        }
        if (mc.gui.screen() != null || mc.player.isUsingItem() || Actions.busy()) return;
        boolean hurt = Damage.health(mc.player) <= health.get(), hungry = mc.player.getFoodData().getFoodLevel() <= hunger.get();
        if (!hurt && !hungry) return;
        int best = -1, bestValue = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            int v = value(s, hurt, hungry);
            if (v > bestValue) {
                bestValue = v;
                best = i;
            }
        }
        if (best < 0) return;
        before = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(best);
        eating = true;
        mc.options.keyUse.setDown(true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    private static int value(ItemStack s, boolean hurt, boolean hungry) {
        boolean golden = s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE);
        if (golden) return hurt ? 100 : 0;
        var food = s.get(DataComponents.FOOD);
        boolean risky = s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE) || s.is(Items.POISONOUS_POTATO) || s.is(Items.PUFFERFISH) || s.is(Items.CHORUS_FRUIT) || s.is(Items.SUSPICIOUS_STEW);
        return food == null || risky || !hungry ? 0 : 1 + food.nutrition();
    }

    private void stop() {
        eating = false;
        mc.options.keyUse.setDown(false);
        if (mc.player != null) mc.player.getInventory().setSelectedSlot(before);
    }

    @Override protected void onDisable() { if (eating) stop(); }

    @Override public void onWorldLeft() {
        eating = false;
        mc.options.keyUse.setDown(false);
    }
}
