# Se mantiene el orden del sistema oficial SAFIO.
execute if score #resultado safio.ruleta matches 1 run function safio:ruleta/rojo_reviil
execute if score #resultado safio.ruleta matches 2 run function safio:ruleta/rosa_nutria
execute if score #resultado safio.ruleta matches 3 run function safio:ruleta/morado_reinicio
execute if score #resultado safio.ruleta matches 4 run function safio:ruleta/azul_sancion
execute if score #resultado safio.ruleta matches 5 run function safio:ruleta/celeste_mision
execute if score #resultado safio.ruleta matches 6 run function safio:ruleta/verde_mejora
execute if score #resultado safio.ruleta matches 7 run function safio:ruleta/amarillo_libro
execute if score #resultado safio.ruleta matches 8 run function safio:ruleta/naranja_dificultad