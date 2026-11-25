package dev.hollow.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Saves modules and friends to JSON, plus named profiles. An unreadable file is moved aside, never overwritten. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file, profiles;

    public Config(Path file) {
        this.file = file;
        profiles = file.resolveSibling("profiles");
    }

    public static String cleanName(String s) {
        String c = s.strip().replaceAll("[^A-Za-z0-9 _-]", "");
        return c.length() > 32 ? c.substring(0, 32) : c;
    }

    public List<String> profiles() {
        if (!Files.isDirectory(profiles)) return List.of();
        List<String> out = new ArrayList<>();
        try (var s = Files.list(profiles)) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5)).sorted().forEach(out::add);
        } catch (IOException e) {
            return List.of();
        }
        return out;
    }

    public void saveProfile(String name, List<Module> modules, Friends friends) throws IOException { write(profiles.resolve(cleanName(name) + ".json"), modules, friends); }

    public boolean loadProfile(String name, List<Module> modules, Friends friends) throws IOException { return read(profiles.resolve(cleanName(name) + ".json"), modules, friends, true); }

    public void deleteProfile(String name) throws IOException { Files.deleteIfExists(profiles.resolve(cleanName(name) + ".json")); }

    public void save(List<Module> modules, Friends friends) throws IOException { write(file, modules, friends); }

    /** Returns false when the file was unreadable and has been moved to a backup. */
    public boolean load(List<Module> modules, Friends friends) throws IOException { return read(file, modules, friends, false); }

    private static void write(Path file, List<Module> modules, Friends friends) throws IOException {
        JsonObject root = new JsonObject(), mods = new JsonObject();
        for (Module m : modules) {
            JsonObject o = new JsonObject(), s = new JsonObject();
            o.addProperty("on", m.on());
            o.addProperty("key", m.key);
            o.addProperty("hold", m.hold);
            o.addProperty("hidden", m.hidden);
            m.settings.forEach(x -> s.add(x.name, x.save()));
            o.add("settings", s);
            mods.add(m.name, o);
        }
        JsonArray names = new JsonArray();
        friends.all().forEach(names::add);
        root.add("modules", mods);
        root.add("friends", names);
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(root));
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean flag(JsonObject o, String name, boolean otherwise) {
        return o.get(name) instanceof JsonPrimitive p && p.isBoolean() ? p.getAsBoolean() : otherwise;
    }

    /** @param live run enable and disable hooks, for switching profiles while playing */
    private static boolean read(Path file, List<Module> modules, Friends friends, boolean live) throws IOException {
        if (!Files.exists(file)) return true;
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException | IOException e) {
            Files.move(file, file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis()));
            return false;
        }
        if (root.get("modules") instanceof JsonObject mods) {
            for (Module m : modules) {
                if (!(mods.get(m.name) instanceof JsonObject o)) continue;
                if (o.get("settings") instanceof JsonObject s) m.settings.forEach(x -> { if (s.has(x.name)) x.load(s.get(x.name)); });
                if (o.get("key") instanceof JsonPrimitive p && p.isNumber()) m.key = p.getAsInt();
                m.hold = flag(o, "hold", m.hold);
                m.hidden = flag(o, "hidden", m.hidden);
                boolean on = flag(o, "on", m.on());
                if (live) m.set(on);
                else m.restore(on);
            }
        }
        if (root.get("friends") instanceof JsonArray a) for (JsonElement e : a) if (e.isJsonPrimitive()) friends.add(e.getAsString());
        return true;
    }
}
