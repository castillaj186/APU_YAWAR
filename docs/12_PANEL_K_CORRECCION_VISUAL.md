# Panel K — corrección de encuadre y nitidez

Fecha: 13 de octubre de 2026.

**Estado: compila y construye limpio (`gradlew build`, `verifyJudgments` en verde),
causas verificadas con evidencia (bytecode real de Minecraft + medición de píxeles
de los PNG), pero SIN confirmación visual en una ventana de Minecraft — no hay
cliente gráfico disponible en este entorno. No se tocó ningún archivo de la
ruleta, Inti, Staff, comandos administrativos, networking ni misiones.**

Este documento complementa a `11_PANEL_K_STAFF_SOL_PE.md` (la implementación
inicial del Panel K) con dos rondas de corrección visual hechas después de que
el usuario probó el panel en Minecraft y reportó problemas.

## Ronda 1 — el panel se veía "ampliado, recortado y borroso"

### Causa real (confirmada por bytecode)

El código usaba la sobrecarga de **8 argumentos** de `DrawContext.drawTexture`:

```java
context.drawTexture(id, x, y, 0f, 0f, destW, destH, texW, texH);
```

Desensamblé esta sobrecarga en el jar real de Minecraft 1.21.1 (caché de Loom) y
confirmé que delega en una de 10 argumentos reutilizando `destW`/`destH` **también
como el tamaño de la región muestreada de la textura**, no solo como el tamaño de
destino en pantalla. Al pedir un destino más chico que la imagen completa (normal,
porque la ventana GUI es mucho más chica que 1672×941 píxeles de textura), Minecraft
terminaba recortando una esquina diminuta de la imagen y ampliándola para llenar el
destino — exactamente el "zoom/recorte" reportado. Afectaba tanto al panel como a
los 4 íconos.

### Corrección

Se cambió a la sobrecarga de **10 argumentos**, donde destino y región muestreada
son parámetros independientes:

```java
context.drawTexture(id, x, y, destW, destH, 0f, 0f, texW, texH, texW, texH);
```

### Coordenadas de los 4 espacios — medidas por código, no a ojo

Se usó PowerShell + `System.Drawing` para escanear los píxeles reales de
`panel.png` (1672×941), buscando el rectángulo oscuro interior de cada espacio por
contraste contra el marco dorado, validado en 3 columnas (x=130,160,180,220) y 4
filas (una por espacio). Resultado: los 4 espacios comparten el mismo ancho
(x: 111–249), pero el cuarto (Búsqueda) es genuinamente más bajo que los otros
tres (730–843 = 113px, contra ~132-135px de los otros) — confirmado en las 3
columnas, no es un error de medición. `PanelLayout.java` quedó con estos valores
exactos.

### Otros ajustes de esta ronda
- Íconos dibujados al 82% del espacio oscuro (margen al borde dorado).
- `.png.mcmeta` (`blur:false, clamp:true`) en las 5 texturas del panel.
- Capa oscura semitransparente detrás del área de contenido para legibilidad.

**Archivos:** `SurvivalPanelScreen.java`, `PanelLayout.java`, 5 archivos
`.png.mcmeta` nuevos.

## Ronda 2 — el encuadre ya estaba bien, pero seguía borroso

El usuario confirmó que el encuadre y los 4 botones ya aparecían correctamente,
pero pidió no asumir que `blur:false` ya resolvía la nitidez sin comprobarlo, y
descartar explícitamente 9 posibles causas antes de actuar.

### Verificación de las 9 causas (una por una, con evidencia)

1. **Reducción excesiva de originales** — no aplica, no se tocaron los masters.
2. **Filtrado bilineal** — **descartado con bytecode**: desensamblé
   `ResourceTexture.load()` → `TextureResourceMetadataReader` →
   `NativeImage.upload()` del jar real. `blur:false` sí llega hasta
   `setTextureFilter(false, false)`, que fija min/mag filter en `GL_NEAREST`
   (constante OpenGL 9728) real.
3. **Mipmaps** — descartado: en esa misma ruta de carga, el flag de mipmap va
   codificado en `false` siempre para `ResourceTexture`.
4-7. **Escala GUI / matrices / shaders / composición** — revisado
   `SurvivalPanelScreen.java`: no hay matriz de escala adicional ni shader
   custom sobre estas texturas, solo la sobrecarga de `drawTexture` ya corregida.
8. **Pérdida de detalle ya presente en los PNG originales — causa real**,
   confirmada con evidencia directa de píxeles: se escaneó la transición alfa de
   un borde dorado en `libro.png` y se encontró una rampa de **4-5 píxeles**
   (alfa 0→86→221→245→250), no un salto de 1 píxel. Son ilustraciones "estilo
   pixel art" con antialiasing real ya horneado en el archivo, no pixel art de 1
   bit — ningún ajuste de filtrado de GPU puede endurecer un borde que ya viene
   suave en el PNG.
9. **Compresión de capturas** — no evaluable sin una captura del usuario en esta
   ronda; no se asumió.

### Corrección: copias optimizadas de los recursos (masters intactos)

No se tocó ningún archivo en `assets-fuente/panel_K/`. Se generaron copias
optimizadas en los recursos del mod, con respaldo previo de las copias anteriores
en `docs/respaldos/panel_k_pre_nitidez_2026-10-13/`.

**Proceso** (PowerShell + System.Drawing — no había ImageMagick ni Python/Pillow
instalados, se comprobó antes de improvisar):

1. Redimensión en **espacio de alfa premultiplicado** (evita halos oscuros/claros
   en bordes transparentes, un problema clásico del redimensionado ingenuo) con
   interpolación **bicúbica de alta calidad** — se probó primero contra un
   redimensionado simple para confirmar que no introducía artefactos.
2. **Unsharp mask** propio (blur 3×3 + realce, amount≈1.1-1.4), también en espacio
   premultiplicado, para endurecer la rampa de 4-5px medida en el paso anterior.
3. Se probó primero en un solo ícono (`libro.png`) y se revisó el resultado
   visualmente antes de aplicarlo a los demás 4 assets.

### Resoluciones finales

| Asset | Antes | Ahora | Por qué |
|---|---|---|---|
| Los 4 íconos del panel | 1254×1254 | **512×512** | Se muestran pequeños (~139px de textura); 512 da margen holgado sin nunca necesitar ampliarse. |
| `sol_pe.png` (ítem) | 1254×1254 | **256×256** | Los ítems vanilla son 16×16; 256 es generoso para el atlas de ítems. |
| `panel.png` | 1672×941 | **1672×941 (sin reducir)**, solo afilado | Reducirlo arriesgaba forzar una ampliación de vuelta en monitores grandes con GUI Scale bajo (ej. 4K a Escala 1), reintroduciendo el desenfoque para esos usuarios. |

### Archivos modificados en la ronda 2
- `PanelTextures.java` (constante `ICON_SIZE` 1254→512, documentado el porqué).
- Las 6 texturas en `textures/gui/panel_k/` y `textures/item/sol_pe.png`.

## Validación técnica (ambas rondas)

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # limpio
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments`: 128 objetivos, 135 catálogos, sigue en verde tras cada cambio —
la ruleta no se vio afectada en ningún momento.

## Lo que sí se verificó directamente vs. lo que sigue pendiente

**Verificado directamente:**
- El mecanismo `blur:false → GL_NEAREST` por desensamblado del bytecode real de
  Minecraft (no es una suposición).
- La causa de la "rampa de 4-5px" por medición directa de píxeles alfa.
- Los PNG resultantes (panel completo y la moneda) vistos con la herramienta de
  lectura de imágenes — sin halos, bordes limpios, visiblemente más definidos que
  antes de afilar.

**Pendiente de verificar en Minecraft** (sin cliente gráfico en este entorno):
- Que el panel se vea nítido dentro del juego en GUI Scale 2/3/4/Auto.
- Que no haya quedado ninguna textura cacheada de una sesión anterior — **si el
  panel ya se había cargado una vez en una sesión de Minecraft abierta antes de
  este cambio, hace falta reiniciar el cliente por completo** (no solo reabrir K)
  para que cargue los PNG nuevos, ya que quedan cacheados en memoria por
  identificador.
