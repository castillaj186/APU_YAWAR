function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #cn rj_k 200
scoreboard players set #cnn rj_k 1
scoreboard players set #hd rj_k 240
scoreboard players set #hn rj_k 12
data modify storage rojo:estado nombre set value "Nuclear y horda"
tellraw @a ["",{"text":"ROJO - NUCLEAR Y HORDA\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers NUCLEARES, hordas de zombies.","color":"red"}]
function rojo:start
