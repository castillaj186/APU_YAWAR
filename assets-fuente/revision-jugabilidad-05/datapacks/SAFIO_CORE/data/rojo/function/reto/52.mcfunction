function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 3600
scoreboard players set #tier rj_k 1
scoreboard players set #twist rj_k 1
scoreboard players set #hd rj_k 240
scoreboard players set #hn rj_k 10
data modify storage rojo:estado nombre set value "Horda de zombies"
tellraw @a ["",{"text":"ROJO - HORDA DE ZOMBIES\n","color":"red","bold":true},{"text":"Sobrevive 3 minutos. Peligros: hordas de zombies.","color":"red"}]
function rojo:start
