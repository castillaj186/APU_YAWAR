package pe.apuyawar.lighting.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        LOGGER.info("[DIAG] onInitializeClient() terminado sin excepciones");
    }
}
