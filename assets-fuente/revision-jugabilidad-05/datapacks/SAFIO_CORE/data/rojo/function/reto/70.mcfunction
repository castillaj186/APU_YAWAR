function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #st rj_k 60
scoreboard players set #sn rj_k 3
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 2
data modify storage rojo:estado nombre set value "Rayos y TNT"
tellraw @a ["",{"text":"ROJO - RAYOS Y TNT\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Peligros: TNT sobre ti cada 2 s, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
