function rojo:cfg_reset
scoreboard players set #kind rj_k 4
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 0
scoreboard players set #cr rj_k 80
scoreboard players set #crn rj_k 1
scoreboard players set #alt rj_k 120
scoreboard players set #rise rj_k 0
scoreboard players set #gg rj_k 600
scoreboard players set #ggs rj_k 30
data modify storage rojo:estado nombre set value "Gas tóxico: Y 120"
tellraw @a ["",{"text":"ROJO - GAS TÓXICO: Y 120\n","color":"red","bold":true},{"text":"Escapa del gas: sube hasta Y 120 en 3 minutos. Peligros: creepers encendidos sobre ti cada 2 s.","color":"red"}]
function rojo:start
