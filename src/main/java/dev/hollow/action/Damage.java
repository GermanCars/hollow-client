package dev.hollow.action;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Explosion damage as the server works it out, from what the client can see. */
public final class Damage {
    private static final Minecraft mc = Minecraft.getInstance();
    public static final float CRYSTAL = 6, ANCHOR = 5;

    private Damage() {}

    /** @param gone a block the explosion removes first (a charged anchor), or null */
    public static float explosion(Vec3 centre, float power, LivingEntity e, BlockPos gone) {
        float reach = power * 2;
        double dist = Math.sqrt(e.distanceToSqr(centre)) / reach;
        if (dist > 1) return 0;
        double impact = (1 - dist) * exposure(centre, e, gone);
        float damage = (float) ((impact * impact + impact) / 2 * 7 * reach + 1);
        if (e instanceof Player) {
            Difficulty d = mc.level.getDifficulty();
            if (d == Difficulty.PEACEFUL) return 0;
            if (d == Difficulty.EASY) damage = Math.min(damage / 2 + 1, damage);
            if (d == Difficulty.HARD) damage *= 1.5f;
        }
        damage = CombatRules.getDamageAfterAbsorb(e, damage, mc.level.damageSources().explosion(null, null), e.getArmorValue(), (float) e.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (e.hasEffect(MobEffects.RESISTANCE)) damage = Math.max(0, damage * (25 - (e.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5) / 25f);
        return CombatRules.getDamageAfterMagicAbsorb(damage, protection(e));
    }

    private static int protection(LivingEntity e) {
        var enchants = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var plain = enchants.getOrThrow(Enchantments.PROTECTION);
        var blast = enchants.getOrThrow(Enchantments.BLAST_PROTECTION);
        int total = 0;
        for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = e.getItemBySlot(s);
            total += EnchantmentHelper.getItemEnchantmentLevel(plain, stack) + 2 * EnchantmentHelper.getItemEnchantmentLevel(blast, stack);
        }
        return total;
    }

    /** Same sample points as ServerExplosion.getSeenPercent, but can see through one block. */
    private static float exposure(Vec3 centre, LivingEntity e, BlockPos gone) {
        AABB b = e.getBoundingBox();
        double sx = 1 / (b.getXsize() * 2 + 1), sy = 1 / (b.getYsize() * 2 + 1), sz = 1 / (b.getZsize() * 2 + 1);
        double ox = (1 - Math.floor(1 / sx) * sx) / 2, oz = (1 - Math.floor(1 / sz) * sz) / 2;
        int hits = 0, total = 0;
        for (double x = 0; x <= 1; x += sx) for (double y = 0; y <= 1; y += sy) for (double z = 0; z <= 1; z += sz) {
            Vec3 from = new Vec3(Mth.lerp(x, b.minX, b.maxX) + ox, Mth.lerp(y, b.minY, b.maxY), Mth.lerp(z, b.minZ, b.maxZ) + oz);
            BlockHitResult hit = mc.level.clip(new ClipContext(from, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(gone)) hits++;
            total++;
        }
        return total == 0 ? 0 : (float) hits / total;
    }

    public static float health(LivingEntity e) { return e.getHealth() + e.getAbsorptionAmount(); }
}
