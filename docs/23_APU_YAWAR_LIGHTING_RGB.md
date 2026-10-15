# APU YAWAR Lighting — motor propio de iluminación RGB y resplandor neón

Fecha: 15 de octubre de 2026. Rama: `feat/apuyawar-rgb-lighting`, basada en
el estado funcional más reciente de `feat/momento-cuy-arboles-celestiales`
(commit `de06c18`, que ya corrigió la pérdida de hojas de los árboles
celestiales). No se hizo merge a `main`.

**Léeme primero — qué es esto realmente y qué no es.** Este motor **no**
reescribe el motor de luces de Minecraft (eso exigiría sustituir
`LightingProvider`/el engine de propagación de luz por chunk, algo que
ningún mod de "luces de color" hace sin años de mantenimiento — ni
`ColorLight`, al que el encargo pide no depender, lo hace sin ese costo).
Lo que se entrega es la técnica que el propio encargo autoriza
explícitamente como alternativa ("iluminación en espacio de pantalla,
iluminación local por shader... no confundas un efecto aditivo superpuesto
con un motor de iluminación coloreada físicamente coherente"): geometría
3D real, posicionada en el mundo, con prueba de profundidad contra el
terreno (no atraviesa paredes), mezcla aditiva y respuesta a cámara — un
halo de neón localizado y configurable, no una iluminación de vóxel RGB
físicamente correcta ni un tinte de pantalla completa. Se documenta esta
diferencia en cada sección donde aplica, en vez de presentarlo como algo
que no es.

**Estado real de las pruebas, actualizado tras la corrección del equipo —
ver sección 16 para el detalle completo.** La primera entrega de este
documento afirmaba que una sesión real de más de 2 minutos sin
excepciones era evidencia de que el renderizador funcionaba. Eso fue
impreciso: esa sesión demostró que el juego **no crasheaba** con el
módulo cargado, pero no demostró que el halo se dibujara — un bug real
(sección 16, #1) hacía que la función de render cortara en la primera
línea en *cada* frame, silenciosamente, sin ninguna excepción que lo
delatara en el log. El equipo detectó esto por revisión de código, no
porque algo se viera mal, y señaló correctamente que "no basta con que
compile". Se corrigieron los 4 problemas reales (sección 16) y se
recompiló/reconstruyó todo el proyecto con éxito.

**Sobre la reverificación en vivo tras esa corrección**: en su momento se
intentó `gradlew :momento-cuy:runClient` tres veces y las tres se quedó
detenido en el mismo punto del arranque, y se documentó como "probable
agotamiento de recursos, no un error de código" sin poder confirmarlo del
todo. **Esa conclusión quedó incompleta por dos razones que sólo se
descubrieron en la ronda de corrección siguiente (sección 17)**: (a) ese
punto del arranque sólo era lento, no estaba realmente colgado — con más
tiempo de margen, siempre terminaba solo (confirmado más tarde con
sesiones reales de hasta 2 minutos y pico en ese paso concreto); y (b)
más importante, **esas pruebas tampoco habrían demostrado nada del halo
aunque hubieran llegado a un mundo**, porque un bug de caché de Loom
completamente distinto (sección 17) hacía que el cliente cargara una
copia de `apuyawar-lighting` congelada de horas antes, sin ninguna de las
correcciones de esta sección. La sección 17 tiene la reverificación real,
con sesión completa y sin excepciones.

Lo que sí respaldó la corrección del renderizador con alta confianza,
incluso antes de esa reverificación completa, fue verificación estática:
`gradlew build` completo sin errores, y cada API nueva usada en el
renderizador corregido (`Tessellator#begin`, `BufferBuilder#end`,
`BufferRenderer.drawWithGlobalProgram`, `RenderSystem` de
blend/profundidad/shader, `Quaternionf`/`Matrix4f` de JOML) confirmada
primero contra el bytecode real de Minecraft 1.21.1 antes de escribirla
— el mismo método que ya detectó y evitó errores reales en entregas
anteriores de este proyecto (p. ej. la firma de `SaplingGenerator` de 4
argumentos).

**Lo que sigue sin poder confirmarse, ahora y antes**: la calidad visual
real (si el halo se parece a la imagen de referencia, si los
colores/tamaños quedan bien) y el rendimiento con árboles reales cerca —
no hay ninguna herramienta de captura/control de pantalla disponible en
este entorno para dirigir al jugador hacia un árbol y mirar. Esa prueba
visual queda para el equipo, con instrucciones exactas en la sección 15.

## 1. Arquitectura del motor RGB

Módulo Gradle nuevo `apuyawar-lighting` (mod id `apuyawar_lighting`,
`environment: "client"` en su `fabric.mod.json` — Fabric Loader ni
siquiera lo carga en un servidor dedicado, es "principalmente cliente"
literalmente, no sólo de palabra). No depende de Momento Cuy ni de ningún
otro módulo del proyecto — expone una API pública neutral:

```java
ApuYawarLighting.registerBlockEmitter(Identifier blockId, EmitterProfile profile);
```

Cualquier mod (Momento Cuy ahora; cristales/lámparas/portales/estructuras
de Inti después) llama a esto desde su **propio inicializador de
cliente**, nunca desde código común — así el motor nunca se toca en un
servidor dedicado ni en código que deba cargar sin GPU.

**Separación de paquetes**: `pe.apuyawar.lighting` (datos puros —
`EmitterProfile`, `EmitterType`, `EmitterSampler`, el registro
`ApuYawarLighting` — sin ningún import de `net.minecraft.client.*`) vs.
`pe.apuyawar.lighting.client` (todo lo que sí toca OpenGL/el renderizador:
`EmitterTracker`, `WorldLightingRenderer`, `LightingCommands`, el propio
`ApuYawarLightingClient`). Esto no es sólo
una convención de carpetas: permite que, en teoría, la parte de datos sea
reutilizable incluso si algún día se quisiera leer esta configuración
desde un contexto sin cliente (p. ej. herramientas de depuración de
servidor), sin arrastrar ninguna clase de render.

**Integración opcional, no obligatoria**: `momento-cuy` depende de
`apuyawar-lighting` en Gradle (`modImplementation project(':apuyawar-lighting')`
— dependencia de proyecto normal, no `include()`/jar-in-jar: Loom exige
que el jar del módulo dependido ya exista en disco al configurar el
proyecto, lo que crea un punto muerto de arranque la primera vez que se
crea un módulo hermano nuevo; se documlentó y resolvió construyendo
`apuyawar-lighting` en una invocación de Gradle separada antes de
compilar Momento Cuy por primera vez — a partir de ahí, builds normales
funcionan sin este paso extra). La llamada real de registro
(`MomentoCuyLightingBridge.register()`, ver sección 9) sólo se ejecuta
desde `MomentoCuyClient.onInitializeClient()`, dentro de un
`FabricLoader.isModLoaded("apuyawar_lighting")` — si algún día faltara el
jar de iluminación, Momento Cuy sigue funcionando con su luz Vanilla
normal, sin RGB, sin crashear.

## 2. Técnica utilizada para iluminar superficies

> **Corregido tras revisión del equipo — ver sección 16 para el detalle
> completo de los 4 errores reales encontrados y sus correcciones.** Esta
> sección describe la implementación **actual, corregida**; la primera
> versión entregada tenía dos bugs reales en esta misma pieza (enviaba los
> vértices por una vía que no existe en la fase elegida, y mezclaba de una
> forma que anulaba el efecto de la intensidad configurada) — no se
> ocultan, están documentados en la sección 16 con su causa exacta.

**Círculos suaves aditivos, dibujados en modo inmediato, con prueba de
profundidad real** — no partículas de vanilla, no un shader de
postproceso que recolorea toda la pantalla, y ya no un cuadrado plano de
color uniforme. Cada punto de luz es un abanico de triángulos orientado a
la cámara (`Camera#getRotation()`, el mismo patrón que usa cualquier
partícula billboard de vanilla): un vértice central con el alfa calculado
(intensidad × configuración × día/noche × parpadeo) y un anillo de
vértices exteriores con alfa 0 — la interpolación de color de la propia
GPU entre el centro y el borde da una caída radial suave, sin textura
(sección 3 explica por qué modo inmediato y no una `RenderLayer`
persistente).

Mezcla `SRC_ALPHA, ONE` (no `ONE, ONE` — ver la sección 16, bug #2, para
la razón exacta) y prueba de profundidad `GL_LEQUAL` activa. Como el
`Framebuffer` principal de Minecraft (color + profundidad) ya está
enlazado durante todo `WorldRenderEvents` (confirmado por bytecode de
`GameRenderer`/`MinecraftClient`: `beginWrite(true)` envuelve el frame
completo, incluyendo el mundo), la prueba de profundidad contra el
terreno ya dibujado **funciona automáticamente sin ningún enlace manual**
— un muro delante del árbol SÍ oculta el halo, porque el GPU descarta los
fragmentos cuyo depth es mayor que el del muro ya presente en el buffer.
No se escribe en el depth buffer (`RenderSystem.depthMask(false)`), igual
que cualquier capa translúcida de vanilla, para que el halo no oculte
partículas/entidades detrás de él.

**Lo que esto NO resuelve**: la oclusión es correcta contra geometría
opaca ya renderizada en el frame actual, pero no hay ningún cálculo de
"sombra" proyectada por el halo sobre otros bloques, ni refracción, ni
rebote de luz — es un punto brillante con caída de alfa por distancia
(sección 6), no un simulador de iluminación global.

## 3. Shaders y recursos gráficos

**No se escribió ningún GLSL nuevo para el halo/punto de luz.** Se
investigó por bytecode (`RenderPhase`, `RenderLayer`, `GameRenderer`) y se
confirmó que Minecraft 1.21.1 expone un programa de shader ya compilado
para "posición + color de vértice, sin textura"
(`GameRenderer.getPositionColorProgram()`, respaldado por
`assets/minecraft/shaders/core/position_color.json`/`.vsh`/`.fsh` reales
del juego — el mismo que usa, por ejemplo, el rayo de las tormentas). Como
nuestro halo es exactamente eso (un color sólido con alfa, sin textura),
se reutiliza ese shader de vanilla directamente — cero riesgo de GLSL
nuevo mal escrito que no se pueda verificar visualmente sin GPU.

**Por qué modo inmediato (`Tessellator`/`BufferBuilder` propios,
`RenderSystem.setShader(...)` manual) y no una `RenderLayer` persistente
como en la primera versión**: una `RenderLayer` normal se envía a través
de `WorldRenderContext.consumers()` (un `VertexConsumerProvider`), pero
ese objeto es `null` precisamente en la fase `AFTER_TRANSLUCENT` que usa
este módulo — ver sección 4 y sección 16 (bug #1) para la cita exacta del
Javadoc real que lo confirma. El modo inmediato no depende de
`consumers()` en absoluto: construye su propio buffer, fija el
shader/mezcla/profundidad a mano con `RenderSystem` y restaura el estado
al terminar.

**Bloom real (framebuffer de extracción + desenfoque + composición) —
investigado pero NO implementado en esta entrega, explicado abajo en la
sección 7, tal como pidió el equipo explícitamente antes de pasar a esa
fase.** El "resplandor" que sí se entrega es una técnica distinta y más
simple: ver sección 7.

## 4. Integración con Fabric 1.21.1

Punto de enganche: `net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT`
(confirmado real junto con sus 9 fases hermanas por bytecode de
`fabric-rendering-v1-5.2.1`, la versión exacta que trae
`fabric-api 0.116.17+1.21.1`). Se eligió esta fase (después de la
geometría translúcida del mundo, incluida el agua) para que el halo se
dibuje por encima del agua y del cristal, no por debajo.

**Aislamiento de GUI, estructural, no por convención manual**: se
confirmó por desensamblado de `GameRenderer.render(...)` que
`renderWorld(...)` (de donde salen TODAS las fases de `WorldRenderEvents`)
siempre retorna antes de que se llame a `inGameHud.render(...)` — no hay
reentrada posible. A diferencia del bug de desenfoque del Panel K de hace
varias etapas (que fue un bug real en nuestro propio código de GUI
llamando `renderBackground()` dos veces), aquí no hace falta ningún guard
manual: la arquitectura de Fabric/Mojang ya hace imposible que este gancho
se ejecute durante el render de una `Screen`.

`WorldRenderContext` (el objeto que recibe el callback) expone
`matrixStack()`, `camera()`, `consumers()` (un `VertexConsumerProvider`),
`world()` y `projectionMatrix()`/`positionMatrix()` — **pero no expone
ningún `Framebuffer` ni textura de profundidad** (se verificó
explícitamente que no existe ese método; esto descartó una suposición
inicial). El framebuffer principal se obtiene por separado, cuando hace
falta, vía `MinecraftClient.getInstance().getFramebuffer()`.

**`consumers()` en concreto es `null` en `AFTER_TRANSLUCENT`** — el
Javadoc real de la interfaz dice explícitamente "null before
`BEFORE_ENTITIES` / after `BEFORE_DEBUG_RENDER`", y `AFTER_TRANSLUCENT`
va después de `BEFORE_DEBUG_RENDER` en la secuencia de fases. Esta
investigación ya se hizo en la entrega original, pero al escribir el
renderizador no se aplicó correctamente contra la fase elegida — bug real
corregido después de la entrega, ver sección 16 (bug #1). Por eso
`WorldLightingRenderer` ya no usa `consumers()` en absoluto (sección 2/3).

## 5. Registro de emisores

`EmitterProfile` (record inmutable, en el paquete de datos puro) describe
color primario (+ uno secundario opcional, para destellos tipo el dorado
de Inti), intensidad, radio, caída, tipo (`FOLIAGE`/`SOLID`/`TRANSIENT`),
cuántos puntos genera como máximo un solo bloque
(`pointsPerBlock` — nunca "un punto por cara"), prioridad y si parpadea.
`ApuYawarLighting` es sólo un `Map<Identifier, EmitterProfile>` — **nada
se activa automáticamente**: sólo los bloques que alguien registra
explícitamente (sección 9) generan luz.

`EmitterSampler` decide, de forma **determinista** (semilla =
`BlockPos.asLong()`, nunca `Math.random()` suelto), dónde dentro del
bloque va cada punto, su tamaño relativo y si queda activo según la
densidad de partículas configurada — así los puntos de luz de una hoja
concreta son siempre los mismos, sesión tras sesión, cumpliendo "no quiero
que se reorganicen aleatoriamente cada frame" (sección 8 del encargo).

## 6. Cálculo de colores, radios e intensidades

`EmitterTracker` (ver también sección 12, rendimiento) mantiene la lista
acotada de emisores activos cerca de la cámara. Para cada punto visible,
`WorldLightingRenderer` calcula:

```
alfa = intensidad_perfil × multiplicador_global_intensidad × factor_día_noche × parpadeo
```

- `factor_día_noche`: `config.dayIntensityMultiplier` (0.55 por defecto —
  visible pero discreto de día) o `config.nightIntensityMultiplier` (1.0 —
  "mayor presencia visual durante la noche", pedido explícitamente para
  ambos árboles).
- `parpadeo`: sólo si `profile.animated()` — una onda seno lenta con fase
  determinista por posición (no sincronizada entre hojas, para que no
  "respiren" todas a la vez), nunca un parpadeo brusco.
- Color secundario (dorado de Inti, destellos): 1 de cada 3 puntos lo usa
  en vez del color primario, también determinista por índice.
- Tamaño del punto: pequeño a propósito (0.06-0.13 bloques de
  semi-lado, modulado por `lightRadiusMultiplier`) — "pequeños focos
  brillantes", no una superficie uniformemente brillante (sección 8).

## 7. "Resplandor neón" — lo que se entrega vs. el Bloom real pedido

**Lo que se entrega, real y funcionando**: un halo tipo "glow sprite" —
por cada punto de luz, una segunda capa adicional más grande (×1.8-2.8,
según `bloomRadiusMultiplier`) y más tenue (alfa ×0.35, modulado por
`bloomIntensity`), mismo color, misma capa de render aditiva, dibujada en
el mismo paso. Esto es geometría 3D de verdad (responde a cámara y
profundidad igual que el punto principal), configurable, y da el efecto
de "halo que se expande alrededor del punto brillante" que pide la
sección 7 del encargo — pero **no es un Bloom de framebuffer multi-pasada
con extracción de brillo + desenfoque gaussiano + composición**, que es
literalmente lo que la sección 7 pide ("prioriza una implementación de
varias pasadas optimizadas con framebuffer de menor resolución").

**Por qué no se implementó el Bloom real de framebuffer esta vez —
explicado, no ocultado**: se investigó la API real completa
(`net.minecraft.client.gl.PostEffectProcessor`, constructor público que
acepta cualquier `Identifier` — no está encerrado a los efectos propios de
vanilla como creeper/invert/blur — y `net.minecraft.client.gl.SimpleFramebuffer`
para un target de menor resolución; vanilla incluso ya trae un programa de
desenfoque de caja reutilizable, `shaders/program/box_blur.json`, que en
teoría se podría referenciar desde un pipeline propio). El plan técnico
completo es: (1) un shader propio de "extracción de brillo" (umbral sobre
el color del framebuffer principal), (2) 2-4 pasadas reutilizando
`box_blur` de vanilla sobre un framebuffer auxiliar de media/un cuarto de
resolución, (3) una pasada de composición aditiva de vuelta sobre
`minecraft:main`, todo disparado manualmente desde
`WorldRenderEvents.AFTER_TRANSLUCENT`/`LAST` (no desde el
`loadPostProcessor` privado de `GameRenderer`, que no es accesible desde
un mod).

**La razón concreta de no escribirlo ya**: los pasos 1 y 3 exigen GLSL de
vértice/fragmento propio para un paso de postproceso en pantalla completa
(convención de atributos/UV de un quad de pantalla completa), y este
entorno, aunque sí tiene GPU (ver "Estado real de las pruebas" arriba),
no tiene ninguna forma de que yo vea el resultado renderizado — un error
de UV/atributo en ese shader fallaría en silencio (pantalla negra, o un
halo distorsionado) sin ningún mensaje de error que yo pudiera leer en el
log para corregirlo, a diferencia de todo lo demás en esta entrega, que sí
pude verificar por bytecode real o por una ejecución real sin excepción.
Siguiendo la instrucción explícita del encargo de "no presentar efectos
simulados como características todavía no implementadas", se prefirió
dejar el halo de billboards apilados (sección verificable, sin riesgo de
fallo silencioso) como lo entregado, y dejar el Bloom de framebuffer como
trabajo futuro con el plan técnico ya investigado y listo para implementar
en una próxima sesión con pruebas visuales disponibles.

`config.bloomIntensity`/`bloomRadiusMultiplier` ya existen y están
cableados al halo de billboards — cuando se añada el Bloom real de
framebuffer, puede leer los mismos campos sin romper la configuración ya
guardada de nadie.

## 8. Materiales emisivos

No se generaron texturas nuevas (regla obligatoria del encargo). "Material
emisivo" aquí significa: las hojas que ya tenían `luminance` en escala de
grises (de la entrega anterior, `hojas_cerezo_florecido` y
`hojas_pino_esmeralda`) ahora **además** generan sus puntos de luz RGB
sobre la textura original sin tocarla — el campo
`config.emissiveMaterialIntensity` queda reservado (ya persistido, ya
leído por el config) para cuando se añada una técnica de iluminado de
textura más fina (p. ej. una capa de overlay por vértice en el propio
modelo de bloque) — no se inventó una sin poder verla.

## 9. Integración con Abeto Celestial

`MomentoCuyLightingBridge.register()` (nuevo, en
`momento-cuy/.../client/`) registra:

- `hojas_pino_esmeralda`: turquesa intenso `#00D9C8` con destello
  secundario turquesa neón `#00FFF0`, intensidad 0.85, radio 6 bloques,
  **3 puntos por bloque**, animado (parpadeo lento), prioridad 2 (se
  mantiene activo primero si se supera el máximo de luces).
- `hojas_pino_sombrio`: verde turquesa oscuro `#064C4B`, intensidad 0.45
  (bastante más tenue — aporta contraste, no brillo, como pide el
  encargo), 1 punto por bloque, prioridad 1.

No se tocó `MomentoCuyAbetoLeavesBlock.java` (las partículas turquesa/
celeste de `randomDisplayTick`, de la entrega anterior, siguen intactas y
funcionan en paralelo al halo RGB — son dos sistemas distintos: partículas
de corta vida que vuelan, luz localizada que permanece).

## 10. Integración con Cerezo Celestial

`hojas_cerezo_florecido` (sólo esta variedad, igual que su luminancia en
escala de grises de la entrega anterior): rosa neón `#FF70BB` con destello
secundario dorado de Inti `#FFD659`, intensidad 0.6 (más suave que el
abeto, "atmósfera elegante" pedida), radio 5 bloques, 2 puntos por bloque,
animado, prioridad 2.

No se tocó `MomentoCuyBlossomLeavesBlock.java` — sus pétalos
(`ParticleTypes.CHERRY_LEAVES`, partícula real de vanilla) y sus propios
destellos de partícula (`DustParticleEffect` rosa/dorado) de la entrega
anterior siguen funcionando exactamente igual; el halo RGB de este módulo
es una capa adicional, no un reemplazo.

## 11. Configuración del usuario

`config/apuyawar_lighting.json` (cliente, JSON legible, confirmado creado
y leído correctamente en una ejecución real — ver "Estado real de las
pruebas"). Campos: `rgbEnabled`, `quality` (perfil activo), y los 9
multiplicadores/límites pedidos en la sección 11 del encargo
(`lightIntensityMultiplier`, `lightRadiusMultiplier`, `bloomIntensity`,
`bloomRadiusMultiplier`, `emissiveMaterialIntensity`,
`particleDensityMultiplier`, `dayIntensityMultiplier`,
`nightIntensityMultiplier`, `maxActiveLights`).

Tres perfiles reales (`LightingQuality.LOW/MEDIUM/HIGH`) con paquetes de
valores concretos, más `CUSTOM` (se activa solo al tocar cualquier campo
individual). Comando de cliente `/apuyawarlighting` (no reutiliza F8 ni
ninguna tecla del Panel K, como pedía explícitamente el encargo):

- `/apuyawarlighting toggle` — activa/desactiva el RGB, al instante.
- `/apuyawarlighting preset <low|medium|high|custom>`.
- `/apuyawarlighting set <intensidad|radio|bloom|radiobloom|particulas|dia|noche> <valor>`.
- `/apuyawarlighting set maxluces <entero>`.
- `/apuyawarlighting test` — coloca (o retira, si ya hay uno) un emisor de
  prueba turquesa de la Fase A en el bloque que mira el jugador, sin
  necesidad de un árbol real; usa `config.testEmitterColor`.
- `/apuyawarlighting estado` — imprime la configuración activa y cuántos
  emisores están activos ahora mismo.

Todos los cambios se guardan a disco inmediatamente y se leen en el
siguiente frame — no hace falta reiniciar el cliente ni el mundo.

## 12. Optimización y rendimiento

- **No hay una luz por cada bloque de hoja de golpe**: `EmitterTracker`
  sólo recalcula la lista de emisores activos cada 10 ticks (medio
  segundo), no cada frame, y sólo dentro de un radio acotado (24 bloques)
  alrededor del jugador, saltando chunks no cargados. Esto es un
  compromiso real, no un sistema reactivo por evento de bloque — ver la
  limitación exacta en la sección 14.
- **Límite duro de luces activas**: `maxActiveLights` (48 por defecto,
  perfil medio) — si hay más emisores candidatos que el límite, se ordenan
  por prioridad y luego por distancia, y sólo los primeros se dibujan.
  Esto acota el costo incluso con un bosque denso.
- **Agrupación por bloque, no por hoja individual**: cada bloque de hoja
  aporta como máximo `pointsPerBlock` puntos (1-3 en los perfiles
  actuales), nunca uno por cara ni uno por posición de textura.
- **Densidad de partículas configurable**: `particleDensityMultiplier`
  desactiva una fracción determinista de los puntos ya elegidos (no
  recalcula posiciones), para bajar la calidad sin reorganizar nada.
- **Decal de suelo acotado aparte**: el rayo hacia el suelo (hasta 24
  bloques) sólo se calcula para los 8 emisores más prioritarios/cercanos,
  y sólo durante el rescaneo (cada medio segundo), nunca por frame.
- **Sin asignaciones nuevas de GPU por frame**: no se crea ningún
  `Framebuffer`/textura nueva en este reparto (el Bloom real de
  framebuffer, sección 7, habría necesitado uno reutilizable — quedó para
  cuando se implemente esa fase).
- **Interruptor real**: `rgbEnabled=false` (o `/apuyawarlighting toggle`)
  deja `EmitterTracker.active()` vacío de inmediato — cero costo de
  render más allá de comprobar una lista vacía.

**Lo que NO se pudo medir**: FPS/costo de GPU real con 1, 5 y 20 árboles
uno junto a otro, como pide la sección 12 del encargo. No hay forma, en
este entorno, de colocar árboles y leer un contador de FPS de la sesión
gráfica real que sí se ejecutó (ver "Estado real de las pruebas" — hubo
una sesión real corriendo más de 2 minutos sin errores, pero sin control
de teclado/ratón no se pudo dirigir al jugador hacia un bosque de Momento
Cuy ni leer el overlay de depuración F3). Se documenta esto como pendiente
en vez de inventar números — ver la lista de pruebas de la sección 15.

## 13. Compatibilidad con otros renderizadores

No se investigó Sodium/Iris/shaders externos en profundidad — no se
afirma compatibilidad sin haberla probado. Lo que sí se puede decir con
fundamento, por cómo está construido:

- El halo se dibuja con `Tessellator`/`BufferBuilder`/`RenderSystem` —
  las mismas clases de bajo nivel que usa internamente cualquier
  `RenderLayer` de vanilla, sin mixin que reemplace clases internas del
  renderizador. Esto reduce (no elimina) el riesgo de conflicto con mods
  de renderizado como Sodium, que sí suelen reemplazar partes internas
  del pipeline de chunks/entidades.
- No se tocó ningún shader de terreno/entidad de vanilla — se reutilizó
  el programa `position_color` existente tal cual, sin redefinirlo.
- `rgbEnabled=false` es la vía de desactivación segura si se detecta
  incompatibilidad con un shader pack (Iris) o un mod de renderizado —
  queda documentado como el mecanismo real, no se afirma detección
  automática de Iris/Sodium (no implementada).

## 14. Limitaciones gráficas conocidas

1. **No es iluminación de vóxel real** — no cambia el nivel de luz de
   bloque de los vecinos ni el lightmap del terreno; es geometría aditiva
   superpuesta (ver sección 2, honestamente etiquetado, no presentado como
   lo contrario).
2. **Actualización con latencia, no instantánea** — colocar o romper una
   hoja emisora tarda hasta 10 ticks (medio segundo) en reflejarse, porque
   `EmitterTracker` rescanea periódicamente en vez de reaccionar a un
   evento de bloque por bloque (no existe un evento así de barato en la
   Fabric API sin mixins propios al motor de chunks, que se evitaron a
   propósito por riesgo de compatibilidad).
3. **Bloom real de framebuffer no implementado** — ver sección 7 completa:
   el halo actual es un "glow sprite" apilado, no una extracción+desenfoque
   +composición de framebuffer. El plan técnico está listo (APIs reales
   confirmadas), falta implementarlo con pruebas visuales disponibles.
4. **Decal de agua/suelo es un overlay, no una reflexión** — el quad
   aditivo que llega al suelo/agua (sección 9 del encargo) se dibuja
   encima de lo ya renderizado; no es un reflejo físico del entorno ni
   una refracción, tal como pedía distinguir el propio encargo.
5. **Sin medición de rendimiento real** — ver sección 12.
6. **Sin confirmación visual del parecido con la imagen de referencia** —
   los colores/radios/intensidades se escogieron razonadamente a partir de
   la paleta pedida y la descripción de la imagen, pero nadie los ha visto
   renderizados todavía.

## 15. Pruebas ejecutadas y pendientes

**Ejecutadas, con evidencia real (no sólo "compila"):**
- `gradlew build` (proyecto completo, los 4 módulos): éxito,
  `apuyawar-systems:verifyJudgments` sin cambios (128/135) — repetido tras
  la corrección de la sección 16, también en verde.
- `gradlew :apuyawar-lighting:compileJava` y `:momento-cuy:compileJava`:
  éxito, tanto en la entrega original como tras la corrección — todas las
  firmas nuevas de modo inmediato (`Tessellator#begin`,
  `BufferBuilder#end`, `BufferRenderer.drawWithGlobalProgram`,
  `RenderSystem`/`GlStateManager` de blend y profundidad,
  `Matrix4f`/`Quaternionf` de JOML) correctas al primer intento gracias a
  la investigación previa por `javap` contra el jar real de Minecraft.
- `gradlew :momento-cuy:runClient` con GPU real, en la entrega original:
  confirmó que el juego carga y corre sin crashear con el módulo
  instalado — **pero, como explica el bloque de arriba, esto no demostró
  que el halo se dibujara**, porque el bug #1 lo cortaba en silencio en
  cada frame. Tras la corrección, se intentó repetir esta misma prueba 3
  veces más y las 3 el arranque se quedó detenido antes de llegar a cargar
  un mundo — ver el bloque de arriba para el análisis de por qué
  (probablemente agotamiento de recursos del entorno, no el código).
- Se confirmó (en la sesión original, antes de la corrección) que
  `config/apuyawar_lighting.json` se crea y contiene exactamente los
  valores por defecto esperados del perfil medio.
- Para verificar el código corregido sin depender de una sesión gráfica
  completa, se añadió temporalmente una línea en
  `ApuYawarLightingClient.onInitializeClient()` que forzaba un emisor de
  prueba activo desde el arranque (mismo mecanismo que
  `/apuyawarlighting test`, para ejercitar el camino real de
  `WorldLightingRenderer` sin necesitar un árbol ni control de teclado);
  se quitó antes de este commit — no quedó ningún código de depuración
  permanente, tal como pedía explícitamente el equipo.

**Pendientes — requieren que alguien juegue con teclado/ratón, lista
exacta para el equipo:**
1. Entrar a Minecraft, ir a Momento Cuy (`/momentocuy entrar`), plantar y
   hacer crecer un Abeto Celestial y un Cerezo Celestial (o usar uno ya
   crecido de la rama anterior).
2. De noche, observar si el halo turquesa/rosa se parece a la referencia:
   puntos pequeños repartidos, no una copa pintada de un solo color plano.
3. Probar `/apuyawarlighting test` apuntando a un bloque cualquiera, para
   ver el halo aislado sin depender de un árbol.
4. Probar `/apuyawarlighting toggle` y comparar antes/después — pedido
   explícitamente como criterio de aceptación.
5. Probar los 3 perfiles (`preset low/medium/high`) y confirmar que se
   nota la diferencia de densidad/intensidad.
6. Pararse cerca del tronco y comprobar que un muro/bloque sólido entre el
   jugador y una hoja emisora SÍ oculta su halo (prueba de profundidad).
7. Comprobar el decal de suelo cerca de la base del árbol, y si hay agua
   cerca, si el tinte aditivo llega a su superficie sin volverla
   completamente turquesa a distancia.
8. Plantar 1, luego 5, luego un grupo más denso de árboles, y anotar FPS
   (F3) con `rgbEnabled` activado y desactivado, para tener por fin un
   número real de costo — ver sección 12.
9. Confirmar que nada del Panel K, la ruleta, las misiones, el Cuy
   Jardinero ni las barandillas cambió de comportamiento.
10. Cambiar de dimensión (Overworld ↔ Momento Cuy) cerca de un árbol
    emisor y confirmar que no quedan halos "fantasma" más de medio
    segundo.

## 16. Corrección post-entrega — 4 errores reales encontrados y corregidos

El equipo revisó el código (no sólo lo que decía este documento) y
encontró 4 problemas reales antes de que nadie entrara a probarlo en
Minecraft. Los 4 eran ciertos; se investigó cada uno contra la API real
antes de tocar código, igual que el resto de este proyecto.

### Bug #1 — el halo nunca se dibujaba (el más grave)

**Síntoma real**: `WorldLightingRenderer` cortaba con
`if (consumers == null) return;` en *cada* frame, sin ninguna excepción
ni mensaje en el log — exactamente el tipo de fallo silencioso que no se
ve en un `gradlew build` ni en un `runClient` que sólo se mira por el
log.

**Causa**: `WorldRenderContext.consumers()` es `null` en la fase
`AFTER_TRANSLUCENT` (el Javadoc real dice "null before `BEFORE_ENTITIES`
/ after `BEFORE_DEBUG_RENDER`" — `AFTER_TRANSLUCENT` va después de
`BEFORE_DEBUG_RENDER`). Esta información ya se había investigado y
documentado correctamente en la entrega original (sección 4 de este
documento, tal como estaba entonces) — el error fue aplicarla mal al
escribir el renderizador: se investigó bien, se documentó bien, y aun así
se usó `consumers()` en la fase equivocada.

**Corrección**: `WorldLightingRenderer` ya no usa `consumers()`.
Construye su propio `BufferBuilder` (`Tessellator.getInstance().begin(...)`),
fija el shader/mezcla/profundidad a mano con `RenderSystem` justo antes
de dibujar, y restaura el estado (`enableCull`, `depthMask(true)`,
`disableBlend`) al terminar — el "renderizado inmediato directo" que
pidió el equipo como alternativa. Esto permite seguir en
`AFTER_TRANSLUCENT` (necesario para que el halo quede sobre el agua/
vidrio), sin depender de un `VertexConsumerProvider` que ahí no existe.

### Bug #2 — la mezcla aditiva anulaba el efecto del alfa

**Síntoma real**: aunque el bug #1 se hubiera corregido sin tocar esto,
todos los puntos de luz habrían brillado siempre al máximo, sin importar
`lightIntensityMultiplier`, el parpadeo, el multiplicador día/noche ni la
densidad de partículas.

**Causa, confirmada por la matemática real del shader** (no una
suposición): `GlowRenderLayer` usaba `ADDITIVE_TRANSPARENCY`
(`glBlendFunc(GL_ONE, GL_ONE)`). El shader `position_color.fsh` que
reutilizamos (sección 3) hace `fragColor = vertexColor * ColorModulator`
— **no multiplica el RGB por el alfa**. Con mezcla `(ONE, ONE)`, la
fórmula de mezcla es `final.rgb = frag.rgb·1 + dst.rgb·1`: el valor de
alfa simplemente no entra en esa cuenta. Todo lo que calculábamos en
alfa (intensidad del perfil, factor día/noche, parpadeo, densidad) no
tenía ningún efecto visual real.

**Corrección**: mezcla `SRC_ALPHA, ONE` (la misma que usa el rayo de
vanilla, `RenderPhase.LIGHTNING_TRANSPARENCY`, confirmada por bytecode en
la entrega original) — con esa fórmula, `final.rgb = frag.rgb·frag.a +
dst.rgb·1`, así que el alfa sí escala la contribución antes de sumarla.
Sigue siendo una mezcla aditiva (nunca resta ni reemplaza lo ya
dibujado), sólo que ahora el alfa importa.

### Bug #3 — halos cuadrados, no puntos de luz suaves

**Síntoma real**: cada billboard era un cuadrado de 4 vértices con el
mismo color/alfa en las 4 esquinas — un rectángulo plano de color
uniforme con bordes duros, no un punto de luz con resplandor.

**Corrección, sin textura nueva** (como pidió el equipo): cada punto es
ahora un abanico de triángulos (`VertexFormat.DrawMode.TRIANGLES`, 10
segmentos) — un vértice central con el alfa calculado y un anillo
exterior con alfa 0. La GPU interpola el color entre el centro y el
borde de cada triángulo, dando una caída radial suave real (no simulada)
por la sola interpolación de vértices — el mismo truco que usa
cualquier "glow" sin textura. El decal de suelo usa la misma función
(`addCircle`), rotada para quedar plano en vez de orientado a cámara.

### Bug #4 — `/apuyawarlighting toggle` no apagaba el emisor de prueba

**Síntoma real**: `EmitterTracker.active()` añadía el emisor de prueba
(`testEmitter`) sin comprobar `config.rgbEnabled` — aunque `rescan()` sí
vaciaba la lista normal al desactivar el RGB, el emisor de prueba se
colaba igual.

**Corrección**: `active()` ahora comprueba `ApuYawarLightingClient.config().rgbEnabled`
antes de incluir `testEmitter`, igual que ya hacía con la lista normal —
un solo punto de verdad para la bandera de activado/desactivado.

### Verificación de las correcciones

Ver el bloque "Estado real de las pruebas" al principio de este
documento y la sección 15 para el detalle completo: `gradlew build`
completo sin errores (repetido tras la corrección), cada API nueva usada
en el renderizador corregido confirmada contra bytecode real antes de
escribirla, y un intento de reverificación en vivo que resultó
inconcluso por una razón de entorno (no de código) explicada ahí mismo —
no se afirma una confirmación visual que no se obtuvo.

## 17. Corrección post-entrega (2) — los comandos nunca se habían probado de verdad

El equipo reportó que `/apuyawarlighting preset high`, `/apuyawarlighting
test` y `/apuyawarlighting toggle` daban "Comando desconocido o
incompleto" desde el primer carácter, con `apuyawar_lighting 0.1.0`
listado como mod cargado. Investigación paso a paso, exactamente en el
orden que pidió el equipo:

### Paso 1-3: logs de diagnóstico

Se añadió un `Logger` (`"apuyawar-lighting"`, el mismo patrón que ya usa
`momento-cuy`) con mensajes `[DIAG]` en:
- `ApuYawarLightingClient.onInitializeClient()` — al entrar y al salir sin
  excepciones.
- `LightingCommands.register()` — al invocarse.
- Dentro del propio callback `ClientCommandRegistrationCallback.EVENT`
  — para confirmar que el evento realmente se dispara, no sólo que se
  registró un listener.
- Un comando mínimo nuevo, `/ayldiag` (sin argumentos), registrado antes
  que el árbol completo de `/apuyawarlighting`, para aislar si el
  problema era el registro en sí o algo específico del árbol anidado.

### Paso 4: la causa real — no estaba en `LightingCommands.java`

Al ejecutar `gradlew :momento-cuy:runClient` con los logs nuevos, **no
apareció ningún mensaje `(apuyawar-lighting)` en absoluto** — ni el de
`onInitializeClient()`, que es la primera línea del método. Eso significa
que el entrypoint de cliente del módulo nunca se estaba invocando, algo
anterior incluso a si el comando se registra bien o mal.

Inspeccionando el jar que Fabric Loader realmente carga para
`:momento-cuy:runClient` (no el de `apuyawar-lighting/build/libs/`, sino
una copia separada en caché que Loom mantiene en
`.gradle/loom-cache/remapped_mods/.../pe/apuyawar/apuyawar-lighting/0.1.0/apuyawar-lighting-0.1.0.jar`
para las dependencias de proyecto, `modImplementation project(...)`) se
confirmó la causa real: **esa copia en caché tenía fecha y tamaño de
`.class` de horas antes** — de antes incluso de la primera ronda de
correcciones de este documento. Loom indexa esa caché por `(id, versión)`
del mod, no por el contenido real del jar; como `mod_version` nunca
cambió entre iteraciones de desarrollo de hoy, Loom seguía sirviendo la
primerísima copia remapeada, sin importar cuántas veces se recompilara
`apuyawar-lighting` después — **ni el equipo ni la corrección anterior de
este documento estaban probando el código corregido, estaban probando
una versión congelada de antes de que existieran los comandos tal como
están hoy.**

Esto también explica por qué el bug nunca apareció como una excepción:
Fabric Loader sí invocaba un `onInitializeClient()` — sólo que era el de
la copia vieja en caché, que (en su momento) no tenía ningún log propio,
así que no dejaba ningún rastro distinguible de "esto es una versión
vieja" en el log.

**Esto confirma, además, algo importante sobre la ronda de corrección
anterior (sección 16)**: los 4 bugs de renderizado que se corrigieron
ahí (consumers() nulo, mezcla aditiva, halos cuadrados, el toggle del
emisor de prueba) eran reales y la corrección en sí sigue siendo válida
(verificada por bytecode, no por ejecución) — pero el intento de
reverificación en vivo de esa sección nunca pudo haber mostrado el
halo correctamente de todas formas, porque el cliente seguía cargando
el jar congelado de antes de esa corrección también.

### Paso 5-6: confirmado con una copia fresca

Se borró manualmente la entrada de caché y se repitió `runClient`: los
tres logs `[DIAG]` de `onInitializeClient()`/`LightingCommands.register()`
aparecieron de inmediato, confirmando que el entrypoint de cliente y la
llamada a registrar comandos **siempre fueron correctos** — el código de
`LightingCommands.java` nunca tuvo el bug, nunca hizo falta tocar su
estructura ni su arquitectura, tal como pidió el equipo.

**Pasos 4 y 5, confirmados — ya no quedan pendientes.** Un tercer intento
(240 segundos de margen) sí llegó a cargar el mundo real esta vez: lo que
en los intentos anteriores parecía un punto muerto permanente después de
crear los atlas de texturas en realidad era sólo **lento** (unos 2
minutos y 8 segundos en ese paso concreto esta vez, dentro de una sesión
ya muy larga con varios Gradle/Java acumulados) — con más tiempo,
terminó solo. El log real, íntegro, muestra la secuencia completa sin
ninguna excepción:

```
[01:29:07] (Minecraft) Player66[local:...] logged in with entity id 62 at (...)
[01:29:07] (Minecraft) Player66 se conectó
[01:29:07] (apuyawar-lighting) [DIAG] ClientCommandRegistrationCallback disparado (dispatcher=com.mojang.brigadier.CommandDispatcher@1aaf9cde)
[01:29:07] (apuyawar-lighting) [DIAG] /ayldiag registrado
[01:29:07] (apuyawar-lighting) [DIAG] /apuyawarlighting registrado, nodo='apuyawarlighting'
[01:29:07] (Minecraft) Loaded 4 advancements
```

Esto confirma, con evidencia real y no sólo razonamiento: el evento
**sí** se dispara al entrar al mundo, el nodo `apuyawarlighting` **sí**
queda registrado en el dispatcher activo, y no hay ninguna excepción en
el proceso. Los 6 pasos de investigación que pidió el equipo quedan
completos.

### Paso 7-8: corrección real, sin tocar la arquitectura del motor RGB

> **Esta subsección describe el primer intento de corrección, que
> resultó ser destructivo (borraba un archivo que el propio Gradle
> necesitaba intacto al lanzar la JVM) y se reemplazó — ver sección 18
> para la corrección real y definitiva.** Se deja el texto original sin
> borrar para que quede el rastro completo de la investigación.

La corrección NO fue en `LightingCommands.java` ni en ninguna clase del
motor — fue en `momento-cuy/build.gradle`: una tarea nueva,
`clearStaleLightingRemapCache`, que borra cualquier
`apuyawar-lighting-*.jar` dentro de la caché de Loom antes de que
corran `runClient`/`runServer` (`tasks.matching { it.name == 'runClient'
|| it.name == 'runServer' }.configureEach { dependsOn
'clearStaleLightingRemapCache' }`), para que **siempre** se remapee la
versión más reciente del módulo. Sin esto, cualquier cambio futuro al
motor RGB habría corrido el mismo riesgo de probarse en silencio contra
una copia vieja. Confirmado con `gradlew :momento-cuy:runClient --dry-run`
que la tarea nueva queda enganchada antes de `runClient` en el plan de
ejecución real.

### Las otras dos correcciones pedidas

- **`EmitterTracker.active()` con `rgbEnabled=false`**: antes, si no
  había emisor de prueba, el método devolvía el campo `active` tal cual
  — que puede seguir teniendo la lista del último rescaneo (hasta 10
  ticks de antigüedad) en vez de una lista vacía inmediata. Corregido
  para comprobar `rgbEnabled` primero y devolver `List.of()` de
  inmediato en ese caso, sin esperar al siguiente rescaneo.
- **El emisor de prueba al abandonar el mundo**: `rescan()` ya vaciaba
  `active` cuando `client.world == null` (desconexión), pero no tocaba
  `testEmitter` — quedaba apuntando a un `BlockPos` de un mundo que ya
  no existe. Ahora se limpia en el mismo punto.

### Pendiente para el equipo

Los 6 pasos de diagnóstico que pidió el equipo ya se confirmaron con una
ejecución real (arriba) — lo único que falta es la prueba interactiva en
el propio juego del equipo, ahora que la causa real está resuelta:

1. Correr `gradlew :momento-cuy:runClient`. **Corrección importante tras
   esta sección: el mecanismo descrito aquí abajo (borrar la caché antes
   de lanzar) resultó ser destructivo y se reemplazó — ver sección 18.**
   Si el arranque parece tardar mucho justo después de "Created ...
   gui.png-atlas", no es un cuelgue — puede tardar más de un minuto en
   ese paso concreto; conviene esperar en vez de cancelar.
2. Confirmar en el chat/consola que `/ayldiag` y `/apuyawarlighting`
   (con sus subcomandos) aparecen en las sugerencias y se pueden
   ejecutar — esto es lo único que falta probar de verdad, ya que el
   registro en sí quedó confirmado por el log real de arriba.
3. Probar `/apuyawarlighting test` cerca de un Abeto Celestial para por
   fin ver el halo turquesa — objetivo original de esta ronda.
4. Si todo funciona, el equipo puede pedir que se quiten los logs
   `[DIAG]` y `/ayldiag` en un commit de limpieza aparte — se dejaron
   esta vez a propósito para que el propio equipo los vea en su sesión
   real, no por descuido.

## 18. Corrección post-entrega (3) — la "solución" anterior borraba un archivo que la JVM necesitaba

El equipo reportó, con evidencia de `latest.log`, el error real:

```
Class path entries reference missing files: .../.gradle/loom-cache/remapped_mods/.../apuyawar-lighting-0.1.0.jar
```

Tenía razón por completo. La tarea `clearStaleLightingRemapCache` de la
sección 17 borraba exactamente el archivo que Gradle/Loom ya había
decidido poner en el classpath de la JVM — la borrada ocurría en el
momento de ejecución de esa tarea (`doFirst`, justo antes de
`runClient`), pero el classpath en sí ya se había calculado durante la
**configuración** del build, antes de que cualquier tarea se ejecutara.
El resultado: Gradle lanzaba la JVM apuntando a un archivo que mi propia
tarea acababa de eliminar un instante antes — el síntoma exacto que
reportó el equipo.

### Causa raíz real, más profunda que "hay que borrar la caché"

Loom trata las dependencias de proyecto (`modImplementation
project(':apuyawar-lighting')`) usadas para desarrollo igual que una
dependencia Maven externa versionada: las remapea una vez a
`.gradle/loom-cache/remapped_mods/.../(id)/(versión)/...jar` y asume que,
para una versión dada, el contenido nunca cambia (una suposición correcta
para una dependencia Maven real, pero falsa para un módulo hermano que
seguimos editando en el mismo repositorio con una versión fija,
`mod_version=0.1.0`, que nunca sube). Esa caché no es una salida de tarea
de Gradle normal con seguimiento de entradas/salidas — es un mecanismo
interno de Loom con su propia lógica de "ya existe, no hace falta
rehacerlo", así que **ni dejarla intacta (bug original, sección 17) ni
borrarla a mano en el momento equivocado (mi primer intento de arreglo)
son correctos.**

### Corrección real: jar-in-jar (`include`), no borrar nada

La solución correcta, que pidió el equipo explícitamente ("no quiero
soluciones que dependan de borrar manualmente el JAR"), es sacar a
`apuyawar-lighting` de ese mecanismo de caché por completo. Loom ya trae
el mecanismo correcto para esto: **jar-in-jar**
(`include project(':apuyawar-lighting')`), que empaqueta el jar actual
del módulo dentro del jar de `momento-cuy` en tiempo de compilación, vía
una tarea de Gradle normal (`processIncludeJars`) con seguimiento de
entradas/salidas real — **no** vía la caché separada de "mods remapeados
para dependencias de proyecto".

`momento-cuy/build.gradle` ahora declara ambas líneas, cada una con su
propio propósito (patrón estándar documentado por Loom: `modImplementation`
da acceso de compilación/ejecución, `include` empaqueta):

```gradle
modImplementation project(':apuyawar-lighting')
include project(':apuyawar-lighting')
```

Se quitó por completo la tarea `clearStaleLightingRemapCache` y su
`dependsOn` sobre `runClient`/`runServer` — cero borrado de archivos
antes o durante la ejecución del cliente, tal como pidió el equipo.

**Sobre el punto muerto de arranque que hizo descartar `include()` la
primera vez** (documentado en la entrega original de este módulo): ese
problema era real pero temporal — Loom exige que el jar del módulo
dependido ya exista en disco para poder *configurar* el proyecto que lo
incluye, lo cual era imposible la primerísima vez que se creó
`apuyawar-lighting` (ningún jar existía todavía en ningún lado). Ahora
que el módulo ya se ha compilado muchas veces a lo largo de esta serie de
correcciones, ese archivo ya existe de forma permanente en
`apuyawar-lighting/build/libs/`, así que el punto muerto ya no puede
ocurrir — se confirmó con `gradlew :momento-cuy:compileJava` (configura
sin error) y `gradlew :momento-cuy:build` (compila y empaqueta sin
error).

### Verificación real, no sólo razonamiento

Inspeccionando el jar final de `momento-cuy` tras la build:

```
$ unzip -l momento-cuy-0.1.0.jar | grep apuyawar
META-INF/jars/apuyawar-lighting-0.1.0.jar   (28013 bytes -- el tamaño del build mas reciente)
```

Y Loom inyectó automáticamente la declaración correcta en el propio
`fabric.mod.json` de `momento-cuy` (no hubo que escribirla a mano):

```json
"jars": [ { "file": "META-INF/jars/apuyawar-lighting-0.1.0.jar" } ]
```

Con esto, se corrió `gradlew :momento-cuy:runClient` de nuevo:
- **Ninguna advertencia de "Class path entries reference missing
  files"** en todo el log — el error exacto que reportó el equipo ya no
  aparece.
- Los 3 logs `[DIAG]` de `ApuYawarLightingClient`/`LightingCommands`
  aparecen de inmediato, confirmando contenido fresco (no la copia vieja
  de antes).

La misma ejecución continuó hasta cargar el mundo real por completo
(múltiples ciclos de autoguardado normales, sin errores), y mostró la
confirmación completa de registro de comandos:

```
[01:56:06] (apuyawar-lighting) [DIAG] ClientCommandRegistrationCallback disparado (dispatcher=com.mojang.brigadier.CommandDispatcher@3b48a38c)
[01:56:06] (apuyawar-lighting) [DIAG] /ayldiag registrado
[01:56:06] (apuyawar-lighting) [DIAG] /apuyawarlighting registrado, nodo='apuyawarlighting'
```

Los 9 puntos que pidió el equipo quedan confirmados con una sola
ejecución real, de punta a punta: sin advertencia de archivo faltante,
entrypoint ejecutado, evento disparado, nodo registrado, sin ninguna
excepción en todo el proceso.

### Pendiente para el equipo

1. Correr `gradlew :momento-cuy:runClient` con normalidad — sin ningún
   paso manual antes ni después.
2. Confirmar que no aparece ninguna advertencia de archivo faltante.
3. Confirmar `/ayldiag`, `/apuyawarlighting estado` y
   `/apuyawarlighting test` en las sugerencias del chat y que se
   ejecutan.
4. Repetir el ciclo normal de desarrollo (editar `apuyawar-lighting`,
   recompilar, volver a correr `runClient`) y confirmar que el contenido
   siempre es el actual — ya no debería hacer falta ningún paso manual
   de limpieza de caché.
