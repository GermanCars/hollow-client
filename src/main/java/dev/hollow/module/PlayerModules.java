package dev.hollow.module;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.action.*;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.Notices;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;

/** Modules about the player's hands and inventory. */
public final class PlayerModules {
    private PlayerModules() {}

    public static final class AutoTool extends Module {
        public AutoTool() { super(Category.PLAYER, "AutoTool", "Holds the fastest tool for the block you are mining."); }

        public static void pick(BlockPos pos) {
            BlockState s = mc.level.getBlockState(pos);
            int best = -1;
            float speed = mc.player.getMainHandItem().getDestroySpeed(s);
            for (int i = 0; i < 9; i++) {
                ItemStack t = mc.player.getInventory().getItem(i);
                float v = t.getDestroySpeed(s);
                // Leave a nearly broken tool alone.
                if (t.getMaxDamage() > 0 && t.getMaxDamage() - t.getDamageValue() < 4) continue;
                if (v > speed) {
                    speed = v;
                    best = i;
                }
            }
            if (best >= 0) mc.player.getInventory().setSelectedSlot(best);
        }
    }

    public static final class FastUse extends Module {
        private final Setting.Num delay = num("Delay", 0, 0, 4, 1);
        private final Setting.Choice what = choice("For", "Blocks and bottles", "Experience bottles", "Everything");

        public FastUse() { super(Category.PLAYER, "FastUse", "Shortens the wait between right clicks."); }

        public boolean applies() {
            ItemStack s = mc.player.getMainHandItem();
            return switch (what.get()) {
                case "Everything" -> true;
                case "Experience bottles" -> s.is(Items.EXPERIENCE_BOTTLE);
                default -> s.is(Items.EXPERIENCE_BOTTLE) || s.getItem() instanceof net.minecraft.world.item.BlockItem;
            };
        }

        public int delay() { return delay.getInt(); }
    }

    public static final class FastBreak extends Module {
        public FastBreak() { super(Category.PLAYER, "FastBreak", "Removes the pause between breaking blocks."); }
    }

    public static final class Reach extends Module {
        public final Setting.Num blocks = num("Blocks", 5, 3, 8, .1), entities = num("Entities", 3, 3, 8, .1);

        public Reach() { super(Category.PLAYER, "Reach", "Lets you reach further. Most servers allow only a little."); }
    }

    public static final class AutoRespawn extends Module {
        private final Setting.Num delay = num("Delay ticks", 10, 0, 60, 5);
        private int wait;

        public AutoRespawn() { super(Category.PLAYER, "AutoRespawn", "Respawns without the death screen."); }

        /** Runs while dead, so outside the usual ticks. */
        public void deadTick() {
            if (!on() || !(mc.gui.screen() instanceof DeathScreen)) {
                wait = delay.getInt();
                return;
            }
            if (wait-- > 0) return;
            mc.player.respawn();
            mc.gui.setScreen(null);
        }
    }

    public static final class AutoFish extends Module {
        private volatile boolean bite;
        private int cast;

        public AutoFish() { super(Category.PLAYER, "AutoFish", "Reels in and casts again when something bites."); }

        @Override public boolean onReceive(Packet<?> p) {
            if (p instanceof ClientboundSoundPacket s && mc.player != null && mc.player.fishing != null && s.getSound().value() == SoundEvents.FISHING_BOBBER_SPLASH && mc.player.fishing.distanceToSqr(s.getX(), s.getY(), s.getZ()) < 9) bite = true;
            return false;
        }

        @Override public void onTick() {
            if (bite) {
                bite = false;
                use();
                cast = 20;
            } else if (cast > 0 && --cast == 0 && mc.player.fishing == null) use();
        }

        private void use() {
            for (InteractionHand h : InteractionHand.values()) if (mc.player.getItemInHand(h).is(Items.FISHING_ROD)) {
                mc.gameMode.useItem(mc.player, h);
                return;
            }
        }
    }

    public static final class MiddleClick extends Module {
        private final Setting.Choice air = choice("In the air", "Ender pearl", "Firework", "Experience bottle", "Nothing");
        private final Setting.Bool friends = bool("Add friends", true);

        public MiddleClick() { super(Category.PLAYER, "MiddleClick", "Middle click adds friends or throws a pearl."); }

        public void pressed() {
            if (!on() || mc.gui.screen() != null) return;
            if (mc.hitResult instanceof EntityHitResult h && h.getEntity() instanceof Player p) {
                if (!friends.get()) return;
                String n = p.getScoreboardName();
                boolean was = Hollow.FRIENDS.has(n);
                if (was) Hollow.FRIENDS.remove(n);
                else Hollow.FRIENDS.add(n);
                Notices.add(n, was ? "No longer a friend" : "Now a friend", !was);
                Hollow.save();
                return;
            }
            var item = switch (air.get()) {
                case "Ender pearl" -> Items.ENDER_PEARL;
                case "Firework" -> Items.FIREWORK_ROCKET;
                case "Experience bottle" -> Items.EXPERIENCE_BOTTLE;
                default -> null;
            };
            if (item == null || mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult b && b.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) return;
            int slot = Inv.hotbar(item);
            if (slot < 0) return;
            int held = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(slot);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            mc.player.getInventory().setSelectedSlot(held);
        }
    }

    public static final class AutoClicker extends Module {
        private final Setting.Choice button = choice("Button", "Left", "Right");
        private final Setting.Num cps = num("Clicks per second", 10, 1, 20, 1);
        private final Setting.Bool held = bool("Only while held", true);
        private double due;

        public AutoClicker() { super(Category.PLAYER, "AutoClicker", "Clicks for you."); }

        @Override public void onMove() {
            var key = button.is("Left") ? mc.options.keyAttack : mc.options.keyUse;
            if (mc.gui.screen() != null || held.get() && !key.isDown()) return;
            due += cps.get() / 20;
            while (due >= 1) {
                due--;
                net.minecraft.client.KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(button.is("Left") ? InputConstants.MOUSE_BUTTON_LEFT : InputConstants.MOUSE_BUTTON_RIGHT));
            }
        }
    }

    public static final class ChestSwap extends Module {
        public ChestSwap() {
            super(Category.PLAYER, "ChestSwap", "Swaps between elytra and chestplate, then switches off.");
            hidden = true;
        }

        @Override protected void onEnable() {
            boolean elytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER);
            int slot = Inv.anywhere(s -> {
                var eq = s.get(DataComponents.EQUIPPABLE);
                return eq != null && eq.slot() == EquipmentSlot.CHEST && s.has(DataComponents.GLIDER) != elytra;
            });
            if (slot < 0) Hollow.tell(elytra ? "No chestplate to swap to" : "No elytra to swap to");
            else if (slot < 9) Inv.swapMenu(InventoryMenu.ARMOR_SLOT_START + 1, slot);
            else {
                // Through a hotbar slot and back again.
                int hot = mc.player.getInventory().getSelectedSlot();
                Inv.swap(slot, hot);
                Inv.swapMenu(InventoryMenu.ARMOR_SLOT_START + 1, hot);
                Inv.swap(slot, hot);
            }
            set(false);
        }
    }

    public static final class Replenish extends Module {
        private final Setting.Num below = num("Below", 8, 1, 32, 1);
        private int wait;

        public Replenish() { super(Category.PLAYER, "Replenish", "Tops up hotbar stacks from your inventory."); }

        @Override public void onTick() {
            if (wait-- > 0 || !Inv.canClick()) return;
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.player.getInventory().getItem(i);
                if (s.isEmpty() || s.getMaxStackSize() == 1 || s.getCount() >= Math.min(below.getInt(), s.getMaxStackSize())) continue;
                for (int j = 9; j < 36; j++) {
                    ItemStack o = mc.player.getInventory().getItem(j);
                    if (!ItemStack.isSameItemSameComponents(s, o)) continue;
                    if (s.getCount() + o.getCount() <= s.getMaxStackSize()) Inv.shiftClickInv(j);
                    else Inv.swap(j, i);
                    wait = 4;
                    return;
                }
            }
        }
    }

    public static final class AntiAfk extends Module {
        private final Setting.Bool jump = bool("Jump", true), turn = bool("Turn", true), swing = bool("Swing", false);
        private final Setting.Num every = num("Every seconds", 10, 2, 120, 1);
        private int ticks;

        public AntiAfk() { super(Category.PLAYER, "AntiAFK", "Moves a little so you aren't kicked for idling."); }

        @Override public void onMove() {
            if (++ticks < every.get() * 20) return;
            ticks = 0;
            if (jump.get() && mc.player.onGround()) mc.player.jumpFromGround();
            if (turn.get()) mc.player.setYRot(mc.player.getYRot() + mc.player.getRandom().nextInt(80) - 40);
            if (swing.get()) mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false);
        }
    }

    public static final class NoRotate extends Module {
        public NoRotate() { super(Category.PLAYER, "NoRotate", "Keeps your view when the server tries to turn you."); }
    }

    public static final class XCarry extends Module {
        public XCarry() { super(Category.PLAYER, "XCarry", "Keeps items in your crafting grid when you close the inventory."); }

        @Override public boolean onSend(Packet<?> p) { return p instanceof ServerboundContainerClosePacket c && c.getContainerId() == InventoryMenu.CONTAINER_ID; }
    }

    public static final class FakePlayer extends Module {
        private RemotePlayer fake;

        public FakePlayer() { super(Category.PLAYER, "FakePlayer", "Stands a copy of you in the world to practise on. Only you see it."); }

        @Override protected void onEnable() {
            fake = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "HollowDummy"));
            fake.copyPosition(mc.player);
            fake.yHeadRot = mc.player.yHeadRot;
            fake.setHealth(20);
            for (EquipmentSlot s : EquipmentSlot.values()) if (s.isArmor() || s == EquipmentSlot.MAINHAND || s == EquipmentSlot.OFFHAND) fake.setItemSlot(s, mc.player.getItemBySlot(s).copy());
            fake.setId(-7357);
            mc.level.addEntity(fake);
        }

        @Override protected void onDisable() {
            if (fake != null) mc.level.removeEntity(fake.getId(), Entity.RemovalReason.DISCARDED);
            fake = null;
        }

        @Override public void onWorldLeft() {
            fake = null;
            restore(false);
        }
    }

    public static final class AntiHunger extends Module {
        public AntiHunger() { super(Category.PLAYER, "AntiHunger", "Hides sprinting from the server so you get hungry more slowly."); }

        @Override public boolean onSend(Packet<?> p) { return p instanceof ServerboundPlayerCommandPacket c && c.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING && mc.player != null && !mc.player.isInWater(); }
    }

    public static final class AutoDrop extends Module {
        private final Setting.Ids items = ids("Items", Setting.Ids.Kind.ITEM, "rotten_flesh", "spider_eye", "poisonous_potato");
        private int wait;

        public AutoDrop() { super(Category.PLAYER, "AutoDrop", "Throws away items you don't want."); }

        @Override public void onTick() {
            if (wait-- > 0 || !Inv.canClick()) return;
            for (int i = 0; i < 36; i++) {
                ItemStack s = mc.player.getInventory().getItem(i);
                if (s.isEmpty() || !items.has(BuiltInRegistries.ITEM.getKey(s.getItem()).toString())) continue;
                Inv.drop(i);
                wait = 3;
                return;
            }
        }
    }

    public static final class AutoMount extends Module {
        private final Setting.Ids mounts = ids("Mounts", Setting.Ids.Kind.ENTITY, "horse", "donkey", "mule", "pig", "strider", "camel", "oak_boat");
        private int wait;

        public AutoMount() { super(Category.PLAYER, "AutoMount", "Gets on the nearest mount."); }

        @Override public void onTick() {
            if (wait-- > 0 || mc.player.isPassenger()) return;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || e.isVehicle() || e.distanceTo(mc.player) > 4 || !mounts.has(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())) continue;
                WorldModules.useOn(e, InteractionHand.MAIN_HAND);
                wait = 10;
                return;
            }
        }
    }

    public static final class NoInteract extends Module {
        private final Setting.Bool containers = bool("Containers", true), frames = bool("Item frames", true), villagers = bool("Villagers", false);

        public NoInteract() { super(Category.PLAYER, "NoInteract", "Stops you opening or rotating things by accident."); }

        public boolean blocks(BlockPos p) { return on() && containers.get() && mc.level.getBlockState(p).getMenuProvider(mc.level, p) != null; }

        public boolean blocks(Entity e) { return on() && (frames.get() && e instanceof net.minecraft.world.entity.decoration.ItemFrame || villagers.get() && e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager); }
    }

    public static final class AutoPot extends Module {
        private final Setting.Num health = num("Below health", 10, 1, 19, 1);
        private int wait;

        public AutoPot() { super(Category.PLAYER, "AutoPot", "Splashes healing potions on yourself when hurt."); }

        @Override public void onTick() {
            if (wait-- > 0 || Damage.health(mc.player) > health.get()) return;
            int slot = Inv.hotbar(s -> {
                if (!s.is(Items.SPLASH_POTION)) return false;
                var c = s.get(DataComponents.POTION_CONTENTS);
                if (c == null) return false;
                for (var e : c.getAllEffects()) if (e.getEffect().is(net.minecraft.world.effect.MobEffects.INSTANT_HEALTH)) return true;
                return false;
            });
            if (slot < 0) return;
            Actions.submit(new Request(Priority.SURROUND, new Rot(mc.player.getYRot(), 90), true, slot, () -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                wait = 10;
            }));
        }
    }

    public static final class Multitask extends Module {
        public Multitask() { super(Category.PLAYER, "Multitask", "Lets you mine while eating or using an item."); }
    }

    public static final class AutoSleep extends Module {
        private int wait;

        public AutoSleep() { super(Category.PLAYER, "AutoSleep", "Uses a bed in reach at night."); }

        @Override public void onTick() {
            if (wait-- > 0 || mc.player.isSleeping() || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
            long t = mc.level.getOverworldClockTime() % 24000;
            if (t < 12600 || t > 23400) return;
            BlockPos me = mc.player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-3, -2, -3), me.offset(3, 2, 3))) {
                if (!(mc.level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.BedBlock)) continue;
                WorldModules.useBlock(p.immutable(), -1);
                wait = 40;
                return;
            }
        }
    }

    public static final class InventoryAlert extends Module {
        private boolean told;

        public InventoryAlert() { super(Category.PLAYER, "FullAlert", "Tells you when your inventory is full."); }

        @Override public void onTick() {
            boolean full = mc.player.getInventory().getFreeSlot() < 0;
            if (full && !told) Notices.add("Inventory", "Full", false);
            told = full;
        }
    }

    public static List<Module> all() {
        return List.of(new AutoTool(), new FastUse(), new FastBreak(), new Reach(), new AutoRespawn(), new AutoFish(), new MiddleClick(), new AutoClicker(), new ChestSwap(), new Replenish(), new AntiAfk(), new NoRotate(), new XCarry(), new FakePlayer(), new AntiHunger(), new AutoDrop(), new AutoMount(), new NoInteract(), new AutoPot(), new Multitask(), new AutoSleep(), new InventoryAlert());
    }
}
