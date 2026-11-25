package dev.hollow.action;

import dev.hollow.Hollow;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** The only place that changes view direction or held slot for modules. Runs around the tick's position packet. */
public final class Actions {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final List<Request> pending = new ArrayList<>();
    private static List<Request> chosen = List.of();
    private static Rot saved;

    private Actions() {}

    /** While a chest or inventory is open, nothing is clicked or held on the player's behalf. */
    public static boolean busy() { return mc.gui.screen() instanceof AbstractContainerScreen; }

    public static void submit(Request r) {
        boolean sane = r.rot() == null || Float.isFinite(r.rot().yaw()) && Float.isFinite(r.rot().pitch());
        if (!sane) Hollow.LOG.warn("{} asked to look at something that isn't a number", r.priority());
        if (sane && !busy()) pending.add(r);
    }

    public static void clear() {
        pending.clear();
        chosen = List.of();
        saved = null;
    }

    /** Turns the player so the position packet carries the wanted view. */
    public static void begin() {
        LocalPlayer p = mc.player;
        boolean rotate = Hollow.INTERFACE.rotate.get(), eating = p.isUsingItem() && p.getUsedItemHand() == InteractionHand.MAIN_HAND;
        // Switching slots would cancel eating.
        if (eating) pending.removeIf(r -> r.slot() >= 0 && r.slot() != p.getInventory().getSelectedSlot());
        chosen = Request.choose(pending, rotate);
        Rot view = Request.view(chosen, rotate);
        if (view == null) return;
        saved = new Rot(p.getYRot(), p.getXRot());
        // Stay on the same turn as the camera so the server never sees a 360 degree jump.
        float turn = net.minecraft.util.Mth.wrapDegrees(view.yaw() - saved.yaw());
        p.setYRot(saved.yaw() + turn);
        p.setXRot(Math.clamp(view.pitch(), -90, 90));
    }

    /** Runs what was chosen, then gives view and slot back. */
    public static void finish() {
        LocalPlayer p = mc.player;
        try {
            if (p == null || mc.gameMode == null || chosen.isEmpty()) return;
            int slot = p.getInventory().getSelectedSlot();
            try {
                for (Request r : chosen) {
                    p.getInventory().setSelectedSlot(r.slot() >= 0 ? r.slot() : slot);
                    r.run().run();
                }
            } finally {
                p.getInventory().setSelectedSlot(slot);
            }
        } finally {
            if (saved != null && p != null) {
                p.setYRot(saved.yaw());
                p.setXRot(saved.pitch());
            }
            clear();
        }
    }
}
