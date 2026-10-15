# Momento Cuy — corrección de geometría de plantas, arbustos y barandillas

Fecha: 14 de octubre de 2026. Rama: `fix/momento-cuy-geometria-bloques`,
basada en el estado funcional más reciente de `feat/momento-cuy-dimension`
(commit `a00b292`, que ya corrigió el crash de creación de mundo por el bioma
y el ítem faltante del huevo de generación). No se hizo merge a `main`.

**Estado: compila el módulo nuevo y el proyecto completo
(`gradlew build`), los 38 archivos JSON nuevos/modificados se validaron con
un parser JSON independiente (sin errores de sintaxis), y `verifyJudgments`
de `apuyawar-systems` sigue en verde (128/135, sin cambios — no se tocó ese
módulo). Sin confirmación visual ni multijugador en Minecraft todavía — no
hay cliente gráfico en este entorno; queda pendiente que el equipo lo pruebe
en `runClient` siguiendo la lista de pruebas de la sección 6.** No se tocó
el acceso/salida de la dimensión, el Cuy Jardinero 3D ni sus
comportamientos, los bloques cúbicos normales, el sistema de minijuegos, el
Panel K, la ruleta, las misiones ni la economía de Sol PE.

## 1. Problema original

Los 80 bloques de Momento Cuy se registraron en la etapa anterior con sus
blockstates/modelos copiados tal cual del paquete de assets
(`assets-fuente/bloques/Momento_Cuy_80_Bloques`). La mayoría son bloques de
material sólido (tierra, roca, madera, cuarzo, cristal, vidrio) para los que
un `minecraft:block/cube` normal es correcto. Pero varios bloques —
brotes, flores, arbustos, enredaderas/musgo y las dos barandillas — también
quedaron con ese mismo modelo de cubo opaco (sólo con `nonOpaque()` cuando
tenían transparencia marcada en el CSV), lo que los hacía ver como cajas
planas en vez de plantas o barandillas reales, y las dos barandillas no
tenían ningún comportamiento de conexión: eran un bloque sólido fijo de
1×1×1 con la imagen de "barandilla completa" estampada en cada cara.

## 2. Brotes y flores (4 bloques): `minecraft:block/cross`

`brote_cerezo`, `brote_abeto_celestial`, `flor_estrella_azul`,
`flor_solar_dorada`.

- **Modelo**: cada uno pasó de `{"parent": "minecraft:block/cube", ...6
  texturas...}` a `{"parent": "minecraft:block/cross", "textures": {"cross":
  "momento_cuy:block/<id>/side"}}` — el mismo patrón que usa
  `minecraft:block/poppy` en vanilla (dos planos cruzados a ±45°, UV
  completo, sin ambient occlusion ni sombreado de cara, igual que el
  `cross.json` de Mojang). Reutiliza únicamente la textura `side.png` ya
  existente de cada bloque; no se generó ni modificó ninguna imagen.
- **Blockstate**: no cambió (`variants: {"": {"model": "..."}}`); el cambio
  está sólo en a qué modelo apunta esa variante.
- **Java**: nueva clase `MomentoCuyPlantBlock` (`block/MomentoCuyPlantBlock.java`),
  que extiende `PlantBlock` de vanilla (no `SaplingBlock` — eso exigiría un
  `SaplingGenerator` y lógica de crecimiento a árbol adulto, que el encargo
  pide explícitamente NO implementar en esta etapa — ni `FlowerBlock`, que
  exige parámetros de efecto de guiso sospechoso que no aplican aquí).
  Define su propio `MapCodec` (`createCodec`, el mismo patrón que usa
  `DeadBushBlock` en vanilla), una `VoxelShape` de selección/colisión
  pequeña y centrada (`3,0,3` a `13,10,13`, no la caja completa de un cubo)
  y `canPlantOnTop` permisivo (cualquier bloque con la cara de arriba
  sólida, no restringido a las etiquetas de tierra de vanilla, porque esto
  es una librería de construcción general, no un sistema de siembra
  realista).
- **Settings**: en `MomentoCuyBlocks.baseSettings`, estos cuatro diseños
  entran en `isNonSolidPlant()` y reciben `.noCollision()` — el jugador los
  atraviesa como cualquier planta vanilla; sólo se ve la `VoxelShape` para
  resaltado de selección.
- **Ítem**: los 4 `models/item/<id>.json` pasaron de heredar directamente el
  modelo de bloque a `{"parent": "minecraft:item/generated", "textures":
  {"layer0": "momento_cuy:block/<id>/side"}}` — el patrón exacto que usa
  vanilla para `poppy`/`vine` en inventario. Un modelo de bloque `cross` no
  trae transformaciones de pantalla (`display.gui`) calibradas para verse
  bien en un slot de inventario; `item/generated` sí, y es justo lo que
  permite el encargo ("puedes usar `minecraft:item/generated`").

## 3. Arbustos (2 bloques): volumen con planos múltiples

`arbusto_esmeralda`, `arbusto_florecido`.

Las texturas (`side.png`/`top.png`) muestran una mancha de hojas
redondeada, no una silueta delgada de tallo+pétalos como las flores — un
cruce de sólo 2 planos se vería notablemente más plano que el resto del
set. Se construyó `models/block/template_bush.json`, un modelo con **4
planos** (no 2) alrededor del mismo eje vertical central, en los ángulos
`0°`, `45°`, `22.5°` y `-22.5°` — verificado por bytecode
(`ModelElement$Deserializer.deserializeRotationAngle`, cargado de
Minecraft 1.21.1 vía `javap`) que el deserializador de modelos **sólo
acepta ángulos de exactamente `0`, `±22.5` o `±45` grados**; cualquier otro
valor lanza `JsonParseException` al cargar el modelo. La primera versión de
esta plantilla usaba `60°`/`-60°` (una distribución de 3 planos a 60° entre
sí) — se detectó el error antes de dejarlo en el repositorio y se corrigió
a los 4 ángulos válidos, que dan 8 "aspas" visibles (cada plano se ve desde
sus dos caras) y más densidad que el cruce estándar de 2 planos, sin
inventar ninguna textura nueva.

- `arbusto_esmeralda.json`/`arbusto_florecido.json` (modelo de bloque) pasan
  de `cube` de 6 texturas a `{"parent": "momento_cuy:block/template_bush",
  "textures": {"cross": "momento_cuy:block/<id>/side"}}`.
- **Java**: `MomentoCuyBushBlock` extiende `MomentoCuyPlantBlock` (mismo
  codec/no-sólido, pero con una `VoxelShape` ligeramente más ancha y alta:
  `2,0,2` a `14,13,14`, acorde a que un arbusto ocupa más espacio visual que
  un brote).
  `MomentoCuyBlocks.newBlock` enruta `plant_bush`/`plant_flowerbush` a esta
  clase; `isNonSolidPlant` también los marca sin colisión.
- **Ítem**: `item/generated` con `layer0` = su propia textura `side.png`,
  igual que brotes/flores.

## 4. Enredadera y musgo colgante (2 bloques): `VineBlock` real

`enredadera_celestial`, `musgo_suspendido`.

Las texturas son tiras delgadas de follaje colgante — el caso de uso exacto
del `VineBlock` de vanilla. Se creó `models/block/template_colgante.json`
(copia estructural de `minecraft:block/vine`: un solo plano delgado,
`ambientocclusion: false`, `shade: false`, UV completo) **sin
`tintindex`** — a diferencia de la enredadera de vanilla (que aplica el
color de bioma sobre la textura), aquí se omite el tinte a propósito para
que la textura pintada se vea con sus colores originales exactos, sin
teñirla de verde de bioma.

- `enredadera_celestial.json`/`musgo_suspendido.json` heredan esa plantilla
  con su propia textura `side.png`.
- **Blockstate**: `multipart` idéntico en estructura al `vine.json` de
  vanilla (una entrada por cada combinación de `north`/`east`/`south`/
  `west`/`up`, con las rotaciones `y`/`x` correspondientes, más el caso sin
  ninguna cara adyacente que cuelga sobre `north` por defecto).
- **Java**: `MomentoCuyBlocks.newBlock` instancia `net.minecraft.block.VineBlock`
  directamente (no una clase propia) para `plant_vine`/`plant_hangingmoss`.
  Se verificó por bytecode (`AbstractBlock`/`AbstractBlock$Settings`) que
  `hasRandomTicks()` sólo lee la bandera `Settings.randomTicks`, que es
  `false` salvo que se llame `.ticksRandomly()` explícitamente — como
  `baseSettings()` nunca llama ese método, estas dos enredaderas **no se
  expanden solas** ni tienen ningún tick aleatorio de crecimiento; son
  puramente decorativas, igual que se pidió.
- **Ítem**: `item/generated` con `layer0` = su propia textura `side.png`.

## 5. Barandilla de madera celestial: `FenceBlock` real con geometría propia

`barandilla_madera_celestial`.

**Por qué no se reutilizó la plantilla de valla de vanilla tal cual**: se
inspeccionaron `minecraft:block/fence_post.json` y `fence_side.json`
(extraídos del jar de Minecraft 1.21.1 cacheado por Loom). Ambos usan
regiones UV explícitas y pequeñas (por ejemplo `"uv": [6, 0, 10, 16]` en el
poste, `"uv": [7, 1, 9, 4]` en el travesaño) pensadas para una textura
**tileable** de tablón de madera, donde recortar un trozo de 4×16 píxeles
del tablón sigue siendo madera coherente en cualquier parte. Nuestra
textura (`side.png`/`side_alt.png`/`top.png`) es, en cambio, **una
ilustración completa** de la barandilla entera (3 postes + 2 travesaños
horizontales dibujados de corrido) — las mismas tres imágenes muestran la
misma composición completa. Aplicar las regiones UV de vanilla sobre esa
imagen habría recortado fragmentos arbitrarios de la ilustración en cada
cara del poste/travesaño, mostrando pedazos sin sentido en vez de la
barandilla reconocible.

**Solución aplicada**: se crearon plantillas propias con la misma geometría
cuboide que vanilla (mismas coordenadas `from`/`to` de poste y travesaños)
pero **sin ninguna región UV explícita** —
`models/block/template_barandilla_post.json` y
`template_barandilla_side.json`. Sin `"uv"`, Minecraft calcula
automáticamente el mapeo UV de cada cara a partir de sus coordenadas
proyectadas sobre el espacio de textura 0–16, es decir, cada cara muestra
una porción de la textura proporcional a su tamaño y posición reales en el
bloque, en vez de los recortes fijos que asume el tablón de vanilla. Esto
es la transformación técnica documentada que permite el encargo: **no se
inventó ningún arte nuevo**, se reutilizó `side.png` entera, sólo se dejó
que el motor recalculara el UV en vez de forzar los cortes de vanilla.

**Limitación documentada**: como la textura es una "imagen completa" de
toda la barandilla (3 postes + 2 barras) y no un material tileable, cada
cara individual del poste o del travesaño sigue mostrando un fragmento de
esa imagen completa (el que corresponde a su posición real, por el cálculo
automático de UV) — no la barandilla íntegra repetida en cada cara. No fue
posible mostrar la composición completa sin cortes en una superficie más
pequeña que la imagen entera, dado que la textura no fue diseñada como un
átlas por pieza; se avisa aquí en vez de sustituirla en silencio por una
textura genérica de vanilla.

Archivos:
- `template_barandilla_post.json` / `template_barandilla_side.json`:
  geometría (poste central 6,0,6–10,16,10; travesaños superior e inferior),
  variable de textura `#texture`, sin UV explícito.
- `template_barandilla_inventory.json`: versión 3D para inventario
  (postes izquierdo/derecho + travesaños completos a ambos lados, mismo
  patrón que `fence_inventory.json` de vanilla pero también sin UV
  explícito), con las transformaciones `display.gui`/`display.fixed`
  copiadas de vanilla para que se vea proporcionado en el slot.
- `barandilla_madera_celestial_post.json` / `_side.json` / `_inventory.json`:
  heredan las plantillas anteriores con `texture` = `.../side.png`.
- `blockstates/barandilla_madera_celestial.json`: `multipart` idéntico en
  estructura al `oak_fence.json` de vanilla (un poste siempre presente, un
  travesaño por cada vecino `north`/`east`/`south`/`west` con `uvlock` y la
  rotación `y` correspondiente).
- `models/item/barandilla_madera_celestial.json`: hereda el modelo de
  inventario 3D (ya no el modelo de bloque de cubo plano anterior).
- Se eliminó el `models/block/barandilla_madera_celestial.json` de cubo
  plano anterior: quedó huérfano, ningún blockstate lo referenciaba ya.

**Java**: `MomentoCuyBlocks.newBlock` instancia `net.minecraft.block.FenceBlock`
directamente para `build_woodrailing`. Es la clase real de vanilla — calcula
sus propios estados de conexión (`north`/`east`/`south`/`west`) comparando
con los bloques vecinos (se conecta a otra `FenceBlock`, a un bloque sólido
de cara completa, o a una puerta vecina, igual que cualquier valla
vanilla), y su colisión ya impide pasar a través de los postes/travesaños
— cumple la protección de caída pedida sin código adicional.

## 6. Barandilla de piedra ornamental: `WallBlock` real, reutilizando las plantillas de Mojang

`barandilla_piedra_ornamental`.

A diferencia de la valla, las plantillas de pared de vanilla
(`template_wall_post.json`, `template_wall_side.json`,
`template_wall_side_tall.json`) **no usan ninguna región UV explícita en
ninguna cara** (`"texture": "#wall"` sin `"uv"`) — ya hacen exactamente lo
que necesitábamos (mapeo automático, no recortes fijos de material
tileable). Por eso aquí **sí se reutilizaron las plantillas de Mojang
directamente** como `parent`, sin necesidad de duplicar geometría:

- `barandilla_piedra_ornamental_post.json` → `parent:
  "minecraft:block/template_wall_post"`.
- `barandilla_piedra_ornamental_side.json` → `parent:
  "minecraft:block/template_wall_side"`.
- `barandilla_piedra_ornamental_side_tall.json` → `parent:
  "minecraft:block/template_wall_side_tall"`.
- `barandilla_piedra_ornamental_inventory.json` → `parent:
  "minecraft:block/wall_inventory"`.

Las cuatro sólo redefinen la variable de textura `wall` → `.../side.png`
(la misma ilustración completa de barandilla, igual que en la de madera).
`blockstates/barandilla_piedra_ornamental.json` es el `multipart` completo
de `cobblestone_wall.json` de vanilla adaptado (poste cuando `up: true`,
lado bajo `low`/alto `tall` por cada una de las 4 direcciones) — reproduce
exactamente el sistema de conexiones de alturas que usa cualquier muro de
vanilla (se conecta más alto contra otra pared o un poste, más bajo contra
un bloque sólido normal).

**Java**: `net.minecraft.block.WallBlock` directamente para
`build_stonerailling`. Misma observación que la valla: altura de postes y
colisión reales de vanilla, suficiente para evitar caídas en bordes de
plataformas elevadas sin lógica adicional.

## 7. Revisión del resto de los 80 bloques (sin cambios)

Se revisaron explícitamente los demás diseños no cúbicos candidatos
pedidos en el encargo (vides, plantas decorativas, pétalos, ventanas
delgadas, rejas, puertas, trampillas, escaleras, losas, otras vallas,
elementos arquitectónicos especiales) contra la tabla real de 80 bloques en
`MomentoCuyBlocks.TABLE`:

- **No existen** en este paquete de 80 bloques diseños de puerta, trampilla,
  escalera, losa o reja — no hay nada de esas categorías que decidir en
  esta etapa.
- `suelo_de_petalos` (`soil_petals`): es una familia `soil_*` (piso), no una
  planta suelta — los pétalos están pintados como cobertura de un bloque de
  suelo caminable. Se deja cúbico a propósito; convertirlo en una plantita
  decorativa cambiaría su función de piso transitable.
- `bloque_cuerda_trenzada` (`build_rope`): familia `build_*` (material de
  construcción sólido), no un elemento delgado/colgante — se deja cúbico.
- `seto_jardin` (`plant_hedge`): **revisado y dejado cúbico a propósito**
  (ya documentado en el código de `MomentoCuyBlocks`): un seto recortado es
  conceptualmente una pared sólida de vegetación, no una planta suelta como
  un arbusto — cambiarlo a planos delgados lo volvería atravesable, lo que
  no es el comportamiento esperado de un seto de jardín.
- `ventana_cerezo` (`window_cherry`) y `ventana_cuarzo_lavanda`
  (`window_lavender`): **revisadas y dejadas cúbicas a propósito**. Su
  textura (`side.png`) muestra una ventana con marco y parteluz de cuatro
  paneles ya pintada sobre la cara completa — es un estilo de bloque
  decorativo sólido válido y común (igual que un `vidrio_tintado`
  enmarcado), no una ventana sin marco que necesite geometría de panel
  delgado. No había ambigüedad suficiente para forzar un cambio de
  geometría sin indicación adicional del equipo de construcción.

No se tocó ningún bloque de material sólido normal (tierra, roca, troncos,
tablones, cuarzo, cristal opaco, vidrio, bloques de minijuego) — todos
conservan `minecraft:block/cube` exactamente como estaban.

## 8. Archivos modificados/creados

**Java** (`momento-cuy/src/main/java/pe/apuyawar/momentocuy/block/`):
- `MomentoCuyPlantBlock.java` (nuevo): brotes y flores.
- `MomentoCuyBushBlock.java` (nuevo): arbustos, extiende la anterior.
- `MomentoCuyBlocks.java` (modificado): `newBlock()` nuevo método de
  despacho (antes era un `if`/`else` sólo para los bloques de minijuego),
  `isNonSolidPlant()` nuevo, e imports de `FenceBlock`/`VineBlock`/
  `WallBlock`.

**Assets** (`momento-cuy/src/main/resources/assets/momento_cuy/`), todos
reutilizando únicamente texturas ya existentes del paquete original, sin
generar ni modificar ninguna imagen:
- 4 modelos de bloque reescritos a `block/cross`: `brote_cerezo`,
  `brote_abeto_celestial`, `flor_estrella_azul`, `flor_solar_dorada`.
- `template_bush.json` (nuevo, 4 planos en ángulos válidos) +
  `arbusto_esmeralda.json`/`arbusto_florecido.json` reescritos para
  heredarlo.
- `template_colgante.json` (nuevo) +
  `enredadera_celestial.json`/`musgo_suspendido.json` reescritos para
  heredarlo, con sus blockstates `multipart` nuevos.
- `template_barandilla_post.json`/`_side.json`/`_inventory.json` (nuevos,
  geometría de valla sin UV explícito) +
  `barandilla_madera_celestial_post.json`/`_side.json`/`_inventory.json`
  (nuevos) + `blockstates/barandilla_madera_celestial.json` reescrito a
  `multipart`. Se eliminó el modelo de cubo plano anterior (huérfano).
- `barandilla_piedra_ornamental_post.json`/`_side.json`/`_side_tall.json`/
  `_inventory.json` (nuevos, heredan las plantillas de pared de Mojang) +
  `blockstates/barandilla_piedra_ornamental.json` reescrito a `multipart`.
  Se eliminó el modelo de cubo plano anterior (huérfano).
- 10 modelos de ítem reescritos a `minecraft:item/generated` (brotes,
  flores, arbustos, enredadera, musgo) o al nuevo modelo de inventario 3D
  (las dos barandillas).

## 9. Compatibilidad

- Mismo namespace (`momento_cuy`) e identificadores de bloque/ítem para los
  10 bloques tocados — ningún id cambió, sólo su modelo/blockstate/clase
  Java. Cualquier bloque ya colocado en un mundo de prueba sigue cargando
  con el mismo id.
- No se tocaron: acceso/salida de la dimensión Momento Cuy
  (`MomentoCuyCommands`/`MomentoCuyTeleport`/`MomentoCuyReturnPoints`), el
  Cuy Jardinero (entidad/render/IA), los otros 70 bloques, el sistema de
  minijuegos (`minigame/*`), el Panel K, la ruleta, las misiones ni Sol PE.
- `gradlew build` completo: éxito, incluyendo `apuyawar-systems:verifyJudgments`
  (128/135, sin cambios) y `apuyawar-world`/`momento-cuy` sin errores de
  compilación ni de recursos.

## 10. Pruebas realizadas y pendientes

**Realizadas en este entorno (sin cliente gráfico):**
- `gradlew :momento-cuy:compileJava` y `gradlew build` (proyecto completo):
  éxito.
- Validación de sintaxis JSON de los 38 archivos nuevos/modificados
  (modelos de bloque, modelos de ítem y blockstates) con un parser JSON
  independiente: sin errores.
- Verificación por bytecode (`javap` contra el Minecraft 1.21.1 cacheado por
  Loom) de que los ángulos de rotación usados en `template_bush.json` están
  dentro del conjunto válido que acepta
  `ModelElement$Deserializer.deserializeRotationAngle` (0, ±22.5, ±45) —
  esto evitó repetir, con una plantilla de modelo, el mismo tipo de crash
  por esquema inválido que ya se corrigió antes en el bioma de la
  dimensión.
- Revisión visual manual (abriendo cada PNG) de las texturas de barandillas,
  brotes, flores, arbustos, ventanas, seto y enredadera/musgo antes de
  decidir su geometría — no se asumió la forma de ninguna textura.

**Pendientes de probar en Minecraft (requieren cliente gráfico, no
disponible en este entorno) — lista de pruebas sugerida para el equipo:**
1. Colocar cada brote/flor/arbusto y confirmar que no se ven como cajas
   planas, que tienen transparencia cutout correcta (sin halo negro) y que
   el ícono de inventario es reconocible.
2. Colocar `enredadera_celestial`/`musgo_suspendido` contra una pared y
   contra el techo; confirmar que no se expanden solas con el tiempo.
3. Barandilla de madera: 1 bloque suelto, 2 conectados, 5 en línea recta,
   una esquina de 90°, una T, un cruce de 4 direcciones, y una conexión
   contra un bloque sólido normal — confirmar que el modelo se actualiza
   solo al colocar/romper un vecino, y que el jugador no puede atravesar la
   barandilla ni caer por el hueco de los travesaños.
4. Lo mismo para la barandilla de piedra, además de confirmar la altura de
   protección (bajo vs. alto según con qué se conecte).
5. Buscar los 10 bloques por nombre en el inventario creativo y confirmar
   que ninguno muestra la textura de error de Minecraft.
6. Confirmar que ningún bloque de material sólido normal (tierra, roca,
   madera, cuarzo, cristal, vidrio) cambió de apariencia.
7. Confirmar que el Cuy Jardinero y el acceso/salida de la dimensión siguen
   funcionando igual que antes de esta rama.

## 11. Problemas conocidos

- La "imagen completa" de las texturas de ambas barandillas (3 postes + 2
  travesaños dibujados de corrido) no puede mostrarse íntegra y sin cortes
  en la cara individual de un solo poste o travesaño — ver la limitación
  documentada en la sección 5. Si el equipo de arte quiere que cada poste
  muestre la ilustración completa sin fragmentar, haría falta una textura
  nueva recortada por pieza (fuera del alcance de esta corrección, que sólo
  reutiliza las imágenes ya existentes).
- `ventana_cerezo`/`ventana_cuarzo_lavanda`/`seto_jardin` quedan como cubos
  sólidos por decisión documentada (sección 7); si el equipo prefiere
  geometría de panel/placa para las ventanas o un seto atravesable, es un
  cambio de diseño a decidir explícitamente, no una corrección de un bug.
