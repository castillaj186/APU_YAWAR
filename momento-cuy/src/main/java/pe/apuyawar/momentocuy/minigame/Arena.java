package pe.apuyawar.momentocuy.minigame;

import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Una región registrada de minijuego: un identificador, su punto de inicio,
 * sus checkpoints en orden, su meta, y el rectángulo (dos esquinas) que
 * delimita su zona — no para impedir que se construya ahí (esta etapa no
 * implementa protección de bloques, ver {@code 20_MOMENTO_CUY_DIMENSION.md}),
 * sino para que {@link MinigameManager} sepa a qué arena pertenece un evento
 * de {@code MinigameTriggers} según la posición del bloque.
 *
 * <p>No define ninguna regla de victoria/derrota: eso es, a propósito, trabajo
 * de una fase futura cuando el equipo decida qué minijuegos concretos quiere.
 * Esta clase es solo el contenedor de datos que el constructor rellena con
 * {@code /momentocuy arena registrar}.
 */
public record Arena(String id, BlockPos corner1, BlockPos corner2, BlockPos startPoint, List<BlockPos> checkpoints, BlockPos finishPoint) {

    public boolean contains(BlockPos pos) {
        int minX = Math.min(corner1.getX(), corner2.getX()), maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY()), maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ()), maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public int checkpointIndexAt(BlockPos pos) {
        return checkpoints.indexOf(pos);
    }

    public boolean isFinish(BlockPos pos) {
        return finishPoint != null && finishPoint.equals(pos);
    }
}
