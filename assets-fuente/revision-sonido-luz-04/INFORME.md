# APU YAWAR — sonido y luz 04

Implementación compilada con Java 21 y Fabric 1.21.1. Ambos módulos pasan `gradlew.bat build`. No se ejecutó Minecraft: la aprobación perceptual de audio, iluminación y rendimiento queda pendiente.

## Archivos de producción modificados

Todos bajo `apuyawar-systems/src/main/java/pe/apuyawar/systems/`:

- `client/ApuYawarClient.java`: callback del reloj cliente.
- `client/RouletteClientState.java`: eventos, pausa, limpieza al reiniciar/desconectar/finalizar.
- `client/RouletteRenderer.java`: escala 0.45, límite de encuadre incluyendo efectos, secuencia del ganador, pulsos multicolor y reflejos.
- `client/RouletteLighting.java`: ondas locales, partículas, reflejos del aro y culminación reforzada.
- `roulette/RouletteTimeline.java`: mezcla y pulso usan el rango circular relativo al ganador.

## Clases nuevas

- `roulette/RouletteFillSequence.java`: rango y posición circular compartidos.
- `roulette/RouletteEvents.java`: cursor sin eventos duplicados y cruces angulares reales.
- `client/RouletteParticles.java`: máximo 80 partículas, arrastre analítico, tamaño y alfa decrecientes.
- `client/RouletteSoundController.java`: reproducción cliente, variación tonal y máximo 12 sonidos propios simultáneos; limpieza sin detener otros sonidos del juego.

## Recursos nuevos

`apuyawar-systems/src/main/resources/assets/apuyawar_systems/sounds.json` y, bajo `sounds/roulette/`:

- `roulette_appear.ogg` — 0.30 s.
- `roulette_tick.ogg` — 0.09 s.
- `roulette_result.ogg` — 0.65 s.
- `roulette_fill.ogg` — 0.20 s.
- `roulette_finish.ogg` — 1.05 s.
- `roulette_outro.ogg` — 0.90 s.

Vorbis mono, 44100 Hz. Generados con síntesis modal, percusión sintética y chirps; no leen grabaciones ni melodías externas. Los seis WAV originales se conservan aquí para escucharlos y regenerarlos. No se modificaron las texturas artísticas ni se agregaron PNG de animación.

## Eventos y geometría

Un único cursor se actualiza en ticks cliente, nunca en render. Entradas: ticks 2,5,8,11,14,17,20,23. Resultado: 204. Llenado: 216,221,226,231,236,241,246,251. Culminación: 256. Salida: 278. Cancelar o reiniciar detiene las instancias propias y limpia las partículas.

El índice aumenta 45 grados en coordenadas GUI con Y hacia abajo: es sentido horario. Para ganador W, la secuencia es W, W+1, ..., W+7 módulo 8. La rotación final común conserva esta vecindad. W ya tiene la textura ganadora: recibe el primer pulso y sonido; las otras siete posiciones reemplazan realmente su textura por el maestro ganador. La selección del servidor permanece intacta.

Los clics ocurren solo en cruces de ángulos `270+n*45`, resueltos por bisección de la rotación sin vibración. Un intervalo mínimo de 2 ticks limita la densidad a 10/s: se omiten cruces a gran velocidad, no se inventan clics. La frecuencia baja naturalmente durante el frenado.

## Luz y partículas

Máscaras suaves existentes y geometría aditiva de bandas/arcos con bordes transparentes. Cada conversión comparte inicio con su sonido, onda localizada y cinco partículas. La entrada emite dos; el giro emite pocas; la culminación emite 28. Arrastre exponencial y alfa cuadrático; máximo 80 partículas, caducidad de 14–22 ticks. Todo usa el centro y escala de la composición y desaparece junto con ella.

La rueda conserva interpolación, vibración acotada, puntero fijo y gema reactiva. Pulsos locales siguen la rotación, y reflejos del aro siguen el color calculado bajo el puntero. No se renderizan los antiguos discos glow/flash ni superficies circulares rellenas. Las luces nuevas no alteran los símbolos ni el resultado matemático.

## Duración conservada

310 ticks / 15.5 segundos nominales. Entrada hasta tick 32 (1.6 s), aceleración hasta 54 (2.7 s), giro rápido hasta 150 (7.5 s), frenado hasta 204 (10.2 s), asentamiento hasta 212 (10.6 s), preparación hasta 216 (10.8 s), llenado hasta 256 (12.8 s), celebración hasta 278 (13.9 s), salida hasta 310 (15.5 s).

## Referencias y límites de validación

Los ZIP no estaban en assets-fuente: se encontraron en `C:\Users\casti\Music`; copias extraídas en `referencias/`. PRUEBA001 tiene 39.43 s de contenedor y la referencia sonora 16.40 s. Se analizaron estadísticas RMS y ataques de audio, no una audición perceptual. `referencias-audio.json` guarda estimaciones automáticas; el video actual tiene audio de muy baja amplitud. La referencia sonora presenta ataques finales alrededor de 12.64–14.14 s, compatible con eventos de llenado/culminación de la secuencia conservada. No se importó ningún audio de referencia al mod.

`validar.ps1` comprueba los ocho ganadores, vecinos horarios después de rotación final, puntero, eventos únicos, cruces exactos, límite de clics, caducidad/reinicio de partículas, IDs de sounds.json y decodificación de los seis OGG. Pruebas aprobadas. Gradle aprobado. No se midieron FPS ni se comprobó la reproducción real del motor de sonido dentro de Minecraft.

## Prueba

Desde E:\APU_YAWAR ejecutar `./gradlew.bat :apuyawar-systems:runClient`. Abrir un mundo de prueba con trucos habilitados y usar `/apuyawar_ruleta forzar COLOR`, donde COLOR es rojo, rosa, morado, azul, turquesa, verde, amarillo o naranja. Repetir un comando durante la animación para comprobar que se limpia el estado anterior. Probar pausa, salida del mundo y una ventana pequeña. Revisar volumen, intensidad, encuadre y fluidez en el equipo real.
