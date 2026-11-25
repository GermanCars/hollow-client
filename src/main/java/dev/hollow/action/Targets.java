package dev.hollow.action;

import dev.hollow.Hollow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public final class Targets {
    private static final Minecraft mc = Minecraft.getInstance();

    private Targets() {}

    /** Living things that may be attacked, nearest first. Never friends. */
    public static List<LivingEntity> near(double range, boolean players, boolean mobs) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity l) || l == mc.player || l.isDeadOrDying() || l instanceof ArmorStand || l.distanceTo(mc.player) > range) continue;
            if (l instanceof Player p) {
                if (!players || p.isCreative() || p.isSpectator() || Hollow.FRIENDS.has(p.getScoreboardName())) continue;
            } else if (!mobs) continue;
            out.add(l);
        }
        out.sort(Comparator.comparingDouble(l -> l.distanceToSqr(mc.player)));
        return out;
    }
}
