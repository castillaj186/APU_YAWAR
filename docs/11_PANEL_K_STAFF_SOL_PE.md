# Panel K, Staff y moneda Sol PE — qué se implementó

Fecha: 13 de octubre de 2026.

**Estado: compila y construye limpio (`gradlew build`, `verifyJudgments` en verde),
pero NADA de esto fue probado visualmente en una ventana de Minecraft todavía.**
Solo hay evidencia de compilación correcta, no de funcionamiento real en juego.
No se tocó ningún archivo de la ruleta (`RouletteRenderer`, `RouletteLighting`,
`RouletteTimeline`, `RouletteDraw`, `RoulettePalette`, `RouletteFillSequence`) ni
sus sonidos — se verificó con `verifyJudgments` tras cada compilación.

## Qué se pidió

Un "Centro de Control" completo abierto con la tecla **K**, con dos variantes:

- **Panel de supervivencia** (cualquier jugador): 4 botones — Mobs/Cambios (libro),
  Objetos (espada), Misiones (pergamino, pantalla aparte), Búsqueda (lupa).
- **Panel administrativo** (solo Staff registrado Y en modo Creative en ese momento):
  jugadores, anuncios globales, control de ruleta, economía, eventos, staff,
  herramientas.

Más una moneda real **Sol PE** (ítem registrado) y un sistema de rango **Staff**
persistente, separado de tener simplemente el modo Creative.

## Investigación previa (antes de escribir nada)

Se usó un subagente de solo-lectura para mapear la arquitectura real del proyecto
antes de tocar código, y así no duplicar ni romper nada. Conclusiones clave:

- Modid real: `apuyawar_systems` (constante `ApuYawarSystems.MOD_ID`).
- Ya existía un patrón de red (`CustomPayload` + `PacketCodec`) en
  `pe.apuyawar.systems.network` (`RouletteStartPayload`, `IntiStartPayload`, etc.) —
  se replicó exactamente para los paquetes nuevos.
- **No existía ningún sistema previo** de: persistencia de datos entre reinicios
  (ni `PersistentState` ni NBT propio — solo `WorldRuleJournal`, que es un diario
  de rollback de gamerules, no un sistema genérico), ítems custom, `Screen`/GUI,
  `KeyBinding`, misiones, economía, ni roles/staff. Todo esto se construyó desde
  cero siguiendo el estilo del proyecto (clases `XxxEvents`/`XxxCommands` con
  `initialize()` estático, llamadas desde `ApuYawarSystems.onInitialize()`).
- Las 6 imágenes en `assets-fuente/panel_K/`: `panel.png` (1672×941),
  `libro.png`/`espada.png`/`pergamino.png`/`lupa.png`/`solPE.png` (1254×1254 cada
  una, fondo transparente).

Varias firmas de API de Minecraft 1.21.1 (`PersistentState.Type`,
`PersistentStateManager.getOrCreate`, `PacketCodec.of`/`PacketCodecs`,
`DrawContext.drawItem`/`enableScissor`, `ButtonWidget.builder`,
`ServerPlayerEntity.teleport`, `SpawnEggItem.getAll`) se verificaron leyendo el
bytecode real del jar remapeado de Minecraft en la caché de Loom, no adivinadas,
para no perder ciclos de compilación.

## Archivos nuevos

### Texturas y recursos
- `textures/gui/panel_k/panel.png`, `icon_mobs.png`, `icon_items.png`,
  `icon_missions.png`, `icon_search.png` (copias de `assets-fuente/panel_K/`).
- `textures/item/sol_pe.png`, `models/item/sol_pe.json`.
- `lang/en_us.json`, `lang/es_es.json` (nombre del ítem y de la tecla).

### Red (`pe.apuyawar.systems.network`)
`PanelOpenRequestPayload` (C2S), `PanelOpenResponsePayload` (S2C),
`MissionsRequestPayload` (C2S), `MissionsSyncPayload` (S2C),
`AdminAnnouncePayload`, `AdminGiveSolPePayload`, `AdminTeleportToPayload`,
`AdminRouletteForcePayload` (C2S, acciones administrativas).

### Ítem (`pe.apuyawar.systems.items`)
`ModItems.java` — registra Sol PE, apilable hasta 64, visible en la pestaña
creativa de ingredientes.

### Staff (`pe.apuyawar.systems.staff`)
- `StaffState.java` — `PersistentState` vanilla (NBT, guardado en el nivel del
  mundo); un `Set<UUID>` de jugadores Staff. Autoritativo en servidor.
- `StaffCommands.java` — `/apuyawar staff dar|quitar|lista|info <jugador>`.
  `dar`/`quitar` requieren nivel de permiso 4 (el más alto); cambian el gamemode
  real del jugador. Solo funcionan con el jugador **conectado** (se resuelve su
  UUID real de la sesión activa) — no hay todavía asignación offline por nombre.

### Misiones (`pe.apuyawar.systems.missions`)
- `MissionDefinition.java`, `MissionCatalog.java` — **2 misiones reales**, no
  inventadas en cantidad: "Bienvenida a APU YAWAR" (conectarte 1 vez, 5 Sol PE) y
  "Visitante Frecuente" (conectarte 5 veces, 15 Sol PE). Ligadas al mismo evento
  `ServerPlayConnectionEvents.JOIN` que ya usa `JudgmentEvents`.
- `MissionState.java` — progreso persistente por jugador (`PersistentState`),
  con un registro de "ya recompensado" para que la recompensa nunca se entregue
  dos veces.
- `MissionEvents.java` — al cumplirse una misión, entrega Sol PE real vía
  `player.giveItemStack(...)` (reparte al inventario o lo suelta al piso si está
  lleno; nunca se pierde ni se duplica).

### Panel — servidor (`pe.apuyawar.systems.panel`)
`PanelEvents.java` — registra los 6 paquetes C2S y, para cada uno, **vuelve a
verificar Staff + Creative en el servidor** antes de ejecutar nada (anunciar,
dar Sol PE, teletransportar, forzar la ruleta). Un cliente modificado no puede
saltarse esto ni abriendo la pantalla ni mandando el paquete directamente.

### Panel — cliente (`pe.apuyawar.systems.panel.client`)
- `PanelKeyBindings.java` — tecla K (configurable desde Opciones > Controles,
  categoría "APU YAWAR"); al presionarla pide al servidor qué panel corresponde
  (nunca decide localmente).
- `PanelTextures.java`, `PanelLayout.java` — rutas de textura y las 4 posiciones
  de los botones sobre `panel.png`, como fracciones del tamaño del panel
  (estimadas visualmente — ver "Riesgos" abajo).
- `SurvivalPanelScreen.java` — fondo + 4 botones + contenido de Mobs/Objetos/
  Búsqueda en la zona derecha; "Misiones" abre `MissionsScreen` aparte.
- `MissionsScreen.java` — lista + detalle, progreso real sincronizado del
  servidor.
- `AdminPanelScreen.java` — secciones Jugadores (teletransporte),
  Anuncios (funcional, difunde por chat a todo el servidor), Control de Ruleta
  (reutiliza `ApuYawarSystems.start(...)`, el mismo método que ya usa el comando
  `/apuyawar_ruleta forzar` — no se reescribió nada de la ruleta), Economía
  (dar Sol PE). Eventos/Staff/Herramientas se muestran explícitamente como
  **"No disponible"** porque no hay sistemas reales que conectar todavía.
- `MobCatalog.java` — todas las criaturas con huevo de generación vanilla
  (eso ya excluye al Ender Dragon y al Wither, que no tienen huevo; además se
  filtran por nombre explícitamente) + 16 espacios "?" de contenido futuro.
- `ItemCatalog.java` — todos los ítems registrados (no hay ninguna regla previa
  de desbloqueo progresivo que conectar — se documenta como pendiente) + 16
  espacios "?".
- `CatalogEntry.java`, `CatalogGrid.java` — cuadrícula con scroll y selección,
  compartida entre Mobs y Objetos para no duplicar código.

## Archivos modificados
- `ApuYawarSystems.java` — registra ítem, comandos de Staff, eventos del panel y
  de misiones, y los paquetes S2C nuevos.
- `ApuYawarClient.java` — registra la tecla K, el tick del keybinding, y los
  receptores de `PanelOpenResponsePayload`/`MissionsSyncPayload`.

## Decisiones y simplificaciones (para que no se tomen por sorpresas)

1. **Desbloqueo progresivo de Objetos**: no existía ninguna regla definida en el
   proyecto; por ahora se muestran todos los ítems como disponibles. El código
   (`CatalogEntry`/`CatalogGrid`) ya está listo para conectar un estado de
   desbloqueo real en cuanto se defina esa regla — no se inventó ninguna.
2. **Glifo "?"**: es texto dibujado por código (`drawText`), no un PNG de pixel
   art dedicado — permitido explícitamente, pero avisar si se prefiere un PNG real.
3. **Búsqueda de recetas**: usa el `RecipeManager` real del cliente, pero
   muestra los ingredientes como una lista de texto, no una grilla 3×3 visual.
4. **Panel administrativo**: dibujado enteramente desde Java (no se fuerza nada
   dentro de los 4 espacios de `panel.png`, que son del panel de supervivencia).
5. **Staff offline**: `dar`/`quitar` solo funcionan con el jugador conectado en
   ese momento (se resuelve su UUID real de la sesión); no hay todavía soporte
   para asignar el rango a alguien desconectado por nombre/UUID crudo.

## Riesgo principal a verificar primero

Las 4 posiciones de botones en `PanelLayout.java` (constantes `SLOT_MOBS`,
`SLOT_ITEMS`, `SLOT_MISSIONS`, `SLOT_SEARCH`) se estimaron mirando la imagen
`panel.png`, no se midieron con una herramienta de precisión de píxeles. Es lo
más probable que necesite un ajuste fino tras verlo en el juego — es un único
archivo pequeño si hay que corregirlo.

## Validación técnica hecha

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # limpio
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments`: 128 objetivos, 135 catálogos, sigue en verde — la ruleta no
se vio afectada.

## Prueba manual pendiente (nada de esto se verificó visualmente)

```mcfunction
/apuyawar staff dar <tu_nombre>     # te vuelve Staff y te cambia a Creative
K                                    # debería abrir el panel administrativo
/gamemode survival
K                                    # debería mostrar el panel de 4 botones (no el admin)
/apuyawar staff quitar <tu_nombre>  # revoca el rango y te regresa a Survival
```

Buscar "Sol PE" en el inventario creativo; probar la pestaña Economía del panel
administrativo para dar unidades; verificar que el scroll y la selección de las
cuadrículas de Mobs/Objetos respondan al mouse; probar el buscador de recetas
escribiendo el nombre de un ítem craftable.
