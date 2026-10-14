# METEORITO: bola de fuego que cae del cielo cerca del jugador.
scoreboard players set #n rj_k 6
function rojo:rand
execute if score #r rj_k matches 1 positioned ~6 ~ ~ run function rojo:h/mt_cae
execute if score #r rj_k matches 2 positioned ~-6 ~ ~ run function rojo:h/mt_cae
execute if score #r rj_k matches 3 positioned ~ ~ ~6 run function rojo:h/mt_cae
execute if score #r rj_k matches 4 positioned ~ ~ ~-6 run function rojo:h/mt_cae
execute if score #r rj_k matches 5 positioned ~4 ~ ~4 run function rojo:h/mt_cae
execute if score #r rj_k matches 6 run function rojo:h/mt_cae
