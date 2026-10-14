# Panel K — solución de nitidez (a partir de la evidencia F8)

Fecha: 14 de octubre de 2026.

**Estado: compila y construye limpio (`gradlew compileJava`, `gradlew build`),
`verifyJudgments` en verde (128 objetivos, 135 catálogos). Cambios de código y
de recursos aplicados, no solo diagnóstico. Sigue pendiente la confirmación
visual del usuario en Minecraft — no hay cliente gráfico en este entorno.** No
se tocó la ruleta, Inti, Staff, comandos administrativos, persistencia, Sol PE,
recompensas, networking ni el orden/diseño de los 4 botones.

## 1. Qué reveló la captura F8 del usuario

En la ronda anterior (`13_PANEL_K_DIAGNOSTICO_FINAL.md`) solo se comparaban
tres elementos (cuadrícula `fill`, un recorte 1:1 de `panel.png`, texto
nativo). El usuario activó F8, probó en Minecraft y reportó algo que esa
prueba no podía detectar: **la cuadrícula A y el texto C se veían nítidos,
pero el marco del panel, los 4 íconos Y los huevos de aparición de mobs
(ítems 100% vanilla, dibujados por `CatalogGrid`, sin ninguna textura
nuestra) también se veían borrosos.**

Esto es información nueva y decisiva: descarta que el problema sea exclusivo
del contenido de nuestros PNG (antialiasing original, elección de filtro en
nuestros `.mcmeta`), porque un ítem vanilla no pasa por ninguno de esos
archivos. Si también se ve borroso, la causa tiene que estar en **cómo lo
dibuja nuestro código**, no en el PNG.

## 2. Causa encontrada

`CatalogGrid.java` dibujaba cada ícono de catálogo así:

```java
ctx.getMatrices().push();
float pad = (cellW - 16 * ICON_SCALE) / 2f;   // ICON_SCALE = 1.7f
ctx.getMatrices().translate(cx + pad, cy + pad, 0);
ctx.getMatrices().scale(ICON_SCALE, ICON_SCALE, 1f);
ctx.drawItem(entries.get(index).icon(), 0, 0);
ctx.getMatrices().pop();
```

El atlas de ítems de Minecraft se muestrea con **nearest** (por eso un ítem
vanilla normal se ve pixelado y nítido en cualquier inventario, a cualquier
tamaño entero). Con un factor de escala **no entero** (1.7×), el límite de
cada texel de origen ya no coincide con el límite de ningún píxel de
destino: distintos píxeles de destino terminan repitiendo el mismo texel una
cantidad desigual de veces (algunos texels se repiten 1 vez, otros 2),
produciendo un patrón de aliasing irregular. En una captura esto se percibe
exactamente como "borroso/sucio", aunque técnicamente no hay ningún
desenfoque (ni blur de GPU, ni mezcla de píxeles) involucrado — es un
problema de **muestreo irregular por escala fraccionaria sobre un filtro
nearest**, no de blur.

Esto explica por qué los huevos de aparición (vanilla, nearest, sin tocar
ningún `.mcmeta` nuestro) se veían mal **solo** dentro de `CatalogGrid`: es
el único lugar del proyecto que aplica una escala no entera vía `MatrixStack`
antes de `drawItem`. Los 4 íconos principales del panel (libro/espada/
pergamino/lupa) no pasan por aquí — se dibujan con `drawTexture` a un tamaño
calculado directamente, sin este `MatrixStack.scale` adicional.

Por separado, también se identificó una causa secundaria en el propio marco
del panel: `panelBox()` calculaba un factor de escala **continuo arbitrario**
(`this.width * 0.92f / PANEL_W`, ej. 0.4387...) sin relación alguna con la
cuadrícula de texels de origen. Aunque esto se dibuja con `drawTexture`
(bilineal, no nearest, así que no sufre el mismo aliasing "en bloques" que
los ítems), un ratio arbitrario sigue produciendo un patrón de muestreo menos
predecible que un ratio limpio 1/n, donde cada fila/columna de destino
corresponde a un número exacto de texels de origen.

## 3. Pruebas que lo demuestran

No se puede confirmar visualmente en este entorno (sin cliente gráfico), así
que en vez de "arreglar y asumir", se amplió el propio overlay F8 para que el
usuario pueda confirmarlo él mismo con una sola captura, aislando cada
variable por separado:

- **D1 vs D2**: el *mismo* bitmap (`icon_mobs.png`) al *mismo* tamaño de
  destino, D1 con el filtro de producción (bilineal) y D2 con una copia de
  depuración en nearest (`debug_icon_nearest.png`, mismo PNG exacto, distinto
  `.mcmeta`). Aísla el filtro de la escala y del contenido.
- **E1 vs E2**: un ítem 100% vanilla (huevo de zombie) dibujado nativo a
  16px sin ninguna escala nuestra (E1), contra el mismo ítem con la escala
  **entera** ×2 que ahora usa `CatalogGrid` (E2). Si ambos se ven nítidos,
  queda confirmado que el factor entero resolvió el problema reportado con
  los huevos de aparición.
- **B1 vs B2**: un recorte de `panel.png` al factor de escala **en vivo** que
  usa el panel real en esa pantalla (B1) contra el mismo recorte a 1:1 sin
  ningún escalado (B2). Si B2 se ve nítido y B1 no, confirma que el
  escalado del marco (ahora corregido a un ratio limpio) era parte del
  problema.
- **A y C** se mantienen como control (nunca deberían verse mal).

Las 6 pruebas se dibujan ahora en una cuadrícula de celdas fijas (2 columnas
× 4 filas) sin superposición — se corrigió también el defecto reportado de
la ronda anterior, donde la prueba B quedaba parcialmente tapada.

## 4. Estrategia elegida

No se trató de "elegir entre 3 alternativas de textura" como una decisión a
ciegas, sino de **eliminar la fuente concreta del defecto** una vez
localizada, y dejar una herramienta de verificación en el propio juego para
que el usuario confirme con datos, no con otra ronda de suposiciones:

1. **`CatalogGrid`**: `ICON_SCALE` pasó de `1.7f` a un entero `2` (`int`, no
   `float`, para que no pueda volver a convertirse en fraccionario por
   error). `CELL` subió de 34 a 40px para darle espacio al ícono más grande
   (32px) con margen. El relleno (`pad`) ahora se calcula como entero
   también, no solo el factor de escala.
2. **`panelBox()`** (en `SurvivalPanelScreen` y `MissionsScreen`, idéntico en
   ambas para que el panel se vea igual de nítido en las dos pantallas): el
   ratio ideal (92% de pantalla) ahora se redondea hacia abajo al ratio
   limpio 1/n más cercano con el nuevo helper `PanelScale.snap()`, en vez de
   usarse como float arbitrario. Esto no es un cambio de filtro (`blur` no
   se tocó en esta ronda, por pedido explícito del usuario de no seguir
   alternándolo sin prueba comparativa) — es una corrección de **qué** ratio
   se le pide al filtro que procese, no de **cómo** filtra.
3. **Overlay F8 ampliado**: añadidas las pruebas D1/D2, E1/E2, B1 (ver
   sección 3), diseño sin superposición.
4. **Texto permanente "F8: diagnóstico de nitidez" eliminado** de la vista
   normal del panel. F8 sigue funcionando como atajo de desarrollo, pero
   ahora sin ningún indicio visible mientras está apagado (apagado por
   defecto, como antes).

No se tocó ningún `.png.mcmeta` de producción en esta ronda (siguen en
`blur:true`, como quedaron en la ronda anterior) — solo se agregó un
`.mcmeta` nuevo para la copia de depuración `debug_icon_nearest.png.mcmeta`
(`blur:false`), que no se usa en ningún lugar de producción, solo en el
overlay F8.

## 5. Archivos modificados

- `panel/client/CatalogGrid.java` — `ICON_SCALE` de `1.7f` a `2` (entero),
  `CELL` de 34 a 40, `pad` calculado como entero.
- `panel/client/PanelScale.java` (nuevo) — `snap(float ideal)`: redondea un
  factor de escala continuo al ratio limpio 1/n más cercano (o 1.0 si cabe a
  tamaño nativo).
- `panel/client/SurvivalPanelScreen.java` — `panelBox()` usa
  `PanelScale.snap(...)`; overlay de diagnóstico F8 reescrito (6 pruebas, sin
  superposición); texto permanente de F8 eliminado de la vista normal.
- `panel/client/MissionsScreen.java` — `panelBox()` usa el mismo
  `PanelScale.snap(...)` que `SurvivalPanelScreen`, para consistencia.
- `panel/client/PanelTextures.java` — nueva constante
  `DEBUG_ICON_NEAREST` (copia de depuración, solo usada por el overlay F8).
- `textures/gui/panel_k/debug_icon_nearest.png` (nuevo, copia exacta de
  `icon_mobs.png`) + `debug_icon_nearest.png.mcmeta` (`blur:false`) — activo
  solo dentro del overlay F8, no se referencia desde ningún botón real.

No se modificó ningún PNG de producción ni ningún otro `.mcmeta` existente en
esta ronda.

## 6. Resoluciones usadas

Sin cambios respecto a la ronda anterior: íconos del panel 512×512, panel
1672×941, `sol_pe.png` 256×256 — todas con el unsharp mask ya aplicado en
`12_PANEL_K_CORRECCION_VISUAL.md`. La copia de depuración
`debug_icon_nearest.png` comparte exactamente los mismos 512×512 píxeles que
`icon_mobs.png` (es una copia binaria, no una regeneración), para que la
comparación D1/D2 aísle solo el filtro.

## 7. Resultado de compilación

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments`: **PASS — 128 objetivos, 135 catálogos** con mecánica,
límites de éxito/fracaso, temporizadores y acuses multijugador. Sin cambios
respecto a antes de esta ronda — la ruleta y el resto de sistemas no se
vieron afectados.

## 8. Pruebas pendientes en Minecraft (el usuario debe confirmarlas)

Esto sigue sin verificarse visualmente — no hay cliente gráfico en este
entorno:

1. Reiniciar Minecraft por completo (`gradlew runClient` desde cero) para
   evitar cualquier textura cacheada de una sesión previa.
2. Abrir el Panel K y confirmar que los huevos de aparición y demás ítems
   vanilla dentro de la cuadrícula de Mobs/Objetos ya se ven nítidos
   (pixelados, como en cualquier inventario vanilla), no borrosos.
3. Presionar **F8** y comparar, en una sola captura:
   - D1 vs D2 (mismo PNG, bilineal vs nearest) — confirma si el filtro
     actual (bilineal) es o no apropiado para los íconos del panel.
   - E1 vs E2 (ítem vanilla nativo vs escalado ×2 entero) — confirma que el
     factor entero resolvió el aliasing de los huevos de aparición.
   - B1 vs B2 (panel a escala en vivo vs 1:1) — confirma si el marco del
     panel ya se ve limpio con el ratio 1/n o si todavía necesita ajuste.
4. Confirmar que ninguna de las 6 pruebas del overlay se superpone ni queda
   tapada.
5. Confirmar que ya no aparece ningún texto de "F8: diagnóstico de nitidez"
   en la vista normal del panel (solo debe aparecer el overlay si se
   presiona F8 activamente).
6. Confirmar que los 4 botones, su orden, Misiones, Staff, Sol PE,
   comandos administrativos y la ruleta siguen funcionando exactamente igual
   que antes.

Si tras esto D1 (bilineal) se ve notablemente peor que D2 (nearest) en la
captura del usuario, eso sería evidencia directa y específica (no una
suposición) para decidir recién ahí si conviene volver `blur:false` en los
`.mcmeta` de producción — pero esa decisión queda para la próxima ronda, con
esa prueba como evidencia, no como otro cambio especulativo.
