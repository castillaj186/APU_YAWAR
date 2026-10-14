# Requisitos del arte definitivo

Se incluyen texturas **temporales para probar el código**, no versiones iguales a la referencia artística aprobada.

Al reemplazarlas:
- Exactamente 512 × 512, formato PNG RGBA con fondo transparente.
- Las 8 porciones y su icono estarán alineados radialmente en el mismo sistema de coordenadas y círculo de referencia.
- `pointer.png` contiene exclusivamente el ornamento oro, la gema y la flecha pequeña original completa. Debe dejar transparente TODO fondo de sectores, pero no agujerear la punta dorada.
- El disco debe girar bajo el puntero sin que ningún segmento fijo permanezca visible.
- `ring.png` y `medallion.png` deben tener el mismo centro geométrico.
- `glow.png` y `flash.png` son máscaras blanquecinas tintadas por Java en tiempo real.
- Asegurar visualización en 128–288 px dentro del juego.

El generador no escribe en los recursos de ningún mod. Sin argumentos muestra ayuda y no genera archivos. Para solicitar explícitamente las 13 capas provisionales (requiere Python y Pillow):

```powershell
python assets-fuente/generar_texturas_provisionales.py --generar
```

La salida se guarda únicamente en `assets-fuente/provisionales/`. Si existe cualquiera de los 13 nombres de salida, se cancela antes de generar; cada escritura también usa creación exclusiva para impedir sobrescrituras. Una interrupción puede dejar una salida parcial que deberá revisarse manualmente. No hay opción de sobrescribir ni instalación automática. El script ya no genera el objeto de World.

El destello provisional tiene un centro transparente hasta el radio 169 y un halo exterior; nunca debe sustituir automáticamente el `flash.png` corregido del mod. Los PNG actuales no necesitan regenerarse para compilar.

## Contrato de las 13 capas

El lienzo común es RGBA 512 × 512, con pivote en (256, 256). Los sectores parten de las 12:00 en orden rojo, rosa, morado, azul, turquesa, verde, amarillo y naranja; cada uno ocupa 45 grados. El puntero está a las 9:00 y apunta hacia el centro. Su lienzo conserva ese pivote aunque el ornamento esté desplazado hacia la izquierda.

Cada capa dispone de rotación, escala y opacidad independientes en `RouletteRenderer.drawLayer`. Actualmente sectores, aro y medallón reciben el mismo ángulo; halo, destello y puntero reciben cero. Se conserva el orden de dibujo y la transformación global de entrada, vibración y salida. La timeline de 375 ticks no cambia.

Las fuentes artísticas deben conservar capas separadas. No recortar el puntero de una rueda compuesta: sus píxeles visibles deben corresponder solo al oro, la gema y la punta completa. Revisar las propuestas a tamaños de HUD antes de aprobar su incorporación manual a `src/main/resources`.

No incluimos 3,000 frames ni enlaces de descargas de imágenes ajenas. El creador decidirá y aprobará las capas finales.
