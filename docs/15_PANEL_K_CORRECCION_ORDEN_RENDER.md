# Panel K — corrección del orden de renderizado (viewport tras el blur de fondo)

Fecha: 14 de octubre de 2026.

**Estado: causa real encontrada y confirmada por desensamblado de bytecode de
tres clases reales del jar de Minecraft 1.21.1 (no suposición), corrección de
una línea aplicada en las 3 pantallas afectadas, compila y construye limpio
(`gradlew compileJava`, `gradlew build`), `verifyJudgments` en verde (128
objetivos, 135 catálogos). Pendiente de confirmación visual del usuario en
Minecraft — no hay cliente gráfico en este entorno.** No se tocó la ruleta,
sus animaciones, sonidos, Inti, Staff, Sol PE, economía, misiones,
persistencia, networking, desbloqueos, ni el diseño/orden de los 4 botones.

## 1. Qué demostró la nueva prueba del usuario

Con el overlay F8 ampliado de la ronda anterior (`14_PANEL_K_SOLUCION_NITIDEZ.md`),
el usuario reportó algo que ninguna ronda anterior había podido aislar:

- Cuadrícula `fill`: nítida.
- Texto nativo: nítido.
- Fragmentos de `panel.png` en el diagnóstico (**incluido el fragmento B1, a
  la misma proporción de escala EN VIVO que usa el panel real**): nítidos.
- Ícono "Libro" en bilineal y en nearest (D1/D2): ambos nítidos.
- Huevo de aparición vanilla nativo y ampliado ×2 (E1/E2): ambos nítidos.
- **El panel principal, sus 4 botones y el catálogo detrás del
  diagnóstico: completamente borrosos.**

Esto es la prueba definitiva de que **ni el contenido del PNG, ni el filtro
(bilineal/nearest), ni la proporción de escala, ni el factor entero/fraccional
de los ítems son la causa** — ya se habían descartado uno por uno en las
rondas anteriores y esta ronda lo termina de confirmar, porque hasta el
fragmento B1 (mismo archivo, misma proporción EXACTA de reducción que usa el
panel real) se ve nítido quieto en el diagnóstico. La única variable que
queda es **algo que ocurre específicamente cuando se dibuja el panel
principal**, no cuando se dibuja el mismo contenido de forma aislada más
tarde en el mismo frame — exactamente la hipótesis de orden de renderizado
que planteó el usuario.

## 2. Investigación del orden real de llamadas (evidencia, no suposición)

Se desensambló (bytecode real, `javap -c -p`, mismo jar de Loom usado para
compilar) el método completo `GameRenderer.render(RenderTickCounter, boolean)`,
que es el que realmente invoca `Screen.renderWithTooltip` cada frame — no
`MinecraftClient` directamente, como se había asumido sin verificar en la
ronda `13`. Se confirmó la secuencia completa del frame:

```
RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight())  // <- una sola vez, al INICIO del frame
renderWorld(...)
postProcessor (mundo, ej. efectos de espectador) .render(...)
getFramebuffer().beginWrite(false)
... configura la matriz ortográfica de GUI (getScaleFactor) UNA sola vez ...
renderNausea / renderFloatingItem / InGameHud.render / Overlay.render
Screen.renderWithTooltip(...)        // <- aquí se dibuja TODO nuestro panel
updateNarrator() / renderAutosaveIndicator / ToastManager.draw / DrawContext.draw()
popMatrix / applyModelViewMatrix
```

**No hay ningún post-proceso ni blur después de `Screen.renderWithTooltip`** —
confirmado leyendo cada `invoke*` del método completo, no solo un fragmento.
Esto descarta con evidencia más fuerte que antes la hipótesis de "algo
desenfoca el panel después de dibujarlo".

### La causa real: el viewport, no un post-proceso posterior

Dentro de nuestro propio `render()`, llamamos primero a
`this.renderBackground(...)` (heredado de `Screen`), que internamente hace
(ya confirmado en la ronda `13`):

```
Screen.renderBackground() -> Screen.applyBlur(delta) -> GameRenderer.renderBlur(delta)
    -> blurPostProcessor.render(delta)         // dibuja el blur del MUNDO de fondo
    -> client.getFramebuffer().beginWrite(false)
```

Lo que **no** se había verificado en ninguna ronda anterior es qué hace
exactamente `blurPostProcessor.render(delta)` con el **viewport** de OpenGL
internamente, ni qué significa el parámetro `false` de `beginWrite`. Se
desensambló esta ronda:

1. `PostEffectProcessor.render(float)` delega cada paso del shader de blur a
   `PostEffectPass.render(float)`.
2. `PostEffectPass.render(float)` (bytecode real, confirmado):
   ```
   RenderSystem.viewport(0, 0, output.textureWidth, output.textureHeight)
   ```
   fija el viewport de GL al tamaño de **su propio framebuffer de salida**
   (el buffer interno del shader de blur), y **nunca lo restaura** al tamaño
   de la ventana antes de retornar — solo llama a `endWrite()`/`endRead()`
   (que desenlazan framebuffers, no tocan el viewport).
3. `Framebuffer.beginWrite(boolean setViewport)` → `bind(boolean)` (bytecode
   real, confirmado):
   ```
   glBindFramebuffer(...)
   if (setViewport) GlStateManager._viewport(0, 0, viewportWidth, viewportHeight)
   ```
   El parámetro **solo controla si se fija el viewport otra vez**. La llamada
   que hace `GameRenderer.renderBlur` es `client.getFramebuffer().beginWrite(false)`
   — con `false`. Es decir: **reenlaza el framebuffer correcto de la
   ventana, pero deja el viewport de GL exactamente como lo dejó el último
   paso del shader de blur** (el tamaño de su buffer interno, no el de la
   ventana real).

Minecraft restaura el viewport correcto **una sola vez, al principio del
frame**, antes de `renderWorld`. Nada lo vuelve a restaurar después de este
blur en particular. Como nuestro `render()` llama a `renderBackground()`
y **a continuación dibuja directamente el panel completo, los 4 íconos y el
catálogo**, todo ese contenido se dibuja con el viewport de GL
potencialmente equivocado — más pequeño o con otra proporción que el de la
ventana real — lo que produce exactamente el síntoma reportado: todo lo
dibujado en esa ventana (nuestro PNG **y** los ítems vanilla del catálogo,
por igual) se renderiza a una resolución física incorrecta y se percibe
borroso al presentarse, mientras que el overlay F8 (dibujado mucho más
tarde en el mismo frame, después de `super.render()`) ya no se ve afectado
porque para entonces el viewport ya no importa del mismo modo (la matriz de
proyección de GUI es independiente del viewport puntual de ese momento, pero
el *recorte físico* de la rasterización si lo usa — de ahí que el contenido
dibujado justo tras el blur sea el que sale mal).

Esto explica, con una sola causa, **todos** los síntomas reportados en las
tres rondas anteriores: por qué afectaba al panel completo, a los 4 íconos
y al catálogo (todo se dibuja en la misma ventana de código, justo después
de `renderBackground()`), por qué afectaba también a los huevos de
aparición vanilla (se dibujan en esa misma ventana, sin que ningún
`.mcmeta` nuestro tenga nada que ver), y por qué ningún cambio de filtro,
escala entera o unsharp mask lo solucionó nunca (ninguno tocaba la causa
real).

## 3. Corrección aplicada

Se agregó una clase de utilidad, `ViewportFix.java`, que restaura
explícitamente el viewport de GL al tamaño real de la ventana justo después
de `renderBackground()`:

```java
static void restoreAfterBackground(MinecraftClient client) {
    if (!enabled || client == null) return;
    var window = client.getWindow();
    RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
}
```

Esta es la misma llamada, con los mismos parámetros, que `GameRenderer`
usa al inicio de cada frame (confirmado por bytecode) — no es una
invención, es repetir la restauración que falta.

Se invoca inmediatamente después de `this.renderBackground(...)` en las 3
pantallas que lo llaman:

- `SurvivalPanelScreen.render()`
- `MissionsScreen.render()`
- `AdminPanelScreen.render()`

Incluye un interruptor oculto `ViewportFix.enabled` (alternado con **F9**,
solo dentro de `SurvivalPanelScreen`) para que el usuario pueda comparar, en
la misma sesión, el panel **con** y **sin** la corrección — es una
herramienta de verificación (prueba C que pidió el usuario: aislar el efecto
sin desactivar el blur de todo Minecraft), no un cambio permanente de
comportamiento: por defecto queda **encendido** (comportamiento corregido).
El estado del interruptor se muestra únicamente dentro del overlay F8 (nunca
en la vista normal).

## 4. Por qué no se tocó ningún PNG ni `.mcmeta` esta ronda

El usuario pidió explícitamente no repetir otra ronda de afilado/escalado de
texturas sin demostrar que fuera necesario. La evidencia de esta ronda
(fragmentos B1/D1/D2/E1/E2 nítidos en el diagnóstico, con el mismo contenido
y la misma proporción que el panel real) demuestra precisamente lo
contrario: los recursos y el filtro ya eran correctos. No se modificó
ningún archivo de `textures/gui/panel_k/` ni sus `.mcmeta` en esta ronda.

## 5. Archivos modificados

- `panel/client/ViewportFix.java` (nuevo) — `restoreAfterBackground(client)`
  y el interruptor `enabled` (F9).
- `panel/client/SurvivalPanelScreen.java` — llama a
  `ViewportFix.restoreAfterBackground(this.client)` justo después de
  `renderBackground(...)`; **F9** alterna `ViewportFix.enabled`; el overlay
  F8 ahora también muestra el estado del interruptor (solo visible con F8
  activo).
- `panel/client/MissionsScreen.java` — misma llamada tras
  `renderBackground(...)`.
- `panel/client/AdminPanelScreen.java` — misma llamada tras
  `renderBackground(...)`.

No se modificó `CatalogGrid.java`, `PanelLayout.java`, `PanelTextures.java`
ni `PanelScale.java` en esta ronda (sus correcciones de la ronda anterior —
escala entera de ítems, ratio limpio 1/n — se mantienen intactas y siguen
siendo correctas por sí mismas, independientemente de este bug de viewport).

## 6. Verificación de que no se perdió ninguna función

- Teclado/ratón/scroll/tooltips: no se tocó ningún método de entrada
  (`mouseClicked`, `mouseScrolled`, `keyPressed` solo gana una rama nueva
  para F9, sin alterar las existentes de F8 ni el resto).
- `super.render(...)` se sigue llamando exactamente en el mismo lugar que
  antes en las 3 pantallas — los widgets (botones, campo de búsqueda) no
  cambiaron de orden ni de comportamiento.
- Los 4 botones (Libro/Espada/Pergamino/Lupa), su orden y su diseño no se
  tocaron.
- Ruleta, Inti, constelaciones, Staff, Sol PE, misiones, persistencia y
  networking: sin cambios, no se tocó ningún archivo de esos sistemas.

## 7. Resultado de compilación

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat :apuyawar-systems:build          # BUILD SUCCESSFUL
```

`verifyJudgments`: **PASS — 128 objetivos, 135 catálogos**, sin cambios
respecto a antes de esta ronda.

## 8. Comprobación visual pendiente (el usuario debe confirmarla)

Sin cliente gráfico en este entorno, esto sigue sin verificarse visualmente:

1. Reiniciar Minecraft por completo (`gradlew runClient`) para descartar
   cualquier estado cacheado de una sesión anterior.
2. Abrir el Panel K normal (sin F8) y confirmar si el marco dorado, los 4
   íconos y el catálogo (incluidos los huevos de aparición) ya se ven
   nítidos, igual que los elementos del diagnóstico F8 en la ronda anterior.
3. Si **todavía** se ve borroso: presionar **F9** (sin F8) para apagar
   `ViewportFix` y comprobar que el panel vuelve a verse exactamente como
   antes de esta corrección (confirma que el interruptor realmente actúa
   sobre la causa que se identificó, aunque no la resuelva del todo) —
   presionar F9 de nuevo para reactivarlo.
4. Presionar **F8** y comparar una vez más contra los fragmentos de
   diagnóstico; ahora el estado de `ViewportFix` (ON/OFF) aparece debajo de
   las 6 pruebas.
5. Confirmar que los 4 botones, Misiones, el Panel Administrativo, Staff,
   Sol PE, comandos y la ruleta siguen funcionando exactamente igual.

Si tras esto el panel sigue borroso incluso con `ViewportFix` en ON, eso
sería evidencia de que existe una SEGUNDA causa adicional (no solo el
viewport) actuando en paralelo, y la siguiente ronda debería partir del
estado del viewport real en tiempo de ejecución (ej. registrando
`GlStateManager`/`RenderSystem` o insertando un `glGetIntegerv(GL_VIEWPORT)`
de diagnóstico) en vez de otra ronda de cambios en las texturas.
