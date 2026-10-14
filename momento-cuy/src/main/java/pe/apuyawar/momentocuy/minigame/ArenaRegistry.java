package pe.apuyawar.momentocuy.minigame;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Arenas registradas en memoria para la sesión actual del servidor. No
 * persiste todavía entre reinicios (ver limitaciones conocidas en
 * {@code 20_MOMENTO_CUY_DIMENSION.md}) — las estructuras SÍ persisten porque
 * son bloques reales del mundo; lo que se pierde al reiniciar es solo el
 * registro en memoria de "estas coordenadas son la arena X", que el
 * constructor puede volver a registrar con un comando.
 */
public final class ArenaRegistry {
    private static final Map<String, Arena> ARENAS = new LinkedHashMap<>();

    private ArenaRegistry() {}

    public static void register(Arena arena) { ARENAS.put(arena.id(), arena); }
    public static void remove(String id) { ARENAS.remove(id); }
    public static Optional<Arena> get(String id) { return Optional.ofNullable(ARENAS.get(id)); }
    public static Collection<Arena> all() { return ARENAS.values(); }

    /** La primera arena registrada cuya región contiene esta posición, si
     * hay alguna — usado por {@link MinigameManager} para saber a qué arena
     * pertenece un evento de {@code MinigameTriggers}. */
    public static Optional<Arena> findContaining(net.minecraft.util.math.BlockPos pos) {
        return ARENAS.values().stream().filter(a -> a.contains(pos)).findFirst();
    }
}
