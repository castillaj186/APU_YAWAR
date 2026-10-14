function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #mt rj_k 30
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "Meteoritos y TNT"
tellraw @a ["",{"text":"ROJO - METEORITOS Y TNT\n","color":"red","bold":true},{"text":"Sobrevive durante 4 minutos mientras caen meteoritos y aparece TNT sobre ti cada 2 segundos. No te detengas demasiado tiempo en un mismo sitio y calcula bien tus movimientos para evitar ambos impactos.","color":"red"}]
function rojo:start
