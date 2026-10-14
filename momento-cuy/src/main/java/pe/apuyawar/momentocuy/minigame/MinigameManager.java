package pe.apuyawar.momentocuy.minigame;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import pe.apuyawar.momentocuy.block.MinigameTriggers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conecta los eventos de {@link MinigameTriggers} (bloques de checkpoint/
 * meta pisados) con la {@link MinigameMatch} activa de la arena a la que
 * pertenece esa posición, y gestiona la desconexión/reconexión de
 * participantes. Un único punto de entrada, registrado una vez desde
 * {@code MomentoCuy.onInitialize()}.
 */
public final class MinigameManager {
    private static final Map<String, MinigameMatch> ACTIVE_MATCHES = new LinkedHashMap<>();

    private MinigameManager() {}

    public static void initialize() {
        MinigameTriggers.addListener(MinigameManager::onTrigger);
    }

    public static MinigameMatch startMatch(Arena arena) {
        MinigameMatch match = new MinigameMatch(arena);
        ACTIVE_MATCHES.put(arena.id(), match);
        return match;
    }

    public static MinigameMatch activeMatch(String arenaId) { return ACTIVE_MATCHES.get(arenaId); }

    private static void onTrigger(ServerWorld world, net.minecraft.util.math.BlockPos pos, Entity entity, MinigameTriggers.Kind kind) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        ArenaRegistry.findContaining(pos).ifPresent(arena -> {
            MinigameMatch match = ACTIVE_MATCHES.get(arena.id());
            if (match == null || match.state() != MatchState.RUNNING) return;
            switch (kind) {
                case CHECKPOINT -> match.onCheckpoint(player.getUuid());
                case FINISH -> match.onFinish(player.getUuid());
                case TRIGGER_TILE -> { /* sin regla propia todavia; el evento ya llego hasta aqui para quien la añada */ }
            }
        });
    }

    /** No elimina al participante -- solo lo marca desconectado, para poder
     * recuperarlo si vuelve a entrar mientras la partida sigue activa (pedido
     * explícitamente en el encargo). */
    public static void handleDisconnect(UUID playerId) {
        for (MinigameMatch match : ACTIVE_MATCHES.values()) {
            MinigameParticipant p = match.participant(playerId);
            if (p != null) p.setConnected(false);
        }
    }

    /** Si el jugador que se reconecta tenía una partida activa en curso, lo
     * marca conectado de nuevo y avisa en el chat en qué partida/checkpoint
     * se había quedado -- no lo reinscribe automáticamente en la arena
     * físicamente (no se asume que reaparecer dentro de una arena en curso
     * sea seguro); el jugador decide si quiere volver a entrar. */
    public static void handleReconnect(ServerPlayerEntity player) {
        for (MinigameMatch match : ACTIVE_MATCHES.values()) {
            MinigameParticipant p = match.participant(player.getUuid());
            if (p != null && !p.isConnected()) {
                p.setConnected(true);
                player.sendMessage(net.minecraft.text.Text.literal(
                        "[Momento Cuy] Tenias una partida activa en '" + match.arena().id() + "' (checkpoint " + (p.checkpointIndex() + 1) + "). Usa /momentocuy minijuego volver para continuar."), false);
            }
        }
    }
}
