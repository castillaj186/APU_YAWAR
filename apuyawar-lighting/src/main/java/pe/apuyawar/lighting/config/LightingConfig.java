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
    public float particleDensityMultiplier = LightingQuality.MEDIUM.particleDensityMultiplier;
    public float dayIntensityMultiplier = 0.55f;
    public float nightIntensityMultiplier = 1.0f;
    public int maxActiveLights = LightingQuality.MEDIUM.maxActiveLights;

    /** Color del emisor de prueba de la Fase A (`/apuyawarlighting test`),
     * sin relación con los perfiles reales de árboles. */
    public int testEmitterColor = 0x00FFF0;

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
