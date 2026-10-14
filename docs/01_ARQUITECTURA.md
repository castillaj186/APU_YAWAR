# Identidad y arquitectura

Nombre: **APU YAWAR: El Juicio de los Dioses**.

- `apuyawar_systems`: mod Fabric de reglas visuales, red, HUD y ruleta (cliente y servidor).
- `apuyawar_world`: contenido original, futuro Cuy Jardinero, Dimensión Cuy, minerales, herramientas, bloques, jefes.
- `apuyawar`: namespace de funciones del datapack `apuyawar-core`.
- Código Java: `pe.apuyawar.systems`, `pe.apuyawar.world`.

## Flujo de ruleta

Un administrador o el datapack ejecuta `apuyawar_ruleta tirar`. El servidor selecciona un índice de 0 a 7 y envía **una vez** `RouletteStartPayload` a los clientes con `apuyawar_systems`. Cada cliente calcula toda la coreografía localmente mediante `RouletteTimeline` y dibuja el arte por capas desde `RouletteRenderer`.

El servidor sigue siendo la autoridad del ganador. En esta base **NO se ejecuta todavía ningún evento o castigo real** al terminar: no puede confundirse una demo visual con el sistema final de desafíos.

## Capas independientes

Todas las texturas están centradas en un canvas RGBA 512×512:
- `sector_<color>.png` (8): sectores y sus iconos. Giran juntos.
- `ring.png`: aro exterior. Gira junto con sectores.
- `medallion.png`: pieza central. Gira.
- `pointer.png`: **solo** oro + gema, completamente independiente; se dibuja encima y no gira.
- `glow.png`, `flash.png`: halos y destellos; tints por código.

No cortar el puntero de una rueda ya completa: ese fue el origen de amarillo fijo y puntas amputadas. Crear el puntero desde una ilustración/capa independiente.

## Estado

V0.1.0 = andamiaje fuente con una demostración visual de ruleta, no el servidor completo de 30 días. Se mantiene intención de ocho resultados y cuatro ediciones narrativas, pero no se afirma tener implementados dimensiones, jefes, Gulag ni eventos.
