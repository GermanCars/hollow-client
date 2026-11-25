package dev.hollow.module;

import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Places, charges and sets off respawn anchors, one step at a time. Does nothing where anchors work as beds. */
public final class AnchorAura extends Module {
    private final Setting.Num targetRange = num("Target range", 8, 4, 12, 1), minDamage = num("Minimum damage", 7, 0, 20, .5), selfCap = num("Self damage cap", 6, 0, 20, .5), delay = num("Delay", 2, 0, 10, 1);
    private final Setting.Bool mobs = bool("Mobs", false);
    private int wait;

    public AnchorAura() { super(Category.COMBAT, "AnchorAura", "Places, charges and sets off respawn anchors near enemies."); }

    private float score(BlockPos pos, List<LivingEntity> enemies) {
        Vec3 at = Vec3.atCenterOf(pos);
        float self = Damage.explosion(at, Damage.ANCHOR, mc.player, pos);
        if (self > selfCap.get() || self >= Damage.health(mc.player) - .5f) return -1;
        float best = -1;
        for (LivingEntity e : enemies) {
            float d = Damage.explosion(at, Damage.ANCHOR, e, pos);
            if (d > best && (d >= minDamage.get() || d >= Damage.health(e))) best = d;
        }
        return best;
    }

    @Override public void onTick() {
        if (wait-- > 0 || mc.level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, mc.player.blockPosition())) return;
        List<LivingEntity> enemies = Targets.near(targetRange.get(), true, mobs.get());
        int glow = Inv.hotbar(Items.GLOWSTONE), anchor = Inv.hotbar(Items.RESPAWN_ANCHOR);
        if (enemies.isEmpty() || glow < 0) return;
        BlockPos best = null, bestAnchor = null;
        float score = 0, anchorScore = 0;
        BlockPos me = BlockPos.containing(mc.player.getEyePosition());
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-5, -5, -5), me.offset(5, 5, 5))) {
            if (!Placement.inReach(p)) continue;
            BlockState s = mc.level.getBlockState(p);
            boolean is = s.is(Blocks.RESPAWN_ANCHOR);
            if (!is && (anchor < 0 || !s.canBeReplaced() || enemies.getFirst().distanceToSqr(Vec3.atCenterOf(p)) > 25)) continue;
            float v = score(p, enemies);
            if (is && v > anchorScore) {
                anchorScore = v;
                bestAnchor = p.immutable();
            } else if (!is && v > score && Placement.support(p) != null && Placement.free(p, Blocks.RESPAWN_ANCHOR.defaultBlockState())) {
                score = v;
                best = p.immutable();
            }
        }
        if (bestAnchor != null) {
            boolean charged = mc.level.getBlockState(bestAnchor).getValue(RespawnAnchorBlock.CHARGE) > 0;
            int slot = charged ? notGlowstone() : glow;
            Vec3 eye = mc.player.getEyePosition(), c = Vec3.atCenterOf(bestAnchor);
            Direction face = Direction.getApproximateNearest(eye.subtract(c));
            BlockHitResult hit = new BlockHitResult(c.add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5), face, bestAnchor, false);
            Actions.submit(new Request(Priority.ANCHOR, Placement.look(hit.getLocation()), false, slot, () -> {
                if (!mc.player.isShiftKeyDown()) Placement.click(InteractionHand.MAIN_HAND, hit);
                wait = delay.getInt();
            }));
        } else if (best != null && Placement.place(Priority.ANCHOR, best, anchor)) wait = delay.getInt();
    }

    /** Anything but glowstone, so the click sets the anchor off instead of charging it. */
    private static int notGlowstone() {
        return Inv.hotbar(s -> !s.is(Items.GLOWSTONE));
    }
}
