function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #cn rj_k 120
scoreboard players set #cnn rj_k 2
data modify storage rojo:estado nombre set value "Creepers nucleares sin descanso"
tellraw @a ["",{"text":"ROJO - CREEPERS NUCLEARES SIN DESCANSO\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers NUCLEARES.","color":"red"}]
function rojo:start
