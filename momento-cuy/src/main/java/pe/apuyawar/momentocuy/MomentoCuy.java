package pe.apuyawar.momentocuy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.momentocuy.block.MomentoCuyBlocks;
import pe.apuyawar.momentocuy.dimension.MomentoCuyDimension;
import pe.apuyawar.momentocuy.dimension.MomentoCuyReturnPoints;
import pe.apuyawar.momentocuy.entity.MomentoCuyEntities;
import pe.apuyawar.momentocuy.minigame.MinigameManager;

/**
 * Punto de entrada común (servidor + cliente) de "Momento Cuy". Ver
 * {@code docs/20_MOMENTO_CUY_DIMENSION.md} para la arquitectura completa.
 */
public final class MomentoCuy implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("momento-cuy");

    @Override
    public void onInitialize() {
        MomentoCuyBlocks.initialize();
        MomentoCuyEntities.initialize();
        MomentoCuyCommands.initialize();
        MinigameManager.initialize();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            MomentoCuyReturnPoints.load(server);
            if (server.getWorld(MomentoCuyDimension.WORLD) == null) {
                LOGGER.error("[MomentoCuy] La dimension momento_cuy no se cargo -- revisa data/momento_cuy/dimension/momento_cuy.json");
            } else {
                LOGGER.info("[MomentoCuy] Dimension disponible; /momentocuy entrar");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(MomentoCuyReturnPoints::save);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                MinigameManager.handleDisconnect(handler.getPlayer().getUuid()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                MinigameManager.handleReconnect(handler.getPlayer()));

        LOGGER.info("[MomentoCuy] Inicializado");
    }
}
