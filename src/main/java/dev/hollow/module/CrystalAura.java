package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CrystalAura extends Module {
    private final Setting.Num placeRange = num("Place range", 4.5, 1, 6, .1), breakRange = num("Break range", 3, 1, 6, .1), targetRange = num("Target range", 10, 4, 16, 1);
    private final Setting.Num minDamage = num("Minimum damage", 6, 0, 20, .5), selfCap = num("Self damage cap", 6, 0, 20, .5);
    private final Setting.Num placeDelay = num("Place delay", 1, 0, 10, 1), breakDelay = num("Break delay", 1, 0, 10, 1);
    private final Setting.Bool autoSwitch = bool("Auto switch", true), mobs = bool("Mobs", false);
    private int placeWait, breakWait;
    private String target = "";

    public CrystalAura() { super(Category.COMBAT, "CrystalAura", "Places and breaks end crystals where they hurt enemies most and you least."); }

    /** Best damage to any enemy from a blast here, or -1 when it isn't worth it or would hurt us too much. */
    private float score(Vec3 at, List<LivingEntity> enemies) {
        float self = Damage.explosion(at, Damage.CRYSTAL, mc.player, null);
        if (self > selfCap.get() || self >= Damage.health(mc.player) - .5f) return -1;
        float best = -1;
        for (LivingEntity e : enemies) {
            float d = Damage.explosion(at, Damage.CRYSTAL, e, null);
            if (d > best && (d >= minDamage.get() || d >= Damage.health(e))) best = d;
        }
        return best;
    }

    @Override public void onTick() {
        placeWait--;
        breakWait--;
        List<LivingEntity> enemies = Targets.near(targetRange.get(), true, mobs.get());
        target = enemies.isEmpty() ? "" : enemies.getFirst().getName().getString();
        if (enemies.isEmpty()) return;
        if (breakWait <= 0) breakBest(enemies);
        if (placeWait <= 0) placeBest(enemies);
    }

    private void breakBest(List<LivingEntity> enemies) {
        EndCrystal best = null;
        float bestScore = 0;
        for (EndCrystal c : mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(8))) {
            if (Placement.distanceTo(c.getBoundingBox()) > breakRange.get()) continue;
            float s = score(c.position(), enemies);
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        if (best == null) return;
        EndCrystal hit = best;
        Actions.submit(new Request(Priority.CRYSTAL, Placement.look(hit.getBoundingBox().getCenter()), false, -1, () -> {
            KillAura.attack(hit);
            breakWait = breakDelay.getInt();
        }));
    }

    private void placeBest(List<LivingEntity> enemies) {
        boolean off = mc.player.getOffhandItem().is(Items.END_CRYSTAL);
        int slot = off ? -1 : Inv.hotbar(Items.END_CRYSTAL);
        if (!off && (slot < 0 || !autoSwitch.get() && slot != mc.player.getInventory().getSelectedSlot())) return;
        BlockPos best = null;
        float bestScore = 0;
        int r = (int) Math.ceil(placeRange.get()) + 1;
        BlockPos me = BlockPos.containing(mc.player.getEyePosition());
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -r, -r), me.offset(r, r, r))) {
            if (!canHold(p)) continue;
            Vec3 at = new Vec3(p.getX() + .5, p.getY() + 1, p.getZ() + .5);
            if (Placement.distanceTo(new AABB(p)) > placeRange.get()) continue;
            float s = score(at, enemies);
            if (s > bestScore) {
                bestScore = s;
                best = p.immutable();
            }
        }
        if (best == null) return;
        BlockHitResult hit = new BlockHitResult(new Vec3(best.getX() + .5, best.getY() + 1, best.getZ() + .5), Direction.UP, best, false);
        Actions.submit(new Request(Priority.CRYSTAL, Placement.look(hit.getLocation()), false, slot, () -> {
            Placement.click(off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, hit);
            placeWait = placeDelay.getInt();
        }));
    }

    static boolean canHold(BlockPos base) {
        var s = mc.level.getBlockState(base);
        if (!s.is(Blocks.OBSIDIAN) && !s.is(Blocks.BEDROCK)) return false;
        BlockPos up = base.above();
        return mc.level.isEmptyBlock(up) && mc.level.getEntities(null, new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1, up.getY() + 2, up.getZ() + 1)).isEmpty();
    }

    @Override public String info() { return target; }
}
