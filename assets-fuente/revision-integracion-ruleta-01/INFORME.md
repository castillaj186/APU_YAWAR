# APU YAWAR — integración visual de ruleta 01

## Recursos y geometría

Las 13 texturas procesadas se encuentran en `texturas/` y se han copiado al directorio activo de Systems. Los originales de `assets-fuente/referencias` se conservan intactos. `respaldo-activo/` conserva las 13 texturas anteriores y el renderer previo a esta integración.

Todas las capas son PNG RGBA 512 × 512. Pivote común: (256,256). Los ocho sectores usan radios 22 y 169 px y apertura matemática 45°. El sector i ocupa [-90°+45°i,-45°+45°i], con el eje Y hacia abajo. Orden: rojo, rosa, morado, azul, turquesa, verde, amarillo y naranja.

Se utilizó image_gen integrado para reconstruir los siete colores y sus símbolos, tomando el rojo como referencia de composición y cada ilustración original como referencia de identidad. Los prompts exactos están en `prompts-sectores.json`. Se mantuvieron montaña, corazón, luna, gota, viento, hojas, sol y fuego. No se comprimieron ni estiraron los símbolos.

El procesado determinista usa una máscara analítica común con 16 muestras por píxel. Reutiliza el mismo borde del rojo maestro en laterales, extremo interior y borde exterior de todos los sectores, con una transición de 2–3 px. Se normaliza la cobertura interior para evitar huecos procedentes de la transparencia del arte generado. En el PNG existen píxeles con alfa parcial de antialias; la exactitud angular corresponde a la geometría continua, rasterizada a 512 px.

## Otras capas

- Aro: escalado uniforme 0,375; centro de la ilustración original (627,606) trasladado a (256,256). El centro se conserva transparente; la opacidad residual se elimina dentro de radio 145 px sin recortar los adornos sólidos. El aro solapa el borde exterior de los sectores, sin cubrir sus símbolos.
- Medallón: escalado uniforme 0,077, desde (627,606) al pivote común. Diámetro principal aproximado de 86 px. Oculta la pequeña apertura central del disco y une los ocho extremos interiores.
- Puntero: escalado uniforme 0,09. La punta original (1007,631) se sitúa en (101,256). El cuerpo completo y la gema quedan a la izquierda; el resplandor arcoíris pertenece a la gema. No se extrajo de una rueda compuesta ni se añadió fondo de sectores.
- Glow y flash: escalado uniforme 0,40, centrados desde (627,617). Se conserva el detalle de los originales en el alfa y se convierte el RGB visible a blanco, para admitir tintado multiplicativo Java. Radio <=169 px transparente; transición suave de 169 a 182 px y salida suave al borde del lienzo. Los símbolos quedan fuera de la cobertura de ambos efectos.

## Vistas técnicas

- `vista_ruleta_completa.png`: ocho sectores, aro, medallón, halo y puntero en la orientación inicial.
- `ruleta_completa_512.png`: ensamblaje de 512 px con transparencia.
- `vista_puntero_aro.png`: ampliación para revisar gema, cuerpo, punta y solape con el aro.
- `vista_uniones.png`: ampliación de las uniones naranja/rojo y rojo/rosa.
- `vista_destello_rojo.png`: parada roja y destello próximo a su intensidad máxima.
- `vista_tamano_aprobado.png`: simulación en pantalla 1600 × 900; no es captura de Minecraft.

Las vistas se ensamblan con los propios PNG procesados mediante System.Drawing. No son ilustraciones generadas de una rueda hipotética.

## Archivos activos cambiados

En `apuyawar-systems/src/main/resources/assets/apuyawar_systems/textures/gui/roulette/`:

`sector_rojo.png`, `sector_rosa.png`, `sector_morado.png`, `sector_azul.png`, `sector_turquesa.png`, `sector_verde.png`, `sector_amarillo.png`, `sector_naranja.png`, `ring.png`, `medallion.png`, `pointer.png`, `glow.png`, `flash.png`.

En `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteRenderer.java`: únicamente el comentario y `WHEEL_DIAMETER`, de 386 a 458, para compensar el diámetro ornamental de la nueva textura. `SCREEN_HEIGHT_FRACTION = 0.42f` se conserva. No se cambia la rotación independiente del puntero ni los tiempos.

En esta carpeta de trabajo se añaden las fuentes artísticas reconstruidas, texturas procesadas, vistas, respaldo, manifest de huellas, este informe, prompts y el procesador reproducible (`PrepararRuleta.cs`, `ValidarRuleta.cs`, `preparar.ps1`). El procesador solo escribe en una carpeta nueva de assets-fuente y no integra recursos automáticamente.

## Comprobaciones

El validador de recursos comprobó formato y transparencia de los 13 PNG, cobertura sin huecos del interior de la rueda, límites de los ocho sectores con tolerancia de rasterizado de 1 px, alineación matemática de los ocho ganadores a las 9:00, efectos blancos sin cobertura de símbolos, centro del aro transparente y puntero sin recortes de lienzo con punta presente. Las 13 huellas de `manifest-integracion.json` coinciden entre los procesados y los recursos activos.

Después del build se abrió el JAR de verificación en modo lectura: sus 13 recursos de ruleta coinciden por SHA-256 con los PNG procesados. También se compararon las huellas previas y posteriores de los 13 originales y RouletteTimeline.java; los 14 archivos permanecen idénticos.

No se han modificado el servidor, mundo_apuyawar, datapack, fuentes de World ni RouletteTimeline.java. Se conservan los 375 ticks. El build genera solamente los artefactos de verificación habituales; no se instalan mods.

## Prueba en Minecraft

Desde E:\APU_YAWAR, ejecutar `.\gradlew.bat :apuyawar-systems:runClient`. Usar un mundo local de pruebas con comandos habilitados.

Ejecutar `/apuyawar_ruleta forzar rojo`, después los demás colores: rosa, morado, azul, turquesa, verde, amarillo, naranja. Esperar a que termine cada tirada (~18,75 s) antes de comenzar otra, pues el sistema actual reinicia la animación al recibir una nueva tirada. Para selección aleatoria: `/apuyawar_ruleta tirar`.

## Límites pendientes de revisión dentro del juego

El arte reconstruido mantiene símbolos e identidad, pero no reproduce cada píxel de las referencias. Hay variaciones leves en textura y pequeños ornamentos interiores, aunque la geometría y el borde maestro son comunes. Los símbolos giran con su sector, como en el sistema anterior.

Los detalles muy pequeños del medallón y el puntero pueden perder legibilidad en escalas de GUI altas. Las uniones rasterizadas pueden mostrar una línea fina durante el giro, según el filtrado de Minecraft. El diámetro ornamental varía ligeramente con la orientación del aro: se conserva la referencia del 42%, con el resplandor exterior fuera del diámetro sólido.

La animación, el brillo en movimiento y la lectura en el entorno real aún necesitan revisión dentro de Minecraft. Esta etapa integra el aspecto visual; no añade eventos ni consecuencias de los ocho resultados.

## Compilación

Se ejecutó `.\gradlew.bat build`: BUILD SUCCESSFUL, ambos módulos correctos (23 s). Systems recompiló el renderer y sus recursos; World quedó UP-TO-DATE. No hay suites JUnit en el proyecto (test NO-SOURCE); la validación específica de imágenes se realizó por separado con ValidarRuleta.cs. Gradle emitió avisos de funciones obsoletas para una futura migración a Gradle 9; no impidieron la compilación con el wrapper 8.12 actual.
