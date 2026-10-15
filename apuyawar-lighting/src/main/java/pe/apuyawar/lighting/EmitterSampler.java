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
     * {@code index} (0-based, {@code index < profile.pointsPerBlock()}).
     *
     * <p>Corrección visual (ver docs/23): la versión anterior centraba los
     * tres ejes de forma independiente (0.2-0.8 en cada uno), lo que deja
     * la mayoría de los puntos hacia el centro geométrico del bloque —
     * "profundamente dentro de bloques opacos, donde nunca se verán"
     * (sección 9 del encargo). Ahora se elige primero, de forma
     * determinista, una cara preferente (una de las 6) y el punto se
     * sesga hacia esa cara en su eje principal (0.1-0.3 o 0.7-0.9), quedando
     * más centrado en los otros dos — "cerca de superficies visibles...
     * no todos en la misma cara" sin necesitar consultar qué vecino es
     * opaco (eso exigiría acceso al mundo dentro de una función que debe
     * seguir siendo pura/barata).</p> */
    public static float[] samplePoint(BlockPos pos, EmitterProfile profile, int index) {
        Random random = new Random(pos.asLong() * 31L + index);
        int face = random.nextInt(6); // 0=-X 1=+X 2=-Y 3=+Y 4=-Z 5=+Z
        float x = biasedAxis(random, face, 0);
        float y = biasedAxis(random, face, 1);
        float z = biasedAxis(random, face, 2);
        return new float[] { x, y, z };
    }

    private static float biasedAxis(Random random, int face, int axis) {
        boolean isMainAxis = face / 2 == axis;
        if (!isMainAxis) {
            // Eje secundario: disperso pero sin tocar el borde exacto,
            // para variar entre puntos de una misma cara.
            return 0.15f + random.nextFloat() * 0.7f;
        }
        boolean towardsHigh = (face % 2) == 1;
        float near = 0.08f + random.nextFloat() * 0.18f; // 0.08-0.26 del borde
        return towardsHigh ? (1.0f - near) : near;
    }

    /** Tamaño relativo del punto (0..1 normalizado dentro de cada
     * categoría — ver {@link #sampleCategory}), determinista también —
     * para que haya "diferentes tamaños e intensidades" sin variar frame a
     * frame. */
    public static float sampleScale(BlockPos pos, int index) {
        Random random = new Random(pos.asLong() * 37L + index);
        return 0.55f + random.nextFloat() * 0.9f;
    }

    /** Categoría visual del punto (sección 9 del encargo: "puntos
     * pequeños: numerosos... medianos: menos frecuentes... principales:
     * muy pocos"). Determinista, con una distribución sesgada (no
     * uniforme) para que los puntos "principales" sean realmente raros:
     * ~70% pequeños, ~25% medianos, ~5% principales. */
    public static PointCategory sampleCategory(BlockPos pos, int index) {
        Random random = new Random(pos.asLong() * 41L + index + 7);
        float roll = random.nextFloat();
        if (roll < 0.70f) return PointCategory.SMALL;
        if (roll < 0.95f) return PointCategory.MEDIUM;
        return PointCategory.MAIN;
    }

    /** true si este punto concreto debe omitirse esta vez, según la
     * densidad de puntos fijos configurada (0..1): sigue siendo
     * determinista por posición+índice, sólo cambia qué fracción de los
     * puntos ya elegidos se queda activa al bajar la calidad. */
    public static boolean isActiveAtDensity(BlockPos pos, int index, float density) {
        if (density >= 1.0f) return true;
        if (density <= 0.0f) return false;
        Random random = new Random(pos.asLong() * 53L + index + 17);
        return random.nextFloat() < density;
    }

    public enum PointCategory {
        SMALL(0.55f, 0.75f, 1.0f),
        MEDIUM(0.85f, 1.0f, 1.25f),
        MAIN(1.2f, 1.4f, 1.9f);

        /** Multiplicador de radio del núcleo. */
        public final float coreRadiusMultiplier;
        /** Multiplicador de intensidad/alfa del núcleo. */
        public final float intensityMultiplier;
        /** Multiplicador adicional sólo para el halo (los puntos
         * "principales" tienen un halo notablemente mayor, pedido en la
         * sección 9/10 del encargo). */
        public final float haloRadiusMultiplier;

        PointCategory(float coreRadiusMultiplier, float intensityMultiplier, float haloRadiusMultiplier) {
            this.coreRadiusMultiplier = coreRadiusMultiplier;
            this.intensityMultiplier = intensityMultiplier;
            this.haloRadiusMultiplier = haloRadiusMultiplier;
        }
    }
}
