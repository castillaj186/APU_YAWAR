function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
data modify storage rojo:estado nombre set value "Tormenta de triple rayo"
tellraw @a ["",{"text":"ROJO - TORMENTA DE TRIPLE RAYO\n","color":"red","bold":true},{"text":"Sobrevive 3 minutos. Peligros: tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
