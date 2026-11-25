package dev.hollow.core;

import java.util.List;

public enum Category {
    COMBAT("Combat"), PLAYER("Player"), MOVEMENT("Movement"), RENDER("Render"), WORLD("World"), MISC("Misc"), HUD("HUD"), CLIENT("Client");

    /** The ones with a column in the module menu. */
    public static final List<Category> COLUMNS = List.of(COMBAT, PLAYER, MOVEMENT, RENDER, WORLD, MISC);

    public final String title;

    Category(String title) { this.title = title; }
}
