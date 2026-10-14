function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "Lluvia de TNT"
tellraw @a ["",{"text":"ROJO - LLUVIA DE TNT\n","color":"red","bold":true},{"text":"Sobrevive 3 minutos. Peligros: TNT sobre ti cada 2 s.","color":"red"}]
function rojo:start
