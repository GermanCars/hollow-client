package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class KillAura extends Module {
    private final Setting.Num range = num("Range", 3, 1, 6, .1);
    private final Setting.Bool players = bool("Players", true), mobs = bool("Mobs", false), walls = bool("Through walls", false), weapon = bool("Only with weapon", false);
    private String target = "";

    public KillAura() { super(Category.COMBAT, "KillAura", "Attacks whoever is in reach as soon as your weapon is ready."); }

    @Override public void onTick() {
        target = "";
        if (mc.player.isUsingItem()) return;
        if (weapon.get() && !mc.player.getMainHandItem().has(net.minecraft.core.component.DataComponents.WEAPON)) return;
        for (LivingEntity e : Targets.near(range.get() + 4, players.get(), mobs.get())) {
            if (Placement.distanceTo(e.getBoundingBox()) > range.get() || !walls.get() && !mc.player.hasLineOfSight(e)) continue;
            target = e.getName().getString();
            if (mc.player.getAttackStrengthScale(.5f) < 1) return;
            Actions.submit(new Request(Priority.KILL_AURA, Placement.look(e.getBoundingBox().getCenter()), false, -1, () -> attack(e)));
            return;
        }
    }

    static void attack(Entity e) {
        mc.gameMode.attack(mc.player, e);
        mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
        mc.player.connection.send(ServerboundPunchPacket.INSTANCE);
    }

    @Override public String info() { return target; }
}
