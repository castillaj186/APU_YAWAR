package pe.apuyawar.lighting.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.lighting.ApuYawarLighting;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.config.LightingConfig;

public final class ApuYawarLightingClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("apuyawar-lighting");
    private static LightingConfig config;

    public static LightingConfig config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DIAG] ApuYawarLightingClient.onInitializeClient() invocado");
        config = LightingConfig.load();
        EmitterTracker.register();
        WorldLightingRenderer.register();
        LightingCommands.register();
        registerEmissiveMaterialSelfTest();
        LOGGER.info("[DIAG] onInitializeClient() terminado sin excepciones");
    }

    /** V4, fase 1, diagnóstico temporal (igual que los demás "[DIAG]" de
     * esta ronda): a los ~2 segundos de arrancar el cliente (tiempo de
     * sobra para que {@code MomentoCuyLightingBridge} ya haya
     * registrado sus emisores, sin depender del orden entre
     * entrypoints de distintos mods), fuerza una vez la generación de
     * la máscara emisiva de cada perfil registrado y deja el resultado
     * en el log -- así se puede confirmar que
     * {@code EmissiveMaskGenerator} funciona de verdad sin necesitar
     * escribir ningún comando dentro del juego. Se ejecuta una sola vez
     * por sesión. */
    private static void registerEmissiveMaterialSelfTest() {
        int[] tickCount = {0};
        boolean[] done = {false};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (done[0]) return;
            if (++tickCount[0] < 40) return;
            done[0] = true;
            var registered = ApuYawarLighting.registeredEmitters();
            int checked = 0;
            int ok = 0;
            for (var entry : registered.entrySet()) {
                EmitterProfile profile = entry.getValue();
                if (!profile.hasEmissiveMask()) continue;
                checked++;
                int rgb = (clampByte01(profile.red()) << 16) | (clampByte01(profile.green()) << 8) | clampByte01(profile.blue());
                var result = EmissiveMaskGenerator.getOrCreate(client.getResourceManager(), profile.emissiveMaskSource(), rgb);
                if (result.isPresent()) {
                    ok++;
                    LOGGER.info("[DIAG] Autoprueba material emisivo: {} -> OK ({})", entry.getKey(), result.get());
                } else {
                    LOGGER.warn("[DIAG] Autoprueba material emisivo: {} -> FALLÓ", entry.getKey());
                }
            }
            LOGGER.info("[DIAG] Autoprueba material emisivo terminada: {} perfiles con máscara, {} generados.", checked, ok);
        });
    }

    private static int clampByte01(float v) {
        return Math.max(0, Math.min(255, (int) (v * 255f)));
    }
}
