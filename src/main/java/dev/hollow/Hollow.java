package dev.hollow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.action.Actions;
import dev.hollow.build.AutoBuild;
import dev.hollow.build.Printer;
import dev.hollow.build.Scaffold;
import dev.hollow.core.Commands;
import dev.hollow.core.Config;
import dev.hollow.core.Friends;
import dev.hollow.core.Module;
import dev.hollow.hud.HudElement;
import dev.hollow.hud.HudElements;
import dev.hollow.hud.Notices;
import dev.hollow.hud.Stats;
import dev.hollow.module.*;
import dev.hollow.ui.ClickGui;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Hollow implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Hollow");
    public static final Friends FRIENDS = new Friends();
    public static final Interface INTERFACE = new Interface();
    public static final AutoTotem AUTO_TOTEM = new AutoTotem();
    public static final Velocity VELOCITY = new Velocity();
    public static final Step STEP = new Step();
    public static final NoSlow NO_SLOW = new NoSlow();
    public static final NoFall NO_FALL = new NoFall();
    public static final ElytraFly ELYTRA_FLY = new ElytraFly();
    public static final Fullbright FULLBRIGHT = new Fullbright();
    public static final HudModule HUD = new HudModule();
    public static final Printer PRINTER = new Printer();
    public static final Scaffold SCAFFOLD = new Scaffold();
    public static final List<HudElement> HUD_ELEMENTS = HudElements.all();
    public static final HudElement EFFECTS = HUD_ELEMENTS.stream().filter(e -> e.name.equals("Effects")).findFirst().orElseThrow();

    private static final List<Module> MODULES = Stream.of(
        List.<Module>of(new KillAura(), new CrystalAura(), new AnchorAura(), AUTO_TOTEM, new Surround(), new HoleFill(), new AutoTrap(), VELOCITY), CombatModules.all(),
        List.<Module>of(new Offhand(), new AutoArmor(), new AutoEat()), PlayerModules.all(),
        List.<Module>of(new Sprint(), STEP, NO_SLOW, NO_FALL, ELYTRA_FLY), MoveModules.all(),
        List.<Module>of(new Esp(), new HoleEsp(), FULLBRIGHT, HUD, new Waypoints()), RenderModules.all(),
        List.<Module>of(PRINTER, new AutoBuild(), SCAFFOLD), WorldModules.all(),
        MiscModules.all(), List.<Module>of(INTERFACE)).flatMap(List::stream).sorted(java.util.Comparator.comparing((Module m) -> m.category).thenComparing(m -> m.name.toLowerCase(java.util.Locale.ROOT))).toList();
    private static final java.util.Map<Class<?>, Module> BY_CLASS = new java.util.HashMap<>();
    static { MODULES.forEach(m -> BY_CLASS.putIfAbsent(m.getClass(), m)); }
    /** Everything that is saved: modules, HUD elements and the client's own settings. */
    private static final List<Module> SAVED = Stream.<Module>concat(MODULES.stream(), HUD_ELEMENTS.stream()).toList();
    private static final Commands COMMANDS = new Commands(MODULES, FRIENDS, Hollow::keyCode, Hollow::waypoint);
    private static Config config;
    private static boolean inWorld;

    @Override public void onInitializeClient() {
        dev.hollow.render.Pipelines.init();
        Module.listener = Hollow::switched;
        config = new Config(FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("hollow.json"));
        try {
            if (!config.load(SAVED, FRIENDS)) LOG.warn("hollow.json couldn't be read. It was kept as a .broken file and defaults were loaded.");
        } catch (IOException e) {
            LOG.error("Couldn't load hollow.json", e);
        }
    }

    /** The one module of a class. */
    public static <T extends Module> T module(Class<T> c) { return c.cast(BY_CLASS.get(c)); }

    public static boolean started() { return config != null; }

    /** Network thread. True drops the packet. */
    public static boolean received(Connection c, Packet<?> p) {
        if (!mine(c)) return false;
        Stats.received(p);
        boolean drop = false;
        for (Module m : MODULES) if (m.on()) drop |= m.onReceive(p);
        return drop;
    }

    public static boolean sending(Connection c, Packet<?> p) {
        if (!mine(c)) return false;
        boolean drop = false;
        for (Module m : MODULES) if (m.on()) drop |= m.onSend(p);
        return drop;
    }

    /** Singleplayer has a server end in the same game; only the player's end counts. */
    private static boolean mine(Connection c) {
        var l = Minecraft.getInstance().getConnection();
        return l != null && l.getConnection() == c;
    }

    public static void overlay(GuiGraphicsExtractor g, float partial) {
        if (ready()) for (Module m : MODULES) if (m.on()) m.onOverlay(g, partial);
    }

    public static void keyUp(int key) {
        for (Module m : MODULES) if (m.hold && m.key == key && key != 0) m.set(false);
    }

    public static long moduleCount() { return MODULES.stream().filter(m -> dev.hollow.core.Category.COLUMNS.contains(m.category)).count(); }

    public static List<String> profiles() { return config.profiles(); }

    public static boolean saveProfile(String name) {
        if (Config.cleanName(name).isEmpty()) return false;
        try {
            config.saveProfile(name, SAVED, FRIENDS);
            return true;
        } catch (IOException e) {
            LOG.error("Couldn't save the profile", e);
            return false;
        }
    }

    public static void loadProfile(String name) {
        try {
            config.loadProfile(name, SAVED, FRIENDS);
        } catch (IOException e) {
            LOG.error("Couldn't load the profile", e);
        }
    }

    public static void deleteProfile(String name) {
        try {
            config.deleteProfile(name);
        } catch (IOException e) {
            LOG.error("Couldn't delete the profile", e);
        }
    }

    private static void switched(Module m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || m.category == dev.hollow.core.Category.HUD || m.category == dev.hollow.core.Category.CLIENT) return;
        if (INTERFACE.toasts.get() && !m.hold) Notices.add(m.name, m.on() ? "On" : "Off", m.on());
        if (INTERFACE.chatToggles.get()) tell(m.name + (m.on() ? " on" : " off"));
    }

    public static List<Module> modules() { return MODULES; }

    public static String version() {
        return FabricLoader.getInstance().getModContainer("hollow").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    public static void save() {
        try {
            config.save(SAVED, FRIENDS);
        } catch (IOException e) {
            LOG.error("Couldn't save hollow.json", e);
        }
    }

    /** A message only this player sees. */
    public static void tell(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(Component.literal("§dHollow§r " + text));
    }

    private static String waypoint(String[] a) {
        Waypoints w = module(Waypoints.class);
        if (Minecraft.getInstance().player == null) return "Join a world first";
        String name = String.join(" ", java.util.Arrays.copyOfRange(a, Math.min(2, a.length), a.length));
        if (a.length >= 2 && a[1].equals("list")) return w.here().isEmpty() ? "No waypoints here" : String.join(", ", w.here().stream().map(p -> p.name() + " (" + p.x() + " " + p.y() + " " + p.z() + ")").toList());
        if (name.isEmpty()) return "Usage: .waypoint add|remove|list <name>";
        if (a[1].equals("add")) {
            w.add(name);
            w.set(true);
            return "Saved waypoint " + name;
        }
        if (a[1].equals("remove")) return w.remove(name) ? "Removed waypoint " + name : "No waypoint called " + name;
        return "Usage: .waypoint add|remove|list <name>";
    }

    private static int keyCode(String name) {
        try {
            return InputConstants.getKey("key.keyboard." + name.toLowerCase(java.util.Locale.ROOT).replace('_', '.')).getValue();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /** True when the text was a command and must not reach the server. */
    public static boolean command(String text) {
        Optional<String> reply = COMMANDS.run(text);
        reply.ifPresent(r -> {
            tell(r);
            save();
        });
        return reply.isPresent();
    }

    public static void keyDown(int key) {
        if (key == 0) return;
        if (key == InputConstants.KEY_RSHIFT) {
            Minecraft.getInstance().gui.setScreen(new ClickGui());
            return;
        }
        for (Module m : MODULES) if (m.key == key) {
            if (m.hold) m.set(true);
            else {
                m.toggle();
                save();
            }
        }
    }

    public static void clientTick() {
        boolean now = Minecraft.getInstance().player != null;
        module(PlayerModules.AutoRespawn.class).deadTick();
        module(MiscModules.DeathSpot.class).tick();
        if (!inWorld && now) {
            Stats.joined();
            for (Module m : MODULES) if (m.on()) m.onWorldJoined();
        }
        if (inWorld && !now) {
            Actions.clear();
            for (Module m : MODULES) if (m.on()) m.onWorldLeft();
            save();
        }
        inWorld = now;
    }

    private static boolean ready() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null && mc.player.isAlive() && !mc.player.isSpectator();
    }

    public static void beforeMove() {
        if (ready()) for (Module m : MODULES) if (m.on()) m.onMove();
    }

    private static net.minecraft.world.phys.Vec3 lastPos = net.minecraft.world.phys.Vec3.ZERO;
    private static float lastYaw, lastPitch;

    /** A position or view that isn't a number gets the player kicked, so it never leaves the game. */
    private static void keepSane() {
        var p = Minecraft.getInstance().player;
        var v = p.getDeltaMovement();
        if (!Double.isFinite(v.x + v.y + v.z)) {
            LOG.warn("Speed stopped being a number: {}. On: {}", v, onNow());
            p.setDeltaMovement(0, 0, 0);
        }
        if (Double.isFinite(p.getX() + p.getY() + p.getZ())) lastPos = p.position();
        else {
            LOG.warn("Position stopped being a number: {}. On: {}", p.position(), onNow());
            p.setPos(lastPos);
        }
        if (Float.isFinite(p.getYRot()) && Float.isFinite(p.getXRot())) {
            lastYaw = p.getYRot();
            lastPitch = p.getXRot();
        } else {
            LOG.warn("View stopped being a number: {} {}. On: {}", p.getYRot(), p.getXRot(), onNow());
            p.setYRot(lastYaw);
            p.setXRot(lastPitch);
        }
    }

    private static List<String> onNow() { return MODULES.stream().filter(Module::on).map(m -> m.name).toList(); }

    public static void beforeSend() {
        Actions.clear();
        if (!ready()) return;
        keepSane();
        for (Module m : MODULES) if (m.on()) m.onTick();
        Actions.begin();
    }

    public static void afterSend() { Actions.finish(); }

    public static void worldRender(float partial) {
        if (!ready()) return;
        // Whatever is asking for this frame, the shapes belong to the level being drawn.
        try (var ignored = Minecraft.getInstance().levelExtractor.collectPerFrameMainThreadGizmos()) {
            for (Module m : MODULES) if (m.on()) m.onWorldRender(partial);
        }
    }
}
