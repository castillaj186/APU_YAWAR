function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #ph rj_k 80
scoreboard players set #sp rj_k 60
scoreboard players set #mc rj_k 2
scoreboard players set #blind rj_k 2
data modify storage rojo:estado nombre set value "Oscuridad, phantoms y arañas"
tellraw @a ["",{"text":"ROJO - OSCURIDAD, PHANTOMS Y ARAÑAS\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: arañas, phantoms, oscuridad permanente.","color":"red"}]
function rojo:start
