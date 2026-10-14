function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #ce rj_k 40
scoreboard players set #cen rj_k 2
data modify storage rojo:estado nombre set value "Creepers eléctricos x2"
tellraw @a ["",{"text":"ROJO - CREEPERS ELÉCTRICOS X2\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers ELÉCTRICOS.","color":"red"}]
function rojo:start
