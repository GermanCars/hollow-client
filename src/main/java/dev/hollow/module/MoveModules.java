package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Movement modules. */
public final class MoveModules {
    private MoveModules() {}

    /** Velocity along the pressed movement keys, or null with none pressed. */
    static Vec3 heading(double speed) {
        var in = mc().player.input.keyPresses;
        double f = (in.forward() ? 1 : 0) - (in.backward() ? 1 : 0), s = (in.left() ? 1 : 0) - (in.right() ? 1 : 0), len = Math.hypot(f, s);
        if (len == 0) return null;
        double yaw = Math.toRadians(mc().player.getYRot()), k = speed / len, sin = Math.sin(yaw), cos = Math.cos(yaw);
        return new Vec3((s * cos - f * sin) * k, 0, (f * cos + s * sin) * k);
    }

    private static net.minecraft.client.Minecraft mc() { return net.minecraft.client.Minecraft.getInstance(); }

    public static final class Speed extends Module {
        private final Setting.Choice mode = choice("Mode", "Strafe", "Bunny hop", "Ground");
        private final Setting.Num speed = num("Speed", 1.2, .5, 3, .05);

        public Speed() { super(Category.MOVEMENT, "Speed", "Moves you faster."); }

        @Override public void onMove() {
            if (mc.player.isFallFlying() || mc.player.isInWater() || mc.player.isPassenger() || mc.player.onClimbable()) return;
            double base = .2873 * speed.get();
            Vec3 h = heading(base), v = mc.player.getDeltaMovement();
            if (h == null) {
                if (!mode.is("Ground")) mc.player.setDeltaMovement(0, v.y, 0);
                return;
            }
            if (mode.is("Ground") && !mc.player.onGround()) return;
            if (mode.is("Bunny hop") && mc.player.onGround()) mc.player.jumpFromGround();
            mc.player.setDeltaMovement(h.x, mc.player.getDeltaMovement().y, h.z);
        }

        @Override public String info() { return mode.get(); }
    }

    public static final class Flight extends Module {
        private final Setting.Choice mode = choice("Mode", "Velocity", "Creative");
        private final Setting.Num speed = num("Speed", 1, .1, 5, .1);
        private final Setting.Bool antiKick = bool("Avoid fly kick", true);
        private int ticks;

        public Flight() { super(Category.MOVEMENT, "Flight", "Lets you fly. Strict servers will pull you back."); }

        @Override public void onMove() {
            if (mode.is("Creative")) {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlyingSpeed(speed.getF() * .05f);
                return;
            }
            Vec3 h = heading(speed.get());
            var in = mc.player.input.keyPresses;
            double y = in.jump() ? speed.get() * .6 : in.shift() ? -speed.get() * .6 : 0;
            if (antiKick.get() && ++ticks % 40 == 0) y = -.04;
            mc.player.setDeltaMovement(h == null ? 0 : h.x, y, h == null ? 0 : h.z);
            mc.player.fallDistance = 0;
        }

        @Override protected void onDisable() {
            mc.player.getAbilities().flying = mc.player.isCreative() && mc.player.getAbilities().flying;
            mc.player.getAbilities().setFlyingSpeed(.05f);
        }

        @Override public String info() { return mode.get(); }
    }

    public static final class Jesus extends Module {
        private final Setting.Bool lava = bool("Lava", true);

        public Jesus() { super(Category.MOVEMENT, "Jesus", "Walk on water and lava."); }

        public boolean stands(boolean isLava) { return on() && (!isLava || lava.get()) && !mc.player.isShiftKeyDown() && !mc.player.isInWater() && !mc.player.isInLava() && mc.player.fallDistance < 3; }

        @Override public void onMove() {
            if ((mc.player.isInWater() || lava.get() && mc.player.isInLava()) && !mc.player.isShiftKeyDown() && !mc.player.input.keyPresses.jump()) {
                Vec3 v = mc.player.getDeltaMovement();
                mc.player.setDeltaMovement(v.x, .11, v.z);
            }
        }
    }

    public static final class Spider extends Module {
        private final Setting.Num speed = num("Speed", .2, .05, 1, .05);

        public Spider() { super(Category.MOVEMENT, "Spider", "Climbs any wall you walk into."); }

        @Override public void onMove() {
            if (!mc.player.horizontalCollision || heading(1) == null) return;
            Vec3 v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x, speed.get(), v.z);
            mc.player.fallDistance = 0;
        }
    }

    public static final class SafeWalk extends Module {
        public SafeWalk() { super(Category.MOVEMENT, "SafeWalk", "Stops you walking off edges."); }
    }

    public static final class AirJump extends Module {
        private boolean was;

        public AirJump() { super(Category.MOVEMENT, "AirJump", "Jump again in mid-air."); }

        @Override public void onMove() {
            boolean now = mc.options.keyJump.isDown() && mc.gui.screen() == null;
            if (now && !was && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isFallFlying()) mc.player.jumpFromGround();
            was = now;
        }
    }

    public static final class HighJump extends Module {
        private final Setting.Num boost = num("Multiplier", 1.5, 1, 5, .1);

        public HighJump() { super(Category.MOVEMENT, "HighJump", "Jumps higher."); }

        public float power(float normal) { return on() ? normal * boost.getF() : normal; }
    }

    public static final class LongJump extends Module {
        private final Setting.Num boost = num("Boost", .6, .1, 2, .05);
        private boolean was = true;

        public LongJump() { super(Category.MOVEMENT, "LongJump", "Pushes you forward when you jump."); }

        @Override public void onMove() {
            boolean ground = mc.player.onGround();
            if (was && !ground && mc.player.getDeltaMovement().y > .2) {
                Vec3 h = heading(boost.get());
                if (h != null) mc.player.addDeltaMovement(h);
            }
            was = ground;
        }
    }

    public static final class NoPush extends Module {
        public final Setting.Bool entities = bool("Entities", true), blocks = bool("Blocks", true), water = bool("Water", true);

        public NoPush() { super(Category.MOVEMENT, "NoPush", "Players, blocks and water can't push you."); }
    }

    public static final class InvMove extends Module {
        private final Setting.Bool sneak = bool("Sneak too", false);

        public InvMove() { super(Category.MOVEMENT, "InvMove", "Walk about with a menu open."); }

        public boolean allows(Screen s) { return on() && s != null && !(s instanceof ChatScreen) && !(s.getFocused() instanceof net.minecraft.client.gui.components.EditBox); }

        @Override public void onMove() {
            if (!allows(mc.gui.screen())) return;
            var o = mc.options;
            for (var k : new net.minecraft.client.KeyMapping[] {o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keySprint, o.keyShift}) {
                if (k == o.keyShift && !sneak.get()) continue;
                var bound = dev.hollow.mixin.KeyMappingAccess.class.cast(k).hollow$key();
                if (bound.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD) k.setDown(com.mojang.blaze3d.platform.InputConstants.isKeyDown(bound.getValue()));
            }
        }
    }

    public static final class FastClimb extends Module {
        private final Setting.Num up = num("Up speed", .287, .1, 1, .05), down = num("Down speed", .4, .1, 1, .05);

        public FastClimb() { super(Category.MOVEMENT, "FastClimb", "Climbs ladders and vines faster."); }

        @Override public void onMove() {
            if (!mc.player.onClimbable()) return;
            Vec3 v = mc.player.getDeltaMovement();
            if (mc.player.horizontalCollision || mc.player.input.keyPresses.jump()) mc.player.setDeltaMovement(v.x, up.get(), v.z);
            else if (!mc.player.isShiftKeyDown() && !mc.player.onGround()) mc.player.setPos(mc.player.getX(), mc.player.getY() - Math.max(0, down.get() - .15), mc.player.getZ());
        }
    }

    public static final class ReverseStep extends Module {
        private final Setting.Num height = num("Up to height", 3, 1, 10, 1), speed = num("Speed", 1, .2, 5, .1);
        private boolean was;

        public ReverseStep() { super(Category.MOVEMENT, "ReverseStep", "Drops straight down when you walk off a block."); }

        @Override public void onMove() {
            boolean ground = mc.player.onGround();
            if (was && !ground && mc.player.getDeltaMovement().y <= 0 && !mc.player.isInWater() && !mc.player.isFallFlying() && !mc.options.keyJump.isDown()) {
                AABB b = mc.player.getBoundingBox();
                if (!mc.level.noCollision(mc.player, b.expandTowards(0, -height.get(), 0))) mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -speed.get(), mc.player.getDeltaMovement().z);
            }
            was = ground;
        }
    }

    public static final class AutoJump extends Module {
        public AutoJump() { super(Category.MOVEMENT, "AutoJump", "Jumps whenever you land."); }

        @Override public void onMove() { if (mc.player.onGround() && heading(1) != null && !mc.player.isShiftKeyDown()) mc.player.jumpFromGround(); }
    }

    public static final class AutoWalk extends Module {
        private final Setting.Bool lock = bool("Lock to 45 degrees", false), stop = bool("Stop at unloaded chunks", true);

        public AutoWalk() { super(Category.MOVEMENT, "AutoWalk", "Keeps walking forward."); }

        @Override public void onMove() {
            if (lock.get()) mc.player.setYRot(Math.round(mc.player.getYRot() / 45) * 45f);
            Vec3 a = mc.player.getLookAngle();
            boolean loaded = !stop.get() || mc.level.hasChunk((int) Math.floor(mc.player.getX() + a.x * 16) >> 4, (int) Math.floor(mc.player.getZ() + a.z * 16) >> 4);
            mc.options.keyUp.setDown(loaded);
        }

        @Override protected void onDisable() { mc.options.keyUp.setDown(false); }

        @Override public void onWorldLeft() { mc.options.keyUp.setDown(false); }
    }

    public static final class Parkour extends Module {
        public Parkour() { super(Category.MOVEMENT, "Parkour", "Jumps at the very edge of blocks."); }

        @Override public void onMove() {
            if (!mc.player.onGround() || mc.player.isShiftKeyDown() || mc.options.keyJump.isDown() || heading(1) == null) return;
            AABB b = mc.player.getBoundingBox().move(0, -.5, 0).inflate(-.001, 0, -.001);
            if (mc.level.noCollision(mc.player, b)) mc.player.jumpFromGround();
        }
    }

    public static final class Blink extends Module {
        private final Setting.Num most = num("Most seconds", 10, 1, 30, 1);
        private final List<Packet<?>> held = new ArrayList<>();
        private Vec3 start;
        private boolean flushing;

        public Blink() { super(Category.MOVEMENT, "Blink", "Holds your movement back, then sends it all at once. Short hops only."); }

        @Override protected void onEnable() { start = mc.player.position(); }

        @Override public boolean onSend(Packet<?> p) {
            // Tick ends are kept in between, as servers kick for two positions in one client tick.
            if (flushing || !(p instanceof ServerboundMovePlayerPacket) && !(p instanceof ServerboundClientTickEndPacket)) return false;
            synchronized (held) {
                held.add(p);
            }
            return true;
        }

        @Override public void onTick() { if (held.size() > most.get() * 40) set(false); }

        @Override protected void onDisable() {
            flushing = true;
            synchronized (held) {
                held.forEach(mc.player.connection::send);
                held.clear();
            }
            flushing = false;
        }

        @Override public void onWorldLeft() {
            held.clear();
            restore(false);
        }

        @Override public void onWorldRender(float partial) {
            if (start != null) net.minecraft.gizmos.Gizmos.cuboid(new AABB(start.x - .3, start.y, start.z - .3, start.x + .3, start.y + 1.8, start.z + .3), net.minecraft.gizmos.GizmoStyle.stroke(0xFF000000 | dev.hollow.ui.Theme.accent(), 2)).setAlwaysOnTop();
        }

        @Override public String info() { return Integer.toString(held.size()); }
    }

    public static final class AntiVoid extends Module {
        private final Setting.Num depth = num("Below the world", 4, 0, 40, 1);

        public AntiVoid() { super(Category.MOVEMENT, "AntiVoid", "Bounces you back up before the void takes you."); }

        @Override public void onMove() {
            if (mc.player.getY() > mc.level.getMinY() - depth.get() || mc.player.getDeltaMovement().y > 0) return;
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, .6, mc.player.getDeltaMovement().z);
            mc.player.fallDistance = 0;
        }
    }

    public static final class NoJumpDelay extends Module {
        public NoJumpDelay() { super(Category.MOVEMENT, "NoJumpDelay", "Jump again the moment you land."); }

        @Override public void onMove() { ((dev.hollow.mixin.LivingEntityAccess) mc.player).hollow$setJumpDelay(0); }
    }

    public static final class Sneak extends Module {
        public Sneak() { super(Category.MOVEMENT, "Sneak", "Keeps sneaking."); }

        @Override public void onMove() { if (mc.gui.screen() == null) mc.options.keyShift.setDown(true); }

        @Override protected void onDisable() { mc.options.keyShift.setDown(false); }

        @Override public void onWorldLeft() { mc.options.keyShift.setDown(false); }
    }

    public static final class HoleSnap extends Module {
        private final Setting.Num range = num("Range", 4, 1, 8, 1), speed = num("Speed", .28, .1, .6, .02);
        private BlockPos goal;

        public HoleSnap() { super(Category.MOVEMENT, "HoleSnap", "Pulls you into the nearest safe hole, then switches off."); }

        @Override protected void onEnable() {
            goal = null;
            int r = range.getInt();
            double best = Double.MAX_VALUE;
            BlockPos me = mc.player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -4, -r), me.offset(r, 0, r))) {
                double d = p.distToCenterSqr(mc.player.position());
                if (d < best && Holes.isHole(p, Obsidian::blastProof, Obsidian::open)) {
                    best = d;
                    goal = p.immutable();
                }
            }
            if (goal == null) {
                Hollow.tell("No safe hole nearby");
                set(false);
            }
        }

        @Override public void onMove() {
            if (goal == null) return;
            double dx = goal.getX() + .5 - mc.player.getX(), dz = goal.getZ() + .5 - mc.player.getZ(), len = Math.hypot(dx, dz);
            if (len < .12 && mc.player.getY() < goal.getY() + .1 || mc.player.getY() < goal.getY() - .5) {
                mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
                set(false);
                return;
            }
            double k = Math.min(1, speed.get() / Math.max(.001, len));
            mc.player.setDeltaMovement(dx * k, mc.player.getDeltaMovement().y, dz * k);
        }
    }

    public static final class EntitySpeed extends Module {
        private final Setting.Num speed = num("Speed", 1, .1, 4, .1);

        public EntitySpeed() { super(Category.MOVEMENT, "EntitySpeed", "Rides horses, pigs and the rest faster."); }

        @Override public void onMove() {
            Entity v = mc.player.getVehicle();
            if (v == null || v instanceof AbstractBoat) return;
            Vec3 h = heading(speed.get() * .5);
            if (h != null) v.setDeltaMovement(h.x, v.getDeltaMovement().y, h.z);
        }
    }

    public static final class BoatFly extends Module {
        private final Setting.Num speed = num("Speed", 1.2, .1, 4, .1), climb = num("Climb speed", .4, .1, 1.5, .1);

        public BoatFly() { super(Category.MOVEMENT, "BoatFly", "Flies the boat you are in."); }

        @Override public void onMove() {
            if (!(mc.player.getVehicle() instanceof AbstractBoat b)) return;
            Vec3 h = heading(speed.get());
            var in = mc.player.input.keyPresses;
            b.setYRot(mc.player.getYRot());
            b.setDeltaMovement(h == null ? 0 : h.x, in.jump() ? climb.get() : in.sprint() ? -climb.get() : 0, h == null ? 0 : h.z);
        }
    }

    public static final class ElytraBoost extends Module {
        private final Setting.Num every = num("Every ticks", 30, 10, 100, 5), below = num("Below speed", 1.2, .2, 3, .1);
        private int wait;

        public ElytraBoost() { super(Category.MOVEMENT, "ElytraBoost", "Uses fireworks to keep your elytra fast."); }

        @Override public void onTick() {
            if (wait-- > 0 || !mc.player.isFallFlying() || mc.player.getDeltaMovement().length() > below.get()) return;
            int slot = Inv.hotbar(Items.FIREWORK_ROCKET);
            if (slot < 0) return;
            Actions.submit(new Request(Priority.SCAFFOLD, null, false, slot, () -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                wait = every.getInt();
            }));
        }
    }

    public static final class AutoElytra extends Module {
        private final Setting.Num fall = num("After falling", 3, 1, 20, 1);

        public AutoElytra() { super(Category.MOVEMENT, "AutoElytra", "Opens your elytra when you fall."); }

        @Override public void onMove() {
            if (mc.player.isFallFlying() || mc.player.onGround() || mc.player.fallDistance < fall.get() || mc.player.isInWater() || !mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) return;
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
    }

    public static final class FastSwim extends Module {
        private final Setting.Num speed = num("Speed", .35, .1, 1.5, .05);

        public FastSwim() { super(Category.MOVEMENT, "FastSwim", "Moves faster in water and lava."); }

        @Override public void onMove() {
            if (!mc.player.isInWater() && !mc.player.isInLava()) return;
            Vec3 h = heading(speed.get());
            var in = mc.player.input.keyPresses;
            double y = in.jump() ? speed.get() * .7 : in.shift() ? -speed.get() * .7 : mc.player.getDeltaMovement().y;
            mc.player.setDeltaMovement(h == null ? mc.player.getDeltaMovement().x : h.x, y, h == null ? mc.player.getDeltaMovement().z : h.z);
        }
    }

    public static final class NoWeb extends Module {
        public NoWeb() { super(Category.MOVEMENT, "NoWeb", "Cobwebs, berry bushes and the like don't slow you."); }
    }

    public static final class AntiLevitation extends Module {
        public AntiLevitation() { super(Category.MOVEMENT, "AntiLevitation", "Ignores levitation."); }

        @Override public void onMove() {
            if (mc.player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, Math.min(0, mc.player.getDeltaMovement().y), mc.player.getDeltaMovement().z);
        }
    }

    public static final class ClickTp extends Module {
        private final Setting.Num range = num("Range", 8, 2, 60, 1);
        private boolean was;

        public ClickTp() { super(Category.MOVEMENT, "ClickTP", "Sneak and right click a block to jump to it. Short hops only on most servers."); }

        @Override public void onMove() {
            boolean now = mc.options.keyUse.isDown() && mc.player.isShiftKeyDown() && mc.gui.screen() == null;
            if (now && !was) {
                var hit = mc.player.pick(range.get(), 1, false);
                if (hit instanceof net.minecraft.world.phys.BlockHitResult b && b.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos p = b.getBlockPos().relative(b.getDirection());
                    mc.player.setPos(p.getX() + .5, p.getY(), p.getZ() + .5);
                }
            }
            was = now;
        }
    }

    public static final class YawLock extends Module {
        private final Setting.Choice step = choice("Snap to", "45 degrees", "90 degrees");

        public YawLock() { super(Category.MOVEMENT, "YawLock", "Keeps you facing straight, for highways."); }

        @Override public void onMove() {
            float s = step.is("45 degrees") ? 45 : 90;
            mc.player.setYRot(Math.round(mc.player.getYRot() / s) * s);
        }
    }

    public static final class IceSpeed extends Module {
        private final Setting.Num speed = num("Speed", .5, .2, 1.2, .05);

        public IceSpeed() { super(Category.MOVEMENT, "IceSpeed", "Runs faster on ice."); }

        @Override public void onMove() {
            var b = mc.level.getBlockState(mc.player.getBlockPosBelowThatAffectsMyMovement());
            if (!mc.player.onGround() || !b.is(Blocks.ICE) && !b.is(Blocks.PACKED_ICE) && !b.is(Blocks.BLUE_ICE) && !b.is(Blocks.FROSTED_ICE)) return;
            Vec3 h = heading(speed.get());
            if (h != null) mc.player.setDeltaMovement(h.x, mc.player.getDeltaMovement().y, h.z);
        }
    }

    public static List<Module> all() {
        return List.of(new Speed(), new Flight(), new Jesus(), new Spider(), new SafeWalk(), new AirJump(), new HighJump(), new LongJump(), new NoPush(), new InvMove(), new FastClimb(), new ReverseStep(), new AutoJump(), new AutoWalk(), new Parkour(), new Blink(), new AntiVoid(), new NoJumpDelay(), new Sneak(), new HoleSnap(), new EntitySpeed(), new BoatFly(), new ElytraBoost(), new AutoElytra(), new FastSwim(), new NoWeb(), new AntiLevitation(), new ClickTp(), new YawLock(), new IceSpeed());
    }
}
