# Panel K — catálogo de Mobs con miniaturas 3D reales

Fecha: 14 de octubre de 2026. Rama: `feat/panel-k-mobs-3d` (basada en la versión
estable y ya confirmada nítida de `fix/panel-k-nitidez`, PR aparte — no se tocó
esa corrección ni se mezcló con `main`).

**Estado: compila y construye limpio (`gradlew clean build`), `verifyJudgments`
en verde (128 objetivos, 135 catálogos, sin cambios). Sin confirmación visual
en Minecraft todavía — no hay cliente gráfico en este entorno.** No se tocó la
ruleta, Inti, Staff, Sol PE, misiones, economía, networking, persistencia,
`panel.png`/`.mcmeta`, `ViewportFix`, `PanelScale`, ni el diseño/orden de los 4
botones. La pestaña Objetos no cambió de comportamiento.

## 1. Problema original

La pestaña "Cambios y Mobs" mostraba el huevo de generación (`SpawnEggItem`) de
cada criatura como ítem 2D, en la misma cuadrícula y con el mismo renderizador
(`DrawContext.drawItem`) que la pestaña Objetos. Un huevo es un ítem para crear
una entidad, no la criatura misma, y esa arquitectura (`CatalogEntry(ItemStack,
String)`) no tiene ningún camino para dibujar un modelo 3D real. Se pidió
sustituir el huevo por una miniatura 3D reconocible de la criatura real
(modelo y texturas Vanilla, sin PNG nuevos), sin romper el catálogo de Objetos
ni la nitidez ya confirmada del resto del Panel K.

## 2. Arquitectura implementada

### Separación de modelos de datos (sin tocar Objetos)

- `CatalogEntry(ItemStack icon, String name)` — **sin cambios**, sigue siendo
  solo para Objetos.
- `MobCatalogEntry(EntityType<? extends MobEntity> entityType, Text displayName,
  Category category, float scaleMultiplier, float verticalOffset)` — nuevo
  record, conserva la identidad real del tipo de entidad en vez de envolverla
  en un `ItemStack`. El acotamiento a `EntityType<? extends MobEntity>` excluye
  en tiempo de COMPILACIÓN cualquier cosa que no sea una criatura real
  (proyectiles, vehículos, decoración, el jugador) — no solo por convención de
  código, sino porque el compilador rechaza cualquier intento de añadir algo
  que no sea un `MobEntity`.
- `category` (PASIVA/NEUTRAL/HOSTIL) es informativa, pensada para un futuro
  orden o filtro; no afecta el renderizado ni ningún desbloqueo todavía — no
  se inventó lógica que no se pidió.
- No existe un campo de "desbloqueo" individual en `MobCatalogEntry`: igual que
  `ItemCatalog` (ver `11_PANEL_K_STAFF_SOL_PE.md`, decisión documentada ya
  entonces), todas las entradas reales se muestran disponibles, y el contenido
  bloqueado sigue siendo los espacios "?" (`FUTURE_SLOTS`) que ya gestionaba
  `CatalogGrid` — añadir un segundo concepto de bloqueo redundante no estaba
  pedido y habría sido inventar un sistema a medias.

### `CatalogGrid` generificado, no duplicado

`CatalogGrid<T>` pasó de operar solo sobre `CatalogEntry` a ser genérico sobre
el tipo de entrada, recibiendo una estrategia `CellRenderer<T>` (dibuja
únicamente el contenido de una celda normal; fondo, hover, selección y los
espacios "?" los sigue dibujando la propia cuadrícula, sin cambios) y un
`Function<T,String>` para el nombre que se muestra al seleccionar. Esto evita
duplicar la gestión de columnas, scroll, hover y selección entre Objetos y
Mobs (lo que pedía explícitamente el encargo), mientras el renderizado —
fundamentalmente distinto entre un ítem 2D y una entidad 3D — vive en dos
clases separadas y simples:

- `ItemCellRenderer implements CatalogGrid.CellRenderer<CatalogEntry>` — el
  mismo código de agrandado por escala entera que ya existía, solo movido
  fuera de `CatalogGrid` sin cambiar una sola línea de su lógica.
- `MobCellRenderer implements CatalogGrid.CellRenderer<MobCatalogEntry>` — la
  pieza nueva, descrita abajo.

`ItemCatalog`, `CatalogEntry` y la pestaña Objetos no se modificaron en ningún
archivo; `CatalogGrid` cambió de forma (genérico) pero su comportamiento para
Objetos es idéntico al anterior, confirmado por `git diff` línea por línea del
bucle de render/scroll/selección (no cambió ninguna).

## 3. Cómo se obtienen los mobs Vanilla (sin depender de huevos)

`MobCatalog.build()` enumera **79 criaturas** a mano, como constantes reales de
`EntityType` (35 pasivas, 10 neutrales, 34 hostiles) — un typo aquí es un error
de compilación, no una sorpresa en producción. Se descartó el filtrado
automático por `SpawnGroup` porque no basta: el Warden, el Golem de Nieve, el
Golem de Hierro y el Soporte de Armadura comparten el grupo `MISC` con
entidades que NO son criaturas (el Soporte de Armadura se excluye; los otros
tres SÍ son criaturas Vanilla reales sin huevo, y están incluidos). Por eso el
encargo pedía explícitamente priorizar una enumeración controlada en vez de
filtrar el registro genéricamente.

Excluidos explícitamente: `EntityType.ENDER_DRAGON`, `EntityType.WITHER`
(jefes, no "mobs" de bestiario). Todo lo que no está en la lista (proyectiles,
vehículos, minecarts, display entities, el jugador, etc.) queda fuera por
construcción, no por un filtro que pueda fallar.

Para añadir una criatura nueva (de este equipo o de un mod colaborador) más
adelante, basta con una línea nueva en `MobCatalog.build()` — no hace falta
tocar `MobCatalogEntry`, `MobCellRenderer` ni `CatalogGrid`. No se implementó
ningún mob que no exista ya en Vanilla, y no se desbloquea nada de mods
futuros automáticamente (ninguno está registrado todavía).

## 4. API usada para dibujar entidades (confirmada por bytecode, no supuesta)

Se desensambló (`javap -c -p`) el jar remapeado de Minecraft 1.21.1 en la
caché de Loom — la misma metodología de las rondas anteriores del Panel K —
para confirmar, antes de escribir código:

- `InventoryScreen.drawEntity(DrawContext, int x1, int y1, int x2, int y2,
  int size, float yOffset, float mouseX, float mouseY, LivingEntity)`: la
  llamada que usa Minecraft para dibujar al jugador en su propia pantalla de
  inventario (`AbstractInventoryScreen`/`InventoryScreen.drawBackground`, caja
  49×70, `size=30`, `yOffset=0.0625`, altura real del jugador 1.8 — usado como
  referencia de calibración, ver sección 5).
- Por dentro, esa sobrecarga: activa `context.enableScissor(x1,y1,x2,y2)` y lo
  desactiva al salir; calcula un ángulo de mirada a partir de `(mouseX,mouseY)`
  relativo al centro de la caja; guarda y repone `bodyYaw`/`yaw`/`pitch`/
  `headYaw`/`prevHeadYaw` de la entidad (la pose es puramente visual, no deja
  estado permanente); y delega en la segunda sobrecarga
  (`drawEntity(DrawContext, float, float, float, Vector3f, Quaternionf,
  Quaternionf, LivingEntity)`), que hace `push()`/`pop()` de `MatrixStack`,
  restaura `DiffuseLighting`, pide al `EntityRenderDispatcher` real de
  `MinecraftClient` que dibuje, desactiva/restaura sombras, y hace
  `context.draw()` para vaciar el buffer — todo dentro de la propia llamada.

**No se usó `EntityRenderDispatcher` directamente** ni se gestionó a mano
ningún `VertexConsumerProvider`/matriz de cámara: la sobrecarga vanilla ya
hace, de forma probada durante años por el propio inventario de Minecraft,
exactamente lo que pedía el encargo (push/pop de matrices, restauración de
iluminación/sombras/buffers) — reimplementarlo a mano habría sido más riesgo
por el mismo resultado. Esto responde directamente a la sección 9 del encargo
("cuidar el estado de renderizado"): el estado se restaura porque la función
vanilla ya lo restaura, confirmado leyendo su bytecode completo, no asumido.

### Recorte (scissor) y profundidad — confirmado seguro

`DrawContext` mantiene una pila real de recortes (`ScissorStack`):
`enableScissor` empuja un rectángulo **intersectado** con el vigente,
`disableScissor` lo saca y restaura el anterior (no desactiva el recorte por
completo) — confirmado desensamblando `ScissorStack.push`/`pop`. Por eso llamar
a `drawEntity` (que hace su propio enable/disableScissor interno a la caja de
la celda) DENTRO del recorte ya activo del área de contenido en
`SurvivalPanelScreen` (`context.enableScissor(fx,fy,fx+fw,fy+fh)`) es seguro:
el resultado siempre queda intersectado con ambos límites. Ningún mob puede
salir del área derecha del panel ni tapar los 4 botones laterales, sin
necesitar cambiar el viewport global (que además ya no se toca para nada
relacionado con blur, ver `17_PANEL_K_LIMPIEZA_FINAL.md`).

## 5. Tamaño automático por dimensiones reales (sin recortes ni mobs invisibles)

`size` se calibró contra la única referencia vanilla verificada (sección 4):
jugador, caja 49×70, `size=30`, altura 1.8 → `30 / (70/1.8) ≈ 0.77`. Es decir,
el tamaño "ideal" por encuadre (`alto de caja / alto de la criatura`) se
multiplica por un **factor de llenado de 0.77** para dejar aire arriba/abajo en
vez de tocar los bordes exactos.

`MobCellRenderer.drawPreview` reproduce esa misma proporción para cualquier
criatura, calculando el tamaño ideal tanto por ALTURA como por ANCHO reales
(`EntityType.getHeight()`/`getWidth()`) y usando el **menor** de los dos:

```java
float sizeByHeight = innerH / typeH;
float sizeByWidth  = innerW / typeW;
float size = Math.min(sizeByHeight, sizeByWidth) * FILL_FACTOR * entry.scaleMultiplier();
```

- Una criatura alta y angosta (Enderman, 0.6×2.9) queda limitada por la altura
  → se reduce lo justo para no recortarse arriba/abajo.
- Una criatura ancha y baja (Araña, Ravager) queda limitada por el ancho → no
  se recorta a los lados.
- Una criatura diminuta (Silverfish, Endermite) tiene ambos valores enormes →
  se amplía para seguir siendo reconocible en vez de quedar invisible, sin
  superar `MAX_SIZE` (clamp de seguridad).

Esto es exactamente "ni la misma escala idéntica para todas, ni que las
grandes se recorten, ni que las pequeñas sean invisibles" — una fórmula, no un
valor fijo por criatura. `scaleMultiplier` (por defecto 1.0 en las 79 entradas
actuales) queda como ajuste manual fino para el futuro, si alguna criatura
concreta necesita un retoque tras verla en el juego — no se le asignó ningún
valor especulativo sin poder confirmarlo visualmente.

## 6. Orientación estable de 3/4 (sin seguir el cursor, sin IA, sin giro)

`InventoryScreen.drawEntity` calcula el ángulo de mirada a partir de
`(mouseX,mouseY)` relativo al centro de la caja — así es como el jugador del
inventario "sigue" al cursor. Para el catálogo se pasa un desplazamiento
horizontal **fijo** (`HEAD_TURN_OFFSET_PX = 36`, no el cursor real) y
`mouseY = centerY` (sin inclinación vertical), dando una pose de 3/4 estable
(~29° de giro de cabeza, ~14° de cuerpo) idéntica para toda criatura y toda
celda, que nunca sigue al mouse ni gira sola. Como la entidad de vista previa
nunca recibe `tick()` (ver sección 7), no hay ninguna animación de por medio —
ni idle, ni de ataque, ni de parpadeo — la pose queda completamente fija.

## 7. Creación segura de las entidades de vista previa y su caché

Cada `LivingEntity` de vista previa se crea con `EntityType.create(World)` —
el factory simple que **solo instancia el objeto Java**, sin añadirlo al mundo.
Nunca se llama a `spawnEntity`/`spawn`/`spawnFromItemStack` (los métodos que sí
registran una entidad real) ni a `tick()` sobre ella: sin `tick()` no hay IA,
no hay movimiento, no hay sonidos, no se generan paquetes al servidor, no hay
eventos de juego ni cambios de inventario/estadísticas — la entidad existe
puramente como dato para que el renderizador la lea.

### Caché: de instancia, no estática ni global

La caché (`Map<EntityType<?>, LivingEntity>`, por tipo de criatura, más un
`Set` de tipos que fallaron para no reintentar cada frame) vive **dentro de la
instancia de `MobCellRenderer`**, que a su vez vive en la instancia de
`CatalogGrid` creada en el constructor de `SurvivalPanelScreen`. Cada vez que
se presiona K se crea una pantalla (y por tanto una caché) nueva; la anterior
queda sin referencias y el recolector de basura la libera, sin necesitar
ningún `close()` ni limpieza explícita. Esto cubre, por construcción, los
cuatro casos que pedía el encargo (cerrar la pantalla, cambiar de mundo,
desconectarse, recargar recursos): ninguno de ellos puede dejar una entidad de
vista previa viva más allá de la pantalla que la creó, porque no hay ningún
punto del código que guarde una referencia más duradera que esa pantalla. No
se implementó un sistema de invalidación explícito porque, con este diseño,
no hay nada que invalidar: no existe una caché global que pueda quedar
desincronizada del mundo actual.

### Criaturas especiales / fallo de renderizado

Si crear o dibujar una entidad concreta lanza una excepción (envuelto en
`try/catch` alrededor de cada celda, nunca alrededor del bucle completo), esa
criatura se marca como fallida (no se reintenta cada frame) y se dibuja un
**fallback fiel a la criatura**: su propio huevo de generación
(`SpawnEggItem.forEntity(entityType)`, un ítem real de esa misma criatura, no
un PNG inventado) si existe, o un glifo "!" en rojo si tampoco tiene huevo —
deliberadamente distinto del "?" de contenido bloqueado, para no confundir
"esta criatura falló al dibujarse" con "esta criatura todavía no existe en el
catálogo". Con las 79 entradas actuales (todas criaturas Vanilla estándar,
sin datos NBT especiales requeridos) no se esperan fallos, pero el camino
existe para cualquier caso futuro (mobs de mods con requisitos de
inicialización particulares).

## 8. Interacción, bloqueados y rendimiento

- Hover/click/selección/scroll: sin cambios de comportamiento — los gestiona
  `CatalogGrid`, que no cambió esa lógica al generificarse.
- Las celdas "?" de contenido futuro siguen siendo dibujadas por `CatalogGrid`
  directamente (texto "?", nunca llega a `MobCellRenderer`) — no pueden
  confundirse con una entidad real que falló, que usa el glifo "!" descrito
  arriba, en un camino de código completamente distinto.
- Rendimiento: `CatalogGrid.render` ya solo itera las celdas realmente
  visibles (filas/columnas calculadas desde el área visible, con scroll) —
  no se dibuja ninguna entidad fuera de pantalla. Las instancias de entidad se
  reutilizan desde la caché (una instancia por especie, no por celda ni por
  frame); con 79 especies, el costo de crearlas la primera vez que se abre el
  catálogo es trivial (no son más pesadas de instanciar que cualquier entidad
  normal del juego) y no vuelve a pagarse en fotogramas posteriores ni al
  hacer scroll. No se ejecuta ningún tick de IA. No se han identificado
  criaturas con renderizadores especialmente costosos entre las 79 curadas.

## 9. Archivos modificados/nuevos

- **Nuevo** `panel/client/MobCatalogEntry.java` — record de identidad de mob.
- **Nuevo** `panel/client/MobCellRenderer.java` — renderizado 3D, caché,
  tamaño automático, orientación fija, fallback.
- **Nuevo** `panel/client/ItemCellRenderer.java` — renderizado de Objetos,
  extraído de la antigua `CatalogGrid` sin cambiar su lógica.
- `panel/client/MobCatalog.java` — reescrito: 79 `EntityType` curados a mano
  (antes: `SpawnEggItem.getAll()`).
- `panel/client/CatalogGrid.java` — genérico (`CatalogGrid<T>`) con
  `CellRenderer<T>`; mismo comportamiento de layout/scroll/hover/selección.
- `panel/client/SurvivalPanelScreen.java` — construcción de `mobGrid`/
  `itemGrid` con el nuevo constructor; búsqueda de "Mobs hostiles" adaptada a
  `MobCatalogEntry.displayName()`.

No se modificó `CatalogEntry.java`, `ItemCatalog.java`, `PanelLayout.java`,
`PanelTextures.java`, `PanelScale.java`, `ViewportFix.java`,
`MissionsScreen.java`, `AdminPanelScreen.java`, ningún `.mcmeta`, ni ningún
archivo de `roulette/`, `judgment/`, `client/judgment/`, `missions/`,
`staff/`, `items/`, `events/`, `difficulty/` o `challenges/`.

## 10. Resultado de compilación y pruebas realizadas

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat clean build                      # BUILD SUCCESSFUL
```

`verifyJudgments`: **PASS — 128 objetivos, 135 catálogos**, sin cambios.

Verificado además:
- `git diff` contra la base estable `fix/panel-k-nitidez` confirma que solo
  cambiaron los 6 archivos listados en la sección 9 (2 líneas de
  `SurvivalPanelScreen.java` fuera de la construcción de las cuadrículas: la
  declaración de campos y el bucle de búsqueda de mobs hostiles).
- Conteo exacto de 79 constantes `EntityType` únicas en `MobCatalog.java`
  (35 pasivas + 10 neutrales + 34 hostiles), sin `ENDER_DRAGON` ni `WITHER`.
- Lectura completa del bytecode real de `InventoryScreen.drawEntity` (ambas
  sobrecargas) y de `DrawContext.enableScissor`/`disableScissor`/
  `ScissorStack` antes de escribir `MobCellRenderer`, no después.

**Pendiente — no se puede confirmar sin cliente gráfico en este entorno:**
que las miniaturas se vean nítidas, bien proporcionadas y sin recortes dentro
de Minecraft real; que el ángulo de 3/4 se vea bien; que el rendimiento sea
fluido al hacer scroll por las 79 criaturas; y que ningún mob concreto (todos
"deberían" funcionar por ser criaturas Vanilla estándar, pero esto es una
expectativa razonada, no una observación) falle al dibujarse y requiera el
fallback. No se declara terminada la validación visual hasta que el usuario
la pruebe en el juego.

## 11. Pruebas pendientes en Minecraft (el usuario debe confirmarlas)

1. Reiniciar Minecraft por completo (`gradlew runClient`).
2. Presionar **K**, abrir "Cambios y Mobs" (botón Libro) y confirmar que se
   ven criaturas 3D reales (Zombie, Creeper, Vaca, etc.), no huevos.
3. Confirmar visualmente que ninguna criatura se recorta (Enderman, Ravager,
   Ghast) ni queda demasiado pequeña para reconocerse (Silverfish, Endermite,
   Bat).
4. Confirmar que la orientación de 3/4 se ve natural y estable, sin girar ni
   seguir el cursor.
5. Recorrer con scroll las 79 criaturas + los espacios "?" finales; confirmar
   que los "?" siguen siendo "?" (nunca una criatura real) y que hover/click/
   selección siguen funcionando igual que antes.
6. Abrir y cerrar el Panel K varias veces (y, si es posible, entrar/salir del
   mundo) para confirmar que no hay fugas de memoria ni errores en el log.
7. Confirmar que la pestaña Objetos, Misiones, el Panel Administrativo/Staff,
   Sol PE y el resto de comandos siguen funcionando exactamente igual, y que
   la nitidez del fondo/marco/íconos del panel (ya confirmada en
   `fix/panel-k-nitidez`) no cambió.
8. Si alguna criatura concreta se ve desproporcionada tras verla, es un ajuste
   de una línea (`scaleMultiplier` en `MobCatalog.build()`), no un rediseño.

## 12. Posibles mejoras futuras (no implementadas ahora, fuera de alcance)

- Filtro/orden del catálogo por `Category` (pasiva/neutral/hostil), ya
  disponible como dato en `MobCatalogEntry` pero sin UI que lo use todavía.
- Rotación interactiva con el mouse (el encargo pidió explícitamente NO
  implementarla en esta primera versión).
- Variantes de criatura (color de gato/conejo, bioma de aldeano, etc.) — se
  usa siempre la variante por defecto de `EntityType.create`.
- Un verdadero sistema de registro dinámico para que mods externos añadan
  entradas sin recompilar `apuyawar-systems` (hoy, añadir una entrada requiere
  una línea en `MobCatalog.build()`, que sigue siendo parte de este módulo).
