# Destellos/glow de la ruleta — bitácora de cambios y estado actual

Fecha: 13 de octubre de 2026.

**Estado: NO CONFIRMADO VISUALMENTE TODAVÍA.** El código compila y el build completo
(`gradlew build`, incluida `verifyJudgments`) pasa sin errores, pero el efecto de
destello/arcoíris/explosión de luz que se buscaba no ha sido validado en una ventana
de juego real por el usuario a fecha de este documento. Este archivo deja constancia
de cada cambio hecho, por qué se hizo, y qué falta por verificar.

## Qué se pidió

1. Animación de giro muy rápido, sincronizada con el sonido (ya existente en el
   proyecto antes de esta sesión, vía `RouletteTimeline` y `RouletteSoundController`).
2. Un glow/luz que sobresalga del contorno dorado de la ruleta, separado en 8 colores
   (uno por sector oficial) que forme un arcoíris al girar — todo por código, sin
   generar frames/PNG nuevos.
3. Al terminar el giro y revelarse el color ganador, un destello/explosión de rayos y
   chispas doradas/del color ganador (referencia: imagen con rueda roja de cráneos
   rodeada de rayos y estelas doradas).

## Archivos tocados en esta sesión

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteRenderer.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteLighting.java`

No se crearon PNG, GIF, vídeo ni frames nuevos en ningún momento. Todo el efecto es
geometría (triángulos aditivos) generada en Java.

## Cronología de cambios

### 1. Orden de dibujo del brillo por sector (`RouletteRenderer.java`)

El brillo por sector (`RouletteLighting.sector(...)`) se dibujaba **antes** que el
aro dorado (`RING`) y el medallón (`CENTER`) dentro de `disc(...)`. Se movió para que
se dibuje **después**, de modo que el aro no lo tape. Esto coincidía con la sospecha
original del usuario ("el contorno dorado lo tapa").

### 2. Reescritura del arcoíris de giro (`RouletteLighting.java`)

La corona arcoíris original saltaba de color en bloques según el tick. Se reemplazó
por una estela por sector: cada uno de los 8 colores oficiales emite copias
rezagadas en ángulo (`lag`) con alfa decreciente, generando un efecto de estela
continua en vez de parches de color.

### 3. Refuerzo de intensidad y alcance del halo

- Aro dorado ambiental subido de 3.5% a 16% de opacidad (antes prácticamente
  invisible), visible de forma permanente y no solo durante el giro.
- Radios y anchos de las bandas de color aumentados (de 250/305 a 258/320, con más
  ancho) para que sobresalgan más allá del aro delgado.
- Rayos tipo "lens flare" por cada color oficial, con chispa blanca en la punta.

### 4. Explosión de celebración al revelar el color ganador

Se añadió, sincronizado con `RouletteTimeline.celebration()`/`exitPulse()` (el
instante justo en que termina de rellenarse el color ganador):

- Starburst de 16 rayos largos que disparan muy por fuera del halo.
- 26 chispas dispersas con parpadeo, a radios variables.
- Dos anillos elípticos inclinados ("swirl" tipo anillos de Saturno) que cruzan la
  explosión, inspirados en la imagen de referencia.

### 5. Intento de corrección de orden de flush (`context.draw()`)

Se añadieron llamadas explícitas a `context.draw()` antes y después del dibujo del
halo en `RouletteRenderer.render()`, bajo la hipótesis de que `DrawContext.drawTexture`
(usado por la rueda, el aro y el puntero) encola sus dibujos y se pintan más tarde,
pudiendo tapar el halo dibujado con `Tessellator` crudo. Esta hipótesis se confirmó
correcta a nivel de bytecode (ver sección siguiente), pero **no fue suficiente por
sí sola** para que el brillo se viera.

### 6. Diagnóstico con cuadrado de prueba

Se insertó temporalmente un cuadrado opaco magenta (sin condición de opacidad,
tamaño ~80% de pantalla) dibujado por el mismo camino que el halo, para descartar
que el problema fuera "demasiado sutil". **El usuario confirmó que no se veía ni
siquiera ese cuadrado** — es decir, el problema no era de diseño del brillo (color,
radio, alfa) sino que el mecanismo de dibujo en sí no llegaba a la pantalla del HUD.

### 7. Causa raíz confirmada (inspección de bytecode)

Se extrajo y desensambló (`javap`) el jar de Minecraft 1.21.1 remapeado a Yarn desde
la caché de Loom del proyecto para confirmar el comportamiento real de las APIs:

- `DrawContext.draw()` → llama a `vertexConsumers.draw()` (confirma que
  `DrawContext.drawTexture(...)` solo **encola** el dibujo; no pinta al instante).
- `DrawContext.drawTexture(...)` (la sobrecarga usada en `RouletteDraw.layer`) baja
  internamente hasta `drawTexturedQuad`, que llama a
  `vertexConsumers.getBuffer(RenderLayer)` — confirma que todo el arte de la ruleta
  (sectores, aro, centro, puntero) pasa por `VertexConsumerProvider.Immediate`.
- El halo, en cambio, usaba `Tessellator.getInstance().begin(...)` +
  `BufferRenderer.drawWithGlobalProgram(...)` — un camino de dibujo completamente
  distinto e independiente del que usa `DrawContext`, nunca antes probado en este
  proyecto (los documentos `08_...md` y `09_...md` ya advertían que la comprobación
  visual estaba pendiente). Este camino nunca llegó a mostrarse en el HUD en las
  pruebas del usuario.

### 8. Reescritura completa sobre el mecanismo que sí funciona

Se reescribió `RouletteLighting.java` para dejar de usar `Tessellator`/
`BufferRenderer` y, en su lugar, usar `context.getVertexConsumers()` — el mismo
`VertexConsumerProvider.Immediate` que ya usa con éxito el resto de la ruleta.

Se definió una capa de render (`RenderLayer`) propia:

```java
private static final RenderLayer GLOW = RenderLayer.of(
        "apuyawar_roulette_glow",
        VertexFormats.POSITION_COLOR,
        VertexFormat.DrawMode.QUADS,
        1536,
        RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.COLOR_PROGRAM)
                .transparency(RenderPhase.ADDITIVE_TRANSPARENCY)
                .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                .writeMaskState(RenderPhase.COLOR_MASK)
                .build(false));
```

Esto conserva la mezcla aditiva real (la luz se suma en vez de taparse) pero a
través del mecanismo de dibujo que Minecraft garantiza que se vuelca a pantalla
correctamente en el HUD.

Todas las funciones geométricas internas (`band`, `diamond`, `star`, `ray`,
`ellipseBand`) se adaptaron de triángulos (`VertexFormat.DrawMode.TRIANGLES`, grupos
de 3 vértices) a cuadriláteros (`QUADS`, grupos de 4 vértices), que es lo que exige
esta capa, igual que las capas `gui`/`gui_overlay` nativas de Minecraft.

Se quitó el cuadrado de diagnóstico una vez localizada la causa.

## Validación técnica hecha hasta ahora

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # limpio
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments` sigue en verde: 128 objetivos, 135 catálogos con mecánica, límites
de éxito/fracaso, temporizadores y acuses multijugador.

## Lo que falta — pendiente de confirmación visual

La compilación y el build no prueban que el efecto se vea bien; solo prueban que el
código es válido para el compilador y no rompió nada existente. **Aún no hay
confirmación del usuario de que el nuevo mecanismo (`VertexConsumerProvider` +
`RenderLayer` aditivo propio) sea visible en el cliente real.**

Próximo paso de prueba (requiere cerrar cualquier cliente de desarrollo abierto y
lanzar uno nuevo, porque los cambios de Java no se recargan en caliente):

```powershell
.\gradlew.bat :apuyawar-systems:runClient
```

```mcfunction
/apuyawar ruleta probar rojo
```

Y observar dos momentos distintos de la secuencia (dura ~15.5 s / 310 ticks):

- **Giro rápido** (~tick 32 a 204, es decir, del segundo ~1.6 al ~10.2): debería
  verse el halo dorado permanente más la estela arcoíris por sector.
- **Revelación del color ganador** (~tick 216 a 278, del segundo ~10.8 al ~13.9):
  debería verse el starburst de rayos, las chispas dispersas y los dos anillos
  elípticos inclinados.

Si en esta prueba tampoco se ve nada, el siguiente paso sería activar de nuevo un
cuadrado de diagnóstico (esta vez sobre la capa `RenderLayer` nueva) para aislar si
el problema persiste incluso con el mecanismo "probado", o revisar si hay algún
mixin/otro mod en el entorno de desarrollo que esté interceptando o limpiando el
`VertexConsumerProvider.Immediate` del HUD antes de que se dibuje esta capa.

## Nota sobre el formato de este documento

Se dejó como Markdown dentro de `docs/`, igual que el resto de los documentos del
proyecto (`01_...md` a `09_...md`), en vez de `.docx` o `.zip`, para mantener
consistencia con la documentación existente y que quede versionado junto al código.
