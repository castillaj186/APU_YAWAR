function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 4800
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 1
scoreboard players set #cn rj_k 200
scoreboard players set #cnn rj_k 1
data modify storage rojo:estado nombre set value "Creepers nucleares"
tellraw @a ["",{"text":"ROJO - CREEPERS NUCLEARES\n","color":"red","bold":true},{"text":"Sobrevive 4 minutos. Peligros: creepers NUCLEARES.","color":"red"}]
function rojo:start
