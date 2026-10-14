function rojo:cfg_reset
scoreboard players set #kind rj_k 6
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k -50
scoreboard players set #sk rj_k 80
scoreboard players set #blind rj_k 2
data modify storage rojo:estado nombre set value "Bajar hasta Y -50"
tellraw @a ["",{"text":"ROJO - BAJAR HASTA Y -50\n","color":"red","bold":true},{"text":"Baja hasta Y -50 o menos en 5 minutos. Peligros: esqueletos y oscuridad permanente.","color":"red"}]
function rojo:start
