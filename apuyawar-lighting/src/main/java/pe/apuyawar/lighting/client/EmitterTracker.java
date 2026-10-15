package pe.apuyawar.lighting.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pe.apuyawar.lighting.ApuYawarLighting;
import pe.apuyawar.lighting.EmitterProfile;
import pe.apuyawar.lighting.EmitterType;
import pe.apuyawar.lighting.config.LightingConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mantiene, sin recorrer todo el mundo cada frame, la lista acotada de
 * posiciones de bloque cercanas a la cámara que son un emisor registrado.
 *
 * <p>No es un sistema reactivo a nivel de bloque individual (no hay un
 * evento de "bloque colocado/roto" barato para esto en la Fabric API sin
 * añadir mixins propios al motor de chunks) — en su lugar, cada
 * {@link #SCAN_INTERVAL_TICKS} ticks (medio segundo) vuelve a recorrer un
 * cubo acotado alrededor del jugador, sólo dentro de secciones de chunk ya
 * cargadas. Esto cumple "actualización al colocar o eliminar bloques" con
 * una latencia real de hasta medio segundo — se documenta así en vez de
 * afirmar una reacción instantánea que esta implementación no tiene.</p>
 */
public final class EmitterTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int SCAN_RADIUS = 24;

    private static int tickCounter = 0;
    private static List<ActiveEmitter> active = List.of();
    private static ActiveEmitter testEmitter = null;

    private EmitterTracker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EmitterTracker::onTick);
    }

    /** Lista activa del último rescaneo, más el emisor de prueba de la Fase
     * A si hay uno colocado (ver {@code LightingCommands#test}) — el de
     * prueba se incluye siempre, sin esperar al próximo rescaneo. */
    public static List<ActiveEmitter> active() {
        if (testEmitter == null) return active;
        List<ActiveEmitter> withTest = new ArrayList<>(active.size() + 1);
        withTest.add(testEmitter);
        withTest.addAll(active);
        return withTest;
    }

    public static void setTestEmitter(BlockPos pos, EmitterProfile profile) {
        testEmitter = new ActiveEmitter(pos, profile, 0.0, null);
    }

    public static void clearTestEmitter() {
        testEmitter = null;
    }

    public static boolean hasTestEmitter() {
        return testEmitter != null;
    }

    private static void onTick(MinecraftClient client) {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        rescan(client);
    }

    private static void rescan(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            active = List.of();
            return;
        }
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || !ApuYawarLighting.hasAnyEmitters()) {
            active = List.of();
            return;
        }

        Vec3d cameraPos = client.player.getPos();
        BlockPos center = client.player.getBlockPos();
        Map<net.minecraft.util.Identifier, EmitterProfile> registered = ApuYawarLighting.registeredEmitters();
        List<ActiveEmitter> found = new ArrayList<>();

        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;
                if (!world.isChunkLoaded(worldX >> 4, worldZ >> 4)) continue;
                for (int dy = -SCAN_RADIUS; dy <= SCAN_RADIUS; dy++) {
                    cursor.set(worldX, center.getY() + dy, worldZ);
                    if (!world.isInBuildLimit(cursor)) continue;
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;
                    net.minecraft.util.Identifier id = Registries.BLOCK.getId(state.getBlock());
                    EmitterProfile profile = registered.get(id);
                    if (profile == null) continue;
                    double distSq = cursor.toCenterPos().squaredDistanceTo(cameraPos);
                    if (distSq > (double) profile.radius() * profile.radius() * 4.0) continue;
                    found.add(new ActiveEmitter(cursor.toImmutable(), profile, distSq));
                }
            }
        }

        found.sort(Comparator.<ActiveEmitter>comparingInt(e -> -e.profile.priority())
                .thenComparingDouble(e -> e.distanceSq));
        int cap = Math.max(1, config.maxActiveLights);
        List<ActiveEmitter> capped = found.size() > cap ? new ArrayList<>(found.subList(0, cap)) : new ArrayList<>(found);

        // Decal de suelo (sección 9): sólo para los más cercanos/prioritarios,
        // para acotar el costo de los rayos hacia abajo -- no uno por cada
        // emisor activo.
        for (int i = 0; i < capped.size() && i < GROUND_DECAL_LIMIT; i++) {
            capped.set(i, capped.get(i).withGroundY(findGroundY(world, capped.get(i).pos())));
        }

        active = capped;
    }

    private static final int GROUND_DECAL_LIMIT = 8;
    private static final int GROUND_SCAN_DEPTH = 24;

    /** Primer bloque sólido por debajo de {@code from}, o {@code null} si no
     * hay ninguno dentro de {@link #GROUND_SCAN_DEPTH} bloques (evita rayos
     * sin fin hacia el vacío de las islas flotantes). */
    private static Integer findGroundY(ClientWorld world, BlockPos from) {
        BlockPos.Mutable cursor = from.mutableCopy();
        for (int i = 0; i < GROUND_SCAN_DEPTH; i++) {
            cursor.setY(cursor.getY() - 1);
            if (!world.isInBuildLimit(cursor)) return null;
            BlockState state = world.getBlockState(cursor);
            if (!state.getCollisionShape(world, cursor).isEmpty() || !state.getFluidState().isEmpty()) {
                return cursor.getY();
            }
        }
        return null;
    }

    public record ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq, Integer groundY) {
        public ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq) {
            this(pos, profile, distanceSq, null);
        }

        public ActiveEmitter withGroundY(Integer groundY) {
            return new ActiveEmitter(pos, profile, distanceSq, groundY);
        }

        public boolean isFoliage() {
            return profile.type() == EmitterType.FOLIAGE;
        }
    }
}
