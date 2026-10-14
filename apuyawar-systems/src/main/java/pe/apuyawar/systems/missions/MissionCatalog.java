package pe.apuyawar.systems.missions;

import java.util.List;
import java.util.Optional;

/**
 * Catálogo mínimo y real de misiones (no decenas inventadas). Ligadas a un evento
 * de servidor genuino y ya existente en el proyecto: la conexión del jugador
 * (mismo hook que ya usa {@code JudgmentEvents} vía {@code ServerPlayConnectionEvents.JOIN}).
 * Pensado para ampliarse más adelante sin romper el formato.
 */
public final class MissionCatalog {
    public static final List<MissionDefinition> ALL = List.of(
            new MissionDefinition("bienvenida", "Bienvenida a APU YAWAR",
                    "Conéctate al servidor.", 1, 5),
            new MissionDefinition("visitante_frecuente", "Visitante Frecuente",
                    "Conéctate al servidor 5 veces.", 5, 15)
    );
    private MissionCatalog() {}
    public static Optional<MissionDefinition> byId(String id) {
        return ALL.stream().filter(m -> m.id().equals(id)).findFirst();
    }
}
