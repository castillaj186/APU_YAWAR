function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #wi rj_k 100
scoreboard players set #vi rj_k 80
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Brujas y vindicadores"
tellraw @a ["",{"text":"ROJO - BRUJAS Y VINDICADORES\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: brujas, vindicadores.","color":"red"}]
function rojo:start
