package pe.apuyawar.lighting.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.config.LightingConfig;

/**
 * APU YAWAR Lighting V4, fase 2: genera "chispas" y "estrellas"
 * celestiales (ver {@link CelestialSparkParticle}/
 * {@link CelestialStarParticle}) alrededor de los emisores de follaje
 * REALES que {@link EmitterTracker} ya tiene activos -- nunca alrededor
 * del jugador (sección 4.3 del encargo: "las partículas deben generarse
 * alrededor del árbol, no alrededor del jugador"). La "mota" original
 * (ver {@code MomentoCuyAbetoLeavesBlock.randomDisplayTick}) sigue
 * intacta y spawneando por su cuenta, por bloque -- esta clase sólo
 * añade los dos tipos nuevos.
 *
 * <p><b>Rendimiento (sección 4.8 del encargo)</b>: reutiliza
 * {@code EmitterTracker.active()}, que ya está acotado por distancia y
 * por chunk cargado -- no se añade ningún recorrido extra del mundo.
 * La probabilidad por emisor y tick es deliberadamente baja (no todos
 * los emisores activos generan una partícula en el mismo tick), así
 * que el costo escala con {@code maxluces}, no con el tamaño del
 * bosque completo.</p>
 */
final class CelestialParticleSpawner {
    /** Probabilidad, por emisor de follaje activo y por tick, de que
     * nazca una chispa/estrella con {@code particleDensityMultiplier
     * = 1.0} -- deliberadamente baja; se multiplica por la densidad
     * configurada, igual que la "mota" de siempre. */
    private static final float BASE_SPARK_CHANCE = 0.005f;

    private CelestialParticleSpawner() {}

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CelestialParticleSpawner::onTick);
    }

    private static void onTick(MinecraftClient client) {
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || config.particleDensityMultiplier <= 0f) return;
        ClientWorld world = client.world;
        if (world == null) return;

        float chance = BASE_SPARK_CHANCE * config.particleDensityMultiplier;
        if (chance <= 0f) return;

        for (EmitterTracker.ActiveEmitter emitter : EmitterTracker.active()) {
            if (!emitter.isFoliage()) continue;
            if (world.random.nextFloat() >= chance) continue;
            spawnOne(world, emitter.profile(), emitter.pos(), config);
        }
    }

    private static void spawnOne(ClientWorld world, EmitterProfile profile, net.minecraft.util.math.BlockPos pos,
                                   LightingConfig config) {
        // Desplazamiento aleatorio alrededor del bloque real -- algunas
        // terminan dentro del follaje, otras claramente fuera de él
        // (sección 4.3: "flotando ligeramente fuera de la copa"), pero
        // siempre centradas en una hoja real del árbol, nunca en el
        // jugador.
        double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 1.8;
        double y = pos.getY() + 0.3 + world.random.nextDouble() * 1.0;
        double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 1.8;

        boolean star = world.random.nextFloat() < Math.max(0f, Math.min(1f, config.celestialStarFraction));
        SimpleParticleType type = profile.warmPalette()
                ? (star ? CelestialParticleTypes.ESTRELLA_ROSA : CelestialParticleTypes.CHISPA_ROSA)
                : (star ? CelestialParticleTypes.ESTRELLA_TURQUESA : CelestialParticleTypes.CHISPA_TURQUESA);

        double vx = (world.random.nextDouble() - 0.5) * 0.004;
        double vy = world.random.nextDouble() * 0.004;
        double vz = (world.random.nextDouble() - 0.5) * 0.004;
        world.addParticle(type, x, y, z, vx, vy, vz);
    }
}
