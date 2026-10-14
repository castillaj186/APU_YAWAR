# Corrección de corona arcoíris e Inti

## Hallazgos

La corona previa se dibujaba a radios 218–233 en un lienzo de radio 256. Esos valores la dejaban sobre o dentro del aro y ornamentos, por lo que su brillo aditivo quedaba visualmente absorbido por el arte dorado. No era una falta de colores de `RoulettePalette`, sino un problema de geometría, radio y amplitud.

Las franjas negras de Inti están presentes en los PNG activos y se observan también en los originales como parte del glitch. Se limpiaron únicamente tramos horizontales opacos casi negros de longitud mínima 18 píxeles en las copias activas de 512×512, interpolando el color desde las filas vecinas. Los 199 originales de 1254×1254 no se modificaron. El script y reporte están en `assets-fuente/inti-juicio/revision-int-02/`.

## Cambios

- `RouletteLighting.java`: corona exterior dinámica con centro radial 274, borde exterior 304 y borde artístico 256. Ocho arcos oficiales se desplazan con `rotation` y `energy`; tres bandas concéntricas con caída alfa producen un halo de 8–18% fuera del aro.
- Estrellas de ocho rayos y estelas limitadas se generan con triángulos y mezcla aditiva; no se añadieron recursos gráficos.
- Aparición de sectores: `buildPulse` crea arco y estrella locales con el RGB del sector.
- Relleno: `fillPulse` crea arco y estrella locales con el RGB ganador, según `RouletteFillSequence`.
- Resultado: anillos expansivos, rayos y estrellas usan el RGB ganador sin rellenar el centro.
- `IntiRenderer.java` conserva el rostro completo y centrado. No se cambió escala ni duración.

## Gema

`RouletteGem` ya calcula el color con los sectores que cruzan el puntero y adopta `RoulettePalette.RGB[winner]` al detenerse. `RouletteLighting.gem` aplica esa luz solo en la máscara de gema; el puntero dorado no se tinta.

## Archivos modificados

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteLighting.java`
- Copias activas `apuyawar-systems/src/main/resources/assets/apuyawar_systems/textures/gui/inti/frames/frame_*.png`
- `assets-fuente/inti-juicio/revision-int-02/limpiar_franjas_negras.cs`

## Validación

`./gradlew.bat clean build` finalizó con `BUILD SUCCESSFUL` y `verifyJudgments` confirmó 128 retos y 135 dificultades.

La comprobación visual interactiva sigue pendiente: debe hacerse en una instancia nueva de cliente con `/apuyawar inti probar`, `/apuyawar ruleta probar rojo` y los comandos forzados azul, verde y amarillo. La compilación no sustituye esta revisión.
