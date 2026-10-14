function rojo:cfg_reset
scoreboard players set #kind rj_k 4
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k 90
scoreboard players set #rise rj_k 0
scoreboard players set #gg rj_k 600
scoreboard players set #ggs rj_k 30
data modify storage rojo:estado nombre set value "Gas tóxico: Y 90"
tellraw @a ["",{"text":"ROJO - GAS TÓXICO: Y 90\n","color":"red","bold":true},{"text":"Escapa del gas: sube hasta Y 90 en 3 minutos.","color":"red"}]
function rojo:start
