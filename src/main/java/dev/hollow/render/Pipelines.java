package dev.hollow.render;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Hollow's own shaders. Optional: without them the client falls back to plain drawing. */
public final class Pipelines {
    static final RenderPipeline DOWN = blit("glass_down"), UP = blit("glass_up"), FINISH = blit("glass_finish"), COPY = blit("glass_copy");
    static final RenderPipeline CAVERN = RenderPipelines.registerOptional(base("cavern").withBindGroupLayout(BindGroupLayout.builder().withUniform("SceneInfo", UniformType.UNIFORM_BUFFER).build()).build());

    private Pipelines() {}

    private static RenderPipeline.Builder base(String name) {
        return RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET).withLocation(id("pipeline/" + name)).withVertexShader("core/screenquad").withFragmentShader(id("core/" + name)).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withColorTargetState(ColorTargetState.DEFAULT);
    }

    private static RenderPipeline blit(String name) { return RenderPipelines.registerOptional(base(name).withBindGroupLayout(BindGroupLayouts.IN_SAMPLER).build()); }

    private static Identifier id(String s) { return Identifier.fromNamespaceAndPath("hollow", s); }

    /** Call early, so the pipelines are known before the game compiles them. */
    public static void init() {}
}
