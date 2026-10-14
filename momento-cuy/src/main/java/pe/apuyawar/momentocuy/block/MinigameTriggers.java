package pe.apuyawar.momentocuy.block;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bus de eventos para los bloques de minijuegos (checkpoint, meta, baldosa
 * activadora): el bloque no sabe a qué arena pertenece ni qué regla debe
 * aplicar — solo avisa aquí "algo se paró en esta posición", con su tipo.
 * El motor de minijuegos ({@code pe.apuyawar.momentocuy.minigame}) es quien
 * decide si esa posición le pertenece a alguna de sus arenas registradas y
 * qué hacer; sin ningún oyente registrado, el bloque simplemente no hace nada
 * más que existir — no se inventa ninguna mecánica aquí.
 */
public final class MinigameTriggers {
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private MinigameTriggers() {}

    public static void addListener(Listener listener) { LISTENERS.add(listener); }
    public static void removeListener(Listener listener) { LISTENERS.remove(listener); }

    static void fire(ServerWorld world, BlockPos pos, Entity entity, Kind kind) {
        for (Listener listener : LISTENERS) listener.onTrigger(world, pos, entity, kind);
    }

    public enum Kind { CHECKPOINT, FINISH, TRIGGER_TILE }

    @FunctionalInterface
    public interface Listener {
        void onTrigger(ServerWorld world, BlockPos pos, Entity entity, Kind kind);
    }
}
