function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #zb rj_k 80
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Tormenta y zombies"
tellraw @a ["",{"text":"ROJO - TORMENTA Y ZOMBIES\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: zombies, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
