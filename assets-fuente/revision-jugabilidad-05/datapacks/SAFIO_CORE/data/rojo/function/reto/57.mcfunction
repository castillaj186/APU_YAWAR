function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #sk rj_k 40
scoreboard players set #mc rj_k 3
data modify storage rojo:estado nombre set value "Lluvia de esqueletos"
tellraw @a ["",{"text":"ROJO - LLUVIA DE ESQUELETOS\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Peligros: esqueletos.","color":"red"}]
function rojo:start
