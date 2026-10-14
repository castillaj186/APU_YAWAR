function rojo:cfg_reset
scoreboard players set #kind rj_k 6
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k -30
scoreboard players set #cr rj_k 60
data modify storage rojo:estado nombre set value "Bajar hasta Y -30"
tellraw @a ["",{"text":"ROJO - BAJAR HASTA Y -30\n","color":"red","bold":true},{"text":"Baja hasta Y -30 o menos en 4 minutos. Peligros: creepers encendidos.","color":"red"}]
function rojo:start
