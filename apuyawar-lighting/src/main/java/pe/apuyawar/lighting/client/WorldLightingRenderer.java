package pe.apuyawar.lighting.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.EmitterSampler;
import pe.apuyawar.lighting.config.LightingConfig;

import java.util.List;

/**
 * Dibuja los puntos de luz (billboards orientados a cámara, mezcla
 * aditiva) y el decal de suelo de {@link EmitterTracker#active()}.
 *
 * <p><b>Lo que esto es</b>: geometría 3D real, posicionada en el mundo,
 * con prueba de profundidad contra el terreno ya dibujado (no atraviesa
 * paredes — ver {@link GlowRenderLayer}) y que responde a la cámara como
 * cualquier partícula billboard de vanilla.</p>
 *
 * <p><b>Lo que esto NO es</b>: no es iluminación de vóxel/lightmap real —
 * no cambia el nivel de luz de bloque del terreno vecino (eso seguiría
 * siendo trabajo de {@code AbstractBlock.Settings#luminance}, en escala de
 * grises, ya aplicado en Momento Cuy). Es la aproximación visual que
 * permite la sección 4 del encargo ("iluminación en espacio de pantalla,
 * iluminación local por shader") sin reescribir el motor de luces de
 * Minecraft — ver {@code docs/23_APU_YAWAR_LIGHTING_RGB.md} para la
 * limitación completa.</p>
 */
final class WorldLightingRenderer {
    private WorldLightingRenderer() {}

    static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldLightingRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        List<EmitterTracker.ActiveEmitter> active = EmitterTracker.active();
        if (active.isEmpty()) return;
        VertexConsumerProvider consumers = context.consumers();
        MatrixStack matrixStack = context.matrixStack();
        if (consumers == null || matrixStack == null) return;

        LightingConfig config = ApuYawarLightingClient.config();
        Vec3d cameraPos = context.camera().getPos();
        Quaternionf cameraRotation = context.camera().getRotation();
        boolean isDay = context.world().isDay();
        float dayNightFactor = isDay ? config.dayIntensityMultiplier : config.nightIntensityMultiplier;
        long worldTime = context.world().getTime();

        VertexConsumer buffer = consumers.getBuffer(GlowRenderLayer.GLOW);

        for (EmitterTracker.ActiveEmitter emitter : active) {
            EmitterProfile profile = emitter.profile();
            int points = emitter.isFoliage() ? Math.max(1, profile.pointsPerBlock()) : 1;
            for (int i = 0; i < points; i++) {
                if (!EmitterSampler.isActiveAtDensity(emitter.pos(), i, config.particleDensityMultiplier)) continue;
                float[] local = emitter.isFoliage()
                        ? EmitterSampler.samplePoint(emitter.pos(), profile, i)
                        : new float[] {0.5f, 0.5f, 0.5f};
                float scale = EmitterSampler.sampleScale(emitter.pos(), i);

                double worldX = emitter.pos().getX() + local[0];
                double worldY = emitter.pos().getY() + local[1];
                double worldZ = emitter.pos().getZ() + local[2];

                boolean useSecondary = profile.hasSecondary() && (i % 3 == 2);
                float r = useSecondary ? profile.secondaryRed() : profile.red();
                float g = useSecondary ? profile.secondaryGreen() : profile.green();
                float b = useSecondary ? profile.secondaryBlue() : profile.blue();

                float flicker = 1.0f;
                if (profile.animated()) {
                    double phase = (emitter.pos().asLong() * 0.37) + i;
                    flicker = 0.72f + 0.28f * (float) Math.sin(worldTime * 0.05 + phase);
                }

                float alpha = clamp01(profile.intensity() * config.lightIntensityMultiplier * dayNightFactor * flicker);
                if (alpha <= 0.01f) continue;

                float halfSize = (0.06f + scale * 0.07f) * config.lightRadiusMultiplier;
                drawBillboard(matrixStack, buffer, cameraPos, cameraRotation, worldX, worldY, worldZ, halfSize, r, g, b, alpha);

                // Halo pseudo-bloom barato: 1-2 capas extra más grandes y más
                // tenues alrededor del mismo punto -- ver docs/23 seccion 7
                // para la diferencia con el Bloom real de framebuffer
                // (seccion "Resplandor neon") que corre aparte.
                if (config.bloomIntensity > 0.01f) {
                    float haloAlpha = clamp01(alpha * 0.35f * config.bloomIntensity);
                    float haloSize = halfSize * (1.8f + config.bloomRadiusMultiplier);
                    drawBillboard(matrixStack, buffer, cameraPos, cameraRotation, worldX, worldY, worldZ, haloSize, r, g, b, haloAlpha);
                }
            }

            if (emitter.groundY() != null) {
                drawGroundDecal(matrixStack, buffer, cameraPos, emitter, config, dayNightFactor);
            }
        }
    }

    private static void drawBillboard(MatrixStack matrixStack, VertexConsumer buffer, Vec3d cameraPos,
                                       Quaternionf cameraRotation, double worldX, double worldY, double worldZ,
                                       float halfSize, float r, float g, float b, float alpha) {
        matrixStack.push();
        matrixStack.translate(worldX - cameraPos.x, worldY - cameraPos.y, worldZ - cameraPos.z);
        matrixStack.multiply(cameraRotation);
        Matrix4f pose = matrixStack.peek().getPositionMatrix();
        int a = (int) (alpha * 255.0f);
        int ri = (int) (r * 255.0f);
        int gi = (int) (g * 255.0f);
        int bi = (int) (b * 255.0f);
        buffer.vertex(pose, -halfSize, -halfSize, 0).color(ri, gi, bi, a);
        buffer.vertex(pose, halfSize, -halfSize, 0).color(ri, gi, bi, a);
        buffer.vertex(pose, halfSize, halfSize, 0).color(ri, gi, bi, a);
        buffer.vertex(pose, -halfSize, halfSize, 0).color(ri, gi, bi, a);
        matrixStack.pop();
    }

    /** Quad horizontal plano a nivel del suelo encontrado, con alfa
     * reducido por la distancia caída desde la copa — ver docs/23 sección
     * "Luz sobre el suelo y el agua": esto es un decal aditivo sobre lo ya
     * dibujado (incluida el agua, si el bloque de abajo es agua), no una
     * reflexión ni una reinterpretación física de la superficie. */
    private static void drawGroundDecal(MatrixStack matrixStack, VertexConsumer buffer, Vec3d cameraPos,
                                         EmitterTracker.ActiveEmitter emitter, LightingConfig config, float dayNightFactor) {
        EmitterProfile profile = emitter.profile();
        BlockPos pos = emitter.pos();
        double groundY = emitter.groundY() + 1.01;
        double fallDistance = groundY - pos.getY();
        float falloff = clamp01(1.0f - (float) (fallDistance / (profile.radius() * 3.0)));
        if (falloff <= 0.02f) return;

        float alpha = clamp01(profile.intensity() * 0.4f * config.lightIntensityMultiplier * dayNightFactor * falloff);
        if (alpha <= 0.01f) return;
        float size = profile.radius() * 0.5f * config.lightRadiusMultiplier;

        matrixStack.push();
        matrixStack.translate(pos.getX() + 0.5 - cameraPos.x, groundY - cameraPos.y, pos.getZ() + 0.5 - cameraPos.z);
        Matrix4f pose = matrixStack.peek().getPositionMatrix();
        int a = (int) (alpha * 255.0f);
        int ri = (int) (profile.red() * 255.0f);
        int gi = (int) (profile.green() * 255.0f);
        int bi = (int) (profile.blue() * 255.0f);
        // Octágono aproximado con 4 vértices (rombo) en vez de un cuadrado
        // girado 45°: suficiente para un decal difuso, sin textura nueva.
        buffer.vertex(pose, -size, 0, 0).color(ri, gi, bi, a);
        buffer.vertex(pose, 0, 0, -size).color(ri, gi, bi, a);
        buffer.vertex(pose, size, 0, 0).color(ri, gi, bi, a);
        buffer.vertex(pose, 0, 0, size).color(ri, gi, bi, a);
        matrixStack.pop();
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
}
