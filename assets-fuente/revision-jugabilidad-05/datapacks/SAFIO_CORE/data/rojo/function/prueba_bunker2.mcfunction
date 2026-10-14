# PRUEBA: BUNKER 2 con TNT, 10 minutos, sin cambio automático de destino.
function rojo:init
function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 0
scoreboard players set #dest rj_k 3
scoreboard players set #tnt rj_k 40
scoreboard players set #tn rj_k 1
data modify storage rojo:estado nombre set value "PRUEBA BUNKER 2: TNT"
tellraw @a ["",{"text":"ROJO - PRUEBA BUNKER 2: TNT\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Coordenadas: X 14198 Y 47 Z -625. Peligros: TNT sobre ti cada 2 s.","color":"red"}]
scoreboard players set #noshift rj_k 1
function rojo:start
