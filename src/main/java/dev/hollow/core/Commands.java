package dev.hollow.core;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Dot commands. Returns the reply to show locally, or empty when the text is ordinary chat. */
public final class Commands {
    private final List<Module> modules;
    private final Friends friends;
    private final ToIntFunction<String> keyLookup;
    private final Function<String[], String> waypoint;

    public Commands(List<Module> modules, Friends friends, ToIntFunction<String> keyLookup, Function<String[], String> waypoint) {
        this.modules = modules; this.friends = friends; this.keyLookup = keyLookup;
        this.waypoint = waypoint;
    }

    public Optional<String> run(String text) {
        if (text.length() < 2 || text.charAt(0) != '.' || !Character.isLetter(text.charAt(1))) return Optional.empty();
        String[] a = text.substring(1).strip().split("\\s+");
        return Optional.of(switch (a[0].toLowerCase(Locale.ROOT)) {
            case "help" -> ".friend add|remove|list <name>, .toggle <module>, .bind <module> <key|none>, .waypoint add|remove|list <name>";
            case "friend" -> friend(a);
            case "toggle", "t" -> toggle(a);
            case "bind" -> bind(a);
            case "waypoint", "wp" -> waypoint.apply(a);
            default -> "Unknown command. Type .help for the list.";
        });
    }

    private String toggle(String[] a) {
        if (a.length != 2) return "Usage: .toggle <module>";
        Optional<Module> m = find(a[1]);
        if (m.isEmpty()) return "No module called " + a[1];
        m.get().toggle();
        return m.get().name + (m.get().on() ? " on" : " off");
    }

    private String friend(String[] a) {
        if (a.length == 2 && a[1].equals("list")) return friends.all().isEmpty() ? "No friends added yet" : String.join(", ", friends.all());
        if (a.length != 3) return "Usage: .friend add|remove|list <name>";
        return switch (a[1]) {
            case "add" -> friends.add(a[2]) ? "Added " + a[2] : a[2] + " is already a friend";
            case "remove" -> friends.remove(a[2]) ? "Removed " + a[2] : a[2] + " isn't a friend";
            default -> "Usage: .friend add|remove|list <name>";
        };
    }

    private String bind(String[] a) {
        if (a.length != 3) return "Usage: .bind <module> <key|none>";
        Optional<Module> m = find(a[1]);
        if (m.isEmpty()) return "No module called " + a[1];
        int key = a[2].equalsIgnoreCase("none") ? 0 : keyLookup.applyAsInt(a[2]);
        if (key < 0) return "No key called " + a[2];
        m.get().key = key;
        return key == 0 ? m.get().name + " unbound" : m.get().name + " bound to " + a[2];
    }

    private Optional<Module> find(String name) { return modules.stream().filter(m -> m.name.equalsIgnoreCase(name)).findFirst(); }
}
