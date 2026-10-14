function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #zb rj_k 120
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "Ve al Nether"
tellraw @a ["",{"text":"ROJO - VE AL NETHER\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: zombies.","color":"red"}]
function rojo:start
