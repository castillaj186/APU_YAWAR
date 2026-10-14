# Momento Cuy — nuevo módulo, dimensión, 80 bloques y el Cuy Jardinero

Fecha: 14 de octubre de 2026. Rama: `feat/momento-cuy-dimension`, basada en
`main` (esta etapa es independiente del Panel K y sus ramas sin fusionar
todavía — no las incluye ni las toca). No se hizo merge a `main`.

**Estado: compila el módulo nuevo y el proyecto completo
(`gradlew clean build`), `verifyJudgments` de `apuyawar-systems` sigue en
verde (128/135, sin cambios — no se tocó ese módulo). Sin confirmación visual
ni multijugador en Minecraft todavía — no hay cliente gráfico en este
entorno.** No se modificó la ruleta, el Panel K, sus recetas, ni el sistema
de misiones.

## 1. Materiales estudiados antes de implementar

- `assets-fuente/safio-content-antiguo/safio_content-0.3.0.jar`: el único
  material disponible localmente del mod antiguo era el JAR **compilado**
  (sin código fuente `.java`). Se **desensambló y decompiló** con Vineflower
  — primero contra el bytecode tal cual (nombres intermediary de Fabric,
  `class_1657`, `method_5992`, etc., ilegibles) y luego se **remapeó el JAR
  completo con tiny-remapper** usando el mismo archivo de mappings Yarn
  1.21.1 que ya usa este proyecto (`net.fabricmc.yarn.1_21_1.1.21.1+build.3`),
  para obtener nombres reales (`PlayerEntity`, `interactMob`,
  `getBlockPos`, etc.) antes de adaptar nada — no se adivinó ninguna firma ni
  se copiaron clases descompiladas a ciegas, tal como se pidió.
- `assets-fuente/bloques/Momento_Cuy_80_Bloques/`: paquete de 80 bloques con
  blockstates, modelos de bloque/ítem y 320 texturas (32×32), más
  `INDICE_80_BLOQUES.csv` (id, nombre, familia de diseño, transparencia) y
  `LEEME.txt`/`COMPROBACION.txt`.
- El enlace de GitHub del mod antiguo no se consultó por separado: el JAR
  local ya contenía las clases exactas que pedía el encargo
  (`CuyJardineroEntity`, `CuyModel`, `CuyRenderer`, los 5 goals/controladores,
  y también `.bbmodel`/textura reales como recursos empaquetados), así que
  decompilar el JAR fue la fuente primaria y suficiente.

## 2. Arquitectura del nuevo módulo

Módulo Gradle nuevo `momento-cuy` (añadido a `settings.gradle` junto a
`apuyawar-systems`/`apuyawar-world`), mismo `build.gradle` mínimo que
`apuyawar-world` (Loom + Minecraft/Yarn/Fabric API de `gradle.properties`,
Java 21). Mod id `momento_cuy`. Paquetes:

```
pe.apuyawar.momentocuy
├── MomentoCuy.java              (ModInitializer: registra todo, eventos de servidor)
├── MomentoCuyCommands.java      (/momentocuy ...)
├── block/                       (80 bloques + 6 clases de comportamiento)
├── client/                      (MomentoCuyClient, CuyModel, CuyRenderer)
├── dimension/                   (identidad de dimensión, lobby, puntos de retorno, teletransporte)
├── entity/                      (Cuy Jardinero + sus 5 goals/controladores)
└── minigame/                    (arenas, partidas, participantes — motor reutilizable)
```

No se referencia `safio-content` desde ningún `build.gradle` ni código — es
una implementación nueva, no una dependencia del mod antiguo.

## 3. Los 80 bloques

`MomentoCuyBlocks.java` registra los 80 a partir de una tabla de datos
(`{id, nombre, familia de diseño, ¿transparente?}`) transcrita directamente
de `INDICE_80_BLOQUES.csv` — igual que `MobCatalog` en el Panel K prioriza
una enumeración controlada. Los blockstates/modelos/texturas JSON del
paquete ya usaban el namespace `momento_cuy`, así que se copiaron **sin
modificar los PNG originales**, tal como se pidió.

Por cada fila, el código decide:
- **Clase de bloque**: `Block` simple para la mayoría; `PillarBlock` para
  troncos/vigas/el pilar de cuarzo (rotan por eje como un tronco vanilla);
  una clase de comportamiento dedicada para 6 de los 7 bloques de minijuego
  (ver sección 5) — nunca se asumió que el JSON ya traía esa lógica.
- **Capa de render** (`BlockRenderLayerMap`, registrado desde
  `MomentoCuyClient`, lado cliente): sólido por defecto; **cutout** para
  hojas/brotes/plantas/barandillas; **translúcido** (alfa real) para vidrios,
  el cristal traslúcido, las ventanas y el portal — así los 92 PNG con algún
  píxel transparente (`COMPROBACION.txt`) se muestran de verdad, no como
  bloques opacos con bordes negros.
- **Dureza y sonido** agrupados por familia (tierra/plantas blandas, piedra/
  cuarzo/construcción firmes, cristal más resistente, bloques de minijuego
  firmes) — un valor razonable por grupo, no inventado bloque por bloque.

Los 80 tienen ítem (`BlockItem`) y aparecen en una pestaña Creative propia
("Momento Cuy"). Nombres traducibles en `lang/es_es.json` y `lang/en_us.json`
(mismo texto en español en ambos — no se inventó una traducción al inglés
que no se pidió).

### Bloques especiales (sección 4 del encargo)

| Bloque | Clase | Comportamiento real |
|---|---|---|
| `plataforma_temporal` | `TemporaryPlatformBlock` | Se convierte en aire ~1s después de que algo se para encima (`AbstractBlock.onLandedUpon` + `WorldAccess.scheduleBlockTick` — el mismo mecanismo real que usa vanilla para la nieve en polvo/farmland, confirmado por bytecode). |
| `bloque_rebotador_rosa` | `BouncePadBlock` | Impulso vertical al aterrizar (`AbstractBlock.onEntityLand` — el mismo método que usa `SlimeBlock`, confirmado por bytecode). |
| `baldosa_activadora` | `TriggerTileBlock` | Avisa a `MinigameTriggers` al pisarse; no decide ninguna regla. |
| `bloque_checkpoint` | `CheckpointBlock` | Avisa a `MinigameTriggers`; el motor de minijuegos decide si cuenta. |
| `bloque_meta` | `FinishLineBlock` | Igual, para el evento de meta. |
| `portal_momento_cuy` | `MomentoCuyPortalBlock` | Clic derecho teletransporta (misma lógica que `/momentocuy entrar`/`salir`, vía `MomentoCuyTeleport` — nunca dos caminos separados). No es un marco multibloque al estilo Nether: eso habría sido inventar una mecánica de detección de estructura que no se pidió. |
| `plataforma_segura` | `Block` simple | Decorativo por ahora; marca visualmente una zona segura sin comportamiento propio todavía. |

## 4. La dimensión

Totalmente data-driven (`data/momento_cuy/...`), sin ningún generador de
terreno automático — exactamente lo que pedía el encargo ("no construyas
automáticamente un mapa gigante"):

- `dimension_type/momento_cuy.json`: cielo visible, sin techo, rango vertical
  completo (-64 a 320) para que el constructor tenga toda la altura posible
  para sus islas.
- `dimension/momento_cuy.json`: generador **`minecraft:flat`** con
  `"features": false` y una sola capa de bedrock en el fondo absoluto
  (y=-64) — el resto del mundo es aire real (vacío), no terreno de overworld
  generado automáticamente. El bioma (`cielo_momento_cuy.json`) solo aporta
  colores de cielo/niebla/agua celestiales; sin precipitación, sin
  features/carvers (coherente con que el generador plano ya los ignora).

### Zona segura de aparición

`MomentoCuyLobby.java` construye **una plataforma pequeña (radio 7, no una
ciudad)** centrada en (0, 100, 0) la primera vez que alguien entra —
comprobando un bloque marcador antes de construir, exactamente el patrón ya
probado del mod antiguo (`NutriaLobby`: nunca reconstruye si el marcador ya
existe). Incluye una barandilla en el borde y coloca un Cuy Jardinero.

### Entrada, salida y protección contra el vacío

`MomentoCuyTeleport.java` centraliza la lógica (usada tanto por
`/momentocuy entrar`/`salir` como por el bloque portal):
1. **Antes de teletransportar**, construye la plataforma si falta y verifica
   que haya suelo sólido justo debajo del punto de entrada
   (`MomentoCuyLobby.hasSafeGroundAtEntry`). Si no lo hay, **se cancela el
   teletransporte** en vez de mandar a alguien a caer — la protección contra
   el vacío pedida explícitamente.
2. `MomentoCuyReturnPoints.java` (adaptado de `ReturnPoints` del mod
   antiguo) guarda dónde estaba cada jugador, en disco (escritura atómica:
   archivo temporal + renombrado, para no corromperse si el servidor se cae
   a mitad de guardar) — sobrevive un reinicio con alguien todavía dentro.
3. Al salir, si no hay punto de retorno guardado (ej. alguien entró por
   teletransporte de operador sin pasar por `entrar`), vuelve al spawn del
   Overworld en vez de negarse a actuar.

### Acceso sin permisos de operador

`/momentocuy entrar` y `/momentocuy salir` **no exigen ningún nivel de
permiso** — cualquier jugador puede usarlos. Esto es un cambio deliberado
respecto al mod antiguo, donde **todo** el comando raíz exigía nivel 2; se
cambió porque el encargo lo pidió explícitamente. Las herramientas
administrativas de prueba (`lobby`, `cuy colocar`, `arena`, `minijuego`) sí
exigen nivel 2, nodo por nodo.

## 5. El Cuy Jardinero

Adaptado de `CuyJardineroEntity`/`CuyModel`/`CuyRenderer`/`CuyGardenGoal`/
`CuyHarvestGoal`/`CuyShelterGoal`/`CuyPauseGoal`/`CuyGateController`/
`CuyFarmController`/`CuyTradeCatalog` del mod antiguo (ver sección 1 sobre
cómo se recuperaron) — **misma lógica de comportamiento, sin reescribirla**:

- **Apariencia y animación**: `CuyModel.java` carga el `.bbmodel` de
  Blockbench **directamente como JSON en tiempo de ejecución** (grupos →
  huesos, elementos → cubos, `animators` → curvas de posición/rotación/
  escala por fotograma clave) y dibuja cada cubo a mano con un
  `VertexConsumer` — **sin GeckoLib ni ninguna otra librería de animación
  externa**, autocontenido, exactamente como en el original. El `.bbmodel`
  (161 KB) y la textura (`cuy_jardinero.png`) son los archivos reales del mod
  antiguo, copiados sin modificar.
- **Movimiento/animación en pantalla** (`CuyRenderer.java`): elige entre
  `idle_biped`/`walk_biped`/`run_quadruped`/`jump_biped`/`run_jump`/`fall`/
  `land`/transiciones bípedo↔cuadrúpedo según la velocidad y si está en el
  suelo, más las animaciones de acción (`wave_hello`, `gardening`,
  `water_plants`, `sniff_flower`, `inspect_crop`, `basket_check`,
  `sit_rest`, `garden_proud`) cuando el Cuy está "ocupado".
- **Agricultura/cosecha**: `CuyGardenGoal` (se acerca a cultivos/flores
  cercanos e interactúa), `CuyHarvestGoal` (cosecha trigo/zanahoria/papa/
  remolacha maduros cerca de su compostador asignado, reservando semilla
  para replantar — nunca agota el cultivo).
- **Canasta**: `CuyFarmController` — inventario de 27 espacios, abre como un
  cofre genérico (agachado + clic con cofre en mano), compostaje automático
  de semillas sobrantes.
- **Comercio**: `CuyTradeCatalog` — ofertas reales de trueque (papa/trigo/
  calabaza/zanahoria/melón por Esmeraldas y viceversa) que se desbloquean por
  nivel de experiencia (Novato→Maestro), con reposición diaria si hay un
  compostador cerca.
- **Refugio y puertas**: `CuyShelterGoal` busca un portón válido con refugio
  techado cerca de noche o huyendo; `CuyGateController` abre/cierra puertas y
  portones **reales** del mundo (no simulado) de forma segura (nunca fuerza
  una puerta con redstone/cerrada por otro).
- **Comportamientos autónomos**: cuando no está ocupado ni huyendo, elige
  cada tanto una acción ambiental (saludar, estar feliz, revisar la canasta,
  descansar, estar orgulloso del jardín).

### Única adaptación deliberada (no una simplificación forzada)

El mod antiguo tenía una clase base `OtterEntity` compartida por **11**
criaturas decorativas (nutrias, gatos, lobos, conejos, etc.); de esas, el
encargo solo pidió recuperar al Cuy Jardinero. Mantener una jerarquía de una
sola clase habría sido código muerto, así que los objetivos base de animal
(nadar, escapar del peligro, apareamiento, deambular, mirar alrededor) se
fusionaron directamente en `CuyJardineroEntity`. Única diferencia de
comportamiento real: el ítem de tentación/apareamiento cambió de pescado
(lógico para una nutria) a **trigo/zanahoria** (más fiel a un cuy herbívoro
de jardín) — el resto es comportamiento idéntico al original.

El Cuy funciona dentro de Momento Cuy sin depender del mod antiguo instalado
(no hay ninguna referencia a `safio_content` en el código ni en los
recursos).

## 6. Motor de minijuegos (preparado, no implementado)

Paquete `pe.apuyawar.momentocuy.minigame`, sin ninguna regla de victoria/
derrota concreta todavía (a propósito — el encargo pidió explícitamente NO
inventar reglas que el equipo no ha decidido):

- `Arena` (record): id, dos esquinas de región, punto de inicio, lista de
  checkpoints, meta.
- `ArenaRegistry`: arenas registradas en memoria para la sesión actual
  (ver limitaciones, sección 9).
- `MinigameParticipant`: progreso por jugador (checkpoint alcanzado,
  terminado, conectado) — se conserva (no se borra) al desconectarse.
- `MinigameMatch`: una partida sobre una arena — estado real (`MatchState`:
  `WAITING`/`RUNNING`/`FINISHED`/`CANCELLED`), tiempo en ticks del mundo,
  `finish()`/`cancel()` devuelven a TODOS los participantes conectados a la
  plataforma de llegada.
- `MinigameManager`: conecta los eventos de `MinigameTriggers` (bloques de
  checkpoint/meta pisados) con la partida activa de la arena correspondiente;
  gestiona desconexión (marca al participante, no lo elimina) y reconexión
  (si tenía una partida activa, se le avisa en qué checkpoint se había
  quedado — pedido explícitamente: "recuperar a jugadores desconectados").

Comandos de prueba: `/momentocuy arena registrar|checkpoint|meta|lista`,
`/momentocuy minijuego iniciar|finalizar|cancelar`.

Los 80 bloques (especialmente los 7 de minijuego) ya están listos para
usarse en arenas futuras sin tocar código — el constructor solo necesita
colocarlos y registrar la arena con los comandos.

## 7. Protección de construcciones — qué coordenadas usa este mod

**Este mod SOLO construye automáticamente una cosa**: la plataforma de
llegada, radio 7 alrededor de (0, 100, 0), y solo la primera vez (marcador en
(0, 98, 0) con `cuarzo_cincelado_cuy`; nunca se reconstruye si ya existe).
**Ninguna otra coordenada de la dimensión es tocada por código automático.**

Guía para el constructor del equipo:
- Evitar construir dentro del radio 7 alrededor de (0, 100, 0) y debajo de
  y=100 ahí mismo (es la zona de la plataforma y su marcador) — todo lo
  demás, en cualquier coordenada, es libre.
- El mundo es vacío (void) salvo un piso de bedrock en y=-64: no hay ningún
  terreno generado automáticamente que limpiar ni evitar.
- Para registrar una arena de minijuego sin tocar código: pararse en el
  centro deseado y `/momentocuy arena registrar <id> <radio>` (crea la
  región), luego pararse en cada checkpoint y `/momentocuy arena checkpoint
  <id>`, y en la meta `/momentocuy arena meta <id>`.
- Los bloques colocados a mano (incluida la propia plataforma del lobby) son
  bloques reales del mundo: persisten entre reinicios como cualquier
  construcción vanilla, sin ningún código adicional de este mod.

## 8. Integración con el resto de APU YAWAR

No se tocó `apuyawar-systems`, `apuyawar-world`, `apuyawar-core`, el Panel K,
la ruleta, ni Sol PE — confirmado por `git diff` (solo archivos nuevos bajo
`momento-cuy/`, más `settings.gradle` con el módulo añadido). Identificadores
(`momento_cuy` para todo: bloques, entidad, ítems, dimensión) no colisionan
con `apuyawar_systems`/`apuyawar_world`. `verifyJudgments` de
`apuyawar-systems` se ejecutó tras el build completo y sigue en verde.

## 9. Pruebas realizadas y limitaciones conocidas

```powershell
.\gradlew.bat :momento-cuy:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat clean build                 # BUILD SUCCESSFUL (3 módulos)
```

Verificado:
- Los 644 archivos de recursos de bloques (80 blockstates + 80 modelos de
  bloque + 80 modelos de ítem + 320 texturas + carpetas) están en el JAR
  final, igual que el `.bbmodel`/textura del Cuy y los 3 JSON de dimensión/
  bioma — confirmado inspeccionando el JAR con `jar tf`, no asumido.
- `verifyJudgments` de `apuyawar-systems` sigue en verde tras el build
  completo (128 objetivos, 135 catálogos) — este módulo no afectó al resto.
- Cada firma de API usada (`AbstractBlock.onLandedUpon`/`onEntityLand`/
  `scheduledTick`, `WorldAccess.scheduleBlockTick`, `BlockRenderLayerMap`,
  `MapColor`, etc.) se confirmó con `javap` contra el jar real de Minecraft
  1.21.1 antes de escribir el código que la usa.

**Pendiente — no se puede confirmar sin cliente gráfico ni servidor
multijugador real en este entorno:**
1. Que los 80 bloques se vean con su textura correcta (ninguno en negro/
   morado) y que la transparencia de vidrios/hojas/portal se muestre bien.
2. Que la dimensión cargue y se genere como un vacío con la plataforma —
   confirmar con `/momentocuy lobby estado` tras entrar.
3. Que `/momentocuy entrar`/`salir` funcionen sin permisos de operador para
   un jugador normal, y que el punto de retorno sea exacto.
4. Que sobrevivir un reinicio del servidor con alguien dentro no lo pierda.
5. Que el Cuy Jardinero se vea con su modelo/textura reales (no un cubo
   genérico) y que sus animaciones (idle/caminar/correr a cuatro patas/
   saltar/aterrizar/acciones) se reproduzcan sin errores en el log.
6. Que la agricultura, cosecha, canasta, comercio y refugio funcionen de
   verdad en el juego (la lógica se adaptó con cuidado, pero nunca se
   ejecutó en un mundo real).
7. Pruebas multijugador reales de desconexión/reconexión durante una
   partida — `MinigameManager` está escrito para manejarlo, pero no hay
   forma de probar dos clientes reales en este entorno.
8. Persistencia de las arenas (`ArenaRegistry`) entre reinicios: **limitación
   conocida, no implementada** — son solo datos en memoria por sesión; las
   construcciones físicas (bloques) sí persisten, pero el registro
   "estas coordenadas son la arena X" se pierde al reiniciar y debe
   recrearse con el comando. Documentado aquí a propósito, no se asumió
   persistencia que no existe.
9. La "plataforma segura" (`plataforma_segura`) es decorativa — no tiene
   comportamiento propio todavía (no se inventó uno sin que el equipo
   pidiera cuál).

No se declara nada de esto validado hasta que el usuario lo confirme en
Minecraft real.
