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

## 19. Corrección de anclaje espacial, decals y efectos celestiales del Abeto

### 19.1. Síntoma observado

El equipo reportó, jugando de verdad: el árbol se ve oscuro a distancia,
al acercarse aparecen los efectos, y **al caminar cerca o debajo del
árbol aparecen manchas luminosas sobre el suelo por donde se desplaza el
jugador** — dando la sensación de que la luz sigue al jugador en vez de
quedar anclada al árbol.

### 19.2. Diagnóstico espacial (Fase 1 del encargo) — no era un bug de matrices

Se revisó con cuidado `billboardPose(...)` (orientación a cámara) y
`addGroundDecal(...)` (decal de suelo) buscando específicamente una doble
traslación o una confusión entre la posición del jugador y la del
emisor. **Conclusión: la posición de cada punto y de cada decal se
calcula exclusivamente a partir de `emitter.pos()` (un `BlockPos` real,
fijo en el mundo) más un desplazamiento local determinista
(`EmitterSampler`) — la posición del jugador/cámara sólo se resta al
final, para convertir a coordenadas relativas a cámara (la convención que
exige `WorldRenderContext` para esta fase), nunca para decidir el centro
del efecto.** `matrixStack.push()`/`pop()` están balanceados en cada
punto y en cada decal, así que no hay traslación residual que se acumule
entre llamadas. **No se encontró ningún bug de coordenadas o de
matrices.**

Lo que sí se encontraron fueron dos bugs reales, completamente distintos
entre sí, que juntos producían exactamente el síntoma reportado — ver
19.3 y 19.4.

### 19.3. Causa real #1 — el signo invertido de la atenuación del decal (sección 5 del encargo)

**Confirmado con números concretos**, tal como pidió el equipo. Un abeto
típico: hoja en `Y=72`, suelo en `Y=64`.

**Cálculo anterior**:
```
fallDistance = groundY - pos.getY() = 64 - 72 = -8
atenuación   = clamp01(1 - fallDistance / (radio*3))
             = clamp01(1 - (-8/18))
             = clamp01(1 + 0.444)
             = clamp01(1.444)
             = 1.0   ← intensidad MÁXIMA, sin importar la altura real
```
Como una hoja está casi siempre por encima del suelo, `fallDistance` daba
negativo casi siempre — y restar un negativo *aumenta* el resultado en
vez de atenuarlo, quedando recortado en el máximo. Una hoja a 2 bloques
de altura y una a 20 habrían producido el mismo decal a máxima
intensidad; el decal prácticamente nunca se desactivaba por distancia
(sólo cuando `fallDistance` era positivo, es decir, cuando la "hoja"
estaba *por debajo* del suelo, algo que casi nunca ocurre).

**Cálculo corregido** (`WorldLightingRenderer.addGroundDecal`):
```java
double verticalDistance = Math.max(0.0, pos.getY() - groundSurfaceY);  // nunca negativo
float linear  = clamp01(1.0f - (float) (verticalDistance / (profile.radius() * 3.0)));
float falloff = (float) Math.pow(linear, Math.max(0.1f, profile.falloff()));  // exponente real del perfil
```
Con los mismos números: `verticalDistance = 72 - 64 = 8`, `linear =
clamp01(1 - 8/18) = 0.556`, y con `falloff=2.0` (el valor por defecto de
`EmitterProfile.foliage`) → `0.556² ≈ 0.31` — una atenuación real que
disminuye con la altura, en vez de un valor fijo en el máximo. Más alto
que `radio*3` (18 bloques en este ejemplo) → `linear` llega a 0 → no se
dibuja ningún decal, tal como pedía el encargo ("si está demasiado lejos,
no debe dibujarse ninguna mancha artificial").

### 19.4. Causa real #2 — la selección de decals cambiaba con la posición del jugador (sección 6 del encargo)

Esta es, con más probabilidad, la causa **principal** de la sensación de
"la luz sigue al jugador" — más que el signo del cálculo (que hacía que
el decal fuera siempre intenso, pero no que *cambiara de lugar*).

**Mecanismo anterior**: `EmitterTracker.rescan()` ordenaba TODOS los
emisores candidatos por prioridad y luego por distancia a la cámara, y
sólo calculaba el suelo (`findGroundY`, un rayo hacia abajo) para "los
primeros 8" de esa lista ordenada. Como el orden por distancia cambia
cada vez que el jugador se mueve — una hoja que antes era la 9ª más
cercana puede pasar a ser la 6ª —, **el conjunto de 8 hojas con decal
cambiaba en cada rescaneo** (cada 10 ticks, medio segundo) según por
dónde caminara el jugador. El resultado visual: manchas de luz que
aparecen y desaparecen en distintos lugares del suelo mientras el
jugador se mueve — exactamente lo que se reportó.

**Corrección** (`EmitterTracker.isGroundDecalCandidate`): la elegibilidad
para decal de suelo ahora es una propiedad **fija de la posición del
bloque**, no de su cercanía al jugador:

```java
private static boolean isGroundDecalCandidate(BlockPos pos) {
    return Math.floorMod(pos.asLong(), GROUND_DECAL_EVERY) == 0;  // GROUND_DECAL_EVERY = 5
}
```

1 de cada 5 bloques de hoja emisores es, de forma determinista y
permanente, "candidato a decal" — esto no cambia nunca por el movimiento
del jugador, sólo por si ese bloque concreto sigue existiendo y dentro
del radio de escaneo. Entre los candidatos reales (ya fijos), si hay más
de los que conviene calcular en un rescaneo (tope de seguridad
`GROUND_DECAL_COMPUTE_LIMIT = 24`, sólo por costo de los rayos hacia
abajo, bastante más generoso que el límite de 8 anterior), se priorizan
por cercanía — pero esto sólo decide *cuáles de los candidatos fijos se
recalculan primero*, nunca *cuáles son candidatos*. El conjunto de
posiciones que pueden tener decal es estable; caminar alrededor del árbol
ya no cambia qué hojas lo producen.

### 19.5. Transiciones suaves de entrada/salida (Fase 3, sección 7 del encargo)

Se añadió un sistema de fundido persistente en `EmitterTracker`
(`fade`, un `Map<Long, Float>` por posición de bloque, actualizado **cada
tick**, no sólo en cada rescaneo de 10 ticks): cuando una posición pasa a
estar en el rango activo, su intensidad sube gradualmente de 0 a 1 a lo
largo de `config.fadeTransitionTicks` (12 por defecto, 0.6 s); cuando
sale de rango, baja de 1 a 0 de la misma forma, en vez de desaparecer de
golpe. `EmitterTracker.active()` ya devuelve cada emisor con su valor de
fundido actual (`ActiveEmitter#fade()`), que `WorldLightingRenderer`
multiplica directamente en el cálculo de alfa del punto y del decal — la
posición nunca cambia durante la transición, sólo la intensidad.

### 19.6. Mejora de la distribución de puntos (Fase 4, sección 9 del encargo)

`EmitterSampler.samplePoint` elegía antes un punto centrado de forma
independiente en los 3 ejes (0.2-0.8 en cada uno), lo que deja la mayoría
de los puntos cerca del centro geométrico del bloque — "profundamente
dentro de bloques opacos, donde nunca se verán", tal como advertía el
encargo. Ahora cada punto elige primero, de forma determinista, una de
las 6 caras del bloque y se sesga hacia ella en su eje principal
(0.08-0.26 desde el borde), quedando disperso en los otros dos ejes — más
cerca de superficies visibles, sin concentrar todos los puntos en la
misma cara.

Se añadió `EmitterSampler.sampleCategory` con tres categorías
deterministas (no uniformes — sesgadas para que las "principales" sean
realmente raras, como pedía el encargo): **pequeños** (~70%, núcleo
pequeño, halo discreto), **medianos** (~25%, núcleo más grande), y
**principales** (~5%, núcleo notablemente mayor y con un tercer nivel de
resplandor exterior que los otros dos no tienen). `WorldLightingRenderer`
ya aplica los multiplicadores de cada categoría al radio/intensidad del
núcleo y del halo.

### 19.7. Tres niveles de halo (sección 10 del encargo)

Antes: núcleo + un único halo. Ahora: núcleo (interpolación de alfa real
centro→borde, sin bordes duros — eso ya estaba bien desde la corrección
anterior) + halo intermedio (más grande, más tenue) + un tercer nivel de
"resplandor exterior", mucho más tenue y grande, **sólo en los puntos de
categoría "principal"** (para no saturar la copa con tres capas en cada
uno de los decenas de puntos). Se subieron los segmentos del abanico de
10 a 14 para un borde algo más suave, sin disparar el costo por vértice
de forma significativa (sigue siendo geometría de pocos triángulos por
punto, nada comparable a un modelo real).

### 19.8. Partículas celestiales (Fase 5, sección 11 del encargo)

No se duplicó el sistema existente. Se revisó
`MomentoCuyAbetoLeavesBlock.randomDisplayTick` contra cómo Minecraft
realmente llama a ese método (investigado en una ronda anterior de este
proyecto): el motor ya sólo selecciona una muestra pequeña y aleatoria de
bloques por tick, no todas las hojas — la probabilidad adicional de 1/8
(noche) o 1/20 (día) de la versión anterior, compuesta sobre ese muestreo
ya disperso, dejaba muy pocas partículas visibles en la práctica sobre un
árbol de decenas de bloques de hoja. Se subió la frecuencia (1/4 noche,
1/12 día), la escala visual de la partícula (0.8→1.0 y 0.7→0.9), y se
añadió deriva lateral en X/Z además de la vertical que ya existía — "
movimiento lateral ligero" pedido en el encargo. Sigue siendo el mismo
`DustParticleEffect` vía `randomDisplayTick`, sin un segundo sistema
paralelo.

**No se conectó la densidad de estas partículas a la configuración de
APU YAWAR Lighting** (`particleDensityMultiplier`/nueva
`fixedPointDensityMultiplier`) — `MomentoCuyAbetoLeavesBlock` es un
bloque común que Momento Cuy debe poder cargar sin `apuyawar-lighting`
instalado (al igual que `MomentoCuyLightingBridge`, protegido con
`FabricLoader.isModLoaded`); importar directamente la clase de
configuración de `apuyawar-lighting` desde un bloque común rompería esa
garantía, porque los bloques se registran incondicionalmente en el
inicializador común, no sólo en el de cliente. Queda documentado como
pendiente (sección 19.14) en vez de introducir ese acoplamiento bajo
presión de tiempo.

### 19.9. Separación de densidades (sección 11 del encargo)

`particleDensityMultiplier` controlaba antes el muestreo de los puntos
FIJOS de la copa en `WorldLightingRenderer`/`EmitterSampler`. Se añadió
`fixedPointDensityMultiplier` (nuevo campo) para ese propósito, y
`particleDensityMultiplier` queda reservado (documentado como tal en el
propio código) para cuando se resuelva el acoplamiento de 19.8 con las
partículas móviles — no se eliminó ni se renombró, para no romper
`config/apuyawar_lighting.json` ya guardados.

### 19.10. Nuevos parámetros de configuración (Fase 6, sección 16 del encargo)

Todos en `config/apuyawar_lighting.json`, con valores por defecto
razonables para no romper nada al actualizar un archivo existente (Gson
conserva el valor Java del campo cuando la clave no está presente en el
JSON guardado):

- `fixedPointDensityMultiplier` — densidad de puntos fijos (separado de
  partículas móviles, ver 19.8-19.9).
- `groundDecalsEnabled` — activa/desactiva el decal de suelo por
  completo, conservando los puntos de la copa (la salida de emergencia
  que pedía la sección 6 si el decal "no puede producir un resultado
  visual coherente").
- `groundDecalIntensityMultiplier` — intensidad del decal, independiente
  de la intensidad general de los puntos.
- `fadeTransitionTicks` — duración de la transición de entrada/salida
  (19.5).
- `maxEffectDistance` — distancia máxima real a la que un emisor puede
  llegar a verse, distinta del radio interno de escaneo (que sigue siendo
  sólo una optimización de rendimiento).
- `simplifyDetailBeyondDistance` — más allá de esta distancia, cada punto
  sólo dibuja su núcleo (sin halo ni decal) — un nivel de detalle
  simplificado a media distancia, sección 12 del encargo.
- `debugHideFixedPoints` — modo de depuración (19.11): oculta
  puntos/halos de la copa sin tocar los decals, para aislar problemas.

Comandos nuevos bajo `/apuyawarlighting set`: `puntosfijos`,
`intensidaddecal`, `distanciamax`, `distanciasimplificar`, `transicion`
(en ticks). Comando nuevo `/apuyawarlighting decals` (alterna
`groundDecalsEnabled`, sin argumentos, igual que `toggle`). Se
conservaron todos los comandos/nombres existentes sin cambios
(`estado`, `toggle`, `preset`, `test`, y los `set` ya existentes).

### 19.11. Modo de depuración (sección 17 del encargo)

`/apuyawarlighting debug <emisores|posiciones|puntos>`, desactivado por
defecto y sin overlay permanente:

- `debug emisores` — un mensaje puntual con el total de emisores activos
  y cuántos tienen decal de suelo ahora mismo.
- `debug posiciones` — imprime, una sola vez por invocación (nunca de
  forma continua en el log, tal como pedía el encargo), la posición
  mundial, el fundido actual y el estado de decal de hasta 10 emisores
  activos.
- `debug puntos` — alterna `debugHideFixedPoints`: oculta por completo
  los puntos/halos de la copa sin tocar los decals de suelo, para poder
  compararlos por separado.

No se implementó un alternador de partículas en este comando (ver 19.8 —
las partículas móviles viven en `momento-cuy`, fuera del alcance directo
de esta configuración por la razón ya explicada).

### 19.12. Limitaciones de los halos geométricos vs. iluminación RGB real

Sigue sin existir Bloom real de framebuffer ni iluminación de vóxel/
lightmap coloreada — ver sección 7 y sección 14 de este documento
(sin cambios en esta ronda, el encargo pidió explícitamente no avanzar
ahí todavía). Lo nuevo de esta ronda (fundido, categorías, tres niveles
de halo, decals estables) sigue siendo geometría aditiva apilada con
prueba de profundidad real — mejor anclada y más agradable visualmente,
pero no un sistema de iluminación físicamente coherente.

### 19.13. Pruebas realizadas

**Con evidencia real de ejecución** (no sólo razonamiento o compilación):
- `gradlew build` (los 4 módulos): éxito.
- `gradlew :apuyawar-lighting:compileJava`: éxito, sin advertencias.
- `gradlew :momento-cuy:runClient`: ejecución real, confirmando que el
  entrypoint sigue invocándose correctamente tras todos estos cambios
  (logs `[DIAG]` presentes) y sin ninguna excepción durante la carga.

**Verificación matemática con valores concretos** (no sólo código): el
cálculo de atenuación del decal se confirmó con las coordenadas reales de
un abeto típico (Y=72 hoja, Y=64 suelo) mostrando el valor exacto erróneo
anterior (1.0, siempre máximo) y el valor corregido (≈0.31, atenuado
según altura real) — ver 19.3.

**Pendiente de prueba visual directa** (no hay herramienta de captura o
control de teclado/ratón en este entorno): las pruebas A-J completas que
pidió el encargo (emisor fijo, abeto real, caminar alrededor, cámara,
distancia, día/noche, partículas, decal, varios árboles, FPS) — la lista
exacta queda en la sección 19.15 para que el equipo las ejecute. Lo que
sí se puede afirmar con alta confianza, por el análisis de código con
valores concretos: el decal ya no se queda fijo en máxima intensidad sin
importar la altura, y el conjunto de hojas que produce decal ya no cambia
al caminar — son las dos causas identificadas del síntoma reportado, y
ambas quedaron corregidas de forma verificable en el código, aunque su
efecto visual final todavía no se ha visto en pantalla.

### 19.14. Problemas pendientes

1. **Partículas móviles sin densidad configurable** — ver 19.8, pendiente
   de una solución de desacoplamiento (p. ej. un campo intermedio en
   `momento-cuy`, actualizado por `MomentoCuyLightingBridge`, que las
   hojas lean sin importar directamente clases de `apuyawar-lighting`) en
   una futura ronda.
2. **Bloom real de framebuffer** — sigue sin implementarse, tal como pidió
   el encargo explícitamente para esta ronda.
3. **Sin medición de FPS real** — la prueba J del encargo (comparar FPS
   con RGB activado/desactivado, con varios árboles) no se pudo ejecutar
   sin control de teclado/ratón en este entorno.
4. **Sin confirmación visual del parecido con la imagen de referencia** —
   los cambios de esta ronda son razonados y verificados matemáticamente/
   por ejecución sin errores, pero nadie los ha visto renderizados
   todavía.
5. **`GROUND_DECAL_EVERY = 5` y `GROUND_DECAL_COMPUTE_LIMIT = 24` son
   valores de partida razonables, no calibrados visualmente** — si el
   equipo ve muy pocos o demasiados decals por árbol, son los dos números
   a ajustar primero.

### 19.15. Pasos para probar la actualización

1. `gradlew :momento-cuy:runClient`.
2. `/apuyawarlighting test` — confirmar que el punto de prueba no se
   mueve al caminar alrededor, en primera y tercera persona.
3. Plantar/crecer un Abeto Celestial, pararse quieto cerca y observar los
   puntos de la copa: deberían verse de tres tamaños, con el halo más
   notorio sólo en los pocos puntos "principales".
4. Caminar alrededor y debajo del árbol varias veces — las manchas de
   suelo (si `groundDecalsEnabled=true`) deberían quedarse en los mismos
   sitios, no reubicarse por el movimiento.
5. `/apuyawarlighting debug posiciones` antes y después de caminar, para
   comparar coordenadas exactas si algo se ve raro.
6. `/apuyawarlighting debug puntos` para aislar si un problema visual
   viene de los puntos/halos o del decal.
7. `/apuyawarlighting decals` para desactivar el decal por completo y
   juzgar sólo la copa.
8. Alejarse del árbol progresivamente y observar si sigue siendo
   reconocible a distancia media (gracias a `simplifyDetailBeyondDistance`)
   antes de desaparecer del todo en `maxEffectDistance`.
9. `/time set night` y `/time set day` para comparar.
10. Confirmar que el Cerezo Celestial sigue con sus pétalos, destellos
    dorados y su propio decal/halo, sin haberse roto por los cambios
    compartidos del renderizador.

## 20. APU YAWAR Lighting V2 — Follaje emisivo, Bloom celestial y foco ambiental estable

El equipo probó la versión anterior en Minecraft real y envió una captura:
copa considerablemente oscura, y **una mancha cian grande y de bordes
evidentes debajo del árbol**, comparable en tamaño a la proyección de la
copa. Esta sección documenta la causa real de ambos problemas y lo que se
corrigió — siguiendo el orden obligatorio que pidió el equipo: primero el
foco de suelo (mandatorio), después la copa, y por último se documenta
honestamente qué de los efectos avanzados (follaje emisivo real, Bloom)
no se completó esta ronda.

### 20.1. Diagnóstico de los problemas iniciales

Con la captura real y el código de la ronda anterior delante:

- **Mancha de suelo grande**: `addGroundDecal` calculaba el radio del
  decal como `profile.radius() * 0.5 * lightRadiusMultiplier`. Para el
  abeto, `profile.radius() = 6.0` → radio del decal ≈ 3 bloques **por
  cada hoja candidata**. La ronda anterior ya había hecho que la
  *elegibilidad* de cada hoja fuera estable (1 de cada 5, determinista),
  pero seguía siendo **un decal independiente por cada una de esas
  hojas** — bajo una copa con decenas de hojas, varios decals de 3
  bloques de radio, superpuestos con mezcla aditiva, se fusionan
  visualmente en una sola mancha grande. Esto coincide exactamente con lo
  que muestra la captura.
- **Copa oscura**: revisando la fórmula del radio de los puntos de la
  copa (`WorldLightingRenderer`), la base era `0.05` bloques — un punto
  "pequeño" (multiplicador 0.55) medía ~0.055 de radio, **unos 5-6 cm**:
  prácticamente invisible a distancia normal de juego, incluso con el
  alfa calculado correctamente. Es un problema de *tamaño*, no de
  *intensidad* — subir sólo la intensidad (que el equipo pidió
  explícitamente no hacer) no lo habría arreglado.

### 20.2. Causa real de que la iluminación pareciera completarse al caminar

Aunque la ronda anterior ya hizo determinista la *elegibilidad* de cada
hoja para decal (no dependía de la cercanía al jugador), **el conjunto de
posiciones que el sistema conocía en absoluto seguía viniendo de un
rescaneo centrado en el jugador, con un tope general de luces
(`maxActiveLights`)**. Con decenas de hojas candidatas bajo un mismo
árbol, cada una generando su propio decal, al caminar podían entrar o
salir hojas individuales del conjunto evaluado — y como cada una aporta
su PROPIO círculo a la mezcla aditiva, la mancha combinada podía crecer o
cambiar de forma según cuántas hojas individuales estuvieran "dentro"
en ese momento, incluso sin que ninguna posición concreta cambiara. La
causa de fondo no era (sólo) selección inestable — era tener **múltiples
focos independientes por árbol en vez de uno solo**, lo que amplifica
cualquier variación en el conjunto de hojas evaluadas.

### 20.3. Corrección del tamaño y la estabilidad del foco de suelo (Fase 2, obligatoria)

**Nueva arquitectura** (`EmitterTracker`): los focos de suelo pasan de
"uno por hoja candidata" a **uno como máximo por celda de una rejilla
fija de 8×8 bloques** (`GROUND_CELL_SIZE`), en coordenadas del mundo,
nunca centrada en el jugador. Un `GroundSpot` nuevo (`record`, separado
de `ActiveEmitter`) representa cada foco, con su propia clave de celda,
posición ancla, altura de suelo y perfil. Un abeto típico cabe en 1-2
celdas → 1-2 focos pequeños, no decenas superpuestos.

El representante de cada celda se elige de forma **determinista** (la
hoja más alta de la celda; empate por `BlockPos#asLong()`), nunca "la
primera que aparezca al iterar" — así el resultado no depende del orden
de escaneo. **Limitación documentada**: si el conjunto exacto de hojas
evaluadas en una celda cambia levemente entre rescaneos (por el límite
`maxActiveLights` o el borde del radio de escaneo), el representante
elegido podría cambiar a otra hoja de la *misma* celda, desplazando el
foco unos pocos bloques dentro de esa celda de 8×8 — nunca saltando a
una celda distinta ni apareciendo/desapareciendo salvo que la celda
entera entre o salga del rango. Es la aproximación espacial determinista
que el encargo autorizó explícitamente como alternativa a un
agrupamiento exacto por árbol (costoso de mantener por frame).

**Tamaño y intensidad desacoplados** (`WorldLightingRenderer`,
`LightingConfig`): el radio y la intensidad base del foco ya **no** salen
de `profile.radius()`/`profile.intensity()` de ninguna hoja — son campos
propios:

```java
public float groundSpotRadius = 1.6f;      // antes: profile.radius()*0.5 ≈ 3.0
public float groundSpotIntensity = 0.55f;  // antes: profile.intensity() (0.85 del abeto)
```

`groundSpotRadius = 1.6` es el punto de partida que sugirió el encargo
(1-2 bloques), muy por debajo del ~3 anterior. La atenuación por altura
conserva la corrección de signo de la ronda pasada
(`verticalDistance = max(0, anchorY - groundY)`, con el exponente
`falloff` real del perfil) — sigue sin producir manchas en superficies
muy por debajo de la copa.

### 20.4. Distancia de visibilidad vs. radio de influencia (Fase 3)

Confirmado el hallazgo que ya se había anotado la ronda anterior: la
fórmula `min(profile.radius()*2, maxEffectDistance)` hacía que
`maxEffectDistance` fuera inerte para el abeto (`radius=6` → siempre
`12`, sin importar la configuración). Corregido: el corte de detección
ahora usa únicamente `maxEffectDistance`, sin mezclar el radio de luz de
ninguna hoja. Como consecuencia, el radio de escaneo horizontal de
`EmitterTracker` ahora deriva de `maxEffectDistance` en vez de ser una
constante fija de 24 — y para que esto no dispare el costo
cuadráticamente, el escaneo vertical se desacopló del horizontal (antes
crecía junto con el radio, O(radio³); ahora tiene una altura fija de ±20
bloques, independiente de cuán lejos se busque horizontalmente).

### 20.5. Cambios en `EmitterTracker.java`

- `GroundSpot` (record nuevo): foco de suelo por celda, no por hoja.
- `buildGroundSpots(...)`: agrupa los emisores de follaje activos por
  celda de 8×8 bloques y elige un representante determinista por celda.
- `cellKey(BlockPos)`: clave estable de celda (no de posición individual).
- Dos mapas de fundido independientes (`fade` para puntos de copa,
  `groundFade` para focos de suelo, con la misma mecánica compartida en
  `updateFadeMap`) — cada uno con su propia identidad de clave (posición
  de bloque vs. celda), así que un foco de suelo no "parpadea" sólo
  porque cambió qué hoja concreta lo representa dentro de la misma celda.
- El corte de distancia ya no usa `profile.radius()` (20.4).
- El escaneo vertical ya no escala con el radio horizontal (20.4).
- Se quitaron `GROUND_DECAL_EVERY`/`GROUND_DECAL_COMPUTE_LIMIT`/
  `isGroundDecalCandidate` de la ronda anterior (ya no aplican: la
  selección ahora es por celda, no por "1 de cada 5 hojas").

### 20.6. Cambios en `WorldLightingRenderer.java`

- `addGroundSpot(...)` reemplaza a `addGroundDecal(...)`: recibe un
  `GroundSpot` (no un `ActiveEmitter`), usa `groundSpotRadius`/
  `groundSpotIntensity` en vez de `profile.radius()`/`profile.intensity()`.
- Bucle nuevo sobre `EmitterTracker.groundSpots()`, separado del bucle de
  puntos de copa sobre `EmitterTracker.active()`.
- Radio base de los puntos de copa subido de 0.05 a 0.11 bloques (20.1)
  — razonado por el tamaño en centímetros, **no confirmado visualmente
  todavía**.

### 20.7. Follaje emisivo real — estado honesto

**No se implementó esta ronda.** El encargo fue explícito en que una
máscara emisiva real, aplicada sobre la geometría/textura del propio
bloque de hojas, es *distinta* de un billboard flotante delante de ella
— y pidió explícitamente no presentar círculos más grandes como si fueran
eso. Los círculos de esta entrega (más grandes que antes, ver 20.6) **no
son material emisivo real**: siguen siendo geometría aditiva flotando
delante del bloque, no una propiedad del bloque en sí.

Las opciones técnicas reales investigadas para una futura ronda, en
orden de riesgo creciente:

1. **Posicionamiento de puntos derivado de los píxeles reales de la
   textura**: leer la imagen real de `hojas_pino_esmeralda`/`side.png`
   (y las demás caras) en el cliente, encontrar los píxeles más claros, y
   usar esas coordenadas (convertidas a espacio 3D del bloque) para
   colocar los puntos de luz en vez del sesgo-por-cara determinista
   actual. Acopla visualmente el brillo a la textura real sin tocar el
   pipeline de render — el riesgo es bajo (lectura de archivo + mapeo de
   coordenadas), pero sigue sin ser una propiedad del material, sólo un
   mejor punto de partida para los billboards.
2. **Segunda pasada de render sobre la geometría real del bloque**, con
   una capa de textura emisiva aditiva — el patrón real que ya usa
   vanilla para materiales que brillan en la oscuridad
   (`rendertype_entity_translucent_emissive`, visible en los logs de
   `runClient` de esta sesión como una advertencia inocua de un sampler
   no usado) es para ENTIDADES, no bloques; adaptarlo a bloques exigiría
   registrar una capa de render adicional vía
   `BlockRenderLayerMap`/un modelo con una segunda textura, investigado
   pero no implementado.
3. **Shader de material real** vía `CoreShaderRegistrationCallback` (la
   misma API que ya se investigó para el Bloom, sección 7 de este
   documento) — el camino más fiel a "máscara emisiva en la textura",
   pero exige GLSL nuevo que no se puede verificar visualmente en este
   entorno (mismo límite ya documentado para el Bloom).

No se implementó ninguna de las tres esta ronda — se documenta la
investigación para la siguiente, en vez de simularlo con círculos más
grandes.

### 20.8. Puntos LED — qué sí se mejoró

Lo real de esta ronda para la copa: el tamaño del núcleo subió (20.1/20.6)
y se conservan las 3 categorías deterministas (pequeño/mediano/principal,
~70/25/5%) y el halo de 3 niveles de la ronda anterior, ahora sobre una
base más grande. No se tocó la distribución por caras (ya sesgada hacia
superficies visibles desde la ronda anterior).

### 20.9. Partículas — sin cambios adicionales esta ronda

La frecuencia/escala/deriva de `MomentoCuyAbetoLeavesBlock` ya se ajustó
en la ronda anterior (docs/23 §19.8) y no se volvió a tocar — el encargo
pidió revisar el sistema existente antes de ampliarlo, y no se encontró
una causa nueva que justificara otro cambio sin antes ver el resultado
real en pantalla del ajuste ya hecho.

### 20.10. Estado real del Bloom

Sin cambios respecto a la ronda anterior: **no implementado**. El plan
técnico (`PostEffectProcessor` propio, extracción de brillo + desenfoque
con el programa `box_blur` reutilizable de vanilla + composición aditiva)
sigue siendo el mismo investigado y documentado en la sección 7 de este
documento — el encargo pidió explícitamente no avanzar aquí hasta que la
base (foco de suelo + copa) esté sólida, y además que no se simule con
halos más grandes presentándolo como Bloom real. Los halos de 3 niveles
de esta entrega siguen siendo geometría aditiva apilada, nunca
framebuffer.

### 20.11. Nuevos controles y comandos

`config/apuyawar_lighting.json`: `groundSpotRadius`, `groundSpotIntensity`
(nuevos, 20.3). Comandos nuevos: `/apuyawarlighting set radiofoco <valor>`,
`/apuyawarlighting set intensidadfoco <valor>`,
`/apuyawarlighting debug focos` (lista cada `GroundSpot` activo con su
posición ancla, altura de suelo y fundido — separado de
`debug posiciones`, que sigue mostrando los puntos de copa).

**Corrección de los presets** (sección 12 del encargo: "verifica que los
presets realmente actualicen todos los parámetros"): confirmado un hueco
real — `applyPreset()` nunca tocaba `fixedPointDensityMultiplier` (el
campo que el renderizador usa de verdad desde la ronda anterior) ni
`maxEffectDistance`/`simplifyDetailBeyondDistance`. Cambiar a HIGH dejaba
esos tres campos en lo que fuera que tuvieran antes. `LightingQuality`
ahora incluye los 8 campos relevantes de calidad/rendimiento, y
`applyPreset()` los aplica todos juntos.

### 20.12. Compatibilidad

Sin cambios de arquitectura respecto a la ronda anterior: sigue siendo
jar-in-jar (`include project(':apuyawar-lighting')`), sin la tarea
destructiva de limpieza de caché, `environment: "client"` para
`apuyawar-lighting`, integración opcional vía `FabricLoader.isModLoaded`
en `MomentoCuyLightingBridge`. No se investigó Iris/Sodium esta ronda
(no hay forma de probarlos en este entorno) — se mantiene sin afirmar
compatibilidad no verificada, igual que en la ronda anterior.

### 20.13. Rendimiento

No medido con FPS reales (sin control de teclado/ratón en este entorno,
igual que en todas las rondas anteriores). Cambios arquitectónicos
relevantes para el costo: el escaneo vertical ya no crece con el radio
horizontal (20.4, una mejora de costo, no un empeoramiento); el número de
focos de suelo cayó de "uno por cada 1-de-5 hojas" a "uno por celda
ocupada de 8×8" (menos geometría de decal, no más); el radio de puntos
subió pero el número de triángulos por punto no cambió (sigue siendo un
abanico de 14 segmentos). No se introdujo ninguna asignación nueva por
frame fuera de lo ya existente (los mapas de fundido son persistentes,
actualizados por tick, no recreados).

### 20.14. Pruebas ejecutadas

**Con evidencia real de ejecución**:
- `gradlew build` (4 módulos): éxito.
- `gradlew :apuyawar-lighting:compileJava`: éxito, sin advertencias.
- Dos ejecuciones reales de `gradlew :momento-cuy:runClient` (una antes y
  otra después del ajuste de tamaño de 20.1/20.6): mundo cargado por
  completo, comandos registrados, ningún error ni excepción en todo el
  proceso, incluidos varios ciclos de autoguardado.

**No verificado visualmente**: si el foco de suelo ahora se ve realmente
pequeño y estable en pantalla, si los puntos de la copa ahora se notan,
si el conjunto se parece más a la referencia — nada de esto se ha visto
renderizado todavía en este entorno.

### 20.15. Pruebas pendientes para el equipo

Exactamente las pruebas A-G de foco de suelo y las de follaje/partículas
que pidió el encargo, en este orden:

1. `/apuyawarlighting test` — confirmar que el punto sigue anclado al
   caminar/rotar cámara (ya se hacía en la ronda anterior, repetir).
2. Plantar/crecer un abeto, pararse cerca de noche. Comparar el tamaño
   del foco de suelo contra la sombra/proyección de la copa — debería ser
   claramente más pequeño que antes (radio ~1.6 bloques, antes ~3).
3. Caminar alrededor y debajo del árbol varias veces, incluyendo
   movimiento lateral puro y rotar sólo la cámara sin mover al jugador.
   El foco (o los 1-2 focos si el árbol cruza una frontera de celda) no
   debería reaparecer "por partes" — debería estar o no estar, completo.
4. `/apuyawarlighting debug focos` antes y después de caminar, para
   comparar las coordenadas ancla exactas.
5. `/apuyawarlighting decals` para aislar el foco de la copa.
6. Observar si los puntos de la copa ahora se notan más (tamaño subido) —
   si siguen pareciendo pequeños, el siguiente ajuste sería
   `lightRadiusMultiplier` o el propio comentario del 20.6 sobre volver a
   calibrar la base.
7. Probar en terreno irregular (hierba/piedra/pendientes) cerca del
   árbol — confirmar que el foco sigue la altura real del terreno sin
   flotar ni atravesar bloques.
8. Repetir con 5 y 20 árboles si es viable, anotando FPS con RGB on/off.
9. Confirmar Cerezo Celestial intacto (pétalos, destellos dorados, su
   propio foco de suelo con el nuevo sistema por celdas).
10. Confirmar que no reaparecieron los problemas de caché de Loom (jar-in-jar
    sigue siendo la única vía) y que el Panel K sigue nítido.

### 20.16. Diferencias restantes con la imagen de referencia

- **Follaje emisivo real** (20.7): no implementado — la copa brilla por
  billboards delante de las hojas, no por una propiedad del material.
- **Bloom real de framebuffer** (20.10): no implementado — el resplandor
  sigue siendo geometría aditiva apilada, no una extracción+desenfoque
  real.
- **Densidad/cobertura de puntos**: la referencia muestra decenas de
  puntos repartidos de forma muy densa por toda la copa; nuestra
  distribución depende de `pointsPerBlock` (3 para esmeralda) × la
  densidad configurada — puede seguir pareciendo más dispersa que la
  referencia incluso con el tamaño corregido.
- **Partículas separadas del árbol**: la referencia muestra motas
  flotando claramente fuera del volumen de la copa; el sistema actual
  (`MomentoCuyAbetoLeavesBlock`) genera partículas dentro/cerca de cada
  bloque de hoja, no con un alcance de dispersión mayor.
- **Forma del árbol**: ya documentado en la conversación — la silueta de
  la referencia no coincide con la geometría real del abeto
  (`straight_trunk_placer`/`spruce_foliage_placer`); esto es geometría,
  no iluminación, y el encargo pidió conservar los modelos originales.

## 21. APU YAWAR Lighting V3 — corrección de los 7 errores confirmados en video

El equipo grabó `errores.mp4` tras el commit `4364829` y describió, con
mucho detalle técnico (incluyendo su propia revisión de código de
`buildGroundSpots()`), 7 problemas concretos. **Nota de honestidad**: este
entorno no tuvo acceso al archivo de video en sí — ningún adjunto llegó
con esta ronda del encargo — así que el diagnóstico de esta sección se
basa por completo en la descripción textual del equipo (incluyendo cifras
exactas que reportaron, como `maxLuces=96 activos=6`) y en su propia
revisión de código, no en haber visto el video. Se dice esto explícitamente
en vez de dar a entender que se comprobó visualmente.

### 21.1. Error 2 primero — por qué sólo 6 emisores activos con `maxluces=96`

Este era el error raíz de varios de los demás (copa oscura, punta
oscura): con el cupo general en 96 y sólo 6 activos, el cupo nunca era el
cuello de botella — el problema estaba en que el **escaneo nunca
encontraba el resto de las hojas reales**. Revisando `EmitterTracker`
anterior a esta ronda:

- El escaneo era un cubo recalculado desde cero cada 10 ticks, con un
  límite duro `MAX_SCAN_RADIUS = 48` y una ventana vertical fija de
  `VERTICAL_SCAN_HALF_HEIGHT = 20` bloques **centrada en la Y del
  jugador en el momento del rescaneo** (no en la del árbol). Si el
  jugador estaba, por ejemplo, de pie en el suelo junto a un abeto cuya
  copa sube bastante por encima de su cabeza, buena parte de la copa
  podía quedar fuera de esa ventana de ±20 — explicando directamente por
  qué la punta (lo más alto) es lo primero en perderse.
- `HIGH` fijaba `maxEffectDistance = 56`, pero el radio de escaneo
  horizontal real estaba atado a una constante separada, no a ese valor
  — subir el preset no ampliaba lo que el escaneo realmente recorría.

**Corrección real (no sólo subir números)**: `EmitterTracker` ahora
indexa por **caché de chunk** (`scanChunkForEmitters`, `ChunkCache`,
`chunkKey`), no por cubo recalculado:

- Cada chunk cargado dentro del radio de escaneo se indexa **una vez**
  (16×16 columnas, ventana vertical de `CHUNK_CACHE_Y_WINDOW = 48`
  bloques alrededor de la Y del jugador **en el momento en que ese chunk
  se cachea por primera vez**, no recalculada cada rescaneo) y el
  resultado se reutiliza en los rescaneos siguientes, revalidando sólo
  el `BlockState` de cada posición ya cacheada (una consulta barata, no
  un recorrido del chunk entero).
- `MAX_NEW_CHUNK_SCANS_PER_RESCAN = 6` reparte el costo de "entrar a una
  zona nueva" en varios rescaneos en vez de un único golpe grande.
- `CHUNK_CACHE_TTL_RESCANS = 200` (≈100 s a 10 ticks/rescaneo) refresca
  la caché de un chunk para notar árboles nuevos o crecidos.
- `MAX_SCAN_RADIUS` subido de 48 a 80 — viable ahora porque el costo ya
  no es un cubo recalculado cada vez.

**Diagnóstico pedido explícitamente por el equipo**: nuevo comando
`/apuyawarlighting debug hojas [radio]` (por defecto radio 24, hasta 80),
wireado a `EmitterTracker.debugCountRealEmitters(...)` — un recorrido
**sin ninguna caché**, bruto, de un cubo fijo alrededor del jugador, que
cuenta por identificador de bloque (`hojas_pino_esmeralda`,
`hojas_pino_sombrio`, cualquier otro emisor registrado) cuántos bloques
reales hay, y lo imprime junto al número que `EmitterTracker.active()`
está renderizando ahora mismo más el límite `maxluces`/`distanciaMax`
vigentes. Esto permite distinguir, en una sola invocación, "pocas hojas
reales" de "el escaneo no las encuentra" de "el cupo las recorta" — las
tres causas que el equipo pidió diferenciar explícitamente.

**No verificado visualmente**: no se ha confirmado en pantalla cuántas
hojas reales tiene el abeto de prueba ni si el conteo tras la corrección
sube de 6 a un número mucho mayor — el comando nuevo está listo
precisamente para que el equipo haga esa comprobación.

### 21.2. Error 1 — la luz desaparecía a distancia

Causa real: la misma limitación de escaneo del §21.1 (ventana vertical
atada a la Y del jugador, radio horizontal con tope bajo) hacía que,
cuanto más lejos estuviera el árbol, más fácil era que su copa cayera
fuera de la ventana indexada o del radio máximo — **no** era un problema
de que `maxEffectDistance` y el radio de escaneo estuvieran mal
separados entre sí (eso ya se había corregido en la V2, §20.4); era que
el propio escaneo, aunque usara `maxEffectDistance` correctamente para
el corte de distancia, no llegaba a indexar esas posiciones en primer
lugar más allá de la ventana vertical fija. La caché por chunk (§21.1)
ataca esto directamente: `CHUNK_CACHE_Y_WINDOW = 48` es más del doble
de la ventana anterior, y como ahora es barato mantener el resultado
cacheado, `MAX_SCAN_RADIUS` pudo subir de 48 a 80 sin recalcular un cubo
completo cada 10 ticks.

No se tocó el tamaño de ningún punto ni halo para "arreglar" esto — el
encargo pidió explícitamente no inflar el tamaño como sustituto de una
detección correcta.

### 21.3. Error 3 — la punta del árbol seguía sin iluminarse

Incluso corrigiendo la detección (§21.1), quedaba un segundo problema
real, independiente: una vez que `found` (la lista de emisores
detectados) supera `maxluces`, la selección anterior ordenaba por
`(prioridad desc, distancia a la cámara asc)` y se quedaba con los `N`
primeros — **puramente por cercanía a la cámara**. En un árbol visto
desde el lado o desde abajo, la punta (la parte más alta) es casi
siempre la más lejana en línea recta desde el jugador, nunca la más
cercana — así que, en cuanto el árbol tiene más hojas reales que
`maxluces`, el recorte por cercanía la descarta sistemáticamente antes
que ninguna otra parte de la copa. Esto es exactamente lo que describió
el equipo ("ignora la punta").

**Corrección**: `selectStratifiedByHeight(...)` (nuevo, en
`EmitterTracker`) reemplaza el recorte plano cuando `found.size() >
maxluces`. Divide los emisores detectados en tres bandas por **altura
real del propio árbol encontrado** (tercio inferior/medio/superior de
los bloques detectados, no de la pantalla ni de la posición del
jugador) y reparte el cupo total entre las tres bandas —
`distributeQuota(...)` cede el cupo que una banda no puede usar (por
tener menos candidatos reales que su parte proporcional) a las bandas
que sí tienen margen, en vez de desperdiciarlo. Dentro de cada banda se
conserva el mismo orden por prioridad+distancia de antes. Resultado: la
banda superior (donde está la punta) recibe su parte del cupo aunque
esté más lejos de la cámara que la banda inferior.

**No verificado visualmente**: no se ha confirmado en pantalla si,
con esto, la punta muestra puntos turquesa claramente visibles — el
comando `/apuyawarlighting debug posiciones` permite comparar las
coordenadas Y de los puntos activos contra la altura real de la copa
para verificarlo sin depender sólo de la vista.

### 21.4. Error 4 — el foco de suelo seguía desplazándose al caminar

El equipo revisó `buildGroundSpots()` y encontró la causa exacta por su
cuenta: aunque `cellKey` fuera estable, el representante elegido dentro
de esa celda se **recalculaba desde cero en cada rescaneo** ("la hoja
más alta, empate por `asLong()`") sobre el subconjunto de hojas que
hubiera pasado el recorte de ese momento concreto — y ese subconjunto
podía variar levemente (por el límite de luces, el borde del radio de
escaneo, o ahora también por el reparto por bandas del §21.3) sin que
el árbol cambiara en absoluto. Si la hoja "más alta disponible esta
vez" no era la misma que "la más alta disponible la vez anterior",
`anchorPos` saltaba, aunque siguiera siendo una hoja de la misma copa y
de la misma celda.

**Corrección**: `stickyGroundRepresentative` (mapa `celda → posición`,
persistente entre rescaneos). `buildGroundSpots(...)` ahora, para cada
celda: si ya había un representante pegajoso y **sigue siendo
candidato** en esa celda este rescaneo, lo reutiliza directamente, sin
recalcular "el mejor". Sólo si el representante anterior ya no es
candidato (se rompió, decayó, o salió de rango) se elige uno nuevo
-determinista, con el mismo criterio de antes- y se fija como el nuevo
pegajoso. Esto hace que el foco pertenezca de verdad al árbol (una vez
fijado, no se mueve mientras exista al menos una hoja de esa celda que
siga siendo esa misma posición), no a qué subconjunto de hojas pasó el
recorte en un instante dado.

### 21.5. Error 5 — el foco de suelo seguía demasiado brillante

Revisando `findGroundY(...)` con el código delante: `LeavesBlock` tiene
colisión completa (confirmado por bytecode en una ronda anterior de este
mismo documento), así que un rayo hacia abajo desde una hoja alta de una
copa densa y por capas —como la del abeto, con varias capas de hoja
apiladas verticalmente— encontraba casi siempre **otra hoja más abajo
del mismo árbol** antes de llegar al suelo real. Eso daba una
`verticalDistance` casi nula (la "hoja de abajo" está a 1-2 bloques,
no a los 5-10 reales hasta el suelo) y, por la fórmula de atenuación por
altura, una intensidad casi siempre cercana al máximo — muy probablemente
la causa real de "el suelo sigue demasiado brillante", con mayor peso que
el radio o la intensidad base.

**Corrección**: `findGroundY(...)` ahora salta explícitamente cualquier
bloque que sea él mismo un emisor registrado (otra hoja de la misma
copa, u otra copa solapada) y continúa el rayo hacia abajo hasta
encontrar un bloque con colisión o fluido real que NO sea una hoja
conocida. Además, `groundSpotIntensity` bajó de `0.55` a `0.4` como
ajuste complementario (no sustituto) — el encargo pidió separar
explícitamente cuánto de "demasiado brillante" viene del decal RGB
frente a la luminancia vanilla nivel 6 de `hojas_pino_esmeralda`; esa
luminancia **no se tocó** esta ronda (no se eliminó ni se redujo sin
evaluar su efecto en el resto del bosque, tal como pidió el encargo). La
comparación real entre ambas fuentes (`/apuyawarlighting decals` para
aislar el aporte RGB, apagar `rgbEnabled` para ver sólo la luminancia
vanilla) queda como prueba pendiente del equipo, listada en §21.8.

### 21.6. Error 6 — partículas celestiales sin densidad controlable real

El campo `particleDensityMultiplier` existía en `LightingConfig` desde
antes, pero no llegaba a ningún sitio: las hojas de `momento-cuy`
(`MomentoCuyAbetoLeavesBlock`) son bloques **comunes**, registrados
incondicionalmente incluso en servidores dedicados sin el motor RGB
instalado — no pueden importar directamente ninguna clase de
`apuyawar-lighting` (`environment: "client"`) sin romper esa garantía.

**Arquitectura nueva** (3 piezas, cada una con un propósito concreto):

1. `MomentoCuyParticleTuning` (nuevo, `momento-cuy`, paquete común, sin
   ningún import de `apuyawar-lighting`): guarda un único
   `volatile float movingParticleDensityMultiplier` (clamp 0.0–4.0), con
   getter/setter estáticos. Si `apuyawar-lighting` no está instalado,
   se queda en `1.0` — comportamiento normal, sin ningún control
   externo.
2. `MomentoCuyAbetoLeavesBlock.randomDisplayTick(...)`: la frecuencia ya
   no es un número fijo (`4`/`12`) — se divide por
   `MomentoCuyParticleTuning.movingParticleDensityMultiplier()`
   (densidad `0` desactiva las partículas directamente, en vez de
   calcular una probabilidad sin sentido; densidad más alta → divisor
   de probabilidad más bajo → partículas más frecuentes).
3. `MomentoCuyLightingBridge.register()` (cliente, ya protegido por
   `FabricLoader.isModLoaded("apuyawar_lighting")`): registra un
   `ClientTickEvents.END_CLIENT_TICK` que sincroniza, cada tick,
   `ApuYawarLightingClient.config().particleDensityMultiplier` hacia
   `MomentoCuyParticleTuning` — así un cambio del comando se nota sin
   recargar el mundo.

Nuevo comando: `/apuyawarlighting set particulasmoviles <valor>` —
separado a propósito de `set puntosfijos` (densidad de los puntos LED
fijos de la copa), exactamente la distinción que pidió el encargo: *"la
densidad de partículas móviles debe estar realmente conectada a un
control funcional, sin confundirla con la densidad de puntos LED
fijos"*.

**No ampliado esta ronda**: la cantidad/variedad de partículas en sí
(número de chispas simultáneas, dispersión fuera del volumen de la
copa, un sistema de partículas de cliente dedicado) — el encargo pedía
primero conectar la densidad existente a un control real antes de
ampliar el sistema, y esa es la pieza que faltaba. Si, tras probar el
nuevo comando, la cantidad máxima alcanzable (densidad 4.0) sigue sin
ser suficientemente visible, la siguiente ronda debería evaluar un
sistema de partículas de cliente dedicado, tal como el encargo autorizó
condicionalmente.

### 21.7. Error 7 — la copa seguía demasiado oscura

Tratado deliberadamente **en último lugar**, siguiendo el orden
obligatorio del encargo: la copa oscura era, con alta probabilidad, un
síntoma de los Errores 1-3 (detección incompleta + recorte sesgado por
cercanía), no un problema de tamaño o intensidad por sí solo. No se
volvió a subir el radio base (`0.11` bloques, fijado en la V2) ni la
intensidad general esta ronda — el comentario correspondiente en
`WorldLightingRenderer` se actualizó para documentar por qué, en vez de
cambiarse a ciegas otra vez. El material emisivo real (la opción 2/3 de
§20.7) sigue sin implementarse, por las mismas razones de riesgo no
verificable documentadas en esa sección — no se afirma haber implementado
una capa emisiva real cuando lo que existe sigue siendo geometría
aditiva flotando delante del bloque.

Si, tras corregir la detección y el reparto por bandas, la copa sigue
viéndose oscura en una prueba real, el siguiente paso sería recalibrar
`lightRadiusMultiplier`/`lightIntensityMultiplier` — pero sólo después de
confirmar con `/apuyawarlighting debug hojas` que el número real de
puntos renderizados ya es alto, para no repetir el error de subir
tamaño/intensidad sobre una detección todavía incompleta.

### 21.8. Pruebas ejecutadas con evidencia real

- `gradlew build` (4 módulos, dos ejecuciones completas tras cada tanda
  de cambios): **éxito**, incluyendo `apuyawar-systems:verifyJudgments`
  (128 objetivos, sin regresión) y `momento-cuy:processIncludeJars`
  (jar-in-jar intacto, sin la tarea destructiva de limpieza de caché).
- `gradlew :apuyawar-lighting:compileJava` tras el rediseño completo de
  `EmitterTracker` (caché por chunk, reparto por bandas, representante
  pegajoso, `findGroundY` corregido): éxito. Se detectó y corrigió antes
  de este resultado un error real de API (`world.getTopYInclusive()` no
  existe en 1.21.1; se verificó con `javap` contra el jar de Minecraft
  cacheado en este entorno que los métodos reales son
  `getBottomY()`/`getTopY()` — exclusivo — y se usó `getTopY() - 1`).
- `gradlew :momento-cuy:runClient`: arranque completo hasta la pantalla
  de título (carga de mods, mixins, recursos, atlas de texturas, motor
  de sonido), mod `apuyawar_lighting` presente en la lista de 57 mods
  cargados, los tres logs `[DIAG]` de inicialización del cliente
  aparecen sin ninguna excepción. **Esta vez, a diferencia de rondas
  anteriores, el proceso no llegó a cargar un mundo real** — el arranque
  automatizado no hace clic en "un jugador"/"cargar mundo" (no hay
  control de entrada en este entorno) y el proceso se cerró tras el
  tiempo de espera sin que nadie lo uniera a una partida. No se puede
  afirmar para esta ronda, por tanto, que el escaneo por caché de chunk
  o el reparto por bandas se ejecutaran de verdad contra bloques reales
  — sólo que el cliente arranca y los comandos se registran sin
  excepciones. Ver `/tmp/runclient_v3.log` de esta sesión.

**No verificado visualmente — ninguna de las siguientes afirmaciones se
ha comprobado en pantalla**, porque este entorno no tiene control de
entrada ni captura de imagen, sólo registro de texto:

- Si el conteo real de hojas (`/apuyawarlighting debug hojas`) ahora
  supera los 6 anteriores y de cuánto se trata en el árbol de prueba.
- Si la punta del abeto muestra puntos turquesa visibles tras el
  reparto por bandas (§21.3).
- Si el foco de suelo deja de desplazarse al caminar/rotar cámara
  (§21.4) y si su brillo ya es razonable (§21.5).
- Si la densidad de partículas móviles, ajustada por el nuevo comando,
  produce un cambio visible real (§21.6).
- Si la copa, en conjunto, ya se parece más a la referencia (§21.7).

### 21.9. Pruebas pendientes para el equipo — las 10 solicitadas, en orden

Usar **un único abeto de prueba** y anotar la coordenada de su tronco con
`/apuyawarlighting debug posiciones` nada más plantarlo/encontrarlo
(apuntar la posición más baja reportada, que corresponde a hojas cerca
de la base), para poder comparar todos los resultados contra el mismo
árbol:

1. **Rotar la cámara sin moverse**: `/apuyawarlighting debug focos` y
   `debug posiciones` antes/después — ninguna coordenada debería
   cambiar.
2. **Caminar en círculo alrededor del árbol**: repetir `debug focos` en
   4-6 puntos del círculo — el `ancla=` de cada foco no debería
   cambiar mientras el árbol siga en rango (corrección del §21.4).
3. **Alejarse y acercarse** (10/20/30/45/60 bloques, respetando chunks
   cargados): `/apuyawarlighting debug hojas 80` en cada distancia,
   comparando el total de hojas reales (que no cambia, es el mismo
   árbol) contra "Puntos de copa renderizados ahora mismo" — debería
   mantenerse estable mientras la distancia sea menor que
   `distanciaMax`, y reducirse progresivamente más allá (nunca
   desaparecer de golpe).
4. **Mirar desde arriba y desde abajo**: confirmar que la punta
   (§21.3) se ve iluminada en ambos casos, no sólo desde un ángulo.
5. **Mirar directamente a la punta**: anotar si hay puntos turquesa
   claramente visibles ahí, comparando con `debug posiciones` (buscar
   las coordenadas Y más altas de la lista).
6. **Emisor de prueba independiente** (`/apuyawarlighting test`):
   confirmar que sigue anclado al caminar/rotar, sin relación con los
   cambios de esta ronda (ya se probó en rondas anteriores, repetir
   como control).
7. **RGB con decals on/off** (`/apuyawarlighting decals`): aislar
   cuánto brillo de suelo viene del decal frente a la luminancia
   vanilla de `hojas_pino_esmeralda` (§21.5) — apagar también
   `rgbEnabled` para ver sólo la luminancia vanilla, sin ningún decal
   ni punto de copa.
8. **Presets LOW/MEDIUM/HIGH** (`/apuyawarlighting preset <nombre>`):
   confirmar que los 8 campos de calidad cambian juntos (ya corregido
   en la V2, repetir como control) y que `debug hojas`/`debug emisores`
   reflejan el cambio de `maxluces`/`distanciaMax`.
9. **Árbol con partículas habilitadas/deshabilitadas**: probar
   `/apuyawarlighting set particulasmoviles 0`, `1`, `4` y observar si
   la frecuencia de chispas cambia visiblemente (§21.6).
10. **Varios árboles cercanos**: plantar o encontrar 2-3 abetos juntos,
    repetir `debug focos` — debería seguir habiendo como mucho un foco
    por celda de 8×8 ocupada, nunca uno por árbol si comparten celda ni
    uno por hoja.

En cada prueba, registrar: emisores seleccionados (`debug emisores`),
focos activos (`debug focos`), posiciones ancla (`debug posiciones`) y
el momento de cada actualización (cada ~0.5 s, `SCAN_INTERVAL_TICKS =
10`), tal como pidió el encargo.

### 21.10. Qué se conservó sin tocar

Panel K y su nitidez, la Ruleta y sus ocho colores, los modelos/crecimiento
de los árboles (ninguna geometría de `MomentoCuyAbetoLeavesBlock` ni de los
generadores de árbol se modificó), el Cerezo Celestial y sus pétalos
(`MomentoCuyBlossomLeavesBlock` no se tocó esta ronda, ver §21.6 sobre por
qué el cerezo no se incluyó en el wireado de densidad todavía), el Cuy
Jardinero 3D, la dimensión Momento Cuy, los bloques personalizados, los
comandos existentes, la integración jar-in-jar (sin reintroducir ninguna
limpieza destructiva de caché de Loom) y la compatibilidad con Fabric
1.21.1.

### 21.11. Comandos nuevos de esta ronda

```
/apuyawarlighting debug hojas [radio]        -- conteo real de hojas emisoras (sin caché) vs. renderizadas
/apuyawarlighting set particulasmoviles <v>  -- densidad de partículas MÓVILES (0.0-4.0), separada de "puntosfijos"
```

### 21.12. Instrucciones exactas para probar en Minecraft

1. `cd E:\APU_YAWAR && gradlew.bat :momento-cuy:runClient` (o desde el
   launcher de desarrollo habitual).
2. Cargar el mundo guardado existente (o uno nuevo), ir de día o de
   noche a un Abeto Celestial.
3. `/apuyawarlighting estado` — confirmar `RGB=true` y anotar
   `maxLuces`/`activos`/`distanciaMax` actuales.
4. `/apuyawarlighting debug hojas 24` — anotar el total real de hojas
   emisoras en un radio de 24 bloques alrededor del jugador, desglosado
   por tipo de bloque.
5. Seguir la secuencia de pruebas de §21.9, anotando los comandos
   `debug emisores`/`debug focos`/`debug posiciones` en cada paso.
6. Si algo sigue mal, citar el paso exacto de §21.9 en el que ocurre y
   el resultado exacto de los comandos `debug`, no sólo una descripción
   visual — así la siguiente ronda puede partir de números, no de una
   nueva grabación.

## 22. Clases viejas en `runClient` — causa raíz real y arreglo definitivo

Tras subir la V3, el equipo reportó evidencia concluyente de que Minecraft
seguía ejecutando una versión antigua de `LightingCommands.java`:
`/apuyawarlighting estado` mostraba el formato viejo (`particulas=...`, sin
la segunda línea de `decals`/`radioFoco`/`distanciaMax`), y el árbol de
sugerencias de `/apuyawarlighting` sólo ofrecía `preset`/`set`/`test`/
`toggle` -- ni `decals` ni `debug`, aunque ambos estaban en el código del
commit `a052613`. Habían probado
`.\gradlew.bat :apuyawar-lighting:build :momento-cuy:runClient --rerun-tasks`
sin éxito.

### 22.1. Diagnóstico pedido por el equipo -- implementado tal cual

Se añadió un diagnóstico temporal en `LightingCommands.register()`
(`logClassOrigin()`, queda en el código hasta que el equipo confirme en
vivo que ya no hace falta) que imprime, apenas arranca el cliente -- sin
necesidad de cargar un mundo, ya que `register()` corre durante
`onInitializeClient()`:

- `BUILD_FINGERPRINT`, una constante que se cambia a mano en cada
  investigación, para confirmar sin ambigüedad qué compilación corrió.
- `LightingCommands.class.getProtectionDomain().getCodeSource().getLocation()`.
- `getClassLoader().getResource(...)` y, más importante,
  `getClassLoader().getResources(...)` (plural) -- lista TODAS las copias
  de esa clase visibles para el classloader, con una advertencia si hay
  más de una (colisión real de classpath).
- Lo mismo para `ApuYawarLightingClient.class`.

### 22.2. Causa raíz confirmada, paso a paso

1. **Prueba directa de que corría bytecode viejo**: se recompiló
   `apuyawar-lighting` con el diagnóstico nuevo (confirmado con `javap`/
   extracción directa del .class que el .jar fresco SÍ tenía
   `BUILD_FINGERPRINT`), se corrió
   `gradlew :apuyawar-lighting:build :momento-cuy:runClient --rerun-tasks`
   -- exactamente el comando que había probado el equipo -- y el log del
   juego **no mostró ninguna línea nueva de diagnóstico**, sólo las 3
   líneas `[DIAG]` antiguas. Prueba directa, por ejecución real, de que
   el `.class` que corre no es el que se acaba de compilar.
2. **Localización de la causa**: `gradlew :momento-cuy:dependencies
   --configuration runtimeClasspath` mostró `apuyawar-lighting` resuelto
   bajo el identificador sintético
   `net_fabricmc_yarn_1_21_1_1_21_1_build_3_v2.pe.apuyawar:apuyawar-lighting:0.1.0`
   -- el MISMO esquema de nombres que usa Loom para dependencias
   EXTERNAS (coincide con la ruta real en disco,
   `.gradle/loom-cache/remapped_mods/net_fabricmc_yarn_.../pe/apuyawar/apuyawar-lighting/0.1.0/apuyawar-lighting-0.1.0.jar`,
   un archivo de 29772 bytes con fecha de las 01:53, muy anterior a
   cualquier cambio de esta sesión). Confirmado extrayendo el `.class`
   real de ese jar: contenía el código VIEJO (sin `puntosFijos` ni
   `decals` ni `debug`, exactamente el síntoma reportado).
3. **Por qué `modImplementation project(':apuyawar-lighting')` causaba
   esto**: al declarar la dependencia con el prefijo `mod*`, Loom la
   procesa con el MISMO pipeline de remapeado-y-caché que usa para mods
   externos (artefactos de Maven, publicados una sola vez por versión,
   donde cachear por (id, version) es seguro porque el contenido nunca
   cambia sin cambiar la versión). Para un proyecto HERMANO en pleno
   desarrollo, esa suposición es falsa: el contenido cambia constantemente
   sin que `version` (`0.1.0`) cambie nunca.
4. **Por qué ni `--rerun-tasks` ni `--refresh-dependencies` lo arreglaban
   -- confirmado empíricamente, no sólo supuesto**:
   - `--rerun-tasks` fuerza a re-ejecutar *Tareas* de Gradle con
     up-to-date-check obsoleto. Esta caché no es la salida de ninguna
     Tarea declarada -- es caché de resolución de dependencias/
     transformación de artefactos, interna de Loom, y `--rerun-tasks` no
     tiene ningún efecto sobre ella (confirmado: se recompiló
     `apuyawar-lighting`, se re-resolvió `runtimeClasspath` de
     `momento-cuy` DOS veces, con `--rerun-tasks` real, y el archivo
     cacheado no cambió ni un byte ninguna de las dos veces).
   - `--refresh-dependencies` sí tocó el archivo (nueva fecha de
     modificación, tras ~3m45s), pero **sirvió el mismo contenido viejo**
     -- confirmado extrayendo el `.class` de nuevo tras el refresh y
     comparando: seguía sin el fingerprint más reciente.
   - Sólo **borrar el archivo a mano** forzaba una regeneración real,
     con el contenido actual -- confirmado tres veces en esta sesión. Pero
     el equipo pidió explícitamente no depender de limpiar cachés a mano
     ni de una tarea que borre un jar del classpath antes de arrancar
     Minecraft (ver §18 de este documento: una tarea así ya causó un
     problema peor, una condición de carrera real, en una ronda
     anterior).

### 22.3. Arreglo real aplicado

La documentación oficial de Fabric Loom, para el caso exacto de "un
proyecto Loom depende de otro proyecto Loom del mismo build", recomienda
la configuración `namedElements` del proyecto dependido en vez de una
dependencia `mod*`:

```groovy
implementation project(path: ':apuyawar-lighting', configuration: 'namedElements')
```

`namedElements` expone la salida del proyecto que **no** se remapea a
nombres intermediarios (al contrario que la variante por defecto de un
`project(...)`, pensada para producción) -- como ambos proyectos ya
comparten las mismas mappings con nombre (Yarn) dentro del mismo build de
Gradle, no hace falta ninguna traducción intermediary/named entre ellos,
así que tampoco hace falta pasar por la caché de "mods remapeados" que
causaba el problema. Al ser una dependencia normal (no `mod*`), sigue
llegando a `runtimeClasspath` -- necesario para que
`MomentoCuyLightingBridge` (protegido por `FabricLoader.isModLoaded`)
encuentre las clases en tiempo de ejecución -- pero apuntando siempre a
la salida compilada actual del proyecto, con el seguimiento normal de
Gradle, nunca a un artefacto cacheado aparte.

`include project(':apuyawar-lighting')` (jar-in-jar, para la distribución
real) se deja intacto -- nunca fue la causa: siempre tomó el jar
directamente de `:apuyawar-lighting:remapJar`, confirmado en esta misma
investigación extrayendo el `.class` del jar anidado dentro del jar de
distribución de `momento-cuy` y comprobando que también tenía el
fingerprint más reciente.

**Intentos intermedios que se probaron y se descartaron** (documentados
para que no se repitan sin motivo):
- `compileOnly project(':apuyawar-lighting')` (sin `include` ni ningún
  prefijo `mod*`): arregló la caché obsoleta, pero `apuyawar_lighting`
  dejó de aparecer en la lista de mods del cliente en desarrollo (56 mods
  en vez de 57) -- `compileOnly` nunca llega a `runtimeClasspath`, así
  que `fabric.mod.json` del módulo nunca estuvo en el classpath real, y
  `MomentoCuyClient` nunca llegó a invocar
  `ApuYawarLightingClient.onInitializeClient()` (ni un solo log `[DIAG]`
  apareció).
- `modCompileOnly project(':apuyawar-lighting')`: mismo resultado -- el
  prefijo `mod` no cambia que "CompileOnly" nunca contribuye a
  `runtimeClasspath`.

### 22.4. Verificación real ejecutada

Con el diagnóstico de §22.1 y el arreglo de §22.3 aplicados:

- `gradlew :momento-cuy:dependencies --configuration runtimeClasspath`:
  `apuyawar-lighting` aparece como `+--- project :apuyawar-lighting`, sin
  ningún identificador sintético de Loom.
- `gradlew :momento-cuy:compileJava`: éxito.
- `gradlew :momento-cuy:runClient` (normal, SIN `--rerun-tasks`,
  ejecutado DOS VECES por separado, sección "9. Vuelve a iniciar el
  cliente una segunda vez" del encargo): en ambas, el log mostró
  `Loading 57 mods` (con `apuyawar_lighting` en la lista) y
  `BUILD_FINGERPRINT=V3-classload-diag-a052613+04-namedElements-fix` --
  el valor exacto fijado en esta ronda, **no** el de ninguna ronda
  anterior -- con `codeSource=.../apuyawar-lighting/build/devlibs/apuyawar-lighting-0.1.0-dev.jar`
  y una sola entrada en `getResources()` (sin advertencia de colisión).
- `gradlew build` (4 módulos): éxito, incluyendo
  `apuyawar-systems:verifyJudgments` (128 objetivos, sin regresión --
  confirma que Panel K/Ruleta no se vieron afectados, ya que este cambio
  es sólo de configuración de dependencias de `momento-cuy`, sin tocar
  ningún código de `apuyawar-systems`).
- Extracción directa del `.class` realmente cargado
  (`apuyawar-lighting/build/devlibs/apuyawar-lighting-0.1.0-dev.jar`,
  confirmado por `codeSource` arriba): contiene `puntosFijos`, `decals`,
  `hojas`, `focos` y `particulasmoviles` -- los 4 nombres de comando que
  el equipo reportó como ausentes.
- Extracción del `.class` dentro de `META-INF/jars/apuyawar-lighting-0.1.0.jar`
  en `momento-cuy/build/libs/momento-cuy-0.1.0.jar` (la ruta de
  PRODUCCIÓN, jar-in-jar real): también contiene el fingerprint más
  reciente -- confirma que el empaquetado de distribución nunca estuvo
  afectado por este problema.

### 22.5. Lo que queda pendiente -- honestamente

Siguiendo la instrucción del encargo ("si no puedes ejecutar los
comandos mediante la interfaz, verifica al menos el bytecode real
cargado y deja claramente pendiente la prueba interactiva"): **no se
pudo escribir `/apuyawarlighting estado` ni ningún otro comando dentro
del juego** -- este entorno no tiene control de teclado/ratón, sólo
arranca el proceso real y lee sus logs. Lo verificado es que el bytecode
que el juego carga, confirmado por su propio `ProtectionDomain` en tiempo
de ejecución, es el actual y contiene las cadenas de texto de los
comandos nuevos. Falta que el equipo, dentro del juego:

1. Confirme que `/apuyawarlighting estado` imprime `puntosFijos=` y la
   segunda línea de `decals`/`radioFoco`/`intensidadFoco`/`distanciaMax`.
2. Confirme que `/apuyawarlighting decals`, `debug hojas 24` y
   `debug focos` aparecen en las sugerencias y ejecutan sin error.
3. Confirme que Momento Cuy (bloques, Cuy Jardinero, dimensión) y el
   Panel K siguen funcionando con normalidad -- no se tocó ningún código
   de esos sistemas, sólo la configuración de dependencias de
   `momento-cuy/build.gradle`, pero la confirmación visual queda
   pendiente del equipo.

Una vez confirmado, el diagnóstico de §22.1 (`logClassOrigin`,
`BUILD_FINGERPRINT`) se retira en un commit aparte, igual que los logs
`[DIAG]` originales de `ApuYawarLightingClient`/`LightingCommands`.
