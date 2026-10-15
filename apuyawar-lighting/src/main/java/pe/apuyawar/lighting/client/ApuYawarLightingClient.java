package pe.apuyawar.lighting.client;

import net.fabricmc.api.ClientModInitializer;
import pe.apuyawar.lighting.config.LightingConfig;

public final class ApuYawarLightingClient implements ClientModInitializer {
    private static LightingConfig config;

    public static LightingConfig config() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = LightingConfig.load();
        EmitterTracker.register();
        WorldLightingRenderer.register();
        LightingCommands.register();
    }
}
