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
 * y la lista separada de focos de suelo (ver {@link GroundSpot}).
 *
 * <p><b>Corrección V3 (ver docs/23, "corrección de errores confirmados en
 * video")</b>: tres cambios reales a partir de la revisión de código del
 * equipo, no sólo ajustes de números:</p>
 * <ol>
 * <li><b>Escaneo por caché de chunk</b> en vez de un cubo recalculado
 *     desde cero cada rescaneo — el costo de escanear hasta
 *     {@code maxEffectDistance} ya no crece sin control (sección "Error
 *     1").</li>
 * <li><b>Representante de foco de suelo "pegajoso"</b>
 *     ({@link #stickyGroundRepresentative}): una vez elegido, una celda
 *     sigue usando la MISMA hoja mientras siga siendo candidata, en vez
 *     de recalcular "la mejor" en cada rescaneo — antes, aunque la celda
 *     fuera la misma, la hoja representante (y por tanto {@code anchorPos})
 *     podía cambiar sin que el árbol cambiara (sección "Error 4").</li>
 * <li><b>{@code findGroundY} ya no puede confundir otra hoja de la misma
 *     copa con el suelo real</b> — las hojas tienen colisión completa, así
 *     que un rayo hacia abajo desde una hoja alta encontraba casi siempre
 *     OTRA hoja más abajo del mismo árbol antes de llegar al suelo de
 *     verdad, dando una distancia vertical casi nula y por tanto un foco
 *     casi siempre al máximo de intensidad (sección "Error 5").</li>
 * </ol>
 */
public final class EmitterTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    /** Tope duro de radio de escaneo horizontal, por rendimiento —
     * {@code config.maxEffectDistance} puede pedir más, pero nunca se
     * escanea más allá de esto. Subido de 48 a 80 ahora que el escaneo es
     * por chunk cacheado, no por cubo recalculado cada vez. */
    private static final int MAX_SCAN_RADIUS = 80;
    /** Ventana vertical (bloques arriba/abajo de la Y del jugador EN EL
     * MOMENTO en que un chunk se cachea por primera vez) que se indexa al
     * construir la caché de ese chunk. Limitación documentada: si el
     * jugador cachea un chunk estando, por ejemplo, bajo tierra, y luego
     * sube mucho más alto, un árbol de ese mismo chunk podría quedar fuera
     * de la ventana ya cacheada hasta que la caché se reconstruya (ver
     * {@link #CHUNK_CACHE_TTL_RESCANS}). */
    private static final int CHUNK_CACHE_Y_WINDOW = 48;
    /** Cuántos chunks SIN caché (o con caché caducada) se indexan como
     * máximo en un mismo rescaneo — reparte el costo de "entrar a una
     * zona nueva" en varios rescaneos (unos segundos) en vez de un único
     * golpe grande, tal como pidió el encargo ("evita un escaneo cúbico
     * enorme en cada actualización"). */
    private static final int MAX_NEW_CHUNK_SCANS_PER_RESCAN = 6;
    /** Cada cuántos rescaneos se considera "vieja" la caché de un chunk y
     * se vuelve a indexar desde cero (para notar árboles nuevos/crecidos
     * en un chunk ya cacheado) — 200 rescaneos × 0.5 s ≈ 100 s. */
    private static final int CHUNK_CACHE_TTL_RESCANS = 200;
    /** Lado (en bloques) de cada celda de la rejilla de focos de suelo —
     * fija en coordenadas del mundo, nunca centrada en el jugador. */
    private static final int GROUND_CELL_SIZE = 8;
    private static final int GROUND_SCAN_DEPTH = 32;

    private static int tickCounter = 0;
    private static long rescanCounter = 0;
    private static List<ActiveEmitter> active = List.of();
    private static List<GroundSpot> groundSpots = List.of();
    private static ActiveEmitter testEmitter = null;
    private static final Map<Long, Float> fade = new HashMap<>();
    private static final Map<Long, Float> groundFade = new HashMap<>();
    private static final Map<Long, ChunkCache> chunkCache = new HashMap<>();
    /** Representante "pegajoso" de cada celda de foco de suelo -- ver
     * documentación de clase, punto 2. */
    private static final Map<Long, BlockPos> stickyGroundRepresentative = new HashMap<>();

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

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
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
     * sólo en el rescaneo, para que la transición sea fluida. */
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
        rescanCounter++;
        ClientWorld world = client.world;
        if (world == null || client.player == null) {
            active = List.of();
            groundSpots = List.of();
            testEmitter = null;
            fade.clear();
            groundFade.clear();
            chunkCache.clear();
            stickyGroundRepresentative.clear();
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
        float maxDist = Math.max(1f, config.maxEffectDistance);
        int scanRadius = (int) Math.min(MAX_SCAN_RADIUS, Math.ceil(maxDist));

        List<ActiveEmitter> found = scanViaChunkCache(world, center, cameraPos, registered, scanRadius, maxDist);

        found.sort(Comparator.<ActiveEmitter>comparingInt(e -> -e.profile.priority())
                .thenComparingDouble(e -> e.distanceSq));
        int cap = Math.max(1, config.maxActiveLights);
        List<ActiveEmitter> capped = found.size() > cap ? selectStratifiedByHeight(found, cap) : new ArrayList<>(found);
        active = capped;

        groundSpots = config.groundDecalsEnabled ? buildGroundSpots(world, registered, capped) : List.of();
    }

    /** Recorre los chunks dentro de {@code scanRadius}, usando (y
     * completando, con presupuesto limitado) la caché por chunk en vez de
     * volver a mirar cada bloque crudo cada vez — ver documentación de
     * clase, punto 1. */
    private static List<ActiveEmitter> scanViaChunkCache(ClientWorld world, BlockPos center, Vec3d cameraPos,
                                                           Map<net.minecraft.util.Identifier, EmitterProfile> registered,
                                                           int scanRadius, float maxDist) {
        List<ActiveEmitter> found = new ArrayList<>();
        int minChunkX = (center.getX() - scanRadius) >> 4;
        int maxChunkX = (center.getX() + scanRadius) >> 4;
        int minChunkZ = (center.getZ() - scanRadius) >> 4;
        int maxChunkZ = (center.getZ() + scanRadius) >> 4;
        double maxDistSq = (double) maxDist * maxDist;
        int newScans = 0;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                long key = chunkKey(cx, cz);
                ChunkCache cache = chunkCache.get(key);
                boolean stale = cache != null && (rescanCounter - cache.builtAtRescan) > CHUNK_CACHE_TTL_RESCANS;
                if ((cache == null || stale) && newScans < MAX_NEW_CHUNK_SCANS_PER_RESCAN) {
                    cache = scanChunkForEmitters(world, cx, cz, center.getY(), registered);
                    chunkCache.put(key, cache);
                    newScans++;
                }
                if (cache == null) continue; // sin presupuesto este rescaneo, se cachea en uno futuro
                for (BlockPos pos : cache.positions) {
                    // El bloque pudo cambiar desde que se cacheó (decaimiento,
                    // rotura, crecimiento) -- se revalida contra el mundo real
                    // en cada rescaneo, que es barato (una consulta por
                    // posición ya cacheada, no un escaneo de todo el chunk).
                    BlockState state = world.getBlockState(pos);
                    EmitterProfile profile = registered.get(Registries.BLOCK.getId(state.getBlock()));
                    if (profile == null) continue;
                    double distSq = pos.toCenterPos().squaredDistanceTo(cameraPos);
                    if (distSq > maxDistSq) continue;
                    found.add(new ActiveEmitter(pos, profile, distSq));
                }
            }
        }
        return found;
    }

    /** Corrección "Error 3" del encargo de corrección V3: cuando los
     * emisores reales detectados superan {@code maxluces}, el recorte
     * anterior era puramente "los más cercanos a la cámara" -- en un árbol
     * visto desde el lado o desde abajo, eso deja sistemáticamente fuera
     * la punta (casi siempre la parte más lejana en línea recta desde el
     * jugador, nunca la más cercana). Ahora el cupo se reparte entre tres
     * bandas de ALTURA REAL DEL PROPIO ÁRBOL detectado (tercio inferior,
     * medio y superior de los bloques encontrados, no de la pantalla ni
     * de la posición del jugador), cada banda ya ordenada por
     * prioridad+distancia (orden heredado de {@code found}, que el
     * llamador ya ordenó así) -- así la punta siempre recibe su parte
     * proporcional del cupo en vez de competir en una sola lista global
     * por cercanía. */
    private static List<ActiveEmitter> selectStratifiedByHeight(List<ActiveEmitter> found, int cap) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (ActiveEmitter e : found) {
            int y = e.pos().getY();
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        int span = maxY - minY;
        if (span <= 0) {
            return new ArrayList<>(found.subList(0, cap));
        }

        List<ActiveEmitter> lower = new ArrayList<>();
        List<ActiveEmitter> middle = new ArrayList<>();
        List<ActiveEmitter> upper = new ArrayList<>();
        for (ActiveEmitter e : found) {
            double frac = (e.pos().getY() - minY) / (double) span;
            if (frac < 1.0 / 3.0) lower.add(e);
            else if (frac < 2.0 / 3.0) middle.add(e);
            else upper.add(e);
        }

        int[] quota = distributeQuota(cap, lower.size(), middle.size(), upper.size());
        List<ActiveEmitter> result = new ArrayList<>(cap);
        result.addAll(lower.subList(0, Math.min(quota[0], lower.size())));
        result.addAll(middle.subList(0, Math.min(quota[1], middle.size())));
        result.addAll(upper.subList(0, Math.min(quota[2], upper.size())));
        return result;
    }

    /** Reparte {@code cap} en tres cupos (uno por banda de altura),
     * empezando por un tercio cada una y, si alguna banda tiene menos
     * candidatos de los que le tocan, cediendo lo que no puede usar a las
     * demás bandas que todavía tengan margen -- así un árbol con poca
     * copa en un tercio (p. ej. la punta, siempre más estrecha) no
     * desperdicia cupo: lo recibe la banda con más hojas reales. */
    private static int[] distributeQuota(int cap, int lowerCount, int middleCount, int upperCount) {
        int[] counts = {lowerCount, middleCount, upperCount};
        int[] quota = {cap / 3, cap / 3, cap - 2 * (cap / 3)};
        for (int pass = 0; pass < 3; pass++) {
            int leftover = 0;
            boolean[] saturated = new boolean[3];
            for (int i = 0; i < 3; i++) {
                if (quota[i] > counts[i]) {
                    leftover += quota[i] - counts[i];
                    quota[i] = counts[i];
                    saturated[i] = true;
                }
            }
            if (leftover == 0) break;
            List<Integer> open = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                if (!saturated[i] && quota[i] < counts[i]) open.add(i);
            }
            if (open.isEmpty()) break;
            int share = leftover / open.size();
            int rem = leftover % open.size();
            for (int idx : open) {
                int add = share + (rem-- > 0 ? 1 : 0);
                quota[idx] = Math.min(counts[idx], quota[idx] + add);
            }
        }
        return quota;
    }

    /** Indexa un chunk completo (16×16 columnas) una sola vez: todas las
     * posiciones que sean en ese momento un emisor registrado, dentro de
     * una ventana vertical fija alrededor de la Y del jugador en el
     * momento de cachear (ver {@link #CHUNK_CACHE_Y_WINDOW}). */
    private static ChunkCache scanChunkForEmitters(ClientWorld world, int chunkX, int chunkZ, int playerY,
                                                     Map<net.minecraft.util.Identifier, EmitterProfile> registered) {
        List<BlockPos> positions = new ArrayList<>();
        int minY = Math.max(world.getBottomY(), playerY - CHUNK_CACHE_Y_WINDOW);
        int maxY = Math.min(world.getTopY() - 1, playerY + CHUNK_CACHE_Y_WINDOW);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(baseX + x, y, baseZ + z);
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (registered.containsKey(Registries.BLOCK.getId(state.getBlock()))) {
                        positions.add(cursor.toImmutable());
                    }
                }
            }
        }
        return new ChunkCache(positions, rescanCounter);
    }

    /** Un foco de suelo como máximo por celda ocupada de la rejilla fija
     * (ver documentación de clase) -- nunca uno por hoja, y el
     * representante de cada celda es "pegajoso": una vez elegido, sigue
     * siendo el mismo mientras continúe siendo candidato en esa celda, en
     * vez de recalcularse "el mejor" en cada rescaneo (eso es lo que
     * hacía que {@code anchorPos} pudiera moverse sin que el árbol
     * cambiara realmente -- ver documentación de clase, punto 2). */
    private static List<GroundSpot> buildGroundSpots(ClientWorld world,
                                                       Map<net.minecraft.util.Identifier, EmitterProfile> registered,
                                                       List<ActiveEmitter> emitters) {
        Map<Long, List<ActiveEmitter>> byCell = new HashMap<>();
        for (ActiveEmitter e : emitters) {
            if (!e.isFoliage()) continue;
            byCell.computeIfAbsent(cellKey(e.pos()), k -> new ArrayList<>()).add(e);
        }

        List<GroundSpot> spots = new ArrayList<>(byCell.size());
        for (var entry : byCell.entrySet()) {
            long key = entry.getKey();
            List<ActiveEmitter> candidates = entry.getValue();
            BlockPos sticky = stickyGroundRepresentative.get(key);
            ActiveEmitter chosen = null;
            if (sticky != null) {
                for (ActiveEmitter c : candidates) {
                    if (c.pos().equals(sticky)) {
                        chosen = c;
                        break;
                    }
                }
            }
            if (chosen == null) {
                // No hay representante pegajoso todavía, o el anterior ya no
                // es candidato en esta celda (se rompió/decayó/salió de
                // rango) -- se elige uno nuevo, determinista, y se fija.
                for (ActiveEmitter c : candidates) {
                    if (chosen == null || isBetterRepresentative(c, chosen)) chosen = c;
                }
                stickyGroundRepresentative.put(key, chosen.pos());
            }
            Integer groundY = findGroundY(world, chosen.pos(), registered);
            if (groundY == null) continue;
            spots.add(new GroundSpot(key, chosen.pos(), groundY, chosen.profile()));
        }
        // Limpia celdas que ya no tienen ningún candidato (el árbol salió de
        // rango del todo) para no acumular memoria indefinidamente.
        stickyGroundRepresentative.keySet().retainAll(byCell.keySet());
        return spots;
    }

    private static boolean isBetterRepresentative(ActiveEmitter candidate, ActiveEmitter current) {
        if (candidate.pos().getY() != current.pos().getY()) return candidate.pos().getY() > current.pos().getY();
        return candidate.pos().asLong() < current.pos().asLong();
    }

    /** Primer bloque sólido por debajo de {@code from} que NO sea él mismo
     * un emisor registrado (una hoja), o {@code null} si no hay ninguno
     * dentro de {@link #GROUND_SCAN_DEPTH} bloques.
     *
     * <p><b>Corrección real (ver documentación de clase, punto 3)</b>: las
     * hojas (`LeavesBlock`) tienen colisión completa -- un rayo hacia
     * abajo desde una hoja alta de una copa densa y por capas (como la
     * del abeto) encontraba casi siempre OTRA hoja más abajo del mismo
     * árbol antes de llegar al suelo real, dando una
     * {@code verticalDistance} casi nula y por tanto una atenuación casi
     * siempre al máximo -- la causa real, con alta probabilidad, de "el
     * suelo sigue demasiado iluminado" (sección 5 del encargo). Ahora se
     * saltan explícitamente los bloques que son ellos mismos un emisor
     * registrado, continuando el rayo hacia abajo hasta encontrar
     * terreno/agua real.</p> */
    private static Integer findGroundY(ClientWorld world, BlockPos from,
                                        Map<net.minecraft.util.Identifier, EmitterProfile> registered) {
        BlockPos.Mutable cursor = from.mutableCopy();
        for (int i = 0; i < GROUND_SCAN_DEPTH; i++) {
            cursor.setY(cursor.getY() - 1);
            if (!world.isInBuildLimit(cursor)) return null;
            BlockState state = world.getBlockState(cursor);
            if (registered.containsKey(Registries.BLOCK.getId(state.getBlock()))) {
                continue; // es otra hoja (u otro emisor) de la misma copa, no es "suelo"
            }
            if (!state.getCollisionShape(world, cursor).isEmpty() || !state.getFluidState().isEmpty()) {
                return cursor.getY();
            }
        }
        return null;
    }

    /** Diagnóstico bajo demanda (sección "Error 2" del encargo): recorre
     * de verdad, sin usar ninguna caché, un volumen fijo alrededor de
     * {@code center}, y cuenta por identificador de bloque cuántas
     * posiciones son un emisor registrado -- para poder comparar contra
     * {@link #active()} y ver exactamente cuántas hojas reales hay, y
     * cuántas de ellas llegan al renderizador. No se usa en el render
     * normal, sólo quien invoque el comando de depuración lo llama. */
    public static Map<net.minecraft.util.Identifier, Integer> debugCountRealEmitters(ClientWorld world, BlockPos center, int radius) {
        Map<net.minecraft.util.Identifier, EmitterProfile> registered = ApuYawarLighting.registeredEmitters();
        Map<net.minecraft.util.Identifier, Integer> counts = new HashMap<>();
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int worldX = center.getX() + dx;
                int worldZ = center.getZ() + dz;
                if (!world.isChunkLoaded(worldX >> 4, worldZ >> 4)) continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    cursor.set(worldX, center.getY() + dy, worldZ);
                    if (!world.isInBuildLimit(cursor)) continue;
                    BlockState state = world.getBlockState(cursor);
                    if (state.isAir()) continue;
                    net.minecraft.util.Identifier id = Registries.BLOCK.getId(state.getBlock());
                    if (registered.containsKey(id)) {
                        counts.merge(id, 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    private record ChunkCache(List<BlockPos> positions, long builtAtRescan) {}

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
