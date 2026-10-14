function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #ce rj_k 60
scoreboard players set #cen rj_k 1
scoreboard players set #zb rj_k 80
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Ve al Nether con eléctricos"
tellraw @a ["",{"text":"ROJO - VE AL NETHER CON ELÉCTRICOS\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: creepers ELÉCTRICOS, zombies.","color":"red"}]
function rojo:start
