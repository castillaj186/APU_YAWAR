# Preparación de Inti — revisión 01

- Origen preservado: `frames-originales/frame_001.png` a `frame_199.png`.
- Validación: 199 archivos, numeración continua, sin hashes SHA-256 repetidos.
- Formato original: 1254 × 1254 px, PNG ARGB de 32 bits; 488 550 154 bytes en total.
- Transparencia: los 199 fotogramas tienen esquinas con alfa transparente. Los tres primeros son la materialización parcial, por eso no todos contienen píxeles opacos en el centro.
- Evolución visual: fragmentos iniciales → rostro y ornamentos estables → interferencias horizontales tardías → apagado de los dos últimos fotogramas. El rostro se mantiene centrado en el mismo lienzo; no se aplicó recorte por fotograma para no introducir saltos.
- Recurso de ejecución: `inti_atlas.png`, atlas RGBA 4096 × 3328 px, cuadrícula 16 × 13, celdas 256 × 256. Incluye los 199 tiempos originales; los nueve espacios no usados permanecen transparentes.
- Reproducción: 100 ticks / 5 s, 199 posiciones interpoladas por reloj local, aproximadamente 39,8 muestras por segundo. No se carga un PNG por fotograma.
- Vista de control: `vista_atlas_inti.png`.

Los originales no fueron modificados.
