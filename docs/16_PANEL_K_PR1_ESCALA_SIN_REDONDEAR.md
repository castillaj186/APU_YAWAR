# Panel K — comparación A/B de escala (revisión de la PR #1)

Fecha: 14 de octubre de 2026.

**Estado: compila y construye limpio (`gradlew compileJava`, `gradlew build`),
`verifyJudgments` en verde (128 objetivos, 135 catálogos). Herramienta de
comparación añadida y corrección de posición aplicada, pero SIN confirmación
visual en Minecraft todavía — no hay cliente gráfico en este entorno.** No se
tocó la ruleta, Inti, Staff, Sol PE, misiones, economía, networking ni el
diseño/orden de los 4 botones.

## 1. Lo que reveló la revisión de la PR #1

Con la instrumentación de `6cf8095` (viewport real + prueba G), el usuario
probó en su cliente (GUI lógica 480x253, escala 4) y reportó dos hallazgos:

- Los tres puntos registrados del viewport real (A, B, C) midieron
  `1920x1009`, igual al framebuffer. **No hay evidencia de un viewport
  incorrecto en esta prueba** — `ViewportFix` está dejando el viewport en el
  valor esperado; esa hipótesis queda descartada como causa activa en este
  caso concreto (no se reabre, pero tampoco explica ya nada por sí sola).
- `PanelScale.snap()` desaprovecha resolución en GUI pequeñas: con ese
  480x253, el factor ideal real es `≈0.247` (justo debajo de 1/4 = 0.25), y
  `snap()` lo redondea **hacia abajo** hasta el siguiente 1/n limpio, que es
  `1/5 = 0.20` — no `1/4`. El panel queda en `334x188` en vez de un posible
  `418x235`, y los 4 íconos se dibujan notablemente más chicos que las
  muestras del propio overlay F8 (que no pasan por `panelBox()`).
- Las muestras G-antes/G-después del overlay F8 estaban fijas en `y=300`,
  fuera de una GUI de solo 253 de alto — invisibles, no solo borrosas.

## 2. Cambios aplicados

### A. Comparación A/B real entre `snap(ideal)` y `ideal` sin redondear

`PanelScale.java`:
- Nuevo campo `rawMode` (apagado por defecto) y nuevo método
  `resolve(float ideal)`: devuelve `snap(ideal)` salvo que `rawMode` esté
  activo, en cuyo caso devuelve `ideal` tal cual, sin redondear a 1/n.
- `snap()` no se tocó — sigue siendo el comportamiento por defecto y la
  única ruta usada hasta que el usuario encienda la prueba explícitamente.

`SurvivalPanelScreen.panelBox()` y `MissionsScreen.panelBox()` (ambas, para
que las dos pantallas respondan igual, como ya exigía el comentario previo
de `panelBox()`): `PanelScale.snap(ideal)` → `PanelScale.resolve(ideal)`.

Nueva tecla oculta **F7** en `SurvivalPanelScreen` (apagada por defecto,
mismo patrón que F9/F10/F12) que alterna `PanelScale.rawMode` en vivo. No se
tocó ningún PNG ni `.mcmeta` — es exclusivamente un cambio de **qué** factor
de escala se calcula, no de cómo se filtra la textura resultante (pedido
explícito del usuario: mantener los mismos PNG y filtros durante esta
prueba).

El overlay F8 ahora imprime una línea con los valores reales en vivo:
`ideal`, `snap`, el factor realmente `usado` (que coincide con uno de los dos
según F7), el tamaño de caja resultante y el tamaño lógico de GUI — la
comparación numérica pedida, no solo visual.

### B. Reposición de las muestras G-antes/G-después

Antes fijas en `(12, 300)` a 110px de destino — invisibles en una GUI de 253
de alto. Se movieron a una tercera columna propia (`GX`, a la derecha de las
dos columnas existentes), con `G_SIZE = 80` en vez de 110, en
`GY_ANTES`/`GY_DESPUES` (una debajo de la otra). Para hacer sitio también se
redujo `ROW_H` de 56 a 46 y el tamaño de los íconos D1/D2 de 44 a 34; ninguna
otra prueba (A, B1/B2, E1/E2) cambió de tamaño ni de contenido. El recorte de
origen de panel.png que usan G-antes/G-después sigue siendo el mismo
(220×220 texels desde `u=700,v=0`) — solo cambió dónde y a qué tamaño de
destino se dibuja en pantalla, no qué se compara.

Con estos valores el overlay completo (las 4 filas, la columna G, y las 4
líneas de estado) cabe en aproximadamente `448x244` lógicos. Se verificó a
mano contra el caso reportado (480x253) con margen (~32px de ancho, ~9px de
alto libres). **No es un cálculo dinámico según el tamaño real de la
pantalla** — son constantes fijas revisadas para este caso; una GUI todavía
más pequeña que 480x253 podría volver a recortar algo, pero no hay reporte
de ese caso.

## 3. Archivos modificados

- `panel/client/PanelScale.java` — `rawMode` + `resolve(float)`.
- `panel/client/SurvivalPanelScreen.java` — `panelBox()` usa `resolve`;
  tecla F7; constantes de layout del overlay F8 (`DX/DY/COL_W/ROW_H/
  DIAG_ICON_SIZE/G_SIZE/GX/GY_ANTES/GY_DESPUES`) extraídas y ajustadas;
  `renderDiagnostic()` reposiciona G-después y añade la línea de
  comparación numérica F7.
- `panel/client/MissionsScreen.java` — `panelBox()` usa `resolve` (idéntico
  a `SurvivalPanelScreen`, sin lo cual las dos pantallas se verían a
  distinta escala si el usuario prueba con F7 encendido).

No se modificó ningún PNG, `.mcmeta`, `PanelLayout`, `PanelTextures`,
`CatalogGrid` ni `ViewportFix` en esta ronda.

## 4. Resultado de compilación

```powershell
.\gradlew.bat :apuyawar-systems:compileJava   # BUILD SUCCESSFUL
.\gradlew.bat build                            # BUILD SUCCESSFUL
```

`verifyJudgments`: **PASS — 128 objetivos, 135 catálogos**, sin cambios
respecto a antes de esta ronda — la ruleta y el resto de sistemas no se
vieron afectados.

## 5. Pruebas pendientes en Minecraft (el usuario debe confirmarlas)

Sin cliente gráfico en este entorno, nada de esto se verificó visualmente:

1. Reiniciar Minecraft por completo (`gradlew runClient`) para descartar
   cualquier textura/estado cacheado de una sesión anterior.
2. Abrir el Panel K y presionar **F8**; confirmar que ahora se ven las 4
   filas, la columna G (antes/después) y las 4 líneas de estado completas,
   sin que nada quede fuera de pantalla ni superpuesto.
3. Leer la nueva línea "F7 escala sin redondear" con F7 en OFF: debería
   mostrar `ideal≈0.247 snap≈0.200 usado≈0.200` (o los valores reales de tu
   GUI) — confirma el desperdicio de resolución reportado.
4. Presionar **F7** (con F8 activo para ver los números) y comparar:
   - La línea debería cambiar a `usado≈ideal` (sin redondear).
   - El panel, sus 4 íconos y el catálogo deberían verse más grandes.
   - Evaluar si se ven igual de nítidos, más nítidos, o si reaparece algún
     patrón "sucio" de muestreo irregular (la razón original de `snap`).
5. Confirmar que G-antes y G-después (ahora en la columna derecha del
   overlay) se ven, están en la misma posición relativa entre sí, y no se
   superponen con nada.
6. Confirmar que los 4 botones, Misiones, el Panel Administrativo, Staff,
   Sol PE, comandos y la ruleta siguen funcionando exactamente igual que
   antes (F7 solo afecta `panelBox()`, nada más).

No se declara resuelto el aprovechamiento de resolución, ni mejorada o
empeorada la nitidez, hasta que el usuario confirme con F7 en Minecraft real.
Si F7 en ON se ve claramente mejor sin introducir un patrón de muestreo
sucio, la siguiente ronda debería considerar hacer ese comportamiento el
predeterminado (quizás con un `snap` más permisivo, ej. redondear al más
cercano en vez de siempre hacia abajo, en lugar de eliminar el redondeo por
completo) — decisión que queda pendiente de esa evidencia visual, no de otra
ronda de suposiciones.
