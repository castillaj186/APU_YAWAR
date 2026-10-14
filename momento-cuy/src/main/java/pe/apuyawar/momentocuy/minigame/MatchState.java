package pe.apuyawar.momentocuy.minigame;

/** Estado real de una {@link MinigameMatch}. No codifica ninguna regla de
 * victoria concreta — solo el ciclo de vida genérico que pidió el encargo. */
public enum MatchState {
    WAITING,
    RUNNING,
    FINISHED,
    CANCELLED
}
