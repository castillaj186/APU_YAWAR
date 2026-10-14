function rojo:cfg_reset
scoreboard players set #kind rj_k 4
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 0
scoreboard players set #zb rj_k 80
scoreboard players set #mc rj_k 2
scoreboard players set #alt rj_k 150
scoreboard players set #rise rj_k 1
scoreboard players set #gg rj_k 0
scoreboard players set #ggs rj_k 0
data modify storage rojo:estado nombre set value "Gas ascendente: Y 145 y zombies"
tellraw @a ["",{"text":"ROJO - GAS ASCENDENTE: Y 150\n","color":"red","bold":true},{"text":"Escapa del gas: sube hasta Y 150 en 3 minutos. Peligros: zombies.","color":"red"}]
function rojo:start
