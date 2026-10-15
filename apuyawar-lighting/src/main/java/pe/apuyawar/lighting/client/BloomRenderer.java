package pe.apuyawar.lighting.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.lighting.config.LightingConfig;

/**
 * APU YAWAR Lighting V4, fase 3: Bloom REAL de framebuffer (sección 5
 * del encargo: "quiero implementar Bloom real de framebuffer, no
 * solamente círculos aditivos superpuestos"). Arquitectura:
 *
 * <ol>
 * <li>Durante {@code WorldLightingRenderer#render} (AFTER_TRANSLUCENT),
 *     justo después de dibujar los puntos/halos aditivos al framebuffer
 *     real del juego (comportamiento SIN cambios, sigue siendo lo que
 *     siempre se ve), {@link #captureFrame} redibuja exactamente la
 *     misma geometría ya construida en un framebuffer PROPIO, separado
 *     -- nunca en {@code minecraft:main} directamente.</li>
 * <li>En {@code WorldRenderEvents.END} (después de que todo el render
 *     del mundo terminó, antes de la GUI -- ver la documentación de ese
 *     evento en {@code fabric-rendering-v1}), {@link #onEnd} difumina
 *     ESE framebuffer propio con el mismo pipeline multipasada que ya
 *     usa vanilla para su propio desenfoque ({@code minecraft:blur},
 *     reutilizado tal cual -- ni el GLSL ni la secuencia de pasadas son
 *     nuevos, sólo el framebuffer de destino es el nuestro, nunca
 *     {@code minecraft:main}) y lo compone de vuelta sobre el
 *     framebuffer real con mezcla aditiva, antes de que nada de GUI se
 *     dibuje.</li>
 * </ol>
 *
 * <p><b>Por qué nunca puede desenfocar el Panel K (sección 5.2 del
 * encargo, restricción absoluta)</b>: el pipeline de desenfoque de
 * vanilla sólo toca el framebuffer que se le pasa como "mainTarget" al
 * construir el {@link PostEffectProcessor} -- en este caso, siempre
 * nuestro framebuffer separado, nunca el framebuffer real del juego.
 * El framebuffer real del juego sólo recibe, al final, un composite
 * ADITIVO de lo YA difuminado -- la GUI, dibujada después de este
 * evento, nunca pasa por ningún desenfoque.</p>
 *
 * <p><b>Desactivado por defecto</b> ({@code LightingConfig#realBloomEnabled}):
 * a diferencia de las fases 1 y 2, este código sólo se ejecuta cuando
 * hay un mundo cargado con emisores activos -- en este entorno nunca
 * se pudo probar más allá de la pantalla de título (sin excepciones,
 * pero sin ejecutar nunca la ruta real). Queda activable por comando
 * (`/apuyawarlighting bloomreal`) para que el equipo lo pruebe de forma
 * explícita, nunca como comportamiento por defecto al actualizar.</p>
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

    private BloomRenderer() {}

    static void register() {
        WorldRenderEvents.END.register(BloomRenderer::onEnd);
    }

    /** Llamado desde {@code WorldLightingRenderer#render}, justo
     * después de dibujar al framebuffer real -- ver documentación de
     * clase, punto 1. Redibuja el mismo {@link BuiltBuffer} (sin volver
     * a construir ninguna geometría) en nuestro framebuffer propio. */
    static void captureFrame(BuiltBuffer built, LightingConfig config) {
        if (disabledDueToError || !config.realBloomEnabled) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!ensureBuffer(client)) return;

            Framebuffer real = client.getFramebuffer();
            // Los tres pasos siguientes, verificados con javap uno por
            // uno, cada uno deja framebuffer 0 (la ventana cruda)
            // enlazado al terminar -- clear() llama a endWrite() por
            // dentro, y copyDepthFrom() hace su propio bind/blit/unbind.
            // Por eso el beginWrite(true) de bloomBuffer va DESPUÉS de
            // ambos, nunca antes: es el único que de verdad deja
            // bloomBuffer listo para dibujar.
            bloomBuffer.clear(false); // limpia color+profundidad a transparente
            bloomBuffer.copyDepthFrom(real); // profundidad real, para que el test de profundidad oculte tras paredes
            bloomBuffer.beginWrite(true); // AHORA sí queda bloomBuffer enlazado para dibujar

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            // Escala la intensidad capturada aquí (no en el composite
            // final, que reutiliza el shader de blit de vanilla sin
            // ningún uniform de color) -- "ColorModulator" es un
            // uniform real que position_color.fsh multiplica contra el
            // color del vértice, verificado leyendo el .fsh real
            // extraído del jar cacheado de este entorno.
            RenderSystem.setShaderColor(1f, 1f, 1f, clamp01(config.bloomIntensity));
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

            // Defensivo: NUNCA llamar a bloomBuffer.endWrite() -- ese
            // método, verificado con javap contra el jar real, enlaza
            // el framebuffer 0 (la superficie cruda de la ventana), NO
            // el framebuffer "principal" real de Minecraft. Hay que
            // volver a enlazar explícitamente el framebuffer real.
            real.beginWrite(true);
            hasContentThisFrame = true;
        } catch (Exception e) {
            disableAfterError("captureFrame", e);
        }
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

    /** Autoprueba (igual que la de la fase 1, ver
     * {@code ApuYawarLightingClient}): fuerza, aunque
     * {@code realBloomEnabled} esté desactivado, la creación real del
     * framebuffer propio y del {@link PostEffectProcessor} (que
     * resuelve/parsea de verdad {@code assets/minecraft/shaders/post/blur.json}
     * contra el {@code ResourceManager} real) y ejecuta un
     * {@code render()} de prueba sobre un framebuffer vacío -- sin esto,
     * esta fase sólo se habría probado hasta "compila", nunca "corre de
     * verdad dentro del cliente", igual que las fases 1 y 2. No deja el
     * resultado activo para el render normal: destruye los objetos de
     * prueba al terminar, para no interferir con la primera vez que el
     * equipo lo active de verdad por comando. */
    static void selfTest() {
        MinecraftClient client = MinecraftClient.getInstance();
        // Framebuffer NO implementa AutoCloseable (verificado con javap)
        // -- se libera a mano con delete(), no con try-with-resources.
        SimpleFramebuffer testBuffer = null;
        PostEffectProcessor testProcessor = null;
        try {
            int width = Math.max(64, client.getWindow().getFramebufferWidth());
            int height = Math.max(64, client.getWindow().getFramebufferHeight());
            testBuffer = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
            testBuffer.setClearColor(0f, 0f, 0f, 0f);
            testBuffer.clear(false);
            testProcessor = new PostEffectProcessor(
                    client.getTextureManager(), client.getResourceManager(), testBuffer, BLUR_EFFECT);
            testProcessor.setupDimensions(width, height);
            testProcessor.render(0.05f);
            LOGGER.info("[DIAG] Autoprueba Bloom real: framebuffer ({}x{}) y pipeline minecraft:blur "
                    + "construidos y ejecutados sin excepciones.", width, height);
        } catch (Exception e) {
            LOGGER.warn("[DIAG] Autoprueba Bloom real FALLÓ (quedará desactivado hasta que el equipo investigue): {}", e.toString());
        } finally {
            if (testProcessor != null) {
                try {
                    testProcessor.close();
                } catch (Exception ignored) {
                }
            }
            if (testBuffer != null) {
                try {
                    testBuffer.delete();
                } catch (Exception ignored) {
                }
            }
            try {
                client.getFramebuffer().beginWrite(true);
            } catch (Exception ignored) {
            }
        }
    }
}
