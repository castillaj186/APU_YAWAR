function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 0
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "REGRESA AL PUNTO: TNT"
tellraw @a ["",{"text":"ROJO - REGRESA AL PUNTO: TNT\n","color":"red","bold":true},{"text":"Regresa rápido al punto indicado y quédate dentro. La brújula te ayudará a encontrarlo, pero ten cuidado: caerá TNT sobre ti cada 2 segundos. Tienes 10 minutos.","color":"red"}]
function rojo:start
