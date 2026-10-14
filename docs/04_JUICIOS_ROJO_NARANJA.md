# APU YAWAR: retos rojos y dificultades naranjas

## Implementación

Los resultados rojo y naranja ahora producen consecuencias de servidor. Los comandos existentes `/apuyawar_ruleta forzar rojo` y `/apuyawar_ruleta forzar naranja` ejecutan la secuencia completa. La selección de color conserva las ocho probabilidades anteriores.

El servidor envía el ganador y un número de secuencia. El cliente confirma únicamente que terminó la presentación; no elige el evento ni sus resultados. El servidor espera al menos 314 ticks y los acuses de clientes conectados. Acuses duplicados, ajenos o de una tirada anterior se ignoran. Sin confirmación, la tirada se cancela a los 511 ticks; no comienza un juicio detrás de una animación pendiente. Una nueva tirada reemplaza la activación pendiente anterior. Los otros seis colores mantienen su animación y no ejecutan estos sistemas.

Rojo anuncia **MOMENTO DEL JUICIO**. Refugio/supervivencia usa **¡Escóndanse o sobrevivan!**; alturas, objetos, profundidad y viajes usan **¡Cumplan el reto antes de que termine el tiempo!**. El objetivo y tiempo aparecen en chat y el temporizador en bossbar. Naranja anuncia **EL JUICIO SE INTENSIFICA**, con **¡Una nueva dificultad ha despertado!**, nombre, efectos y duración.

## Catálogos recuperados

- `WeekEvents`: 12 retos rojos, 180 segundos cada uno, con resultado individual y comprobación al terminar. Se verificaron las coordenadas Y, techo, manos, escudo, arco, manzana dorada y antorcha de `LiveEvents.reviilPassed`.
- `DifficultyCatalog`: 35 dificultades Java, cinco por jornada durante siete jornadas. Se inspeccionaron `DifficultyEngine` y sus callbacks de daño, interacción, carga de entidades y actualización periódica.
- Datapack: **116 variantes rojas**, no 100 pese a un comentario desactualizado de `rojo:elegir`. Hay 44 de refugio, 40 de supervivencia, 11 de altura, 10 de gas, 8 de Nether y 3 de profundidad.
- Datapack: **100 entradas naranjas**. Algunas tenían solo anuncios o reglas incoherentes; se completaron con alternativas explícitas.

En total hay **128 entradas rojas ejecutables** y **135 entradas naranjas ejecutables**. Son variantes de familias de mecánicas, no 263 sistemas distintos. Las listas completas están en [catálogo rojo](05_CATALOGO_ROJO.md) y [catálogo naranja](06_CATALOGO_NARANJA.md).

## Alcance, objetivos y resultados

Los doce retos Java conservan resultados individuales. Las variantes del datapack conservan alcance global: todos los participantes deben cumplir; los retos de ubicación pueden acabar anticipadamente cuando el grupo está simultáneamente en la posición requerida. Los de supervivencia deben llegar vivos al final. Los retos Java verifican su objetivo al vencer el temporizador, como el sistema original; sostener un objeto al principio no basta.

Participan jugadores de supervivencia o aventura presentes al inicio. Creativos, espectadores y jugadores con la etiqueta `apuyawar_no_juicio` quedan excluidos. Morir, desconectarse o abandonar el modo elegible cuenta como fracaso y no se borra al reconectar. No se castiga automáticamente al compañero de un reto individual. Por defecto, quienes entran después esperan al próximo reto; esto es configurable. Solo puede existir un reto rojo y una dificultad naranja simultáneamente.

Los efectos naranja son globales. Los 35 del catálogo Java se aplican al Overworld, como su motor original. Las variantes naranjas se aplican a las entidades y condiciones adecuadas en dimensiones cargadas. Se atienden entidades recién cargadas y se actualizan hasta 256 criaturas cercanas por segundo, dentro de 38 bloques de jugadores activos.

## Adaptaciones explícitas

La reconstrucción no es idéntica en estas partes:

- Los refugios fijos del mundo antiguo no existen en este proyecto. Las claves 1–5 de `refuges` permiten fijar coordenadas y dimensión. Sin configurar una clave, se utiliza el spawn del Overworld con radio 12. Los 44 retos mantienen variantes de peligros, aunque los destinos sin configurar coinciden.
- TNT, meteoros y descargas se representan con impactos mágicos, daño y partículas. No se reproducen explosiones masivas de TNT ni cambios globales de clima. Los creepers sí son criaturas vanilla reales; sus explosiones normales dependen de las reglas del mundo. Las variantes eléctricas/nucleares usan creepers cargados, sin el radio nuclear histórico de 14.
- Las hordas usan criaturas inicializadas mediante APIs de Minecraft; se corrigen los NBT de efectos/equipamiento antiguos incompatibles. Se limitan cantidad y pulsos. Los peligros se etiquetan con dueño de evento y se eliminan al finalizar; los residuos de sesiones anteriores se descartan al cargar.
- Los giros secundarios históricos de intercambio/teletransporte se sustituyen por pulsos breves de oscuridad. No se transporta a jugadores a coordenadas inseguras ni se abre otra ruleta. El parámetro de un corazón sí modifica temporalmente la salud máxima y se limpia.
- Gas fijo/ascendente conserva umbral Y, retardo y exposición progresiva: daño 1–4 por segundo, aumentando cada cinco exposiciones, y recuperación de exposición al salir del gas.
- El fracaso se anuncia sin matar automáticamente al jugador ni retirar inventario. No se copian premios ligados a la economía/semanas anteriores. Los peligros durante el reto sí pueden matar; las recompensas y sanciones de finalización quedan pendientes de definir para APU YAWAR.
- Las dificultades permanentes/cumulativas antiguas pasan a una dificultad temporal configurable. Predeterminado: 1200 segundos, una jornada de Minecraft. No se restaura el calendario semanal antiguo ni la progresión de misiones de otros colores.
- Telarañas generadas se sustituyen por ralentización temporal. Los topes de animales bloquean reproducción en vez de matar animales; aldeanos bebés no se eliminan. Se bloquea el inicio de cura de aldeanos zombi en vez de destruir curas al azar.
- Se completan hojas/carbón/telarañas con probabilidades explícitas del 15% al romper bloques. Los mobs resultantes son temporales y tienen un límite.
- La entrada 87 realmente modificaba `forgiveDeadPlayers`, no una regla de portales; se corrige el anuncio. La entrada 90 no cambiaba la munición vanilla de esqueletos; se adapta a seguimiento +8 bloques. La entrada 81 incorpora fatiga bajo agua sosteniendo antorchas, evitando que sea solo una restricción ya impuesta por vanilla. La entrada 10 conserva la Fuerza I realmente aplicada por la función antigua, corrigiendo el anuncio de veneno.

Las condiciones de proximidad de algunos efectos del datapack se normalizan a 12 bloques. No se afirma identidad exacta con radios específicos antiguos. Se bloquea ordeño antes de consumir recursos; las dificultades de leche también convierten leche almacenada en cubos. Esos consumos, el daño y el desgaste son consecuencias reales y no se revierten al terminar.

## Limpieza y reinicio

Los atributos usan identificadores propios y modificadores temporales; no sobrescriben atributos base. El modificador no se duplica al refrescar. La velocidad nocturna se retira al hacerse de día. Se conserva el estado de creepers que ya estaban cargados antes del evento.

Los efectos propios tienen duración máxima de seis segundos, se refrescan según condición y se retiran al finalizar. Las pociones ajenas iguales o más fuertes se conservan. Una mejora temporal sobre un efecto débil guarda y restaura el efecto anterior con su tiempo restante. Tras un cierre abrupto, las mejoras con efecto oculto se dejan expirar brevemente para que Minecraft restaure el efecto anterior; no se aplica de nuevo ningún castigo. Los otros efectos marcados se limpian al cargar.

Las reglas de mundo/dificultad se guardan **antes** de modificarse en `apuyawar-rule-recovery.json`, dentro del mundo de desarrollo o del mundo donde el usuario ejecute el mod. Al detenerse se restauran los valores anteriores. Tras un cierre abrupto, se recuperan al arrancar el servidor. Los eventos temporales no se reanudan ni conceden premios repetidos.

No se escribieron archivos en `servidor`, `mundo_apuyawar`, datapack actual ni `apuyawar-world`. Las pruebas usan únicamente `apuyawar-systems/build/judgment-gametest`.

## Configuración

Al iniciar se crea `config/apuyawar-judgments.json` en el entorno que ejecuta Minecraft. Para `runClient`, normalmente está en `apuyawar-systems/run/config`. Se carga al iniciar el servidor; reinicia el mundo/servidor de desarrollo después de editarla.

| Campo | Valor inicial | Efecto |
|---|---|---|
| redEnabled / orangeEnabled | true | Activa cada sistema de jugabilidad |
| includeDatapackVariants | true | Incluye las 116/100 variantes en selección aleatoria |
| challengeSecondsOverride | 0 | 0 conserva cada duración; otro valor fija segundos de prueba |
| difficultySeconds | 1200 | Duración naranja, entre 1 y 86400 segundos |
| maxHazardMobs | 64 | Límite por motor, entre 0 y 128 |
| joinDuringChallenge | false | Permite incorporar jugadores que llegan después |
| disabledChallenges / disabledDifficulties | [] | IDs excluidos de inicio y selección |
| refuges | {} | Destinos por clave 1–5 |

Ejemplo de refugio, dentro de `refuges`:

```json
"1": { "dimension": "minecraft:overworld", "x": 100.5, "y": 70, "z": 200.5, "radius": 12 }
```

## Comandos y prueba manual

Todos los comandos administrativos requieren nivel de permiso 2. Usa un mundo de desarrollo con trucos.

```text
/apuyawar reto listar
/apuyawar reto listar 2
/apuyawar reto iniciar muro_viviente
/apuyawar reto iniciar ascenso_desesperado @s
/apuyawar reto iniciar juicio_071 @a
/apuyawar reto estado
/apuyawar reto detener
/apuyawar dificultad listar
/apuyawar dificultad activar zombis_feroces
/apuyawar dificultad activar flechas_crueles
/apuyawar dificultad activar adversidad_003
/apuyawar dificultad estado
/apuyawar dificultad detener
/apuyawar ruleta probar rojo
/apuyawar ruleta probar naranja
/apuyawar_ruleta forzar rojo
/apuyawar_ruleta forzar naranja
```

`detener` también cancela una activación de ruleta pendiente. Los comandos directos de inicio se rechazan mientras exista una presentación pendiente para evitar títulos solapados.

```powershell
.\gradlew.bat build
.\gradlew.bat :apuyawar-systems:runJudgmentGameTest
.\gradlew.bat :apuyawar-systems:runClient
```

La prueba GameTest ejecuta un servidor headless separado con jugadores simulados, sin instalar ningún JAR. Su informe está en `apuyawar-systems/build/judgment-gametest/reports/results.xml`. El mod auxiliar de pruebas no se incluye en el JAR de producción.

## Validación y límites

`verifyJudgments` comprueba los 128 objetivos, límites de altura/profundidad, objetos ausentes, techo, dimensión, supervivencia no automática, IDs únicos, catálogos sin identidad antigua y control de acuses. Se ejecuta en `check/build`.

GameTest prueba inicio/finalización de las 135 dificultades, atributos sin acumulación, restauración de reglas, recuperación de efectos, preservación de pociones ajenas, y los 128 retos con dos jugadores simulados usando coordenadas/equipamiento/dimensiones reales. También comprueba fracaso individual y limpieza de criaturas. Estos tests usan duraciones cortas y condiciones preparadas; no equivalen a jugar cada variante durante varios minutos.

Quedan por revisar con clientes humanos: presentación de títulos después de la ruleta, todos los callbacks de combate/interacción en sesiones reales, equilibrio de peligros, objetivos de gas prolongados, redes con latencia y rendimiento con muchos jugadores. No se afirma aprobación visual ni balance definitivo.

## Fuentes y dependencias eliminadas

Se inspeccionaron sin ejecutarlos los JAR `safio_ui-1.6.9-apuyawar-ruleta.jar` y `safio_content-0.3.0.jar`, así como `datapacks.zip`. Los volcados `javap` y las copias de análisis están en `assets-fuente/revision-jugabilidad-05`.

`CalendarService`, `WeekMissions` y `RouletteHistory` coordinaban fechas, misiones e historial mediante scoreboards; no son necesarios para activar estos juicios. `ReturnPoints` y `NutriaLobby` pertenecían a la dimensión/NPC históricos y no se cargan. Los siete minijuegos adicionales de `WeekEvents` corresponden al resultado rosa y quedan fuera de esta etapa. No se usan paquetes, entidades, scoreboards, dimensiones, funciones ni JAR antiguos como dependencia de ejecución.

La lista exacta de cambios y archivos nuevos está en [archivos de implementación](07_ARCHIVOS_JUICIOS.md).
