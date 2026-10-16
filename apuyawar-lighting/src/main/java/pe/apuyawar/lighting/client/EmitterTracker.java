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
     * se vuelve a indexar desde cero (para notar árboles nuevos/crecidos,
     * o talados, en un chunk ya cacheado) — V4 fase 4 (sección 6.4 del
     * encargo: "un árbol que crece con polvo de hueso debería ser
     * reconocido sin esperar innecesariamente a que expire toda la
     * caché"): bajado de 200 a 80 rescaneos (≈100 s → ≈40 s) -- sigue
     * siendo barato (sólo re-escanea el chunk donde el jugador sigue
     * parado/cerca, no todo el mundo cacheado a la vez, ver
     * {@link #MAX_NEW_CHUNK_SCANS_PER_RESCAN}), pero reacciona bastante
     * más rápido a un árbol recién crecido o talado sin necesitar un
     * listener de cambios de bloque (Mixin) nuevo esta ronda. */
    private static final int CHUNK_CACHE_TTL_RESCANS = 80;
    /** Lado (en bloques) de cada celda de la rejilla de focos de suelo —
     * fija en coordenadas del mundo, nunca centrada en el jugador. */
    private static final int GROUND_CELL_SIZE = 8;
    private static final int GROUND_SCAN_DEPTH = 32;

    /** V4.1, entrega 2 (sección 5.2 del encargo: "la emisión visual de
     * las hojas y los emisores puntuales deben tener presupuestos...
     * separados... las hojas no deberían perder su textura emisiva
     * únicamente porque otro árbol consumió los 96 emisores"). Tope
     * propio, mucho más alto que {@code maxActiveLights} (96 en HIGH),
     * para el follaje emisivo real (ver {@code WorldLightingRenderer
     * #renderEmissiveSurfaces}) -- dibujar un cubo con textura no tiene
     * el mismo costo que un punto de luz con halos, así que no debería
     * compartir el mismo presupuesto restrictivo. Sigue acotado (nunca
     * "sin límite") por si un escaneo real encuentra más hojas de las
     * esperadas dentro del radio. */
    private static final int MAX_EMISSIVE_SURFACES = 600;

    private static int tickCounter = 0;
    private static long rescanCounter = 0;
    private static List<ActiveEmitter> active = List.of();
    /** V4.1, entrega 2: candidatos de follaje emisivo, INDEPENDIENTES
     * del recorte por presupuesto de {@link #active} -- ver
     * documentación de {@link #MAX_EMISSIVE_SURFACES}. */
    private static List<ActiveEmitter> emissiveSurfaces = List.of();
    private static List<GroundSpot> groundSpots = List.of();
    /** V4, fase 4: cuántos emisores reales había y cuántos recibió cada
     * celda/árbol en el último reparto de presupuesto -- sólo para el
     * comando de diagnóstico {@code debug arboles}, ver
     * {@link #selectWithPerTreeBudget}. {@code int[2]}: {encontrados,
     * cupo asignado}. Vacío si sólo había un árbol (no se aplicó
     * reparto por árbol, ver el atajo en {@link #selectWithPerTreeBudget}). */
    private static Map<Long, int[]> lastTreeBudgets = Map.of();
    private static ActiveEmitter testEmitter = null;
    private static GroundSpot testGroundSpot = null;
    private static final Map<Long, Float> fade = new HashMap<>();
    private static final Map<Long, Float> emissiveFade = new HashMap<>();
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

    /** V4.1, entrega 2: candidatos de follaje emisivo real, con su
     * fundido de entrada/salida PROPIO -- independientes del recorte
     * por presupuesto de {@link #active()} (ver
     * {@link #MAX_EMISSIVE_SURFACES}). {@code WorldLightingRenderer
     * #renderEmissiveSurfaces} itera esta lista, no {@link #active()}. */
    public static List<ActiveEmitter> emissiveSurfaces() {
        if (!ApuYawarLightingClient.config().rgbEnabled) return List.of();
        List<ActiveEmitter> result = new ArrayList<>(emissiveSurfaces.size());
        for (ActiveEmitter e : emissiveSurfaces) {
            float f = emissiveFade.getOrDefault(e.pos().asLong(), 0f);
            if (f > 0.001f) result.add(e.withFade(f));
        }
        return result;
    }

    /** Focos de suelo activos, uno como máximo por celda ocupada de la
     * rejilla (ver documentación de clase), con su fundido de
     * entrada/salida ya aplicado, más el foco de prueba de la sección
     * 4.2 del encargo V4.2 si hay uno colocado (siempre con fundido
     * 1.0, ignorando por completo la selección de hojas/sticky
     * representative -- un foco de prueba real, anclado a una posición
     * de mundo fija, para aislar el pipeline gráfico). */
    public static List<GroundSpot> groundSpots() {
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled) return List.of();
        List<GroundSpot> result = new ArrayList<>(groundSpots.size() + 1);
        if (testGroundSpot != null) result.add(testGroundSpot);
        if (!config.groundDecalsEnabled) return result;
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

    /** V4.2, entrega 2 (sección 4.2 del encargo: "necesito un modo
     * diagnóstico que dibuje temporalmente un foco en coordenadas de
     * mundo conocidas... ignorar temporalmente la lógica de selección
     * de hojas"). {@code anchorPos}/{@code groundY} son arbitrarios,
     * pasados directamente -- no derivan de ningún escaneo ni de
     * {@code findGroundY}, para aislar el pipeline de dibujo del
     * pipeline de detección. Visible aunque {@code groundDecalsEnabled}
     * esté desactivado (es una prueba manual explícita, no un foco
     * real). */
    public static void setTestGroundSpot(BlockPos anchorPos, int groundY, EmitterProfile profile) {
        testGroundSpot = new GroundSpot(0L, anchorPos, groundY, profile, 1.0f);
    }

    public static void clearTestGroundSpot() {
        testGroundSpot = null;
    }

    public static boolean hasTestGroundSpot() {
        return testGroundSpot != null;
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
        updateFadeMap(fade, activePositionKeys(active), ApuYawarLightingClient.config().fadeTransitionTicks);
        updateFadeMap(emissiveFade, activePositionKeys(emissiveSurfaces), ApuYawarLightingClient.config().fadeTransitionTicks);
        updateFadeMap(groundFade, groundSpotKeys(), ApuYawarLightingClient.config().fadeTransitionTicks);
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        rescan(client);
    }

    private static Set<Long> activePositionKeys(List<ActiveEmitter> list) {
        Set<Long> keys = new HashSet<>();
        for (ActiveEmitter e : list) keys.add(e.pos().asLong());
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
            emissiveSurfaces = List.of();
            groundSpots = List.of();
            testEmitter = null;
            testGroundSpot = null;
            fade.clear();
            emissiveFade.clear();
            groundFade.clear();
            chunkCache.clear();
            stickyGroundRepresentative.clear();
            return;
        }
        LightingConfig config = ApuYawarLightingClient.config();
        if (!config.rgbEnabled || !ApuYawarLighting.hasAnyEmitters()) {
            active = List.of();
            emissiveSurfaces = List.of();
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
        List<ActiveEmitter> capped = found.size() > cap ? selectWithPerTreeBudget(found, cap) : new ArrayList<>(found);
        active = capped;

        // V4.1, entrega 2: lista INDEPENDIENTE para el follaje emisivo
        // real -- ver documentación de MAX_EMISSIVE_SURFACES. No
        // reutiliza `capped`: una hoja con máscara emisiva debe brillar
        // aunque no haya entrado en el cupo (mucho más pequeño) de
        // puntos de luz.
        List<ActiveEmitter> emissiveCandidates = new ArrayList<>();
        for (ActiveEmitter e : found) {
            if (e.isFoliage() && e.profile().hasEmissiveMask()) emissiveCandidates.add(e);
        }
        // Si varios árboles juntos superan incluso este presupuesto más
        // generoso, reparte por árbol (la misma lógica que ya protege a
        // los puntos de luz, sección 5.3 del encargo: "no basta con
        // seleccionar los más cercanos... distribución por árbol") en
        // vez de truncar por cercanía/prioridad sin más, lo que dejaría
        // a los árboles más lejanos sin ningún follaje emisivo.
        emissiveSurfaces = emissiveCandidates.size() > MAX_EMISSIVE_SURFACES
                ? selectWithPerTreeBudget(emissiveCandidates, MAX_EMISSIVE_SURFACES, false)
                : emissiveCandidates;

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

    /** Reparte {@code cap} en tres cupos (uno por banda de altura) --
     * delega en {@link #distributeQuotaAcrossGroups}, la versión
     * genérica para N grupos que también usa
     * {@link #selectWithPerTreeBudget} (V4 fase 4) para repartir entre
     * árboles en vez de bandas. */
    private static int[] distributeQuota(int cap, int lowerCount, int middleCount, int upperCount) {
        return distributeQuotaAcrossGroups(cap, new int[] {lowerCount, middleCount, upperCount});
    }

    /** Reparte {@code cap} en {@code counts.length} cupos, empezando por
     * una parte igual para cada grupo y, si alguno tiene menos
     * candidatos de los que le tocan, cediendo lo que no puede usar a
     * los demás grupos que todavía tengan margen -- así un grupo con
     * pocos candidatos (p. ej. la punta de un árbol, o un árbol
     * pequeño junto a uno grande) no desperdicia cupo: lo recibe el
     * grupo con más candidatos reales. Genérico sobre N grupos --
     * usado tanto para las 3 bandas de altura de un árbol
     * ({@link #selectStratifiedByHeight}) como para repartir el cupo
     * global entre varios árboles ({@link #selectWithPerTreeBudget},
     * sección 6.2 del encargo V4: "si existen varios árboles juntos,
     * uno solo no debería consumir todo el presupuesto"). */
    private static int[] distributeQuotaAcrossGroups(int cap, int[] counts) {
        int n = counts.length;
        int[] quota = new int[n];
        if (n == 0) return quota;
        int base = cap / n;
        int remainder = cap - base * n;
        for (int i = 0; i < n; i++) quota[i] = base + (i < remainder ? 1 : 0);
        for (int pass = 0; pass < n; pass++) {
            int leftover = 0;
            boolean[] saturated = new boolean[n];
            for (int i = 0; i < n; i++) {
                if (quota[i] > counts[i]) {
                    leftover += quota[i] - counts[i];
                    quota[i] = counts[i];
                    saturated[i] = true;
                }
            }
            if (leftover == 0) break;
            List<Integer> open = new ArrayList<>();
            for (int i = 0; i < n; i++) {
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

    /** V4, fase 4 (sección 6.2 del encargo: "presupuesto de emisores
     * por árbol... uno solo no debería consumir todo el presupuesto").
     * Agrupa los emisores detectados por la MISMA rejilla de celdas de
     * 8×8 bloques que ya agrupa los focos de suelo
     * ({@link #cellKey(BlockPos)}) -- una aproximación espacial barata
     * a "un árbol", consistente con el resto del motor, sin necesitar
     * un algoritmo de components conexos nuevo. Reparte el cupo GLOBAL
     * entre las celdas ocupadas (vía {@link #distributeQuotaAcrossGroups})
     * y, DENTRO de cada celda, aplica el reparto por bandas de altura
     * ya existente ({@link #selectStratifiedByHeight}) -- así ni un
     * árbol con copa muy densa deja a los árboles vecinos sin ninguna
     * luz, ni dentro de un mismo árbol se repite el sesgo hacia las
     * hojas más cercanas (Error 3 de la V3). Con un solo árbol/celda en
     * rango, el comportamiento es idéntico al de antes de esta fase. */
    private static List<ActiveEmitter> selectWithPerTreeBudget(List<ActiveEmitter> found, int cap) {
        return selectWithPerTreeBudget(found, cap, true);
    }

    /** @param trackForDiagnostics si actualiza {@link #lastTreeBudgets}
     *        (el comando {@code debug arboles}) -- V4.1 llama a este
     *        método también para el follaje emisivo
     *        ({@link #MAX_EMISSIVE_SURFACES}), con {@code false} aquí,
     *        para no pisar el diagnóstico del reparto de PUNTOS DE LUZ
     *        con el del follaje emisivo (presupuestos independientes a
     *        propósito, sección 5.2 del encargo V4.1). */
    private static List<ActiveEmitter> selectWithPerTreeBudget(List<ActiveEmitter> found, int cap, boolean trackForDiagnostics) {
        Map<Long, List<ActiveEmitter>> byCell = new java.util.LinkedHashMap<>();
        for (ActiveEmitter e : found) {
            byCell.computeIfAbsent(cellKey(e.pos()), k -> new ArrayList<>()).add(e);
        }
        if (byCell.size() <= 1) {
            if (trackForDiagnostics) lastTreeBudgets = Map.of();
            return selectStratifiedByHeight(found, cap);
        }

        List<Long> cellKeys = new ArrayList<>(byCell.keySet());
        int[] counts = new int[cellKeys.size()];
        for (int i = 0; i < cellKeys.size(); i++) counts[i] = byCell.get(cellKeys.get(i)).size();
        int[] quota = distributeQuotaAcrossGroups(cap, counts);

        Map<Long, int[]> budgets = new HashMap<>();
        List<ActiveEmitter> result = new ArrayList<>(cap);
        for (int i = 0; i < cellKeys.size(); i++) {
            int cellQuota = quota[i];
            budgets.put(cellKeys.get(i), new int[] {counts[i], cellQuota});
            if (cellQuota <= 0) continue;
            List<ActiveEmitter> cellEmitters = byCell.get(cellKeys.get(i));
            result.addAll(cellEmitters.size() <= cellQuota
                    ? cellEmitters
                    : selectStratifiedByHeight(cellEmitters, cellQuota));
        }
        if (trackForDiagnostics) lastTreeBudgets = budgets;
        return result;
    }

    /** V4, fase 4: ver documentación de {@link #lastTreeBudgets}. */
    public static Map<Long, int[]> treeBudgets() {
        return lastTreeBudgets;
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

    /** V4.2, entrega 2 -- causa raíz real confirmada del "foco de suelo
     * registrado pero invisible" (sección 4.1 del encargo, el equipo ya
     * había diagnosticado correctamente la hipótesis): esta función
     * ANTES preferÍa la hoja de MAYOR altura de la celda
     * (`candidate.pos().getY() > current.pos().getY()`). Combinado con
     * {@code WorldLightingRenderer#addGroundSpot}, donde
     * {@code maxDistance = groundSpotRadius * 6} (≈9.6 bloques con el
     * valor por defecto 1.6) y el foco se descarta por completo
     * (`return false`, CERO vértices) en cuanto la distancia vertical
     * del ancla al suelo supera ese valor -- para un Abeto Celestial de
     * más de ~10 bloques de alto (la silueta real de pino, con varias
     * capas), la hoja MÁS ALTA de la copa, elegida como representante,
     * está casi garantizado que exceda esa distancia. El foco quedaba
     * registrado en {@code EmitterTracker.groundSpots()} (con su
     * `anchorPos`, `groundY`, etc. todos válidos) pero
     * {@code addGroundSpot} nunca llegaba a generar un solo triángulo
     * -- exactamente "22 focos registrados" sin ningún píxel visible.
     * Invertido a preferir la hoja de MENOR altura (más cerca del
     * suelo real, donde la caída vertical es mínima) -- sigue siendo
     * una elección determinista (mismo empate por
     * {@code BlockPos#asLong()}), sólo cambia CUÁL hoja gana. */
    private static boolean isBetterRepresentative(ActiveEmitter candidate, ActiveEmitter current) {
        if (candidate.pos().getY() != current.pos().getY()) return candidate.pos().getY() < current.pos().getY();
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
