# Revisión de referencias APU YAWAR

Estado: análisis terminado; preparación y ensamblaje pendientes de decidir la reconstrucción de sectores. No son texturas listas para integrar.

Las 13 referencias están presentes, incluido glow.png (sin barra en su nombre). Todas miden 1254 × 1254 y tienen formato de 32 bits con alfa real. Se inspeccionaron visualmente las 13 imágenes y se recorrieron sus píxeles para comprobar transparencia. No se modificaron originales, recursos activos ni código.

| Capa | Alfa en (627,627) | Límites del contenido con alfa >=128 |
|---|---:|---|
| ring | 0 | (17,14)-(1234,1195) |
| medallion | 253 | (66,50)-(1187,1156) |
| pointer | 253 | (103,54)-(1003,1207) |
| glow | 1 | (50,57)-(1209,1164) |
| flash | 1 | (38,21)-(1215,1203) |
| sector_rojo | 253 | (59,217)-(1194,1052) |
| sector_rosa | 253 | (205,84)-(1157,1129) |
| sector_morado | 253 | (286,141)-(1114,1152) |
| sector_azul | 252 | (50,110)-(1203,1131) |
| sector_turquesa | 253 | (43,242)-(1210,1104) |
| sector_verde | 253 | (41,169)-(1210,1064) |
| sector_amarillo | 253 | (45,105)-(1202,1152) |
| sector_naranja | 252 | (8,132)-(1241,1092) |

## Geometría y orientación

Los ocho sectores no constituyen una plantilla común de 45 grados. Sus bordes radiales, arcos y radios interiores difieren. Rojo, verde y naranja apuntan visualmente hacia arriba; azul, turquesa y amarillo hacia abajo; rosa y morado tienen otras orientaciones. Los símbolos están incorporados a cada pieza, por lo que rotar el sector también gira su símbolo. No basta con redimensionar y rotar para obtener ocho uniones exactas. No se han medido aperturas exactas a partir de estos contornos ilustrados.

El aro tiene un hueco central transparente y conserva ornamentos. Su centro y radio deben calibrarse sobre el contorno circular, no únicamente sobre el rectángulo de píxeles visibles: el brillo y los adornos son asimétricos. El medallón requiere escala proporcional y colocación en el pivote común.

El puntero apunta a la derecha, orientación adecuada para situarlo a la izquierda de la rueda. Su punta está visible y completa; no se observa una porción de sector adherida. La gema y su resplandor contienen color intencional. Actualmente la pieza ocupa casi toda la altura de su propio lienzo; necesita reducirse proporcionalmente y trasladarse, conservando el lienzo común de la rueda.

Glow y flash contienen transparencia real, pero su centro tiene alfa 1, no cero. Su halo ocupa zonas amplias que podrían cubrir símbolos al superponerse. Son dorados, mientras que el renderer actual multiplica su RGB por el color ganador; el resultado no equivale a una máscara blanca neutra. Se debe preparar una máscara de efecto que conserve detalles y controle la cobertura interior, y comprobarla con el renderer antes de integrar.

## Decisión necesaria antes de modificar diseño

Recomendación: reconstruir los ocho sectores sobre una plantilla matemática común de 45 grados, conservando colores, símbolos e identidad ornamental como referencias. Aprobar primero un único sector y después derivar los restantes. El redibujo artístico requiere aprobación; una herramienta generativa por sí sola no garantiza encaje matemático.

Alternativa: aportar sectores exportados desde una única rueda maestra con capas, todos con el mismo pivote y ocho porciones de 45 grados.

No se generó una vista ensamblada presentada como definitiva ni texturas procesadas: hacerlo con estos contornos implicaría deformar, recortar o dejar solapes y huecos. Las vistas de conjunto, puntero y uniones se producirán después de resolver la geometría.