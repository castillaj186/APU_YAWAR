# Inti e iluminación dinámica de la ruleta

Fecha: 12 de octubre de 2026.

## Corrección de Inti

### Causa identificada

El problema estaba en la llamada de dibujo de `IntiRenderer`: se utilizaba una sobrecarga corta de `DrawContext.drawTexture` que no declaraba en la misma llamada, de forma separada, el rectángulo de destino y la región completa de origen. Aunque los PNG individuales eran correctos, esa ambigüedad hacía que el tamaño de destino y el muestreo de la textura no quedaran explícitamente separados.

Se sustituyó por la sobrecarga de Minecraft 1.21.1 confirmada por la compilación:

```java
context.drawTexture(FRAMES[frame], x, y, side, side,
    0.0f, 0.0f, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
```

Su firma Yarn efectiva es:

```text
drawTexture(Identifier, int x, int y, int destinationWidth, int destinationHeight,
            float u, float v, int regionWidth, int regionHeight,
            int textureWidth, int textureHeight)
```

Así, el destino puede ser `side×side` y el origen se fija explícitamente a `0..512 × 0..512`. No se usa el atlas histórico en el renderer activo.

### Recursos y secuencia

- Frames usados: 199 PNG individuales.
- Resolución activa: 512×512 RGBA por frame.
- Duración: 270 ticks, 13,5 segundos.
- Tamaño: 55% de la altura escalada de la ventana, limitado a 62% del ancho. La rueda ocupa aproximadamente 50% de altura, de modo que Inti se presenta a ~1,10×.
- Posición: `x = screenWidth / 2 - side / 2`, `y = screenHeight / 2 - side / 2`.
- Orden: ruleta roja → transformación → salida → Inti → `MOMENTO DEL JUICIO` → reto. El servidor mantiene el reto preparado y espera el acuse `IntiFinishedPayload` antes de iniciar su temporizador.

El aura roja vuelve a dibujar el mismo PNG completo con alfa 0,14 y escala apenas superior; por eso no puede introducir un rectángulo, disco opaco o recorte independiente.

## Iluminación dinámica de la ruleta

Toda la luz adicional se genera por Java como triángulos de interfaz con mezcla aditiva. No se añadieron PNG, GIF, vídeo ni dependencias gráficas.

### Giro arcoíris

`RouletteLighting` usa `RoulettePalette.RGB`, la única paleta oficial. Durante la fase con velocidad angular positiva:

- dibuja ocho zonas de color que se desplazan con la rotación y el reloj interpolado;
- cada zona tiene tres arcos concéntricos con alfa decreciente para crear halo suave;
- agrega estrellas geométricas blancas/doradas de ocho rayos;
- escala la intensidad por `RouletteTimeline.energy`, derivada de la velocidad angular real;
- conserva el aro dorado original debajo de las capas de luz.

### Construcción y transformación

- `RouletteTimeline.buildPulse(tick, slot)` produce un arco y estrella locales del color propio de cada sector cuando aparece.
- `RouletteTimeline.fillPulse(tick, slot, winner)` produce un arco y estrella locales del color ganador cuando cada sector se transforma.
- La posición corresponde al mismo ángulo de `RouletteRenderer` y al orden circular de `RouletteFillSequence`; no es aleatoria.

### Resultado final y gema

Al completar los ocho sectores, `celebration` y `exitPulse` generan anillos expansivos transparentes, rayos y estrellas del color ganador. `RouletteGem` toma colores de los sectores que pasan bajo el puntero durante el giro y adopta el RGB ganador al detenerse. El cuerpo dorado y la posición del puntero no se modifican.

Los efectos no rellenan el centro del disco ni la pantalla: cada banda tiene caída de alfa hacia sus bordes y los anillos son contornos, no discos.

## Archivos modificados

- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/judgment/IntiRenderer.java`
- `apuyawar-systems/src/main/java/pe/apuyawar/systems/client/RouletteLighting.java`

No se crearon clases ni imágenes nuevas para esta actualización. Se conservaron los 199 PNG activos, el atlas histórico, la ruleta, los retos, las dificultades, el servidor, el mundo y los datapacks.

## Validación técnica

Se ejecutó:

```powershell
.\gradlew.bat clean build
```

Resultado: `BUILD SUCCESSFUL`.

También pasó `verifyJudgments`:

```text
PASS: 128 objetivos, 135 catálogos con mecánica,
límites de éxito/fracaso, temporizadores y acuses multijugador.
```

La compilación confirma que la sobrecarga nueva de `drawTexture` existe en los mappings y APIs reales de Minecraft 1.21.1 usados por este proyecto.

## Prueba manual pendiente

La comprobación final de composición en una ventana de Minecraft debe realizarse manualmente; una compilación no sustituye esa revisión visual. Cerrar clientes de desarrollo anteriores antes de iniciar una instancia limpia para garantizar que el código y los recursos actuales se carguen.

```powershell
.\gradlew.bat :apuyawar-systems:runClient
```

Comandos:

```mcfunction
/apuyawar inti probar
/apuyawar ruleta probar rojo
/apuyawar_ruleta forzar azul
/apuyawar_ruleta forzar verde
/apuyawar_ruleta forzar amarillo
```

Los comandos de ruleta exigen nivel de permiso 2. La prueba visual debe confirmar: máscara completa centrada, aura transparente, arcoíris durante el giro, flash de cada construcción, flash ganador por cada sector y pulso final.
