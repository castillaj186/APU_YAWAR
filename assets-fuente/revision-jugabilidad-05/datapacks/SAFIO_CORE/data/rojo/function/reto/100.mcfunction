function rojo:cfg_reset
scoreboard players set #kind rj_k 1
scoreboard players set #dur rj_k 12000
scoreboard players set #tier rj_k 3
scoreboard players set #twist rj_k 1
scoreboard players set #dest rj_k 0
scoreboard players set #ce rj_k 60
scoreboard players set #cen rj_k 1
scoreboard players set #cn rj_k 300
scoreboard players set #cnn rj_k 1
scoreboard players set #zb rj_k 50
scoreboard players set #sk rj_k 60
scoreboard players set #ph rj_k 120
scoreboard players set #st rj_k 60
scoreboard players set #sn rj_k 3
scoreboard players set #hd rj_k 240
scoreboard players set #hn rj_k 12
scoreboard players set #mc rj_k 2
data modify storage rojo:estado nombre set value "Destino al azar: infierno total"
tellraw @a ["",{"text":"ROJO - DESTINO AL AZAR: INFIERNO TOTAL\n","color":"red","bold":true},{"text":"Llega al destino y quédate dentro. Tienes 10 minutos. Peligros: creepers ELÉCTRICOS, creepers NUCLEARES, zombies, esqueletos, phantoms, tormenta con TRIPLE rayo, hordas de zombies.","color":"red"}]
function rojo:start
