package dev.hollow.test;

import dev.hollow.Hollow;
import dev.hollow.action.Damage;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Switches every module on in a real world, then checks what the important ones actually do. */
public class ModuleTest implements FabricClientGameTest {
    private ClientGameTestContext c;
    private TestSingleplayerContext world;
    private final List<String> results = new ArrayList<>();

    private static Module mod(String name) { return Hollow.modules().stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(() -> new AssertionError("no module " + name)); }

    private void cmd(String... s) { for (String x : s) world.getServer().runCommand(x); }

    private void set(String n, boolean on) { c.runOnClient(mc -> mod(n).set(on)); }

    private void option(String n, String setting, Object v) {
        c.runOnClient(mc -> {
            Setting s = mod(n).settings.stream().filter(x -> x.name.equals(setting)).findFirst().orElseThrow(() -> new AssertionError(n + " has no " + setting));
            if (s instanceof Setting.Bool b) b.set((Boolean) v);
            else if (s instanceof Setting.Num m) m.set(((Number) v).doubleValue());
            else if (s instanceof Setting.Choice ch) ch.pick(ch.options.indexOf(v));
        });
    }

    private void check(String what, boolean ok) {
        results.add((ok ? "PASS " : "FAIL ") + what);
        System.out.println("HOLLOWTEST " + (ok ? "PASS " : "FAIL ") + what);
    }

    private boolean until(int ticks, Predicate<Minecraft> p) {
        for (int i = 0; i < ticks; i++) {
            if (c.computeOnClient(p::test)) return true;
            c.waitTick();
        }
        return false;
    }

    private float health() { return c.computeOnClient(mc -> mc.player.getHealth()); }

    private void clean() {
        c.runOnClient(mc -> {
            for (Module m : Hollow.modules()) if (m != Hollow.INTERFACE && m != Hollow.HUD) m.set(false);
            mc.gui.setScreen(null);
        });
        c.runOnClient(mc -> {
            mc.player.getInventory().setSelectedSlot(0);
            mc.player.setDeltaMovement(0, 0, 0);
        });
        cmd("kill @e[type=!player]", "clear @a", "effect clear @a", "fill -12 -60 -12 12 -50 12 air", "fill -12 -61 -12 12 -61 12 grass_block", "tp @a 0.5 -60 0.5 0 0", "gamemode survival @a", "effect give @a instant_health 1 10", "time set day");
        c.waitTicks(12);
        // What was killed leaves loot, which would be picked up into the hand.
        cmd("kill @e[type=item]", "clear @a");
        c.waitTicks(3);
    }

    @Override public void runTest(ClientGameTestContext c) {
        this.c = c;
        c.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext w = c.worldBuilder().adjustSettings(s -> s.setAllowCommands(true)).create()) {
            world = w;
            c.waitTicks(40);
            cmd("gamerule advance_time false", "gamerule spawn_mobs false", "difficulty normal");
            everyModule();
            behaviours();
        }
        c.runOnClient(mc -> {
            Hollow.INTERFACE.startMenu.set(false);
            mc.gui.setScreen(new TitleScreen());
        });
        c.waitTicks(5);
        long failed = results.stream().filter(r -> r.startsWith("FAIL")).count();
        System.out.println("HOLLOWTEST SUMMARY " + (results.size() - failed) + " passed, " + failed + " failed");
    }

    private void everyModule() {
        clean();
        cmd("summon zombie 3 -60 3", "summon sheep 2 -60 -3", "give @a obsidian 64", "give @a end_crystal 64", "give @a torch 16", "give @a diamond_sword", "give @a cobblestone 64", "give @a bone_meal 8", "setblock 2 -60 0 chest", "setblock 0 -60 3 oak_door");
        c.waitTicks(10);
        int n = 0;
        for (Module m : c.computeOnClient(mc -> List.copyOf(Hollow.modules()))) {
            if (m == Hollow.INTERFACE || m.name.equals("AutoReconnect")) continue;
            System.out.println("HOLLOWTEST switching " + m.name);
            boolean was = c.computeOnClient(mc -> m.on());
            c.runOnClient(mc -> m.set(true));
            c.waitTicks(6);
            c.runOnClient(mc -> m.set(was && m == Hollow.HUD));
            c.waitTicks(1);
            n++;
        }
        check("all " + n + " modules switch on and off in a world", true);
        // Everything at once.
        c.runOnClient(mc -> { for (Module m : Hollow.modules()) if (!List.of("Panic", "AutoReconnect", "Blink", "Freecam", "ChestSwap", "AutoLog").contains(m.name)) m.set(true); });
        c.waitTicks(40);
        c.takeScreenshot("everything");
        check("everything on together for two seconds", true);
        clean();
    }

    private void behaviours() {
        // KillAura
        cmd("give @a netherite_sword", "summon zombie 0.5 -60 3 {NoAI:1b,Silent:1b}");
        option("KillAura", "Mobs", true);
        set("KillAura", true);
        boolean killed = until(200, mc -> mc.level.getEntitiesOfClass(Zombie.class, new AABB(-8, -62, -8, 8, -50, 8)).isEmpty());
        if (!killed) System.out.println("HOLLOWTEST WHY " + c.computeOnClient(mc -> mc.player.position() + " held " + mc.player.getMainHandItem() + " using " + mc.player.isUsingItem() + " ready " + mc.player.getAttackStrengthScale(.5f) + " screen " + mc.gui.screen() + " on " + Hollow.modules().stream().filter(Module::on).map(m -> m.name).toList() + " zombies " + mc.level.getEntitiesOfClass(Zombie.class, new AABB(-8, -62, -8, 8, -50, 8)).stream().map(z -> z.position() + " hp " + z.getHealth() + " sees " + mc.player.hasLineOfSight(z) + " dist " + dev.hollow.action.Placement.distanceTo(z.getBoundingBox())).toList()));
        check("KillAura kills a zombie", killed);
        clean();

        // Criticals: a plain netherite sword does 8, a critical hit 12
        cmd("give @a netherite_sword", "summon husk 0.5 -60 2.5 {NoAI:1b,Silent:1b,Health:100f,attributes:[{id:\"minecraft:max_health\",base:100},{id:\"minecraft:armor\",base:0}]}");
        c.waitTicks(30);
        set("Criticals", true);
        c.runOnClient(mc -> mc.gameMode.attack(mc.player, mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(-3, -61, 1, 3, -57, 5), e -> e != mc.player).getFirst()));
        c.waitTicks(10);
        float left = c.computeOnClient(mc -> mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(-9, -61, -9, 9, -57, 12), e -> e != mc.player).getFirst().getHealth());
        check("Criticals makes a critical hit (100 to " + left + ")", left < 89);
        check("Criticals doesn't get the player kicked", c.computeOnClient(mc -> mc.player != null));
        clean();

        // Blink
        set("Blink", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 25);
        double far = c.computeOnClient(mc -> mc.player.getZ());
        double serverZ = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getZ());
        set("Blink", false);
        c.waitTicks(10);
        double after = world.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getZ());
        check(String.format("Blink holds the player back (%.1f), then catches up (%.1f of %.1f)", serverZ, after, far), serverZ < 1 && far > 2 && Math.abs(after - far) < 1);
        clean();

        // Friends
        c.runOnClient(mc -> Hollow.FRIENDS.add("HollowDummy"));
        set("FakePlayer", true);
        c.waitTicks(3);
        check("friends are never targets", c.computeOnClient(mc -> dev.hollow.action.Targets.near(10, true, false).isEmpty()));
        c.runOnClient(mc -> Hollow.FRIENDS.remove("HollowDummy"));
        check("strangers are targets", c.computeOnClient(mc -> !dev.hollow.action.Targets.near(10, true, false).isEmpty()));
        clean();
        // Surround
        cmd("give @a obsidian 64");
        c.waitTicks(5);
        set("Surround", true);
        check("Surround walls in the feet", until(80, mc -> {
            BlockPos p = mc.player.blockPosition();
            return mc.level.getBlockState(p.north()).is(Blocks.OBSIDIAN) && mc.level.getBlockState(p.south()).is(Blocks.OBSIDIAN) && mc.level.getBlockState(p.east()).is(Blocks.OBSIDIAN) && mc.level.getBlockState(p.west()).is(Blocks.OBSIDIAN);
        }));
        c.takeScreenshot("surround");
        clean();

        // AutoTotem
        cmd("item replace entity @a inventory.5 with totem_of_undying");
        set("AutoTotem", true);
        check("AutoTotem fills the offhand", until(40, mc -> mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)));
        clean();

        // CrystalAura
        cmd("fill -3 -61 -3 3 -61 5 obsidian", "item replace entity @a hotbar.0 with end_crystal 64", "item replace entity @a armor.chest with netherite_chestplate[enchantments={blast_protection:4}]", "item replace entity @a armor.legs with netherite_leggings[enchantments={blast_protection:4}]", "item replace entity @a armor.feet with netherite_boots[enchantments={blast_protection:4}]", "item replace entity @a armor.head with netherite_helmet[enchantments={blast_protection:4}]", "summon zombie 0.5 -60 4.5 {NoAI:1b,Silent:1b,Health:20f}");
        c.waitTicks(10);
        option("CrystalAura", "Mobs", true);
        option("CrystalAura", "Minimum damage", 2);
        option("CrystalAura", "Self damage cap", 10);
        option("CrystalAura", "Break range", 5);
        set("CrystalAura", true);
        check("CrystalAura kills a zombie with crystals", until(200, mc -> mc.level.getEntitiesOfClass(Zombie.class, new AABB(-9, -64, -9, 9, -50, 9)).isEmpty()));
        check("CrystalAura leaves the player alive", c.computeOnClient(mc -> mc.player.isAlive() && mc.player.getHealth() > 1));
        clean();
        cmd("fill -12 -62 -12 12 -64 12 dirt");

        // Damage estimate against the real thing
        for (int i = 0; i < 3; i++) {
            clean();
            cmd("fill -6 -61 -6 6 -61 6 obsidian", "item replace entity @a armor.chest with diamond_chestplate[enchantments={protection:" + (i + 1) + "}]", "item replace entity @a armor.legs with iron_leggings");
            cmd("attribute @p minecraft:max_health base set 200", "effect give @a instant_health 1 20");
            double z = 4.5 + i;
            cmd("summon end_crystal 0.5 -60 " + z + " {ShowBottom:0b}");
            c.waitTicks(30);
            float before = health();
            float guess = c.computeOnClient(mc -> Damage.explosion(mc.level.getEntitiesOfClass(EndCrystal.class, new AABB(-9, -64, -9, 9, -50, 12)).getFirst().position(), Damage.CRYSTAL, mc.player, null));
            cmd("execute as @p run damage @e[type=end_crystal,limit=1] 5 player_attack by @p");
            c.waitTicks(4);
            float took = before - health();
            check(String.format("damage estimate %.2f against real %.2f", guess, took), Math.abs(guess - took) < .6f);
        }
        cmd("attribute @p minecraft:max_health base set 20");
        clean();
        cmd("fill -12 -64 -12 12 -62 12 dirt");

        // HoleFill and Trap need a player; AutoTrap logic shares Placement with Surround. SelfTrap:
        cmd("give @a obsidian 64");
        c.waitTicks(3);
        set("SelfTrap", true);
        check("SelfTrap covers the head", until(120, mc -> mc.level.getBlockState(mc.player.blockPosition().above(2)).is(Blocks.OBSIDIAN)));
        clean();

        // AutoArmor
        cmd("give @a diamond_helmet", "give @a netherite_chestplate", "give @a iron_boots");
        set("AutoArmor", true);
        check("AutoArmor puts armor on", until(60, mc -> mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE) && mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.IRON_BOOTS)));
        clean();

        // AutoEat
        cmd("give @a cooked_beef 8", "effect give @a hunger 8 255");
        c.waitTicks(150);
        int food = c.computeOnClient(mc -> mc.player.getFoodData().getFoodLevel());
        cmd("effect clear @a");
        set("AutoEat", true);
        check("AutoEat eats (was " + food + ")", food < 15 && until(200, mc -> mc.player.getFoodData().getFoodLevel() > food + 3));
        clean();
        cmd("effect give @a saturation 1 20");

        // NoFall
        set("NoFall", true);
        float hp = health();
        cmd("tp @a 0.5 -42 0.5");
        c.waitTicks(60);
        check("NoFall prevents damage", health() >= hp - .01f);
        set("NoFall", false);
        clean();

        // Velocity
        cmd("summon zombie 0.5 -60 1.6 {Silent:1b,NoAI:1b}");
        set("Velocity", true);
        c.waitTicks(5);
        double x = c.computeOnClient(mc -> mc.player.getX()), zz = c.computeOnClient(mc -> mc.player.getZ());
        cmd("damage @p 2 mob_attack by @e[type=zombie,limit=1]");
        c.waitTicks(10);
        check("Velocity cancels knockback", c.computeOnClient(mc -> Math.hypot(mc.player.getX() - x, mc.player.getZ() - zz)) < .05);
        clean();

        // Scaffold
        cmd("fill -12 -61 -12 12 -61 12 air", "setblock 0 -61 0 stone", "tp @a 0.5 -60 0.5 0 60", "give @a cobblestone 64", "fill -12 -70 -12 12 -62 12 air");
        c.waitTicks(5);
        set("Scaffold", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 50);
        check("Scaffold bridges without falling", c.computeOnClient(mc -> mc.player.getY() > -60.6 && mc.player.getZ() > 2));
        set("Scaffold", false);
        cmd("fill -12 -64 -12 12 -62 12 dirt", "fill -12 -61 -12 12 -61 12 grass_block");
        clean();

        // Nuker, VeinMiner
        cmd("fill 1 -60 1 2 -59 2 dirt");
        option("Nuker", "Shape", "Above my feet");
        set("Nuker", true);
        check("Nuker clears dirt", until(300, mc -> mc.level.getBlockState(new BlockPos(1, -60, 1)).isAir() && mc.level.getBlockState(new BlockPos(2, -59, 2)).isAir()));
        clean();
        cmd("fill 0 -60 2 0 -57 2 oak_log", "give @a diamond_axe");
        c.waitTicks(3);
        set("VeinMiner", true);
        c.runOnClient(mc -> mc.gameMode.startDestroyBlock(new BlockPos(0, -60, 2), net.minecraft.core.Direction.NORTH));
        check("VeinMiner fells the trunk", until(260, mc -> mc.level.getBlockState(new BlockPos(0, -58, 2)).isAir() && mc.level.getBlockState(new BlockPos(0, -60, 2)).isAir()));
        clean();

        // AutoTool
        cmd("item replace entity @a hotbar.0 with diamond_sword", "item replace entity @a hotbar.3 with diamond_pickaxe", "setblock 0 -60 2 stone");
        c.waitTicks(3);
        set("AutoTool", true);
        c.runOnClient(mc -> mc.gameMode.startDestroyBlock(new BlockPos(0, -60, 2), net.minecraft.core.Direction.NORTH));
        check("AutoTool takes the pickaxe", c.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == 3);
        clean();

        // Step
        cmd("fill -3 -60 2 3 -60 6 stone", "tp @a 0.5 -60 0.5 0 0");
        c.waitTicks(3);
        set("Step", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 25);
        check("Step climbs a block", c.computeOnClient(mc -> mc.player.getY()) > -59.2);
        clean();

        // Flight, Spider, HighJump, Jesus
        set("Flight", true);
        c.getInput().holdKeyFor(o -> o.keyJump, 20);
        check("Flight climbs", c.computeOnClient(mc -> mc.player.getY()) > -55);
        set("Flight", false);
        set("NoFall", true);
        c.waitTicks(60);
        clean();
        cmd("fill -3 -60 3 3 -50 3 stone", "tp @a 0.5 -60 1.5 0 0");
        c.waitTicks(3);
        set("Spider", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 40);
        check("Spider climbs a wall", c.computeOnClient(mc -> mc.player.getY()) > -58);
        set("NoFall", true);
        set("Spider", false);
        c.waitTicks(40);
        clean();
        cmd("fill -8 -61 -8 8 -61 8 water", "tp @a 0.5 -59.5 0.5");
        set("Jesus", true);
        c.waitTicks(40);
        check("Jesus stands on water", c.computeOnClient(mc -> mc.player.getY()) > -60.4);
        set("Jesus", false);
        clean();

        // SafeWalk
        cmd("fill -3 -60 -3 3 -60 3 stone", "tp @a 0.5 -59 0.5 0 0");
        c.waitTicks(3);
        set("SafeWalk", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 60);
        check("SafeWalk stays on the edge", c.computeOnClient(mc -> mc.player.getY()) > -59.2);
        clean();

        // ChestStealer
        cmd("setblock 0 -60 2 chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:5},{Slot:9b,id:\"minecraft:emerald\",count:3}]}", "tp @a 0.5 -60 0.5 0 30");
        c.waitTicks(5);
        set("ChestStealer", true);
        c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(.5, -59.5, 2), net.minecraft.core.Direction.NORTH, new BlockPos(0, -60, 2), false)));
        check("ChestStealer empties a chest", until(80, mc -> mc.player.getInventory().contains(s -> s.is(Items.DIAMOND)) && mc.player.getInventory().contains(s -> s.is(Items.EMERALD))));
        check("ChestStealer closes it", until(40, mc -> mc.gui.screen() == null));
        clean();

        // ChestSwap
        cmd("item replace entity @a armor.chest with netherite_chestplate", "item replace entity @a inventory.3 with elytra");
        c.waitTicks(5);
        set("ChestSwap", true);
        c.waitTicks(5);
        check("ChestSwap puts the elytra on", c.computeOnClient(mc -> mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA)));
        clean();

        // Replenish
        cmd("item replace entity @a hotbar.0 with obsidian 3", "item replace entity @a inventory.2 with obsidian 64");
        set("Replenish", true);
        check("Replenish tops up", until(40, mc -> mc.player.getInventory().getItem(0).getCount() > 30));
        clean();

        // Freecam, Zoom
        cmd("tp @a 0.5 -60 0.5 0 0");
        c.waitTicks(3);
        set("Freecam", true);
        c.getInput().holdKeyFor(o -> o.keyUp, 20);
        check("Freecam moves the camera, not the player", c.computeOnClient(mc -> mc.gameRenderer.mainCamera().position().distanceTo(mc.player.getEyePosition()) > 2 && Math.abs(mc.player.getZ() - .5) < .1));
        c.takeScreenshot("freecam");
        set("Freecam", false);
        clean();
        float fov = c.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
        set("Zoom", true);
        c.waitTicks(20);
        check("Zoom narrows the view", c.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov()) < fov / 2);
        set("Zoom", false);
        clean();

        // Timer
        set("Timer", true);
        option("Timer", "Speed", 1);
        set("Timer", false);

        // Render modules together
        cmd("summon zombie 3 -60 8 {NoAI:1b}", "summon sheep -3 -60 8 {NoAI:1b}", "setblock 2 -60 5 chest", "setblock -2 -60 5 ender_chest", "setblock 0 -60 6 ancient_debris", "summon item -1 -60 4 {Item:{id:\"minecraft:diamond\",count:3}}", "tp @a 0.5 -60 0.5 0 10", "give @a bow");
        for (String n : new String[] {"Tracers", "StorageESP", "BlockESP", "ItemESP", "ESP", "Crosshair", "TargetRing", "Trajectories", "Nametags", "BlockOutline", "ChunkBorders", "FakePlayer"}) set(n, true);
        option("ESP", "Monsters", true);
        option("TargetRing", "Mobs", true);
        option("Tracers", "Monsters", true);
        c.waitTicks(5);
        cmd("tp @a 0.5 -60 -3 0 10");
        c.waitTicks(60);
        c.takeScreenshot("render");
        check("BlockESP finds debris", c.computeOnClient(mc -> Integer.parseInt(mod("BlockESP").info()) >= 1));
        check("StorageESP finds both chests", c.computeOnClient(mc -> Integer.parseInt(mod("StorageESP").info()) >= 2));
        clean();

        // Commands stay local; waypoints
        c.runOnClient(mc -> mc.player.connection.sendChat(".waypoint add Base"));
        c.waitTicks(3);
        check("waypoint command saves", c.computeOnClient(mc -> Hollow.module(dev.hollow.module.Waypoints.class).here().stream().anyMatch(p -> p.name().equals("Base"))));
        cmd("tp @a 20 -60 20 135 0");
        c.waitTicks(10);
        c.takeScreenshot("waypoint");
        clean();

        // Ambience
        set("Ambience", true);
        c.waitTicks(10);
        c.takeScreenshot("ambience");
        clean();

        // Panic
        set("Sprint", true);
        set("ESP", true);
        set("Panic", true);
        check("Panic switches everything off", c.computeOnClient(mc -> Hollow.modules().stream().noneMatch(m -> m.on() && m != Hollow.INTERFACE && m != Hollow.HUD)));

        // Profiles
        c.runOnClient(mc -> {
            mod("Sprint").set(true);
            Hollow.saveProfile("Test one");
            mod("Sprint").set(false);
            Hollow.loadProfile("Test one");
        });
        check("a profile brings modules back", c.computeOnClient(mc -> mod("Sprint").on() && Hollow.profiles().contains("Test one")));
        c.runOnClient(mc -> Hollow.deleteProfile("Test one"));
        check("a profile can be deleted", c.computeOnClient(mc -> !Hollow.profiles().contains("Test one")));
        clean();
    }
}
