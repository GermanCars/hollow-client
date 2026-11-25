package dev.hollow.module;

import dev.hollow.core.Category;
import dev.hollow.core.Module;

/** Walk at full speed while eating, drinking or blocking. */
public final class NoSlow extends Module {
    public NoSlow() { super(Category.MOVEMENT, "NoSlow", "Keeps full speed while eating, drinking or blocking."); }
}
