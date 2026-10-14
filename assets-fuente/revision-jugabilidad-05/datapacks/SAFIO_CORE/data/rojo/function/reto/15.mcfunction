function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 2
scoreboard players set #hd rj_k 400
scoreboard players set #hn rj_k 8
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "BUNKER 1: Horda"
tellraw @a ["",{"text":"ROJO - BUNKER 1: HORDA\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: zombies, hordas de zombies.","color":"red"}]
function rojo:start
