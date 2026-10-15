package pe.apuyawar.lighting.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

/**
 * Capa de render propia para los cuadrados de resplandor (billboards) y el
 * decal de suelo: color de vértice puro (sin textura), mezcla aditiva
 * (sección 6 del encargo — brillo "neón"), con prueba de profundidad real
 * contra el terreno ya dibujado (para que las luces no atraviesen paredes
 * opacas) pero sin escribir en el depth buffer (igual que cualquier capa
 * translúcida de vanilla, para no ocluir partículas/entidades detrás del
 * halo). Reutiliza el shader de color de vértice ya existente de vanilla
 * ({@code RenderPhase.COLOR_PROGRAM}, el mismo "position_color" que usa
 * p. ej. el rayo — ver {@code assets/minecraft/shaders/core/position_color.json})
 * en vez de inventar un shader propio para esto.
 */
final class GlowRenderLayer {
    static final RenderLayer GLOW = RenderLayer.of(
            "apuyawar_lighting_glow",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            false, true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .lightmap(RenderPhase.DISABLE_LIGHTMAP)
                    .overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
                    .writeMaskState(RenderPhase.COLOR_MASK)
                    .target(RenderPhase.MAIN_TARGET)
                    .build(false));

    private GlowRenderLayer() {}
}
