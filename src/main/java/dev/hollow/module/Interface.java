package dev.hollow.module;

import dev.hollow.Hollow;
import dev.hollow.core.Category;
import dev.hollow.core.Colors;
import dev.hollow.core.Module;
import dev.hollow.core.Setting;
import dev.hollow.ui.Theme;

/** Look and behaviour of the client itself. Edited on the Theme page. */
public final class Interface extends Module {
    private static final String CUSTOM = "Custom";

    public final Setting.Choice preset;
    private final Setting.Color accent, second, ink;
    private final Setting.Bool rainbow;
    private final Setting.Num panel, frost, cut, speed, menuScale;
    private final Setting.Choice corners;
    private final Setting.Choice face;
    private final Setting.Bool everywhere;
    private final Setting.Bool reduceMotion;
    public final Setting.Bool containers, hotbar, chat, menuSnow, sounds, startMenu, widgets, aurora, skin, loading, pauseBrand, windowTitle, tooltips;
    public final Setting.Num snowfall;
    private final Setting.Choice scene, quality;
    public final Setting.Choice moduleStyle;
    public final Setting.Bool toasts, chatToggles, rotate, chatPrefix;

    public Interface() {
        super(Category.CLIENT, "Interface", "Look and behaviour of Hollow Client.");
        section("Colours");
        preset = choice("Preset", java.util.stream.Stream.concat(java.util.Arrays.stream(Theme.PRESETS).map(Theme.Preset::name), java.util.stream.Stream.of(CUSTOM)).toArray(String[]::new));
        accent = when(fixedColor("Accent", 0xFF8CF0CB), () -> preset.is(CUSTOM));
        second = when(fixedColor("Second colour", 0xFF7FB2FF), () -> preset.is(CUSTOM));
        ink = when(fixedColor("Panel colour", 0xFF0F171C), () -> preset.is(CUSTOM));
        rainbow = bool("Rainbow", false);
        panel = num("Glass darkness", .78, .1, 1, .02);
        frost = num("Frost", 1, 0, 1.6, .1);
        section("Shape and type");
        corners = choice("Corners", "Rounded", "Chipped");
        cut = num("Corner size", 7, 0, 10, .5);
        face = choice("Text font", "Urbanist", "Unbounded", "Minecraft");
        everywhere = bool("Font in the whole game", true);
        moduleStyle = choice("Enabled modules", "Fill", "Edge", "Diamond");
        menuScale = num("Menu size", 1, .7, 1.5, .05);
        section("Motion");
        speed = num("Animation speed", 1, .3, 3, .1);
        reduceMotion = bool("Reduce motion", false);
        section("Menus");
        startMenu = bool("Hollow start menu", true);
        scene = choice("Scene", "Ice cavern", "Snowy mountains");
        quality = choice("Cavern detail", "Medium", "High", "Low");
        aurora = bool("Aurora", true);
        snowfall = num("Snowfall", 1, 0, 2, .1);
        skin = bool("Show my skin", true);
        widgets = bool("Restyle game buttons", true);
        tooltips = bool("Restyle tooltips", true);
        containers = bool("Glass inventories", true);
        hotbar = bool("Glass hotbar", true);
        chat = bool("Glass chat", true);
        loading = bool("Hollow loading screen", true);
        pauseBrand = bool("Name on pause screen", true);
        windowTitle = bool("Name in window title", true);
        menuSnow = bool("Snow in module menu", true);
        section("Behaviour");
        sounds = bool("Menu sounds", true);
        toasts = bool("Toggle pop-ups", true);
        chatToggles = bool("Toggle messages in chat", false);
        chatPrefix = bool("Dot commands", true);
        rotate = bool("Rotate", true);
        set(true);
    }

    private static Interface i() { return Hollow.INTERFACE; }

    private Theme.Preset picked() { return preset.index() < Theme.PRESETS.length ? Theme.PRESETS[preset.index()] : null; }

    public static int accent() { return i().picked() instanceof Theme.Preset p ? p.accent() : i().accent.own() | 0xFF000000; }

    public static int second() { return i().picked() instanceof Theme.Preset p ? p.second() : i().second.own() | 0xFF000000; }

    public static int ink() { return i().picked() instanceof Theme.Preset p ? p.ink() : i().ink.own() | 0xFF000000; }

    /** Starts a custom theme from the preset being shown. */
    public void customise() {
        accent.set(accent());
        second.set(second());
        ink.set(ink());
        preset.pick(preset.options.size() - 1);
    }

    public static boolean rainbow() { return i().rainbow.get(); }

    public static float panelAlpha() { return i().panel.getF(); }

    public static float frost() { return i().frost.getF(); }

    public static boolean rounded() { return i().corners.is("Rounded"); }

    public static float cut() { return i().cut.getF(); }

    public static boolean everywhere() { return i().everywhere.get() && !face().equals("Minecraft"); }

    public static String face() { return i().face.get(); }

    public static float menuScale() { return i().menuScale.getF(); }

    public static boolean calm() { return i().reduceMotion.get(); }

    public static float speed() { return i().speed.getF(); }

    public static boolean snowyMenus() { return i().startMenu.get(); }

    public static boolean cavern() { return i().scene.is("Ice cavern") && dev.hollow.render.Cavern.available(); }

    /** Share of the window's resolution the cavern is drawn at. */
    public static float sceneQuality() {
        return switch (i().quality.get()) {
            case "High" -> 1;
            case "Low" -> .35f;
            default -> .62f;
        };
    }

    public static boolean glassContainers() { return i().containers.get(); }

    public static boolean styled() { return i().widgets.get(); }
}
