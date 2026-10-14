package pe.apuyawar.momentocuy.dimension;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.apuyawar.momentocuy.block.MomentoCuyBlocks;
import pe.apuyawar.momentocuy.entity.MomentoCuyEntities;

/**
 * Zona segura de aparición: UNA plataforma pequeña (15x15), construida una
 * sola vez y nunca reconstruida — mismo patrón ya probado en el mod antiguo
 * ({@code NutriaLobby}: comprobar un bloque "marcador" antes de construir, y
 * no tocar nada si ya existe). A propósito mucho más pequeña que la plaza de
 * 53x53 del mod antiguo: esto es solo el punto de llegada seguro, no una
 * construcción definitiva — las islas flotantes reales las añade el
 * constructor del equipo por fuera de este código.
 *
 * <p>Coordenadas que usa este mod (documentadas también en
 * {@code 20_MOMENTO_CUY_DIMENSION.md} para el constructor): plataforma
 * circular de radio {@link #RADIUS} centrada en (0, {@link #Y}, 0). El
 * constructor puede construir con total libertad en cualquier otra
 * coordenada de la dimensión — este mod nunca vuelve a tocar esta zona una
 * vez construida, y nunca toca ninguna otra.
 */
public final class MomentoCuyLobby {
    private static final Logger LOGGER = LoggerFactory.getLogger("momento-cuy");
    public static final int Y = 100;
    public static final double ENTRY_X = 0.5, ENTRY_Y = 101.0, ENTRY_Z = 0.5;
    private static final int RADIUS = 7;
    private static final BlockPos MARKER = new BlockPos(0, Y - 2, 0);

    private MomentoCuyLobby() {}

    public static boolean exists(ServerWorld world) {
        return world.getBlockState(MARKER).isOf(MomentoCuyBlocks.BY_ID.get("cuarzo_cincelado_cuy"));
    }

    /** Construye la plataforma solo si no existe todavía; nunca sobrescribe
     * nada si el marcador ya está puesto. Devuelve si construyó algo nuevo. */
    public static boolean ensure(ServerWorld world) {
        if (exists(world)) return false;
        construct(world);
        return true;
    }

    private static void place(ServerWorld world, int x, int y, int z, net.minecraft.block.Block block) {
        world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 3);
    }

    private static void construct(ServerWorld world) {
        LOGGER.info("[MomentoCuy] Construyendo la plataforma segura de llegada (una sola vez, radio {})", RADIUS);
        var floor = MomentoCuyBlocks.BY_ID.get("baldosa_ornamental_lavanda");
        var edge = MomentoCuyBlocks.BY_ID.get("barandilla_piedra_ornamental");
        var accent = MomentoCuyBlocks.BY_ID.get("cristal_translucido_celestial");
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                int d2 = x * x + z * z;
                if (d2 <= RADIUS * RADIUS) {
                    place(world, x, Y, z, floor);
                    if (d2 > (RADIUS - 1) * (RADIUS - 1)) {
                        place(world, x, Y + 1, z, edge);
                    }
                }
            }
        }
        place(world, 3, Y + 1, 0, accent);
        place(world, -3, Y + 1, 0, accent);
        place(world, 0, Y + 1, 3, accent);
        place(world, 0, Y + 1, -3, accent);
        place(world, MARKER.getX(), MARKER.getY(), MARKER.getZ(), MomentoCuyBlocks.BY_ID.get("cuarzo_cincelado_cuy"));
        spawnCuy(world, 2.5, 2.5, "Cuy Jardinero");
        LOGGER.info("[MomentoCuy] Plataforma lista; su marcador queda guardado y no se reconstruira al entrar de nuevo");
    }

    private static void spawnCuy(ServerWorld world, double x, double z, String name) {
        var cuy = MomentoCuyEntities.CUY_JARDINERO.create(world);
        if (cuy != null) {
            cuy.refreshPositionAndAngles(x, Y + 1.0, z, 0.0F, 0.0F);
            cuy.setCustomName(Text.literal(name));
            cuy.setCustomNameVisible(true);
            cuy.setPersistent();
            world.spawnEntity(cuy);
        }
    }

    /** Verificación defensiva previa a un teletransporte: hay suelo sólido
     * justo debajo del punto de entrada — protección explícita contra caer al
     * vacío al aparecer, pedida en el encargo. Con la plataforma ya
     * construida por {@link #ensure} esto siempre debería cumplirse; si no se
     * cumpliera (ej. alguien rompió el suelo de la plataforma), abortar el
     * teletransporte es más seguro que mandar al jugador a caer. */
    public static boolean hasSafeGroundAtEntry(ServerWorld world) {
        BlockPos below = BlockPos.ofFloored(ENTRY_X, ENTRY_Y - 1.0, ENTRY_Z);
        return !world.getBlockState(below).isAir();
    }
}
