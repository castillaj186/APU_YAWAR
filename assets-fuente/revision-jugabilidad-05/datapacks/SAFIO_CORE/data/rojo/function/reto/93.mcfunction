function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
data modify storage rojo:estado nombre set value "Ve al Nether en tormenta"
tellraw @a ["",{"text":"ROJO - VE AL NETHER EN TORMENTA\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
