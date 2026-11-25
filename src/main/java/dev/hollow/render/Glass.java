package dev.hollow.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;

/** A blurred copy of what is behind the interface, remade every frame something wants it. */
public final class Glass {
    private static final int LEVELS = 4;
    private static final TextureTarget[] CHAIN = new TextureTarget[LEVELS];
    private static TextureTarget result;
    private static int width, height;
    private static boolean wanted, ready;

    private Glass() {}

    /** Once per frame, before anything is drawn with it. */
    public static void prepare() {
        var w = Minecraft.getInstance().getWindow();
        int fw = Math.max(16, w.getWidth()), fh = Math.max(16, w.getHeight());
        if (fw == width && fh == height && result != null) return;
        close();
        width = fw;
        height = fh;
        for (int i = 0; i < LEVELS; i++) CHAIN[i] = target("glass " + i, fw >> i + 1, fh >> i + 1);
        result = target("glass", fw >> 2, fh >> 2);
        ready = false;
    }

    static TextureTarget target(String name, int w, int h) { return new TextureTarget("hollow " + name, Math.max(2, w), Math.max(2, h), GpuFormat.RGBA8_UNORM, null); }

    private static void close() {
        for (int i = 0; i < LEVELS; i++) if (CHAIN[i] != null) CHAIN[i].destroyBuffers();
        if (result != null) result.destroyBuffers();
        result = null;
    }

    /** Asks for this frame's blur. Null until there is one, so draw something plain instead. */
    public static TextureSetup texture() {
        wanted = true;
        return ready && result != null ? TextureSetup.singleTexture(result.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)) : null;
    }

    /** Render thread, with the finished world or scene in the main target and no interface yet. */
    public static void capture() {
        if (!wanted || result == null) return;
        wanted = false;
        var main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        boolean ok = pass(Pipelines.DOWN, main.getColorTextureView(), CHAIN[0].getColorTextureView());
        for (int i = 1; i < LEVELS && ok; i++) ok = pass(Pipelines.DOWN, CHAIN[i - 1].getColorTextureView(), CHAIN[i].getColorTextureView());
        for (int i = LEVELS - 2; i >= 1 && ok; i--) ok = pass(Pipelines.UP, CHAIN[i + 1].getColorTextureView(), CHAIN[i].getColorTextureView());
        ready = ok && pass(Pipelines.FINISH, CHAIN[1].getColorTextureView(), result.getColorTextureView());
    }

    static boolean pass(RenderPipeline p, GpuTextureView from, GpuTextureView to) {
        CompiledRenderPipeline c = RenderSystem.getCompiledPipelineNullable(p);
        if (c == null) return false;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Hollow glass", to, Optional.empty())) {
            pass.setPipeline(c);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("InSampler", from, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
        return true;
    }
}
