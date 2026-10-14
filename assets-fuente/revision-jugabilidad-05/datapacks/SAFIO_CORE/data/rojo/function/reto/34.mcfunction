function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 5
scoreboard players set #cr rj_k 40
scoreboard players set #crn rj_k 1
data modify storage rojo:estado nombre set value "BUNKER 4: Creepers"
tellraw @a ["",{"text":"ROJO - BUNKER 4: CREEPERS\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: creepers encendidos sobre ti cada 2 s.","color":"red"}]
function rojo:start
