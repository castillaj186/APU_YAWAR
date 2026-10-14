function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #st rj_k 60
scoreboard players set #sn rj_k 3
scoreboard players set #ce rj_k 100
scoreboard players set #cen rj_k 1
data modify storage rojo:estado nombre set value "Tormenta infernal"
tellraw @a ["",{"text":"ROJO - TORMENTA INFERNAL\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers ELÉCTRICOS, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
