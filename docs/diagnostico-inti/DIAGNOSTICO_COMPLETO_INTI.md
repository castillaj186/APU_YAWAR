# Diagnóstico técnico completo: presentación de Inti

Fecha de auditoría: 12 de octubre de 2026. Esta auditoría no modifica el renderer, la ruleta, los retos ni los recursos activos.

## Entorno

| Elemento | Valor |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.14 |
| Fabric API | 0.116.17+1.21.1 |
| Java objetivo | 21 |
| Módulo | `apuyawar-systems` |
| Namespace de recursos | `apuyawar_systems` |

## Flujo actual

1. `JudgmentService` confirma el final de una ruleta roja y envía `IntiStartPayload` a los clientes compatibles.
2. `ApuYawarClient` recibe el paquete y llama a `IntiClientState.begin`.
3. `IntiClientState` mantiene un reloj local de 270 ticks y calcula un índice entre 0 y 198.
4. `HudRenderCallback` llama a `IntiRenderer.render` cada fotograma de interfaz.
5. Al terminar los 270 ticks, el cliente envía `IntiFinishedPayload`; el servidor inicia el reto rojo y `ChallengeEngine` muestra **MOMENTO DEL JUICIO**. El temporizador no inicia durante la presentación.

Archivos participantes:

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/judgment/IntiRenderer.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/judgment/IntiClientState.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/judgment/IntiSoundController.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/judgment/IntiPresentationTimeline.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/ApuYawarClient.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/events/JudgmentService.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/network/IntiStartPayload.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/network/IntiFinishedPayload.java`

Las copias exactas de esos fuentes están en `codigo-para-revision/`.

## Frames originales

Ruta: `E:\APU_YAWAR\assets-fuente\inti-juicio\frames-originales`.

- Cantidad: **199** PNG, numerados de `frame_001.png` a `frame_199.png`, sin ausencias ni hashes SHA-256 duplicados.
- Resolución: **1254×1254** en los 199 archivos.
- Formato: `Format32bppArgb`; las esquinas analizadas presentan alfa transparente.
- Tamaño combinado: 488 550 154 bytes.
- El archivo `evidencias/limites_alpha_originales.csv` contiene los límites de píxeles con alfa > 8 y el centroide alfa de los 199 archivos.

Los frames 1 a 197 tienen composición visible amplia, normalmente casi todo el lienzo: por ejemplo, `frame_100.png` tiene límites alfa `x=6..1253`, `y=12..1234` y centroide `(631.66, 617.22)`, muy próximo al centro geométrico `(626.5, 626.5)`. La máscara/figura completa está presente en los fotogramas de estado estable, incluidos 50, 100 y 150. Los frames 198 y 199 contienen las etapas finales de desvanecimiento y por diseño poseen menos contenido visible.

La evidencia `evidencias/frame_100_original_completo.png` es una copia no modificada del original completo. No se detectó recorte en el material de origen.

## Atlas histórico

Ruta: `E:\APU_YAWAR\assets-fuente\inti-juicio\revision-int-01\inti_atlas.png`.

| Propiedad | Valor comprobado |
|---|---|
| Dimensiones | 4096×3328 px |
| Formato | PNG `Format32bppArgb` |
| Columnas | 16 |
| Filas | 13 |
| Tamaño de celda | 256×256 px |
| Capacidad | 208 celdas |
| Frames usados | 199 |
| Celdas transparentes sin uso | 9 |

El atlas histórico se construyó con la siguiente operación equivalente:

```text
ffmpeg -framerate 40 -start_number 1 -i frame_%03d.png \
  -vf "scale=256:256:flags=lanczos,tile=16x13:padding=0:margin=0:color=0x00000000" \
  -frames:v 1 -pix_fmt rgba inti_atlas.png
```

Distribución: índice cero basado `i = númeroDeFrame - 1`; `columna = i % 16`; `fila = i / 16`; `x = columna × 256`; `y = fila × 256`.

Ejemplos:

| Frame | Índice | Columna | Fila | Rectángulo en atlas |
|---|---:|---:|---:|---|
| 001 | 0 | 0 | 0 | 0,0–255,255 |
| 100 | 99 | 3 | 6 | 768,1536–1023,1791 |
| 199 | 198 | 6 | 12 | 1536,3072–1791,3327 |

Los 199 frames caben: 12 filas completas contienen 192 celdas y la fila 13 contiene los 7 frames finales. La extracción de la celda 100 se guardó como `evidencias/frame_100_extraido_atlas.png`. Muestra la figura completa; no hay prueba de un recorte dentro de esa celda. La comparación con el original reducido está en `evidencias/comparacion_diferencia_frame_100.png`. La diferencia de píxeles visible corresponde al orden de reescalado (1254→256→512 frente a 1254→512), no a una extracción parcial.

## Render histórico mediante atlas

Una versión anterior de `IntiRenderer` usó el atlas. Su lógica era:

```java
int column = frame % 16;
int row = frame / 16;
context.drawTexture(ATLAS, x, y, 0,
    column * 256, row * 256, side, side, 4096, 3328);
```

Ese método usa coordenadas de píxel `u` y `v`, no UV normalizadas. Para el frame 100 seleccionaba `u=768`, `v=1536`, ancho y alto de origen implícitos de 256 por la relación de `side`, con tamaño total 4096×3328. La cuadrícula y ese cálculo son matemáticamente coherentes, pero el atlas grande sigue siendo una variable de riesgo de renderizado o de diagnóstico en clientes reales.

## Render activo actual

El renderer actual **no usa atlas**. Fragmentos reales de `IntiRenderer.java`:

```java
// líneas 10–14: identificador individual de cada frame
FRAMES[frame] = Identifier.of("apuyawar_systems",
    "textures/gui/inti/frames/frame_%03d.png".formatted(frame + 1));

// línea 19: frame 0..198, calculado por IntiClientState
int frame = IntiClientState.frame(tickDelta);

// líneas 25–27: rectángulo de dibujo centrado
int side = Math.min((int)(context.getScaledWindowHeight() * .55f),
    (int)(context.getScaledWindowWidth() * .62f));
int x = (context.getScaledWindowWidth() - side) / 2;
int y = (context.getScaledWindowHeight() - side) / 2;

// líneas 38–40: PNG entero, desde u=0,v=0
context.drawTexture(FRAMES[frame], x, y, 0, 0,
    side, side, 512, 512);
```

`DrawContext.drawTexture(Identifier, x, y, u, v, width, height, textureWidth, textureHeight)` recibe coordenadas de píxel. Para el recurso activo, `u=0`, `v=0` y `textureWidth=textureHeight=512`; por tanto, no existe selección de celda, UV normalizada ni subregión.

El tamaño depende de la interfaz escalada: `side = min(0.55 × altura, 0.62 × anchura)`. El centro del rectángulo es exactamente `(ancho/2, alto/2)`. La rueda usa aproximadamente 50% de la altura y por eso Inti está configurado cerca de 1.10×. No existe desplazamiento horizontal o vertical en el dibujo principal. El aura repite el mismo PNG completo a escala ligeramente mayor y alfa máximo 0.14; no recorta ni agrega un disco sólido.

`IntiClientState.frame` calcula:

```java
(int) (time(partial) * 199 / 270)
```

con límite superior de 198. Reproduce todos los índices, sin saltar a un atlas ni modificar UV.

## Historial de intentos conocido

1. **Atlas único:** se redujeron los originales a 256×256 y se ubicaron en una cuadrícula 16×13. Fue eficiente, pero la captura en Minecraft siguió mostrando una presentación que el usuario percibió como parcial.
2. **Ajuste de renderer de atlas:** se hicieron explícitos tamaño total, celda, centrado y una aura enmascarada. No resolvió la percepción del problema en la prueba del usuario.
3. **Frames individuales activos:** se exportaron 199 PNG de 512×512 y el renderer actual eliminó por completo el atlas y las UV de cuadrícula. El JAR actual contiene los 199 archivos; el atlas histórico permanece solo como referencia y no es referenciado por `IntiRenderer`.

No existe en el código activo lógica `cover`, `crop-center`, recorte por stencil o transformación de matrices para Inti. La única operación de escala preserva 1:1.

## Pruebas realizadas

| Prueba | Resultado | Evidencia |
|---|---|---|
| Continuidad de originales | 199/199, sin ausencias | este informe |
| Original completo | Frame 100 incluye figura completa | `frame_100_original_completo.png` |
| Alfa | Esquina del frame activo 100 = 0 | comprobación de auditoría |
| Atlas real | 4096×3328, 16×13, 256×256 | este informe |
| Celda atlas 100 | Figura completa, no subregión | `frame_100_extraido_atlas.png` |
| Comparación | Diferencias por dos reescalados, no por crop | `comparacion_diferencia_frame_100.png` |
| Recurso activo | 199 PNG individuales 512×512 incluidos en JAR | inspección `jar tf` |
| Proporción actual | Cuadrado→cuadrado, sin deformación | líneas 25–40 del renderer |

No se ejecutó una validación visual interactiva concluyente en esta auditoría: había procesos de cliente de desarrollo abiertos y los logs estaban bloqueados. No se afirma que una prueba visual final haya pasado.

## CAUSA PROBABLE DEL RECORTE DE INTI

**Confirmado:** los originales y la celda histórica examinada contienen la figura completa. El atlas admite los 199 frames y la celda 100 se extrae completa. El recurso activo actual tampoco utiliza atlas ni UV de celdas.

**Pendiente de confirmar en un cliente reiniciado:** la captura puede provenir de una instancia de desarrollo que cargó una clase o recursos anteriores, ya que durante la auditoría `latest.log` y `debug.log` estaban bloqueados por procesos existentes. Otra posibilidad es que la expectativa visual sea una máscara aislada, mientras que los frames disponibles contienen la composición ornamental completa; el frame 100 evidencia una figura completa, pero incluye ornamentos y energía fuera del rostro.

**No confirmado:** no hay evidencia de que el PNG original, la celda 100 del atlas, ni el cálculo actual `u=0,v=0` recorten la figura. No se puede atribuir el problema actual a UV del atlas porque el renderer activo ya no usa atlas.

## PROPUESTA DE CORRECCIÓN DEFINITIVA

No implementar aún. Primero cerrar todas las instancias de cliente de desarrollo, iniciar una única instancia limpia y usar `/apuyawar inti probar`. Si la imagen continúa parcial con el renderer individual activo, registrar en pantalla el identificador de frame y las dimensiones escaladas para comprobar qué recurso concreta está cargando el cliente.

Si el requisito artístico es mostrar solamente una máscara facial en vez de la composición completa existente, preparar un recurso maestro nuevo con esa máscara aislada y transparencia; ningún ajuste de UV puede producir esa composición si no existe como frame fuente. Mantener los frames individuales para eliminar el atlas de la ecuación.
