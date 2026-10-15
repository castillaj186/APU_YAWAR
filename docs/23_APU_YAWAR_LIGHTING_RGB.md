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

**Estado real de las pruebas — más fuerte que en entregas anteriores de
este proyecto**: por primera vez en esta serie de encargos, este entorno
sí tiene GPU/pantalla disponible. Se ejecutó `gradlew :momento-cuy:runClient`
dos veces: la primera confirmó que todos los shaders/modelos/recursos
cargan sin error; la segunda llegó a cargar el mundo guardado
("Mundo nuevo") real, con el jugador dentro, y estuvo **más de 2 minutos
en una partida real corriendo** (con ciclos de autoguardado normales de
las 4 dimensiones, incluida `momento_cuy:momento_cuy`) sin ningún error ni
excepción — es decir, `WorldRenderEvents.AFTER_TRANSLUCENT` (donde se
dibuja el resplandor) se ejecutó con éxito en cientos de frames reales. El
archivo `config/apuyawar_lighting.json` se creó solo, con los valores
correctos del perfil MEDIO, confirmado leyéndolo tras la sesión. **Lo que
sigue sin poder confirmarse** es la calidad visual real (si el halo se
parece a la imagen de referencia, si los colores/tamaños quedan bien) y
el rendimiento con árboles reales cerca — no hubo forma de enviar teclas o
mover al jugador dentro de esa sesión automatizada (no hay una herramienta
de captura/control de pantalla disponible en este entorno), así que el
jugador apareció en el Overworld por defecto, no necesariamente cerca de
un Abeto/Cerezo Celestial plantado. Esa prueba visual queda para el
equipo, con instrucciones exactas en la sección 15.

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
`EmitterTracker`, `WorldLightingRenderer`, `GlowRenderLayer`,
`LightingCommands`, el propio `ApuYawarLightingClient`). Esto no es sólo
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

**Billboards aditivos con prueba de profundidad real**, no partículas de
vanilla y no un shader de postproceso que recolorea toda la pantalla.
Cada punto de luz es un cuadrado pequeño orientado a la cámara
(`Camera#getRotation()`, el mismo patrón que usa cualquier partícula
billboard de vanilla), dibujado con la capa de render propia
`GlowRenderLayer.GLOW` (ver sección siguiente), con mezcla
`ADDITIVE_TRANSPARENCY` (`SrcFactor.ONE, DstFactor.ONE` — confirmado por
bytecode real de `RenderPhase`) y `LEQUAL_DEPTH_TEST` activo. Como el
`Framebuffer` principal de Minecraft (color + profundidad) ya está
enlazado durante todo `WorldRenderEvents` (confirmado por bytecode de
`GameRenderer`/`MinecraftClient`: `beginWrite(true)` envuelve el frame
completo, incluyendo el mundo), la prueba de profundidad contra el
terreno ya dibujado **funciona automáticamente sin ningún enlace manual**
— un muro delante del árbol SÍ oculta el halo, porque el GPU descarta los
fragmentos cuyo depth es mayor que el del muro ya presente en el buffer.
No se escribe en el depth buffer (`writeMaskState(COLOR_MASK)`, no
`ALL_MASK`), igual que cualquier capa translúcida de vanilla, para que el
halo no oculte partículas/entidades detrás de él.

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
(`RenderPhase.COLOR_PROGRAM`, respaldado por
`assets/minecraft/shaders/core/position_color.json`/`.vsh`/`.fsh` reales
del juego — el mismo que usa, por ejemplo, el rayo de las tormentas). Como
nuestro halo es exactamente eso (un color sólido con alfa, sin textura),
se reutiliza ese shader de vanilla directamente — cero riesgo de GLSL
nuevo mal escrito que no se pueda verificar visualmente sin GPU.

**Bloom real (framebuffer de extracción + desenfoque + composición) —
investigado pero NO implementado en esta entrega, explicado abajo en la
sección 7.** El "resplandor" que sí se entrega es una técnica distinta y
más simple: ver sección 7.

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
`matrixStack()`, `camera()`, `consumers()` (el `VertexConsumerProvider`
donde se envían los vértices), `world()` y `projectionMatrix()`/
`positionMatrix()` — **pero no expone ningún `Framebuffer` ni textura de
profundidad** (se verificó explícitamente que no existe ese método; esto
descartó una suposición inicial). El framebuffer principal se obtiene por
separado, cuando hace falta, vía `MinecraftClient.getInstance().getFramebuffer()`.

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

- El halo se dibuja con la `RenderLayer`/`VertexConsumerProvider`
  estándar del juego, el mismo mecanismo que usan las partículas y
  entidades vanilla — no hay ningún mixin que reemplace clases internas
  del renderizador. Esto reduce (no elimina) el riesgo de conflicto con
  mods de renderizado como Sodium, que sí suelen reemplazar partes
  internas del pipeline de chunks/entidades.
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
  `apuyawar-systems:verifyJudgments` sin cambios (128/135).
- `gradlew :apuyawar-lighting:compileJava` y `:momento-cuy:compileJava`:
  éxito — en particular, confirmó en el primer intento real varias firmas
  investigadas por bytecode (`SaplingGenerator` de 4 argumentos la ronda
  anterior; aquí, `RenderLayer.MultiPhaseParameters`/`RenderPhase`/
  `Camera#getRotation`/`MatrixStack`/`VertexConsumer` — todas correctas al
  primer intento gracias a la investigación previa por `javap`).
- **`gradlew :momento-cuy:runClient` — ejecución real con GPU, dos veces**:
  la primera confirmó carga de recursos (atlas de texturas, shaders,
  sonido) sin ningún error relacionado con `apuyawar_lighting` o Momento
  Cuy; la segunda cargó el mundo guardado real y corrió **más de 2
  minutos** con el jugador dentro, sin ninguna excepción — la primera vez
  en esta serie de entregas que se consigue una confirmación de ejecución
  real en vivo, no sólo de arranque de servidor o de compilación.
- Se confirmó que `config/apuyawar_lighting.json` se crea y contiene
  exactamente los valores por defecto esperados del perfil medio, leído
  directamente del disco tras la sesión real.

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
