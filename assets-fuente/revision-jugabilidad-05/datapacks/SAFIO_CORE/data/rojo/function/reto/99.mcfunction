function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 5
scoreboard players set #ph rj_k 80
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
scoreboard players set #mc rj_k 2
scoreboard players set #blind rj_k 2
data modify storage rojo:estado nombre set value "Bunker 4: oscuridad, phantoms y TNT"
tellraw @a ["",{"text":"ROJO - BUNKER 4: OSCURIDAD, PHANTOMS Y TNT\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: TNT sobre ti cada 2 s, phantoms, oscuridad permanente.","color":"red"}]
function rojo:start
