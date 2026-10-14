# Comprobación de llegada. NO cambia de destino cuando todos llegan:
# eso lo gestiona k1 y llama a exito.
function rojo:dest_tags

# Reponemos la brújula cada 5 segundos solo si falta/está siendo actualizada.
execute if score #compass_cd rj_k matches ..0 run function rojo:dest_compass
execute if score #compass_cd rj_k matches ..0 run scoreboard players set #compass_cd rj_k 100
