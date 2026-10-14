package pe.apuyawar.systems.roulette;

/** En coordenadas GUI (Y hacia abajo), +45 grados avanza al vecino horario. */
public final class RouletteFillSequence {
    private RouletteFillSequence() {}
    public static int slot(int winner, int rank) { return Math.floorMod(winner + rank, 8); }
    public static int rank(int winner, int slot) { return Math.floorMod(slot - winner, 8); }
    public static int start(int winner, int slot) {
        return RouletteTimeline.FILL_START + rank(winner, slot) * RouletteTimeline.FILL_STEP;
    }
}
