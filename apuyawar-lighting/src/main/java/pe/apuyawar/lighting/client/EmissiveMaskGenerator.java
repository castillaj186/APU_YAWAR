package pe.apuyawar.lighting.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * APU YAWAR Lighting V4, fase 1 ("follaje con materiales o superficies
 * emisivas"): deriva, de la textura REAL de un bloque de hoja (la misma
 * que ya usa su modelo -- no una textura nueva dibujada a mano), una
 * máscara con sólo sus píxeles más claros vueltos emisivos. El resto de
 * la textura queda completamente transparente en la máscara, así que al
 * dibujarla encima con mezcla aditiva sólo esos píxeles "brillan",
 * exactamente lo que pidió el encargo en la sección 3.3: "una máscara
 * puede identificar regiones donde exista iluminación: píxeles claros,
 * pequeñas grietas, bordes seleccionados... no quiero que toda la
 * textura sea emisiva".
 *
 * <p><b>Diferencia real con un billboard</b>: esta máscara se dibuja
 * como un cubo con las UV 0..1 de cada cara (ver
 * {@code WorldLightingRenderer#renderEmissiveSurfaces}), en la posición
 * y orientación real del bloque -- no un punto flotante orientado a la
 * cámara. Apagar los puntos/halos de siempre (comando
 * {@code debug puntos}) no apaga esto, porque son dos sistemas de
 * dibujo independientes (ver criterio de aceptación de la fase 1 en
 * docs/23).</p>
 *
 * <p><b>Umbral adaptativo por textura</b>: en vez de un umbral de brillo
 * fijo (que dejaría una textura ya muy clara completamente emisiva, o
 * una muy oscura sin ningún píxel emisivo), el umbral es relativo al
 * brillo máximo real de cada textura concreta -- siempre selecciona
 * "los píxeles más claros de ESTA textura", nunca "todo lo que supere
 * un número absoluto".</p>
 */
final class EmissiveMaskGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-lighting");
    private static final String TEXTURE_PATH_PREFIX = "textures/";
    private static final String TEXTURE_PATH_SUFFIX = ".png";

    /** Fracción del brillo máximo de la textura por encima de la cual un
     * píxel opaco se considera "emisivo" -- 1.0 dejaría sólo el píxel
     * más claro, 0.0 dejaría toda la textura emisiva. */
    private static final float BRIGHTNESS_THRESHOLD_FRACTION = 0.74f;
    /** Nunca más de esta fracción de los píxeles opacos de la textura
     * puede terminar siendo emisiva -- evita "toda la textura emisiva"
     * en texturas muy uniformes (sección 3.3 del encargo). */
    private static final float MAX_EMISSIVE_FRACTION = 0.30f;
    /** Nunca menos de esta fracción, para que una textura muy plana
     * siga recibiendo algunos píxeles emisivos reconocibles. */
    private static final float MIN_EMISSIVE_FRACTION = 0.04f;

    private static final Map<Long, Identifier> cache = new HashMap<>();

    private EmissiveMaskGenerator() {}

    /** Devuelve el identificador de una textura dinámica ya registrada
     * en el {@link TextureManager} con la máscara emisiva de
     * {@code sourceTexture} teñida con {@code rgb} -- la genera y la
     * cachea la primera vez que se pide ese par (textura, color); no
     * vuelve a tocar el disco en rescaneos posteriores. */
    static Optional<Identifier> getOrCreate(ResourceManager resourceManager, Identifier sourceTexture, int rgb) {
        long key = (sourceTexture.hashCode() & 0xFFFFFFFFL) ^ ((long) rgb << 32);
        Identifier cached = cache.get(key);
        if (cached != null) return Optional.of(cached);

        Identifier resourcePath = Identifier.of(sourceTexture.getNamespace(),
                TEXTURE_PATH_PREFIX + sourceTexture.getPath() + TEXTURE_PATH_SUFFIX);
        try {
            NativeImage mask = buildMask(resourceManager, resourcePath, rgb);
            if (mask == null) return Optional.empty();
            Identifier maskId = Identifier.of(sourceTexture.getNamespace(),
                    "apuyawar_lighting_emissive_mask/" + sourceTexture.getPath().replace('/', '_') + "_" + Integer.toHexString(rgb));
            MinecraftClient.getInstance().getTextureManager().registerTexture(maskId, new NativeImageBackedTexture(mask));
            cache.put(key, maskId);
            LOGGER.info("[DIAG] Máscara emisiva generada para {} (color #{}) -> {}",
                    resourcePath, Integer.toHexString(rgb), maskId);
            return Optional.of(maskId);
        } catch (IOException e) {
            LOGGER.warn("No se pudo generar la máscara emisiva de {}: {}", resourcePath, e.toString());
            return Optional.empty();
        }
    }

    private static NativeImage buildMask(ResourceManager resourceManager, Identifier resourcePath, int rgb) throws IOException {
        Optional<Resource> resource = resourceManager.getResource(resourcePath);
        if (resource.isEmpty()) {
            LOGGER.warn("Textura base para máscara emisiva no encontrada: {}", resourcePath);
            return null;
        }
        NativeImage source;
        try (InputStream stream = resource.get().getInputStream()) {
            source = NativeImage.read(stream);
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int maxLuminance = 0;
        int[][] luminance = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = source.getColor(x, y);
                int alpha = ColorHelper.Abgr.getAlpha(color);
                if (alpha == 0) continue;
                int lum = perceivedLuminance(color);
                luminance[x][y] = lum;
                if (lum > maxLuminance) maxLuminance = lum;
            }
        }

        if (maxLuminance == 0) {
            source.close();
            return null;
        }

        int threshold = adaptiveThreshold(luminance, width, height, maxLuminance);
        int maskRgbNoAlpha = rgb & 0x00FFFFFF;
        NativeImage mask = new NativeImage(width, height, true);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = source.getColor(x, y);
                int alpha = ColorHelper.Abgr.getAlpha(color);
                if (alpha == 0 || luminance[x][y] < threshold) {
                    mask.setColor(x, y, 0);
                    continue;
                }
                // El alfa de la máscara combina el alfa original (para no
                // "rellenar" píxeles que la hoja original dejaba
                // transparentes) con lo brillante que era ese píxel
                // relativo al máximo -- un píxel apenas por encima del
                // umbral brilla menos que el más claro de la textura.
                float relative = clamp01((luminance[x][y] - threshold) / (float) Math.max(1, maxLuminance - threshold));
                int maskAlpha = (int) (alpha * (0.35f + 0.65f * relative));
                // ColorHelper.Abgr.getAbgr(alpha, blue, green, red) -- el
                // orden de parámetros NO es (a,r,g,b); se verificó con
                // javap contra el jar real antes de usarlo (un primer
                // intento tenía rojo y azul intercambiados).
                int red = (maskRgbNoAlpha >> 16) & 0xFF;
                int green = (maskRgbNoAlpha >> 8) & 0xFF;
                int blue = maskRgbNoAlpha & 0xFF;
                mask.setColor(x, y, ColorHelper.Abgr.getAbgr(maskAlpha, blue, green, red));
            }
        }
        source.close();
        return mask;
    }

    /** Baja el umbral relativo (sección "no quiero menos de X%") o lo
     * sube (sección "no quiero toda la textura emisiva") hasta que la
     * fracción de píxeles opacos que califican quede dentro del rango
     * aceptado -- sigue siendo determinista, no aleatorio. */
    private static int adaptiveThreshold(int[][] luminance, int width, int height, int maxLuminance) {
        int opaqueCount = 0;
        int[] histogram = new int[256];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int lum = luminance[x][y];
                if (lum <= 0) continue;
                opaqueCount++;
                histogram[Math.min(255, lum)]++;
            }
        }
        if (opaqueCount == 0) return maxLuminance;

        int base = (int) (maxLuminance * BRIGHTNESS_THRESHOLD_FRACTION);
        int qualifying = countAtOrAbove(histogram, base);
        float fraction = qualifying / (float) opaqueCount;
        if (fraction <= MAX_EMISSIVE_FRACTION && fraction >= MIN_EMISSIVE_FRACTION) return base;

        // Busca, entre 0 y 255, el umbral más alto que todavía deje al
        // menos MIN_EMISSIVE_FRACTION de los píxeles opacos calificando
        // (evita una textura sin ningún punto emisivo reconocible).
        for (int t = 255; t >= 0; t--) {
            float f = countAtOrAbove(histogram, t) / (float) opaqueCount;
            if (f >= MIN_EMISSIVE_FRACTION) return t;
        }
        return 0;
    }

    private static int countAtOrAbove(int[] histogram, int threshold) {
        int count = 0;
        for (int i = Math.max(0, threshold); i < histogram.length; i++) count += histogram[i];
        return count;
    }

    private static int perceivedLuminance(int abgrColor) {
        int r = ColorHelper.Abgr.getRed(abgrColor);
        int g = ColorHelper.Abgr.getGreen(abgrColor);
        int b = ColorHelper.Abgr.getBlue(abgrColor);
        // Luma perceptual estándar (ITU-R BT.601) -- consistente con cómo
        // el ojo pondera cada canal, no un promedio simple.
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
}
