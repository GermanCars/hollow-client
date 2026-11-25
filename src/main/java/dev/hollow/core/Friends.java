package dev.hollow.core;

import java.util.Set;
import java.util.TreeSet;

public final class Friends {
    private final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public boolean has(String name) { return names.contains(name); }

    public boolean add(String name) { return names.add(name.strip()); }

    public boolean remove(String name) { return names.remove(name); }

    public Set<String> all() { return names; }
}
