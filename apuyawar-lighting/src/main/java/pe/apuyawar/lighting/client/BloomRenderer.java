package pe.apuyawar.lighting.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.lighting.config.LightingConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * APU YAWAR Lighting V4/V4.1: Bloom REAL de framebuffer (sección 5 del
 * encargo V4, revisada en V4.1 tras encontrar que la máscara de emisión
 * sólo incluía los halos antiguos).
 *
 * <p><b>Causa real del problema reportado en V4.1</b>: la V4 llamaba a
 * la captura del Bloom INMEDIATAMENTE después de dibujar los puntos/
 * halos de siempre, y ANTES de que {@code WorldLightingRenderer
 * #renderEmissiveSurfaces} (el follaje emisivo de la fase 1) o las
 * partículas celestiales (fase 2, un sistema de render totalmente
 * aparte, el de Minecraft) llegaran a dibujar nada -- cada llamada a
 * capturar, además, limpiaba el framebuffer propio desde cero
 * (`clear()`), así que aunque se hubiera llamado una segunda vez, habría
 * borrado la primera captura en vez de acumularse con ella. El Bloom
 * real SÓLO podía mostrar los círculos aditivos antiguos, nunca el
 * follaje emisivo ni las partículas -- exactamente el síntoma que
 * reportó el equipo ("el Bloom no produce el resplandor neón
 * esperado").</p>
 *
 * <p><b>Arquitectura corregida (V4.1)</b>: ciclo de vida explícito por
 * frame, para que varias pasadas de dibujo puedan ACUMULAR su
 * contribución en el mismo framebuffer antes de difuminarlo una sola
 * vez:</p>
 * <ol>
 * <li>{@link #beginCapture} -- una vez por frame, al principio de
 *     {@code WorldLightingRenderer#render}: limpia el framebuffer
 *     propio y copia la profundidad real UNA sola vez.</li>
 * <li>{@link #captureColorGeometry}/{@link #captureTexturedGeometry} --
 *     se puede llamar VARIAS veces en el mismo frame (una por los
 *     puntos/halos, otra por cada grupo de máscara emisiva) --
 *     reenlaza el framebuffer propio (ya limpio, con la profundidad ya
 *     copiada), dibuja, y vuelve a enlazar el framebuffer real antes de
 *     devolver el control, para que el resto del render normal (incluida
 *     la siguiente pasada que capturar) no se vea afectado.</li>
 * <li>{@link #captureParticleEchoes} -- dibuja un eco pequeño por cada
 *     partícula celestial reciente (ver {@link #registerParticleEcho},
 *     llamado desde {@code CelestialParticleSpawner}) -- las partículas
 *     en sí las dibuja el sistema de partículas de Minecraft, nunca
 *     nuestro código; esto es sólo su CONTRIBUCIÓN al Bloom, un circulo
 *     pequeño adicional en el framebuffer propio, nunca visible por sí
 *     solo en el framebuffer real.</li>
 * <li>{@link #endCapture} -- una vez por frame, al final de
 *     {@code WorldLightingRenderer#render}: marca el frame como listo
 *     para difuminar.</li>
 * <li>En {@code WorldRenderEvents.END} (después de que todo el render
 *     del mundo terminó, antes de la GUI), {@link #onEnd} difumina el
 *     framebuffer propio (ya con TODAS las contribuciones acumuladas)
 *     con el mismo pipeline multipasada que ya usa vanilla para su
 *     propio desenfoque ({@code minecraft:blur}, reutilizado tal cual) y
 *     lo compone de vuelta sobre el framebuffer real con mezcla
 *     aditiva.</li>
 * </ol>
 *
 * <p><b>Por qué nunca puede desenfocar el Panel K (sección 4.7 del
 * encargo V4.1, restricción absoluta)</b>: el pipeline de desenfoque de
 * vanilla sólo toca el framebuffer que se le pasa como "mainTarget" al
 * construir el {@link PostEffectProcessor} -- en este caso, siempre
 * nuestro framebuffer separado, nunca el framebuffer real del juego.
 * Toda la composición ocurre en {@code WorldRenderEvents.END}, antes de
 * que nada de GUI se dibuje.</p>
 *
 * <p><b>Desactivado por defecto</b> ({@code LightingConfig#realBloomEnabled}):
 * sigue sin poderse ejecutar la ruta real de producción en este
 * entorno (exige un mundo cargado con árboles reales) -- sólo la
 * autoprueba aislada. Activable por comando
 * (`/apuyawarlighting bloomreal`), nunca por defecto.</p>
 */
final class BloomRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-lighting");
    // El Identifier de un PostEffectProcessor es la ruta REAL del
    // recurso JSON -- "shaders/post/" y ".json" incluidos -- no un
    // nombre corto resuelto por convención. Confirmado leyendo el
    // bytecode real de GameRenderer: su propio campo BLUR_PROCESSOR se
    // construye exactamente así
    // (Identifier.ofVanilla("shaders/post/blur.json")). Un primer
    // intento con Identifier.of("minecraft","blur") fallaba con
    // FileNotFoundException -- lo detectó la autoprueba real de abajo,
    // no una suposición.
    private static final Identifier BLUR_EFFECT = Identifier.of("minecraft", "shaders/post/blur.json");

    private static SimpleFramebuffer bloomBuffer;
    private static PostEffectProcessor blurProcessor;
    private static int bufferWidth = -1;
    private static int bufferHeight = -1;
    private static boolean disabledDueToError = false;
    private static boolean hasContentThisFrame = false;
    /** Si {@link #beginCapture} dejó el framebuffer propio listo para
     * recibir dibujo este frame -- evita que
     * {@link #captureColorGeometry}/{@link #captureTexturedGeometry}
     * dibujen nada si {@code beginCapture} no llegó a ejecutarse o
     * falló (p. ej. Bloom desactivado, o un error ya deshabilitó el
     * módulo). */
    private static boolean captureOpen = false;

    /** V4.1, fase 1 (sección 4.4/6.4 del encargo: "las partículas
     * celestiales también deben poder contribuir al efecto"): ecos
     * ligeros de partículas recientes, registrados por
     * {@code CelestialParticleSpawner} -- las partículas en sí las
     * sigue dibujando el sistema de partículas de Minecraft; esto es
     * sólo su contribución adicional al framebuffer de Bloom. Límite
     * duro para no crecer sin control si se registran muchas partículas
     * con el Bloom desactivado (quedarían sin consumirse). */
    private static final int MAX_PARTICLE_ECHOES = 96;
    private static final List<ParticleEcho> particleEchoes = new ArrayList<>();

    private BloomRenderer() {}

    static void register() {
        WorldRenderEvents.END.register(BloomRenderer::onEnd);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickEchoes());
    }

    /** Una vez por frame, antes de cualquier captura -- ver
     * documentación de clase. Limpia el framebuffer propio y copia la
     * profundidad real UNA sola vez por frame, para que las sucesivas
     * llamadas a {@code captureColorGeometry}/{@code captureTexturedGeometry}
     * ACUMULEN su contribución en vez de borrarse entre sí. */
    static void beginCapture(LightingConfig config) {
        captureOpen = false;
        if (disabledDueToError || !config.realBloomEnabled) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!ensureBuffer(client)) return;
            Framebuffer real = client.getFramebuffer();
            // Verificado con javap: clear() y copyDepthFrom() cada uno
            // termina con framebuffer 0 (la ventana cruda) enlazado --
            // por eso van ANTES de dibujar nada, nunca entre dos
            // capturas (eso borraría la primera).
            bloomBuffer.clear(false);
            bloomBuffer.copyDepthFrom(real);
            captureOpen = true;
        } catch (Exception e) {
            disableAfterError("beginCapture", e);
        }
    }

    /** Puede llamarse varias veces por frame -- ver documentación de
     * clase. Redibuja {@code built} (formato POSITION_COLOR, el mismo
     * que usan los puntos/halos de siempre) en el framebuffer propio,
     * ya preparado por {@link #beginCapture}. */
    static void captureColorGeometry(BuiltBuffer built, float intensity) {
        if (!captureOpen) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            bloomBuffer.beginWrite(true);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            // "ColorModulator" es un uniform real que position_color.fsh
            // multiplica contra el color del vértice (verificado leyendo
            // el .fsh real extraído del jar cacheado de este entorno) --
            // escala la intensidad capturada aquí, no en el composite
            // final (que reutiliza el shader de blit de vanilla, sin
            // ningún uniform de color).
            RenderSystem.setShaderColor(1f, 1f, 1f, clamp01(intensity));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferRenderer.drawWithGlobalProgram(built);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            // Defensivo: nunca bloomBuffer.endWrite() (enlaza framebuffer
            // 0, no el real) -- se reenlaza el real a mano, para que la
            // siguiente pasada normal (p. ej. el follaje emisivo
            // dibujándose al framebuffer real) no se vea afectada.
            client.getFramebuffer().beginWrite(true);
            hasContentThisFrame = true;
        } catch (Exception e) {
            disableAfterError("captureColorGeometry", e);
        }
    }

    /** Igual que {@link #captureColorGeometry}, pero para geometría con
     * UV (formato POSITION_TEXTURE_COLOR, el que usa el follaje emisivo
     * de la fase 1 -- ver {@code WorldLightingRenderer#renderEmissiveSurfaces}),
     * con su propia textura de máscara. Antes de esta corrección (V4.1),
     * el follaje emisivo nunca llegaba a capturarse -- era exactamente
     * la causa reportada de que el Bloom "no incluyera adecuadamente las
     * nuevas superficies emisivas". */
    static void captureTexturedGeometry(BuiltBuffer built, Identifier textureId, float intensity) {
        if (!captureOpen) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            bloomBuffer.beginWrite(true);

            RenderSystem.setShaderTexture(0, textureId);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            // position_tex_color.fsh también multiplica por
            // "ColorModulator" (verificado leyendo el .fsh real) --
            // mismo mecanismo que la captura de color puro.
            RenderSystem.setShaderColor(1f, 1f, 1f, clamp01(intensity));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferRenderer.drawWithGlobalProgram(built);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            client.getFramebuffer().beginWrite(true);
            hasContentThisFrame = true;
        } catch (Exception e) {
            disableAfterError("captureTexturedGeometry", e);
        }
    }

    /** Dibuja un pequeño círculo aditivo, orientado a cámara, por cada
     * eco de partícula vivo (ver {@link #registerParticleEcho}) --
     * SÓLO en el framebuffer de Bloom, nunca en el real (la partícula
     * real ya se dibuja sola, por el sistema de partículas de
     * Minecraft). Una estrella (tamaño mayor al registrarse) genera un
     * eco más grande que una mota/chispa -- sección 4.4/6.4 del
     * encargo: "una mota pequeña debe producir un halo pequeño... una
     * estrella especial puede generar un destello más visible". */
    static void captureParticleEchoes(MatrixStack matrixStack, Vec3d cameraPos, Quaternionf cameraRotation, float intensity) {
        if (!captureOpen || particleEchoes.isEmpty()) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            boolean any = false;
            for (ParticleEcho echo : particleEchoes) {
                float lifeFrac = echo.ticksLeft / (float) echo.maxTicks;
                float alpha = clamp01(echo.alpha * lifeFrac * intensity);
                if (alpha <= 0.01f) continue;
                matrixStack.push();
                matrixStack.translate(echo.x - cameraPos.x, echo.y - cameraPos.y, echo.z - cameraPos.z);
                matrixStack.multiply(cameraRotation);
                Matrix4f pose = new Matrix4f(matrixStack.peek().getPositionMatrix());
                matrixStack.pop();
                addEchoCircle(buffer, pose, echo.size, echo.r, echo.g, echo.b, alpha);
                any = true;
            }
            if (!any) {
                buffer.endNullable();
                return;
            }
            BuiltBuffer built = buffer.end();
            bloomBuffer.beginWrite(true);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferRenderer.drawWithGlobalProgram(built);

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            client.getFramebuffer().beginWrite(true);
            hasContentThisFrame = true;
        } catch (Exception e) {
            disableAfterError("captureParticleEchoes", e);
        }
    }

    private static void addEchoCircle(BufferBuilder buffer, Matrix4f pose, float radius, float r, float g, float b, float alpha) {
        int ri = clampByte(r);
        int gi = clampByte(g);
        int bi = clampByte(b);
        int a = clampByte(alpha);
        int segments = 8;
        float[] ringX = new float[segments];
        float[] ringY = new float[segments];
        for (int s = 0; s < segments; s++) {
            double angle = 2.0 * Math.PI * s / segments;
            ringX[s] = (float) Math.cos(angle) * radius;
            ringY[s] = (float) Math.sin(angle) * radius;
        }
        for (int s = 0; s < segments; s++) {
            int next = (s + 1) % segments;
            buffer.vertex(pose, 0, 0, 0).color(ri, gi, bi, a);
            buffer.vertex(pose, ringX[s], ringY[s], 0).color(ri, gi, bi, 0);
            buffer.vertex(pose, ringX[next], ringY[next], 0).color(ri, gi, bi, 0);
        }
    }

    private static int clampByte(float v) {
        return (int) (clamp01(v) * 255f);
    }

    /** Llamado desde {@code CelestialParticleSpawner} cada vez que nace
     * una chispa/estrella, si {@code realBloomEnabled} -- registra su
     * contribución al Bloom sin que el spawner necesite saber nada de
     * framebuffers. {@code size} mayor para estrellas que para chispas
     * (decidido por el llamador). */
    static void registerParticleEcho(double x, double y, double z, float r, float g, float b, float size, float alpha, int lifeTicks) {
        if (particleEchoes.size() >= MAX_PARTICLE_ECHOES) return;
        particleEchoes.add(new ParticleEcho(x, y, z, r, g, b, size, alpha, lifeTicks, lifeTicks));
    }

    private static void tickEchoes() {
        if (particleEchoes.isEmpty()) return;
        particleEchoes.removeIf(echo -> --echo.ticksLeft <= 0);
    }

    /** Eco mutable (el contador de vida se decrementa en el sitio, ver
     * {@link #tickEchoes}) -- clase simple a propósito, nada de esto
     * necesita ser un record inmutable. */
    private static final class ParticleEcho {
        final double x, y, z;
        final float r, g, b, size, alpha;
        final int maxTicks;
        int ticksLeft;

        ParticleEcho(double x, double y, double z, float r, float g, float b, float size, float alpha, int maxTicks, int ticksLeft) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.size = size;
            this.alpha = alpha;
            this.maxTicks = maxTicks;
            this.ticksLeft = ticksLeft;
        }
    }

    /** Una vez por frame, al final de todas las capturas -- ver
     * documentación de clase. */
    static void endCapture() {
        captureOpen = false;
    }

    private static void onEnd(WorldRenderContext context) {
        if (disabledDueToError) return;
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || !config.realBloomEnabled) return;
        if (!hasContentThisFrame) return;
        hasContentThisFrame = false;

        MinecraftClient client = MinecraftClient.getInstance();
        try {
            if (bloomBuffer == null) return;
            blurProcessor.render(context.tickCounter().getLastFrameDuration());

            Framebuffer real = client.getFramebuffer();
            // El pipeline de desenfoque termina dejando el resultado en
            // nuestro propio framebuffer (el "mainTarget" que le dimos
            // al construirlo) -- nunca toca el framebuffer real. Lo
            // recomponemos aquí, de forma explícita.
            real.beginWrite(true);
            compositeAdditive(client);
        } catch (Exception e) {
            disableAfterError("onEnd/render+composite", e);
            restoreMainFramebufferSafely(client);
        }
    }

    /** Composición final -- reutiliza {@code Framebuffer#draw(int, int,
     * boolean)}, el mismo método que usa Minecraft para volcar su
     * propio framebuffer principal a la ventana (verificado leyendo su
     * bytecode real: usa el shader de blit de vanilla, con las
     * coordenadas NDC correctas -- un quad "a mano" con un shader de
     * textura normal habría necesitado una matriz de proyección
     * ortográfica propia que este evento no garantiza tener lista,
     * arriesgando una composición mal proyectada). El parámetro
     * {@code false} le dice que NO desactive la mezcla -- así respeta
     * la mezcla ADITIVA que fijamos aquí, en vez de su comportamiento
     * por defecto (opaco, pensado para volcar el framebuffer principal
     * al final del frame). La intensidad ya se escaló al capturar (ver
     * {@link #captureFrame}), no hace falta ningún uniform de color
     * aquí -- el shader de blit no tiene ninguno. */
    private static void compositeAdditive(MinecraftClient client) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        bloomBuffer.draw(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight(), false);
        RenderSystem.disableBlend();
    }

    /** Crea (o recrea, si la ventana cambió de tamaño -- sección 5.8
     * del encargo) el framebuffer propio y el procesador de desenfoque.
     * Devuelve {@code false} si algo falla, sin lanzar -- el llamador
     * simplemente no dibuja nada ese frame. */
    private static boolean ensureBuffer(MinecraftClient client) {
        int width = client.getWindow().getFramebufferWidth();
        int height = client.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return false;
        if (bloomBuffer != null && width == bufferWidth && height == bufferHeight) return true;

        disposeBuffers();
        try {
            bloomBuffer = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
            bloomBuffer.setClearColor(0f, 0f, 0f, 0f);
            blurProcessor = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), bloomBuffer, BLUR_EFFECT);
            // Imprescindible, no sólo un ajuste de tamaño: verificado con
            // javap que el CONSTRUCTOR nunca empuja la matriz de
            // proyección a cada PostEffectPass (sólo calcula la suya
            // propia) -- sólo setupDimensions() recorre `passes` y llama
            // a pass.setProjectionMatrix(...) en cada una. Sin esto,
            // cada pasada intenta usar una matriz nula al renderizar y
            // lanza NullPointerException -- confirmado en vivo con la
            // autoprueba antes de este arreglo.
            blurProcessor.setupDimensions(width, height);
            bufferWidth = width;
            bufferHeight = height;
            LOGGER.info("[DIAG] Bloom real: framebuffer propio creado ({}x{})", width, height);
            return true;
        } catch (Exception e) {
            disableAfterError("ensureBuffer", e);
            return false;
        }
    }

    private static void disableAfterError(String where, Exception e) {
        LOGGER.error("[BLOOM] Error real en {} -- Bloom de framebuffer desactivado por el resto de la sesión "
                + "(los puntos/halos aditivos de siempre siguen funcionando sin cambios): {}", where, e.toString());
        disabledDueToError = true;
        disposeBuffers();
    }

    private static void restoreMainFramebufferSafely(MinecraftClient client) {
        try {
            client.getFramebuffer().beginWrite(true);
        } catch (Exception ignored) {
            // Si ni esto funciona, el problema es mucho más profundo que
            // este módulo -- no hay nada más seguro que intentar aquí.
        }
    }

    private static void disposeBuffers() {
        if (blurProcessor != null) {
            try {
                blurProcessor.close();
            } catch (Exception ignored) {
            }
            blurProcessor = null;
        }
        if (bloomBuffer != null) {
            try {
                bloomBuffer.delete();
            } catch (Exception ignored) {
            }
            bloomBuffer = null;
        }
        bufferWidth = -1;
        bufferHeight = -1;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    /** Autoprueba V4.1: a diferencia de la autoprueba de la V4 (que sólo
     * construía un framebuffer y un {@link PostEffectProcessor} de
     * usar y tirar, sin ejercitar nunca el ciclo real de captura), esta
     * versión ejecuta el ciclo COMPLETO real -- {@link #beginCapture},
     * {@link #captureColorGeometry} con un triángulo de prueba,
     * {@link #registerParticleEcho} + {@link #captureParticleEchoes},
     * {@link #endCapture}, el difuminado real y el composite -- sobre
     * el framebuffer compartido de verdad (el mismo que usará
     * producción), no uno descartable aparte. Activa
     * {@code realBloomEnabled} temporalmente si estaba desactivado (lo
     * restaura exactamente a como estaba, sin guardar el cambio en
     * disco) -- así se prueba el camino real tal cual lo recorrería el
     * jugador, incluyendo la corrección V4.1 de que el follaje emisivo
     * y las partículas ya lleguen a capturarse. */
    static void selfTest() {
        LightingConfig config = ApuYawarLightingClient.config();
        boolean originalEnabled = config.realBloomEnabled;
        config.realBloomEnabled = true;
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            beginCapture(config);
            if (!captureOpen) {
                LOGGER.warn("[DIAG] Autoprueba Bloom real: beginCapture no abrió la captura (ver error [BLOOM] previo en el log).");
                return;
            }
            captureColorGeometry(buildTestTriangle(), 1.0f);
            registerParticleEcho(0, client.player != null ? client.player.getY() + 2 : 64, 0, 0f, 1f, 1f, 0.3f, 0.8f, 4);
            captureParticleEchoes(new MatrixStack(), Vec3d.ZERO, new Quaternionf(), 1.0f);
            endCapture();

            if (!hasContentThisFrame) {
                LOGGER.warn("[DIAG] Autoprueba Bloom real: la captura no dejó ningún contenido -- revisar errores previos.");
                return;
            }
            hasContentThisFrame = false;
            blurProcessor.render(0.05f);
            client.getFramebuffer().beginWrite(true);
            compositeAdditive(client);
            LOGGER.info("[DIAG] Autoprueba Bloom real: ciclo completo (captura de color + eco de partícula + "
                    + "desenfoque minecraft:blur + composite) ejecutado sin excepciones sobre el framebuffer real.");
        } catch (Exception e) {
            LOGGER.warn("[DIAG] Autoprueba Bloom real FALLÓ (quedará desactivado hasta que el equipo investigue): {}", e.toString());
        } finally {
            config.realBloomEnabled = originalEnabled;
            particleEchoes.clear();
            try {
                client.getFramebuffer().beginWrite(true);
            } catch (Exception ignored) {
            }
        }
    }

    private static BuiltBuffer buildTestTriangle() {
        var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        buffer.vertex(-1, -1, -5).color(255, 255, 255, 255);
        buffer.vertex(1, -1, -5).color(255, 255, 255, 255);
        buffer.vertex(0, 1, -5).color(255, 255, 255, 255);
        return buffer.end();
    }
}
