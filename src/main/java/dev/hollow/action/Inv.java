package dev.hollow.action;

import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Finding and moving items. Slots are inventory indices: 0-8 hotbar, 9-35 main. */
public final class Inv {
    private static final Minecraft mc = Minecraft.getInstance();
    public static final int OFFHAND = 40;

    private Inv() {}

    /** Prefers the held slot. -1 when missing. */
    public static int hotbar(Predicate<ItemStack> want) {
        int held = mc.player.getInventory().getSelectedSlot();
        if (want.test(mc.player.getInventory().getItem(held))) return held;
        for (int i = 0; i < 9; i++) if (want.test(mc.player.getInventory().getItem(i))) return i;
        return -1;
    }

    public static int hotbar(Item item) { return hotbar(s -> s.is(item)); }

    /** Anywhere in hotbar or main inventory. Main first, so the hotbar stays stocked. */
    public static int anywhere(Predicate<ItemStack> want) {
        for (int i = 9; i < 36; i++) if (want.test(mc.player.getInventory().getItem(i))) return i;
        return hotbar(want);
    }

    public static int count(Item item) {
        int n = mc.player.getOffhandItem().is(item) ? mc.player.getOffhandItem().getCount() : 0;
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getItem(i).is(item)) n += mc.player.getInventory().getItem(i).getCount();
        return n;
    }

    /** True when inventory clicks are safe: nothing open and nothing on the cursor. */
    public static boolean canClick() {
        return !Actions.busy() && mc.player.containerMenu instanceof InventoryMenu m && m.getCarried().isEmpty();
    }

    private static int menuSlot(int inv) { return inv < 9 ? InventoryMenu.USE_ROW_SLOT_START + inv : inv; }

    /** Swaps an inventory slot with a hotbar slot (0-8) or the offhand (40) in one click. */
    public static void swap(int inv, int button) { click(menuSlot(inv), button, ContainerInput.SWAP); }

    /** Swaps a slot of the inventory screen with a hotbar slot. */
    public static void swapMenu(int menuSlot, int button) { click(menuSlot, button, ContainerInput.SWAP); }

    /** Throws a whole stack. */
    public static void drop(int inv) { click(menuSlot(inv), 1, ContainerInput.THROW); }

    public static void shiftClick(int menuSlot) { click(menuSlot, 0, ContainerInput.QUICK_MOVE); }

    public static void shiftClickInv(int inv) { shiftClick(menuSlot(inv)); }

    private static void click(int menuSlot, int button, ContainerInput type) {
        if (canClick()) mc.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, menuSlot, button, type, mc.player);
    }
}
