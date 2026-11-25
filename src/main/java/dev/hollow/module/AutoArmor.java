package dev.hollow.module;

import dev.hollow.action.Inv;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class AutoArmor extends Module {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final Setting.Num delay = num("Delay", 3, 1, 20, 1);
    private final Setting.Bool keepElytra = bool("Keep elytra", true);
    private int wait;

    public AutoArmor() { super(Category.PLAYER, "AutoArmor", "Wears the best armor you are carrying."); }

    private double score(ItemStack s, EquipmentSlot slot) {
        var eq = s.get(DataComponents.EQUIPPABLE);
        if (s.isEmpty() || eq == null || eq.slot() != slot) return -1;
        ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double v = mods.compute(Attributes.ARMOR, 0, slot) * 10 + mods.compute(Attributes.ARMOR_TOUGHNESS, 0, slot) * 5;
        var enchants = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        v += EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.PROTECTION), s) * 4;
        v += EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.BLAST_PROTECTION), s) * 4;
        return v;
    }

    private boolean cursed(ItemStack s) {
        return EnchantmentHelper.getItemEnchantmentLevel(mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), s) > 0;
    }

    @Override public void onTick() {
        if (wait-- > 0 || !Inv.canClick()) return;
        for (int i = 0; i < SLOTS.length; i++) {
            EquipmentSlot slot = SLOTS[i];
            ItemStack worn = mc.player.getItemBySlot(slot);
            if (cursed(worn) || keepElytra.get() && worn.has(DataComponents.GLIDER)) continue;
            double best = Math.max(0, score(worn, slot));
            int from = -1;
            for (int s = 0; s < 36; s++) {
                ItemStack c = mc.player.getInventory().getItem(s);
                double v = cursed(c) ? -1 : score(c, slot);
                if (v > best) {
                    best = v;
                    from = s;
                }
            }
            if (from < 0) continue;
            // Taking off and putting on happen on separate turns, so a full inventory can't lose anything.
            if (worn.isEmpty()) Inv.shiftClickInv(from);
            else if (mc.player.getInventory().getFreeSlot() >= 0) Inv.shiftClick(InventoryMenu.ARMOR_SLOT_START + i);
            else continue;
            wait = delay.getInt();
            return;
        }
    }
}
