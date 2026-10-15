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
 *
 * <p><b>Corrección de anclaje espacial (ver docs/23)</b>: la versión
 * anterior elegía qué emisores recibían decal de suelo ordenando TODOS los
 * candidatos por prioridad y luego por distancia a la cámara, y tomando
 * siempre "los primeros 8" — como ese orden cambia cada vez que el
 * jugador se mueve (una hoja que antes era la 9ª más cercana puede pasar a
 * ser la 7ª), el conjunto de hojas con decal cambiaba con cada rescaneo,
 * dando la sensación de que la luz "seguía" al jugador. Ahora la
 * elegibilidad para decal de suelo es una propiedad fija de cada posición
 * de bloque ({@link #isGroundDecalCandidate}, determinista por
 * {@code BlockPos#asLong()}), no de su posición relativa al jugador — qué
 * hojas pueden tener decal no cambia nunca por caminar, sólo cambia si
 * esa hoja sigue dentro del radio de escaneo o no.</p>
 */
public final class EmitterTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int SCAN_RADIUS = 24;
    /** 1 de cada N bloques emisores de hoja es "candidato" a decal de
     * suelo — fijo por posición, nunca por cercanía al jugador (ver
     * {@link #isGroundDecalCandidate}). */
    private static final int GROUND_DECAL_EVERY = 5;
    /** Tope de seguridad sólo para costo (rayos hacia abajo), no para qué
     * posiciones son candidatas -- con muchos árboles a la vez, limita
     * cuántos candidatos ya elegidos de forma fija se recalculan este
     * rescaneo, por cercanía (empate de candidatos reales, no churn de
     * "cuál es top-8 ahora"). */
    private static final int GROUND_DECAL_COMPUTE_LIMIT = 24;
    private static final int GROUND_SCAN_DEPTH = 24;

    private static int tickCounter = 0;
    private static List<ActiveEmitter> active = List.of();
    private static ActiveEmitter testEmitter = null;
    private static final Map<Long, Float> fade = new HashMap<>();

    private EmitterTracker() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(EmitterTracker::onTick);
    }

    /** Lista activa del último rescaneo, con su intensidad de
     * entrada/salida (sección 7 del encargo) ya aplicada, más el emisor de
     * prueba de la Fase A si hay uno colocado (ver
     * {@code LightingCommands#test}) — el de prueba se incluye siempre,
     * con fundido completo (aparece al instante, es una herramienta de
     * diagnóstico manual, no un emisor real del mundo).
     *
     * <p>Corrección (ronda anterior): antes el emisor de prueba se añadía
     * incondicionalmente, incluso con {@code rgbEnabled=false}.</p>
     *
     * <p>Corrección (esta ronda): se calcula aquí, en cada llamada (una
     * por frame), el fundido de cada emisor a partir del mapa persistente
     * {@link #fade} — así el renderizador no necesita saber nada de
     * fundidos, sólo multiplicar {@code ActiveEmitter#fade()} en su
     * cálculo de alfa.</p> */
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

    public static void setTestEmitter(BlockPos pos, EmitterProfile profile) {
        testEmitter = new ActiveEmitter(pos, profile, 0.0, null, 1.0f);
    }

    public static void clearTestEmitter() {
        testEmitter = null;
    }

    public static boolean hasTestEmitter() {
        return testEmitter != null;
    }

    /** true si esta posición de bloque es, de forma fija (nunca depende de
     * dónde esté el jugador), una candidata a decal de suelo. */
    private static boolean isGroundDecalCandidate(BlockPos pos) {
        return Math.floorMod(pos.asLong(), GROUND_DECAL_EVERY) == 0;
    }

    private static void onTick(MinecraftClient client) {
        updateFade();
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        rescan(client);
    }

    /** Sube/baja la intensidad de cada posición hacia 1 (si sigue en
     * {@link #active}) o hacia 0 (si ya no) — se llama todos los ticks,
     * no sólo en el rescaneo, para que la transición sea fluida y no un
     * escalón cada 10 ticks. */
    private static void updateFade() {
        LightingConfig config = ApuYawarLightingClient.config();
        float step = 1.0f / Math.max(1, config.fadeTransitionTicks);

        Set<Long> targets = new HashSet<>();
        for (ActiveEmitter e : active) targets.add(e.pos().asLong());

        for (Long key : targets) {
            fade.merge(key, step, (old, inc) -> Math.min(1.0f, old + inc));
        }
        if (!fade.isEmpty()) {
            var it = fade.entrySet().iterator();
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
    }

    private static void rescan(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            active = List.of();
            // Corrección: el emisor de prueba no se limpiaba al abandonar el
            // mundo (desconectar o volver al menú principal) -- quedaba
            // apuntando a un BlockPos de un mundo que ya no existe.
            testEmitter = null;
            fade.clear();
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
        float maxDist = Math.max(1f, config.maxEffectDistance);

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
                    double cutoff = Math.min(profile.radius() * 2.0, maxDist);
                    if (distSq > cutoff * cutoff) continue;
                    found.add(new ActiveEmitter(cursor.toImmutable(), profile, distSq));
                }
            }
        }

        found.sort(Comparator.<ActiveEmitter>comparingInt(e -> -e.profile.priority())
                .thenComparingDouble(e -> e.distanceSq));
        int cap = Math.max(1, config.maxActiveLights);
        List<ActiveEmitter> capped = found.size() > cap ? new ArrayList<>(found.subList(0, cap)) : new ArrayList<>(found);

        if (config.groundDecalsEnabled) {
            // Candidatos a decal: posiciones fijas (ver isGroundDecalCandidate),
            // nunca "los N más cercanos" -- eso es justo lo que causaba el
            // salto de ubicación al caminar. Entre los candidatos reales,
            // si hay más de los que conviene calcular este rescaneo, se
            // ordenan por cercanía sólo para decidir CUÁLES YA CALCULADOS
            // priorizar, no para decidir CUÁLES SON candidatos.
            List<Integer> candidateIdx = new ArrayList<>();
            for (int i = 0; i < capped.size(); i++) {
                ActiveEmitter e = capped.get(i);
                if (e.isFoliage() && isGroundDecalCandidate(e.pos())) candidateIdx.add(i);
            }
            candidateIdx.sort(Comparator.comparingDouble(i -> capped.get(i).distanceSq()));
            int limit = Math.min(candidateIdx.size(), GROUND_DECAL_COMPUTE_LIMIT);
            for (int k = 0; k < limit; k++) {
                int i = candidateIdx.get(k);
                capped.set(i, capped.get(i).withGroundY(findGroundY(world, capped.get(i).pos())));
            }
        }

        active = capped;
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

    public record ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq, Integer groundY, float fade) {
        public ActiveEmitter(BlockPos pos, EmitterProfile profile, double distanceSq) {
            this(pos, profile, distanceSq, null, 1.0f);
        }

        public ActiveEmitter withGroundY(Integer groundY) {
            return new ActiveEmitter(pos, profile, distanceSq, groundY, fade);
        }

        public ActiveEmitter withFade(float f) {
            return new ActiveEmitter(pos, profile, distanceSq, groundY, f);
        }

        public boolean isFoliage() {
            return profile.type() == EmitterType.FOLIAGE;
        }
    }
}
