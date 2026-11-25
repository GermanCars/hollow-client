package dev.hollow.ui;

import com.mojang.blaze3d.platform.NativeImage;
import dev.hollow.Hollow;
import dev.hollow.module.Interface;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Container backgrounds turned to glass. The game's (or the resource pack's) own picture is read and its grey panel made clear,
 * so arrows, flames and everything else that isn't panel stays where it was.
 */
public final class GlassTextures {
    private static final Map<Identifier, Identifier> DONE = new HashMap<>();

    private GlassTextures() {}

    public static void forget() { DONE.clear(); }

    /** The see-through version, or the original when it can't be made. */
    public static Identifier of(Identifier id) {
        if (!Interface.glassContainers() || !id.getPath().startsWith("textures/gui/container/") || id.getPath().contains("creative")) return id;
        return DONE.computeIfAbsent(id, GlassTextures::make);
    }

    private static Identifier make(Identifier id) {
        Minecraft mc = Minecraft.getInstance();
        var res = mc.getResourceManager().getResource(id);
        if (res.isEmpty()) return id;
        try (var in = res.get().open()) {
            NativeImage img = NativeImage.read(in);
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) img.setPixel(x, y, clear(img.getPixel(x, y)));
            Identifier out = Identifier.fromNamespaceAndPath("hollow", "glass/" + id.getNamespace() + "/" + id.getPath());
            mc.getTextureManager().register(out, new DynamicTexture(() -> "hollow glass " + id, img));
            return out;
        } catch (Exception e) {
            Hollow.LOG.warn("Couldn't turn {} to glass", id, e);
            return id;
        }
    }

    private static int clear(int c) {
        int a = c >>> 24, r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        if (a < 8) return c;
        int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
        // Only greys belong to the panel.
        if (hi - lo > 14) return c;
        if (near(hi, 198)) return 0;
        if (near(hi, 139)) return 0x55000A14;
        if (near(hi, 55)) return 0x66000A14;
        if (near(hi, 85)) return 0;
        if (hi >= 250) return 0x3CFFFFFF;
        if (hi <= 4) return 0x40000A14;
        return (Math.min(a, 150) << 24) | c & 0xFFFFFF;
    }

    private static boolean near(int v, int to) { return Math.abs(v - to) <= 6; }
}
