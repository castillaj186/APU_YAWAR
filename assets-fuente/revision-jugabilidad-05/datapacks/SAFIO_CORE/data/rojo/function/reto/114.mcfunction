function rojo:cfg_reset
scoreboard players set #kind rj_k 6
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k 0
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "Bajar hasta Y 0"
tellraw @a ["",{"text":"ROJO - BAJAR HASTA Y 0\n","color":"red","bold":true},{"text":"Baja hasta Y 0 o menos en 4 minutos. Peligros: zombies.","color":"red"}]
function rojo:start
