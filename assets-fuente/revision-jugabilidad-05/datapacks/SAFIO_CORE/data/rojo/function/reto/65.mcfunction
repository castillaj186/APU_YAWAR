function rojo:cfg_reset
scoreboard players set #kind rj_k 2
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #ce rj_k 60
scoreboard players set #cen rj_k 1
scoreboard players set #cn rj_k 300
scoreboard players set #cnn rj_k 1
scoreboard players set #zb rj_k 60
scoreboard players set #sk rj_k 60
scoreboard players set #sp rj_k 80
scoreboard players set #ph rj_k 120
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #hd rj_k 400
scoreboard players set #hn rj_k 8
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Apocalipsis"
tellraw @a ["",{"text":"ROJO - APOCALIPSIS\n","color":"red","bold":true},{"text":"Sobrevive 5 minutos. Peligros: creepers ELÉCTRICOS, creepers NUCLEARES, zombies, esqueletos, arañas, phantoms, tormenta con TRIPLE rayo, hordas de zombies.","color":"red"}]
function rojo:start
