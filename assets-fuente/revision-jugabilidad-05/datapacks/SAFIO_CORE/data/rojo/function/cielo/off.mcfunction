# Quita la vineta roja y devuelve la hora (solo si el cielo rojo estaba activo).
execute if score #sky_on rj_k matches 1 run function rojo:cielo/restaurar
worldborder warning distance 5
