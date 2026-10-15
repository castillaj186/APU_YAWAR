package pe.apuyawar.lighting;

import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Convierte un {@link EmitterProfile} de tipo {@link EmitterType#FOLIAGE}
 * en un número fijo y pequeño de puntos de luz dentro de un bloque — nunca
 * uno por cara ni uno por frame aleatorio (sección 8/12 del encargo:
 * "no quiero que se reorganicen aleatoriamente cada frame" y "no crear una
 * luz GPU por cada bloque de hoja"). La semilla depende únicamente de la
 * posición del bloque ({@link BlockPos#asLong()}), así que para una misma
 * hoja los puntos son siempre los mismos, en cada frame y cada sesión.
 */
public final class EmitterSampler {
    private EmitterSampler() {}

    /** Offset local (0..1 en cada eje) dentro del bloque para el punto
     * {@code index} (0-based, {@code index < profile.pointsPerBlock()}). */
    public static float[] samplePoint(BlockPos pos, EmitterProfile profile, int index) {
        Random random = new Random(pos.asLong() * 31L + index);
        // Margen hacia el centro: evita puntos exactamente en la cara del
        // bloque, que se verían "pegados" a la textura vecina.
        float x = 0.2f + random.nextFloat() * 0.6f;
        float y = 0.25f + random.nextFloat() * 0.55f;
        float z = 0.2f + random.nextFloat() * 0.6f;
        return new float[] { x, y, z };
    }

    /** Tamaño relativo del punto (0..1), determinista también — para que
     * haya "diferentes tamaños e intensidades" (pedido en el encargo) sin
     * variar frame a frame. */
    public static float sampleScale(BlockPos pos, int index) {
        Random random = new Random(pos.asLong() * 37L + index);
        return 0.55f + random.nextFloat() * 0.9f;
    }

    /** true si este punto concreto debe omitirse esta vez, según la
     * densidad de partículas/calidad configurada (0..1): sigue siendo
     * determinista por posición+índice, sólo cambia qué fracción de los
     * puntos ya elegidos se queda activa al bajar la calidad. */
    public static boolean isActiveAtDensity(BlockPos pos, int index, float density) {
        if (density >= 1.0f) return true;
        if (density <= 0.0f) return false;
        Random random = new Random(pos.asLong() * 53L + index + 17);
        return random.nextFloat() < density;
    }
}
