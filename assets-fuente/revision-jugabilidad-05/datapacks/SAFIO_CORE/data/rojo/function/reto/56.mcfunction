function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #ph rj_k 60
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Phantoms sobre ti"
tellraw @a ["",{"text":"ROJO - PHANTOMS SOBRE TI\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Peligros: phantoms.","color":"red"}]
function rojo:start
