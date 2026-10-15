# Momento Cuy — árboles celestiales (crecimiento, pétalos, luz) y corrección real de las barandillas

Fecha: 14 de octubre de 2026. Rama: `feat/momento-cuy-arboles-celestiales`,
basada en el estado funcional más reciente de
`fix/momento-cuy-geometria-bloques` (commit `ffdb64a`, que corrigió la
geometría de plantas/arbustos/barandillas). No se hizo merge a `main`.

**Estado: compila el módulo nuevo y el proyecto completo (`gradlew build`),
`verifyJudgments` de `apuyawar-systems` sigue en verde (128/135, sin
cambios), y se arrancó un servidor dedicado real (`gradlew
:momento-cuy:runServer`) que terminó en `Done (7.152s)! For help, type
"help"` sin ningún error ni advertencia relacionada con los archivos nuevos
de este módulo** — lo que confirma que el JSON de los árboles
(`configured_feature`), los tags de barandillas y el registro de los
bloques de hoja/brote nuevos son válidos y cargan correctamente. **No se
pudo probar dentro del juego en sí** (plantar, ver crecer, aplicar polvo de
hueso, ver partículas) — no hay cliente gráfico en este entorno; queda
pendiente que el equipo lo compruebe en Minecraft siguiendo la sección 9.

## 0. Investigación previa (sin adivinar ninguna firma)

Antes de escribir código se verificó por bytecode real (`javap -p -c` sobre
el jar de Minecraft 1.21.1 con mappings Yarn que usa Loom,
`minecraft-unpicked.jar` del caché de `net.fabricmc.yarn.1_21_1.1.21.1+build.3`)
y JSON real extraído del mismo jar:

- `SaplingBlock`/`SaplingGenerator` reales y su arquitectura completa.
- El JSON real de los árboles de cerezo y abeto vanilla
  (`data/minecraft/worldgen/configured_feature/cherry.json`/`spruce.json`),
  incluyendo los parámetros exactos de `cherry_trunk_placer` y
  `spruce_foliage_placer`.
- `LeavesBlock`/`CherryLeavesBlock` reales, y se confirmó que
  **`ParticleTypes.CHERRY_LEAVES` es una partícula de pétalo cayendo real
  de vanilla** (`CherryLeavesParticle`, cliente), separada del bloque
  decorativo de pétalos en el suelo (`FlowerbedBlock`, antes conocido como
  "Pink Petals").
- `AbstractBlock.Settings#luminance(ToIntFunction<BlockState>)` y el
  helper `Blocks.createLightLevelFromLitBlockState` (no aplica a nuestro
  caso, es para bloques con una propiedad `LIT`; usamos una lambda
  constante en su lugar).
- `DustParticleEffect(Vector3f color, float scale)` y `ParticleTypes.DUST`.
- `Block#randomDisplayTick` — confirmado que lo llama únicamente
  `ClientWorld` (cliente), sin depender de `.ticksRandomly()`; en cambio
  `randomTick` (crecimiento de brotes, descomposición de hojas) **sí** lo
  necesita.
- `Fertilizable` — ya implementada por el `SaplingBlock` real de vanilla,
  no hubo que escribirla a mano.
- **El contenido real de `data/minecraft/tags/block/{fences,wooden_fences,walls}.json`**,
  y el código real de `FenceBlock#canConnect`/`WallBlock` — ver sección 3,
  es la causa real del problema de conexión reportado.

Todos estos hallazgos están respaldados por bytecode/JSON real, no
supuestos — ver detalle en cada sección.

## 1. Brotes reales: `SaplingBlock` de vanilla, no una clase propia

`momento_cuy:brote_cerezo` y `momento_cuy:brote_abeto_celestial` dejan de
usar `MomentoCuyPlantBlock` (decorativo, sin crecimiento) y pasan a ser el
**`net.minecraft.block.SaplingBlock` real de vanilla** — la misma clase que
usa cualquier brote de Minecraft. No se escribió una subclase propia: por
bytecode se confirmó que `SaplingGenerator` **no** es una clase abstracta
con subclases por árbol (`CherrySaplingGenerator`, etc. no existen); es una
única clase final, controlada enteramente por datos (`Optional<RegistryKey<ConfiguredFeature<?,?>>>`).
Por eso `MomentoCuyTrees.java` (nuevo) sólo crea dos **instancias** de esa
clase real, igual que vanilla crea `SaplingGenerator.CHERRY`/`.SPRUCE`:

```java
SaplingGenerator("cerezo_celestial", Optional.empty(), Optional.of(CEREZO_CELESTIAL), Optional.empty());
SaplingGenerator("abeto_celestial", Optional.empty(), Optional.of(ABETO_CELESTIAL), Optional.empty());
```

(firma real de 4 parámetros confirmada por el compilador tras un primer
intento con 3 — `id, megaVariant, regularVariant, beesVariant`; no
queremos variante "mega" de 2×2 ni de abejas).

`MomentoCuyBlocks.newBlock()` ahora instancia
`new SaplingBlock(MomentoCuyTrees.CEREZO_GENERATOR, settings)` /
`...ABETO_GENERATOR...` para esos dos diseños. Esto nos da **gratis**, sin
código propio:
- Crecimiento natural (`SaplingBlock.randomTick`: si hay luz ≥9 y
  `random.nextInt(7)==0`).
- Polvo de hueso real (`SaplingBlock implements Fertilizable`:
  `canGrow` = `random.nextFloat() < 0.45` — la misma probabilidad razonable
  de vanilla, no "siempre crece a la primera", tal como pedía el encargo).
- El doble golpe de hueso típico de Minecraft (una aplicación puede sólo
  avanzar la propiedad interna `STAGE` 0→1 antes de intentar generar el
  árbol — mismo comportamiento que cualquier brote vanilla).

El modelo de bloque (`minecraft:block/cross` con la textura `side.png`
propia) y su blockstate (variante `""` en blanco) **no cambiaron** — se
confirmó contra el `oak_sapling.json` real de vanilla que una única
variante en blanco es válida aunque el bloque tenga la propiedad `STAGE`
(es una propiedad de estado interno sin efecto visual, no necesita
cubrirse en el blockstate).

## 2. Árboles como `ConfiguredFeature` de datos, reutilizando el algoritmo real de vanilla

No se escribió ningún generador de árbol en Java. Siguiendo exactamente la
arquitectura de datos de vanilla, se crearon:

- `data/momento_cuy/worldgen/configured_feature/cerezo_celestial.json`
- `data/momento_cuy/worldgen/configured_feature/abeto_celestial.json`

Ambos son copias estructurales de los JSON reales de `cherry.json`/
`spruce.json` de Minecraft (mismo `trunk_placer`/`foliage_placer` y mismos
parámetros numéricos — mismo tipo de forma, ramas y copa que un cerezo/
abeto vanilla), cambiando únicamente:

- `trunk_provider` → `momento_cuy:tronco_cerezo_celestial` /
  `momento_cuy:tronco_abeto_celestial` (con `"axis":"y"`).
- `foliage_provider` → en vez de `simple_state_provider` (un solo tipo de
  hoja, como hace vanilla), se usa **`weighted_state_provider`** combinando
  nuestras variedades de hoja:
  - Cerezo: `hojas_cerezo_rosa_claro` (peso 4), `hojas_cerezo_rosa_intenso`
    (peso 4), `hojas_cerezo_florecido` (peso 2, más baja a propósito para
    que la variedad luminosa no domine la copa).
  - Abeto: `hojas_pino_esmeralda` (peso 3), `hojas_pino_sombrio` (peso 2).
  Esto es justo lo que pedía el encargo ("la copa deberá combinar estos
  tipos de hojas... si utilizas proveedores de estados ponderados,
  asegúrate de que su comportamiento sea válido" — `weighted_state_provider`
  es el tipo de datos real de Minecraft para esto, usa internamente
  `net.minecraft.util.collection.DataPool`).
- Abeto: `trunk_placer.base_height` subido de 5 (vanilla) a 8, con
  `height_rand_a=3`/`height_rand_b=2`, para un árbol notablemente más alto
  ("alto, de tronco vertical" pedido), sin tocar el `spruce_foliage_placer`
  (la silueta cónica ya es la de un abeto reconocible).
- Cerezo: parámetros sin cambios respecto a vanilla (altura 7, ramificación
  `cherry_trunk_placer` con 1-3 ramas, copa ancha radio 4) — ya da el
  "cerezo con ramas, volumen y copa amplia" pedido, sin inventar una forma
  nueva.

**Por qué esto cumple la sección 5 (seguridad antes de generar el árbol)
casi por completo sin código propio**: el tipo de feature `minecraft:tree`
de vanilla ya valida espacio libre antes de colocar cualquier bloque
(campo `minimum_size`, copiado de cherry/spruce sin cambios), y sólo
reemplaza aire/hojas/vides, nunca construcciones del jugador — es el mismo
mecanismo que protege cualquier árbol vanilla al crecer junto a una
construcción. No se implementó ninguna comprobación propia porque
reutilizar el feature real de Minecraft ya la incluye.

**No se registró ningún `placed_feature`** ni se tocó el bioma de la
dimensión (`cielo_momento_cuy.json`) — a propósito: sólo queremos que el
brote plantado pueda convertirse en árbol, no generación de bosques al
crear chunks, tal como pedía explícitamente el encargo.

## 3. Troncos: corregida la rotación por eje (bug real encontrado durante esta revisión)

Al revisar `PillarBlock` (la clase real de vanilla ya usada para los
troncos) se encontró que su blockstate tenía una única variante `""` sin
cubrir la propiedad `axis` que `PillarBlock` sí registra — confirmado
comparando contra el `oak_log.json` real de vanilla, que si necesita
variantes `axis=x`/`axis=y`/`axis=z` con sus rotaciones (a diferencia de
`STAGE` en los brotes, `axis` sí cambia la apariencia). Sin esto, cualquier
tronco horizontal (las ramas del cerezo, por ejemplo) se habría visto
siempre con la orientación vertical por defecto — justo el problema de
"respetar la orientación de los troncos" que pedía el encargo.

Se corrigió para los dos troncos usados por los árboles
(`tronco_cerezo_celestial`, `tronco_abeto_celestial`):
- Modelo de bloque cambiado de `minecraft:block/cube` (6 texturas
  explícitas) a `minecraft:block/cube_column` (`end`=`top.png`,
  `side`=`side.png` — ya no se usa `side_alt.png` en estos dos, igual que
  un tronco vanilla usa una sola textura de lado que rodea las 4 caras).
- Nuevo modelo `..._horizontal.json` que hereda de
  `minecraft:block/cube_column_horizontal` (plantilla real de vanilla —
  idéntica a `cube_column` salvo que rota 180° la textura de la tapa para
  que no se vea invertida al tumbar el tronco).
- Blockstate reescrito con las 3 variantes `axis=x/y/z` y las mismas
  rotaciones que usa `oak_log.json` de vanilla.

**No se tocaron** `viga_cerezo`, `vigas_reforzadas_abeto` ni
`pilar_cuarzo_lavanda` — son `PillarBlock` también y tienen el mismo
problema latente, pero no son necesarios para el crecimiento de los
árboles y no fueron mencionados en este encargo; se documentan aquí como
problema conocido (sección 10) en vez de tocarlos sin pedírmelo.

## 4. Hojas: `LeavesBlock`/`CherryLeavesBlock` reales, con subclases propias sólo para las partículas extra

Las 5 hojas dejan de ser un `Block` simple (sin transparencia de
comportamiento, sin distancia al tronco, sin descomposición) y pasan a
usar las clases reales de hoja de Minecraft:

- `hojas_cerezo_rosa_claro`, `hojas_cerezo_rosa_intenso` →
  `net.minecraft.block.CherryLeavesBlock` **directamente** (la clase real
  de vanilla, sin subclase propia). Esto les da gratis: pétalos cayendo
  (`ParticleTypes.CHERRY_LEAVES`, la partícula real de vanilla — ver
  sección 0), goteo de lluvia, y la descomposición/distancia al tronco de
  cualquier hoja real.
- `hojas_cerezo_florecido` → `MomentoCuyBlossomLeavesBlock` (nueva,
  extiende `CherryLeavesBlock`): hereda los pétalos de la clase anterior
  (`super.randomDisplayTick`) y además añade destellos propios — ver
  sección 5.
- `hojas_pino_esmeralda`, `hojas_pino_sombrio` →
  `MomentoCuyAbetoLeavesBlock` (nueva, extiende `LeavesBlock`): misma
  colisión/distancia/descomposición reales, con chispas turquesa/celeste
  propias — ver sección 6.

En `MomentoCuyBlocks.baseSettings()` se añadió `.ticksRandomly()` para
brotes y hojas (`design.startsWith("sapling_")` / `"leaf_"`) — se
confirmó por bytecode que sin esto el selector de tick aleatorio del
servidor **nunca** llama a `randomTick` (ni el crecimiento del brote ni la
descomposición de hojas funcionarían). Esto **no** afecta a las
partículas: se confirmó que `randomDisplayTick` lo llama `ClientWorld`
para cualquier bloque sin mirar esa bandera.

Nada de esto cambió los identificadores de bloque, sus modelos de bloque/
ítem, ni la capa de render (cutout, ya configurada desde la corrección de
geometría anterior) — sólo la clase Java que los implementa.

## 5. Cerezo Celestial — pétalos y aura (sección prioritaria del encargo)

**Pétalos**: ya cubiertos en la sección 4 — las 3 hojas rosadas (vía
`CherryLeavesBlock`, directamente o heredado por
`MomentoCuyBlossomLeavesBlock`) desprenden la partícula real de pétalo de
cerezo de Minecraft (`ParticleTypes.CHERRY_LEAVES`), con la misma
frecuencia/comportamiento que un cerezo vanilla (cae lento, se mece de
lado a lado, no se acumula en pantalla — es la física ya calibrada de
`CherryLeavesParticle`). No se generó ningún sprite nuevo: es la partícula
ya existente del juego.

**Aura rosada + destellos dorados de Inti** (sólo en
`MomentoCuyBlossomLeavesBlock`, no en las otras dos hojas, para no saturar
el árbol): `randomDisplayTick` añade, tras heredar los pétalos, una
partícula `DustParticleEffect` rosa (`Vector3f(0.95, 0.55, 0.78)`) o,
1 de cada 3 veces, dorada (`Vector3f(1.0, 0.84, 0.35)`, el "destello de
Inti"), con probabilidad `1/14` de día y `1/6` de noche (más presencia
nocturna, como pedía el encargo, variando sólo la frecuencia de partícula
— nunca la luz del bloque, que permanece fija).

## 6. Cerezo Celestial — luz real (nivel moderado, sólo una variedad)

`hojas_cerezo_florecido` recibe `.luminance(state -> 7)` en
`baseSettings()` — dentro del rango moderado 6-8 que sugería el encargo,
usando la API real confirmada por bytecode
(`AbstractBlock.Settings#luminance(ToIntFunction<BlockState>)`; no existe
una sobrecarga con `int` plano, por eso es una lambda constante). Las
otras dos hojas rosadas **no** son luminosas, tal como pedía el encargo
("sin convertir necesariamente todas las hojas rosadas en fuentes de
luz"). Esta es iluminación real del motor de Minecraft (afecta el nivel de
luz de bloque del terreno de verdad) — **no** es luz de color: como
advierte el propio encargo, Minecraft no tiene iluminación ambiental RGB
por luminancia de bloque; el "rosado" del entorno nocturno lo dan
únicamente las partículas de la sección 5, nunca se afirmó lo contrario en
el código ni debe afirmarse al jugador.

## 7. Abeto Celestial — partículas y luz

`MomentoCuyAbetoLeavesBlock` (usada por ambas variedades de hoja de pino)
añade en `randomDisplayTick` chispas turquesa (`Vector3f(0.25, 0.85, 0.82)`)
o celeste (`Vector3f(0.42, 0.70, 1.0)`), con una pequeña deriva vertical
lenta (`driftY` aleatorio, sesgado ligeramente hacia arriba) para simular
una luciérnaga suspendida en vez de una partícula que cae — probabilidad
`1/20` de día, `1/8` de noche. A diferencia del cerezo, **ambas** hojas de
pino tienen partículas (el encargo pedía que aparecieran "alrededor de las
ramas y hojas" en general, sin restringir a una variedad), pero sólo
`hojas_pino_esmeralda` recibe `.luminance(state -> 6)` — la variedad
sombría se queda sin luz propia, para no iluminar todo el bosque como
advertía el encargo.

## 8. Barandillas — causa real del problema de conexión, y su corrección

El encargo reportó que las barandillas (sobre todo la de piedra) seguían
sin conectarse bien tras la corrección de geometría anterior, aunque ya
usan `FenceBlock`/`WallBlock` reales con blockstates `multipart` calcados
de `oak_fence.json`/`cobblestone_wall.json`. Se investigó el código real
(bytecode) de `FenceBlock#canConnect`/`WallBlock` en vez de asumir que el
problema estaba en el blockstate:

```java
// FenceBlock (real, HorizontalConnectingBlock)
private boolean canConnectToFence(BlockState state) {
    return state.isIn(BlockTags.FENCES)
        && (state.isIn(BlockTags.WOODEN_FENCES) == this.getDefaultState().isIn(BlockTags.WOODEN_FENCES));
}
```
```java
// WallBlock (real)
private boolean shouldConnectTo(BlockState state, boolean sideSolidFullSquare, Direction dir) {
    ...
    return state.isIn(BlockTags.WALLS) || (!cannotConnect(state) && sideSolidFullSquare) || ...
}
```

**La causa real era que ningún bloque del mod pertenecía a los tags
`minecraft:fences`/`minecraft:wooden_fences`/`minecraft:walls`.** La
decisión de conectar o no es por **pertenencia a tag**, no por ser la
misma clase Java ni por el blockstate — dos postes de nuestra propia
barandilla, al no estar en esos tags, nunca se consideraban "conectables"
entre sí (la rama alternativa, `sideSolidFullSquare`, tampoco se cumple
porque un poste de barandilla no es un cubo sólido completo). El
blockstate `multipart` en sí ya era correcto (mismas propiedades
`north`/`east`/`south`/`west` para la valla, `up`/`north_shape`/etc. para
el muro — confirmado contra `HorizontalConnectingBlock`/`WallBlock` real).

**Corrección**: dos archivos de datos nuevos, el mecanismo estándar con el
que cualquier mod extiende un tag de vanilla sin sobrescribirlo
(`"replace": false`, fusiona con el tag original):

- `data/minecraft/tags/block/wooden_fences.json` →
  añade `momento_cuy:barandilla_madera_celestial` (pertenecer a
  `wooden_fences` también la mete en `fences`, que a su vez incluye
  `#minecraft:wooden_fences`).
- `data/minecraft/tags/block/walls.json` →
  añade `momento_cuy:barandilla_piedra_ornamental`.

No se tocó ninguna clase Java de las barandillas (siguen siendo
`FenceBlock`/`WallBlock` reales) ni sus modelos/texturas — el problema
nunca estuvo en la geometría, sino en la membresía de tag que decide la
conexión. El arranque del servidor (sección "Estado", arriba) confirmó que
ambos archivos de tag son JSON válido y se fusionan sin error; falta la
prueba visual en el juego (lista en la sección 9).

## 9. Pruebas pendientes en Minecraft (requieren cliente gráfico)

**Árboles:**
1. Plantar `brote_cerezo` y `brote_abeto_celestial`; confirmar que
   conservan su textura/ícono actuales.
2. Esperar crecimiento natural (puede tardar, es probabilístico como
   cualquier brote vanilla) y aplicar polvo de hueso (confirmar que no
   siempre crece a la primera aplicación).
3. Confirmar que el cerezo adulto usa `tronco_cerezo_celestial` con ramas
   visibles en la orientación correcta (ya no siempre verticales), copa
   amplia combinando las 3 hojas rosadas, y que no destruye construcciones
   cercanas si no hay espacio.
4. Confirmar que el abeto adulto es notablemente alto, con copa cónica
   verde esmeralda/sombría.
5. Confirmar pétalos cayendo del cerezo adulto, destellos rosa/dorados
   concentrados donde hay `hojas_cerezo_florecido`, y luz suave visible de
   noche sin parpadeo violento.
6. Confirmar chispas turquesa/celeste en el abeto, visibles de noche, y
   que `hojas_pino_esmeralda` emite luz suave mientras `hojas_pino_sombrio`
   no.
7. Confirmar que ninguna hoja desaparece de golpe por un cálculo de
   distancia incorrecto, y que las hojas sueltas (sin tronco cerca)
   terminan descomponiéndose solas con el tiempo.
8. Probar en la Overworld y en la dimensión Momento Cuy.
9. Vigilar rendimiento con varios árboles juntos (jardines/islas) —
   confirmar que no hay caída notable de FPS ni de TPS del servidor.

**Barandillas:**
10. Colocar barandilla de madera: 1 suelta, 2 juntas, 5 en línea, esquina,
    T, cruce de 4 — confirmar que ahora sí se conectan entre sí.
11. Lo mismo para la barandilla de piedra (postes altos/bajos según
    vecino).
12. Confirmar colisión (no se puede atravesar ni caer por el hueco).

## 10. Rendimiento y multijugador

- Todas las partículas se generan únicamente desde `randomDisplayTick`,
  confirmado por bytecode que sólo lo invoca `ClientWorld` — no hay
  paquetes de red por partícula, ni lógica de servidor añadida por este
  efecto.
- Cada bloque de hoja decide su propia partícula de forma independiente
  con una probabilidad baja (1/14 a 1/20 de día, 1/6 a 1/8 de noche) sobre
  el mismo muestreo aleatorio de posiciones que ya usa el juego para
  cualquier bloque (no se añadió ningún bucle propio que recorra todo el
  árbol cada frame).
- No se instancian objetos de partícula nuevos por tick: las instancias de
  `DustParticleEffect` son constantes estáticas reutilizadas.
- El crecimiento (`SaplingBlock.randomTick`) y la descomposición
  (`LeavesBlock.randomTick`) usan el sistema de tick aleatorio normal de
  Minecraft (vía `.ticksRandomly()`), que ya respeta chunks cargados y la
  velocidad de tick aleatorio del mundo — no se creó ningún temporizador
  propio que siga corriendo con el chunk descargado.
- Ninguna clase nueva (`MomentoCuyTrees`, `MomentoCuyBlossomLeavesBlock`,
  `MomentoCuyAbetoLeavesBlock`) referencia clases de cliente
  (`net.minecraft.client....`) — son seguras de cargar en un servidor
  dedicado, confirmado por el arranque real de `:momento-cuy:runServer`
  (servidor puro, sin cliente) sin errores de carga de clase.

## 11. Archivos modificados/creados

**Java** (`momento-cuy/src/main/java/pe/apuyawar/momentocuy/block/`):
- `MomentoCuyTrees.java` (nuevo): claves de `ConfiguredFeature` + instancias
  de `SaplingGenerator`.
- `MomentoCuyBlossomLeavesBlock.java` (nuevo): hojas florecidas del cerezo.
- `MomentoCuyAbetoLeavesBlock.java` (nuevo): hojas del abeto.
- `MomentoCuyBlocks.java` (modificado): `newBlock()` ahora instancia
  `SaplingBlock`/`CherryLeavesBlock`/las dos clases nuevas; `baseSettings()`
  añade `.ticksRandomly()` para brotes/hojas y `.luminance(...)` para
  `hojas_cerezo_florecido`/`hojas_pino_esmeralda`.

**Datos** (`momento-cuy/src/main/resources/data/`):
- `momento_cuy/worldgen/configured_feature/cerezo_celestial.json` (nuevo).
- `momento_cuy/worldgen/configured_feature/abeto_celestial.json` (nuevo).
- `minecraft/tags/block/wooden_fences.json` (nuevo, fusión no destructiva).
- `minecraft/tags/block/walls.json` (nuevo, fusión no destructiva).

**Assets** (`momento-cuy/src/main/resources/assets/momento_cuy/`):
- `models/block/tronco_cerezo_celestial.json` (reescrito a `cube_column`) +
  `tronco_cerezo_celestial_horizontal.json` (nuevo).
- `models/block/tronco_abeto_celestial.json` (reescrito a `cube_column`) +
  `tronco_abeto_celestial_horizontal.json` (nuevo).
- `blockstates/tronco_cerezo_celestial.json` /
  `blockstates/tronco_abeto_celestial.json` (reescritos con variantes
  `axis=x/y/z`).

Ningún identificador de bloque/ítem cambió; ninguna textura PNG original
se modificó ni se generó una nueva.

## 12. Compatibilidad

No se tocó: Panel K, catálogos de mobs, recetas, misiones, sistema Staff,
Sol PE, la ruleta ni sus colores/animaciones, el sistema de Inti (salvo el
nombre temático "destello de Inti" en los comentarios del código de
partículas del cerezo, que no tiene relación con el sistema de juego Inti),
el Cuy Jardinero ni su agricultura, el acceso/salida de la dimensión, el
motor de minijuegos, ni el resto de los 80 bloques (salvo los 2 troncos de
árbol, documentado en la sección 3 con su razón). Los mundos ya creados
siguen cargando: ningún id de bloque cambió, sólo la clase Java detrás de
8 de ellos (2 brotes + 5 hojas... en realidad 2+5=7 bloques, más el
poste/lado de las barandillas que no cambiaron de clase, sólo ganaron
membresía de tag) y el modelo/blockstate de 2 troncos.

## 13. Problemas conocidos

- `viga_cerezo`, `vigas_reforzadas_abeto` y `pilar_cuarzo_lavanda` tienen el
  mismo bug de blockstate sin variantes `axis` que tenían los troncos antes
  de esta corrección (son `PillarBlock` también) — no se tocaron porque no
  son necesarios para el crecimiento de los árboles y no fueron
  mencionados en este encargo; queda pendiente una decisión del equipo
  sobre si corregirlos en una futura pasada.
- No se pudo verificar dentro de Minecraft ninguno de los comportamientos
  nuevos (crecimiento, polvo de hueso, partículas, luz, conexión de
  barandillas) — sólo se verificó que el servidor arranca sin errores de
  carga. La lista completa de pruebas pendientes está en la sección 9.
- La luminancia de `hojas_cerezo_florecido`/`hojas_pino_esmeralda` se
  escogió en 7 y 6 respectivamente como punto de partida razonable dentro
  del rango sugerido (6-8); el encargo pide comprobar en el juego "qué
  cantidad de hojas luminosas se necesita para una iluminación suave" — es
  un ajuste fino que sólo se puede calibrar viéndolo en Minecraft.
