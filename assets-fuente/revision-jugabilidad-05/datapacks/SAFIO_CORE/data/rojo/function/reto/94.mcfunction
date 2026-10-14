function rojo:cfg_reset
scoreboard players set #kind rj_k 5
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #ph rj_k 100
scoreboard players set #sp rj_k 80
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Ve al Nether con phantoms y arañas"
tellraw @a ["",{"text":"ROJO - VE AL NETHER CON PHANTOMS Y ARAÑAS\n","color":"red","bold":true},{"text":"Viaja al Nether en 5 minutos. Peligros: arañas, phantoms.","color":"red"}]
function rojo:start
