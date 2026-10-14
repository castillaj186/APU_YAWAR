package pe.apuyawar.momentocuy.minigame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pe.apuyawar.momentocuy.dimension.MomentoCuyLobby;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Una partida en curso sobre una {@link Arena}: individual o multijugador
 * (ambas son simplemente "cuántos participantes tiene"), con estado real
 * ({@link MatchState}), tiempo controlado en ticks, y la capacidad de
 * finalizar o cancelar devolviendo a todos a la plataforma de llegada. No
 * decide CUÁNDO terminar (eso, otra vez, es regla de un minijuego concreto
 * que no existe todavía) — expone los métodos para que quien sepa la regla
 * los llame.
 */
public final class MinigameMatch {
    private final Arena arena;
    private final Map<UUID, MinigameParticipant> participants = new LinkedHashMap<>();
    private MatchState state = MatchState.WAITING;
    private long startTick = -1;

    public MinigameMatch(Arena arena) { this.arena = arena; }

    public Arena arena() { return arena; }
    public MatchState state() { return state; }

    public void join(ServerPlayerEntity player) {
        participants.computeIfAbsent(player.getUuid(), id -> new MinigameParticipant(id, player.getGameProfile().getName()));
    }

    public MinigameParticipant participant(UUID playerId) { return participants.get(playerId); }
    public Map<UUID, MinigameParticipant> participants() { return participants; }

    public void start(MinecraftServer server) {
        this.state = MatchState.RUNNING;
        this.startTick = server.getOverworld().getTime();
    }

    public long elapsedTicks(MinecraftServer server) {
        return startTick < 0 ? 0 : server.getOverworld().getTime() - startTick;
    }

    public void onCheckpoint(UUID playerId) {
        MinigameParticipant p = participants.get(playerId);
        if (p == null) return;
        int index = arena.checkpoints().isEmpty() ? -1 : p.checkpointIndex() + 1;
        if (index >= 0 && index < arena.checkpoints().size()) p.advanceCheckpoint(index);
    }

    public void onFinish(UUID playerId) {
        MinigameParticipant p = participants.get(playerId);
        if (p != null) p.markFinished();
    }

    /** Termina la partida devolviendo a todos los participantes conectados a
     * la plataforma de llegada -- nunca los deja varados dentro de la arena. */
    public void finish(MinecraftServer server) { end(server, MatchState.FINISHED, "La partida ha terminado."); }

    public void cancel(MinecraftServer server) { end(server, MatchState.CANCELLED, "La partida se cancelo."); }

    private void end(MinecraftServer server, MatchState finalState, String message) {
        this.state = finalState;
        for (UUID id : participants.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null) {
                player.sendMessage(Text.literal(message), false);
                player.teleport(player.getServerWorld(), MomentoCuyLobby.ENTRY_X, MomentoCuyLobby.ENTRY_Y, MomentoCuyLobby.ENTRY_Z, 180.0F, 0.0F);
            }
        }
    }
}
