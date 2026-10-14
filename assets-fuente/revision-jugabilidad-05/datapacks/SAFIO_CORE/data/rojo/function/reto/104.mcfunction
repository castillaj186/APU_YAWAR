function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #gh rj_k 200
data modify storage rojo:estado nombre set value "Fantasmas del infierno"
tellraw @a ["",{"text":"ROJO - FANTASMAS DEL INFIERNO\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Ghasts del Nether aparecen sobre ti y disparan bolas de fuego.","color":"red"}]
function rojo:start
