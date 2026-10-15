package pe.apuyawar.lighting.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.EmitterSampler;
import pe.apuyawar.lighting.config.LightingConfig;

import java.util.List;

/**
 * Dibuja los puntos de luz y el decal de suelo de
 * {@link EmitterTracker#active()}.
 *
 * <p><b>Corrección sobre la primera versión de este archivo</b>: la
 * primera implementación enviaba los vértices a través de
 * {@code context.consumers()} (un {@code VertexConsumerProvider}). Se
 * confirmó (reportado por el equipo, y verificado contra el Javadoc real
 * de {@code WorldRenderContext}, que ya se había investigado antes pero
 * se aplicó mal al elegir la fase) que {@code consumers()} es
 * {@code null} precisamente en {@code AFTER_TRANSLUCENT} — es no-nulo
 * sólo entre {@code BEFORE_ENTITIES} y {@code BEFORE_DEBUG_RENDER}. Como
 * el guard {@code if (consumers == null) return;} cortaba la función
 * entera, el halo nunca se dibujó en ninguna ejecución anterior — el
 * `runClient` de la entrega previa confirmó que el juego no crasheaba,
 * pero nunca demostró que algo se viera, y no se afirmó lo contrario; aun
 * así, era un bug real que corregir.</p>
 *
 * <p><b>Corrección</b>: en vez de depender de {@code consumers()}, esta
 * versión dibuja en modo inmediato — {@code Tessellator} +
 * {@code BufferBuilder} propios, con el shader/mezcla/profundidad puestos
 * a mano vía {@code RenderSystem} y restaurados al terminar — exactamente
 * la alternativa de "renderizado inmediato directo" que se pidió. Esto
 * permite seguir en {@code AFTER_TRANSLUCENT} (para que el halo quede por
 * encima del agua/vidrio ya dibujados), sin depender de un
 * {@code VertexConsumerProvider} que ahí no existe.</p>
 */
final class WorldLightingRenderer {
    private static final int CIRCLE_SEGMENTS = 10;

    private WorldLightingRenderer() {}

    static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldLightingRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        List<EmitterTracker.ActiveEmitter> active = EmitterTracker.active();
        if (active.isEmpty()) return;
        MatrixStack matrixStack = context.matrixStack();
        if (matrixStack == null) return;

        LightingConfig config = ApuYawarLightingClient.config();
        Vec3d cameraPos = context.camera().getPos();
        Quaternionf cameraRotation = context.camera().getRotation();
        boolean isDay = context.world().isDay();
        float dayNightFactor = isDay ? config.dayIntensityMultiplier : config.nightIntensityMultiplier;
        long worldTime = context.world().getTime();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean anyVertex = false;

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

                float radius = (0.06f + scale * 0.07f) * config.lightRadiusMultiplier;
                Matrix4f pose = billboardPose(matrixStack, cameraPos, cameraRotation, worldX, worldY, worldZ);
                addCircle(buffer, pose, radius, r, g, b, alpha);
                anyVertex = true;

                // Halo pseudo-bloom barato: un segundo círculo más grande y
                // más tenue -- ver docs/23 seccion 7 para la diferencia con
                // el Bloom real de framebuffer (todavía no implementado).
                if (config.bloomIntensity > 0.01f) {
                    float haloAlpha = clamp01(alpha * 0.35f * config.bloomIntensity);
                    float haloRadius = radius * (1.8f + config.bloomRadiusMultiplier);
                    addCircle(buffer, pose, haloRadius, r, g, b, haloAlpha);
                }
            }

            if (emitter.groundY() != null) {
                anyVertex |= addGroundDecal(buffer, matrixStack, cameraPos, emitter, config, dayNightFactor);
            }
        }

        if (!anyVertex) {
            buffer.endNullable();
            return;
        }

        BuiltBuffer built = buffer.end();
        drawAdditive(built);
    }

    private static Matrix4f billboardPose(MatrixStack matrixStack, Vec3d cameraPos, Quaternionf cameraRotation,
                                           double worldX, double worldY, double worldZ) {
        matrixStack.push();
        matrixStack.translate(worldX - cameraPos.x, worldY - cameraPos.y, worldZ - cameraPos.z);
        matrixStack.multiply(cameraRotation);
        Matrix4f pose = new Matrix4f(matrixStack.peek().getPositionMatrix());
        matrixStack.pop();
        return pose;
    }

    /** Círculo suave real (abanico de triángulos, centro opaco y borde a
     * alfa 0) en vez de un cuadrado de color uniforme — ver sección 3 del
     * encargo de corrección: nada de rectángulos planos. Sin textura. */
    private static void addCircle(BufferBuilder buffer, Matrix4f pose, float radius, float r, float g, float b, float centerAlpha) {
        int ri = clampByte(r);
        int gi = clampByte(g);
        int bi = clampByte(b);
        int a = clampByte(centerAlpha);
        float[] ringX = new float[CIRCLE_SEGMENTS];
        float[] ringY = new float[CIRCLE_SEGMENTS];
        for (int s = 0; s < CIRCLE_SEGMENTS; s++) {
            double angle = 2.0 * Math.PI * s / CIRCLE_SEGMENTS;
            ringX[s] = (float) Math.cos(angle) * radius;
            ringY[s] = (float) Math.sin(angle) * radius;
        }
        for (int s = 0; s < CIRCLE_SEGMENTS; s++) {
            int next = (s + 1) % CIRCLE_SEGMENTS;
            buffer.vertex(pose, 0, 0, 0).color(ri, gi, bi, a);
            buffer.vertex(pose, ringX[s], ringY[s], 0).color(ri, gi, bi, 0);
            buffer.vertex(pose, ringX[next], ringY[next], 0).color(ri, gi, bi, 0);
        }
    }

    /** Decal de suelo: igual que {@link #addCircle}, plano en el suelo
     * (sin orientar a cámara), con alfa reducido por la distancia caída —
     * ver docs/23 sección "Luz sobre el suelo y el agua". */
    private static boolean addGroundDecal(BufferBuilder buffer, MatrixStack matrixStack, Vec3d cameraPos,
                                           EmitterTracker.ActiveEmitter emitter, LightingConfig config, float dayNightFactor) {
        EmitterProfile profile = emitter.profile();
        var pos = emitter.pos();
        double groundY = emitter.groundY() + 1.01;
        double fallDistance = groundY - pos.getY();
        float falloff = clamp01(1.0f - (float) (fallDistance / (profile.radius() * 3.0)));
        if (falloff <= 0.02f) return false;

        float alpha = clamp01(profile.intensity() * 0.4f * config.lightIntensityMultiplier * dayNightFactor * falloff);
        if (alpha <= 0.01f) return false;
        float radius = profile.radius() * 0.5f * config.lightRadiusMultiplier;

        matrixStack.push();
        matrixStack.translate(pos.getX() + 0.5 - cameraPos.x, groundY - cameraPos.y, pos.getZ() + 0.5 - cameraPos.z);
        Matrix4f pose = new Matrix4f(matrixStack.peek().getPositionMatrix());
        matrixStack.pop();
        // Rota el abanico 90° sobre X para que quede plano en el suelo (XZ)
        // en vez de vertical (XY, orientación por defecto de addCircle).
        pose.rotateX((float) Math.PI / 2f);
        addCircle(buffer, pose, radius, profile.red(), profile.green(), profile.blue(), alpha);
        return true;
    }

    private static void drawAdditive(BuiltBuffer built) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        // SRC_ALPHA, ONE (no ONE, ONE): con mezcla puramente aditiva el
        // canal alfa no influye en absoluto en el resultado (el shader
        // position_color de vanilla no premultiplica por alfa), así que la
        // intensidad/parpadeo/perfiles configurados no tenían ningún efecto
        // visual real -- ver sección 2 de la corrección. Con SRC_ALPHA el
        // alfa sí escala la contribución de color antes de sumarla.
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static int clampByte(float v) {
        return (int) (clamp01(v) * 255.0f);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
}
