function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 3
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "BUNKER 2: TNT"
tellraw @a ["",{"text":"ROJO - BUNKER 2: TNT\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: TNT sobre ti cada 2 s.","color":"red"}]
function rojo:start
