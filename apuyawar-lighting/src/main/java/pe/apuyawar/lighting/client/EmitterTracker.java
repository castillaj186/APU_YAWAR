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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mantiene, sin recorrer todo el mundo cada frame, la lista acotada de
 * posiciones de bloque cercanas a la cámara que son un emisor registrado,
 * y la lista separada de focos de suelo (ver {@link GroundSpot} y la
 * sección V2 de docs/23).
 *
 * <p>No es un sistema reactivo a nivel de bloque individual (no hay un
 * evento de "bloque colocado/roto" barato para esto en la Fabric API sin
 * añadir mixins propios al motor de chunks) — en su lugar, cada
 * {@link #SCAN_INTERVAL_TICKS} ticks (medio segundo) vuelve a recorrer un
 * volumen acotado alrededor del jugador, sólo dentro de secciones de
 * chunk ya cargadas.</p>
 *
 * <p><b>Rediseño V2 del foco de suelo (ver docs/23)</b>: la ronda anterior
 * ya hizo que la elegibilidad de una hoja para decal fuera determinista
 * por posición (no por cercanía al jugador), pero seguía siendo **un
 * decal por cada hoja candidata** — con el radio del decal derivado del
 * radio de luz de la hoja (6 bloques → ~3 de radio), muchas hojas bajo la
 * misma copa producían decals superpuestos que, con mezcla aditiva, se
 * fusionaban en una sola mancha grande, tan ancha como la proyección de
 * la copa. Ahora los focos de suelo son una {@link GroundSpot} **por
 * celda espacial** ({@link #GROUND_CELL_SIZE} bloques, una rejilla fija
 * en coordenadas del mundo, no centrada en el jugador): como mucho un
 * foco por celda ocupada, con radio propio
 * ({@code LightingConfig#groundSpotRadius}) completamente independiente
 * del radio de luz de las hojas. Un abeto típico cabe en 1-2 celdas, así
 * que produce 1-2 focos pequeños, no decenas superpuestos.</p>
 */
public final class EmitterTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    /** Tope duro de radio de escaneo horizontal, por rendimiento —
     * {@code config.maxEffectDistance} puede pedir más, pero nunca se
     * escanea más allá de esto (sección 3 del encargo: separar radio de
     * influencia y distancia de visibilidad, sin disparar el costo). */
    private static final int MAX_SCAN_RADIUS = 48;
    /** El escaneo vertical NO crece con el radio horizontal (antes sí,
     * haciendo el costo O(radio³)) — los árboles no son tan altos, así
     * que un rango vertical fijo y más pequeño basta y es mucho más
     * barato para radios de escaneo grandes. */
    private static final int VERTICAL_SCAN_HALF_HEIGHT = 20;
    /** Lado (en bloques) de cada celda de la rejilla de focos de suelo —
     * fija en coordenadas del mundo, nunca centrada en el jugador. */
    private static final int GROUND_CELL_SIZE = 8;
    private static final int GROUND_SCAN_DEPTH = 24;

    private static int tickCounter = 0;
    private static List<ActiveEmitter> active = List.of();
    private static List<GroundSpot> groundSpots = List.of();
    private static ActiveEmitter testEmitter = null;
    private static final Map<Long, Float> fade = new HashMap<>();
    private static final Map<Long, Float> groundFade = new HashMap<>();

    private EmitterTracker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EmitterTracker::onTick);
    }

    /** Lista activa del último rescaneo, con su intensidad de
     * entrada/salida ya aplicada, más el emisor de prueba de la Fase A si
     * hay uno colocado. */
    public static List<ActiveEmitter> active() {
        if (!ApuYawarLightingClient.config().rgbEnabled) return List.of();
        List<ActiveEmitter> base = active;
        List<ActiveEmitter> result = new ArrayList<>(base.size() + 1);
        if (testEmitter != null) result.add(testEmitter);
        for (ActiveEmitter e : base) {
            float f = fade.getOrDefault(e.pos().asLong(), 0f);
            if (f > 0.001f) result.add(e.withFade(f));
        }
        return result;
    }

    /** Focos de suelo activos, uno como máximo por celda ocupada de la
     * rejilla (ver documentación de clase), con su fundido de
     * entrada/salida ya aplicado. */
    public static List<GroundSpot> groundSpots() {
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || !config.groundDecalsEnabled) return List.of();
        List<GroundSpot> result = new ArrayList<>(groundSpots.size());
        for (GroundSpot s : groundSpots) {
            float f = groundFade.getOrDefault(s.cellKey(), 0f);
            if (f > 0.001f) result.add(s.withFade(f));
        }
        return result;
    }

    public static void setTestEmitter(BlockPos pos, EmitterProfile profile) {
        testEmitter = new ActiveEmitter(pos, profile, 0.0, 1.0f);
    }

    public static void clearTestEmitter() {
        testEmitter = null;
    }

    public static boolean hasTestEmitter() {
        return testEmitter != null;
    }

    private static long cellKey(BlockPos pos) {
        long cx = Math.floorDiv(pos.getX(), GROUND_CELL_SIZE);
        long cz = Math.floorDiv(pos.getZ(), GROUND_CELL_SIZE);
        return (cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    private static void onTick(MinecraftClient client) {
        updateFadeMap(fade, activePositionKeys(), ApuYawarLightingClient.config().fadeTransitionTicks);
        updateFadeMap(groundFade, groundSpotKeys(), ApuYawarLightingClient.config().fadeTransitionTicks);
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        rescan(client);
    }

    private static Set<Long> activePositionKeys() {
        Set<Long> keys = new HashSet<>();
        for (ActiveEmitter e : active) keys.add(e.pos().asLong());
        return keys;
    }

    private static Set<Long> groundSpotKeys() {
        Set<Long> keys = new HashSet<>();
        for (GroundSpot s : groundSpots) keys.add(s.cellKey());
        return keys;
    }

    /** Sube/baja cada valor del mapa hacia 1 (si su clave está en
     * {@code targets}) o hacia 0 (si no) — se llama todos los ticks, no
     * sólo en el rescaneo, para que la transición sea fluida. Compartida
     * entre el fundido de puntos y el de focos de suelo: misma mecánica,
     * mapas y claves distintos. */
    private static void updateFadeMap(Map<Long, Float> map, Set<Long> targets, int transitionTicks) {
        float step = 1.0f / Math.max(1, transitionTicks);
        for (Long key : targets) {
            map.merge(key, step, (old, inc) -> Math.min(1.0f, old + inc));
        }
        if (map.isEmpty()) return;
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (targets.contains(entry.getKey())) continue;
            float next = entry.getValue() - step;
            if (next <= 0f) {
                it.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    private static void rescan(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            active = List.of();
            groundSpots = List.of();
            // El emisor de prueba no se limpiaba al abandonar el mundo
            // (desconectar o volver al menú) -- quedaba apuntando a un
            // BlockPos de un mundo que ya no existe.
            testEmitter = null;
            fade.clear();
            groundFade.clear();
            return;
        }
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || !ApuYawarLighting.hasAnyEmitters()) {
            active = List.of();
            groundSpots = List.of();
            return;
        }

        Vec3d cameraPos = client.player.getPos();
        BlockPos center = client.player.getBlockPos();
        Map<net.minecraft.util.Identifier, EmitterProfile> registered = ApuYawarLighting.registeredEmitters();
        List<ActiveEmitter> found = new ArrayList<>();
        // Corrección de arquitectura (sección 6 del encargo): la distancia
        // de detección ya NO depende de profile.radius() (eso mezclaba
        // "qué tan lejos se ve" con "qué tan grande es el halo") -- sólo
        // de maxEffectDistance, el control real de visibilidad.
        float maxDist = Math.max(1f, config.maxEffectDistance);
        int scanRadius = (int) Math.min(MAX_SCAN_RADIUS, Math.ceil(maxDist));

        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;
                if (!world.isChunkLoaded(worldX >> 4, worldZ >> 4)) continue;
                for (int dy = -VERTICAL_SCAN_HALF_HEIGHT; dy <= VERTICAL_SCAN_HALF_HEIGHT; dy++) {
                    cursor.set(worldX, center.getY() + dy, worldZ);
                    if (!world.isInBuildLimit(cursor)) continue;
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;
                    net.minecraft.util.Identifier id = Registries.BLOCK.getId(state.getBlock());
                    EmitterProfile profile = registered.get(id);
                    if (profile == null) continue;
                    double distSq = cursor.toCenterPos().squaredDistanceTo(cameraPos);
                    if (distSq > (double) maxDist * maxDist) continue;
                    found.add(new ActiveEmitter(cursor.toImmutable(), profile, distSq));
                }
            }
        }

        found.sort(Comparator.<ActiveEmitter>comparingInt(e -> -e.profile.priority())
                .thenComparingDouble(e -> e.distanceSq));
        int cap = Math.max(1, config.maxActiveLights);
        List<ActiveEmitter> capped = found.size() > cap ? new ArrayList<>(found.subList(0, cap)) : new ArrayList<>(found);
        active = capped;

        groundSpots = config.groundDecalsEnabled ? buildGroundSpots(world, capped) : List.of();
    }

    /** Un foco de suelo como máximo por celda ocupada de la rejilla fija
     * (ver documentación de clase) -- nunca uno por hoja. El
     * representante de cada celda se elige de forma determinista (la hoja
     * más alta; empate por {@code BlockPos#asLong()}), no por el primero
     * que aparezca al iterar, para que el resultado no dependa del orden
     * de escaneo. */
    private static List<GroundSpot> buildGroundSpots(ClientWorld world, List<ActiveEmitter> emitters) {
        Map<Long, ActiveEmitter> representativeByCell = new HashMap<>();
        for (ActiveEmitter e : emitters) {
            if (!e.isFoliage()) continue;
            long key = cellKey(e.pos());
            ActiveEmitter current = representativeByCell.get(key);
            if (current == null || isBetterRepresentative(e, current)) {
                representativeByCell.put(key, e);
            }
        }
        List<GroundSpot> spots = new ArrayList<>(representativeByCell.size());
        for (var entry : representativeByCell.entrySet()) {
            ActiveEmitter rep = entry.getValue();
            Integer groundY = findGroundY(world, rep.pos());
            if (groundY == null) continue;
            spots.add(new GroundSpot(entry.getKey(), rep.pos(), groundY, rep.profile()));
        }
        return spots;
    }

    private static boolean isBetterRepresentative(ActiveEmitter candidate, ActiveEmitter current) {
        if (candidate.pos().getY() != current.pos().getY()) return candidate.pos().getY() > current.pos().getY();
        return candidate.pos().asLong() < current.pos().asLong();
    }

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

    public record ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq, float fade) {
        public ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq) {
            this(pos, profile, distanceSq, 1.0f);
        }

        public ActiveEmitter withFade(float f) {
            return new ActiveEmitter(pos, profile, distanceSq, f);
        }

        public boolean isFoliage() {
            return profile.type() == EmitterType.FOLIAGE;
        }
    }

    /** Foco de suelo: uno como máximo por celda de la rejilla
     * ({@link #GROUND_CELL_SIZE}), con radio/intensidad propios
     * ({@code LightingConfig#groundSpotRadius}/{@code groundSpotIntensity}),
     * independientes del radio de luz de la hoja que lo originó. */
    public record GroundSpot(long cellKey, BlockPos anchorPos, int groundY, EmitterProfile profile, float fade) {
        public GroundSpot(long cellKey, BlockPos anchorPos, int groundY, EmitterProfile profile) {
            this(cellKey, anchorPos, groundY, profile, 1.0f);
        }

        public GroundSpot withFade(float f) {
            return new GroundSpot(cellKey, anchorPos, groundY, profile, f);
        }
    }
}
