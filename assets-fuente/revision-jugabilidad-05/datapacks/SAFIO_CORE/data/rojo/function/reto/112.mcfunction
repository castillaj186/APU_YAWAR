function rojo:cfg_reset
scoreboard players set #kind rj_k 3
scoreboard players set #dur rj_k 6000
scoreboard players set #tier rj_k 2
scoreboard players set #twist rj_k 0
scoreboard players set #alt rj_k 250
scoreboard players set #zb rj_k 100
scoreboard players set #mc rj_k 1
data modify storage rojo:estado nombre set value "Subir hasta Y 250 (zombies)"
tellraw @a ["",{"text":"ROJO - SUBIR HASTA Y 250\n","color":"red","bold":true},{"text":"Sube hasta Y 250 en 5 minutos. Peligros: zombies.","color":"red"}]
function rojo:start
