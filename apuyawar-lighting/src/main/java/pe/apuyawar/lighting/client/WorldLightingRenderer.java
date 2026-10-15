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
 * Dibuja los puntos de luz de {@link EmitterTracker#active()} y los focos
 * de suelo de {@link EmitterTracker#groundSpots()} (dos listas separadas
 * desde la corrección V2 — ver docs/23).
 *
 * <p><b>Corrección sobre la primera versión de este archivo</b>: la
 * primera implementación dependía de {@code context.consumers()}, que es
 * {@code null} en {@code AFTER_TRANSLUCENT}. Esta versión dibuja en modo
 * inmediato — {@code Tessellator} + {@code BufferBuilder} propios, con el
 * shader/mezcla/profundidad puestos a mano vía {@code RenderSystem}.</p>
 *
 * <p><b>Anclaje espacial</b>: todas las posiciones se calculan desde
 * {@code emitter.pos()}/{@code spot.anchorPos()} + un desplazamiento
 * local determinista — la cámara sólo se resta al final para convertir a
 * coordenadas relativas, nunca decide el centro de nada.</p>
 *
 * <p><b>Rediseño V2 del foco de suelo</b>: antes cada hoja candidata
 * dibujaba su propio decal con radio heredado de {@code profile.radius()}
 * (el radio de luz de la hoja, 6 bloques → ~3 de radio) — con varias hojas
 * bajo la misma copa, los círculos se superponían por mezcla aditiva y
 * formaban una mancha tan ancha como la proyección de la copa, tal como
 * reportó el equipo con capturas reales. Ahora {@code addGroundSpot}
 * dibuja un único foco por {@link EmitterTracker.GroundSpot} (como mucho
 * uno por celda de 8×8 bloques, ver {@code EmitterTracker}), con radio
 * propio ({@code LightingConfig#groundSpotRadius}, 1.6 bloques por
 * defecto) completamente desacoplado del radio de luz de cualquier
 * hoja.</p>
 */
final class WorldLightingRenderer {
    private static final int CIRCLE_SEGMENTS = 14;

    private WorldLightingRenderer() {}

    static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldLightingRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        List<EmitterTracker.ActiveEmitter> active = EmitterTracker.active();
        List<EmitterTracker.GroundSpot> spots = EmitterTracker.groundSpots();
        if (active.isEmpty() && spots.isEmpty()) return;
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

        if (!config.debugHideFixedPoints) {
            for (EmitterTracker.ActiveEmitter emitter : active) {
                EmitterProfile profile = emitter.profile();
                boolean simplified = emitter.distanceSq() > simplifyBeyondSq;

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

                    // Corrección V2: con radio base 0.05 (diámetro 0.1
                    // bloques, ~10 cm), un punto "pequeño" (coreRadiusMultiplier
                    // 0.55) medía ~0.055 de radio -- casi invisible a
                    // distancia normal, aun con el alfa correcto. Esto es
                    // muy probablemente la causa numérica de "la copa
                    // permanece casi totalmente oscura" (el equipo no
                    // reportó alfa=0, reportó puntos que no se notan). Se
                    // sube la base a 0.11 (~2.2×) -- un punto de partida
                    // razonado, no confirmado visualmente todavía.
                    float radius = 0.11f * category.coreRadiusMultiplier * config.lightRadiusMultiplier;
                    Matrix4f pose = billboardPose(matrixStack, cameraPos, cameraRotation, worldX, worldY, worldZ);
                    addCircle(buffer, pose, radius, r, g, b, alpha);
                    anyVertex = true;

                    // Halo de dos capas (núcleo ya dibujado arriba, halo
                    // intermedio + resplandor exterior tenue) -- tres
                    // niveles, sin bordes duros. No es Bloom real de
                    // framebuffer (ver docs/23), sigue siendo geometría
                    // aditiva apilada.
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
        }

        for (EmitterTracker.GroundSpot spot : spots) {
            anyVertex |= addGroundSpot(buffer, matrixStack, cameraPos, spot, config, dayNightFactor);
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

    /** Foco de suelo: plano en el suelo (sin orientar a cámara), con alfa
     * reducido por la distancia vertical real de caída.
     *
     * <p>Mantiene la corrección del signo de la ronda anterior
     * ({@code verticalDistance = max(0, anchorY - groundY)}, nunca
     * negativo, con el exponente {@code falloff} real del perfil) — lo
     * nuevo de esta ronda es que el RADIO y la INTENSIDAD BASE ya no
     * salen de {@code profile.radius()}/{@code profile.intensity()}, sino
     * de {@code LightingConfig#groundSpotRadius}/{@code groundSpotIntensity},
     * independientes del radio de luz de la hoja (sección 5.1 del
     * encargo V2) — y sólo hay un foco por {@link EmitterTracker.GroundSpot}
     * (como mucho uno por celda), no uno por hoja.</p> */
    private static boolean addGroundSpot(BufferBuilder buffer, MatrixStack matrixStack, Vec3d cameraPos,
                                          EmitterTracker.GroundSpot spot, LightingConfig config, float dayNightFactor) {
        EmitterProfile profile = spot.profile();
        var pos = spot.anchorPos();
        double groundSurfaceY = spot.groundY() + 1.01;
        double verticalDistance = Math.max(0.0, pos.getY() - groundSurfaceY);
        double maxDistance = Math.max(2.0, config.groundSpotRadius * 6.0);
        float linear = clamp01(1.0f - (float) (verticalDistance / maxDistance));
        float falloff = (float) Math.pow(linear, Math.max(0.1f, profile.falloff()));
        if (falloff <= 0.02f) return false;

        float alpha = clamp01(config.groundSpotIntensity * config.groundDecalIntensityMultiplier
                * dayNightFactor * falloff * spot.fade());
        if (alpha <= 0.01f) return false;
        float radius = Math.max(0.1f, config.groundSpotRadius);

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
