function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 2
scoreboard players set #tnt rj_k 60
scoreboard players set #tn rj_k 1
scoreboard players set #ce rj_k 60
scoreboard players set #cen rj_k 1
scoreboard players set #zb rj_k 60
scoreboard players set #sk rj_k 80
scoreboard players set #ph rj_k 160
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "BUNKER 1: Infierno"
tellraw @a ["",{"text":"ROJO - BUNKER 1: INFIERNO\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: TNT sobre ti cada 2 s, creepers ELÉCTRICOS, zombies, esqueletos, phantoms, tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
