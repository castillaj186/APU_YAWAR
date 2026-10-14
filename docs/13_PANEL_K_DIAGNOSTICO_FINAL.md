# Panel K — diagnóstico final del desenfoque

Fecha: 13 de octubre de 2026.

**Estado: compila y construye limpio (`gradlew build`, `verifyJudgments` en
verde), con dos hallazgos nuevos confirmados por bytecode y una corrección de
filtrado que no se había probado antes. Sin confirmación visual en Minecraft —
sigue sin haber cliente gráfico disponible en este entorno.** No se tocó ningún
archivo de la ruleta, Inti, Staff, comandos administrativos, persistencia ni
networking de la ruleta.

## 1. Estado original del problema

Tras dos rondas previas de corrección (ver `11_PANEL_K_STAFF_SOL_PE.md` y
`12_PANEL_K_CORRECCION_VISUAL.md`), el encuadre y los 4 botones ya funcionaban
correctamente, pero el panel, los íconos, las decoraciones de Inti y las
constelaciones seguían viéndose borrosos. El usuario pidió explícitamente no
repetir el afilado sin antes separar: blur del mundo detrás de la pantalla vs.
filtrado de nuestras texturas vs. escalado.

## 2. Causas investigadas

Se investigaron, con evidencia verificable (no suposición), las siguientes:

1. Blur del mundo de fondo de Minecraft (`Screen.renderBackground`).
2. Doble escalado GUI (lógico vs. físico).
3. Filtrado nearest vs. bilineal para el tipo de arte que tenemos.
4. JAR/directorio de ejecución duplicado o desactualizado.
5. Caché de texturas de una sesión anterior.
6. Espaciado/tamaño de la cuadrícula de catálogos (afecta legibilidad, no
   "nitidez" en sentido estricto, pero el usuario lo reportó como parte del
   mismo problema de presentación).

## 3. Causas realmente confirmadas

### A. El blur del mundo NO afecta nuestro panel (descartado con evidencia)

Desensamblé `Screen.renderBackground()` del jar real de Minecraft 1.21.1:

```
renderBackground(context, mouseX, mouseY, delta):
  if (client.world == null) renderPanoramaBackground(...)
  applyBlur(delta)          // <- captura y desenfoca el framebuffer EXISTENTE
      -> GameRenderer.renderBlur(delta)
      -> client.getFramebuffer().beginWrite(false)   // vuelve a enlazar el framebuffer normal
  renderDarkening(context)  // overlay oscuro semitransparente, nada más
```

`applyBlur` desenfoca lo que YA está en el framebuffer en ese instante (el
mundo renderizado) y luego **vuelve a enlazar el framebuffer normal** para que
todo lo dibujado DESPUÉS (nuestro panel, íconos, texto) se pinte sin blur. En
nuestro código, `renderBackground(...)` se llama **primero**, antes de dibujar
el panel — exactamente el orden correcto. Confirmado: el blur de fondo no es la
causa de que el panel se vea borroso.

### B. No hay doble escalado (descartado con evidencia)

Desensamblé dónde `MinecraftClient` construye cada `Screen`:

```
screen.init(client, window.getScaledWidth(), window.getScaledHeight())
```

`Screen.width`/`Screen.height` son las mismas unidades lógicas GUI-escaladas
que ya usa el resto del proyecto (`context.getScaledWindowWidth()` en la
ruleta). Nuestro `panelBox()` usa esas mismas unidades de forma consistente.
Descartado.

### C. El filtro `blur:false` (nearest-neighbor) era la elección equivocada para este arte — CAUSA PRINCIPAL CORREGIDA ESTA RONDA

Esto es lo nuevo de esta ronda. En la ronda anterior se verificó (correctamente)
que `blur:false` sí fuerza `GL_NEAREST` real sin mipmaps — ese mecanismo nunca
estuvo roto. Pero **nearest-neighbor es la herramienta correcta para subir de
tamaño pixel art de baja resolución nativa (ej. 16×16 → 64×64), no para reducir
imágenes grandes y detalladas** como las nuestras (1672×941 / 1254×1254,
ilustraciones con antialiasing real, no pixel art de 1 bit — confirmado en la
ronda anterior midiendo una rampa de alfa de 4-5 píxeles en un borde). Al
reducir una imagen así con nearest-neighbor, cada píxel de destino solo toma
UN texel de origen y descarta el resto — eso genera un patrón irregular/ruidoso
que se percibe como "sucio" o "borroso", incluso con el filtro técnicamente
"correcto" según su propia definición.

**Corrección:** los 5 `.png.mcmeta` del panel pasaron de `blur:false` a
`blur:true` (bilineal, el comportamiento por defecto de Minecraft), que es la
elección correcta para reducir imágenes detalladas de alta resolución. Se
mantiene `clamp:true`. Las versiones ya redimensionadas y afiladas (unsharp
mask) de la ronda anterior se conservan — bilineal + un origen ya nítido deben
combinarse mejor que nearest + el mismo origen.

### D. JAR y caché (verificado, sin anomalías)

- `apuyawar-systems/run/mods/` está vacío — Loom ejecuta `runClient` directo
  desde las clases/recursos compilados, no desde un jar empaquetado en una
  carpeta mods. No hay conflicto posible ahí.
- Se encontró `C:\Users\casti\Music\ruleta nueva\safio_ui-1.6.9-apuyawar-ruleta.jar`
  — un archivo histórico suelto en la carpeta Music del usuario, **no está en
  ninguna carpeta `mods` de ninguna instalación de Minecraft**, no se carga,
  no genera conflicto.
- Dado que Loom usa recursos directamente desde `src/main/resources` (no un jar
  intermedio) para `runClient`, un reinicio limpio del cliente debería recoger
  los archivos actuales del disco sin pasos adicionales.

## 4. Evidencia para distinguir blur de fondo / filtrado / escalado

Se implementó un **modo de diagnóstico temporal** en `SurvivalPanelScreen`
(tecla **F8** para alternar, apagado por defecto, con un aviso discreto siempre
visible abajo a la derecha: "F8: diagnóstico de nitidez"). Al activarlo,
dibuja tres elementos lado a lado en la esquina superior izquierda:

- **A — Cuadrícula blanco/negro** dibujada con `DrawContext.fill()` (nunca
  puede verse borrosa, no es una textura — control de referencia).
- **B — Un recorte 1:1 de `panel.png`** (32×32 texels del borde dorado),
  dibujado con destino = región muestreada, es decir, **sin ningún factor de
  escala aplicado** — si esto se ve blando, es intrínseco a la textura/filtro,
  no al escalado.
- **C — Texto nativo** de Minecraft justo al lado, para comparar contra ambos.

Esto deja una evidencia reproducible: si A y C se ven nítidos pero B no, el
problema es específico de la textura (filtro o contenido fuente). Si los tres
se ven blandos en la misma captura, el problema es ajeno a este código
(compresión de la captura, escalado del sistema operativo, etc.).

## 5. Archivos modificados esta ronda

- `textures/gui/panel_k/{panel,icon_mobs,icon_items,icon_missions,icon_search}.png.mcmeta` — `blur:false` → `blur:true`.
- `SurvivalPanelScreen.java` — overlay de diagnóstico F8 (temporal, detrás de un toggle apagado por defecto, fácil de quitar).
- `CatalogGrid.java` — reescrito: celdas de 34px (antes 20px) con separación visible, íconos ampliados 1.7× (verificado por bytecode que `DrawContext.drawItem` usa el mismo `MatrixStack` del contexto, así que escalarlo antes de dibujar amplía el ítem sin deformarlo), contorno de selección/hover en vez de solo relleno, fila de información reservada aparte de la cuadrícula.
- `MissionsScreen.java` — reescrito: ahora dibuja el mismo fondo `panel.png` y la misma capa oscura semitransparente que el panel principal (antes era texto plano sobre el mundo), conservando lista + detalle + botón de volver + progreso/recompensa reales ya existentes. No se inventó ninguna misión nueva.

## 6. Cambios aplicados al renderizador

Ninguno nuevo en la lógica de dibujo del panel/íconos más allá de lo ya
corregido en la ronda anterior (sobrecarga de 10 argumentos de `drawTexture`,
coordenadas medidas). Esta ronda el cambio de renderizado es puramente el
filtro de textura (`blur:true`) y la ampliación de ítems vía `MatrixStack` en
`CatalogGrid`.

## 7. Texturas optimizadas

Ninguna nueva esta ronda — se reutilizan las copias de 512×512 (íconos) y
256×256 (Sol PE) ya preparadas, y `panel.png` a su resolución nativa 1672×941,
todas con el unsharp mask ya aplicado. Solo cambió su `.mcmeta` (filtro).

## 8. Resultado de compilación

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # limpio
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments`: 128 objetivos, 135 catálogos, en verde — la ruleta no se vio
afectada.

## 9. Pruebas efectivamente realizadas

- Desensamblado de bytecode real de `Screen.renderBackground`, `applyBlur`,
  `MinecraftClient` (construcción de `Screen` con dimensiones GUI-escaladas),
  y `DrawContext.drawItem` (confirmando el `MatrixStack` compartido).
- Búsqueda de jars duplicados/carpetas `mods` en el sistema.
- Verificación de que `run/mods` está vacío.
- Compilación y build completos, `verifyJudgments` en verde.

## 10. Pruebas que el usuario debe realizar personalmente en Minecraft

Esto sigue sin confirmarse visualmente — no hay cliente gráfico en este
entorno:

1. **Cerrar Minecraft por completo** (no solo reabrir K) y volver a ejecutar
   `gradlew runClient`, para asegurar que no quede ninguna textura cacheada de
   una sesión anterior.
2. Presionar **F8** dentro del Panel K y comparar los tres elementos de
   diagnóstico (A, B, C) en una misma captura — esto determina si el problema
   sigue siendo de la textura o es ajeno al código.
3. Comparar el panel con `blur:true` (este cambio) contra cómo se veía con
   `blur:false` (ronda anterior) — debería notarse más suave pero MENOS
   "ruidoso/sucio" en los bordes detallados.
4. Probar GUI Scale 2, 3, 4 y Auto, idealmente con capturas F2 sin redimensionar
   después.
5. Confirmar que los catálogos de Mobs/Objetos ahora tienen más separación e
   íconos más grandes, y que la pantalla de Misiones ya no se ve como texto
   plano sobre el mundo.
6. Confirmar que los 4 botones del panel, el panel administrativo y el resto
   de funciones (Staff, Sol PE, comandos) siguen funcionando sin cambios.

No se declara el desenfoque resuelto hasta que se confirme con el modo de
diagnóstico F8 y una prueba directa en el juego.
