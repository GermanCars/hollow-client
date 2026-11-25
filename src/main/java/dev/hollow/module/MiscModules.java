package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.hud.Notices;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;

/** Chat, server and everything else. */
public final class MiscModules {
    private MiscModules() {}

    public static final class AutoReconnect extends Module {
        public final Setting.Num seconds = num("After seconds", 5, 1, 60, 1);
        private ServerData server;

        public AutoReconnect() { super(Category.MISC, "AutoReconnect", "Joins again after you are disconnected."); }

        public void connecting(ServerData s) { server = s; }

        public boolean ready() { return server != null; }

        public void reconnect() { if (server != null) ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null); }
    }

    public static final class VisualRange extends Module {
        private final Setting.Choice where = choice("Tell me in", "Pop-ups", "Chat");
        private final Setting.Bool leave = bool("Leaving too", true), friends = bool("Friends too", false), sound = bool("Sound", true);
        private final Map<UUID, String> seen = new HashMap<>();

        public VisualRange() { super(Category.MISC, "VisualRange", "Tells you when players come into view."); }

        @Override public void onTick() {
            Map<UUID, String> now = new HashMap<>();
            for (AbstractClientPlayer p : mc.level.players()) if (p != mc.player) now.put(p.getUUID(), p.getScoreboardName());
            now.forEach((id, n) -> { if (!seen.containsKey(id)) say(n, "Came into view", true); });
            seen.forEach((id, n) -> { if (!now.containsKey(id) && leave.get()) say(n, "Left your view", false); });
            seen.clear();
            seen.putAll(now);
        }

        private void say(String who, String what, boolean arrived) {
            if (!friends.get() && Hollow.FRIENDS.has(who)) return;
            if (where.is("Chat")) Hollow.tell(who + ": " + what.toLowerCase(java.util.Locale.ROOT));
            else Notices.add(who, what, !arrived);
            if (sound.get() && arrived) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1));
        }

        @Override public void onWorldLeft() { seen.clear(); }
    }

    public static final class ChatTag extends Module {
        private final Setting.Text tag = text("Tag", "Hollow Client");
        private final Setting.Choice divider = choice("Divider", "|", "»", "-");

        public ChatTag() { super(Category.MISC, "ChatTag", "Adds Hollow Client to the end of your chat messages."); }

        public String add(String msg) {
            if (!on() || tag.get().isBlank() || msg.startsWith("/") || msg.startsWith("!") || msg.startsWith("#")) return msg;
            String out = msg + " " + divider.get() + " " + tag.get();
            return out.length() > 256 ? msg : out;
        }
    }

    public static final class ChatTimes extends Module {
        private final Setting.Bool seconds = bool("Seconds", false);

        public ChatTimes() { super(Category.MISC, "ChatTimes", "Shows when each chat message arrived."); }

        public Component stamp(Component c) {
            return on() ? Component.literal("[" + LocalTime.now().format(DateTimeFormatter.ofPattern(seconds.get() ? "HH:mm:ss" : "HH:mm")) + "] ").withStyle(ChatFormatting.DARK_GRAY).append(Component.empty().withStyle(ChatFormatting.WHITE).append(c)) : c;
        }
    }

    public static final class Mentions extends Module {
        private final Setting.Text words = text("Words, with commas between", "");

        public Mentions() { super(Category.MISC, "Mentions", "Plays a sound when someone says your name or words you choose."); }

        public void chat(String s) {
            if (!on() || mc.player == null) return;
            String low = s.toLowerCase(java.util.Locale.ROOT), me = mc.getUser().getName().toLowerCase(java.util.Locale.ROOT);
            // Your own messages start with your name.
            boolean hit = low.indexOf(me, Math.min(low.length(), me.length() + 2)) >= 0;
            for (String w : words.get().split(",")) if (!w.isBlank() && low.contains(w.strip().toLowerCase(java.util.Locale.ROOT))) hit = true;
            if (!hit) return;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 1));
            Notices.add("Mentioned in chat", s.length() > 40 ? s.substring(0, 40) : s, true);
        }
    }

    public static final class BrandSpoof extends Module {
        private final Setting.Text brand = text("Brand", "vanilla");

        public BrandSpoof() { super(Category.MISC, "BrandSpoof", "Tells servers you use another client."); }

        public String brand(String normal) { return on() ? brand.get() : normal; }
    }

    public static final class NoToasts extends Module {
        public NoToasts() { super(Category.MISC, "NoToasts", "Hides the game's own pop-ups, such as advancements."); }
    }

    public static final class BetterTab extends Module {
        public final Setting.Bool ping = bool("Ping numbers", true), friends = bool("Highlight friends", true);
        public final Setting.Num most = num("Most players", 160, 20, 400, 10);

        public BetterTab() { super(Category.MISC, "BetterTab", "More players and real ping in the player list."); }
    }

    public static final class SoundFilter extends Module {
        private final Setting.Bool wither = bool("Wither", true), dragon = bool("Ender dragon", true), thunder = bool("Thunder", true), portal = bool("Portals", true), pistons = bool("Pistons", false), withers = bool("End portal opening", true), villagers = bool("Villagers", false), explosions = bool("Explosions", false);

        public SoundFilter() { super(Category.MISC, "SoundFilter", "Mutes sounds that get annoying."); }

        public boolean mutes(SoundInstance s) {
            if (!on()) return false;
            String p = s.getIdentifier().getPath();
            return wither.get() && p.startsWith("entity.wither.") || dragon.get() && p.startsWith("entity.ender_dragon.") || thunder.get() && p.startsWith("entity.lightning_bolt.") || portal.get() && p.startsWith("block.portal.") || pistons.get() && p.startsWith("block.piston.") || withers.get() && p.startsWith("block.end_portal.") || villagers.get() && p.startsWith("entity.villager.") || explosions.get() && p.equals("entity.generic.explode");
        }
    }

    public static final class DeathSpot extends Module {
        private final Setting.Bool waypoint = bool("Save a waypoint", true);
        private boolean was;

        public DeathSpot() { super(Category.MISC, "DeathSpot", "Remembers where you died."); }

        public void tick() {
            boolean dead = mc.player != null && mc.player.isDeadOrDying();
            if (on() && dead && !was) {
                Hollow.tell("You died at " + mc.player.getBlockX() + ", " + mc.player.getBlockY() + ", " + mc.player.getBlockZ());
                if (waypoint.get()) Hollow.module(Waypoints.class).add("Death");
            }
            was = dead;
        }
    }

    public static final class Panic extends Module {
        public Panic() {
            super(Category.MISC, "Panic", "Switches every module off at once.");
            hidden = true;
        }

        @Override protected void onEnable() { Hollow.modules().forEach(m -> { if (m.category != Category.CLIENT && m != Hollow.HUD) m.set(false); }); }
    }

    public static final class PearlAlert extends Module {
        private final Set<Integer> seen = new HashSet<>();

        public PearlAlert() { super(Category.MISC, "PearlAlert", "Tells you when someone throws an ender pearl and which way."); }

        @Override public void onTick() {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof ThrownEnderpearl p) || !seen.add(e.getId()) || p.getOwner() == mc.player) continue;
                Player who = p.getOwner() instanceof Player o ? o : mc.level.getNearestPlayer(e, 4);
                Notices.add(who == null ? "Someone" : who.getScoreboardName(), "Pearled " + net.minecraft.core.Direction.getApproximateNearest(p.getDeltaMovement().multiply(1, 0, 1)).getName(), true);
            }
            if (seen.size() > 500) seen.clear();
        }

        @Override public void onWorldLeft() { seen.clear(); }
    }

    public static final class Macro extends Module {
        private final Setting.Text send = text("Sends", "");

        public Macro(int n) {
            super(Category.MISC, "Macro " + n, "Sends a message or /command when you press its key.");
            hidden = true;
        }

        @Override protected void onEnable() {
            String s = send.get().strip();
            if (s.isEmpty()) Hollow.tell("Right click " + name + " to choose what it sends");
            else if (s.startsWith("/")) mc.player.connection.sendCommand(s.substring(1));
            else mc.player.connection.sendChat(s);
            set(false);
        }
    }

    public static final class LagBack extends Module {
        public LagBack() { super(Category.MISC, "LagBack", "Tells you when the server pulls you back."); }

        @Override public boolean onReceive(net.minecraft.network.protocol.Packet<?> p) {
            if (p instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket) mc.execute(() -> Notices.add("Pulled back", "By the server", false));
            return false;
        }
    }

    public static final class AutoMessage extends Module {
        private final Setting.Text line = text("Message", "");
        private final Setting.Num minutes = num("Every minutes", 10, 1, 120, 1);
        private int ticks;

        public AutoMessage() { super(Category.MISC, "AutoMessage", "Says something now and then. Check that the server allows it."); }

        @Override protected void onEnable() { ticks = 0; }

        @Override public void onTick() {
            if (line.get().isBlank() || ++ticks < minutes.get() * 1200) return;
            ticks = 0;
            mc.player.connection.sendChat(line.get());
        }
    }

    public static final class JoinNotice extends Module {
        private final Set<UUID> online = new HashSet<>();
        private boolean first = true;

        public JoinNotice() { super(Category.MISC, "FriendAlert", "Tells you when friends join or leave the server."); }

        @Override public void onTick() {
            if (mc.player.tickCount % 20 != 0) return;
            Set<UUID> now = new HashSet<>();
            for (var i : mc.getConnection().getOnlinePlayers()) {
                String n = i.getProfile().name();
                if (!Hollow.FRIENDS.has(n)) continue;
                now.add(i.getProfile().id());
                if (!first && !online.contains(i.getProfile().id())) Notices.add(n, "Joined the server", true);
            }
            first = false;
            online.clear();
            online.addAll(now);
        }

        @Override public void onWorldLeft() {
            online.clear();
            first = true;
        }
    }

    public static List<Module> all() {
        return List.of(new AutoReconnect(), new VisualRange(), new ChatTag(), new ChatTimes(), new Mentions(), new BrandSpoof(), new NoToasts(), new BetterTab(), new SoundFilter(), new DeathSpot(), new PearlAlert(), new LagBack(), new AutoMessage(), new JoinNotice(), new Macro(1), new Macro(2), new Macro(3), new Panic());
    }
}
