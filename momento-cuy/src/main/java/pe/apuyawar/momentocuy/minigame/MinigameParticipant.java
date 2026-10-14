package pe.apuyawar.momentocuy.minigame;

import java.util.UUID;

/** Progreso de un jugador dentro de una {@link MinigameMatch}. Se conserva
 * (no se borra) cuando el jugador se desconecta -- ver
 * {@link MinigameManager#handleDisconnect}/{@code handleReconnect} -- para
 * poder recuperarlo si vuelve a entrar mientras la partida sigue activa. */
public final class MinigameParticipant {
    public final UUID playerId;
    public final String playerName;
    private int checkpointIndex = -1;
    private boolean finished = false;
    private boolean connected = true;

    public MinigameParticipant(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public int checkpointIndex() { return checkpointIndex; }
    public void advanceCheckpoint(int index) { if (index > this.checkpointIndex) this.checkpointIndex = index; }
    public boolean isFinished() { return finished; }
    public void markFinished() { this.finished = true; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
}
