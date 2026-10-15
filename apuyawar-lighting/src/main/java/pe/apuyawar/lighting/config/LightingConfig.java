package pe.apuyawar.lighting.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuración de cliente de APU YAWAR Lighting — JSON en
 * {@code config/apuyawar_lighting.json}, editable sin recompilar y
 * ajustable en caliente por el comando {@code /apuyawarlighting} (ver
 * {@code client.LightingCommands}). No requiere reiniciar el juego: cada
 * campo se lee directamente desde el renderizador en cada frame/tick, así
 * que un cambio aplicado por comando es inmediato.
 */
public final class LightingConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("apuyawar_lighting.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean rgbEnabled = true;
    public LightingQuality quality = LightingQuality.MEDIUM;

    public float lightIntensityMultiplier = LightingQuality.MEDIUM.lightIntensityMultiplier;
    public float lightRadiusMultiplier = LightingQuality.MEDIUM.lightRadiusMultiplier;
    public float bloomIntensity = LightingQuality.MEDIUM.bloomIntensity;
    public float bloomRadiusMultiplier = LightingQuality.MEDIUM.bloomRadiusMultiplier;
    public float emissiveMaterialIntensity = 1.0f;
    /** Reservado/sin usar en el renderizador: ya no controla el muestreo
     * de puntos fijos de la copa (eso ahora es
     * {@link #fixedPointDensityMultiplier}) — se conserva el campo, con su
     * nombre y valor, únicamente para no romper
     * {@code config/apuyawar_lighting.json} ya guardados por el equipo;
     * queda reservado para una futura integración con las partículas
     * móviles de {@code MomentoCuyAbetoLeavesBlock} (ver docs/23, sección
     * "Partículas celestiales" — no se conectó todavía porque esas clases
     * son bloques comunes que deben cargar sin {@code apuyawar-lighting}
     * instalado, y acoplarlas directamente a esta config rompería esa
     * garantía). */
    public float particleDensityMultiplier = LightingQuality.MEDIUM.particleDensityMultiplier;
    public float dayIntensityMultiplier = 0.55f;
    public float nightIntensityMultiplier = 1.0f;
    public int maxActiveLights = LightingQuality.MEDIUM.maxActiveLights;

    /** Color del emisor de prueba de la Fase A (`/apuyawarlighting test`),
     * sin relación con los perfiles reales de árboles. */
    public int testEmitterColor = 0x00FFF0;

    // --- Campos nuevos de esta ronda de corrección (ver docs/23, sección
    // "Corrección de anclaje espacial, decals y efectos celestiales") ---

    /** Densidad de los puntos de luz FIJOS de la copa (núcleo + halos) —
     * separado de {@link #particleDensityMultiplier} (sección 11 del
     * encargo: "separar densidad de puntos luminosos fijos de densidad de
     * partículas móviles"). Reemplaza al campo anterior en
     * {@code WorldLightingRenderer}/{@code EmitterSampler}. */
    public float fixedPointDensityMultiplier = LightingQuality.MEDIUM.particleDensityMultiplier;

    /** Si los emisores dibujan una mancha de luz en el suelo debajo de
     * ellos. Se puede desactivar por completo (sección 6 del encargo:
     * "si el decal de suelo actual no puede producir un resultado visual
     * coherente, desactívalo... manteniendo los puntos de la copa") sin
     * perder los puntos luminosos de la copa. */
    public boolean groundDecalsEnabled = true;

    /** Multiplicador adicional sólo para la intensidad del decal de
     * suelo, independiente de {@link #lightIntensityMultiplier}. */
    public float groundDecalIntensityMultiplier = 1.0f;

    /** Ticks para que un emisor que entra/sale del rango de efecto suba o
     * baje su intensidad de 0 a 1 (o viceversa) — evita el salto brusco de
     * oscuro a brillante de golpe (sección 7 del encargo). 1 tick = muy
     * brusco; valores más altos son más suaves. */
    public int fadeTransitionTicks = 12;

    /** Distancia máxima (en bloques) a la que un emisor puede llegar a
     * verse, independiente del radio interno de escaneo de
     * {@code EmitterTracker} (que es sólo una optimización de
     * rendimiento, no un límite visual) — sección 7/12 del encargo: "no
     * confundas el radio de influencia luminosa con la distancia máxima
     * de renderizado". */
    public float maxEffectDistance = 40.0f;

    /** Más allá de esta distancia (bloques), se deja de dibujar el halo
     * secundario y el decal de suelo de cada punto — sólo el núcleo — como
     * simplificación de nivel de detalle a media distancia (sección 12:
     * "distancia intermedia: menos partículas, pero suficientes luces
     * representativas"). */
    public float simplifyDetailBeyondDistance = 20.0f;

    // --- Modo de depuración (sección 17 del encargo) — desactivado por
    // defecto, pensado para aislar problemas, no para dejarlo activado en
    // juego normal. ---
    public boolean debugHideFixedPoints = false;

    public void applyPreset(LightingQuality preset) {
        this.quality = preset;
        if (preset == LightingQuality.CUSTOM) return;
        this.lightIntensityMultiplier = preset.lightIntensityMultiplier;
        this.lightRadiusMultiplier = preset.lightRadiusMultiplier;
        this.bloomIntensity = preset.bloomIntensity;
        this.bloomRadiusMultiplier = preset.bloomRadiusMultiplier;
        this.particleDensityMultiplier = preset.particleDensityMultiplier;
        this.maxActiveLights = preset.maxActiveLights;
    }

    /** Marca el perfil activo como personalizado — se llama tras cualquier
     * ajuste manual de un campo individual por comando. */
    public void markCustom() {
        this.quality = LightingQuality.CUSTOM;
    }

    public static LightingConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                LightingConfig loaded = GSON.fromJson(reader, LightingConfig.class);
                if (loaded != null) return loaded;
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                // Config corrupta o ilegible: seguimos con los valores por
                // defecto en memoria y la sobrescribimos en el próximo save(),
                // en vez de bloquear el arranque del cliente por esto.
            }
        }
        LightingConfig fresh = new LightingConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            // No hay nada sensato que hacer si el disco falla aquí; el
            // motor sigue funcionando con los valores actuales en memoria.
        }
    }
}
