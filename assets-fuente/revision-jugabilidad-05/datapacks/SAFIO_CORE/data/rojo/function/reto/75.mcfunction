function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 0
scoreboard players set #st rj_k 100
scoreboard players set #sn rj_k 3
scoreboard players set #alt rj_k 160
data modify storage rojo:estado nombre set value "Subir hasta Y 160"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 160\n","color":"red","bold":true},{"text":"Sube hasta Y 160 en 5 minutos. Peligros: tormenta con TRIPLE rayo.","color":"red"}]
function rojo:start
