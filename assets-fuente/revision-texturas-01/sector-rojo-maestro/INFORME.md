# Sector rojo maestro — propuesta para revisión

## Contrato geométrico

- Textura final: PNG RGBA 512 × 512, pivote (256,256).
- Ángulos de imagen: -90° a -45°, con eje X a la derecha y eje Y hacia abajo. Centro angular: -67,5°. Es la orientación inicial del sector rojo que ya utiliza Java.
- Apertura matemática: 45°. Radio interior: 22 px. Radio exterior: 169 px. Estos radios conservan la referencia provisional existente; su ajuste con el aro artístico se decidirá al preparar las demás capas.
- La herramienta artística reconstruye el diseño sobre una guía vertical de -112,5° a -67,5°. El procesado solo escala uniformemente el lienzo y gira 22,5° alrededor del pivote; no comprime horizontalmente la ilustración.
- Una máscara analítica recorta los bordes a los límites anteriores. Se muestrean 16 subpíxeles por píxel y se interpola el color con alfa premultiplicado para reducir contornos oscuros. Los píxeles de borde pueden tener alfa parcial: la exactitud corresponde a la geometría continua, su representación PNG está rasterizada.

## Archivos

- sector_rojo.png: textura propuesta, aún no aprobada ni integrada.
- plantilla_45.png: máscara matemática reutilizable.
- vista_sector_solo.png: vista sobre damero; el damero no forma parte de la textura.
- vista_rueda_prueba.png: rojo y siete celdas grises de referencia geométrica; no representa los otros siete diseños.
- preparar-maestro.ps1: procesado reproducible; no sobrescribe las salidas existentes.
- guia-generacion.png: guía usada para la reconstrucción artística.
- arte-reconstruido.png: primer intento descartado por no respetar la geometría.
- arte-reconstruido-v2.png: segundo intento artístico, separado de la textura procesada.

## Replicación futura

Tras aprobar el rojo, se conservarán la máscara, radios, pivote y jerarquía de bordes dorados. Cada color se reconstruirá con su símbolo original sobre la misma guía; no se deformará la referencia. Cada sector i ocupará [-90°+45°i,-45°+45°i] alrededor del mismo centro. Se comprobarán símbolo completo, cobertura interior y uniones a tamaño real antes de integrar. La máscara fija los límites pero no garantiza que el arte generado llene todo su interior; esa cobertura se valida por separado.

No se modifican las otras capas, los originales, las texturas activas, el código Java, el servidor, el mundo ni el datapack. La timeline permanece en 375 ticks.

## Herramienta y solicitud artística

Se utilizó image_gen integrado, con transparencia real, y System.Drawing para la plantilla geométrica, el muestreo y las vistas técnicas. El segundo prompt pidió editar únicamente la silueta de la guía, conservar su posición y márgenes transparentes, aplicar rojo rubí y marco dorado andino, y mantener el símbolo doble de montaña blanco-dorado. No se solicitaron nuevos símbolos ni cambios de identidad.
## Validación realizada

PNG final 512 × 512, Format32bppArgb; alfa central 0. Ningún píxel visible queda fuera de los límites con tolerancia de rasterizado de 1 px. En el interior de control (radios 25–166 y ángulos -88° a -47°), el 99,67% de los píxeles tiene alfa >=200. El arte conserva pequeñas zonas de transparencia y sombreado; no se afirma opacidad perfecta ni uniones artísticas definitivas con siete sectores aún no creados. La vista gris permite revisar el contorno; las uniones reales se comprobarán al preparar vecinos.

Las huellas SHA-256 de los 13 originales, las 13 texturas activas y RouletteTimeline.java (27 archivos) coinciden con las previas. No se ejecutó Gradle: no hubo cambios de Java ni integración de recursos.

La vista_detalle.png amplía la textura rasterizada; su suavidad ampliada no representa una pérdida adicional en sector_rojo.png. La ilustración redibujada se simplificó para el sector estrecho; los ornamentos no son una reproducción idéntica del original y quedan sujetos a aprobación visual.
