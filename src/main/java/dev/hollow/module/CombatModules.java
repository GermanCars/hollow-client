package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.Notices;
import dev.hollow.hud.Stats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Combat modules small enough to share a file. */
public final class CombatModules {
    private CombatModules() {}

    public static final class Criticals extends Module {
        private final Setting.Choice mode = choice("Mode", "Packet", "Small jump");
        private boolean sending;

        public Criticals() { super(Category.COMBAT, "Criticals", "Makes every hit a critical hit."); }

        @Override public boolean onSend(Packet<?> p) {
            if (sending || !(p instanceof ServerboundAttackPacket) || mc.player == null || !mc.player.onGround() || mc.player.isInWater() || mc.player.onClimbable() || mc.player.isPassenger()) return false;
            if (mode.is("Small jump")) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, .25, mc.player.getDeltaMovement().z);
                return false;
            }
            sending = true;
            // Servers kick for two positions in one client tick, so each gets a tick of its own.
            mc.player.connection.send(ServerboundClientTickEndPacket.INSTANCE);
            for (double y : new double[] {.0625, 0}) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(mc.player.getX(), mc.player.getY() + y, mc.player.getZ(), false, false));
                mc.player.connection.send(ServerboundClientTickEndPacket.INSTANCE);
            }
            sending = false;
            return false;
        }
    }

    public static final class TriggerBot extends Module {
        private final Setting.Bool players = bool("Players", true), mobs = bool("Mobs", false);

        public TriggerBot() { super(Category.COMBAT, "TriggerBot", "Attacks whatever your crosshair is on when the weapon is ready."); }

        @Override public void onTick() {
            Entity e = mc.crosshairPickEntity;
            if (!(e instanceof LivingEntity l) || l.isDeadOrDying() || mc.gui.screen() != null || mc.player.isUsingItem() || mc.player.getAttackStrengthScale(.5f) < 1) return;
            if (l instanceof Player p ? !players.get() || Hollow.FRIENDS.has(p.getScoreboardName()) : !mobs.get()) return;
            KillAura.attack(l);
        }
    }

    public static final class AimAssist extends Module {
        private final Setting.Num range = num("Range", 5, 2, 10, .5), speed = num("Speed", 4, 1, 20, .5), fov = num("Field of view", 90, 10, 360, 10);
        private final Setting.Bool mobs = bool("Mobs", false);

        public AimAssist() { super(Category.COMBAT, "AimAssist", "Eases your aim towards the nearest enemy."); }

        @Override public void onWorldRender(float partial) {
            if (mc.gui.screen() != null) return;
            for (LivingEntity e : Targets.near(range.get(), true, mobs.get())) {
                Rot want = Placement.look(e.getPosition(partial).add(0, e.getBbHeight() * .6, 0));
                float dy = Mth.wrapDegrees(want.yaw() - mc.player.getYRot()), dp = want.pitch() - mc.player.getXRot();
                if (Math.abs(dy) > fov.get() / 2 || !mc.player.hasLineOfSight(e)) continue;
                float k = 1 - (float) Math.exp(-speed.get() * dev.hollow.ui.Frame.realDt);
                mc.player.setYRot(mc.player.getYRot() + dy * k);
                mc.player.setXRot(Mth.clamp(mc.player.getXRot() + dp * k, -90, 90));
                return;
            }
        }
    }

    public static final class AutoWeapon extends Module {
        private final Setting.Choice prefer = choice("Prefer", "Most damage", "Sword", "Axe");
        private final Setting.Bool shields = bool("Axe against shields", true);

        public AutoWeapon() { super(Category.COMBAT, "AutoWeapon", "Switches to your best weapon when you attack."); }

        private double damage(ItemStack s) {
            return s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE, 1, EquipmentSlot.MAINHAND);
        }

        public void beforeAttack(Entity e) {
            if (!(e instanceof LivingEntity l)) return;
            boolean axe = shields.get() && l.isBlocking();
            int best = -1;
            double score = damage(mc.player.getMainHandItem());
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.player.getInventory().getItem(i);
                boolean isAxe = s.is(net.minecraft.tags.ItemTags.AXES), isSword = s.is(net.minecraft.tags.ItemTags.SWORDS);
                if (!isAxe && !isSword && !s.has(DataComponents.WEAPON)) continue;
                double v = damage(s);
                if (axe && isAxe || prefer.is("Axe") && isAxe || prefer.is("Sword") && isSword) v += 100;
                if (v > score) {
                    score = v;
                    best = i;
                }
            }
            if (best >= 0) mc.player.getInventory().setSelectedSlot(best);
        }
    }

    public static final class Hitboxes extends Module {
        public final Setting.Num size = num("Extra size", .3, .05, 1.5, .05);

        public Hitboxes() { super(Category.COMBAT, "Hitboxes", "Makes players easier to hit."); }

        public boolean grows(Entity e) { return on() && e instanceof Player p && p != mc.player && !Hollow.FRIENDS.has(p.getScoreboardName()); }
    }

    public static final class SelfTrap extends Module {
        private final Setting.Num perTick = num("Blocks per tick", 2, 1, 6, 1);
        private final Setting.Choice shape = choice("Shape", "Full", "Roof only");

        public SelfTrap() { super(Category.COMBAT, "SelfTrap", "Closes obsidian around and above you."); }

        @Override public void onTick() {
            if (!mc.player.onGround()) return;
            BlockPos feet = mc.player.blockPosition();
            List<BlockPos> want = new ArrayList<>();
            if (shape.is("Full")) for (int y = 0; y < 2; y++) for (Direction d : Direction.Plane.HORIZONTAL) want.add(feet.above(y).relative(d));
            else {
                want.add(feet.north());
                want.add(feet.above().north());
            }
            want.add(feet.above(2).north());
            want.add(feet.above(2));
            int slot = Obsidian.slot(), left = perTick.getInt();
            for (BlockPos p : want) if (left > 0 && mc.level.getBlockState(p).canBeReplaced() && Placement.inReach(p) && Placement.place(Priority.SURROUND, p, slot)) left--;
        }
    }

    public static final class AutoWeb extends Module {
        private final Setting.Num range = num("Range", 4.5, 2, 6, .5);

        public AutoWeb() { super(Category.COMBAT, "AutoWeb", "Puts cobwebs on enemies."); }

        @Override public void onTick() {
            int slot = Inv.hotbar(Items.COBWEB);
            if (slot < 0) return;
            for (LivingEntity e : Targets.near(range.get(), true, false)) {
                BlockPos p = e.blockPosition();
                if (mc.level.getBlockState(p).canBeReplaced() && Placement.inReach(p) && Placement.support(p) != null) {
                    Placement.place(Priority.TRAP, p, slot);
                    return;
                }
            }
        }
    }

    public static final class AutoCity extends Module {
        private final Setting.Num range = num("Range", 5, 3, 6, .5);
        private final Setting.Bool off = bool("Switch off when done", true);
        private BlockPos target;

        public AutoCity() { super(Category.COMBAT, "AutoCity", "Mines a block of the nearest enemy's surround so crystals reach them."); }

        @Override protected void onEnable() { target = null; }

        @Override public void onTick() {
            if (target != null && mc.level.getBlockState(target).isAir()) {
                target = null;
                if (off.get()) {
                    set(false);
                    return;
                }
            }
            if (target == null || !Placement.inReach(target)) {
                target = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : Targets.near(range.get() + 2, true, false)) for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos p = e.blockPosition().relative(d);
                    var s = mc.level.getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(mc.level, p) < 0 || !Placement.inReach(p) || p.equals(mc.player.blockPosition().relative(d.getOpposite()))) continue;
                    double v = p.distToCenterSqr(mc.player.position());
                    if (v < best) {
                        best = v;
                        target = p;
                    }
                }
                if (target == null) return;
            }
            PlayerModules.AutoTool.pick(target);
            WorldModules.mine(target);
        }

        @Override public void onWorldRender(float partial) {
            if (target != null) net.minecraft.gizmos.Gizmos.cuboid(target, net.minecraft.gizmos.GizmoStyle.strokeAndFill(0xFFFF9A7A, 2, 0x30FF9A7A)).setAlwaysOnTop();
        }
    }

    public static final class AutoXp extends Module {
        private final Setting.Num below = num("Below durability %", 80, 10, 99, 1), enemies = num("Stop with enemies within", 0, 0, 16, 1), delay = num("Delay", 1, 0, 10, 1);
        private int wait;

        public AutoXp() { super(Category.COMBAT, "AutoXP", "Throws experience bottles at your feet to mend worn gear."); }

        @Override public void onTick() {
            if (wait-- > 0 || mc.player.isUsingItem()) return;
            if (enemies.get() > 0 && !Targets.near(enemies.get(), true, false).isEmpty()) return;
            boolean worn = false;
            for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack i = mc.player.getItemBySlot(s);
                if (i.isDamaged() && 100 * (1 - i.getDamageValue() / (double) i.getMaxDamage()) < below.get()) worn = true;
            }
            int slot = Inv.hotbar(Items.EXPERIENCE_BOTTLE);
            if (!worn || slot < 0) return;
            Actions.submit(new Request(Priority.SCAFFOLD, new Rot(mc.player.getYRot(), 90), true, slot, () -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                wait = delay.getInt();
            }));
        }
    }

    public static final class AutoLog extends Module {
        private final Setting.Num health = num("Below health", 6, 0, 36, 1), totems = num("Below totems", 0, 0, 10, 1);
        private final Setting.Bool strangers = bool("When a stranger appears", false), off = bool("Switch off after", true);

        public AutoLog() { super(Category.COMBAT, "AutoLog", "Leaves the server before you die."); }

        @Override public void onTick() {
            String why = null;
            if (Damage.health(mc.player) < health.get()) why = "Health was low";
            else if (Inv.count(Items.TOTEM_OF_UNDYING) < totems.get()) why = "Totems were running out";
            else if (strangers.get()) for (Player p : mc.level.players()) if (p != mc.player && !Hollow.FRIENDS.has(p.getScoreboardName())) why = p.getScoreboardName() + " appeared";
            if (why == null || mc.hasSingleplayerServer()) return;
            if (off.get()) set(false);
            mc.player.connection.getConnection().disconnect(Component.literal("Hollow Client left for you: " + why));
        }
    }

    public static final class BowRelease extends Module {
        private final Setting.Num charge = num("Charge ticks", 20, 3, 20, 1);

        public BowRelease() { super(Category.COMBAT, "BowRelease", "Lets arrows go as soon as the bow is drawn far enough."); }

        @Override public void onTick() {
            if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof BowItem && mc.player.getTicksUsingItem() >= charge.get()) mc.gameMode.releaseUsingItem(mc.player);
        }
    }

    public static final class BowAim extends Module {
        private final Setting.Num range = num("Range", 60, 10, 120, 5);
        private final Setting.Bool mobs = bool("Mobs", false);

        public BowAim() { super(Category.COMBAT, "BowAim", "Aims a drawn bow at the enemy nearest your crosshair."); }

        @Override public void onMove() {
            if (!mc.player.isUsingItem() || !(mc.player.getUseItem().getItem() instanceof BowItem)) return;
            LivingEntity best = null;
            double score = Double.MAX_VALUE;
            for (LivingEntity e : Targets.near(range.get(), true, mobs.get())) {
                double v = new Rot(Mth.wrapDegrees(mc.player.getYRot()), mc.player.getXRot()).angleTo(Placement.look(e.getEyePosition()));
                if (v < score && mc.player.hasLineOfSight(e)) {
                    score = v;
                    best = e;
                }
            }
            if (best == null) return;
            double speed = BowItem.getPowerForTime(mc.player.getTicksUsingItem()) * 3, dist = Math.sqrt(best.distanceToSqr(mc.player)), time = dist / Math.max(.6, speed);
            Vec3 at = best.position().add(0, best.getBbHeight() * .55, 0).add(best.position().subtract(best.xo, best.yo, best.zo).scale(time)).subtract(mc.player.getEyePosition());
            double flat = Math.hypot(at.x, at.z), g = .05 * 1.02, v2 = speed * speed, root = v2 * v2 - g * (g * flat * flat + 2 * at.y * v2);
            if (root < 0 || flat < .5) return;
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-at.x, at.z)));
            mc.player.setXRot((float) -Math.toDegrees(Math.atan((v2 - Math.sqrt(root)) / (g * flat))));
        }
    }

    public static final class PopCounter extends Module {
        private final Setting.Choice where = choice("Tell me in", "Pop-ups", "Chat");

        public PopCounter() { super(Category.COMBAT, "PopCounter", "Counts the totems players use and tells you when they die."); }

        public void popped(Entity e, int n) { if (on() && e != mc.player) say(e.getName().getString(), n == 1 ? "Popped a totem" : "Popped " + n + " totems"); }

        public void died(Entity e, int n) { if (on() && e != mc.player) say(e.getName().getString(), n == 0 ? "Died" : "Died after " + n + (n == 1 ? " totem" : " totems")); }

        private void say(String who, String what) {
            if (where.is("Chat")) Hollow.tell(who + ": " + what);
            else Notices.add(who, what, true);
        }
    }

    public static final class ArmorAlert extends Module {
        private final Setting.Num below = num("Below durability %", 15, 1, 50, 1);
        private int wait;

        public ArmorAlert() { super(Category.COMBAT, "ArmorAlert", "Warns you when armor is about to break."); }

        @Override public void onTick() {
            if (wait-- > 0) return;
            for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack i = mc.player.getItemBySlot(s);
                if (i.getMaxDamage() <= 0 || 100.0 * (i.getMaxDamage() - i.getDamageValue()) / i.getMaxDamage() >= below.get()) continue;
                Notices.add(i.getHoverName().getString(), "Nearly broken", false);
                wait = 200;
            }
        }
    }

    public static final class KeepDistance extends Module {
        private final Setting.Num dist = num("Distance", 2.8, 1, 6, .1);

        public KeepDistance() { super(Category.COMBAT, "KeepDistance", "Stops you walking any closer to an enemy than your reach needs."); }

        @Override public void onMove() {
            for (LivingEntity e : Targets.near(dist.get(), true, false)) {
                Vec3 away = mc.player.position().subtract(e.position());
                double len = away.horizontalDistance();
                Vec3 v = mc.player.getDeltaMovement();
                if (len < .05 || v.x * away.x + v.z * away.z >= 0) continue;
                double k = (v.x * away.x + v.z * away.z) / (len * len);
                mc.player.setDeltaMovement(v.x - away.x * k, v.y, v.z - away.z * k);
            }
        }
    }

    public static List<Module> all() {
        return List.of(new Criticals(), new TriggerBot(), new AimAssist(), new AutoWeapon(), new Hitboxes(), new SelfTrap(), new AutoWeb(), new AutoCity(), new AutoXp(), new AutoLog(), new BowRelease(), new BowAim(), new PopCounter(), new ArmorAlert(), new KeepDistance());
    }
}
