# APU YAWAR — corrección de energía visual 03

## Comparación realizada

El ZIP no estaba en assets-fuente. Se localizó el adjunto real en `C:\Users\casti\Music\COMPARA.zip`, se leyó sin modificarlo y se extrajeron exclusivamente los dos MP4 a `videos/` de esta revisión.

FFprobe: referencia antigua 18,2 s, 1340 × 796, 30 fps; APU44 32,766633 s, 1918 × 1036, 30 fps. Se examinaron ambos mediante fotogramas y hojas de contacto. Se extrajeron además 30 fps del giro y 10 fps del final antiguo.

La referencia comienza con la rueda ya construida y girando: no permite medir su construcción desde cero. Entre aproximadamente 2 y 5 s presenta su mayor energía; frena durante aproximadamente 6–11 s; confirma, transforma al azul y celebra entre aproximadamente 11–13,3 s; contrae y destella alrededor de 13,5–14,2 s. El resto del archivo muestra la interfaz del reproductor. Por eso no se usaron los 18,2 s como duración de la animación.

APU44 muestra la entrada desde cerca de 1,5 s del archivo, construcción prolongada, un giro más lento, llenado que ocupa cerca de ocho segundos y anillos dorados/rojos con apariencia opaca. El desplazamiento del escenario corresponde también a movimientos de cámara del jugador, por lo que no se confundió con vibración del HUD.

`rotacion-observada.csv` registra una estimación por correlación de color entre fotogramas. Incluye fotogramas repetidos, desenfoque, variación de escala y alias angular. Sus sumas aproximadas por segundo alcanzaron ~1.026 grados/s en la zona rápida; son estimaciones, no mediciones exactas del motor antiguo. El pico nuevo de 1.080 grados/s se eligió cercano a ese rango, con frenado verificable matemáticamente. La calibración no reproduce el video píxel a píxel.

## Timing nuevo

| Ticks | Segundos nominales | Comportamiento |
|---|---|---|
| 0–32 | 0–1,6 | Construcción, escala y alfa por sector, flash del color de cada casilla |
| 32–54 | 1,6–2,7 | Aceleración suave hasta 54 grados/tick |
| 54–150 | 2,7–7,5 | Giro rápido a 1.080 grados/s, estela y vibración |
| 150–204 | 7,5–10,2 | Frenado progresivo al ganador exacto |
| 204–212 | 10,2–10,6 | Asentamiento leve |
| 212–216 | 10,6–10,8 | Confirmación breve |
| 216–256 | 10,8–12,8 | Transformación en ocho pasos de 0,25 s con transición de 0,2 s y flash individual |
| 256–278 | 12,8–13,9 | Celebración y permanencia |
| 278–310 | 13,9–15,5 | Destello expansivo, contracción y desvanecimiento sincronizado |

La construcción añadida al principio y la salida legible llevan el total a 15,5 s. SCREEN_HEIGHT_FRACTION sigue en 0.42f. Aro, medallón, sectores, colores y asignación del ganador se conservan.

## Causa de los círculos sólidos y corrección

Las texturas glow/flash tenían alfa real; el renderer anterior dependía del estado de blending que dejaba el HUD. Se inspeccionó el bytecode de Minecraft 1.21.1: la variante usada de DrawContext.drawTexture llama al quad POSITION_TEXTURE y no activa blending. Otra variante coloreada sí lo activa y luego lo desactiva. Dibujar las máscaras con mezcla desactivada produce recortes opacos y bordes oscuros, coherentes con APU44.

RouletteDraw ahora activa explícitamente mezcla alfa estándar para cada capa artística. La luz usa composición aditiva SRC_ALPHA/ONE. Se restauran las funciones de mezcla, activación de blending, profundidad, shader y color anteriores al terminar el HUD. No se cubrieron los círculos con otra imagen.

Los amplios glow.png/flash.png anteriores ya no se dibujan en la ruleta. Sus archivos se conservan sin alterarlos. Se sustituyó su función por bandas radiales estrechas con alfa cero en ambos bordes, arcos de velocidad, anillos expansivos, rayos sutiles y chispas. No hay una primitiva de disco relleno alrededor de la rueda. El único destello central es pequeño (35,8 unidades de textura) y dura seis ticks; no tapa los ocho símbolos.

## Vibración y estela

La aceleración integra una curva cosenoidal. El frenado Hermite llega al ángulo exacto del ganador con velocidad final cero. La vibración combina dos senos de desplazamiento y una oscilación angular acotada a menos de 0,3 grados; se aplica solo a la matriz del disco. Se anula al detenerse y no altera el cálculo del ganador ni el muestreo matemático del color.

El puntero está fuera de esa matriz y queda fijo a la izquierda. Dos muestras temporales de baja opacidad, dibujadas detrás del arte nítido, aportan estela sin un shader ni un desenfoque costoso. Solo están activas en la zona rápida y desaparecen al frenar. La interpolación entre ticks sigue usando tickDelta.

## Gema

RouletteGem calcula qué sector queda bajo la punta a las 9:00 desde el ángulo real del disco. Suaviza el cambio en los límites y utiliza una ventana temporal de 0,12 s, independiente del FPS. Al parar conserva el RGB del ganador.

gem_light.png tiene soporte únicamente dentro de un círculo de radio 13 en (52,256), siguiendo la luminancia del cristal original. No llega al cuerpo dorado ni a la punta. Se combina un velo coloreado suave con luz aditiva, preservando el dibujo debajo. Durante la construcción mantiene la iluminación blanca tenue sobre la gema arcoíris. No se tinta el puntero completo.

## Llenado y celebración

Se reemplazan realmente los maestros en cada casilla; las posiciones se derivan con rotaciones exactas de 45 grados. La casilla que ya tenía el color ganador permanece igual, pero también recibe su pulso. light_sector.png restringe cada flash a la porción correcta. El aro recibe reflejos del ganador mediante ring_light.png, que sigue exclusivamente sus detalles luminosos. Las celebraciones incluyen chispas, rayos, anillos expansivos y un pulso en la gema.

Toda la composición comparte escala y alfa de salida. Las partículas y anillos finalizan antes de acabar la rueda, y al tick 310 no queda ningún componente activo. No hay una capa residual ni un puntero separado del ciclo de vida.

## Archivos del mod

Modificados:

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteRenderer.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/roulette/RouletteTimeline.java`

Nuevos:

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteDraw.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteLighting.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteGem.java`
- En `apuyawar-systems/src/main/resources/assets/apuyawar_systems/textures/gui/roulette/`: `light_soft.png`, `light_sector.png`, `gem_light.png`, `ring_light.png`.

Las 13 texturas artísticas activas y todos los originales de referencias permanecen idénticos por SHA-256. No se cambió servidor, mundo, datapack, World, paleta, estado ni selección del ganador. La red sigue enviando una sola elección; el reloj cliente lee automáticamente la nueva DURATION.

## Herramientas y pruebas

EffectsCheck.java prueba las funciones Java reales para los ocho ganadores: posiciones, construcción, llenado, flashes, frenado, continuidad de velocidad/ángulo, vibración acotada y anulada, gema continua en los límites y correcta al detenerse, y alfa final cero. Detectó una pequeña desviación numérica del easing en el extremo; se corrigió limitando su resultado a [0,1].

ValidarLuz.cs comprueba las cuatro máscaras RGBA, su neutralidad, ausencia de alfa totalmente opaco, soporte geométrico, aislamiento de la gema y transparencia del centro del aro. Las máscaras son auxiliares matemáticas, no fotogramas de animación.

SimularEfectos.cs genera una simulación CPU a 30 fps con las texturas reales y datos exportados desde Java. La geometría de luz se aproxima en CPU y no usa el rasterizador OpenGL de Minecraft. Comprueba que el último fotograma está vacío de efectos y rueda. Las hojas de comparación, fotogramas y videos están solo en esta carpeta técnica, nunca en los recursos del mod.

Gradle build pasó para ambos mods. NO-SOURCE en las tareas test significa que las pruebas específicas se ejecutaron por separado. No se instaló ni distribuyó el JAR de verificación.

## Prueba en Minecraft y límites

No se ejecutó Minecraft ni se afirma que una prueba visual dentro del juego haya pasado. Queda revisar el brillo sobre nieve/cielo claro, la intensidad percibida de la gema y la estela en distintas tasas de FPS. La simulación no demuestra igualdad exacta con la animación antigua ni sustituye esa revisión.

Desde E:\APU_YAWAR:

```powershell
.\gradlew.bat :apuyawar-systems:runClient
```

En un mundo local con comandos habilitados, uno por vez, esperando 16 segundos:

```text
/apuyawar_ruleta forzar rojo
/apuyawar_ruleta forzar rosa
/apuyawar_ruleta forzar verde
/apuyawar_ruleta forzar azul
```

También funcionan morado, turquesa, amarillo y naranja. Una nueva tirada reinicia la anterior, como antes. Comprobar puntero fijo, color de gema durante giro, flashes por casilla, llenado completo, celebración sin fondo pintado y salida sin residuo.
