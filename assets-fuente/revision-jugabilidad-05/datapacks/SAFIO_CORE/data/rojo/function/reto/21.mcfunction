function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 3
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "BUNKER 2: Tormenta"
tellraw @a ["",{"text":"ROJO - BUNKER 2: TORMENTA\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: zombies, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
