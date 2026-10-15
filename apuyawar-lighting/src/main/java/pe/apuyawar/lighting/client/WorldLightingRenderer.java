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
 * entera, el halo nunca se dibujó en ninguna ejecución anterior.</p>
 *
 * <p><b>Corrección</b>: en vez de depender de {@code consumers()}, esta
 * versión dibuja en modo inmediato — {@code Tessellator} +
 * {@code BufferBuilder} propios, con el shader/mezcla/profundidad puestos
 * a mano vía {@code RenderSystem} y restaurados al terminar.</p>
 *
 * <p><b>Corrección de anclaje espacial (ver docs/23, esta ronda)</b>:
 * todas las posiciones que entran aquí (centro de cada punto, centro del
 * decal de suelo) se calculan exclusivamente a partir de
 * {@code emitter.pos()} + un desplazamiento local determinista — nunca a
 * partir de la posición del jugador/cámara, que sólo se usa para restar
 * (convertir a coordenadas relativas a cámara, como exige
 * {@code WorldRenderContext}) y para decidir, en {@code EmitterTracker},
 * qué posiciones del mundo están lo bastante cerca para evaluarse. La
 * sensación reportada de "la luz sigue al jugador" no era un bug de
 * coordenadas (se revisó la matriz con cuidado: no hay doble traslación —
 * {@code matrixStack} se push/pop simétricamente en cada punto, partiendo
 * de la base que ya trae {@code WorldRenderContext} para esta fase) sino
 * dos bugs reales distintos, ambos corregidos: el signo invertido de la
 * atenuación del decal (sección 5 del encargo, ver {@link #addGroundDecal})
 * y la selección de qué hojas reciben decal basada en cercanía al jugador
 * en vez de en la posición fija del bloque (sección 6, corregido en
 * {@link EmitterTracker#isGroundDecalCandidate} vía
 * {@code EmitterTracker}, no en este archivo).</p>
 */
final class WorldLightingRenderer {
    private static final int CIRCLE_SEGMENTS = 14;

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
        float simplifyBeyond = Math.max(1f, config.simplifyDetailBeyondDistance);
        float simplifyBeyondSq = simplifyBeyond * simplifyBeyond;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean anyVertex = false;

        for (EmitterTracker.ActiveEmitter emitter : active) {
            EmitterProfile profile = emitter.profile();
            boolean simplified = emitter.distanceSq() > simplifyBeyondSq;

            if (!config.debugHideFixedPoints) {
                int points = emitter.isFoliage() ? Math.max(1, profile.pointsPerBlock()) : 1;
                for (int i = 0; i < points; i++) {
                    if (!EmitterSampler.isActiveAtDensity(emitter.pos(), i, config.fixedPointDensityMultiplier)) continue;
                    float[] local = emitter.isFoliage()
                            ? EmitterSampler.samplePoint(emitter.pos(), profile, i)
                            : new float[] {0.5f, 0.5f, 0.5f};
                    EmitterSampler.PointCategory category = emitter.isFoliage()
                            ? EmitterSampler.sampleCategory(emitter.pos(), i)
                            : EmitterSampler.PointCategory.MAIN;

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

                    float alpha = clamp01(profile.intensity() * category.intensityMultiplier
                            * config.lightIntensityMultiplier * dayNightFactor * flicker * emitter.fade());
                    if (alpha <= 0.01f) continue;

                    float radius = 0.05f * category.coreRadiusMultiplier * config.lightRadiusMultiplier;
                    Matrix4f pose = billboardPose(matrixStack, cameraPos, cameraRotation, worldX, worldY, worldZ);
                    addCircle(buffer, pose, radius, r, g, b, alpha);
                    anyVertex = true;

                    // Halo de dos capas (núcleo ya dibujado arriba, halo
                    // intermedio + resplandor exterior tenue) -- sección 10
                    // del encargo: tres niveles, sin bordes duros. No es
                    // Bloom real de framebuffer (ver docs/23 sección 7),
                    // sigue siendo geometría aditiva apilada.
                    if (!simplified && config.bloomIntensity > 0.01f) {
                        float haloAlpha = clamp01(alpha * 0.4f * config.bloomIntensity);
                        float haloRadius = radius * category.haloRadiusMultiplier * (1.6f + config.bloomRadiusMultiplier);
                        addCircle(buffer, pose, haloRadius, r, g, b, haloAlpha);

                        if (category == EmitterSampler.PointCategory.MAIN) {
                            float outerAlpha = clamp01(alpha * 0.15f * config.bloomIntensity);
                            float outerRadius = haloRadius * 1.8f;
                            addCircle(buffer, pose, outerRadius, r, g, b, outerAlpha);
                        }
                    }
                }
            }

            if (config.groundDecalsEnabled && emitter.groundY() != null && !simplified) {
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
     * alfa 0), sin textura. */
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

    /** Decal de suelo: plano en el suelo (sin orientar a cámara), con alfa
     * reducido por la distancia vertical real de caída.
     *
     * <p><b>Corrección del signo (sección 5 del encargo)</b>: la versión
     * anterior calculaba {@code fallDistance = groundY - pos.getY()}. Como
     * una hoja está casi siempre POR ENCIMA del suelo, {@code groundY}
     * (más bajo) menos {@code pos.getY()} (más alto) da un número
     * NEGATIVO — al restar ese negativo en
     * {@code 1 - fallDistance/alcance}, el resultado AUMENTA en vez de
     * disminuir, y queda recortado (clamp) en 1.0: la intensidad máxima,
     * sin importar lo lejos que esté realmente la hoja del suelo. Una hoja
     * a 2 bloques de altura y una a 20 habrían dado exactamente el mismo
     * decal al máximo. Se confirmó con valores concretos (hoja en Y=72,
     * suelo en Y=64 → fallDistance antiguo = 64-72 = -8 → atenuación
     * antigua = clamp(1 - (-8/18)) = clamp(1.44) = 1.0, el bug exacto que
     * describía el encargo).</p>
     *
     * <p>Corrección: {@code verticalDistance = max(0, pos.getY() - groundY)}
     * (altura real de caída, nunca negativa), y la atenuación usa el
     * exponente {@code profile.falloff()} ya definido en
     * {@code EmitterProfile} en vez de una resta lineal fija.</p> */
    private static boolean addGroundDecal(BufferBuilder buffer, MatrixStack matrixStack, Vec3d cameraPos,
                                           EmitterTracker.ActiveEmitter emitter, LightingConfig config, float dayNightFactor) {
        EmitterProfile profile = emitter.profile();
        var pos = emitter.pos();
        double groundSurfaceY = emitter.groundY() + 1.01;
        double verticalDistance = Math.max(0.0, pos.getY() - groundSurfaceY);
        double maxDistance = profile.radius() * 3.0;
        float linear = clamp01(1.0f - (float) (verticalDistance / maxDistance));
        float falloff = (float) Math.pow(linear, Math.max(0.1f, profile.falloff()));
        if (falloff <= 0.02f) return false;

        float alpha = clamp01(profile.intensity() * 0.4f * config.lightIntensityMultiplier * config.groundDecalIntensityMultiplier
                * dayNightFactor * falloff * emitter.fade());
        if (alpha <= 0.01f) return false;
        float radius = profile.radius() * 0.5f * config.lightRadiusMultiplier;

        matrixStack.push();
        matrixStack.translate(pos.getX() + 0.5 - cameraPos.x, groundSurfaceY - cameraPos.y, pos.getZ() + 0.5 - cameraPos.z);
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
        // position_color de vanilla no premultiplica por alfa). Con
        // SRC_ALPHA el alfa sí escala la contribución de color antes de
        // sumarla.
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
