package dev.hollow.hud;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;

/** Numbers worked out from what the server sends. */
public final class Stats {
    private static final Map<Integer, Integer> POPS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> NAMES = new ConcurrentHashMap<>();
    private static final ArrayDeque<Long> CLICKS = new ArrayDeque<>();
    private static volatile long lastPacket = Util.getMillis(), lastTime, joined = Util.getMillis();
    private static volatile float tps = 20;
    private static int combo;

    private Stats() {}

    public static void joined() {
        joined = lastPacket = Util.getMillis();
        lastTime = 0;
        tps = 20;
        combo = 0;
        POPS.clear();
        NAMES.clear();
    }

    /** Network thread. */
    public static void received(Packet<?> p) {
        long now = Util.getMillis();
        lastPacket = now;
        if (p instanceof ClientboundSetTimePacket) {
            // Sent every 20 server ticks.
            if (lastTime > 0 && now > lastTime) tps += (Math.clamp(20_000f / (now - lastTime), 0, 20) - tps) * .25f;
            lastTime = now;
        }
    }

    public static float tps() { return tps; }

    public static long silence() { return Util.getMillis() - lastPacket; }

    public static int ping() {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo i = mc.getConnection() == null || mc.player == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return i == null ? 0 : i.getLatency();
    }

    public static String session() {
        long s = (Util.getMillis() - joined) / 1000;
        return s >= 3600 ? s / 3600 + "h " + s / 60 % 60 + "m" : s / 60 + "m " + s % 60 + "s";
    }

    public static void clicked() { CLICKS.add(Util.getMillis()); }

    public static int cps() {
        while (!CLICKS.isEmpty() && Util.getMillis() - CLICKS.peek() > 1000) CLICKS.poll();
        return CLICKS.size();
    }

    public static void hit() { combo++; }

    public static void gotHit() { combo = 0; }

    public static int combo() { return combo; }

    /** Returns the new count. */
    public static int popped(Entity e) {
        NAMES.put(e.getId(), e.getName().getString());
        return POPS.merge(e.getId(), 1, Integer::sum);
    }

    public static int pops(Entity e) { return POPS.getOrDefault(e.getId(), 0); }

    /** Forget a player who died; returns how many they had used. */
    public static int died(Entity e) {
        Integer n = POPS.remove(e.getId());
        return n == null ? 0 : n;
    }

    public static List<Map.Entry<String, Integer>> popList() {
        return POPS.entrySet().stream().map(e -> Map.entry(NAMES.getOrDefault(e.getKey(), "?"), e.getValue())).sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(8).toList();
    }
}
