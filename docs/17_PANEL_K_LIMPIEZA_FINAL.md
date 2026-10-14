# Panel K — limpieza final: diagnósticos retirados, configuración definitiva

Fecha: 14 de octubre de 2026.

**Estado: el usuario confirmó visualmente en Minecraft que el Panel K se ve
nítido** con la corrección de doble `renderBackground()` (commits `09170e2`,
`66ccd22`, `90eae29`) y la combinación F9 ON / F10 ON / F12 ON / F7 OFF. Esta
ronda convierte esa combinación en el comportamiento permanente del código y
retira toda la instrumentación temporal (F7/F8/F9/F10/F12) que ya cumplió su
propósito. Compila y construye limpio (`gradlew clean build`),
`verifyJudgments` en verde (128 objetivos, 135 catálogos). No se tocó la
ruleta, Inti, Staff, Sol PE, misiones, economía, networking, los catálogos,
ni el diseño/orden de los 4 botones.

## 1. Configuración permanente adoptada

| Prueba temporal | Config. confirmada | Cómo quedó en el código |
|---|---|---|
| F9 (ViewportFix) | ON | `ViewportFix.restoreAfterBackground()` sin interruptor — siempre se ejecuta. |
| F10 (sin blur de fondo) | ON | `render()` dibuja directamente `context.fill(0,0,width,height,0xCC000000)` en vez de llamar al `renderBackground()` real; nunca hay blur de mundo detrás del panel. |
| F12 (panel nearest) | ON | `panel.png.mcmeta` cambió de `blur:true` a `blur:false` en el recurso de producción. Ya no existe `debug_panel_nearest`: es la textura real la que ahora usa nearest. |
| F7 (escala sin redondear) | OFF | `panelBox()` volvió a llamar directamente a `PanelScale.snap(ideal)` (sin `rawMode`/`resolve`) — el tamaño redondeado a 1/n, que el usuario prefirió visualmente. |

### Sobre la restauración del viewport (F9)

Se evaluó explícitamente si seguía siendo necesaria, como pidió el usuario.
Dado que ninguna de las 3 pantallas invoca ya el `renderBackground()` real
(con blur) en ningún punto — ni como primer fondo (ahora `fill()` plano) ni
en la llamada oculta de `super.render()` (bloqueada, ver sección 2) — el
blur que dejaba el viewport de OpenGL mal nunca se ejecuta en este flujo.
**Ya no es estrictamente necesaria para este bug.** Se conserva de todas
formas porque el usuario pidió conservarla y porque es una sola llamada
idempotente sin costo real: una red de seguridad barata si algún cambio
futuro reintroduce una llamada real al blur antes de dibujar el panel.
`ViewportFix.enabled` (el interruptor F9) se eliminó; la restauración ahora
es incondicional.

### Sobre el filtro nearest del panel (F12)

Solo se aplicó a `panel.png` (lo único que el interruptor F12 original
comparaba). Los 4 íconos (`icon_mobs/items/missions/search.png.mcmeta`)
siguen en `blur:true`, exactamente como estaban durante la combinación que
el usuario confirmó nítida — no se tocaron, porque F12 nunca los afectaba.

## 2. La corrección real se conservó intacta (y se simplificó)

El mecanismo que evita que `Screen.render()` vuelva a ejecutar
`renderBackground()` (y por tanto el blur) encima del panel ya dibujado
sigue activo en `SurvivalPanelScreen`, `MissionsScreen` y `AdminPanelScreen`.
Se simplificó: como ahora **ninguna** de las 3 pantallas quiere el
`renderBackground()` real bajo ninguna circunstancia (el fondo de producción
es siempre el `fill()` oscuro plano, no solo durante la prueba F10), el
método sobrescrito ya no necesita la bandera condicional
`suppressBackgroundDuringWidgetRender` ni el `try/finally` alrededor de
`super.render()` — ahora es un no-op incondicional:

```java
@Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
}
```

Esto bloquea igual la llamada oculta que `Screen.render()` hace antes de
pintar los widgets (botones, campos de texto, el botón "Volver" de
Misiones), sin ningún estado mutable que gestionar. El `super.render(...)`
se sigue llamando exactamente en el mismo lugar que antes en las 3
pantallas.

## 3. Herramientas de diagnóstico retiradas

De `SurvivalPanelScreen.java`:
- Pantalla de comparación F8 completa (`renderDiagnostic`): cuadrícula de
  control A, texto nativo C, muestras B1/B2 de `panel.png`, D1/D2 de ícono
  bilineal/nearest, E1/E2 de ítem vanilla nativo/escalado, y las muestras
  grandes G-antes/G-después con sus constantes de layout (`DX/DY/COL_W/
  ROW_H/DIAG_ICON_SIZE/G_SIZE/GX/GY_ANTES/GY_DESPUES`).
- El helper `drawPanelSample(...)`.
- `logViewport(...)` y sus 3 puntos de registro por frame (`GL11.glGetIntegerv`
  vía LWJGL) — ya no hay registros repetidos de viewport en el log.
- Los campos `diagnosticMode` (F8), `skipBlurBackground` (F10),
  `useNearestPanelDebug` (F12), y las 4 ramas de `keyPressed` para
  F7/F8/F9/F10/F12. El método `keyPressed` completo se eliminó (no queda
  ninguna tecla propia que interceptar).
- Imports ya no usados: `org.lwjgl.opengl.GL11`, `org.slf4j.Logger`,
  `org.slf4j.LoggerFactory`.

De `PanelScale.java`: el campo `rawMode` y el método `resolve(float)`;
`snap(float)` es otra vez el único método de la clase.

De `ViewportFix.java`: el campo `enabled` (F9); `restoreAfterBackground`
ahora es incondicional.

De `PanelTextures.java`: las constantes `DEBUG_ICON_NEAREST` y
`DEBUG_PANEL_NEAREST`.

Recursos eliminados (no solo de producción — los archivos ya no existen):
- `textures/gui/panel_k/debug_icon_nearest.png` + `.png.mcmeta`.
- `textures/gui/panel_k/debug_panel_nearest.png` + `.png.mcmeta`.

## 4. Decisión no pedida explícitamente: fondo plano también en Missions/Admin

El interruptor F10 original solo existía en `SurvivalPanelScreen`. Al hacer
permanente "el fondo oscuro sin blur", se aplicó el mismo tratamiento a
`MissionsScreen` y `AdminPanelScreen` (que antes de esta ronda seguían
llamando al `renderBackground()` real como primer fondo, con el mundo
difuminado detrás) — por consistencia visual entre las 3 pantallas del
Panel K, siguiendo el mismo criterio que ya se usó para mantener la misma
escala (`PanelScale.snap`) entre `SurvivalPanelScreen` y `MissionsScreen`
desde la ronda de `12_PANEL_K_CORRECCION_VISUAL.md`. El usuario solo probó
esta combinación en el panel de supervivencia (las teclas F7-F12 nunca
existieron en Misiones ni en el Panel Administrativo); si al probar
Misiones o el Panel Administrativo prefiere el mundo difuminado detrás en
vez del fondo oscuro plano, es un cambio de una línea por pantalla.

## 5. Archivos modificados

- `panel/client/SurvivalPanelScreen.java` — reescrito: sin diagnósticos, fondo
  plano fijo, `renderBackground()` no-op, `panelBox()` con `snap` directo.
- `panel/client/MissionsScreen.java` — `renderBackground()` no-op (sin
  bandera), fondo plano fijo, `panelBox()` con `snap` directo.
- `panel/client/AdminPanelScreen.java` — `renderBackground()` no-op (sin
  bandera), fondo plano fijo.
- `panel/client/PanelScale.java` — solo `snap(float)`.
- `panel/client/ViewportFix.java` — restauración incondicional, sin `enabled`.
- `panel/client/PanelTextures.java` — sin las 2 constantes de depuración.
- `textures/gui/panel_k/panel.png.mcmeta` — `blur:true` → `blur:false`.
- Eliminados: `debug_icon_nearest.png(.mcmeta)`, `debug_panel_nearest.png(.mcmeta)`.

No se modificó ningún otro archivo de `textures/gui/panel_k/`, ni
`PanelLayout`, `CatalogGrid`, `MobCatalog`, `ItemCatalog`, `MissionCatalog`,
`MissionEvents`, `MissionState`, `StaffCommands`, `StaffState`, `ModItems`,
ni ningún archivo de `roulette/`, `judgment/` o `client/judgment/`.

## 6. Pruebas realizadas

```powershell
.\gradlew.bat clean build
```

`BUILD SUCCESSFUL`. `verifyJudgments`: **PASS — 128 objetivos, 135
catálogos**, sin cambios.

Verificado además:
- `grep` de los 20+ símbolos de depuración retirados (`diagnosticMode`,
  `skipBlurBackground`, `useNearestPanelDebug`, `ViewportFix.enabled`,
  `PanelScale.rawMode`/`resolve`, `DEBUG_ICON_NEAREST`/`DEBUG_PANEL_NEAREST`,
  las 5 teclas F7/F8/F9/F10/F12, `renderDiagnostic`, `drawPanelSample`,
  `logViewport`) sobre todo `apuyawar-systems/src/`: cero coincidencias.
- El jar final (`apuyawar-systems-0.1.0.jar`, tras `clean build`) ya no
  contiene `debug_icon_nearest`/`debug_panel_nearest` en ningún punto
  (confirmado inspeccionando el jar con `jar tf`, no solo los recursos
  fuente — una compilación incremental anterior había dejado una copia
  vieja en el jar cacheado hasta que se forzó `clean`).
- `git diff` confirmó que `PanelLayout`, `CatalogGrid`, `MobCatalog`,
  `ItemCatalog`, `missions/`, `staff/`, `items/`, `roulette/`,
  `judgment/`, `client/judgment/` no tienen ningún cambio en esta ronda.

## 7. Pendiente de confirmación visual del usuario

Lo único que no se puede verificar sin un cliente gráfico es que, al
presionar **K** sin tocar F7/F8/F9/F10/F12, el panel aparezca exactamente
tan nítido como en la combinación que el usuario ya confirmó manualmente.
El usuario debe:

1. Reiniciar Minecraft por completo (`gradlew runClient`).
2. Presionar **K** sin pulsar ninguna tecla de función — confirmar que el
   panel, los 4 íconos y el catálogo se ven nítidos de entrada.
3. Confirmar que los 4 botones (Libro/Espada/Pergamino/Lupa → Mobs/Objetos/
   Misiones/Búsqueda), Misiones y el Panel Administrativo (Staff) siguen
   funcionando igual.
4. Confirmar que Sol PE sigue apareciendo en el inventario creativo y que
   la pestaña Economía del panel administrativo lo sigue entregando.
5. Si entra como Staff en Creative (Panel Administrativo) o abre Misiones,
   avisar si el fondo oscuro plano (en vez del mundo difuminado) se ve bien
   ahí también, o si prefiere el blur de mundo en esas dos pantallas (ver
   sección 4).
