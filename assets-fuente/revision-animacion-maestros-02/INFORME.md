# APU YAWAR — animación con maestros actuales

Implementado e integrado en Systems. Build de ambos mods: BUILD SUCCESSFUL (10 s). Los originales aprobados permanecen idénticos por SHA-256. No se modificaron servidor, mundo, datapack ni World.

## Fases reales

| Ticks | Tiempo nominal | Fase |
|---|---|---|
| 0–84 | 0–4,2 s | Entrada por escala y opacidad; ocho sectores aparecen a intervalos de 9 ticks |
| 84–132 | 4,2–6,6 s | Aceleración cosenoidal suave |
| 132–252 | 6,6–12,6 s | Giro energético a 20 grados/tick, la mitad de la velocidad anterior |
| 252–332 | 12,6–16,6 s | Frenado progresivo hasta el ganador exacto |
| 332–350 | 16,6–17,5 s | Rebote pequeño con velocidad continua |
| 350–360 | 17,5–18 s | Pausa de confirmación |
| 360–520 | 18–26 s | Llenado en ocho pasos de un segundo, transición de 12 ticks por posición |
| 520–560 | 26–28 s | Permanencia del disco completo en el color ganador |
| 560–620 | 28–31 s | Salida con reducción de tamaño, transparencia y expansión suave del halo |

La duración nueva es 620 ticks, autorizada en esta etapa. RouletteClientState ya utiliza RouletteTimeline.DURATION, por lo que no necesitó cambios. El paquete de red y los comandos tampoco cambian.

## Ocho posiciones para cada color

Se mantienen solo 13 PNG activos: ocho maestros de color más cinco capas. Los ocho sectores están normalizados a RGBA 512 × 512, pivote (256,256), radios 22 y 169, y posición base [-90°,-45°]. Las otras siete posiciones se derivan en RouletteRenderer mediante `diskRotation + slot * 45°`. Hay ocho posiciones únicas y 64 combinaciones color/posición, sin guardar 64 PNG.

El orden angular normal se conserva: rojo, rosa, morado, azul, turquesa, verde, amarillo, naranja. Es independiente del orden en que se enumeran los archivos en referencias. La punta está a las 9:00; se calcula la parada usando el centro angular de la casilla ganadora.

Los símbolos provienen de los maestros actuales: calavera roja, cuy rosa, reinicio morado, sanción azul, documento turquesa, hojas verdes, confirmación amarilla y fuego naranja. No se reutilizan los símbolos antiguos. Las proporciones de los símbolos se conservan mediante escala uniforme 0,1292 y rotación de 22,5°. El pivote virtual de muestreo de las ilustraciones es (627,1440). Se aplica la misma máscara y el marco dorado común de la plantilla roja previamente preparada; se recortan y reconstruyen los márgenes para obtener 45°, sin comprimir las figuras. Se completa únicamente el fondo donde el lienzo original no cubre el radio interior.

## Llenado

Cada posición tiene su propio `fillBlend(tick, slot)`. Mientras la transición es parcial, el renderer conserva la textura normal debajo y superpone el maestro ganador con alfa creciente. Al llegar a 1 solo dibuja el ganador en esa casilla. Al final las ocho casillas utilizan el mismo maestro ganador, cada una con su rotación correcta. No es un tintado de las texturas normales: también se sustituye el símbolo y el arte interior.

La casilla que ya tenía el color ganador conserva ese color. Las otras siete cambian de forma visible. Los ocho pasos siguen la misma secuencia horaria, con el disco detenido durante todo el llenado.

## Capas y efectos

`ring_delgado.png` se procesa como `ring.png`, centrado desde (627,606), a escala uniforme 0,375. `medallion_delgado.png` se procesa como `medallion.png`, a escala 0,057 (antes 0,077), para dejar más superficie de color visible. Se conserva el diámetro de referencia 458 px y SCREEN_HEIGHT_FRACTION = 0.42f.

El puntero conserva su punta en (101,256), el cuerpo y la gema. Nunca hereda la matriz de rotación del disco. Glow/flash se preparan como máscaras blancas con alfa derivado del arte original; su zona interior de radio 169 es transparente. La pulsación, expansión y opacidad se controlan en la timeline. Solo estas máscaras se tintan; sectores, aro, medallón y puntero se dibujan en blanco multiplicativo para conservar sus colores originales.

Pointer, glow y flash procesados son idénticos por SHA-256 a los ya integrados. Se comprobaron nuevamente contra sus referencias actuales; no cambiaron sus bytes activos.

## Archivos existentes modificados

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteRenderer.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/roulette/RouletteTimeline.java`
- En `apuyawar-systems/src/main/resources/assets/apuyawar_systems/textures/gui/roulette/`: `ring.png`, `medallion.png`, `sector_rojo.png`, `sector_rosa.png`, `sector_morado.png`, `sector_azul.png`, `sector_turquesa.png`, `sector_verde.png`, `sector_amarillo.png`, `sector_naranja.png`.

Los nuevos archivos de preparación, validación, vistas y respaldos se enumeran exactamente en ARCHIVOS.md. Los artefactos normales de Gradle no son ediciones de código fuente ni distribuciones instaladas.

## Validación

- TimelineCheck.java compiló y probó la clase real de producción con Java 21: ocho posiciones únicas, ocho ganadores alineados, construcción y llenado monotónicos, frenado progresivo, continuidad de ángulo/velocidad, disco detenido durante llenado y resultado, permanencia y salida.
- ValidarMaestros.cs comprobó los 13 PNG RGBA 512 × 512, máscara alfa idéntica entre colores, geometría y cobertura del sector base, las 64 combinaciones por rotación, transparencia del aro y efectos, y punta del puntero.
- Las 13 texturas procesadas coinciden por SHA-256 con los activos y con los recursos dentro del JAR de verificación.
- Build: ambos mods correctos; World UP-TO-DATE. Las tareas Gradle test siguen en NO-SOURCE; las pruebas específicas anteriores son independientes del build. Gradle mantiene sus avisos de obsolescencia para una futura migración, sin errores de compilación.

## Vistas y límites

`vista_secuencia.png` muestra 12 instantes de la tirada roja. `vista_resultados.png` compara los resultados rojo, verde, azul y amarillo. Las vistas usan los maestros procesados y valores exportados de la timeline Java real en frames.csv. Son simulaciones técnicas; no capturas de Minecraft ni animaciones grabadas. No representan la vibración subpíxel ni el filtrado exacto del motor.

Los símbolos giran con cada casilla. Los detalles pequeños pueden perder legibilidad con escalas GUI grandes, y el antialias de los límites de 45° requiere revisión durante movimiento en Minecraft. El aspecto final representa el color ganador en los ocho fondos; los adornos dorados y el puntero arcoíris conservan sus colores.

## Prueba exacta

Desde E:\APU_YAWAR:

```powershell
.\gradlew.bat :apuyawar-systems:runClient
```

En un mundo local de prueba con comandos habilitados:

```text
/apuyawar_ruleta forzar rojo
/apuyawar_ruleta forzar verde
/apuyawar_ruleta forzar azul
/apuyawar_ruleta forzar amarillo
```

Esperar al menos 31 segundos entre comandos. Una nueva tirada todavía reinicia la anterior. Para selección aleatoria: `/apuyawar_ruleta tirar`. Comprobar construcción, puntero fijo, frenado, ocho casillas al color ganador, permanencia y salida. Esta etapa no añade consecuencias de juego a los resultados.
