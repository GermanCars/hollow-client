package dev.hollow.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

/** One configurable value. Loading a bad value keeps the current one. */
public abstract sealed class Setting {
    public final String name;
    /** Heading this setting sits under, or empty. */
    public String section = "";
    private BooleanSupplier visible = () -> true;

    private Setting(String name) { this.name = name; }

    public abstract JsonElement save();

    public abstract void load(JsonElement json);

    public abstract void reset();

    public final boolean visible() { return visible.getAsBoolean(); }

    final void showWhen(BooleanSupplier when) { visible = when; }

    public static final class Bool extends Setting {
        private final boolean initial;
        private boolean value;

        public Bool(String name, boolean value) { super(name); this.value = initial = value; }

        public boolean get() { return value; }

        public void set(boolean v) { value = v; }

        @Override public JsonElement save() { return new JsonPrimitive(value); }

        @Override public void load(JsonElement j) { if (j instanceof JsonPrimitive p && p.isBoolean()) value = p.getAsBoolean(); }

        @Override public void reset() { value = initial; }
    }

    public static final class Num extends Setting {
        public final double min, max, step;
        private final double initial;
        private double value;

        public Num(String name, double value, double min, double max, double step) {
            super(name);
            this.min = min;
            this.max = max;
            this.step = step;
            set(value);
            initial = this.value;
        }

        public double get() { return value; }

        public float getF() { return (float) value; }

        public int getInt() { return (int) Math.round(value); }

        public void set(double v) {
            if (!Double.isFinite(v)) return;
            double snapped = min + Math.round((v - min) / step) * step;
            value = Math.clamp(Math.round(snapped * 1000) / 1000.0, min, max);
        }

        public String text() { return value == Math.rint(value) ? Integer.toString((int) value) : Double.toString(value); }

        @Override public JsonElement save() { return new JsonPrimitive(value); }

        @Override public void load(JsonElement j) { if (j instanceof JsonPrimitive p && p.isNumber()) set(p.getAsDouble()); }

        @Override public void reset() { value = initial; }
    }

    public static final class Choice extends Setting {
        public final List<String> options;
        private int index;

        public Choice(String name, String... options) { super(name); this.options = List.of(options); }

        public String get() { return options.get(index); }

        public int index() { return index; }

        public boolean is(String option) { return get().equals(option); }

        public void pick(int i) { index = Math.clamp(i, 0, options.size() - 1); }

        public void cycle(int by) { index = Math.floorMod(index + by, options.size()); }

        @Override public JsonElement save() { return new JsonPrimitive(get()); }

        @Override public void load(JsonElement j) {
            if (j instanceof JsonPrimitive p && p.isString() && options.contains(p.getAsString())) index = options.indexOf(p.getAsString());
        }

        @Override public void reset() { index = 0; }
    }

    /** Hue, saturation and value are kept as they were picked, so dragging through black doesn't lose the hue. */
    public static final class Color extends Setting {
        public float hue, sat, val, alpha;
        /** Use the theme's accent instead. */
        public boolean theme;
        private final int initial;
        private final boolean initialTheme;

        public Color(String name, int argb, boolean theme) {
            super(name);
            initial = argb;
            this.theme = initialTheme = theme;
            set(argb);
        }

        public void set(int argb) {
            float[] h = Colors.toHsv(argb);
            hue = h[0];
            sat = h[1];
            val = h[2];
            alpha = (argb >>> 24) / 255f;
        }

        public int own() { return Colors.hsv(hue, sat, val, alpha); }

        @Override public JsonElement save() {
            JsonObject o = new JsonObject();
            o.addProperty("argb", own());
            o.addProperty("theme", theme);
            return o;
        }

        @Override public void load(JsonElement j) {
            if (!(j instanceof JsonObject o)) return;
            if (o.get("argb") instanceof JsonPrimitive p && p.isNumber()) set(p.getAsInt());
            if (o.get("theme") instanceof JsonPrimitive p && p.isBoolean()) theme = p.getAsBoolean();
        }

        @Override public void reset() {
            set(initial);
            theme = initialTheme;
        }
    }

    public static final class Text extends Setting {
        public static final int MAX = 120;
        private final String initial;
        private String value;

        public Text(String name, String value) { super(name); this.value = initial = value; }

        public String get() { return value; }

        public void set(String v) { value = v.length() > MAX ? v.substring(0, MAX) : v; }

        @Override public JsonElement save() { return new JsonPrimitive(value); }

        @Override public void load(JsonElement j) { if (j instanceof JsonPrimitive p && p.isString()) set(p.getAsString()); }

        @Override public void reset() { value = initial; }
    }

    /** A set of block, item or entity ids. */
    public static final class Ids extends Setting {
        public enum Kind { BLOCK, ITEM, ENTITY }

        public final Kind kind;
        private final List<String> initial;
        private final Set<String> value = new TreeSet<>();
        /** Goes up with every change, so users of the set can cache. */
        public int version;

        public Ids(String name, Kind kind, String... ids) {
            super(name);
            this.kind = kind;
            initial = List.of(ids).stream().map(s -> s.contains(":") ? s : "minecraft:" + s).toList();
            value.addAll(initial);
        }

        public boolean has(String id) { return value.contains(id); }

        public Set<String> all() { return value; }

        public void toggle(String id) {
            if (!value.remove(id)) value.add(id);
            version++;
        }

        @Override public JsonElement save() {
            JsonArray a = new JsonArray();
            value.forEach(a::add);
            return a;
        }

        @Override public void load(JsonElement j) {
            if (!(j instanceof JsonArray a)) return;
            value.clear();
            for (JsonElement e : a) if (e.isJsonPrimitive()) value.add(e.getAsString());
            version++;
        }

        @Override public void reset() {
            value.clear();
            value.addAll(initial);
            version++;
        }
    }
}
